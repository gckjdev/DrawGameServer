package com.orange.game.draw.messagehandler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;

import com.orange.common.utils.StringUtil;
import com.orange.game.constants.DBConstants;
import com.orange.game.draw.model.DrawGameSession;
import com.orange.game.traffic.messagehandler.room.JoinGameRequestHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.game.traffic.model.manager.GameSessionAllocationManager;
import com.orange.game.traffic.model.manager.GameSessionManager;
import com.orange.game.traffic.server.GameEventExecutor;
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

		// use this to support new client interface
		PBGameUser pbUser = null;
		if (joinRequest.hasUser() && joinRequest.getUser() != null){
			pbUser = joinRequest.getUser();
		}
		else{
			pbUser = builder.build();
		}		
		
		GameSession session = null;
		if (joinRequest.hasTargetSessionId()){
			
			int sessionId;
			if (joinRequest.hasTargetSessionId()){
				sessionId = joinRequest.getTargetSessionId();
			}
			else {
				sessionId = (int) request.getSessionId();
			}
			
			session = GameSessionAllocationManager.getInstance().allocSession(userId, sessionId);			
		}
		else if (request.hasSessionId()){
			// has session id, alloc user into the session directly
			session = GameSessionAllocationManager.getInstance().allocSession(userId, (int)request.getSessionId());
		}
		else{					
			Set<Integer> excludeSessionSet = new HashSet<Integer>();
			if (request.getJoinGameRequest().hasSessionToBeChange()){
				
				// user quit current session	
				GameSession oldSession = GameEventExecutor.getInstance().getSessionManager().findSessionById((int)request.getJoinGameRequest().getSessionToBeChange());
				GameEventExecutor.getInstance().getSessionManager().userQuitSession(oldSession, userId, true, false);

				// create exclude session set
				List<Long> list = joinRequest.getExcludeSessionIdList();
				if (list != null){
					for (Long i : list){
						excludeSessionSet.add(i.intValue());
					}
				}
			}
			
			// no session id, alloc a session from session queue
			session = GameSessionAllocationManager.getInstance().allocSession(userId, excludeSessionSet);
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
		
		// update user game status
		sessionManager.updateUserEnterGameStatus(session, user);		
		return session;				
	}	
	
	@Override
	public void handleRequest(GameMessage message, Channel channel, GameSession requestSession) {
		processRequest(message, channel, requestSession);			
	}

	@Override
	public JoinGameResponse responseSpecificPart(JoinGameResponse.Builder builder,GameSession session) {
		
		JoinGameResponse response;
				
		// to adapt to old version
		String roomName = session.getName();
		if (StringUtil.isEmpty(roomName)){
			roomName = Integer.toString(session.getSessionId());
		}
		else{
			builder.getGameSessionBuilder().setName(roomName);
		}
		
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




	

