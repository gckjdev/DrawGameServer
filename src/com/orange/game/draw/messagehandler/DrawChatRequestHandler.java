package com.orange.game.draw.messagehandler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;

import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.message.GameMessageProtos.GameChatRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;

public class DrawChatRequestHandler extends AbstractMessageHandler {

	public DrawChatRequestHandler(MessageEvent messageEvent) {
		super(messageEvent);
	}

	@Override
	public void handleRequest(GameMessage message, Channel channel,
			GameSession session) {

		GameChatRequest chatRequest = message.getChatRequest();
		if (chatRequest == null)
			return;				
		
		String fromUserId = message.getUserId();
		if (session.safeGetCurrentPlayUserId().equals(fromUserId)){
			// fire draw user chat event
			GameEventExecutor.getInstance().fireAndDispatchEvent(GameCommandType.LOCAL_DRAW_USER_CHAT, 
					session.getSessionId(), fromUserId);
		}
		
		// broast draw data to all other users in the session
		NotificationUtils.broadcastChatNotification(session, message, fromUserId);
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
