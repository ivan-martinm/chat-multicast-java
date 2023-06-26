package cliente;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

/**
 * Esta clase, extiende a Thread y se encarga de la recepción de mensajes a
 * través de multicast una vez establecida la conexión y habiéndose permitido el
 * acceso al chat (estas 2 operaciones se realizan previamente en la clase
 * Cliente).
 * El flujo del hilo consta de la escucha en bucle de mensajes provenientes del
 * socket multicast proporcionado en el constructor de la clase. Cada mensaje
 * recibido se procesa, ajustando su tamaño y creando un objeto String que
 * traduce los bytes del datagrama, y se escribe en el JTextArea de la
 * interfaz.
 *
 * @author Ivan Martin
 */
public class HiloMulticast extends Thread {

    private static final int BYTES_MAXIMOS = 256;

    private Cliente cliente;
    private MulticastSocket socketMulticast;

    public HiloMulticast(Cliente cliente, MulticastSocket multicastSocket) {
        this.cliente = cliente;
        this.socketMulticast = multicastSocket;
    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[BYTES_MAXIMOS];
            byte[] bufferAjuste;

            DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);

            /* Se mantiene el hilo en escucha de mensajes a través del socket 
            multicast, procesándolos y escribiéndolos en el JTextArea. */
            String mensajeMulticast;
            while (true) {
                socketMulticast.receive(paquete);
                bufferAjuste = new byte[paquete.getLength()];
                System.arraycopy(paquete.getData(), 0, bufferAjuste, 0, paquete.getLength());
                mensajeMulticast = new String(bufferAjuste);
                cliente.escribirEnTextArea(mensajeMulticast);
            }
        } catch (IOException ex) {

        } finally {
            if (socketMulticast != null) {
                socketMulticast.close();
            }
        }
    }
}
