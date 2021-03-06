package br.com.servelojapagamento.utils;

import android.app.Activity;
import android.util.Log;

import java.lang.reflect.Field;

import br.com.servelojapagamento.interfaces.RespostaTransacaoAplicativoListener;
import br.com.servelojapagamento.interfaces.RespostaTransacaoClienteListener;
import br.com.servelojapagamento.interfaces.RespostaTransacaoRegistradaServelojaListener;
import br.com.servelojapagamento.interfaces.RespostaTransacaoStoneListener;
import br.com.servelojapagamento.preferences.PrefsHelper;
import br.com.servelojapagamento.webservice_mundipagg.MundipaggWebService;
import br.com.servelojapagamento.webservice_mundipagg.ParamsCriarTokenCartao;
import br.com.servelojapagamento.webservice_mundipagg.ParamsCriarTokenEndereco;
import br.com.servelojapagamento.webservice_mundipagg.ParamsCriarTransacaoSemToken;
import br.com.servelojapagamento.webservice_serveloja.Bandeira;
import br.com.servelojapagamento.webservice_serveloja.ConteudoBandeira;
import br.com.servelojapagamento.webservice_serveloja.ParamsRegistrarTransacao;
import br.com.servelojapagamento.webservice_serveloja.PedidoPinPadResposta;
import br.com.servelojapagamento.webservice_serveloja.ServelojaWebService;
import br.com.servelojapagamento.webservice_serveloja.TransacaoServeloja;
import stone.application.enums.TransactionStatusEnum;
import stone.database.transaction.TransactionDAO;
import stone.database.transaction.TransactionObject;
import stone.providers.TransactionProvider;

/**
 * Efetua a comunicação entre a transação da Stone com a WebService da Serveloja
 * Created by alexandre on 04/07/2017.
 */

public class ServelojaTransacaoUtils
        implements RespostaTransacaoStoneListener,
        RespostaTransacaoRegistradaServelojaListener {

    private Activity activity;
    private String TAG;
    private StoneUtils stoneUtils;
    private ServelojaWebService servelojaWebService;
    private MundipaggWebService mundipaggWebService;
    private ParamsRegistrarTransacao paramsRegistrarTransacao;
    private PrefsHelper prefsHelper;
    private ConteudoBandeira conteudoBandeira;
    private boolean cartaoExigeCvv;
    private boolean cartaoExigeSenha;
    private RespostaTransacaoClienteListener respostaTransacaoClienteListener;


    public ServelojaTransacaoUtils(Activity activity) {
        this.activity = activity;
        this.TAG = getClass().getSimpleName();
        this.stoneUtils = new StoneUtils(activity);
        this.paramsRegistrarTransacao = new ParamsRegistrarTransacao();
        this.prefsHelper = new PrefsHelper(activity);
        this.servelojaWebService = new ServelojaWebService(activity, this);
        this.mundipaggWebService = new MundipaggWebService();
        iniciarListaConteudoBandeira();
    }

    public void iniciarTransacao(TransacaoServeloja transacaoServeloja,
                                 RespostaTransacaoClienteListener respostaTransacaoClienteListener) {
        Log.d(TAG, "iniciarTransacao: ");
        this.respostaTransacaoClienteListener = respostaTransacaoClienteListener;
        paramsRegistrarTransacao.setValor(transacaoServeloja.getValor());
        paramsRegistrarTransacao.setValorSemTaxas(transacaoServeloja.getValorSemTaxas());
        paramsRegistrarTransacao.setTipoTransacao(transacaoServeloja.getTipoTransacao());
        paramsRegistrarTransacao.setNumParcelas(transacaoServeloja.getNumParcelas());
        paramsRegistrarTransacao.setPinPadId(prefsHelper.getPinpadModelo());
        paramsRegistrarTransacao.setPinPadMac(prefsHelper.getPinpadMac());
        paramsRegistrarTransacao.setLatLng(prefsHelper.getLocalizacao());
        paramsRegistrarTransacao.setNsuSitef("");
        paramsRegistrarTransacao.setComprovante(transacaoServeloja.getComprovante());
        paramsRegistrarTransacao.setDescricao(transacaoServeloja.getDescricao());
        paramsRegistrarTransacao.setProblemas(transacaoServeloja.getProblemas());
        paramsRegistrarTransacao.setDddTelefone(transacaoServeloja.getDddTelefone());
        paramsRegistrarTransacao.setNumTelefone(transacaoServeloja.getNumTelefone());
        paramsRegistrarTransacao.setUsoTarja(transacaoServeloja.isUsoTarja());
        paramsRegistrarTransacao.setDataTransacao(Utils.getDataAtualStr());
        paramsRegistrarTransacao.setCpfCNPJ(transacaoServeloja.getCpfCnpjComprador());
        paramsRegistrarTransacao.setCpfCnpjAdesao("06130856555");
        // chamando procedimento de transação da Stone, e sua resposta será trazida
        // no Callback onRespostaTransacaoStone
        stoneUtils.iniciarTransacao(paramsRegistrarTransacao.getValor(),
                paramsRegistrarTransacao.getTipoTransacao(),
                paramsRegistrarTransacao.getNumParcelas(), this);
    }

    private void setupParametrosParaRegistrarTransacao(TransactionObject dadosTransacao) {
        Log.d(TAG, "setupParametrosParaRegistrarTransacao: ");
        // parametros retornado referente a transação atual (última transação)
        String cartaoNomeTitular = dadosTransacao.getCardHolderName().trim();
        String cartaoBin = dadosTransacao.getCardHolderNumber().substring(0, 6);
        String cartaoNumero = dadosTransacao.getCardHolderNumber();
        String codAutorizacao = dadosTransacao.getAuthorizationCode();
        String nsuHost = dadosTransacao.getRecipientTransactionIdentification();
        String cartaoBandeira = Utils.obterBandeiraPorBin(cartaoBin);
        String statusTransacao = getStatusTransacao(dadosTransacao.getTransactionStatus());
        // set parametros salvo nas preferencias
        paramsRegistrarTransacao.setNomeTitularCartao(cartaoNomeTitular);
        paramsRegistrarTransacao.setChaveAcesso(prefsHelper.getChaveAcesso());
        paramsRegistrarTransacao.setBandeiraCartao(cartaoBandeira);
        paramsRegistrarTransacao.setNsuHost(nsuHost);
        paramsRegistrarTransacao.setCodAutorizacao(codAutorizacao);
        paramsRegistrarTransacao.setStatusTransacao(statusTransacao);
        paramsRegistrarTransacao.setNumCartao(cartaoNumero);
        paramsRegistrarTransacao.setNumBinCartao(cartaoBin);
        Log.d(TAG, "setupParametrosParaRegistrarTransacao: " + paramsRegistrarTransacao.toString());
    }

    private void criptografarTransacaoStone() {
        Log.d(TAG, "setupParametrosParaRegistrarTransacao: ");
        paramsRegistrarTransacao.setBandeiraCartao(Utils.encriptar(paramsRegistrarTransacao.getBandeiraCartao()));
        paramsRegistrarTransacao.setNumCartao(Utils.encriptar(paramsRegistrarTransacao.getNumCartao()));
    }

    public void criarTransacaoSemToken(ParamsCriarTransacaoSemToken transacao,
                                       RespostaTransacaoAplicativoListener respostaTransacaoAplicativoListener) {
        mundipaggWebService.criarTransacaoSemToken(transacao, respostaTransacaoAplicativoListener);
    }

    public void obterToken(ParamsCriarTokenCartao cartao, ParamsCriarTokenEndereco endereco,
                           RespostaTransacaoAplicativoListener respostaTransacaoAplicativoListener) {
        mundipaggWebService.criarToken(cartao, endereco, respostaTransacaoAplicativoListener);
    }

    public void consultarToken(String token,
                               RespostaTransacaoAplicativoListener respostaTransacaoAplicativoListener) {
        mundipaggWebService.consultarToken(token, respostaTransacaoAplicativoListener);
    }

    public void consultarTransacaoPorReferenciaPedido(String referencia,
                                                      RespostaTransacaoAplicativoListener respostaTransacaoAplicativoListener) {
        mundipaggWebService.consultarTransacaoPorReferenciaPedido(referencia, respostaTransacaoAplicativoListener);
    }

    public void consultarTransacaoPorChavePedido(String chave,
                                                 RespostaTransacaoAplicativoListener respostaTransacaoAplicativoListener) {
        mundipaggWebService.consultarTransacaoPorChavePedido(chave, respostaTransacaoAplicativoListener);
    }

    private String getStatusTransacao(TransactionStatusEnum transactionStatusEnum) {
        switch (transactionStatusEnum) {
            case APPROVED:
                return "APR";
            case DECLINED:
                return "DEC";
            case REJECTED:
                return "REJ";
        }
        return "";
    }

    private String tratarData(String data) {
        int ano = Integer.valueOf(data.substring(0, 2));
        int mes = Integer.valueOf(data.substring(2));
        String result = data.substring(2) + "/";
        result += ano < 60 ? String.valueOf(ano + 2000) : String.valueOf(ano + 1900);
        return result;
    }

    /**
     * Resposta da transação via Stone
     * ao efetuar esta transação, será efetuado o registro na base de dados da Serveloja
     *
     * @param status
     * @param transactionProvider
     */
    @Override
    public void onRespostaTransacaoStone(boolean status, TransactionProvider transactionProvider) {
        Log.d(TAG, "onRespostaTransacaoStone: status " + status);
        Log.d(TAG, "onRespostaTransacaoStone: listaErros " + transactionProvider.getListOfErrors().toString());

        TransactionDAO transactionDAO = new TransactionDAO(activity);
        TransactionObject transactionObject = transactionDAO.findTransactionWithId(
                transactionDAO.getLastTransactionId());

        String cartaoBin = transactionObject.getCardHolderNumber().substring(0, 6);
        ;
        String cartaoBandeira = Utils.obterBandeiraPorBin(cartaoBin);

        if (!(cartaoBandeira.toLowerCase().equals("mastercard") || cartaoBandeira.toLowerCase().equals("visa"))) {
            // falha "Passar um cartão da bandeira " + bandeira + usando o chip não é suportado.\n Por favor " +
            // repita a operação usando a tarja magnética"
        }

        if (status) {
            if (transactionProvider.getListOfErrors().size() > 0) {
                Log.d(TAG, "onRespostaTransacaoStone: erro na transação com a Stone - lista de erro > 0");
                // Usuario Passou cartão chipado que não é master ou visa
            } else {
                Log.d(TAG, "onRespostaTransacaoStone: transação efetuada com sucesso");
                // após verificação de não ocorrência de erros, procede para preparação dos parâmetros
                // e seguir para etapa de inserção na base de dados Serveloja
                setupParametrosParaRegistrarTransacao(transactionObject);
                criptografarTransacaoStone();
                servelojaWebService.registrarTransacao(paramsRegistrarTransacao, this);
            }
        } else {
            // Vendas por tarja magnética sempre retornam o callback onError (false) pois a stone não suporta
            // esse tipo de operação, neste momento, verificar qual foi o Erro e mudar o fluxo caso
            // o fluxo seja para outras bandeiras.
            if (transactionProvider.getListOfErrors().size() > 0) {
                Log.d(TAG, "onRespostaTransacaoStone: erro na transação com a Stone");
                // indicando erro de transação com a Stone
            } else {
                // transação de débito via tarja, não é permitida
                if (paramsRegistrarTransacao.getTipoTransacao() == TransacaoEnum.TipoTransacao.DEBITO) {
                    // enviar erro
                    respostaTransacaoClienteListener.onRespostaTransacaoCliente(
                            TransacaoEnum.StatusSeveloja.TRANSAC_SERVELOJA_DEBITO_NAO_PERMITIDO);
                } else {
                    // indicando que o erro foi referente a tarja
                    Log.d(TAG, "onRespostaTransacaoStone: seguindo o fluxo para transação com tarja");
                    // Usar Reflection para pegar o numero de cartao da resposta do provider tratando a resposta
                    Object dadosCartao = null;
                    try {
                        Field field = transactionProvider.getClass().getDeclaredField("gcrResponseCommand");
                        field.setAccessible(true);
                        // obtem os dados referente ao cartão
                        dadosCartao = field.get(transactionProvider);
                        cartaoBin = dadosCartao.toString().split(",")[8].split("=")[1].substring(1, 7);
                        cartaoBandeira = Utils.obterBandeiraPorBin(cartaoBin);
                        // verifica se a bandeira do cartão, não é permitida pela STONE, pois assim, este cartão, deverá
                        // ser lido com o CHIP. (Caso seja permitido pela Stone, indica que o mesmo possui CHIP)
                        // e verifica se a operação não é de débito
                        if (!stoneUtils.checkBandeiraPermitidaPelaStone(cartaoBandeira)) {
                            Log.d(TAG, "onRespostaTransacaoStone: cartão permitido");
                            setupParametrosParaRegistrarTransacao(transactionObject);
                            String[] tracktrace = dadosCartao.toString().split(",")[8].split("=");
                            String dataValidadeCartao = tratarData(tracktrace[2].substring(0, 4));
                            // atualização dos atributos referente ao cartão
                            paramsRegistrarTransacao.setBandeiraCartao(cartaoBandeira);
                            paramsRegistrarTransacao.setNumCartao(tracktrace[1].substring(1));
                            paramsRegistrarTransacao.setDataValidadeCartao(dataValidadeCartao);
                            paramsRegistrarTransacao.setNumBinCartao(cartaoBin);
                            preVerificacaoTransacaoSegura();
                        }
                    } catch (NoSuchFieldException e) {
                        Log.d(TAG, "onRespostaTransacaoStone: NoSuchFieldException " + e.getMessage());
                    } catch (IllegalAccessException e) {
                        Log.d(TAG, "onRespostaTransacaoStone: IllegalAccessException " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Após verificar que a bandeira é aceita pelo autorizador Serveloja, é feito um pré-processamento
     * efetuar a transação segura
     */
    private void preVerificacaoTransacaoSegura() {
        paramsRegistrarTransacao.setCodSeguracaCartao("");
        paramsRegistrarTransacao.setSenhaCartao("");
        Log.d(TAG, "preVerificacaoTransacaoSegura: ");
        Log.d(TAG, "preVerificacaoTransacaoSegura: " + paramsRegistrarTransacao.toString());
        if (paramsRegistrarTransacao.getBandeiraCartao().equalsIgnoreCase("assomise")
                && paramsRegistrarTransacao.getDataValidadeCartao().equals(""))
            paramsRegistrarTransacao.setDataValidadeCartao("01/20");
        if (!paramsRegistrarTransacao.getNumBinCartao().equals("") &&
                !paramsRegistrarTransacao.getDataValidadeCartao().equals("")) {
            if (checkExigeCVV(paramsRegistrarTransacao.getBandeiraCartao().toLowerCase())) {
                Log.d(TAG, "iniciarTransacaoServeloja: checkExigeCVV true");
                cartaoExigeCvv = true;
                // notifica que é necessário o CVV
                respostaTransacaoClienteListener.onRespostaTransacaoCliente(
                        TransacaoEnum.StatusSeveloja.CARTAO_EXIGE_INFORMAR_CVV);
            } else if (checkExigeSenha(paramsRegistrarTransacao.getBandeiraCartao().toLowerCase())) {
                Log.d(TAG, "iniciarTransacaoServeloja: checkExigeSenha true");
                cartaoExigeSenha = true;
                // notifica que é necessário a SENHA
                respostaTransacaoClienteListener.onRespostaTransacaoCliente(
                        TransacaoEnum.StatusSeveloja.CARTAO_EXIGE_INFORMAR_SENHA);
            } else {
                iniciarTransacaoSegura();
            }
        }
    }

    /**
     * Na transação SEGURA, pode ser feito a solicitação da CVV, logo, o usuário deverá informar a CVV através
     * desse método
     *
     * @param cvv
     */
    public void informarCvv(String cvv) {
        Log.d(TAG, "informarCvv: ");
        if (cartaoExigeCvv) {
            paramsRegistrarTransacao.setCodSeguracaCartao(Utils.encriptar(cvv));
            // após informar a CVV, verifica a exigência de SENHA
            if (checkExigeSenha(paramsRegistrarTransacao.getBandeiraCartao().toLowerCase())) {
                Log.d(TAG, "informarCvv: checkExigeSenha true");
                cartaoExigeSenha = true;
                // notifica o usuário para informar a senha
                respostaTransacaoClienteListener.onRespostaTransacaoCliente(
                        TransacaoEnum.StatusSeveloja.CARTAO_EXIGE_INFORMAR_SENHA);
            } else {
                iniciarTransacaoSegura();
            }
        }
    }

    /**
     * Na transação SEGURA, pode ser feito a solicitação da SENHA, logo, o usuário deverá informar a SENHA através
     * desse método
     *
     * @param senha
     */
    public void informarSenha(String senha) {
        Log.d(TAG, "informarSenha: ");
        if (cartaoExigeSenha) {
            paramsRegistrarTransacao.setSenhaCartao(Utils.encriptar(senha));
            // após informar  senha, é iniciada a transação segura
            iniciarTransacaoSegura();
        }
    }

    private void iniciarTransacaoSegura() {
        Log.d(TAG, "iniciarTransacaoSegura: ");
        // criptografia dos campos
        paramsRegistrarTransacao.setImei(Utils.getIMEI(activity));
        paramsRegistrarTransacao.setBandeiraCartao(Utils.encriptar(paramsRegistrarTransacao.getBandeiraCartao()));
        paramsRegistrarTransacao.setNumCartao(Utils.encriptar(paramsRegistrarTransacao.getNumCartao()));
        paramsRegistrarTransacao.setDataValidadeCartao(Utils.encriptar(paramsRegistrarTransacao.getDataValidadeCartao()));
        paramsRegistrarTransacao.setIp(Utils.getLocalIpAddress());
        paramsRegistrarTransacao.setClienteInformouCartaoInvalido(false);
        paramsRegistrarTransacao.setCpfCnpjAdesao(paramsRegistrarTransacao.getCpfCnpjAdesao());
        paramsRegistrarTransacao.setCpfCNPJ(Utils.encriptar(paramsRegistrarTransacao.getCpfCNPJ()));
        // iniciar registro transação
        Log.d(TAG, "iniciarTransacaoSegura: " + paramsRegistrarTransacao.toString());
        servelojaWebService.registrarTransacaoSegura(paramsRegistrarTransacao);
    }

    /**
     * Resposta do registro da transação na base de dados Serveloja
     * Após efetuar a transação com a Stone, este método é chamado
     *
     * @param status
     * @param pedidoPinPadResposta
     * @param mensagemErro
     */
    @Override
    public void onRespostaTransacaoRegistradaServeloja(boolean status, PedidoPinPadResposta pedidoPinPadResposta,
                                                       String mensagemErro) {
        Log.d(TAG, "onRespostaTransacaoRegistradaServeloja: status " + status);
        Log.d(TAG, "onRespostaTransacaoRegistradaServeloja: mensagemErro " + mensagemErro);
        if (status) {
            Log.d(TAG, "onRespostaTransacaoRegistradaServeloja: PedidoPinPadResposta " +
                    pedidoPinPadResposta.toString());
        }

    }

    @Override
    public void onRespostaTransacaoStatusServeloja(int status) {
        Log.d(TAG, "onRespostaTransacaoStatusServeloja: status " + status);
        respostaTransacaoClienteListener.onRespostaTransacaoCliente(status);
    }

    private boolean checkExigeCVV(String bandeira) {
        Log.d(TAG, "checkExigeCVV: " + bandeira);
        boolean r = false;
        for (Bandeira b : conteudoBandeira.bandeiras)
            if (b.getNomeBandeira().toLowerCase().equals(bandeira.toLowerCase())) {
                r = Boolean.parseBoolean(b.possuiCCV);
            }
        return r;
    }

    private boolean checkExigeSenha(String bandeira) {
        Log.d(TAG, "checkExigeSenha: " + bandeira);
        boolean r = false;
        for (Bandeira b : conteudoBandeira.bandeiras)
            if (b.getNomeBandeira().toLowerCase().equals(bandeira.toLowerCase())) {
                Log.d(TAG, "Bandeira: " + b.toString());
                r = Boolean.parseBoolean(b.possuiSenha);
            }
        return r;
    }

    private void iniciarListaConteudoBandeira() {
        Bandeira[] bandeiras = {
                new Bandeira(2,
                        "AMEX",
                        "False",
                        "True"),

                new Bandeira(18,
                        "ASSOMISE",
                        "True",
                        "True"),

                new Bandeira(4,
                        "DINERS",
                        "False",
                        "True"),

                new Bandeira(16,
                        "ELO",
                        "False",
                        "True"),

                new Bandeira(10,
                        "FORTBRASIL",
                        "False",
                        "True"),

                new Bandeira(7,
                        "HIPER",
                        "False",
                        "True"),

                new Bandeira(5,
                        "HIPERCARD",
                        "False",
                        "True"),

                new Bandeira(3,
                        "MASTERCARD",
                        "False",
                        "True"),

                new Bandeira(6,
                        "SOROCRED",
                        "False",
                        "True"),

                new Bandeira(1,
                        "VISA",
                        "False",
                        "True")
        };
        conteudoBandeira = new ConteudoBandeira(1, bandeiras);
    }

}
