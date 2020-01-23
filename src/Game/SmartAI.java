package Game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SmartAI implements Strategy  {

	@Override
	public String determineMove(Board board, Marble marble) {
		List<Integer> empty = new ArrayList<Integer>();
		for (int i = 0; i < 61; i ++) {
			if (board.isEmptyField(i)) {
				empty.add(i);
			}
		}
		
		//check if you can win the game
		Board copy = board.deepCopy();
			if(board.getPlayers() == 2) {
				Marble oppmarble = Marble.BLACK;
				if(marble == Marble.BLACK) {
					oppmarble = Marble.WHITE;
				}
				for(int j = 0; j <= 61; j++) {
					copy.setField(j, marble);
					if(empty.contains(j)) {
						if(board.getNRofMarbles(oppmarble) == 8) {
							// maak die zet, maar hoe krijg je 2e of 3e marble????
						}
					}
				}
		}
			if(board.getPlayers() == 3) {
				Marble opp1marble = Marble.BLACK;
				Marble opp2marble = Marble.WHITE;
				if(marble == Marble.BLACK) {
					opp1marble = Marble.BLUE;
				}
				else if(marble == Marble.WHITE) {
					opp2marble = Marble.BLUE;
				}
				for(int j = 0; j < empty.size(); j++) {
					
				}
			}
		
		//makes a random move
		int m1 = empty.get((int) (Math.random()*empty.size()));
		int m2 = empty.get((int) (Math.random()*empty.size()));
		int d = (int) (6.00 * Math.random());
		Direction dir = null;
		if(d == 0) {
			dir = Direction.TOP_LEFT;
		}
		else if(d == 1) {
			dir = Direction.TOP_RIGHT;
		}
		else if(d == 2) {
			dir = Direction.RIGHT;
		}
		else if(d == 3) {
			dir = Direction.BOTTOM_RIGHT;
		}
		else if(d == 4) {
			dir = Direction.BOTTOM_LEFT;
		}
		else if(d == 5) {
			dir = Direction.LEFT;
		}
		
		String marble1 = board.getCoords(m1);
		String marble2 = board.getCoords(m2);
		
		Game game;
		String p1name = "pc";
		Player p1 = new ComputerPlayer(p1name, Marble.WHITE);
		String move = marble1 + ";" + marble2 + ";" + dir;
		
		//checks if the move proposed is valid, if not it makes a new move
		if (board.isValidMove(p1, move) == false) {
			determineMove(board, marble);
		}
		return move;
	}
	
}
