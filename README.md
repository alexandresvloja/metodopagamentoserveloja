## ServelojaBluetooth
Gerencia todas operações em relação a comunicação Bluetooth com a Pidpad

* Construtor
```java
public ServelojaBluetooth(Activity activity)
```

* Define o ouvinte dos Status do Bluetooth. Através deste ouvinte, é possível obter os dispositivos encontrados, os estados (ativado ou desativado) e a finalização de procura de dispositivos.
```java
public void setStatusBluetoothListener(StatusBluetoothListener statusBluetoothListener)
```

* Após chamar esse método, um serviço será inicializado, e assim, detectará todas alterações referente aos Status do Bluetooth, onde essas alterações serão passadas para o ouvinte definido no método setStatusBluetoothListener()
```java
public void iniciarServicoBluetooth()
```

* Pára o serviço do Bluetooth
```java
public void pararServicoBluetooth()
```

* Verifica se o Bluetooth do aparelho, está ativo.
```java
public boolean checkBluetoothAtivado()
```

* Inicializa a procura por dispositivos.
```java
public boolean iniciarProcuraDispositivos()
```

* Pára a procura por dispositivos.
```java
public boolean pararProcuraDispositivo()
```

* Envia uma solicitação para o usuário ativar o Bluetooth do aparelho.
```java
public void solicitarAtivacaoBluetooth()
```

* Obtém uma lista de dispositivos já pareados.
```java
public Set<BluetoothDevice> getDispositivosPareados()
```

## Implementação ServelojaBluetooth

```java
private ServelojaBluetooth servelojaBluetooth;
private Button btAbrirDialogProcurarDispositivos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        servelojaBluetooth = new ServelojaBluetooth(this);

        // a prória Activity, implementa o ouvinte, e assim, passando this como parâmetro de ouvinte
        servelojaBluetooth.setStatusBluetoothListener(this);

        btAbrirDialogProcurarDispositivos = (Button) findViewById(R.id.ac_main_bt_abrir_dialog);

        btAbrirDialogProcurarDispositivos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // verifica se o Bluetooth está ativo
                if (servelojaBluetooth.checkBluetoothAtivado()) {
                    // inicia a busca por dispositivos
                    servelojaBluetooth.iniciarProcuraDispositivos();
                } else {
                    // caso não esteja, solicita o usuário que o ative
                    servelojaBluetooth.solicitarAtivacaoBluetooth();
                }
            }
        });
        
    }

    @Override
    public void onDispositivoEncontradoBluetooth(BluetoothDevice dispositivo) {
        
    }

    @Override
    public void onEstadoAlteradoBluetooth(int estadoAtual, int estadoAnterior) {

    }

    @Override
    public void onProcuraDispositivoFinalizadaBluetooth() {

    }
```

Outra forma de implementar o ouvinte StatusBluetoothListener

```java
    servelojaBluetooth.setStatusBluetoothListener(new StatusBluetoothListener() {
            @Override
            public void onDispositivoEncontradoBluetooth(BluetoothDevice dispositivo) {

            }

            @Override
            public void onEstadoAlteradoBluetooth(int estadoAtual, int estadoAnterior) {

            }

            @Override
            public void onProcuraDispositivoFinalizadaBluetooth() {

            }
        });
```
