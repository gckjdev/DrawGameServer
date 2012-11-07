package com.orange.game.draw.model;

import java.util.ArrayList;
import java.util.List;
import com.orange.common.log.ServerLog;
import com.orange.game.draw.statemachine.DrawGameStateMachineBuilder;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.network.game.protocol.constants.GameConstantsProtos.DiceGameRuleType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCompleteReason;


public class DrawGameSession extends GameSession {

	// how many rounds this game has go for?
	private int playRound = 0 ;
	
	// Does next player's timer get decreased? 
	private boolean decreaseTimeForNextPlayUser = false;

	
	public DrawGameSession(int sessionId, String name, String password, boolean createByUser, String createBy, int ruleType,int testEnable) {
		super(sessionId, name, password, createByUser, createBy, ruleType, testEnable);
		// init state
		this.currentState = DrawGameStateMachineBuilder.INIT_STATE;
	}
	
	
	public boolean isDrawGameSession(){
		// this is just for compatibility for online draw app
		return true;
	}
	
//	public void resetGame(){
//		super.resetGame();
//	}
	
//	@Override	
//	public void restartGame(){	
//		clearTimer();
//		decreaseTimeForNextPlayUser = false;
//	}
	
	public void setCurrentPlayUser(int index) {
		gameSessionUserList.selectCurrentPlayUser(index);
	}

	public void setDecreaseTimeForNextPlayUser(boolean b) {
		decreaseTimeForNextPlayUser  = b;		
	}

	public boolean getDecreaseTimeForNextPlayUser() {
		return decreaseTimeForNextPlayUser;
	}
	
//	private int getDiceCallCeiling() {
//		
//		int ruleType = getRuleType();
//		int playUserCount = getPlayUserCount();
//		
//		return playUserCount * (ruleType == DiceGameRuleType.RULE_NORMAL_VALUE ? 5 :7);
//		
//	}
	
	enum SessionStatus{
		INIT,
		WAIT,
		PLAYING		
	};
	
	SessionStatus status = SessionStatus.INIT;	

	DrawGameTurn currentTurn = null;		

	
//	public boolean isStart() {
//		return (status == SessionStatus.PLAYING);
//	}

//	public void startGame(){
//		super.startGame();
////		status = SessionStatus.PLAYING;
////		ServerLog.info(sessionId, "start game, set status to " + status);
//	}
	
//	public void finishGame(){
//		status = SessionStatus.WAIT;
//		clearTimer();
////		clearStartExpireTimer();
//		ServerLog.info(sessionId, "finish game, set status to " + status);
//	}
	
//	public void resetGame() {
//		super.resetGame();
//		status = SessionStatus.INIT;
//		clearTimer();
////		this.resetExpireTimer();
////		clearStartExpireTimer();
//		ServerLog.info(sessionId, "reset game, set status to " + status);
//	}

//	public void waitForPlay() {
//		status = SessionStatus.WAIT;
//		clearTimer();
////		this.resetExpireTimer();
//		ServerLog.info(sessionId, "wait for play, set status to " + status);
//	}
	
	public synchronized void startNewTurn(String word, int level, int language){
		if (currentTurn == null){
			currentTurn = new DrawGameTurn(sessionId, 1, word, level, language, this.getCurrentPlayUser());
		}
		else{
			currentTurn.storeDrawData();
			currentTurn = new DrawGameTurn(sessionId, currentTurn.getRound() + 1, word, level, language, this.getCurrentPlayUser());
		}		
		ServerLog.info(sessionId, "start new game turn "+currentTurn.getRound(), "word=" + word);
	}
	
	public synchronized boolean isGameTurnPlaying(){
		if (currentTurn == null){			
			return (this.status == SessionStatus.PLAYING);
		}
		
		return currentTurn.isTurnPlaying();
	}	

	public int getCurrentRound() {
		if (currentTurn == null)
			return 1;
		
		return currentTurn.getRound();
	}

	public String getCurrentGuessWord() {
		if (currentTurn == null)
			return "";
		
		return currentTurn.getWordText();
	}

//	public void resetExpireTimer(){
//		if (this.expireTimer != null){
//			ServerLog.info(sessionId, "cancel & clear expire timer");			
//			this.expireTimer.cancel();
//			this.expireTimer = null;
//		}		
//	}
//	
//	public void setExpireTimer(Timer timer) {
//		if (this.expireTimer != null){
//			this.expireTimer.cancel();
//			this.expireTimer = null;
//		}
//		
//		this.expireTimer = timer;
//	}


	public void userGuessWord(GameUser user, String guessWord) {
		if (currentTurn == null || user == null)
			return;
		
		String guessUserId = user.getUserId();
		ServerLog.info(sessionId, "user " + guessUserId + " guess " + guessWord);			
		currentTurn.userGuessWord(user, guessWord);
	}

//	public synchronized boolean isCurrentPlayUser(String userId) {
//		GameUser
//		if (currentPlayUser == null || userId == null)
//			return false;
//				
//		return currentPlayUser.userId.equals(userId);
//	}

//	Timer startExpireTimer = null;
//	static final int DEFAULT_START_EXPIRE_TIMER = 32*1000;
//	
//	public void clearStartExpireTimer(){
//		if (startExpireTimer != null){
//			ServerLog.info(sessionId, "Clear start expire timer");			
//			startExpireTimer.cancel();
//			startExpireTimer = null;
//		}
//	}
//	
//	@Deprecated
//	public void scheduleStartExpireTimer(final String userId){
//				
//		clearStartExpireTimer();		
//
//		ServerLog.info(sessionId, "Scheule start expire timer on userId="+userId);
//		startExpireTimer = new Timer();
//		startExpireTimer.schedule(new TimerTask(){
//
//			@Override
//			public void run() {
//				try{
//					ServerLog.info(sessionId, "Fire start expire timer on userId="+userId);
//					User user = UserManager.getInstance().findUserById(userId);
//					if (user == null){
//						// user already disconnect?
//						GameService.getInstance().fireUserTimeOutEvent(sessionId, userId, null);					
//					}
//					else{
//						GameService.getInstance().fireUserTimeOutEvent(sessionId, userId, user.getChannel());
//					}
//				}
//				catch (Exception e){
//					ServerLog.error(sessionId, e, "Exception while fire start expire timer on userId="+userId);
//				}
//				
//				startExpireTimer = null;
//			}
//			
//		}, DEFAULT_START_EXPIRE_TIMER);
//	}
		
//	public void startExpireTimerIfNeeded() {
//		if (this.currentPlayUser != null && startExpireTimer == null){
//			scheduleStartExpireTimer(currentPlayUser.getUserId());
//		}
//	}

//	public void resetStartExpireTimer() {
//		GameUser currentPlayUser = this.getCurrentPlayUser();
//		if (currentPlayUser != null){
//			scheduleStartExpireTimer(currentPlayUser.getUserId());
//		}
//	}
//	
//	public void startStartExpireTimerIfNeeded() {
//		if (startExpireTimer == null){
//			if (this.isGameTurnPlaying() == false){			
//				scheduleStartExpireTimer(currentPlayUser.getUserId());
//			}
//		}
//	}
	
//	public synchronized void setCurrentPlayUser(User user, int userIndex){
//		this.currentPlayUser = user;
//		this.currentPlayUserIndex = userIndex;
//		ServerLog.info(sessionId, "current play user is set to "+user);		
		
//		if (user != null){
//			// set a start timer here
//			scheduleStartExpireTimer(user.getUserId());
//		}
//	}
	
	/*
	@Deprecated
	private synchronized void setCurrentPlayUser(User user){
		this.currentPlayUser = user;
		ServerLog.info(sessionId, "current play user is set to "+user);		
		
//		if (user != null){
//			// set a start timer here
//			scheduleStartExpireTimer(user.getUserId());
//		}
	}
	*/

	private boolean isAllUserGuessWord(List<String> userIdList) {
		if (currentTurn == null){
			ServerLog.warn(sessionId, "call isAllUserGuessWord but current turn is null?");
			return false;
		}
		
		return currentTurn.isAllUserGuessWord(userIdList);
	}

	public int getCurrentGuessUserCoins(String userId) {
		if (currentTurn == null)
			return 0;
		
		UserGuessWord uw = currentTurn.userGuessWordMap.get(userId);
		if (uw == null)
			return 0;
		
		return uw.finalCoins;
	}

	public void calculateDrawUserCoins() {
		if (currentTurn == null){
			return;
		}
		
		GameUser currentPlayUser = getCurrentPlayUser();
		if (currentPlayUser == null){
			return;
		}
		
		currentTurn.calculateDrawUserCoins(currentPlayUser);
	}
	
	public int getDrawUserCoins(){
		if (currentTurn == null){
			return 0;
		}

		return currentTurn.drawUserCoins;
	}

	public int getCurrentUserGainCoins(String userId) {
		if (currentTurn == null)
			return 0;
				
		return currentTurn.getUserFinalCoins(userId);
	}

	public void completeTurn(GameCompleteReason completeReason) {
		if (this.currentTurn == null)
			return;
		
		ServerLog.info(sessionId, "<completeTurn> on session " + sessionId + " reason=" + completeReason);
		currentTurn.completeTurn(completeReason);
	}

	public void completeTurn() {
		if (this.currentTurn == null)
			return;
		
		ServerLog.info(sessionId, "<completeTurn> on session " + sessionId + " reason=" + 
				currentTurn.getCompleteReason());
		currentTurn.completeTurn(currentTurn.getCompleteReason());
	}
	
	public GameCompleteReason getCompleteReason() {
		if (this.currentTurn == null)
			return GameCompleteReason.REASON_NOT_COMPLETE;

		return currentTurn.completeReason;
	}
	
	public void setCompleteReason(GameCompleteReason reason){
		if (this.currentTurn == null)
			return;
		
		currentTurn.setCompleteReason(reason);
	}

//	public User getCurrentPlayUser() {
//		return this.currentPlayUser;
//	}

//	private ScheduledFuture timeOutFuture = null;
//	
//	public void setTimeOutFuture(ScheduledFuture future) {
//		if (timeOutFuture != null){
//			timeOutFuture.cancel(false);
//			timeOutFuture = null;
//		}
//		
//		timeOutFuture = future;
//	}
//
//	public void clearTimeOutFuture(ScheduledFuture future) {
//		if (timeOutFuture != null){
//			timeOutFuture.cancel(false);
//			timeOutFuture = null;
//		}		
//	}
//
//	ScheduledFuture<Object> inviteRobotTimer = null;
		
//	public void setRobotTimeOutFuture(ScheduledFuture<Object> future) {
//		if (inviteRobotTimer != null){
//			inviteRobotTimer.cancel(false);
//			inviteRobotTimer = null;
//		}
//		
//		inviteRobotTimer = future;
//	}
//
//	public void clearRobotTimer() {
//		if (inviteRobotTimer != null){
//			inviteRobotTimer.cancel(false);
//			inviteRobotTimer = null;
//		}		
//	}

	public void appendDrawData(List<Integer> pointsList, int color, float width) {
		if (currentTurn == null)
			return;

		currentTurn.appendDrawData(pointsList, color, width);
	}

	public void appendCleanDrawAction() {
		if (currentTurn == null)
			return;
		
		currentTurn.appendCleanDrawAction();
	}

//	public String getFriendRoomId() {
//		return this.friendRoomId;
//	}
	
//	public void clearTimer() {
//		if (commonTimerFuture != null){
//			commonTimerFuture.cancel(false);
//			commonTimerFuture = null;
//		}
//	}

//	public void setTimer(ScheduledFuture<Object> future) {
//		this.commonTimerFuture = future;
//	}	
	
	public boolean isAllUserGuessWord(){
		List<String> userIdList = new ArrayList<String>();
		List<GameUser> userList = getUserList().getPlayingUserList();
		for (GameUser user : userList){
			if (user.isPlaying() && user != this.getCurrentPlayUser()){
				userIdList.add(user.getUserId());
			}
		}
		
		return this.isAllUserGuessWord(userIdList);
	}
}
