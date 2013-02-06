package com.orange.game.draw.model;

import java.util.ArrayList;
import java.util.List;
import com.orange.common.log.ServerLog;
import com.orange.game.draw.statemachine.DrawGameStateMachineBuilder;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCompleteReason;


public class DrawGameSession extends GameSession {

	// how many rounds this game has go for?
	private int playRound = 0;
	
	// Does next player's timer get decreased? 
	private boolean decreaseTimeForNextPlayUser = false;

	
	public DrawGameSession(int sessionId, String name, String password, boolean createByUser, String createBy, int ruleType, int maxPlayerCount, int testEnable) {
		super(sessionId, name, password, createByUser, createBy, ruleType, maxPlayerCount, testEnable);
		// init state
		this.currentState = DrawGameStateMachineBuilder.INIT_STATE;
	}
	
	
	public boolean isDrawGameSession(){
		// this is just for compatibility for online draw app
		return true;
	}
	
	public void setCurrentPlayUser(int index) {
		gameSessionUserList.selectCurrentPlayUser(index);
	}

	public void setDecreaseTimeForNextPlayUser(boolean b) {
		decreaseTimeForNextPlayUser  = b;		
	}

	public boolean getDecreaseTimeForNextPlayUser() {
		return decreaseTimeForNextPlayUser;
	}
	
	enum SessionStatus{
		INIT,
		WAIT,
		PLAYING		
	};
	
	SessionStatus status = SessionStatus.INIT;	

	DrawGameTurn currentTurn = null;		

	
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
	
	public void userGuessWord(GameUser user, String guessWord) {
		if (currentTurn == null || user == null)
			return;
		
		String guessUserId = user.getUserId();
		ServerLog.info(sessionId, "user " + guessUserId + " guess " + guessWord);			
		currentTurn.userGuessWord(user, guessWord);
	}

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

	volatile boolean isDrawGuessing = false;
	
	public synchronized void setDrawGuessing() {
		isDrawGuessing = true;
	}
	
	public synchronized void clearDrawGuessing() {
		isDrawGuessing = false;
	}
	
	@Override
	public synchronized boolean canAllocate() {
		return (isDrawGuessing == false);  // if it's in draw & guess status, cannot allocate this session
	}
	
}
