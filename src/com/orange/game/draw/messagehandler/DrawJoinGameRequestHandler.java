package com.orange.game.draw.messagehandler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;

import com.orange.game.constants.DBConstants;
import com.orange.game.draw.model.DrawGameSession;
import com.orange.game.traffic.messagehandler.room.JoinGameRequestHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.game.traffic.model.manager.GameSessionAllocationManager;
import com.orange.game.traffic.server.HandlerUtils;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.game.traffic.service.SessionUserService;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.JoinGameRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.JoinGameResponse;
import com.orange.network.game.protocol.message.GameMessageProtos.UserDiceNotification;
import com.orange.network.game.protocol.model.GameBasicProtos;
import com.orange.network.game.protocol.model.GameBasicProtos.PBGameUser;
import com.orange.network.game.protocol.model.GameBasicProtos.PBSNSUser;

public class DrawJoinGameRequestHandler extends JoinGameRequestHandler {

	public DrawJoinGameRequestHandler(MessageEvent event) {
		super(event);
	}
	
	@Override
	protected GameSession processRequest(GameMessage request, Channel channel, GameSession requestSession){
		JoinGameRequest joinRequest = request.getJoinGameRequest();
		
		String userId = joinRequest.getUserId();
					
		String nickName = joinRequest.getNickName();			
		String avatar = joinRequest.getAvatar();
		boolean gender = false;
		if (joinRequest.hasGender()){
			gender = request.getJoinGameRequest().getGender();
		}		
		String location = joinRequest.getLocation();
		List<PBSNSUser> snsUser = joinRequest.getSnsUsersList();

		int guessDifficultLevel = 1;
		if (joinRequest.hasGuessDifficultLevel())
			guessDifficultLevel = joinRequest.getGuessDifficultLevel(); 		

		PBGameUser.Builder builder = PBGameUser.newBuilder();
		builder.setUserId(userId);
		builder.setNickName(nickName);
		if (avatar != null){
			builder.setAvatar(avatar);
		}
		builder.setGender(gender);
		if (location != null){
			builder.setLocation(location);
		}
		if (snsUser != null){
			builder.addAllSnsUsers(snsUser);
		}
		PBGameUser pbUser = builder.build();

		
		
//		int gameSessionId = -1;
//		GameUser user = new GameUser(userId, nickName, avatar, gender,
//				location, snsUser, channel, gameSessionId, 
//				joinRequest.getIsRobot(), guessDifficultLevel, joinRequest.getUserLevel());		
		
//		if (joinRequest.hasRoomId()){
//			String roomId = joinRequest.getRoomId();
//			String roomName = joinRequest.getRoomName();
//			GameSession session = gameManager.allocFriendRoom(roomId, roomName, user);
//			if (session == null){
//				HandlerUtils.sendErrorResponse(request, GameResultCode.ERROR_NO_SESSION_AVAILABLE, channel);
//				return;
//			}
//			else{
//				gameSessionId = session.getSessionId();
//			}						
//		}
		
		GameSession session = null;
		if (joinRequest.hasTargetSessionId() || request.hasSessionId()){
			
			int sessionId;
			if (joinRequest.hasTargetSessionId()){
				sessionId = joinRequest.getTargetSessionId();
			}
			else {
				sessionId = (int) request.getSessionId();
			}
			
			session = GameSessionAllocationManager.getInstance().allocSession(userId, sessionId);
			
//			gameSessionId = joinRequest.getTargetSessionId();
//			boolean isRobot = false;
//			if (joinRequest.hasIsRobot()){
//				isRobot = joinRequest.getIsRobot();
//			}
//			
//			GameResultCode result = gameManager.directPutUserIntoSession(user, gameSessionId);
//			if (result != GameResultCode.SUCCESS){
//				HandlerUtils.sendErrorResponse(request, result, channel);
//				return;
//			}					
		}
		else{		
			
			// no session id, alloc a session from session queue
			session = GameSessionAllocationManager.getInstance().allocSession(userId);

			
//			GameSession session = GameSessionManager.getInstance().findGameSessionById((int)request.getJoinGameRequest().getSessionToBeChange());
//			
//			Set<Integer> excludeSessionSet = new HashSet<Integer>();
//			if (request.getJoinGameRequest().hasSessionToBeChange()){
//				
//				// user quit current session
//				gameManager.userQuitSession(userId, session, true);
//
//				// create exclude session set
//				List<Long> list = joinRequest.getExcludeSessionIdList();
//				if (list != null){
//					for (Long i : list){
//						excludeSessionSet.add(i.intValue());
//					}
//				}
//			}
//			
//			gameSessionId = gameManager.allocGameSessionForUser(user, excludeSessionSet);
//			if (gameSessionId == -1){
//				HandlerUtils.sendErrorResponse(request, GameResultCode.ERROR_NO_SESSION_AVAILABLE, channel);
//				return;
//			}
		}
		
		if (session == null){
			HandlerUtils.sendErrorResponse(request, GameResultCode.ERROR_NO_SESSION_AVAILABLE, channel);
			return null;
		}
		
		// add user into session
		GameUser user = SessionUserService.getInstance().addUserIntoSession(session, pbUser, channel, request);
		
		// for draw guess game, all user is in playing state by default
		user.setPlaying(true);
		
		// send response
		sendResponseForUserJoin(session, user.getPBUser(), channel, request);
		
		return session;
		
		// TODO add online user count in response
//		int onlineUserCount = UserManager.getInstance().getOnlineUserCount();
		
		// send back response
//		List<GameBasicProtos.PBGameUser> pbGameUserList = sessionUserManager.usersToPBUsers(gameSessionId);	
//		GameBasicProtos.PBGameSession gameSessionData = GameBasicProtos.PBGameSession.newBuilder()		
//										.setGameId(DBConstants.DRAW_GAME_ID)
//										.setCurrentPlayUserId(gameSession.getCurrentPlayUserId())
//										.setNextPlayUserId("")
//										.setHost(gameSession.getHost())
//										.setName(gameSession.getName())
//										.setSessionId(gameSession.getSessionId())
//										.addAllUsers(pbGameUserList)										
//										.build();
//
//		GameMessageProtos.JoinGameResponse joinGameResponse = GameMessageProtos.JoinGameResponse.newBuilder()
//										.setGameSession(gameSessionData)
//										.build();
//		
//		GameMessageProtos.GameMessage response = GameMessageProtos.GameMessage.newBuilder()
//					.setCommand(GameCommandType.JOIN_GAME_RESPONSE)
//					.setMessageId(request.getMessageId())
//					.setResultCode(GameResultCode.SUCCESS)
//					.setOnlineUserCount(onlineUserCount)
//					.setJoinGameResponse(joinGameResponse)
//					.build();
//
//		GameEvent gameEventForResponse = new GameEvent(
//				GameCommandType.JOIN_GAME_REQUEST, 
//				gameSessionId, 
//				request, 
//				channel);
//		HandlerUtils.sendResponse(gameEventForResponse, response);			
		
		// send notification to all other users in the session
//		GameNotification.broadcastUserJoinNotification(gameSession, userId, gameEventForResponse);	
					
		
		
	}
	
//	@Override
//	protected GameSession processRequest(GameMessage message, Channel channel, GameSession requestSession){
//		// init data
//		JoinGameRequest request = message.getJoinGameRequest();
//		String userId = request.getUserId();
//		PBGameUser pbUser = request.getUser();
//
//		GameSession session = null;
//		if (message.hasSessionId()){
//			// has session id, alloc user into the session directly
//			session = GameSessionAllocationManager.getInstance().allocSession(userId, (int)message.getSessionId());
//		}
//		else{
//			// no session id, alloc a session from session queue
//			session = GameSessionAllocationManager.getInstance().allocSession(userId);
//		}
//		
//		if (session == null){
//			HandlerUtils.sendErrorResponse(message, GameResultCode.ERROR_NO_SESSION_AVAILABLE, channel);
//			return null;
//		}
//		
//		// add user into session
//		GameUser user = SessionUserService.getInstance().addUserIntoSession(session, pbUser, channel, message);		
//		
//		// send response
//		sendResponseForUserJoin(session, user.getPBUser(), channel, message);
//		
//		return session;
//	}

	
	@Override
	public void handleRequest(GameMessage message, Channel channel, GameSession requestSession) {
		processRequest(message, channel, requestSession);			
	}

	@Override
	public JoinGameResponse responseSpecificPart(JoinGameResponse.Builder builder,GameSession session) {
		
		JoinGameResponse response;
				
		// TODO how to adapt to new version?
		builder.getGameSessionBuilder().setName(Integer.toString(session.getSessionId()));
		
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




	

