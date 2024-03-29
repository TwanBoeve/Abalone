package client;

import exceptions.ExitProgram;
import exceptions.OffBoardException;
import exceptions.ProtocolException;
import exceptions.ServerUnavailableException;
import game.ClientPlayer;
import game.Game;
import game.Marble;
import game.Player;
import game.ServerGame;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import protocol.ClientProtocol;
import protocol.ProtocolMessages;
import server.Server;

public class Client implements ClientProtocol {

    public Socket serverSocket;
    private Server server;
    protected BufferedWriter out;
    protected String name;
    public boolean connected = false;
    private Game currentGame = null;

    protected ServerListener sl;
    public ClientTui tui;

    public Client() throws IOException {
        tui = new ClientTui(this);
    }

    /**
     * Starts a connection and TUI.
     * 
     * @throws ExitProgram if user wants to exit
     * @throws IOException if createConnection() throws this
     */
    public void start() throws ExitProgram, IOException {
        try {
            Thread t;
            tui.showMessage("Starting Abalone client...");
            while (true) {
                t = null;
                createConnection();
                tui.handleUserInput(ProtocolMessages.CONNECT + ProtocolMessages.DELIMITER + name
                        + ProtocolMessages.DELIMITER + ProtocolMessages.DELIMITER);

                t = new Thread(sl);
                t.start();
                synchronized (serverSocket) {
                    serverSocket.wait();
                }
                if (connected) {
                    tui.showMessage("Successfully connected!");
                    break;
                }
                tui.showMessage("Failed to connect, please try again\n");
            }
            tui.start();
        } catch (ExitProgram e) {
            tui.showMessage("Disconnected.");
            return;
        } catch (ServerUnavailableException e) {
            // do nothing
        } catch (InterruptedException e) {
            // do nothing
        } catch (ProtocolException e) {
            // do nothing
        }
        if (tui.getBoolean("ERROR: server connection broke. Try again? (y/n)")) {
            // enter while loop again
        } else {
            throw new ExitProgram("User indicated to exit.");
        }
    }

    /**
     * Creates a connection to the server. Requests the IP and port to connect to at
     * the view (TUI). The method continues to ask for an IP and port and attempts
     * to connect until a connection is established or until the user indicates to
     * exit the program.
     * 
     * @throws ExitProgram if a connection is not established and the user indicates
     *                     to want to exit the program.
     * @throws IOException if clearConnection() throws this
     * @ensures serverSock contains a valid socket connection to a server
     */
    public void createConnection() throws ExitProgram, IOException {
        clearConnection();
        while (serverSocket == null) {
            name = tui.getString("What is your name?");
            if (tui.getBoolean("Do you want to be a ComputerPlayer? (y/n)")) {
                tui = new BotClientTui(this);
                name = "BOT-" + name;
            }
            String host = tui.getString("What IP would you like to connect to?");
            int port = tui.getInt("What port would you like to use? ");
            // try to open a Socket to the server
            try {
                InetAddress addr = InetAddress.getByName(host);
                tui.showMessage("Attempting to connect to " + addr + ":" + port + "...");
                serverSocket = new Socket(addr, port);
                out = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
                sl = new ServerListener(serverSocket, this, tui);
            } catch (IOException e) {
                tui.showMessage("ERROR: could not create a socket on " + host + " and port " + port + ".");
                if (!tui.getBoolean("Try again? (y/n)")) {
                    throw new ExitProgram("User indicated to exit.");
                }
            }
        }
    }

    /**
     * Resets the serverSocket and In- and OutputStreams to null. Always make sure
     * to close current connections via shutdown() before calling this method!
     */
    public void clearConnection() {
        serverSocket = null;
        out = null;
    }

    /**
     * Sends a message to the server.
     * 
     * @param msg message to send
     * @throws ServerUnavailableException if server is unavailable
     */
    public void sendMessage(String msg) throws ServerUnavailableException {
        if (out != null) {
            try {
                out.write(msg);
                out.newLine();
                out.flush();
            } catch (IOException e) {
                tui.showMessage(e.getMessage());
                throw new ServerUnavailableException("Could not write to the server");
            }
        } else {
            throw new ServerUnavailableException("Could not write to the server");
        }
    }

    /**
     * When a move is received, this is sent to the game so that it is synced.
     * 
     * @param line with move, e.g. MOVE;1,A;1,A;3;
     */
    public void processMove(String line) {
        String[] movesplit = line.split(ProtocolMessages.DELIMITER);
        String move = movesplit[2] + ProtocolMessages.DELIMITER + movesplit[3] + ProtocolMessages.DELIMITER
                + movesplit[4];
        Game game = server.getGame(name);
        try {
            game.getCurrentPlayer().setFields(game.getBoard(), move);
        } catch (OffBoardException e) {
            // Client should always send correct move
        }
    }

    @Override
    public void connect(String name) throws ProtocolException, ServerUnavailableException {
        sendMessage(ProtocolMessages.CONNECT + ProtocolMessages.DELIMITER + name + ProtocolMessages.DELIMITER);
    }

    @Override
    public void createLobby(String lobbyname, int size) throws ServerUnavailableException {
        sendMessage(ProtocolMessages.CREATE + ProtocolMessages.DELIMITER + lobbyname + ProtocolMessages.DELIMITER + size
                + ProtocolMessages.DELIMITER);
    }

    @Override
    public void getLobbyList() throws ServerUnavailableException {
        sendMessage(ProtocolMessages.LISTL + ProtocolMessages.DELIMITER);
    }

    @Override
    public void joinLobby(String lobby) throws ServerUnavailableException {
        sendMessage(ProtocolMessages.JOIN + ProtocolMessages.DELIMITER + lobby + ProtocolMessages.DELIMITER);
    }

    @Override
    public void leaveLobby() throws ServerUnavailableException {
        sendMessage(ProtocolMessages.LEAVE + ProtocolMessages.DELIMITER);
    }

    public void doReady() throws ServerUnavailableException {
        sendMessage(ProtocolMessages.READY + ProtocolMessages.DELIMITER);
    }

    @Override
    public void doUnready() throws ServerUnavailableException {
        sendMessage(ProtocolMessages.UNREADY + ProtocolMessages.DELIMITER);
    }

    @Override
    public void makeMove(String move) throws ServerUnavailableException {
        String[] smove = move.split(";");
        try {
            move = currentGame.getCurrentPlayer().makeLeadingFirst(currentGame.getBoard(),
                    currentGame.getCurrentPlayer().makeGoodFormat(currentGame.getBoard(), smove[0].toUpperCase())
                            + ProtocolMessages.DELIMITER + currentGame.getCurrentPlayer()
                                    .makeGoodFormat(currentGame.getBoard(), smove[1].toUpperCase())
                            + ProtocolMessages.DELIMITER + smove[2]);
        } catch (OffBoardException e) {
            e.printStackTrace();
        }
        sendMessage(ProtocolMessages.MOVE + ProtocolMessages.DELIMITER + move + ProtocolMessages.DELIMITER);
    }

    @Override
    public void playerForfeit() throws ServerUnavailableException {
        sendMessage(ProtocolMessages.FORFEIT + ProtocolMessages.DELIMITER);
    }

    @Override
    public void getServerList() throws ServerUnavailableException {
        sendMessage(ProtocolMessages.LISTP + ProtocolMessages.DELIMITER);
    }

    @Override
    public void challengePlayer(String target) throws ServerUnavailableException {
        sendMessage(ProtocolMessages.CHALL + ProtocolMessages.DELIMITER + target + ProtocolMessages.DELIMITER);
    }

    @Override
    public void challengeAccept(String challenger) throws ServerUnavailableException {
        sendMessage(ProtocolMessages.CHALLACC + ProtocolMessages.DELIMITER + challenger + ProtocolMessages.DELIMITER);
    }

    @Override
    public void sendPM(String receiver, String message) throws ServerUnavailableException {
        sendMessage(ProtocolMessages.PM + ProtocolMessages.DELIMITER + receiver + ProtocolMessages.DELIMITER + message);
    }

    @Override
    public void sendLM(String message) throws ServerUnavailableException {
        sendMessage(ProtocolMessages.LMSG + ProtocolMessages.DELIMITER + message + ProtocolMessages.DELIMITER);
    }

    @Override
    public void getLeaderboard() throws ServerUnavailableException {
        sendMessage(ProtocolMessages.LEADERBOARD + ProtocolMessages.DELIMITER);
    }

    /**
     * Creates a new game.
     * 
     * @param line with players for this game
     */
    public void createGame(String line) {
        String[] sline = line.split(";");
        int playerAmount = sline.length - 1;
        Player p1 = new ClientPlayer(sline[1], Marble.BLACK);
        Player p2 = new ClientPlayer(sline[2], Marble.WHITE);
        currentGame = new ServerGame(p1, p2);
        if (playerAmount == 3) {
            p2.setMarble(Marble.BLUE);
            Player p3 = new ClientPlayer(sline[3], Marble.WHITE);
            currentGame = new ServerGame(p1, p2, p3);
        } else if (playerAmount == 4) {
            Player p3 = new ClientPlayer(sline[3], Marble.BLUE);
            Player p4 = new ClientPlayer(sline[4], Marble.RED);
            currentGame = new ServerGame(p1, p2, p3, p4);
        }
    }

    public void clearGame() {
        currentGame = null;
    }

    public Game getGame() {
        return currentGame;
    }

    public String getName() {
        return name;
    }

    /**
     * Closes this client.
     */
    public void shutDown() {
        try {
            out.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws ExitProgram, IOException {
        (new Client()).start();
    }

}