package com.orange.game.draw.statemachine;

import com.orange.common.statemachine.Action;
import com.orange.common.statemachine.Condition;
import com.orange.common.statemachine.DecisionPoint;
import com.orange.common.statemachine.State;
import com.orange.common.statemachine.StateMachine;
import com.orange.common.statemachine.StateMachineBuilder;
import com.orange.game.draw.statemachine.action.DrawGameAction;
import com.orange.game.draw.statemachine.state.GameState;
import com.orange.game.draw.statemachine.state.GameStateKey;
import com.orange.game.traffic.statemachine.CommonGameAction;
import com.orange.game.traffic.statemachine.CommonGameCondition;
import com.orange.game.traffic.statemachine.CommonGameState;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;

public class DrawGameStateMachineBuilder extends StateMachineBuilder {

	// thread-safe singleton implementation
    private static DrawGameStateMachineBuilder builder = new DrawGameStateMachineBuilder();
    public static final State INIT_STATE = new CommonGameState(GameStateKey.CREATE);
    
    private DrawGameStateMachineBuilder(){		
	} 	
    public static DrawGameStateMachineBuilder getInstance() {         	
    	return builder; 
    } 
    

	
//   public static final int START_GAME_TIMEOUT = 3;			// 36 seconds, 20 for start, 10 for result, 6 for reserved
//	public static final int WAIT_CLAIM_TIMEOUT = 18;
//	public static final int ROLL_DICE_TIMEOUT = 3;
//	public static final int SHOW_RESULT_TIMEOUT = 10;
//	public static final int TAKEN_OVER_USER_WAIT_TIMEOUT = 1;
//	public static final int WAIT_USER_BET_TIMEOUT = 7;
	
	static final int PICK_WORD_TIMEOUT = 60;
	static final int START_GAME_TIMEOUT = 36;			// 36 seconds, 20 for start, 10 for result, 6 for reserved
	static final int USER_WAIT_TIMEOUT = 60*30;		// 30 minutes
	static final int DRAW_GUESS_TIMEOUT = 60;
	
    	
    @Override
	public StateMachine buildStateMachine() {
		StateMachine sm = new StateMachine();
		
		Action initGame = new CommonGameAction.InitGame();
		Action startGame = new DrawGameAction.StartGame();
		Action completeGame = new DrawGameAction.CompleteGame();
		Action selectDrawUser = new DrawGameAction.SelectDrawUser();
		Action kickDrawUser = new DrawGameAction.KickDrawUser();
		Action playGame = new DrawGameAction.PlayGame();
		Action prepareRobot = new DrawGameAction.PrepareRobot();
		Action calculateDrawUserCoins = new DrawGameAction.CalculateDrawUserCoins();
		Action selectDrawUserIfNone = new DrawGameAction.SelectDrawUserIfNone();
		
		Action setOneUserWaitTimer = new DrawGameAction.SetOneUserWaitTimer();
		Action setStartGameTimer = new CommonGameAction.CommonTimer(START_GAME_TIMEOUT, DrawGameAction.DrawTimerType.START);
		Action setWaitPickWordTimer = new CommonGameAction.CommonTimer(PICK_WORD_TIMEOUT, DrawGameAction.DrawTimerType.PICK_WORD);
		Action setDrawGuessTimer = new CommonGameAction.CommonTimer(DRAW_GUESS_TIMEOUT, DrawGameAction.DrawTimerType.DRAW_GUESS);
		Action clearTimer = new CommonGameAction.ClearTimer();
		Action clearRobotTimer = new DrawGameAction.ClearRobotTimer();
		
		Action broadcastDrawUserChange = new DrawGameAction.BroadcastDrawUserChange();

		Condition checkUserCount = new CommonGameCondition.CheckUserCount();
		
		sm.addState(INIT_STATE)		
			.addAction(initGame)
			.addAction(clearTimer)
			.addEmptyTransition(GameCommandType.LOCAL_DRAW_USER_QUIT)			
			.addEmptyTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT)			
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)			
			.addEmptyTransition(GameCommandType.LOCAL_TIME_OUT)			
			.addTransition(GameCommandType.LOCAL_NEW_USER_JOIN, GameStateKey.CHECK_USER_COUNT)
			.addAction(selectDrawUser)
			.addAction(broadcastDrawUserChange);
		
		sm.addState(new GameState(GameStateKey.CHECK_USER_COUNT))
			.setDecisionPoint(new DecisionPoint(checkUserCount){
				@Override
				public Object decideNextState(Object context){
					int userCount = condition.decide(context);
					if (userCount == 0){
						return GameStateKey.CREATE;
					}
					else if (userCount == 1){ // only one user
						return GameStateKey.ONE_USER_WAITING;
					}
					else{ // more than one user
						return GameStateKey.WAIT_FOR_START_GAME;
					}
				}
			});
		
		sm.addState(new GameState(GameStateKey.ONE_USER_WAITING))
			.addAction(setOneUserWaitTimer)
			.addAction(prepareRobot)
			.addTransition(GameCommandType.LOCAL_NEW_USER_JOIN, GameStateKey.CHECK_USER_COUNT)
			.addTransition(GameCommandType.LOCAL_DRAW_USER_QUIT, GameStateKey.CREATE)
			.addTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT, GameStateKey.CREATE)	
			.addTransition(GameCommandType.LOCAL_OTHER_USER_QUIT, GameStateKey.CREATE)
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.KICK_DRAW_USER)	
			.addAction(clearTimer)
			.addAction(clearRobotTimer);
		
		sm.addState(new GameState(GameStateKey.WAIT_FOR_START_GAME))
			.addAction(setStartGameTimer)
			.addTransition(GameCommandType.LOCAL_DRAW_USER_QUIT, GameStateKey.DRAW_USER_QUIT)
			.addTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT, GameStateKey.CHECK_USER_COUNT)	
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_NEW_USER_JOIN)
			.addTransition(GameCommandType.LOCAL_START_GAME, GameStateKey.WAIT_PICK_WORD)
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.KICK_DRAW_USER)	
			.addTransition(GameCommandType.LOCAL_DRAW_USER_CHAT, GameStateKey.WAIT_FOR_START_GAME)	
			.addAction(clearTimer);
		
		sm.addState(new GameState(GameStateKey.DRAW_USER_QUIT))	
			.addAction(selectDrawUser)
			.addAction(broadcastDrawUserChange)			
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					return GameStateKey.CHECK_USER_COUNT;	// goto check user count state directly
				}
			});	
		
		sm.addState(new GameState(GameStateKey.KICK_DRAW_USER))
			.addAction(kickDrawUser)
			.addAction(selectDrawUser)
			.addAction(broadcastDrawUserChange)
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					return GameStateKey.CHECK_USER_COUNT;	// goto check user count state directly
				}
			});
		
		sm.addState(new GameState(GameStateKey.WAIT_PICK_WORD))
			.addAction(startGame)
			.addAction(setWaitPickWordTimer)
			.addTransition(GameCommandType.LOCAL_WORD_PICKED, GameStateKey.DRAW_GUESS)
			.addTransition(GameCommandType.LOCAL_DRAW_USER_QUIT, GameStateKey.COMPLETE_GAME)
			.addTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT, GameStateKey.COMPLETE_GAME)	
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.KICK_DRAW_USER)	
			.addAction(clearTimer);
		
		sm.addState(new GameState(GameStateKey.DRAW_GUESS))
			.addAction(setDrawGuessTimer)
			.addAction(playGame)		
			.addTransition(GameCommandType.LOCAL_DRAW_USER_QUIT, GameStateKey.COMPLETE_GAME)
			.addTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT, GameStateKey.COMPLETE_GAME)	
			.addTransition(GameCommandType.LOCAL_ALL_USER_GUESS, GameStateKey.COMPLETE_GAME)
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_DRAW_USER_CHAT)
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.COMPLETE_GAME)				
			.addAction(clearTimer);
		
		sm.addState(new GameState(GameStateKey.COMPLETE_GAME))
			.addAction(calculateDrawUserCoins)
			.addAction(selectDrawUser)
			.addAction(completeGame)
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					return GameStateKey.CHECK_USER_COUNT;	// goto check user count state directly
				}
			});
		
		sm.printStateMachine();				
		return sm;
	}

}
