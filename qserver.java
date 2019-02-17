import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * qserver side
 * pscp -r.\P1\ nirajbha@storm.cise.ufl.edu:/cise/homes/nirajbha/Network/P1
 * java -cp json.jar: qserver
 *
 * @author : Niraj Patel
 */

class qserver {
    private final static String FILE_PATH = "./assets/help.txt";
    private static String helpMenu;
    private static DB database;
    private int threadsCount;

    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(FILE_PATH)));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            helpMenu = sb.toString();
            reader.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        database = new DB();
        qserver server = new qserver();
        server.start(6666);
    }

    private void start(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Host Name: " + serverSocket.getInetAddress().getHostName());
            while (true) {
                /// just waits, listening to the socket for client to make connection request,
                // gets new socket upon connection that is connected to the client
                Socket clientSocket = serverSocket.accept();
                // a new thread is created for each client, which is responsible for handling
                // the communication between the server and the client process
                new ClientHandler(clientSocket, threadsCount).start();
                System.out.println("Established connection with client#" + threadsCount);
                threadsCount++;
            }
        } catch (IOException e) {
            System.err.println("Unable to start the server");
        }
    }

    private class ClientHandler extends Thread {
        private final Socket clientSocket;
        private final int instanceId;
        private DataOutputStream output;
        private DataInputStream input;

        private ClientHandler(Socket clientSocket, int threadCount) {
            this.clientSocket = clientSocket;
            this.instanceId = threadCount;
        }


        private void addQuestion(String[] params) {
            String qNumber = database.addQuestion(params);
            sendResponse(qNumber);

        }

        private String[] parseQuestion(String message) {
            String[] params = new String[3];
            int newLineIndex = message.indexOf('\n');
            params[0] = message.substring(0, newLineIndex);
            params[1] = message.substring(newLineIndex + 1, message.length() - 1);
            params[2] = message.substring(message.length() - 1);
            return params;
        }


        private void checkAnswer(String[] params) {
            String choice = params[2];
            String answer = database.getAnswer(params[1]);  // send to db
            if (answer.length() > 1) { // question is not in the back
                sendResponse(answer);
            } else {
                sendResponse(answer.equalsIgnoreCase(choice) ? "Correct" : "Incorrect");    // send response if successful
            }

        }

        private void getRandomQuestion(String[] params) {
            if (params.length > 1) {
                sendResponse("Invalid input try again!");
                return;
            }
            sendResponse(database.getRandomQuestion());
        }

        private void getQuestion(String[] params) {
            try {
                String question = database.getQuestion(params[1]);
                sendResponse(question);
            } catch (Exception e) {
                sendResponse("Invalid input try again!");
            }
        }

        private void deleteQuestion(String[] params) {
            try {
                String response = database.deleteQuestion(params[1]); // send to db
                sendResponse(response);  // send response if successful
            } catch (Exception e) {
                System.err.println(e.getMessage());
                sendResponse("Something went wrong please try again!");
            }
        }

        private void sendResponse(String response) {
            try {
                output.writeUTF(response);
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

        private void stopServerThread() {
            try {
                input.close();
                output.close();
                clientSocket.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

        private void shutDown() {
            sendResponse("Goodbye");
            stopServerThread();
            System.exit(0);   // kill the process, shutting down the server completely
        }

        public void run() {
            try {
                output = new DataOutputStream(clientSocket.getOutputStream()); // write to the client socket
                input = new DataInputStream(clientSocket.getInputStream());   // read from client
                String message;
                while ((message = input.readUTF()) != null) {
                    char operation = message.charAt(0);
                    String[] params = operation == 'p' ? parseQuestion(message.substring(2)) : message.split("\\s+");

                    switch (operation) {
                        case 'p':
                            addQuestion(params);
                            break;
                        case 'd':
                            deleteQuestion(params);
                            break;
                        case 'g':
                            getQuestion(params);
                            break;
                        case 'r':
                            getRandomQuestion(params);
                            break;
                        case 'c':
                            checkAnswer(params);
                            break;
                        case 'k':
                            shutDown();
                            break;
                        case 'h':
                            sendResponse(helpMenu);
                            break;
                        default:
                            sendResponse("Invalid message format try again!");
                    }
                }
            } catch (IOException e) {
                sendResponse("Something went wrong...please try again");
            } finally {
                System.out.println("Closed connection with client#:" + instanceId);
            }
        }
    }
}