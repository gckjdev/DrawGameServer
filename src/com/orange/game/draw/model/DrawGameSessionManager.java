package com.orange.game.draw.model;

import com.orange.game.constants.DBConstants;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.game.traffic.model.manager.GameSessionManager;
import com.orange.network.game.protocol.constants.GameConstantsProtos.DiceGameRuleType;

public class DrawGameSessionManager extends GameSessionManager {

	private static final int MAX_PLAYER_PER_SESSION = 6;
	@Override
	public GameSession createSession(int sessionId, String name, String password, boolean createByUser, String createBy, int ruleType, int maxPlayerCount, int testEnable) {
		return new DrawGameSession(sessionId, name, password, createByUser, createBy, ruleType, maxPlayerCount, testEnable);
	}

	/*
	@Override
	public void userQuitSession(GameSession session, String userId, boolean needFireEvent, boolean needRemoveUserChannel) {
		
		if (session == null || userId == null){
			return;
		}
		
		int sessionId = session.getSessionId();
		ServerLog.info(sessionId, "user "+userId+" quit");

		int sessionUserCount = session.getUserCount();
		GameUser user = session.findUser(userId);
		if (user == null){
			ServerLog.info(sessionId, "user "+userId+" quit, but user not found in session");
			return;
		}
				
		boolean removeUserFromSession = true;		// always true here
		
		GameCommandType command = null;		
		if (session.isCurrentPlayUser(userId)){
			command = GameCommandType.LOCAL_PLAY_USER_QUIT;			
		}
		else if (sessionUserCount <= 2){
			command = GameCommandType.LOCAL_ALL_OTHER_USER_QUIT;			
		}
		else {
			command = GameCommandType.LOCAL_OTHER_USER_QUIT;						
		}			
		
		// broadcast user exit message to all other users
		NotificationUtils.broadcastUserStatusChangeNotification(session, userId);			
		
		// fire message
		if (needFireEvent){
			GameEventExecutor.getInstance().fireAndDispatchEvent(command, sessionId, userId);
		}
		
		if (removeUserFromSession){
			SessionUserService.getInstance().removeUser(session, userId, needRemoveUserChannel);
		}
	}
	*/

	@Override
	public String getGameId() {
		return DBConstants.GAME_ID_DRAW;
	}
			
	// from GameConstantProtos
	// RULE_NORMAL_VALUE = 0;
	// RULE_HIGH_VALUE = 1;
	// RULE_SUPER_HIGH_VALUE = 2;
	static int ruleType = loadRuleTypeFromConfig();		
	public static int loadRuleTypeFromConfig() {
		String ruleType = System.getProperty("ruletype");
		if (ruleType != null && !ruleType.isEmpty()){
			return Integer.parseInt(ruleType);
		}
		return DiceGameRuleType.RULE_NORMAL_VALUE; // default
	}

	@Override
	public int getRuleType() {
		return ruleType;
	}
	
	@Override
	// On: 1, Off:0[default]
	public int getTestEnable() {
			String testEnable = System.getProperty("test_enable");
			if (testEnable != null && !testEnable.isEmpty()){
				return Integer.parseInt(testEnable);
			}
			return 0;
	}

	@Override
	public void updateQuitUserInfo(GameSession session, GameUser quitUser) {
	}

	@Override
	public boolean takeOverWhenUserQuit(GameSession session, GameUser quitUser,
			int sessionUserCount) {
		return false;
	}

	@Override
	public int getMaxPlayerCount() {
		return readMaxPlayerCount(MAX_PLAYER_PER_SESSION);
	}
	
}
