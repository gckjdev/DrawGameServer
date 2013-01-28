package com.orange.game.draw.statemachine.state;

public enum GameStateKey {
	CREATE (0),
	WAITING (1),
	PLAYING (2),
	SUSPEND (3),
	FINISH (4), 

	CHECK_USER_COUNT(11), ONE_USER_WAITING(12),
	SELECT_NEW_DRAW_USER(13), WAIT_FOR_START_GAME(14), 
	KICK_DRAW_USER(15), WAIT_PICK_WORD(16), DRAW_GUESS(17), 
	COMPLETE_GAME(18), DRAW_USER_QUIT(19), FIRE_START_GAME(20), START_GAME_TIMEOUT(21);
		
	final int value;
	
	GameStateKey(int value){
		this.value = value;
	}
}
