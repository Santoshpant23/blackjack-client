package client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

//TODO:
// check buttons, reshuffle
public class BlackjackGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    private JButton hitButton;
    private JButton standButton;
    private JLabel scoreLabel;

    private static final String BASE_URL = "http://euclid.knox.edu:8080/api/blackjack";
    private static final String USERNAME = "spant";
    private static final String PASSWORD = "2ec6db";

    private ClientConnecter clientConnecter;
    private CardPanel cardPanel;
    private Map<Card, ImageIcon> cardImages;
    private UUID sessionId;
    private GameState currState;

    public BlackjackGUI() {
        setTitle("Blackjack Game");
        setSize(1000, 800);
        loadCards();

        hitButton = new JButton("Hit");
        standButton = new JButton("Stand");
        hitButton.setEnabled(false);
        standButton.setEnabled(false);

        cardPanel = new CardPanel(hitButton, standButton, cardImages);
        cardPanel.setLayout(null); // absolute positioning
        setContentPane(cardPanel);

        addScoreLabel(); // stats bar on top‑right

        // classic anonymous listener (no lambdas)
        hitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleHit();
            }
        });
        standButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleStand();
            }
        });

        clientConnecter = new ClientConnecter(BASE_URL, USERNAME, PASSWORD);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        sessionId = null;
        currState = null;

        addMenuBar();
        addKeyboardShortcuts();
    }

    /* ------------------------------------------------------------ */
    /* UI helpers */
    /* ------------------------------------------------------------ */
    private void addScoreLabel() {
        scoreLabel = new JLabel("Player: 0 | Dealer: ? | Balance: 0 | Cards left: 0");
        scoreLabel.setBounds(600, 10, 380, 20);
        cardPanel.add(scoreLabel);
    }

    private void resetScoreLabel() {
        scoreLabel.setText("Player: 0 | Dealer: ? | Balance: 0 | Cards left: 0");
    }

    private void updateScoreLabel() {
        if (scoreLabel == null || currState == null)
            return;
        int playerVal = currState.playerValue;
        String dealerValue = (currState.dealerValue != null) ? currState.dealerValue.toString() : "?";
        String balance = String.valueOf(currState.balance);
        String remaining = String.valueOf(currState.cardsRemaining);
        scoreLabel.setText("Player: " + playerVal +
                " | Dealer: " + dealerValue +
                " | Balance: " + balance +
                " | Cards left: " + remaining);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /* ------------------------------------------------------------ */
    /* Menu & keyboard */
    /* ------------------------------------------------------------ */
    private void addKeyboardShortcuts() {
        cardPanel.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                char ch = e.getKeyChar();
                if (ch == 'h')
                    handleHit();
                else if (ch == 's')
                    handleStand();
                else if (ch == 'n')
                    startNewGame();
                else if (ch == 'r')
                    reconnectSession();
                else if (ch == 'b')
                    askForBet();
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }
        });
        cardPanel.setFocusable(true);
        cardPanel.requestFocusInWindow();
    }

    private void addMenuBar() {
        JMenuBar bar = new JMenuBar();
        setJMenuBar(bar);
        JMenu file = new JMenu("File");
        bar.add(file);

        JMenuItem newGame = new JMenuItem("New Game");
        newGame.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startNewGame();
            }
        });
        file.add(newGame);

        JMenuItem reconnect = new JMenuItem("Reconnect");
        reconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reconnectSession();
            }
        });
        file.add(reconnect);
    }

    /* ------------------------------------------------------------ */
    /* Game flow */
    /* ------------------------------------------------------------ */
    private void startNewGame() {
        try {
            clearState(); // Clear any existing state first
            currState = clientConnecter.startGame();
            if (currState != null) {
                sessionId = currState.sessionId;
                cardPanel.clearCards();
                askForBet();
            } else {
                showError("Failed to initialize game state");
            }
        } catch (Exception e) {
            showError("Error starting new game: " + e.getMessage());
        }
    }

    private void reconnectSession() {
        try {
            List<SessionSummary> sessions = clientConnecter.listSessions();
            if (sessions.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No saved sessions found");
                return;
            }

            DefaultListModel<String> model = new DefaultListModel<>();
            Map<String, UUID> map = new HashMap<>();
            for (SessionSummary s : sessions) {
                String label = s.sessionId.toString().substring(0, 8) + "… | Balance: " + s.balance;
                model.addElement(label);
                map.put(label, s.sessionId);
            }

            final JList<String> list = new JList<>(model);
            JScrollPane pane = new JScrollPane(list);
            final JDialog dlg = new JDialog(this, "Select Session(Double Click)", true);
            dlg.setSize(300, 200);
            dlg.add(pane);
            list.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        String sel = list.getSelectedValue();
                        UUID id = map.get(sel);
                        dlg.dispose();
                        try {
                            currState = clientConnecter.resumeSession(id);
                            sessionId = currState.sessionId;
                            cardPanel.clearCards();
                            askForBet();
                        } catch (Exception ex) {
                            showError("Resume error: " + ex.getMessage());
                        }
                    }
                }
            });
            dlg.setLocationRelativeTo(this);
            dlg.setVisible(true);
        } catch (Exception e) {
            showError("Failed to load sessions: " + e.getMessage());
        }
    }

    private void askForBet() {
        while (true) {
            String in = JOptionPane.showInputDialog(this, "Enter bet (multiple of 10 and <=1000):");
            if (in == null) {
                clearState();
                return;
            }
            try {
                int bet = Integer.parseInt(in);
                if (bet <= 0 || bet % 10 != 0)
                    throw new NumberFormatException();

                // Make sure we have a valid session before placing bet
                System.out.println(
                        "Session Id is: " + sessionId + " and let's see if currState is null " + (currState == null));
                if (sessionId == null || currState == null) {
                    showError("Invalid session state. Starting a new game.");
                    startNewGame();
                    return;
                }

                currState = clientConnecter.placeBet(sessionId, bet);

                // Verify we got a valid state back
                if (currState == null || currState.playerCards == null || currState.dealerCards == null) {
                    showError("Received invalid game state. Starting a new game.");
                    startNewGame();
                    return;
                }

                displayInitialCards();
                updateButtonStates();
                updateScoreLabel();
                break;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid bet. Enter a positive multiple of 10.");
            } catch (Exception ex) {
                showError("Error placing bet: " + ex.getMessage());
                // Try to recover by starting a new game
                startNewGame();
                return;
            }
        }
    }

    private void displayInitialCards() {
        cardPanel.clearCards();

        // Safety check to ensure we have valid card collections
        if (currState == null || currState.playerCards == null || currState.dealerCards == null ||
                currState.dealerCards.isEmpty()) {
            showError("Invalid card state detected.");
            return;
        }

        for (String p : currState.playerCards)
            cardPanel.addPlayerCard(getCard(p));
        cardPanel.addDealerCard(getCard(currState.dealerCards.get(0))); // show only first dealer card
        repaint();
    }

    private void handleHit() {
        try {
            currState = clientConnecter.hit(sessionId);
            displayInitialCards();
            updateScoreLabel();
            if (!currState.canHit || currState.gameOver)
                finishRound();
        } catch (Exception e) {
            showError("Hit failed: " + e.getMessage());
        }
    }

    private void handleStand() {
        try {
            currState = clientConnecter.stand(sessionId);
            cardPanel.clearCards();
            for (String p : currState.playerCards)
                cardPanel.addPlayerCard(getCard(p));
            for (String d : currState.dealerCards)
                cardPanel.addDealerCard(getCard(d));
            repaint();
            updateScoreLabel();
            finishRound();
        } catch (Exception e) {
            showError("Stand failed: " + e.getMessage());
        }
    }

    private void finishRound() {
        updateButtonStates();
        JOptionPane.showMessageDialog(this,
                "Cards remaining: " + currState.cardsRemaining + " Outcome: " + currState.outcome + " Balance: "
                        + currState.balance + " units",
                " Game Over", JOptionPane.INFORMATION_MESSAGE);

        int again = JOptionPane.showConfirmDialog(this, "Play again?", "New Round", JOptionPane.YES_NO_OPTION);
        if (again == JOptionPane.YES_OPTION) {
            try {
                clearCards();
                // Make sure we keep our session ID
                // Save the current state before starting a new round
                clientConnecter.finishGame(sessionId);
                // Start a new round with the same session
                System.out.println("Game is saved now trying to call the same game again down");
                currState = clientConnecter.resumeSession(currState.sessionId);
                // Make sure we still have the correct session ID
                System.out.println("New game was called successfully");
                sessionId = currState.sessionId;
                // Check if we have a valid state before proceeding
                // System.out.println(currState);
                // System.out.println(currState.playerValue);
                System.out.println(sessionId);
                if (currState != null) {
                    askForBet();
                } else {
                    showError("Invalid game state returned. Starting a new game instead.");
                    startNewGame();
                }
            } catch (Exception e) {
                showError("Failed to start new round: " + e.getMessage());
                // Try to recover by starting a new game
                startNewGame();
            }
        } else {
            try {
                // Make sure to save the game before clearing everything
                clientConnecter.finishGame(sessionId);
                resetScoreLabel();
                clearState();
                repaint();
            } catch (Exception e) {
                showError("Cannot Save: " + e.getMessage());
                // Even if saving fails, we should still clear the UI
                resetScoreLabel();
                clearState();
                repaint();
            }
        }
    }

    private void updateButtonStates() {
        boolean active = sessionId != null && currState != null && !currState.gameOver;
        boolean canHit = active && currState != null && currState.canHit;
        hitButton.setEnabled(canHit);
        standButton.setEnabled(active && currState != null && currState.canStand);
    }

    private void clearState() {
        sessionId = null;
        currState = null;
        cardPanel.clearCards();
        updateScoreLabel();
        updateButtonStates();
    }

    /* ------------------------------------------------------------ */
    /* Utility */
    /* ------------------------------------------------------------ */
    private Card getCard(String name) {
        return Card.valueOf(name.toUpperCase().replace(' ', '_'));
    }

    private void loadCards() {
        cardImages = new HashMap<Card, ImageIcon>();
        for (Card c : Card.values()) {
            cardImages.put(c, new ImageIcon(getClass().getResource("/assets/" + c.getFilename())));
        }
    }

    public void clearCards() {
        cardPanel.clearCards();
        updateScoreLabel();
    }

    /* ------------------------------------------------------------ */
    /* Main */
    /* ------------------------------------------------------------ */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                BlackjackGUI gui = new BlackjackGUI();
                gui.setVisible(true);
            }
        });
    }
}