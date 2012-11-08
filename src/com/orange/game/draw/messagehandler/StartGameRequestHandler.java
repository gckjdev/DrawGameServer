package com.orange.game.draw.messagehandler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;

import com.orange.common.log.ServerLog;
import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.server.HandlerUtils;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;

public class StartGameRequestHandler extends AbstractMessageHandler {
	
	public StartGameRequestHandler(MessageEvent messageEvent) {
		super(messageEvent);
	}

	@Override
	public void handleRequest(GameMessage message, Channel channel,
			GameSession session) {	
		String userId = message.getUserId();
		if (session.isCurrentPlayUser(userId)){
			GameEventExecutor.getInstance().fireAndDispatchEvent(GameCommandType.LOCAL_START_GAME, session.getSessionId(), message.getUserId());
		}
		else{
			ServerLog.warn(session.getSessionId(), "receive START GAME request but userId "+userId+" is not current play user");
			HandlerUtils.sendMessageWithResultCode(message, GameResultCode.ERROR_USER_NOT_CURRENT_PLAY_USER, channel);
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
