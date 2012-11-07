package com.orange.game.draw.messagehandler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;

import com.orange.game.draw.model.DrawGameSession;
import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;

public class CleanDrawRequestHandler extends AbstractMessageHandler  {


	public CleanDrawRequestHandler(MessageEvent messageEvent) {
		super(messageEvent);
	}

	@Override
	public void handleRequest(GameMessage message, Channel channel,
			GameSession session) {
		if (session != null){
			((DrawGameSession)session).appendCleanDrawAction();
		}
		
		// broast draw data to all other users in the session
		NotificationUtils.broadcastCleanDrawNotification(session, message.getUserId());		
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
