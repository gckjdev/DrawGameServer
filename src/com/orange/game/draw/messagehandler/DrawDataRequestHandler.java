package com.orange.game.draw.messagehandler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;

import com.orange.game.draw.model.DrawGameSession;
import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCompleteReason;
import com.orange.network.game.protocol.message.GameMessageProtos;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.SendDrawDataRequest;

public class DrawDataRequestHandler extends AbstractMessageHandler {


	public DrawDataRequestHandler(MessageEvent messageEvent) {
		super(messageEvent);
	}

	@Override
	public void handleRequest(GameMessage message, Channel channel,
			GameSession gameSession) {

		DrawGameSession session = (DrawGameSession)gameSession;
		GameCommandType stateMachineCommandType = null;
		
		GameCompleteReason reason = GameCompleteReason.REASON_NOT_COMPLETE;
		
		SendDrawDataRequest drawRequest = message.getSendDrawDataRequest();
		if (drawRequest == null){
			return;
		}

		if (drawRequest.getPointsCount() > 0){
			session.appendDrawData(drawRequest.getPointsList(),
					drawRequest.getColor(),
					drawRequest.getWidth());
		}
				
		if (drawRequest.hasWord()){
			session.startNewTurn(drawRequest.getWord(), drawRequest.getLevel(), drawRequest.getLanguage());

			// schedule timer for finishing this turn
//			gameService.scheduleGameSessionExpireTimer(session);
			
			stateMachineCommandType = GameCommandType.LOCAL_WORD_PICKED;
		}
		
		
		if (drawRequest.hasGuessWord()){
//			GameLog.info(session.getSessionId(), "user "+drawRequest.getGuessUserId()+ 
//					" guess "+drawRequest.getGuessWord());
			GameUser guessUser = session.findUser(drawRequest.getGuessUserId());
			session.userGuessWord(guessUser, drawRequest.getGuessWord());						

			if (session.isAllUserGuessWord()){
				session.setCompleteReason(GameCompleteReason.REASON_ALL_USER_GUESS);
				stateMachineCommandType = GameCommandType.LOCAL_ALL_USER_GUESS;
			}		
		}				
						
//		if (reason != GameCompleteReason.REASON_NOT_COMPLETE){
//			gameService.fireTurnFinishEvent(session, reason);
//		}
		
		SendDrawDataRequest drawData = message.getSendDrawDataRequest();
		if (drawData == null){
			return;
		}
		
		String userId = message.getUserId();
		boolean guessCorrect = false;
		int guessGainCoins = 0;
		if (drawData.hasGuessWord()){
			String currentWord = session.getCurrentGuessWord();
			if (currentWord != null){
				guessCorrect = drawData.getGuessWord().equalsIgnoreCase(currentWord);
			}
			
			if (guessCorrect){
				guessGainCoins = session.getCurrentGuessUserCoins(userId);
			}
		}
		// send notification for the user
		GameMessageProtos.GeneralNotification notification = GameMessageProtos.GeneralNotification.newBuilder()		
			.setColor(drawData.getColor())
			.addAllPoints(drawData.getPointsList())
			.setWidth(drawData.getWidth())
			.setPenType(drawData.getPenType())
			.setWord(drawData.getWord())
			.setLevel(drawData.getLevel())	
			.setLanguage(drawData.getLanguage())
			.setRound(session.getCurrentRound())
			.setGuessWord(drawData.getGuessWord())
			.setGuessUserId(drawData.getGuessUserId())
			.setGuessCorrect(guessCorrect)
			.setGuessGainCoins(guessGainCoins)
			.build();		
		
		// broast draw data to all other users in the session
		NotificationUtils.broadcastDrawDataNotification(session, message.getUserId(), guessCorrect, notification);			
		
		// fire pick word local message
		// drive state machine running
		if (stateMachineCommandType != null){			
			GameEventExecutor.getInstance().fireAndDispatchEvent(stateMachineCommandType, session.getSessionId(), message.getUserId());
//			
//			GameEvent stateMachineEvent = new GameEvent(
//					stateMachineCommandType, 
//					session.getSessionId(), 
//					gameEvent.getMessage(), 
//					channel);
//			
//			gameService.dispatchEvent(stateMachineEvent);
		}
	}

	@Override
	public boolean isProcessForSessionAllocation() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isProcessIgnoreSession() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isProcessInStateMachine() {
		// TODO Auto-generated method stub
		return false;
	}


}
