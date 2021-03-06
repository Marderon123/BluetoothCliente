package com.example.bluethoothtruco;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //Declaro los componentes que vamos a usar
    Button botonEnviar, botonBuscarDispositivos;
    CheckBox botonhabilitado, botonvisible;
    ImageView botonbuscar;
    TextView nombre_bt;
    ListView listaEmparejados,listaDDisponibles;
    private BluetoothSocket mmSocket;
    private byte[] mmBuffer;

    // Nombre de la clase para el log
    private static final String TAG = MainActivity.class.getSimpleName();

    //variables para guardar el nombre de los dispositivos emparejados y su direccion
    private String nombreDispositivo;
    private String direccionDispositivo;

    //Una lista de arrays para guardar el nombre de los dispositivos que se están buscando
    ArrayList<String> arraydisponibles = new ArrayList<String>();
    ArrayAdapter<String> adaptadordisponibles;

    //java.util.UUID myOwnUUID = UUID.randomUUID();
    //String MY_UUID = myOwnUUID.toString();
    private static final UUID MY_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");


    //Declaro un adaptador de bluetooth para comprobar si el servicio Bluetooth esta disponible en el dispositivo.
    //Dentro del archivo Manifest hay que declarar dos permisos adecuados para poder usar el Bluetooth.
    private BluetoothAdapter adaptadorBluetooth;

    //Este objeto de tipo BluetoothDevice permite crear una conexion con el dispositivo respectivo o consultar informacion sobre él,nombre, dirección... para los emparejados
    private Set<BluetoothDevice> emparejardispositivos;


    //Para empezar a detectar cada dispositivo nuevo usamos un BroadcastReceiver, basicamente un registrado dinámico
    private final BroadcastReceiver miBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String accion = intent.getAction();

            switch (accion) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.d(TAG, "Búsqueda de dispositivos iniciada");
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.d(TAG, "Búsqueda de dispositivos finalizada");
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice dispositivo = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    arraydisponibles.add(dispositivo.getName());
                    adaptadordisponibles.notifyDataSetChanged();
                    Log.d(TAG, "Dispositivo encontrado: " + dispositivo.getName());
                    //listaDDisponibles.setAdapter(adaptadordisponibles);
                    break;
            }

        }
    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Asigno las variables a sus respectivos componentes
        botonhabilitado = findViewById(R.id.botonhabilitado);
        botonvisible = findViewById(R.id.botonvisible);
        botonbuscar = findViewById(R.id.botonbuscar);
        botonEnviar = findViewById(R.id.botonEnviar);
        botonBuscarDispositivos = findViewById(R.id.botonBusqueda);


        nombre_bt = findViewById(R.id.nombrebluetooth);
        //mediante este metodo mostramos el nombre del dispositivo Bluetooth
        nombre_bt.setText(getLocalBluetoothName());

        //Lista donde saldrán los dispositivos emparejados
        listaEmparejados = findViewById(R.id.listaemparejados);
        //Lista donde saldrán los dispositivos disponibles cercanos
        listaDDisponibles = findViewById(R.id.listadisponibles);
        adaptadordisponibles = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,arraydisponibles);
        listaDDisponibles.setAdapter(adaptadordisponibles);

        //Aqui comenzamos a usar el adaptador para comprobar si se puede usar el Bluetooth
        adaptadorBluetooth = BluetoothAdapter.getDefaultAdapter();

        //Si no es soportado,no se realiza nada
        if (adaptadorBluetooth == null) {
            Toast.makeText(this, "Bluetooth no soportado", Toast.LENGTH_SHORT).show();
            finish();
        }

        //Si esta habilitado el checkbox se marca
        if (adaptadorBluetooth.isEnabled()) {
            botonhabilitado.setChecked(true);
        } else {
            botonhabilitado.setChecked(false);
        }


        /**
         * Hay que registrar el Broadcast Receiver con las 3 acciones demandadas
         */
        IntentFilter filtroBusqueda = new IntentFilter();
        filtroBusqueda.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filtroBusqueda.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filtroBusqueda.addAction(BluetoothDevice.ACTION_FOUND);

        registerReceiver(miBroadcastReceiver, filtroBusqueda);

        /**
         * Implementar el metodo OnCheckedListener para mostrar que hacer en cada caso
         * Cuando no está seleccionado el checkbox quiere decir que el bluetooth está apagado(llama al metodo) y muestra un mensaje
         * Si no realiza un intent en el que busca otra vez la accion para habilitar el bluetooth de nuevo y muestra otro mensaje
         * Para esto hay que añadir el permiso "android.permission.BLUETOOTH_CONNECT" al archivo Manifest
         */
        botonhabilitado.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton vistaBoton, boolean seleccionado) {
                if (!seleccionado) {
                    adaptadorBluetooth.disable();
                    Toast.makeText(MainActivity.this, "Apagado", Toast.LENGTH_SHORT).show();
                    botonbuscar.setEnabled(false);
                    botonBuscarDispositivos.setEnabled(false);
                    botonEnviar.setEnabled(false);
                } else {
                    Intent intentencendido = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    //startActivityForResult(intentencendido, 0);
                    ActivityResult.launch(intentencendido);
                    Toast.makeText(MainActivity.this, "Encendido", Toast.LENGTH_SHORT).show();
                    botonbuscar.setEnabled(true);
                    botonBuscarDispositivos.setEnabled(true);
                    botonEnviar.setEnabled(true);
                }
            }

            ActivityResultLauncher<Intent> ActivityResult = registerForActivityResult(

                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        //Se implementa el método onActivityResult
                        @Override
                        @SuppressLint("LongLogTag")
                        public void onActivityResult(ActivityResult result) {
                            //Se comprueba que el código del resultado sea correcto
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                Log.d(TAG, "El usuario ha aceptado la peticion");
                            } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                                Log.d(TAG, "Error o " + "El usuario ha rechazado");
                            }
                        }
                    });
        });


        /**
         * El botonBusqueda se encarga de buscar los dispositivos disponibles a los que se puede emparejar
         * Saldrán los dispositivos en una lista nueva
         */
        botonBuscarDispositivos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (adaptadorBluetooth.isDiscovering()) {
                    Log.d(TAG, "Ya está buscando dispositivos");
                } else if (adaptadorBluetooth.startDiscovery()) {
                    Log.d(TAG, "Buscando dispositivos");
                } else {
                    Log.d(TAG, "Error al buscar dispositivos");
                }
                if (adaptadorBluetooth.cancelDiscovery()) {
                    Log.d(TAG, "Deteniendo la busqueda de dispositivos");
                } else {
                    Log.d(TAG, "Error al detener la busqueda de dispositivos");
                }
                list();
            }


            public void onDetenerBusquedaClick(View view) {
                if (adaptadorBluetooth.cancelDiscovery()) {
                    Log.d(TAG, "Deteniendo la busqueda de dispositivos");
                } else {
                    Log.d(TAG, "Error al detener la busqueda de dispositivos");
                }
            }
        });

        /**
         * En caso del checkbox visible, indica si el dispositivo bluetooth esta visible para otros dispositivos
         * Para que esta accion funcione hay que añadir el permiso "android.permission.BLUETOOTH_ADVERTISE" al archivo Manifest
         */
        botonvisible.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton vistaBoton, boolean seleccionado) {
                if (seleccionado) {
                    Intent volversevisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    //startActivityForResult(volversevisible, 0);
                    startActivity(volversevisible);
                    Toast.makeText(MainActivity.this, "Visible por 2 minutos", Toast.LENGTH_SHORT).show();
                }
            }
        });


        //El boton buscar es la imagen con el simbolo de bluetooth, que llama al metodo list al clicarlo
        botonbuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                list();
            }
        });
        botonEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarTexto();
            }
        });
    }


    //Metodo para mostrar los dispositivos emparejados en la lista
    public void list() {
        emparejardispositivos = adaptadorBluetooth.getBondedDevices();

        ArrayList lista = new ArrayList();

        for (BluetoothDevice dispositivoBluetooth : emparejardispositivos) {
            lista.add(dispositivoBluetooth.getName());
        }

        Toast.makeText(this, "Mostrando dispositivos emparejados", Toast.LENGTH_SHORT).show();
        ArrayAdapter adaptador = new ArrayAdapter(this, android.R.layout.simple_list_item_1, lista);
        listaEmparejados.setAdapter(adaptador);
        //listaDDisponibles.setAdapter(adaptadordisponibles);
        listaDDisponibles.setAdapter(adaptador);


        //Cuando pulsas cualquier elemento de la lista de dispositivos posibles, obtiene su nombre
        //y su direccion hardware(UUID)
        listaEmparejados.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                nombreDispositivo = (String) lista.get(position);
                for (BluetoothDevice dispositivoBluetooth : emparejardispositivos) {
                    if (nombreDispositivo.equals(dispositivoBluetooth.getName())) {
                        direccionDispositivo = dispositivoBluetooth.getAddress();
                    }
                }


                //conexion al dispositivo que hemos indicado
                BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                BluetoothDevice mBluetoothDevice = bluetoothManager.getAdapter().getRemoteDevice(direccionDispositivo);

                try {
                    mmSocket = mBluetoothDevice.createRfcommSocketToServiceRecord((MY_UUID));
                } catch (IOException e) {
                    System.out.println("falla creacion de socket");
                }

                try {
                    mmSocket.connect();

                } catch (IOException connectException) {
                    try {
                        mmSocket.close();
                    } catch (IOException closeException) {
                        System.out.println("falla conexion");
                    }
                }

            }
        });


    }

    //Metodo para recoger el Nombre Bluetooth Local y lo muestra por pantalla
    public String getLocalBluetoothName() {
        if (adaptadorBluetooth == null) {
            adaptadorBluetooth = BluetoothAdapter.getDefaultAdapter();
        }
        String nombre = adaptadorBluetooth.getName();
        if (nombre == null) {
            nombre = adaptadorBluetooth.getAddress();
        }
        return nombre;
    }

    //constantes para saber qué acción hacemos en cada momento de enviar mensajes
    private interface MessageConstants {
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;
    }

    public void enviarTexto() {

        String mensaje = "Hola Mundo!";
        byte[] byteArrray = mensaje.getBytes();
        Handler handler = null;
        OutputStream mmOutStream = null;

        try {
            //método que envia el mensaje que queremos
            mmOutStream.write(byteArrray);

            //Si el mensaje se envia de forma correcta, se manda a la activity principal
            Message writtenMsg = handler.obtainMessage(
                    MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
            writtenMsg.sendToTarget();

        } catch (IOException e) {

            //Si el mensaje no se envia de forma correcta se notifica con un toast
            Message writeErrorMsg =
                    handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString("toast",
                    "No se ha podido enviar los datos");
            writeErrorMsg.setData(bundle);
            handler.sendMessage(writeErrorMsg);
        }

    }


}