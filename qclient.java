import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

class qclient {
    private Socket clientSocket;
    private DataOutputStream output;
    private DataInputStream input;
    private Scanner scannerInput;

    public static void main(String[] args) {
        qclient client = new qclient();
        client.scannerInput = new Scanner(System.in);
        client.startConnection("127.0.0.1", 6666); // start connection when client want to send a request
        while (true) {
            System.out.print("> ");
            String userInput = client.scannerInput.nextLine();
            char operation = userInput.charAt(0);
            if (userInput.split("\\s+")[0].length() > 1) {
                System.err.println("Invalid message format try again, refer to the help menu (> h)");
                continue;
            }
            try {
                switch (operation) {
                    case 'p':
                        client.addQuestion();
                        break;
                    case 'd':
                        client.deleteQuestion(userInput);
                        break;
                    case 'g':
                        client.getQuestion(userInput);
                        break;
                    case 'r':
                        client.getRandomQuestion(userInput);
                        continue;
                    case 'c':
                        client.checkAnswer(userInput);
                        break;
                    case 'k':
                        client.killServer();
                        break;
                    case 'q':
                        //closing connection
                        client.stopConnection();
                        System.exit(0);
                        break;
                    case 'h':
                        client.displayHelp(userInput);
                        break;
                    default:
                        System.err.println("Invalid message format try again, refer to the help menu (> h)");
                        continue;
                }
                // read response
                System.out.println(client.getResponse());
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private void displayHelp(String userInput) {
        sendRequest(userInput);
    }

    private void addQuestion() {
        String qTag = scannerInput.nextLine();
        StringBuilder question = new StringBuilder("p\n");
        question.append(qTag).append('\n');
        String prevLine = "";
        while (scannerInput.hasNext()) {
            String newLine = scannerInput.nextLine();
            if (prevLine.equals(".") && prevLine.equals(newLine)) {  //end of question info
                question.append(scannerInput.nextLine());      // correct choice
                break;
            }
            if (!newLine.equals(".")) question.append(newLine).append('\n');
            prevLine = newLine;
        }

        sendRequest(question.toString());
    }

    private void killServer() {
        sendRequest("k"); // kill the server
    }

    private void checkAnswer(String userInput) {
        sendRequest(userInput);
    }

    private void getRandomQuestion(String userInput) {
        sendRequest(userInput);
        String response = getResponse();
        System.out.println(response);
        int questionNumber = Integer.parseInt(response.substring(0, response.indexOf('\n')));  // first line must be a number
        char userChoice = scannerInput.nextLine().charAt(0);               // read user choice
        sendRequest("c " + questionNumber + " " + userChoice);  // check user's answer
        System.out.println(getResponse());
    }

    private void getQuestion(String userInput) {
        sendRequest(userInput);
    }

    private void deleteQuestion(String userInput) {
        sendRequest(userInput);
    }

    private void startConnection(String ip, int port) {
        // new socket has to be created since API does not have option to reopen the socket once closed
        try {
            clientSocket = new Socket(ip, port); // if server has accepted the connection a new object is created
            output = new DataOutputStream(clientSocket.getOutputStream()); // write to server
            input = new DataInputStream(clientSocket.getInputStream()); // read response from server
        } catch (IOException e) {
            System.err.println("Server is down :(");
            System.exit(0);
        }
    }

    private void sendRequest(String request) {

        try {
            output.writeUTF(request);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private String getResponse() {
        String response = "";
        try {
            response = input.readUTF();
        } catch (IOException ioe) {
            System.err.println("Server is down :(");
        }
        return response;
    }

    private void stopConnection() {
        try {
            input.close();
            output.close();
            clientSocket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
