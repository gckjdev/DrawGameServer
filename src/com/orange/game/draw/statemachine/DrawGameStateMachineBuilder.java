package com.orange.game.draw.statemachine;

import com.orange.common.statemachine.Action;
import com.orange.common.statemachine.Condition;
import com.orange.common.statemachine.DecisionPoint;
import com.orange.common.statemachine.State;
import com.orange.common.statemachine.StateMachine;
import com.orange.game.draw.model.DrawGameSession;
import com.orange.game.draw.statemachine.action.DrawGameAction;
import com.orange.game.draw.statemachine.state.GameState;
import com.orange.game.draw.statemachine.state.GameStateKey;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.statemachine.CommonGameAction;
import com.orange.game.traffic.statemachine.CommonGameCondition;
import com.orange.game.traffic.statemachine.CommonGameState;
import com.orange.game.traffic.statemachine.CommonStateMachineBuilder;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;

public class DrawGameStateMachineBuilder extends CommonStateMachineBuilder {

	// thread-safe singleton implementation
    private static DrawGameStateMachineBuilder builder = new DrawGameStateMachineBuilder();
    public static final State INIT_STATE = new CommonGameState(GameStateKey.CREATE);
    
    private DrawGameStateMachineBuilder(){		
	} 	
    public static DrawGameStateMachineBuilder getInstance() {         	
    	return builder; 
    } 
    	
	static final int PICK_WORD_TIMEOUT = 13;
	static final int START_GAME_TIMEOUT = 33;			// 33 seconds, 20 for start, 10 for result, 3 for reserved
	static final int USER_WAIT_TIMEOUT = 60*30;		// 30 minutes
	static final int DRAW_GUESS_TIMEOUT = 60;
	
	final Action completeGame = new DrawGameAction.CompleteGame();
	final Action playGame = new DrawGameAction.PlayGame();
	final Action clearPlayGame = new DrawGameAction.ClearPlayGame();
	final Action calculateDrawUserCoins = new DrawGameAction.CalculateDrawUserCoins();	
	final Action setOneUserWaitTimer = new DrawGameAction.SetOneUserWaitTimer();
	
	final Action setStartGameTimer = new CommonGameAction.CommonTimer(START_GAME_TIMEOUT, DrawGameAction.DrawTimerType.START);
	final Action setWaitPickWordTimer = new CommonGameAction.CommonTimer(PICK_WORD_TIMEOUT, DrawGameAction.DrawTimerType.PICK_WORD);
	final Action setDrawGuessTimer = new CommonGameAction.CommonTimer(DRAW_GUESS_TIMEOUT, DrawGameAction.DrawTimerType.DRAW_GUESS);
	
	final Action broadcastDrawUserChange = new DrawGameAction.BroadcastDrawUserChange();
	
	final Action fireStartGame = new DrawGameAction.FireStartGame();
	
    @Override
	public StateMachine buildStateMachine() {
		StateMachine sm = new StateMachine();
		
		
		sm.addState(INIT_STATE)		
			.addAction(initGame)
			.addAction(clearTimer)
			.addEmptyTransition(GameCommandType.LOCAL_PLAY_USER_QUIT)			
			.addEmptyTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT)			
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)			
			.addEmptyTransition(GameCommandType.LOCAL_TIME_OUT)		
			.addEmptyTransition(GameCommandType.LOCAL_ALL_USER_GUESS)		
			.addTransition(GameCommandType.LOCAL_NEW_USER_JOIN, GameStateKey.CHECK_USER_COUNT)
			.addAction(setAllUserPlaying)
			.addAction(selectPlayUser)
			.addAction(broadcastDrawUserChange);
		
		sm.addState(new GameState(GameStateKey.CHECK_USER_COUNT))
			.addAction(clearPlayGame)							
			.setDecisionPoint(new DecisionPoint(checkUserCount){
				@Override
				public Object decideNextState(Object context){
					GameSession session = (GameSession)context;
					int userCount = condition.decide(context);
					if (userCount == 0){
						if ( session.isCreatedByUser() ) {
							GameEventExecutor.getInstance().executeForSessionRealease(session.getSessionId());
							return null;
						}
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
			.addAction(clearPlayGame)					
			.addTransition(GameCommandType.LOCAL_NEW_USER_JOIN, GameStateKey.CHECK_USER_COUNT)
			.addTransition(GameCommandType.LOCAL_PLAY_USER_QUIT, GameStateKey.CREATE)
			.addTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT, GameStateKey.CREATE)	
			.addTransition(GameCommandType.LOCAL_OTHER_USER_QUIT, GameStateKey.CREATE)
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.KICK_DRAW_USER)	
			.addAction(clearTimer)
			.addAction(clearRobotTimer);
		
		sm.addState(new GameState(GameStateKey.WAIT_FOR_START_GAME))
			.addAction(setAllUserPlaying)		
			.addAction(setStartGameTimer)
			.addTransition(GameCommandType.LOCAL_PLAY_USER_QUIT, GameStateKey.DRAW_USER_QUIT)
			.addTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT, GameStateKey.CHECK_USER_COUNT)	
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_NEW_USER_JOIN)
			.addEmptyTransition(GameCommandType.LOCAL_DRAW_USER_CHAT)
			.addTransition(GameCommandType.LOCAL_START_GAME, GameStateKey.FIRE_START_GAME)
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.START_GAME_TIMEOUT)		
			.addAction(clearTimer);
				
		sm.addState(new GameState(GameStateKey.DRAW_USER_QUIT))	
			.addAction(selectPlayUser)
			.addAction(broadcastDrawUserChange)			
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					return GameStateKey.CHECK_USER_COUNT;	// goto check user count state directly
				}
			});	
		
		sm.addState(new GameState(GameStateKey.KICK_DRAW_USER))
			.addAction(kickPlayUser)
			.addAction(selectPlayUser)
			.addAction(broadcastDrawUserChange)
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					return GameStateKey.COMPLETE_GAME;	// complete game here
				}
			});
		
		sm.addState(new GameState(GameStateKey.START_GAME_TIMEOUT))
			.addAction(incUserZombieTimeOut)
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					return GameStateKey.FIRE_START_GAME;	// goto check user count state directly
				}
			});
		
		
		sm.addState(new GameState(GameStateKey.FIRE_START_GAME))
//			.addAction(setAllUserPlaying)
			.addAction(playGame)				
			.addAction(fireStartGame)
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					return GameStateKey.WAIT_PICK_WORD;	// goto check user count state directly
				}
			});
		
		sm.addState(new GameState(GameStateKey.WAIT_PICK_WORD))
			.addAction(startGame)
//			.addAction(broadcastDrawGameStart)
			.addAction(setWaitPickWordTimer)
			.addTransition(GameCommandType.LOCAL_START_GAME, GameStateKey.FIRE_START_GAME)
			.addTransition(GameCommandType.LOCAL_WORD_PICKED, GameStateKey.DRAW_GUESS)
			.addTransition(GameCommandType.LOCAL_PLAY_USER_QUIT, GameStateKey.COMPLETE_GAME)
			.addTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT, GameStateKey.COMPLETE_GAME)	
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_NEW_USER_JOIN)			
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.KICK_DRAW_USER)	
			.addAction(clearTimer);
		
		sm.addState(new GameState(GameStateKey.DRAW_GUESS))
			.addAction(setDrawGuessTimer)
			.addTransition(GameCommandType.LOCAL_PLAY_USER_QUIT, GameStateKey.COMPLETE_GAME)
			.addTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT, GameStateKey.COMPLETE_GAME)	
			.addTransition(GameCommandType.LOCAL_ALL_USER_GUESS, GameStateKey.COMPLETE_GAME)
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT) // GameStateKey.CHECK_IS_ALL_USER_GUESSED)
			.addEmptyTransition(GameCommandType.LOCAL_DRAW_USER_CHAT)
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.COMPLETE_GAME)				
			.addAction(clearPlayGame)		
			.addAction(clearTimer);
		
		/*
		sm.addState(new GameState(GameStateKey.CHECK_IS_ALL_USER_GUESSED))
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					DrawGameSession session = (DrawGameSession)context;					
					return GameStateKey.WAIT_PICK_WORD;	// goto check user count state directly
				}
			});		
		*/
		
		sm.addState(new GameState(GameStateKey.COMPLETE_GAME))
			.addAction(clearPlayGame)				
			.addAction(setAllUserPlaying)					
			.addAction(calculateDrawUserCoins)
			.addAction(selectPlayUser)
			.addAction(completeGame)
			.addAction(kickZombieUser)			
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
