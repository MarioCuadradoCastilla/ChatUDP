import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class Cliente2 extends JFrame {
    private JTextField nicknameField;
    private JButton joinButton;
    private JTextField mensaje;
    private JTextArea chat;
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private String nombreUsuario;

    public Cliente2(String title) {
        setTitle(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel(new FlowLayout());
        nicknameField = new JTextField(20);
        joinButton = new JButton("Entrar");
        joinButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String nickname = nicknameField.getText().trim();
                if (!nickname.isEmpty()) {
                    try {
                        joinChat(nickname);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(Cliente2.this, "Introduce el nombre.");
                }
            }
        });

        inputPanel.add(new JLabel("Nombre:"));
        inputPanel.add(nicknameField);
        inputPanel.add(joinButton);

        mainPanel.add(inputPanel, BorderLayout.NORTH);

        mensaje = new JTextField();
        JButton sendButton = new JButton("Enviar");
        sendButton.addActionListener(e -> sendMessage(mensaje.getText()));

        chat = new JTextArea();
        chat.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chat);
        mainPanel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.add(mensaje, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);

        mainPanel.add(messagePanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void joinChat(String nickname) throws IOException {
        serverAddress = InetAddress.getByName("localhost");
        serverPort = 12345;
        socket = new DatagramSocket();
        nombreUsuario = nickname;

        String loginMessage = "/login " + nickname;
        sendMessage(loginMessage);

        receiveMessages();
    }

    private void sendMessage(String message) {
        if (!message.isEmpty()) {
            try {
                String formattedMessage;
                if (message.equals("/salir")) {
                    formattedMessage = "/salir " + nombreUsuario;
                } else {
                    formattedMessage = message.startsWith("/") ? message : "/msg " + nombreUsuario + " " + message;
                }
                byte[] buffer = formattedMessage.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
                socket.send(packet);

                if (message.equals("/salir")) {
                    socket.close();
                    System.exit(0); // Cierra el cliente después de enviar el mensaje de salida
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            mensaje.setText(""); // Limpiar el campo de texto después de enviar el mensaje
        }
    }

    private void receiveMessages() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());
                    handleServerMessage(received);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("/loginSuccess")) {
            chat.append("Conectado como " + nombreUsuario + "\n");
        } else if (message.startsWith("/nombreUsado")) {
            JOptionPane.showMessageDialog(this, "El nombre de usuario está en uso. Elige otro.");
        } else if (message.startsWith("/listaUsuarios")) {
            // Actualiza la lista de usuarios conectados si es necesario
        } else {
            chat.append(message + "\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Cliente2("Cliente 2").setVisible(true));
    }
}