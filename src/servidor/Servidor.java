package servidor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextArea;

/**
 * En esta clase, que hereda de JTextArea (como log del servidor) e implementa
 * Runnable para actuar como Thread, se gestiona la conexión de los clientes con
 * el servidor. También contiene la lista de clientes conectados, la lista de
 * palabras prohibidas y los métodos sincronizados para el acceso a los recursos
 * compartidos por los hilos. En el método run() se inicia ServerSocket, el
 * MulticastSocket y después se atiende en bucle las peticiones de conexión de
 * los clientes. Por cada cliente que accede, se inicia un HiloGestionClientes
 * para gestionar la comunicación, dejando este hilo exclusivamente para atender
 * conexiones.
 *
 * @author Ivan Martin
 */
public class Servidor extends JTextArea implements Runnable {

    private static JTextArea log;
    private static final String IP_MULTICAST = "231.0.0.1";
    private static final int PUERTO_TCP = 2000;
    private static final int PUERTO_MULTICAST = 10000;

    private static ServerSocket socketServidor;
    private static InetAddress grupo;
    private static MulticastSocket socketMulticast;

    private static List<HiloGestionClientes> clientes;
    private static List<String> palabrasProhibidas;

    private Thread hilo;

    public Servidor() {
        clientes = new ArrayList<>();
        hilo = new Thread(this);
        palabrasProhibidas = new ArrayList<>() {
            {
                add("Cocacola");
                add("Pepsi");
                add("Danone");
                add("Nestle");
                add("Puleva");
                add("Bimbo");
                add("Pascual");
                add("Campofrio");
            }
        };
        // Valores para el JTextArea
        log = this;
        this.setEditable(false);
        this.setColumns(20);
        this.setRows(5);
    }

    public void start() {
        hilo.start();
    }

    public static synchronized List<HiloGestionClientes> getClientes() {
        return clientes;
    }

    /**
     * Método que comprueba que el nick recibido no está siendo usado o
     * pertenezca a un usuario bloqueado, buscando en la lista de clientes
     * conectados.
     *
     * @param nickSolicitado el nick enviado por el cliente.
     * @return true si el nick está libre, false si está siendo ya usado.
     */
    public static synchronized boolean nickDisponible(String nickSolicitado) {
        for (HiloGestionClientes cliente : clientes) {
            if (cliente.getNick().equalsIgnoreCase(nickSolicitado)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Método que elimina un cliente de la lista de clientes.
     *
     * @param cliente el hilo que solicita la desconexión.
     */
    public static synchronized void eliminarCliente(HiloGestionClientes cliente) {
        clientes.remove(cliente);
        MainServidor.actualizarListaClientes();
    }

    /**
     * Método que comprueba que el mensaje sea adecuado, y no contenga ninguna
     * palabra de las que han sido prohibidas.
     *
     * @param mensaje el mensaje enviado por el cliente para comprobarlo.
     * @return true si el mensaje es adecuado, false si encuentra alguna palabra
     * prohibida.
     */
    public static synchronized boolean mensajeAdecuado(String mensaje) {
        for (String palabra : palabrasProhibidas) {
            if (mensaje.toLowerCase().contains(palabra.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Método que envía el mensaje de un cliente (después de haber sido
     * validado) por multicast. El mensaje que cada HiloGestionClientes recibe,
     * es enviado por este mismo socket multicast, por lo que se debe
     * sincronizar el método.
     *
     * @param mensaje el mensaje ya validado que se enviará por multicast a los
     * clientes.
     */
    public static synchronized void enviarMensajePorMulticast(String mensaje) {
        DatagramPacket paquete = new DatagramPacket(new byte[0], 0, grupo, PUERTO_MULTICAST);
        byte[] buffer = mensaje.getBytes();
        paquete.setData(buffer);
        paquete.setLength(buffer.length);
        try {
            socketMulticast.send(paquete);
        } catch (IOException ex) {
            escribirLog("Error. El mensaje no se pudo enviar a los clientes.");
        }
    }

    /**
     * Método que escribe el log del servidor, con todos los eventos que van
     * sucediendo.
     *
     * @param mensaje el mensaje que los hilos que participan en el servidor
     * envían para ser escritos en el JTextArea.
     */
    public static synchronized void escribirLog(String mensaje) {
        log.setText(log.getText() + "\n" + mensaje);
    }

    @Override
    public void run() {
        try {
            socketServidor = new ServerSocket(PUERTO_TCP);
            setText("Servidor iniciado.\nEscuchando en puerto " + PUERTO_TCP + "...");

            grupo = InetAddress.getByName(IP_MULTICAST);
            socketMulticast = new MulticastSocket(PUERTO_MULTICAST);

            // Escucha constante de peticiones de conexión de clientes
            while (true) {
                Socket socketCliente = socketServidor.accept();
                HiloGestionClientes nuevoCliente = new HiloGestionClientes(socketCliente);
                clientes.add(nuevoCliente);
                nuevoCliente.start();
            }
        } catch (IOException ex) {
            escribirLog("Servicio servidor finalizado.");
        } finally {
            cerrarRecursos();
        }
    }

    /**
     * Método que libera los recursos de los sockets. A su vez permite la
     * finalización manual de la conexión.
     *
     */
    public void cerrarRecursos() {
        try {
            if (socketMulticast != null) {
                socketMulticast.close();
            }
            if (socketServidor != null) {
                socketServidor.close();
            }
        } catch (IOException ex) {

        }
    }
}
