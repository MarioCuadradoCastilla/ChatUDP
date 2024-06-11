import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class Servidor {
    static final int PORT = 12345;
    private static Set<String> listaUsuarios = new HashSet<>();
    private static Set<ClientInfo> clientes = new HashSet<>();

    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(PORT)) {
            System.out.println("Puerto del servidor: " + PORT);
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());
                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();

                handleClientMessage(received, clientAddress, clientPort, serverSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClientMessage(String message, InetAddress address, int port, DatagramSocket serverSocket) throws IOException {
        String[] parts = message.split(" ", 2);
        String command = parts[0];

        if (command.equals("/login")) {
            String nombreUsuario = parts[1];
            synchronized (listaUsuarios) {
                if (!listaUsuarios.contains(nombreUsuario)) {
                    listaUsuarios.add(nombreUsuario);
                    clientes.add(new ClientInfo(address, port, nombreUsuario));
                    sendResponse("/loginSuccess " + nombreUsuario, address, port, serverSocket);
                    broadcast(nombreUsuario + " se unió al chat.", serverSocket);
                    sendConnectedUsers(serverSocket);
                } else {
                    sendResponse("/nombreUsado", address, port, serverSocket);
                }
            }
        } else if (command.startsWith("/msg")) {
            broadcast(message.substring(5), serverSocket);
        } else if (command.equals("/salir")) {
            String nombreUsuario = parts[1];
            synchronized (listaUsuarios) {
                listaUsuarios.remove(nombreUsuario);
                clientes.removeIf(client -> client.getNombreUsuario().equals(nombreUsuario));
                broadcast(nombreUsuario + " salió del chat.", serverSocket);
                sendConnectedUsers(serverSocket);
            }
        }
    }

    private static void broadcast(String message, DatagramSocket serverSocket) throws IOException {
        byte[] buffer = message.getBytes();
        for (ClientInfo client : clientes) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, client.getAddress(), client.getPort());
            serverSocket.send(packet);
        }
    }

    private static void sendConnectedUsers(DatagramSocket serverSocket) throws IOException {
        StringBuilder userListMessage = new StringBuilder("/listaUsuarios ");
        for (ClientInfo client : clientes) {
            userListMessage.append(client.getNombreUsuario()).append(" ");
        }
        broadcast(userListMessage.toString(), serverSocket);
    }

    private static void sendResponse(String message, InetAddress address, int port, DatagramSocket serverSocket) throws IOException {
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        serverSocket.send(packet);
    }

    private static class ClientInfo {
        private InetAddress address;
        private int port;
        private String nombreUsuario;

        public ClientInfo(InetAddress address, int port, String nombreUsuario) {
            this.address = address;
            this.port = port;
            this.nombreUsuario = nombreUsuario;
        }

        public InetAddress getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }

        public String getNombreUsuario() {
            return nombreUsuario;
        }
    }
}
