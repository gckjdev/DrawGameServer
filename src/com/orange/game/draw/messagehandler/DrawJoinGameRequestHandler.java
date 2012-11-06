package com.orange.game.draw.messagehandler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;

import com.orange.game.draw.model.DrawGameSession;
import com.orange.game.traffic.messagehandler.room.JoinGameRequestHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.JoinGameResponse;
import com.orange.network.game.protocol.message.GameMessageProtos.UserDiceNotification;

public class DrawJoinGameRequestHandler extends JoinGameRequestHandler {

	public DrawJoinGameRequestHandler(MessageEvent event) {
		super(event);
	}
	
	@Override
	public void handleRequest(GameMessage message, Channel channel, GameSession requestSession) {

		DrawGameSession session = (DrawGameSession)processRequest(message, channel, requestSession);		
	}

	@Override
	public JoinGameResponse responseSpecificPart(JoinGameResponse.Builder builder,GameSession session) {
		
		JoinGameResponse response;
		
		if (session.isGamePlaying()) {
			//TODO : to be completed
			response = builder.build();
			return response;
	
	   
		} else {	
			response = builder.build();
			return response;
		}
	}

}




	

