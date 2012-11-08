package com.orange.game.draw.robot.client;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.orange.common.log.ServerLog;
import com.orange.common.mongodb.MongoDBClient;
import com.orange.common.utils.RandomUtil;
import com.orange.game.constants.DBConstants;
import com.orange.game.constants.ServiceConstant;
import com.orange.game.draw.model.DrawAction;
import com.orange.game.draw.model.WordManager;
import com.orange.game.draw.server.DrawOnlineServer;
import com.orange.game.draw.service.DrawStorageService;
import com.orange.game.model.dao.Item;
import com.orange.game.model.dao.User;
import com.orange.game.model.manager.UserManager;
import com.orange.game.traffic.robot.client.AbstractRobotClient;
import com.orange.game.traffic.robot.client.AbstractRobotManager;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos.CallDiceRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.GameChatRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.GeneralNotification;
import com.orange.network.game.protocol.message.GameMessageProtos.OpenDiceRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.SendDrawDataRequest;
import com.orange.network.game.protocol.model.DiceProtos.PBDice;
import com.orange.network.game.protocol.model.DiceProtos.PBUserDice;
import com.orange.network.game.protocol.model.DrawProtos.PBDraw;
import com.orange.network.game.protocol.model.GameBasicProtos.PBDrawAction;
import com.orange.network.game.protocol.model.GameBasicProtos.PBGameUser;
import com.orange.network.game.protocol.model.GameBasicProtos.PBKeyValue;
import com.orange.network.game.protocol.model.GameBasicProtos.PBGameUser.Builder;

public class DrawRobotClient extends AbstractRobotClient {

	private final static Logger logger = Logger.getLogger(DrawRobotClient.class.getName());
	
	String openUserId = null;
	String callUserId = null;
	int callDice = -1;
	int callDiceNum = -1;
	boolean callDiceIsWild = false;
	private int callUserSeatId = -1;
	private boolean canOpenDice = false;
	private int playerCount = 0;
	private boolean robotWinThisGame = false;
	private boolean firstRound = true;
	
	// for itemType
	private final static int ITEM_TYPE_DICE_MIN = 2500;
	private final static int ITEM_TYPE_DICE_MAX = 2512;
	
	// chatContent type
	private final static int TEXT = 1;
	private final static int EXPRESSION = 2;
	
	private final static int IDX_CONTENT = 0;
	private final static int IDX_CONTENTID = 1;
	private final static int IDX_CONTNET_TYPE = 2;

	// What dices robot gets
	int[] robotRollResult={0,0,0,0,0,0};
	
	DrawRobotChatContent chatContent = DrawRobotChatContent.getInstance();
	
	List<PBUserDice> pbUserDiceList = null;
	List<PBDice> pbDiceList = null;
	
	ScheduledFuture<?> callDiceFuture = null;
	ScheduledFuture<?> openDiceFuture = null;
	
	ScheduledFuture<?> rollEndChatFuture = null;
	volatile static int rollEndChatLock = 2;
	private ScheduledFuture<?> chatFuture = null;
	
	int ruleType = GameEventExecutor.getInstance().getSessionManager().getRuleType();
	DrawRobotIntelligence diceRobotIntelligence = new DrawRobotIntelligence(ruleType);
	DrawRobotChatContent diceRobotChatContent = DrawRobotChatContent.getInstance();
	
	int round = -1;
	String word = null;
	int level = 0;
	int language = 0;
	int guessCount = 0;
	boolean guessCorrect = false;
	
	// simulation
	Timer guessWordTimer;
	PBDraw pbDraw;
	
	public DrawRobotClient(User user, int sessionId, int index) {
		super(user, sessionId,index);
		oldExp = experience = user.getExpByAppId(DBConstants.APPID_DICE);
		level = user.getLevelByAppId(DBConstants.APPID_DICE); 
		balance = user.getBalance();
	}
	
	// for draw game compatibility
	@Override
	protected boolean isForDrawGame(){
		return true;
	}
	
	@Override
	public void handleMessage(GameMessage message){
		
		switch (message.getCommand()){
		
//		case JOIN_GAME_RESPONSE:
//			handleJoinGameResponse(message);
//			break;
						
		case START_GAME_RESPONSE:
			break;
			
		case NEW_DRAW_DATA_NOTIFICATION_REQUEST:
			handleDrawDataNotification(message);
			break;
			
		case GAME_TURN_COMPLETE_NOTIFICATION_REQUEST:			
			handleGameTurnCompleteNotification(message);
			break;
			
		case GAME_START_NOTIFICATION_REQUEST:
			handleGameStartNotification(message);
			break;

		case USER_JOIN_NOTIFICATION_REQUEST:			
			handleUserJoinNotification(message);
			break;
			
		case USER_QUIT_NOTIFICATION_REQUEST:
			handleUserQuitNotification(message);
			break;
		}	
	}

	private void scheduleSendCallDice(final int[] whatToCall) {
		
		if (callDiceFuture != null){
			callDiceFuture.cancel(false);
		}
		
		callDiceFuture = scheduleService.schedule(new Runnable() {			
			@Override
			public void run() {
				sendCallDice(whatToCall);
			}
		}, 
		RandomUtils.nextInt(5)+1, TimeUnit.SECONDS);
	}

	private void sendCallDice(final int[] whatToCall) {
		
		
		int diceNum = whatToCall[0];
		int dice = whatToCall[1];
		boolean isWild = (whatToCall[2] == 1 ? true : false);
		
		// send call dice request here
		CallDiceRequest request = CallDiceRequest.newBuilder()
			.setDice(dice)
			.setNum(diceNum)
			.setWilds(isWild)
			.build();

		GameMessage message = GameMessage.newBuilder()
			.setMessageId(getClientIndex())
			.setCommand(GameCommandType.CALL_DICE_REQUEST)
			.setSessionId(sessionId)
			.setUserId(userId)
			.setCallDiceRequest(request)
			.build();
		
		send(message);
	}

	
	private void scheduleSendOpenDice(final int openType, final int multiple) {
		
		if (openDiceFuture != null){
			openDiceFuture.cancel(false);
		}
		
		openDiceFuture = scheduleService.schedule(new Runnable() {			
			@Override
			public void run() {
				sendOpenDice(openType, multiple);
			}
		}, 
		RandomUtils.nextInt(2)+1, TimeUnit.SECONDS);
	}

	private void sendOpenDice(int openType, int multiple) {
		ServerLog.info(sessionId, "Robot "+nickName+" open dice");
		
		OpenDiceRequest request = OpenDiceRequest.newBuilder()
				.setOpenType(openType)
				.build();
		GameMessage message = GameMessage.newBuilder()
			.setOpenDiceRequest(request)
			.setMessageId(getClientIndex())
			.setCommand(GameCommandType.OPEN_DICE_REQUEST)
			.setUserId(userId)
			.setSessionId(sessionId)
			.build();
		
		send(message);		
	}
	
	
	
	public void sendChat(final String[] content) {
		
		// index IDX_CONTENT(0) : content(only valid for TEXT)
		// index IDX_CONTENTID(1) : content voiceId or expressionId, depent on contentType
		// index IDX_CONTENT_TYPE(2) : contentType, TEXT or EXPRESSION
		String chatContent = content[IDX_CONTENT];
		String contentId = content[IDX_CONTENTID];
		int contentType = Integer.parseInt(content[IDX_CONTNET_TYPE]);
		
		ServerLog.info(sessionId, "Robot "+nickName+" sends chat content: " + contentId);
		
		GameChatRequest request = null;
		
		GameChatRequest.Builder builder = GameChatRequest.newBuilder()
				.setContentType(contentType) // 1: text, 2: expression
				.setContent(chatContent); // will be ignored when contentType is 2
		if ( contentType == TEXT ) {
				builder.setContentVoiceId(contentId);
		} else {
				builder.setExpressionId(contentId);
		}
		
		request = builder.build();
				
		GameMessage message = GameMessage.newBuilder()
			.setChatRequest(request)
			.setMessageId(getClientIndex())
			.setCommand(GameCommandType.CHAT_REQUEST)
			.setUserId(userId)
			.setSessionId(sessionId)
			.build();
		
		ServerLog.info(sessionId, "<DiceRobotChatContent.sendChat()>Robot "+nickName+ " sends "+message.getCommand());
		send(message);		
	}
	
	
	public void scheduleSendChat(ScheduledFuture<?> chatFuture, int delay) {
		
		if (chatFuture != null){
			chatFuture.cancel(false);
		}
		
		// index 0: contentType
		// index 1: content( only valid for TEXT)
		// index 2: contentVoiceId or expressionId,depent on contentType
		
		chatFuture = scheduleService.schedule(new Runnable() {			
			@Override
			public void run() {
				String[] tmp = {null, null};
				tmp = chatContent.getExpressionByMeaning("NEGATIVE");
				String[] expression = {null, null, Integer.toString(EXPRESSION)};
				expression[0] = tmp[0];
				expression[1] = tmp[1];
				sendChat(expression);
			}
		}, 
		delay, TimeUnit.SECONDS);
		
	}
	

	@Override
	public void resetPlayData(boolean robotWinThisGame) {
		
		openUserId = null;
		callUserId = null;
		callDice = -1;
		callDiceNum = -1;
		callDiceIsWild = false;
		callUserSeatId = -1;
		
		canOpenDice = false;
		pbUserDiceList = null;
		pbDiceList = null;
		
		openDiceFuture = null;
		chatFuture = null;
		
		this.robotWinThisGame = robotWinThisGame;
		firstRound = false;
		rollEndChatLock = 2;
	}
	
	public String getAppId() {
		return DBConstants.APPID_DRAW;
	}
	
	@Override
	public String getGameId() {
		return DBConstants.GAME_ID_DRAW;
	}	
	
	@Override
	public PBGameUser toPBGameUserSpecificPart(Builder builder) {
		
		List<Item> items = user.getItems();
		if ( items.size() > 0 ) {
			int diceItemType = ITEM_TYPE_DICE_MIN;
			for ( Item item: items ) {
				int itemType = item.getItemType();
				if ( itemType > ITEM_TYPE_DICE_MIN && itemType < ITEM_TYPE_DICE_MAX ) {
					diceItemType = item.getItemType();
					break;
				}
			}
			PBKeyValue pbKeyValue = PBKeyValue.newBuilder()
					.setName("CUSTOM_DICE")
					.setValue(Integer.toString(diceItemType-2500)) // should substract by 2500, required by the client
					.build();
			
			builder.addAttributes(pbKeyValue);
			logger.info("<DiceRobotClient.toPBGameUserSpecificPart> Robot["+ nickName+"] adds a dice item, itemType is " + diceItemType);
		}
		
		return builder.build();
	}
	
	private void handleStartGameResponse(GameMessage message) {
//		service.sendStartDraw(user, "杯子", 1);
		if (message.getResultCode() != GameResultCode.SUCCESS){
			ServerLog.info(sessionId, "start game but response code is "+message.getResultCode());
			disconnect();
		}
	}
	
	private void handleGameTurnCompleteNotification(GameMessage message) {
		setState(ClientState.WAITING);
		updateByNotification(message.getNotification());		
		resetPlayData();
		
		if (canQuitNow()){
			ServerLog.info(sessionId, "reach min user for session, robot can escape now!");
			disconnect();
			return;
		}
		
		checkStart();

	}

	private void handleDrawDataNotification(GameMessage message) {
		updateTurnData(message.getNotification());
		
		if (message.getNotification() == null)
			return;
			
		String word = message.getNotification().getWord();
		if (word != null && word.length() > 0){
			setState(ClientState.PLAYING);
			resetPlayData();

			// now here need to simulate guess word...
			setGuessWordTimer();
		}
		
	}

	private void handleGameStartNotification(GameMessage message) {
		if (state != ClientState.PLAYING){
			setState(ClientState.PICK_WORD);
		}
		updateByNotification(message.getNotification());								
	}

	private void handleUserJoinNotification(GameMessage message) {		
		updateByNotification(message.getNotification());						
		checkStart();		
	}	

	private void handleUserQuitNotification(GameMessage message) {
		String userId = message.getNotification().getQuitUserId();
		if (userId == null){
			return;
		}
		
		removeUserByUserId(userId);
		if (sessionRealUserCount() <= 0){
			// no other users, quit robot
			sendQuitGameRequest();
			disconnect();
		}
		
		checkStart();		
	}	
	
	public void updateTurnData(GeneralNotification notification) {
		if (notification == null)
			return;
		
		if (notification.hasRound())
			this.round = notification.getRound();
		
		if (notification.hasWord() && notification.getWord().length() > 0)
			this.word = notification.getWord();
		
		if (notification.hasLevel())
			this.level = notification.getLevel();
		
		if (notification.hasLanguage() && notification.getLanguage() > 0)
			this.language = notification.getLanguage();
		
	}
	
	public final void sendGuessWord(String guessWord){
		SendDrawDataRequest request = SendDrawDataRequest.newBuilder().setGuessWord(guessWord)
			.setGuessUserId(userId)
			.build();
		
		GameMessage message = GameMessage.newBuilder().setCommand(GameCommandType.SEND_DRAW_DATA_REQUEST)
			.setMessageId(getClientIndex())
			.setUserId(userId)
			.setSessionId(sessionId)
			.setSendDrawDataRequest(request)
			.build();
		
		send(message);
	}
	
	public final void sendStartGame(){		
		GameMessage message = GameMessage.newBuilder().setCommand(GameCommandType.START_GAME_REQUEST)
			.setMessageId(getClientIndex())
			.setUserId(userId)
			.setSessionId(sessionId)
			.build();
		
		send(message);
	}

	public final void cleanDraw(){		
		GameMessage message = GameMessage.newBuilder().setCommand(GameCommandType.CLEAN_DRAW_REQUEST)
			.setMessageId(getClientIndex())
			.setUserId(userId)
			.setSessionId(sessionId)
			.build();
		
		send(message);
	}
	
	public final void sendStartDraw(String word, int level, int language){
		SendDrawDataRequest request = SendDrawDataRequest.newBuilder().setWord(word)
			.setLevel(level)
			.setLanguage(language)
			.build();
		
		GameMessage message = GameMessage.newBuilder().setCommand(GameCommandType.SEND_DRAW_DATA_REQUEST)
			.setMessageId(getClientIndex())
			.setUserId(userId)
			.setSessionId(sessionId)
			.setSendDrawDataRequest(request)
			.build();
		
		send(message);
	}

	public final void sendDraw(List<Integer> pointList, float width, int color){
		SendDrawDataRequest request = SendDrawDataRequest.newBuilder()
			.addAllPoints(pointList)
			.setWidth(width)
			.setColor(color)
			.build();
		
		GameMessage message = GameMessage.newBuilder().setCommand(GameCommandType.SEND_DRAW_DATA_REQUEST)
			.setMessageId(getClientIndex())
			.setUserId(userId)
			.setSessionId(sessionId)
			.setSendDrawDataRequest(request)
			.build();
		
		send(message);
	}	
	
	public void setGuessWordTimer(){
				
		clearGuessWordTimer();				
		
		if (currentPlayUserId.equals(userId)){
			// draw user cannot guess...
			return;
		}
		
		guessWordTimer = new Timer();
		guessWordTimer.schedule(new TimerTask(){

			@Override
			public void run() {	
				try{
					
					if (guessCorrect){
						ServerLog.info(sessionId, "Robot client, try guess but already guess correct");
						return;
					}
					
					String guessWord = null;
//					boolean isMatchWordLen = (language == DrawGameServer.LANGUAGE_CHINESE) ? false : true;
					
										
					boolean isMatchWordLen = true;
					String randomWord = null;
					
					if (guessCount >= 3 && RandomUtil.random(1) == 0){
						guessWord = word;
					}
					else{
						randomWord = DrawStorageService.getInstance().randomGetWord(language, word);
						if (randomWord == null){
							randomWord = WordManager.getInstance().randomGetWord(language, word.length(), isMatchWordLen);
						}

						guessWord = randomWord;
					}
					
					if (guessWord.equalsIgnoreCase(word)){
						guessCorrect = true;
					}
					
					guessCount ++;
					sendGuessWord(guessWord.toUpperCase());
					
				}
				catch (Exception e){
					ServerLog.error(sessionId, e, "robot client guess word timer ");
				}

				// schedule next timer
				if (!guessCorrect){
					setGuessWordTimer();
				}
			}
			
		}, 1000*RandomUtil.random(RANDOM_GUESS_WORD_INTERVAL)+1000);
	}
	
	public void clearGuessWordTimer(){
		if (guessWordTimer != null){
			guessWordTimer.cancel();
			guessWordTimer = null;
		}
		
	}

	public void resetPlayData() {
		clearGuessWordTimer();
		clearStartGameTimer();
		clearStartDrawTimer();
		
		guessCount = 0;
		guessCorrect = false;
	}

	/*
	public void saveUserList(List<PBGameUser> pbUserList) {
		if (pbUserList == null)
			return;
		
		userList.clear();
		for (PBGameUser pbUser : pbUserList){
			User user = new User(pbUser.getUserId(), pbUser.getNickName(), 
					pbUser.getAvatar(), pbUser.getGender(),
					pbUser.getLocation(), pbUser.getSnsUsersList(),
					null, sessionId, 1, 5);
			userList.put(pbUser.getUserId(), user);
		}
	}
	*/

	Timer startGameTimer = null;
	Timer startDrawTimer = null;
	volatile int sendDrawIndex = 0;
	private static final int START_TIMER_WAITING_INTERVAL = 10;
	private static final int START_DRAW_WAITING_INTERVAL = 1;
	public static int RANDOM_GUESS_WORD_INTERVAL = 20;	
	
	public void clearStartDrawTimer(){
		if (startDrawTimer != null){
			startDrawTimer.cancel();
			startDrawTimer = null;
		}		
	}
	
	public void clearStartGameTimer(){
		if (startGameTimer != null){
			startGameTimer.cancel();
			startGameTimer = null;
		}
	}
	
	public void checkStart() {
		if (state != ClientState.WAITING)
			return;
		
		if (!this.currentPlayUserId.equals(this.userId)){
			return;
		}
		
		if (startGameTimer != null){
			// ongoing...
			return;
		}
		
		resetPlayData();
		
		startGameTimer = new Timer();
		startGameTimer.schedule(new TimerTask(){

			@Override
			public void run() {
				
				try{
				
					Set<String> excludeUserSet = new HashSet<String>();
					Set<String> userIdList = userList.keySet();
					userIdList.remove(userId);
					for (String id : userIdList){
						if (!AbstractRobotManager.isRobotUser(id)){
							excludeUserSet.add(id);						
						}
					}
					
					BasicDBObject obj = DrawStorageService.getInstance().randomGetDraw(sessionId,
							DrawOnlineServer.getLanguage(),
							excludeUserSet);
					if (obj == null){
						ServerLog.warn(sessionId, "robot cannot find any draw for simulation! have to quit");
						disconnect();
						return;					
					}
	
					byte[] data = (byte[])obj.get(DBConstants.F_DRAW_DATA);
					if (data == null){
						ServerLog.warn(sessionId, "robot cannot find any draw for simulation! have to quit");
						disconnect();
						return;					
					}
					
					try {
						pbDraw = PBDraw.parseFrom(data);
					} catch (InvalidProtocolBufferException e) {
						ServerLog.warn(sessionId, "robot catch exception while parsing draw data, e="+e.toString());
						disconnect();
						return;					
					}
					
					String word = obj.getString(DBConstants.F_DRAW_WORD);
					int level = obj.getInt(DBConstants.F_DRAW_LEVEL);
					int language = obj.getInt(DBConstants.F_DRAW_LANGUAGE);
					
					sendStartGame();
					sendStartDraw(word, level, language);				
	
					state = ClientState.PLAYING;		
					long FIRST_DELAY = 10;	// 10 ms
					sendDrawIndex = 0;
					scheduleSendDrawDataTimer(pbDraw, FIRST_DELAY);	
				}
				catch(Exception e){
					ServerLog.error(sessionId, e);					
				}
			}


			
		}, RandomUtil.random(START_TIMER_WAITING_INTERVAL)*1000+3000);
		
	}
	
	private long getSendDrawInterval(long currentDataLen){
		return currentDataLen*10;
	}	
	
	private void scheduleSendDrawDataTimer(final PBDraw pbDraw, long delay) {
		if (state != ClientState.PLAYING){
			return;
		}
		
		if (!this.currentPlayUserId.equals(this.userId)){
			return;
		}
				
		startDrawTimer = new Timer();
		startDrawTimer.schedule(new TimerTask(){

			@Override
			public void run() {
				try{
					if (sendDrawIndex < 0 || sendDrawIndex >= pbDraw.getDrawDataCount()){
						ServerLog.info(sessionId, "robot has no more draw data");
						clearStartDrawTimer();
						return;
					}
					else{					
						PBDrawAction drawData = pbDraw.getDrawData(sendDrawIndex);
						
						if (drawData.getType() == DrawAction.DRAW_ACTION_TYPE_CLEAN)
							cleanDraw();
						else
							sendDraw(drawData.getPointsList(), drawData.getWidth(), drawData.getColor());
						
						sendDrawIndex++;
						
						// schedule next one
						clearStartDrawTimer();
						long nextDelay = getSendDrawInterval(drawData.getPointsCount());
						scheduleSendDrawDataTimer(pbDraw, nextDelay);
					}
				}
				catch(Exception e){
					ServerLog.error(sessionId, e);					
				}
			}
			
		}, delay); //START_DRAW_WAITING_INTERVAL*1000+1000, START_DRAW_WAITING_INTERVAL*1000+1000);
	}

	
}
