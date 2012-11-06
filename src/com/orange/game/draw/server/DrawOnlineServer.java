package com.orange.game.draw.server;

import java.net.ServerSocket;

import org.apache.log4j.Logger;

import com.orange.common.statemachine.StateMachine;
import com.orange.game.draw.model.DrawGameSessionManager;
import com.orange.game.draw.robot.client.DrawRobotManager;
import com.orange.game.draw.statemachine.DrawGameStateMachineBuilder;
import com.orange.game.traffic.robot.client.RobotService;
import com.orange.game.traffic.server.GameServer;
import com.orange.game.traffic.server.ServerMonitor;

public class DrawOnlineServer {
	
	private static final Logger logger = Logger.getLogger(DrawOnlineServer.class
			.getName());
	
	public static final int LANGUAGE_CHINESE = 1;
	public static final int LANGUAGE_ENGLISH = 2;
	
	public static int getPort() {
		String port = System.getProperty("server.port");
		if (port != null && !port.isEmpty()){
			return Integer.parseInt(port);
		}
		return 8080; // default
	}
	
	public static int getLanguage() {
		String lang = System.getProperty("config.lang");
		if (lang != null && !lang.isEmpty()){
			return Integer.parseInt(lang);
		}
		return LANGUAGE_CHINESE; // default
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		RobotService.getInstance().initRobotManager(new DrawRobotManager());
		
		// init data
		StateMachine diceStateMachine = DrawGameStateMachineBuilder.getInstance().buildStateMachine();
		DrawGameSessionManager sessionManager = new DrawGameSessionManager();
		
		// create server
		GameServer server = new GameServer(new DrawGameServerHandler(), diceStateMachine, sessionManager);
		
		// start server
		server.start();			
	}

}
