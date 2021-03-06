package com.orange.game.draw.server;

import org.jboss.netty.channel.MessageEvent;

import com.orange.common.log.ServerLog;
import com.orange.game.draw.messagehandler.CleanDrawRequestHandler;
import com.orange.game.draw.messagehandler.DrawChatRequestHandler;
import com.orange.game.draw.messagehandler.DrawDataRequestHandler;
import com.orange.game.draw.messagehandler.DrawJoinGameRequestHandler;
import com.orange.game.draw.messagehandler.StartGameRequestHandler;
import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.messagehandler.ChatRequestHandler;
import com.orange.game.traffic.messagehandler.room.CreateRoomRequestHandler;
import com.orange.game.traffic.messagehandler.room.GetRoomRequestHandler;
import com.orange.game.traffic.messagehandler.room.JoinGameRequestHandler;
import com.orange.game.traffic.messagehandler.room.RegisterRoomsRequestHandler;
import com.orange.game.traffic.messagehandler.room.UnRegisterRoomsRequestHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.server.GameServerHandler;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.game.traffic.service.SessionUserService;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;

public class DrawGameServerHandler extends GameServerHandler {
//	private static final Logger logger = Logger.getLogger(DiceGameServerHandler.class.getName());

	@Override
	public AbstractMessageHandler getMessageHandler(MessageEvent messageEvent) {
		
		GameMessage message = (GameMessage)messageEvent.getMessage();
		
		switch (message.getCommand()){
		
			case SEND_DRAW_DATA_REQUEST:
				return new DrawDataRequestHandler(messageEvent);

			case CLEAN_DRAW_REQUEST:
				return new CleanDrawRequestHandler(messageEvent);
		
			case START_GAME_REQUEST:
				return new StartGameRequestHandler(messageEvent);
		
			case CREATE_ROOM_REQUEST:
				return new CreateRoomRequestHandler(messageEvent);
				
			case GET_ROOMS_REQUEST:
				return new GetRoomRequestHandler(messageEvent);
				
			case CHAT_REQUEST:
				return new DrawChatRequestHandler(messageEvent);

			case JOIN_GAME_REQUEST:
				return new DrawJoinGameRequestHandler(messageEvent);
				
			case REGISTER_ROOMS_NOTIFICATION_REQUEST:
				return new RegisterRoomsRequestHandler(messageEvent);
				
			case UNREGISTER_ROOMS_NOTIFICATION_REQUEST:
				return new UnRegisterRoomsRequestHandler(messageEvent);				
		}
		
		return null;
	}

}
