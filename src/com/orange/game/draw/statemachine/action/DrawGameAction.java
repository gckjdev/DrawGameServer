package com.orange.game.draw.statemachine.action;

import java.util.List;
import com.orange.common.log.ServerLog;
import com.orange.common.statemachine.Action;
import com.orange.game.draw.model.DrawGameSession;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.game.traffic.model.manager.GameUserManager;
import com.orange.game.traffic.robot.client.RobotService;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.server.HandlerUtils;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.game.traffic.service.SessionUserService;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos;

public class DrawGameAction{

	public static class FireStartGame implements Action {

			public static void fireGameStartNotification(GameSession gameSession) {
			
				List<GameUser> list = gameSession.getUserList().getAllUsers();
				String currentPlayUserId = gameSession.getCurrentPlayUserId();
				if (currentPlayUserId == null){
					ServerLog.warn(gameSession.getSessionId(), "<fireGameStartNotification> but current play user null");
					return;
				}
				
				for (GameUser user : list){		
				
				if (!user.isPlaying()){
					ServerLog.info(gameSession.getSessionId(), "send START game notificaiton but user "+
							user.getNickName()+" not in play state");
					continue;
				}
				
				if (currentPlayUserId.equals(user.getUserId())){
					// send start game response
					GameMessageProtos.StartGameResponse gameResponse = GameMessageProtos.StartGameResponse.newBuilder()
						.setCurrentPlayUserId(currentPlayUserId)
						.setNextPlayUserId("")
						.build();
					GameMessageProtos.GameMessage response = GameMessageProtos.GameMessage.newBuilder()
						.setCommand(GameCommandType.START_GAME_RESPONSE)
						.setMessageId(GameEventExecutor.getInstance().generateMessageId())
						.setResultCode(GameResultCode.SUCCESS)
						.setStartGameResponse(gameResponse)
						.build();
					HandlerUtils.sendMessage(response, user.getChannel());
	
				}
				else{
					// send notification for the user
					GameMessageProtos.GeneralNotification notification = GameMessageProtos.GeneralNotification.newBuilder()		
						.setCurrentPlayUserId(gameSession.getCurrentPlayUserId())
						.setNextPlayUserId("")
						.build();
					
					GameMessageProtos.GameMessage message = GameMessageProtos.GameMessage.newBuilder()
						.setCommand(GameCommandType.GAME_START_NOTIFICATION_REQUEST)
						.setMessageId(GameEventExecutor.getInstance().generateMessageId())
						.setNotification(notification)
						.setSessionId(gameSession.getSessionId())
						.setUserId(user.getUserId())
						.setToUserId(user.getUserId())				
						.build();
					
					HandlerUtils.sendMessage(message, user.getChannel());
					
				}
			}
		}
		
		
		@Override
		public void execute(Object context) {			
			DrawGameSession session = (DrawGameSession)context;		
			fireGameStartNotification(session);	
		}
	}

	public enum DrawTimerType {
		START,
		PICK_WORD,
		DRAW_GUESS,
		USER_WAIT
	};
	
	public static class BroadcastDrawUserChange implements Action {

		@Override
		public void execute(Object context) {
			GameSession session = (GameSession)context;
			NotificationUtils.broadcastDrawUserChangeNotification(session);
		}

	}

	public static class SelectDrawUserIfNone implements Action {

		@Override
		public void execute(Object context) {
			GameSession session = (GameSession)context;
			if (session.getCurrentPlayUser() == null){
				session.selectPlayerUser();
			}
		}

	}



	public static class CalculateDrawUserCoins implements Action {

		@Override
		public void execute(Object context) {
			DrawGameSession session = (DrawGameSession)context;
			session.calculateDrawUserCoins();
		}

	}

	static final int PICK_WORD_TIMEOUT = 60;
	static final int START_GAME_TIMEOUT = 36;			// 36 seconds, 20 for start, 10 for result, 6 for reserved
	static final int USER_WAIT_TIMEOUT = 60*30;		// 30 minutes
	static final int DRAW_GUESS_TIMEOUT = 60;
	
//	public static class StartGame implements Action {
//
//		@Override
//		public void execute(Object context) {
//			GameSession session = (GameSession)context;
//			session.startGame();
//		}
//
//	}


	public static class CompleteGame implements Action {

		public static void broadcastDrawGameCompleteNotification(DrawGameSession session) {

			int onlineUserCount = GameUserManager.getInstance().getOnlineUserCount();
			List<GameUser> list = session.getUserList().getAllUsers();			
			for (GameUser user : list){						
				GameMessageProtos.GeneralNotification notification;			
				notification = GameMessageProtos.GeneralNotification.newBuilder()		
					.setCurrentPlayUserId(session.safeGetCurrentPlayUserId())
					.setTurnGainCoins(session.getCurrentUserGainCoins(user.getUserId()))
					.build();				
				
				// send notification for the user			
				GameMessageProtos.GameMessage message = GameMessageProtos.GameMessage.newBuilder()
					.setCommand(GameCommandType.GAME_TURN_COMPLETE_NOTIFICATION_REQUEST)
					.setMessageId(GameEventExecutor.getInstance().generateMessageId())
					.setSessionId(session.getSessionId())
					.setUserId(user.getUserId())
					.setToUserId(user.getUserId())
					.setCompleteReason(session.getCompleteReason())
					.setNotification(notification)			
					.setRound(session.getCurrentRound())
					.setOnlineUserCount(onlineUserCount)
					.build();
				
				HandlerUtils.sendMessage(message, user.getChannel());
			}
		}		
		
		@Override
		public void execute(Object context) {
			DrawGameSession session = (DrawGameSession)context;
			session.completeTurn();	
			broadcastDrawGameCompleteNotification(session);
		}
	}	


	




	public static class PlayGame implements Action {

		@Override
		public void execute(Object context) {
			GameSession session = (GameSession)context;
			
			// TODO think about it
		}

	}

	public static class SetOneUserWaitTimer implements Action {

		@Override
		public void execute(Object context) {
			GameSession session = (GameSession)context;
			GameEventExecutor.getInstance().startTimer(session, USER_WAIT_TIMEOUT, DrawTimerType.USER_WAIT);
		}

	}
	
	public static class SelectDrawUser implements Action {

		@Override
		public void execute(Object context) {
			GameSession session = (GameSession)context;
			session.selectPlayerUser();
		}

	}

	public static class InitGame implements Action{

		@Override
		public void execute(Object context) {
			GameSession session = (GameSession)context;
			session.resetGame();
		}
		
	}
	
	public static class SetWaitClaimTimer implements Action {

		int interval;
		final Object timerType;
		final int normalInterval;
		
		public SetWaitClaimTimer(int interval, Object timerType){
			this.interval = interval;
			this.timerType = timerType;
			this.normalInterval = interval;
		}
		
		private void  getNewInterval(GameSession session) {
			int newInterval = 0;
			if ((newInterval = session.getNewInterval()) != 0) {
				interval = newInterval; 
			}
			interval = normalInterval;
			
		}
		
		private void clearNewInterval(GameSession session) {
			session.setNewInternal(0);
		}
		
		@Override
		public void execute(Object context) {
			DrawGameSession session = (DrawGameSession)context;
			// check to see if the interval is changed(by last user using decTime item)
			getNewInterval(session);
			GameEventExecutor.getInstance().startTimer(session, interval, timerType);
			// clear it, so the intervel won't influence next user.
			clearNewInterval(session);
			// correctly set the  decreaseTimeForNextPlayUser 
			if ( session.getDecreaseTimeForNextPlayUser() == true ) {
				session.setDecreaseTimeForNextPlayUser(false);
			}
		}
	}
	

	
	public static class BroadcastNextPlayerNotification implements Action {

		@Override
		public void execute(Object context) {
			DrawGameSession session = (DrawGameSession)context;
			NotificationUtils.broadcastNotification(session, null, GameCommandType.NEXT_PLAYER_START_NOTIFICATION_REQUEST);			
		}

	}
	
	public static class RestartGame implements Action {

		@Override
		public void execute(Object context) {
			DrawGameSession session = (DrawGameSession)context;
			session.restartGame();
			SessionUserService.getInstance().kickTakenOverUser(session);			
			return;
		}
	}		

}
