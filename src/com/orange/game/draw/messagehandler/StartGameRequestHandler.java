package com.orange.game.draw.messagehandler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;

import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;

public class StartGameRequestHandler extends AbstractMessageHandler {
	
	public StartGameRequestHandler(MessageEvent messageEvent) {
		super(messageEvent);
		// TODO Auto-generated constructor stub
	}

	/*
	private GameResultCode validateStartGameRequest(GameEvent gameEvent, GameSession session) {
		String userId = gameEvent.getMessage().getUserId();
		if (userId == null)
			return GameResultCode.ERROR_USERID_NULL;
		
		if (session == null)
			return GameResultCode.ERROR_SESSIONID_NULL;
		
		if (session.isStart())
			return GameResultCode.ERROR_SESSION_ALREADY_START;
		
		return GameResultCode.SUCCESS;
	}
	*/
	
	@Override
	public void handleRequest(GameMessage message, Channel channel,
			GameSession session) {
		
//		GameSessionManager.getInstance().adjustSessionSetForPlaying(session); // adjust set so that it's not allowed to join
//		sessionUserManager.setUserPlaying(session);

		/*
		// send reponse
		GameMessageProtos.StartGameResponse gameResponse = GameMessageProtos.StartGameResponse.newBuilder()
			.setCurrentPlayUserId(session.getCurrentPlayUserId())
			.setNextPlayUserId("")
			.build();
		GameMessageProtos.GameMessage response = GameMessageProtos.GameMessage.newBuilder()
			.setCommand(GameCommandType.START_GAME_RESPONSE)
			.setMessageId(gameEvent.getMessage().getMessageId())
			.setResultCode(GameResultCode.SUCCESS)
			.setStartGameResponse(gameResponse)
			.build();
		HandlerUtils.sendResponse(gameEvent, response);
		
		// broast to all users in the session
		GameNotification.broadcastGameStartNotification(session, gameEvent);
		*/
		
		// drive state machine running
//		GameEvent stateMachineEvent = new GameEvent(
//				GameCommandType.LOCAL_START_GAME, 
//				session.getSessionId(), 
//				gameEvent.getMessage(), 
//				channel);
//		
//		gameService.dispatchEvent(stateMachineEvent);
		
		GameEventExecutor.getInstance().fireAndDispatchEvent(GameCommandType.LOCAL_START_GAME, session.getSessionId(), message.getUserId());
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
