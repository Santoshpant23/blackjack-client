package client;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class BlackjackClient {
    private static final String BASE_URL = "http://euclid.knox.edu:8080/api/blackjack";
    private static final String USERNAME = "spant"; // replace with your username
    private static final String PASSWORD = "2ec6db"; // replace with your from the file posted to Classroom

    public static void main(String[] args) throws Exception {
        ClientConnecter clientConnecter = new ClientConnecter(BASE_URL, USERNAME, PASSWORD);

        Scanner input = new Scanner(System.in);

        System.out.println("Welcome to the Blackjack game!");
        System.out.println("Do you want to start a new session or connect to an old session?");

        // List sessions
        System.out.println("Available sessions:");
        List<SessionSummary> sessions = clientConnecter.listSessions();
        int sessionNum = 1;
        for (SessionSummary session : sessions) {
            System.out.println("session number: " + sessionNum + " with Session ID: " + session.sessionId
                    + ", Balance: " + session.balance);
            sessionNum++;
        }
        System.out.println("Enter session ID to connect to an old session or 'new' for a new session:");
        String sessionIdInput = input.nextLine().trim();
        UUID sessionId = null;
        GameState state = null;

        if (sessionIdInput.equalsIgnoreCase("new")) {
            // Start a new session
            System.out.println("A new session! Great idea.");
        } else if (sessionIdInput.matches("\\d+")) {
            // If the input is a number, treat it as an index
            int sessionIndex = Integer.parseInt(sessionIdInput) - 1;
            if (sessionIndex >= 0 && sessionIndex < sessions.size()) {
                sessionId = sessions.get(sessionIndex).sessionId;
                System.out.println("Connecting to session ID: " + sessionId);
            } else {
                System.out.println("Invalid session number. Starting a new session.");
            }
        } else {
            try {
                System.out.println(sessionIdInput);
                sessionId = UUID.fromString(sessionIdInput);
                System.out.println("Connecting to session ID: " + sessionId);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid session ID. Starting a new session.");
            }
        }
        if (sessionId == null) {
            // Start a new session
            System.out.println("Starting a new session...");
            state = clientConnecter.startGame();
        } else {
            // Connect to an existing session
            System.out.println("Connecting to session ID: " + sessionId);
            state = clientConnecter.resumeSession(sessionId);

        }

        while (true) {
            System.out.println("\nYour balance: " + state.balance + " units");
            System.out.println("Cards remaining: " + state.cardsRemaining);

            System.out.print("Enter bet (must be multiple of 10): ");
            int bet = Integer.parseInt(input.nextLine());
            state = clientConnecter.placeBet(state.sessionId, bet);
            printState(state);

            boolean hasReshuffled = false;
            // Player turn loop
            while (!state.gameOver && state.canHit) {
                System.out.print("Action hit(h) / stand(s): ");
                String action = input.nextLine().trim().toLowerCase();

                if (action.equals("hit") || action.equals("h")) {
                    state = clientConnecter.hit(state.sessionId);
                    if (state.reshuffled) {
                        hasReshuffled = true;
                    }
                } else if (action.equals("stand") || action.equals("s")) {
                    state = clientConnecter.stand(state.sessionId);
                    if (state.reshuffled) {
                        hasReshuffled = true;
                    }
                } else {
                    System.out.println("Invalid action.");
                    continue;
                }

                printState(state);
                if (hasReshuffled) {
                    System.out.println("Cards reshuffled!");
                    hasReshuffled = false;
                } else {
                    System.out.println("Cards not reshuffled.");
                }
            }
            System.out.println("Cards remaining: " + state.cardsRemaining);
            System.out.println("==> Outcome: " + state.outcome);
            System.out.println("Balance: " + state.balance + " units");

            System.out.print("\nPlay again? yes(y) / no(n): ");
            String playAgain = input.nextLine().trim().toLowerCase();
            if (!playAgain.equals("yes") && !playAgain.equals("y")) {
                break;
            }
            state = clientConnecter.newGame(state.sessionId);
        }

        System.out.println("Thanks for playing!");
        input.close();
        clientConnecter.finishGame(state.sessionId);

    }

    private static void printState(GameState state) {
        // this is a stupid AI generated method
        try {
            System.out.println(
                    "Your cards: " + String.join(", ", state.playerCards) + " (value: " + state.playerValue + ")");
            if (state.dealerValue != null) {
                System.out.println("Dealer cards: " + String.join(", ", state.dealerCards) + " (value: "
                        + state.dealerValue + ")");
            } else {
                System.out.println("Dealer shows: " + state.dealerCards.get(0));
            }
        } catch (RuntimeException e) {
            System.out.println(state);
            throw e;
        }
    }
}
