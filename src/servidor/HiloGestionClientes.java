package servidor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import javax.swing.JTextArea;

/**
 * Esta clase, extiende a Thread y se encarga de la comunicación del servidor
 * con el cliente una vez establecida la conexión entre ambos (la conexión se
 * realiza en la clase Servidor y se delega el resto de la comunicación a este
 * hilo). El flujo del hilo consta del control de acceso mediante la recepción
 * del nick proveniente del cliente, seguido de la escucha y procesado de los
 * mensajes que envía el cliente. En función del mensaje y del procesado, se
 * envían mensajes por TCP al cliente o por multicast a todo el grupo de
 * clientes.
 *
 * @author Ivan Martin
 */
public class HiloGestionClientes extends Thread {

    private String nick;
    private int contadorAdvertencias;
    private boolean bloqueado;
    private Socket socketCliente;
    private JTextArea txtAreaLog;
    private DataInputStream entrada;
    private DataOutputStream salida;

    /**
     * En el constructor de esta clase se recibe el Socket de conexión con el
     * cliente, proveniente de la clase Cliente.
     *
     * @param socketCliente
     */
    public HiloGestionClientes(Socket socketCliente) {
        this.nick = "";
        this.contadorAdvertencias = 0;
        this.bloqueado = false;
        this.socketCliente = socketCliente;
        this.txtAreaLog = txtAreaLog;
    }

    public String getNick() {
        return nick;
    }

    public boolean getBloqueado() {
        return bloqueado;
    }

    /**
     * Método que ejecuta cuando se ha realizado la desconexión, bien por
     * petición del cliente o por un cierre del socket (una excepción o
     * finalizar el proceso cliente sin solicitar la desconexión al servidor).
     */
    private void desconectar() {
        // Si el cliente tenía acceso al chat, se notifica a los demás clientes
        if (!nick.equals("")) {
            Servidor.enviarMensajePorMulticast(">> " + nick + " ha abandonado el chat.");
        }

        // Si no hay orden de bloquearlo, se elimina de la lista para liberar su nick
        if (!bloqueado) {
            Servidor.eliminarCliente(this);
        }

        MainServidor.actualizarListaClientes();
        Servidor.escribirLog("Un cliente se ha desconectado. (Nick:\"" + nick + "\")");
    }

    @Override
    public void run() {
        try {
            entrada = new DataInputStream(socketCliente.getInputStream());
            salida = new DataOutputStream(socketCliente.getOutputStream());

            // Cliente conectado, se envían las instrucciones para que introduzca un nick
            salida.writeUTF("Bienvenido al chat. Introduce tu nick.");
            Servidor.escribirLog("Un cliente nuevo se ha conectado. Esperando a que introduzca un nick.");

            /* El hilo se mantiene a la espera de recibir un nick válido por 
            parte del cliente, enviando false en caso contrario. El flujo sólo 
            continuará cuando el nick sea válido.
             */
            String nickSolicitado = entrada.readUTF();
            while (!Servidor.nickDisponible(nickSolicitado)) {
                salida.writeBoolean(false);
                Servidor.escribirLog("Un cliente ha elegido un nick no disponible. Enviando 'false' para que escoja otro.");
                nickSolicitado = entrada.readUTF();
            }

            this.nick = nickSolicitado;
            salida.writeBoolean(true); // Nick válido, se permite el acceso
            MainServidor.actualizarListaClientes();
            Servidor.escribirLog("El nick " + nick + " ha sido asignado a un cliente. Enviando 'true' para darle acceso al chat.");

            /* Se notifica a todos los clientes conectados por multicast, la 
            conexión de un nuevo cliente*/
            Servidor.enviarMensajePorMulticast(">> " + nick + " se ha unido al chat.");

            /* A partir de ahora el hilo se mantiene a la escucha de nuevos
            mensajes por parte del cliente. Para cada mensaje se comprueba si
            el usuario solicita desconexión (en cuyo caso se envía el mensaje
            de desconexión) o se procesa el mensaje y se actúa en consecuencia.
             */
            String mensaje;
            do {
                mensaje = entrada.readUTF(); // Se recibe el mensaje del cliente
                if (mensaje.equals("!salir")) { // Si el cliente solicita la desconexión
                    salida.writeUTF("!TERMINAR_SESION"); // Se envía la orden de desconexión al cliente.
                    desconectar();
                    continue;
                }
                // Si no era el mensaje de desconexión, se procesa:
                if (!Servidor.mensajeAdecuado(mensaje)) { // Si el mensaje no es adecuado (tiene palabras prohibidas)
                    // Se notifica por TCP sólamente a este cliente
                    salida.writeUTF(">> Tu mensaje contiene palabras prohibidas. Por favor, sigue las normas de los mensajes.");
                    Servidor.escribirLog("El cliente " + nick + " ha escrito un mensaje inapropiado. No se enviará por multicast.");
                    contadorAdvertencias++; // Y se aumenta el número de advertencias
                } else { // Si el mensaje era adecuado
                    // Se envía a todos los clientes por multicast
                    Servidor.enviarMensajePorMulticast(nick + ": " + mensaje);
                    Servidor.escribirLog("El cliente " + nick + " ha sido escrito un mensaje válido. Se ha enviado por multicast a todos los clientes.");
                }
            } while (!mensaje.equals("!salir") && contadorAdvertencias < 3);

            if (contadorAdvertencias >= 3) { // Si se llega a 3 advertencias
                bloqueado = true; // Se bloquea al usuario, y se le comunica por TCP
                salida.writeUTF(">> Tu acceso al chat ha sido bloqueado por inclumplir las normas 3 veces");
                Servidor.escribirLog("El cliente " + nick + " ha sido expulsado y bloqueado por inclumplir las normas");
                // Se informa a todos los clientes de la expulsión
                Servidor.enviarMensajePorMulticast(">> El cliente " + nick + " ha sido expulsado y bloqueado por inclumplir las normas");
                desconectar(); // Y se le desconecta
            }
        } catch (IOException ex) { // Si el cliente pierde la conexión o cierra la ventana
            desconectar();
        } finally {
            cerrarRecursos(); // Se liberan los recursos
        }
    }

    /**
     * Método para liberar los recursos del socket y los streams.
     */
    private void cerrarRecursos() {
        try {
            System.out.println("HiloGestionClientes (" + nick + "): ");
            if (entrada != null) {
                entrada.close();
            }
            if (salida != null) {
                salida.close();
            }
            if (socketCliente != null) {
                socketCliente.close();
            }
        } catch (IOException ex) {

        }
    }
}
