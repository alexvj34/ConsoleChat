import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ChatServer {
    private static final int PORT = 12346;
    private static Map<String, User> users = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер запущен. Ожидание подключения...");


            new Thread(ChatServer::handleServerCommands).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Клиент подключен: " + clientSocket.getInetAddress());
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void handleServerCommands() {
        BufferedReader serverInput = new BufferedReader(new InputStreamReader(System.in));
        String command;

        try {
            while ((command = serverInput.readLine()) != null) {
                if (command.startsWith("/setadmin ")) {
                    String username = command.split(" ")[1];
                    User user = users.get(username);
                    if (user != null) {
                        user.setRole("ADMIN");
                        user.sendMessage("Вы получили права администратора.");
                        System.out.println("Пользователь " + username + " назначен администратором.");
                    } else {
                        System.out.println("Пользователь " + username + " не найден.");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private User user;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);


                out.println("Введите ваше имя:");
                username = in.readLine();

                user = new User(username, "USER", out);
                users.put(username, user);

                broadcastMessage(username + " присоединился к чату.");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/w ")) {
                        String[] parts = message.split(" ", 3);
                        if (parts.length == 3) {
                            sendPrivateMessage(parts[1], parts[2]);
                        }
                    } else if (message.startsWith("/kick ") && user.getRole().equals("ADMIN")) {
                        String[] parts = message.split(" ", 2);
                        if (parts.length == 2) {
                            kickUser(parts[1]);
                        }
                    } else {
                        broadcastMessage(username + ": " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (username != null) {
                        users.remove(username);
                        broadcastMessage(username + " покинул чат.");
                    }
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        private void sendPrivateMessage(String recipient, String message) {
            User recipientUser = users.get(recipient);
            if (recipientUser != null) {
                recipientUser.sendMessage(username + " (лично): " + message);
                out.println("Личное сообщение отправлено " + recipient + ": " + message);
            } else {
                out.println("Пользователь " + recipient + " не найден.");
            }
        }


        private void kickUser(String username) {
            User userToKick = users.get(username);
            if (userToKick != null) {
                userToKick.sendMessage("Вы были кикнуты администратором.");
                users.remove(username);
                broadcastMessage(username + " был кикнут из чата.");
                try {
                    userToKick.getOut().close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                out.println("Пользователь " + username + " не найден.");
            }
        }


        private void broadcastMessage(String message) {
            for (User user : users.values()) {
                user.sendMessage(message);
            }
        }
    }
}
