import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private final Game game;
    private BufferedReader in;
    private PrintWriter out;
    private String clientName;

    public ClientHandler(Socket socket, Server server, Game game) {
        this.socket = socket;
        this.server = server;
        this.game = game;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // 클라이언트 입력 스트림
            out = new PrintWriter(socket.getOutputStream(), true); // 클라이언트 출력 스트림
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendEmojiToClient(String emojiPath, String clientName) {
        sendMessage("EMOJI|" + emojiPath + "|" + clientName); // "EMOJI|경로" 형식으로 클라이언트로 전송
    }

    public void sendSubmittedCardToClient(Card playerSubmittedCard, String clientName) {
        sendMessage("SUBMITTED_CARD|" + playerSubmittedCard.getRank() + "|" + playerSubmittedCard.getSuit() + "|" + clientName); //   

    }

    @Override
    public void run() {
        try {
            // 클라이언트의 이름을 수신
            clientName = in.readLine();
            System.out.println("클라이언트 연결: " + clientName);

            // 클라이언트로부터 메시지를 계속 수신
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("클라이언트 메시지: " + message);

                // 메시지에 따라 처리
                if (message.startsWith("EMOJI|")) {
                    // 이모티콘 처리
                    String[] parts = message.split("\\|");
                    String emojiPath = parts[1];
                    String clientName = parts[2];
                    server.broadcastEmoji(emojiPath, clientName); // 다른 클라이언트로 이모티콘 브로드캐스트
                } else if (message.startsWith("SUBMITTED_CARD|")) {
                    handleCardSumission(message);
                } else if (message.startsWith("TOP_SUBMITTED_CARD")){
                    Card topCard = server.getTopSubmittedCard();
                    if (topCard != null) {
                        sendMessage("TOP_SUBMITTED_CARD|" + topCard.getRank() + "|" + topCard.getSuit());
                        System.out.println("클라이언트핸들러 sendMessage: TOP_SUBMITTED_CARD|" + topCard.getRank() + "|" + topCard.getSuit());
                    } else {
                        sendMessage("ERROR|No top card available");
                    }
                } else {
                    System.out.println("알 수 없는 메시지: " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 클라이언트 연결 종료
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            server.removeClient(this); // 서버에서 클라이언트 제거
        }
    }

    private void handleCardSumission(String message){
        try{
            String[] parts = message.split("\\|");
            String rank = parts[1];
            String suit = parts[2];
            String clientName = parts[3];
            Card card = new Card(rank, suit);

            boolean isGameOver = server.handleCardSubmission(clientName, card);
            if(isGameOver){
                server.broadcastMessage(clientName + "(이)가 승리했습니다.");
            }
            else{
                server.broadcastGameState();
            }
        } catch (Exception e){
            sendMessage("ERROR: 카드 제출 중 문제가 발생했습니다." + e.getMessage());
        }

    }

    public String serializeHand() {
        return game.serializeHand(clientName); // 게임에서 손패 직렬화
    }

    public String getClientName() {
        return clientName;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

}