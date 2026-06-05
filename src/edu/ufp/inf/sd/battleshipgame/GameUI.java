package edu.ufp.inf.sd.battleshipgame;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import edu.ufp.inf.sd.battleshipgame.pubsub.BattleshipGameConsumer;
import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipGameObserverRI;
import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipGameSubjectRI;
import edu.ufp.inf.sd.battleshipgame.rmi.Coordinate;
import edu.ufp.inf.sd.battleshipgame.rmi.GameAction;
import edu.ufp.inf.sd.battleshipgame.rmi.GamePhase;
import edu.ufp.inf.sd.battleshipgame.rmi.GameState;
import edu.ufp.inf.sd.battleshipgame.rmi.Orientation;
import edu.ufp.inf.sd.battleshipgame.rmi.PlayerState;
import edu.ufp.inf.sd.battleshipgame.rmi.ShipPlacement;
import edu.ufp.inf.sd.battleshipgame.rmi.Shot;

/**
 * Interface gráfica do jogo. Abre depois do login/lobby no terminal.
 * Suporta ambos os modos de atualização: Observer/RMI e Publish-Subscribe/RabbitMQ.
 */
public class GameUI extends JFrame {

    private final BattleshipGameSubjectRI game;
    private final String gameId;
    private final String username;
    private final String token;
    private final boolean usePubSub;

    // Boards reutilizados da implementação original
    private final Board myBoard;      // campo próprio (navios + tiros recebidos)
    private final Board enemyBoard;   // campo adversário (onde disparamos)

    private final JLabel lblStatus;
    private final JLabel lblShipInfo;
    private final JButton btnRotate;
    private final JButton btnPass;
    private final JTextArea txaLog;

    private boolean isVertical = false;
    private volatile GameState lastState;
    private BattleshipGameConsumer consumer;

    public GameUI(BattleshipGameSubjectRI game, String gameId, String username, String token, boolean usePubSub) {
        super("Battleship  —  " + username + "  [" + (usePubSub ? "PubSub/RabbitMQ" : "Observer/RMI") + "]");
        this.game = game;
        this.gameId = gameId;
        this.username = username;
        this.token = token;
        this.usePubSub = usePubSub;

        this.myBoard = new Board();
        this.enemyBoard = new Board();
        this.lblStatus = new JLabel("A ligar ao jogo...");
        this.lblShipInfo = new JLabel(" ");
        this.btnRotate = new JButton("Orientação: Horizontal");
        this.btnPass = new JButton("Passar Vez");
        this.txaLog = new JTextArea(6, 40);

        buildUI();
        hookButtonActions();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                leaveGame();
                dispose();
            }
        });

        attachToGame();
    }

    // -------------------------------------------------------------------------
    // Construção da interface
    // -------------------------------------------------------------------------

    private void buildUI() {
        setLayout(null);
        getContentPane().setBackground(Color.BLACK);
        setSize(940, 750);
        setLocationRelativeTo(null);

        JLabel lblTitle = new JLabel("Battleship  —  " + username + "   |   Jogo: " + gameId);
        lblTitle.setBounds(10, 5, 900, 26);
        lblTitle.setForeground(Color.GREEN);
        lblTitle.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        add(lblTitle);

        lblStatus.setBounds(10, 31, 900, 22);
        lblStatus.setForeground(Color.CYAN);
        lblStatus.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(lblStatus);

        // Etiquetas dos campos
        addLabel("O Teu Campo", 10, 58, Color.GREEN, Font.BOLD, 12);
        addLabel("Campo do Adversário", 480, 58, Color.GREEN, Font.BOLD, 12);

        // Boards (10×10 cada)
        myBoard.drawBoard();
        myBoard.setBounds(10, 80, 430, 355);
        add(myBoard);

        enemyBoard.drawBoard();
        enemyBoard.setBounds(480, 80, 430, 355);
        add(enemyBoard);

        // Legenda de cores
        buildLegend(10, 444);

        // Informação do próximo navio
        lblShipInfo.setBounds(10, 464, 600, 22);
        lblShipInfo.setForeground(Color.YELLOW);
        lblShipInfo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(lblShipInfo);

        // Botões de controlo
        btnRotate.setBounds(10, 490, 215, 28);
        btnRotate.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        btnRotate.setBackground(Color.DARK_GRAY);
        btnRotate.setForeground(Color.GREEN);
        btnRotate.setFocusable(false);
        add(btnRotate);

        btnPass.setBounds(235, 490, 155, 28);
        btnPass.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        btnPass.setBackground(Color.DARK_GRAY);
        btnPass.setForeground(Color.GREEN);
        btnPass.setFocusable(false);
        btnPass.setEnabled(false);
        add(btnPass);

        // Log de eventos
        addLabel("Log de Eventos:", 10, 527, Color.GREEN, Font.PLAIN, 11);

        txaLog.setEditable(false);
        txaLog.setBackground(Color.BLACK);
        txaLog.setForeground(Color.GREEN);
        txaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        JScrollPane scroll = new JScrollPane(txaLog);
        scroll.setBounds(10, 547, 900, 150);
        scroll.getViewport().setBackground(Color.BLACK);
        scroll.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        add(scroll);

        setVisible(true);
    }

    private void buildLegend(int x, int y) {
        String[] labels = {"Navio intacto", "Navio afundado", "Tiro falhado", "Acerto nosso"};
        Color[]  colors  = {Color.GREEN, Color.RED, Color.GRAY, Color.YELLOW};
        for (int i = 0; i < labels.length; i++) {
            JPanel sq = new JPanel();
            sq.setBackground(colors[i]);
            sq.setBounds(x + i * 150, y, 12, 12);
            add(sq);
            JLabel lbl = new JLabel(labels[i]);
            lbl.setForeground(Color.WHITE);
            lbl.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            lbl.setBounds(x + i * 150 + 16, y, 130, 12);
            add(lbl);
        }
    }

    private void addLabel(String text, int x, int y, Color color, int style, int size) {
        JLabel l = new JLabel(text);
        l.setBounds(x, y, 400, 20);
        l.setForeground(color);
        l.setFont(new Font(Font.MONOSPACED, style, size));
        add(l);
    }

    // -------------------------------------------------------------------------
    // Listeners dos botões
    // -------------------------------------------------------------------------

    private void hookButtonActions() {
        // Substitui os listeners originais dos boards pelos nossos
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                final int row = r, col = c;

                for (java.awt.event.ActionListener al : myBoard.btnGrid[r][c].getActionListeners())
                    myBoard.btnGrid[r][c].removeActionListener(al);
                myBoard.btnGrid[r][c].addActionListener(e -> onMyBoardClick(row, col));
                myBoard.btnGrid[r][c].setEnabled(false);

                for (java.awt.event.ActionListener al : enemyBoard.btnGrid[r][c].getActionListeners())
                    enemyBoard.btnGrid[r][c].removeActionListener(al);
                enemyBoard.btnGrid[r][c].addActionListener(e -> onEnemyBoardClick(row, col));
                enemyBoard.btnGrid[r][c].setEnabled(false);
            }
        }

        btnRotate.addActionListener(e -> {
            isVertical = !isVertical;
            btnRotate.setText("Orientação: " + (isVertical ? "Vertical" : "Horizontal"));
            updateShipInfoLabel();
        });

        btnPass.addActionListener(e -> {
            try {
                game.setState(token, GameAction.pass(username));
            } catch (RemoteException ex) {
                showError("Erro ao passar a vez:\n" + ex.getMessage());
            }
        });
    }

    // -------------------------------------------------------------------------
    // Ligação ao jogo (attach)
    // -------------------------------------------------------------------------

    private void attachToGame() {
        try {
            if (usePubSub) {
                boolean rabbitOk = false;
                try {
                    consumer = new BattleshipGameConsumer(gameId, username);
                    consumer.start(state -> SwingUtilities.invokeLater(() -> onStateUpdate(state)));
                    rabbitOk = true;
                    System.out.println("[GameUI] RabbitMQ consumer ativo para " + gameId);
                } catch (Exception ex) {
                    System.err.println("[GameUI] RabbitMQ indisponível: " + ex.getMessage());
                }

                if (rabbitOk) {
                    // RabbitMQ ok — usa NullObserver (updates chegam pelo consumer)
                    game.attach(token, username, new NullObserver());
                    setTitle(getTitle().replace("[PubSub/RabbitMQ]", "[PubSub/RabbitMQ]"));
                } else {
                    // RabbitMQ falhou — fallback automático para Observer/RMI
                    System.out.println("[GameUI] Fallback para Observer/RMI.");
                    game.attach(token, username, new GameObserver());
                    setTitle(getTitle().replace("[PubSub/RabbitMQ]", "[RMI — RabbitMQ indisponível]"));
                    SwingUtilities.invokeLater(() ->
                        lblStatus.setText("RabbitMQ indisponível — a usar Observer/RMI automaticamente."));
                }
            } else {
                game.attach(token, username, new GameObserver());
            }

            // Garante que o estado inicial é sempre carregado via RMI.
            // No modo PubSub o estado chega de forma assíncrona — sem isto
            // lastState ficaria null e nenhum botão seria ativado.
            GameState estadoInicial = game.getState(token);
            SwingUtilities.invokeLater(() -> onStateUpdate(estadoInicial));

        } catch (Exception ex) {
            showError("Erro ao entrar no jogo:\n" + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Atualização de estado (chamado no EDT)
    // -------------------------------------------------------------------------

    public void onStateUpdate(GameState state) {
        this.lastState = state;

        GamePhase phase = state.getPhase();
        boolean myTurn = username.equals(state.getCurrentTurn());

        PlayerState myState = state.getPlayerStates().get(username);
        String opponentName = getOpponent(state);
        PlayerState oppState = opponentName != null ? state.getPlayerStates().get(opponentName) : null;

        // Status
        lblStatus.setText(buildStatusText(state, myTurn));

        // Info do próximo navio
        updateShipInfoLabel(myState, phase, myTurn);

        // Atualizar visualmente os boards
        if (myState != null) refreshMyBoard(myState);
        if (oppState != null) refreshEnemyBoard(oppState);

        // Ativar/desativar botões
        updateButtonStates(phase, myState, oppState, myTurn);

        // Atualizar log
        refreshLog(state.getEventLog());

        // Diálogo de fim de jogo
        if (phase == GamePhase.FINISHED) {
            String winner = state.getWinner();
            String msg = username.equals(winner)
                    ? "Parabéns, GANHASTE!"
                    : "O jogo terminou.\nVencedor: " + winner;
            JOptionPane.showMessageDialog(this, msg, "Fim do Jogo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private String buildStatusText(GameState state, boolean myTurn) {
        return switch (state.getPhase()) {
            case WAITING_FOR_PLAYERS -> "Fase: A aguardar segundo jogador...";
            case PLACING_SHIPS -> "Fase: Colocar Navios  |  "
                    + (myTurn ? "A TUA VEZ de colocar navios!" : "Aguarda: adversário a colocar navios...");
            case IN_PROGRESS -> "Fase: Em Jogo  |  "
                    + (myTurn ? ">>> A TUA VEZ DE DISPARAR! <<<" : "Vez do adversário (" + state.getCurrentTurn() + ")");
            case FINISHED -> {
                String w = state.getWinner();
                yield "Jogo Terminado!  |  " + (username.equals(w) ? "GANHASTE!" : "Perdeste. Vencedor: " + w);
            }
        };
    }

    private void updateShipInfoLabel() {
        if (lastState != null) {
            PlayerState ms = lastState.getPlayerStates().get(username);
            updateShipInfoLabel(ms, lastState.getPhase(), username.equals(lastState.getCurrentTurn()));
        }
    }

    private void updateShipInfoLabel(PlayerState myState, GamePhase phase, boolean myTurn) {
        if (phase == GamePhase.PLACING_SHIPS && myTurn && myState != null && !myState.allShipsPlaced()) {
            lblShipInfo.setText("Próximo navio: " + myState.nextShipLength()
                    + " células  |  Orientação: " + (isVertical ? "Vertical" : "Horizontal")
                    + "  |  Restam: " + myState.shipsRemainingToPlace());
        } else {
            lblShipInfo.setText(" ");
        }
    }

    private void refreshMyBoard(PlayerState myState) {
        boolean[][] ships = myState.getShips();
        boolean[][] hits  = myState.getHitsReceived();
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                JButton btn = myBoard.btnGrid[r][c];
                if (hits[r][c] && ships[r][c]) {
                    colorButton(btn, Color.RED);
                } else if (hits[r][c]) {
                    colorButton(btn, Color.GRAY);
                } else if (ships[r][c]) {
                    colorButton(btn, Color.GREEN);
                }
            }
        }
    }

    private void refreshEnemyBoard(PlayerState oppState) {
        boolean[][] oppHits  = oppState.getHitsReceived();
        boolean[][] oppShips = oppState.getShips();
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                if (!oppHits[r][c]) continue;
                colorButton(enemyBoard.btnGrid[r][c], oppShips[r][c] ? Color.YELLOW : Color.GRAY);
            }
        }
    }

    private void colorButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
    }

    private void updateButtonStates(GamePhase phase, PlayerState myState, PlayerState oppState, boolean myTurn) {
        boolean placing = phase == GamePhase.PLACING_SHIPS && myTurn;
        boolean firing  = phase == GamePhase.IN_PROGRESS  && myTurn;

        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                boolean cellOccupied = myState != null && myState.getShips()[r][c];
                boolean cellHit      = myState != null && myState.getHitsReceived()[r][c];
                myBoard.btnGrid[r][c].setEnabled(placing && !cellOccupied && !cellHit);

                boolean alreadyShot = oppState != null && oppState.getHitsReceived()[r][c];
                enemyBoard.btnGrid[r][c].setEnabled(firing && !alreadyShot);
            }
        }

        btnPass.setEnabled(firing);
        btnRotate.setEnabled(placing);
    }

    private void refreshLog(List<String> log) {
        int from = Math.max(0, log.size() - 8);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < log.size(); i++) {
            sb.append(log.get(i)).append("\n");
        }
        txaLog.setText(sb.toString());
        txaLog.setCaretPosition(txaLog.getDocument().getLength());
    }

    // -------------------------------------------------------------------------
    // Cliques nos boards
    // -------------------------------------------------------------------------

    private void onMyBoardClick(int row, int col) {
        if (lastState == null || lastState.getPhase() != GamePhase.PLACING_SHIPS) return;
        if (!username.equals(lastState.getCurrentTurn())) return;

        PlayerState myState = lastState.getPlayerStates().get(username);
        if (myState == null || myState.allShipsPlaced()) return;

        int len = myState.nextShipLength();
        Orientation orientation = isVertical ? Orientation.VERTICAL : Orientation.HORIZONTAL;
        ShipPlacement placement = new ShipPlacement(new Coordinate(row, col), len, orientation);
        try {
            game.setState(token, GameAction.placeShip(username, placement));
        } catch (RemoteException ex) {
            showError("Não foi possível colocar navio:\n" + ex.getMessage());
        }
    }

    private void onEnemyBoardClick(int row, int col) {
        if (lastState == null || lastState.getPhase() != GamePhase.IN_PROGRESS) return;
        if (!username.equals(lastState.getCurrentTurn())) return;
        try {
            game.setState(token, GameAction.fire(username, new Shot(new Coordinate(row, col))));
        } catch (RemoteException ex) {
            showError("Não foi possível disparar:\n" + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    private void leaveGame() {
        try { game.detach(token, username); } catch (Exception ignored) {}
        if (consumer != null) { try { consumer.close(); } catch (Exception ignored) {} }
    }

    private String getOpponent(GameState state) {
        for (String p : state.getPlayers()) {
            if (!p.equals(username)) return p;
        }
        return null;
    }

    private void showError(String msg) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE));
    }

    // -------------------------------------------------------------------------
    // Observers internos
    // -------------------------------------------------------------------------

    /** Observer RMI: recebe update() e repassa para o EDT. */
    private class GameObserver extends UnicastRemoteObject implements BattleshipGameObserverRI {
        protected GameObserver() throws RemoteException { super(); }

        @Override
        public void update(GameState state) throws RemoteException {
            SwingUtilities.invokeLater(() -> onStateUpdate(state));
        }
    }

    /** Observer nulo: usado no modo PubSub (attach é obrigatório mas callbacks são via RabbitMQ). */
    private static class NullObserver extends UnicastRemoteObject implements BattleshipGameObserverRI {
        protected NullObserver() throws RemoteException { super(); }

        @Override
        public void update(GameState state) throws RemoteException {}
    }
}
