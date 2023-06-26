package cliente;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import javax.swing.JTextArea;

/**
 * En esta clase, que hereda de JTextArea (como ventana de mensajes del chat) e
 * implementa Runnable para actuar como Thread, se gestiona la conexión del
 * cliente con el servidor y el envío y recepción de mensajes por TCP entre
 * ambos. El flujo del hilo consta de la conexión por socket TCP con el
 * servidor, seguidamente se conecta y une al grupo multicast para
 * posteriormente iniciar un HiloMulticast que mantendrá la escucha de mensajes
 * recibidos por multicast provenientes del servidor, dejando este hilo
 * exclusivamente para el intercambio de mensajes por TCP.
 *
 * @author Ivan Martin
 */
public class Cliente extends JTextArea implements Runnable {

    private static final String HOST = "localhost";
    private static final int PUERTO = 2000;
    private static final String IP_MULTICAST = "231.0.0.1";
    private static final int PUERTO_MULTICAST = 10000;

    private Socket socket;
    private MulticastSocket socketMulticast;

    private InetAddress grupo;
    private DataInputStream entrada;
    private DataOutputStream salida;

    private String nick;
    private Thread hilo;

    public Cliente() {
        // Valores para el JTextArea
        this.setEditable(false);
        this.setColumns(20);
        this.setRows(5);
        this.setFocusable(false);

        hilo = new Thread(this);
    }

    public void start() {
        hilo.start();
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    /**
     * Envia un mensaje a través del socket TCP con el mensaje proveniente del
     * JTextField de la interfaz. Se usa tanto para enviar el nick como los
     * mensajes de chat.
     *
     * @param mensaje el mensaje a enviar.
     */
    public void enviarMensajeAlServidor(String mensaje) {
        try {
            salida.writeUTF(mensaje);
        } catch (IOException ex) {
            escribirEnTextArea(">> Error. No se pudo enviar el mensaje.");
        }
    }

    /**
     * Comprueba que el socket de conexión con el servidor no se ha cerrado.
     * (como validación para evitar una excepción si se intenta enviar un nick
     * una vez terminada la conexión).
     *
     * @return true si la conexión está activa, false si la conexión ha
     * finalizado o nunca llegó a producirse.
     */
    public boolean hayConexion() {
        if (socket == null) {
            return false;
        }
        return !socket.isClosed();
    }

    /**
     * Método que escribe todos los mensajes en el JTextArea del chat.
     *
     * @param mensaje el mensaje que se escribirá en el JTextArea.
     */
    public synchronized void escribirEnTextArea(String mensaje) {
        String textoActual = this.getText();
        this.setText(textoActual += mensaje + "\n");
    }

    @Override
    public void run() {
        try {
            socket = new Socket(HOST, PUERTO);

            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());

            /* A la espera del mensaje de bienvenida. Una vez recibido se escribe
            en el TextArea */
            escribirEnTextArea(entrada.readUTF());

            /* A la espera de recibir 'true' por parte del servidor, lo que indica
            que el nick ha sido aceptado. NOTA: la tarea de enviar el nick no le
            corresponde a este hilo, sino que se produce por el evento del botón
            correspondiente en la interfaz gráfica. */
            while (!entrada.readBoolean()) {
                escribirEnTextArea(">> El nick introducido no está disponible. Por favor, escoge otro.");
            }

            // Nick válido, se une al grupo multicast y se concede acceso.
            socketMulticast = new MulticastSocket(PUERTO_MULTICAST);
            grupo = InetAddress.getByName(IP_MULTICAST);
            socketMulticast.joinGroup(grupo);
            HiloMulticast lector = new HiloMulticast(this, socketMulticast);
            lector.start();

            MainCliente.concederAcceso(true);
            escribirEnTextArea(">> Acceso al chat concedido.\n------------------");

            /* Escucha constante de mensajes por TCP desde el servidor (mensajes
            individuales). El servidor también enviará "!TERMINAR_SESION" para 
            dar por terminada la sesión (esto sucede cuando el cliente envía por 
            chat "!salir" o usa el botón de la interfaz para desconectarse). */
            String mensajeTCP;
            do {
                mensajeTCP = entrada.readUTF();
                escribirEnTextArea(mensajeTCP);
            } while (!mensajeTCP.equals("!TERMINAR_SESION"));

            // Se da la conexión por terminada y se revoca el acceso al chat
            MainCliente.concederAcceso(false);
            escribirEnTextArea(">> La conexión con el servidor finalizó.");

        } catch (IOException ex) {
            escribirEnTextArea(">> Se ha perdido la conexión con el servidor.");
            System.out.println(ex.getMessage());
        } finally {
            cerrarRecusos();
        }
    }

    /**
     * Método para liberar los recursos de los sockets y los streams.
     */
    private void cerrarRecusos() {
        try {
            if (entrada != null) {
                entrada.close();
            }
            if (salida != null) {
                salida.close();
            }
            if (socketMulticast != null) {
                socketMulticast.leaveGroup(grupo);
                socketMulticast.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ex) {

        }
    }
}
