package ai;

import ai.Global;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

import javax.swing.*;
import java.awt.*;
import kalaha.*;

/**
 * This is the main class for your Kalaha AI bot. Currently it only makes a
 * random, valid move each turn.
 *
 * @author Johan HagelbÃ¤ck
 */
public class AIClient implements Runnable {

    private int player;
    private JTextArea text;

    private PrintWriter out;
    private BufferedReader in;
    private Thread thr;
    private Socket socket;
    private boolean running;
    private boolean connected;

    private boolean searching;
    
    public ArrayList<Integer> moves = new ArrayList<Integer>();
    public boolean isTerminal;

    /**
     * Creates a new client.
     */
    public AIClient() {
        player = -1;
        connected = false;

        //This is some necessary client stuff. You don't need
        //to change anything here.
        initGUI();

        try {
            addText("Connecting to localhost:" + KalahaMain.port);
            socket = new Socket("localhost", KalahaMain.port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            addText("Done");
            connected = true;
        } catch (Exception ex) {
            addText("Unable to connect to server");
            return;
        }
    }

    /**
     * Starts the client thread.
     */
    public void start() {
        //Don't change this
        if (connected) {
            thr = new Thread(this);
            thr.start();
        }
    }

    /**
     * Creates the GUI.
     */
    private void initGUI() {
        //Client GUI stuff. You don't need to change this.
        JFrame frame = new JFrame("My AI Client");
        frame.setLocation(Global.getClientXpos(), 445);
        frame.setSize(new Dimension(420, 250));
        frame.getContentPane().setLayout(new FlowLayout());

        text = new JTextArea();
        JScrollPane pane = new JScrollPane(text);
        pane.setPreferredSize(new Dimension(400, 210));

        frame.getContentPane().add(pane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
        frame.setVisible(true);
    }

    /**
     * Adds a text string to the GUI textarea.
     *
     * @param txt The text to add
     */
    public void addText(String txt) {
        //Don't change this
        text.append(txt + "\n");
        text.setCaretPosition(text.getDocument().getLength());
    }

    /**
     * Thread for server communication. Checks when it is this client's turn to
     * make a move.
     */
    public void run() {
        String reply;
        running = true;

        try {
            while (running) {
                //Checks which player you are. No need to change this.
                if (player == -1) {
                    out.println(Commands.HELLO);
                    reply = in.readLine();

                    String tokens[] = reply.split(" ");
                    player = Integer.parseInt(tokens[1]);

                    addText("I am player " + player);
                }

                //Check if game has ended. No need to change this.
                out.println(Commands.WINNER);
                reply = in.readLine();
                if (reply.equals("1") || reply.equals("2")) {
                    int w = Integer.parseInt(reply);
                    if (w == player) {
                        addText("I won!");
                    } else {
                        addText("I lost...");
                    }
                    running = false;
                }
                if (reply.equals("0")) {
                    addText("Even game!");
                    running = false;
                }

                //Check if it is my turn. If so, do a move
                out.println(Commands.NEXT_PLAYER);
                reply = in.readLine();
                if (!reply.equals(Errors.GAME_NOT_FULL) && running) {
                    int nextPlayer = Integer.parseInt(reply);

                    if (nextPlayer == player) {
                        out.println(Commands.BOARD);
                        String currentBoardStr = in.readLine();
                        boolean validMove = false;
                        while (!validMove) {
                            long startT = System.currentTimeMillis();
                            //This is the call to the function for making a move.
                            //You only need to change the contents in the getMove()
                            //function.
                            GameState currentBoard = new GameState(currentBoardStr);
                            int cMove = getMove(currentBoard);

                            //Timer stuff
                            long tot = System.currentTimeMillis() - startT;
                            double e = (double) tot / (double) 1000;

                            out.println(Commands.MOVE + " " + cMove + " " + player);
                            reply = in.readLine();
                            if (!reply.startsWith("ERROR")) {
                                validMove = true;
                                addText("Made move " + cMove + " in " + e + " secs");
                            }
                        }
                    }
                }

                //Wait
                Thread.sleep(100);
            }
        } catch (Exception ex) {
            running = false;
        }

        try {
            socket.close();
            addText("Disconnected from server");
        } catch (Exception ex) {
            addText("Error closing connection: " + ex.getMessage());
        }
    }

    
    public int getMove(GameState currentBoard) {
    	
    	int best_move = 0;
    	int best_score= Integer.MIN_VALUE;
    	int current_score= 0;
    	int depth = 3;
    	
    	for(int ambo=1; ambo<=6 ; ambo++) {
    		if(currentBoard.moveIsPossible(ambo)) {
    			moves.add(ambo);
    		}
    	}
    	
    	for(int i: moves) {
    		
    		GameState gs = currentBoard.clone();
    		if(gs.makeMove(i)) {
    			current_score= play(gs,depth,Integer.MIN_VALUE,Integer.MAX_VALUE,player);	
    		}
    		
    		if(current_score > best_score) {
    			best_score = current_score;
    			best_move = i;
    		}
    		System.out.println(current_score + "  "+best_move );
    		}
    	return best_move;
    		
    }
    
    public int play(GameState gs, int depth, int alpha, int beta, int player) {
    	
    	int current_score, utility;
    	int val = (player == 1) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    	
    	if(depth == 0) {
    		
    		utility = gs.getScore(1) - gs.getScore(2);
    		return utility;
    	}
    	
    	for(int i : moves) {
    		
    		GameState state = gs.clone();
    		
    		if(player == 1) {
    			
    			current_score = play(gs,depth - 1,alpha,beta,2);
    			
    			val = Math.max(val,current_score);
    			
    			alpha = Math.max(alpha, val);
    			if(alpha > beta)
    				break;
    		}
    		
    		else if(player == 2) {
    			
    			current_score = play(gs,depth - 1,alpha,beta,1);
    			
    			val = Math.min(val, current_score);
    			
    			beta = Math.min(beta, val);
    			if(beta < alpha)
    				break;
    		}
    	}
    	
    	return val;
    	
    }

    /**
     * Returns a random ambo number (1-6) used when making a random move.
     *
     * @return Random ambo number
     */
    public int getRandom() {
        return 1 + (int) (Math.random() * 6);
    }
}
