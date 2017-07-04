package br.com.servelojapagamento.interfaces;

import java.util.List;

import stone.application.enums.ErrorsEnum;

/**
 * Created by Alexandre on 03/07/2017.
 */

public interface RespostaTransacaoListener {

    void onRespostaTransacao(boolean status, List<ErrorsEnum> listaErros);

}