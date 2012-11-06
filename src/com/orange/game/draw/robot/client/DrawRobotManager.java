package com.orange.game.draw.robot.client;

import com.orange.game.model.dao.User;
import com.orange.game.traffic.robot.client.AbstractRobotClient;
import com.orange.game.traffic.robot.client.AbstractRobotManager;

public class DrawRobotManager extends AbstractRobotManager {

	@Override
	public AbstractRobotClient createRobotClient(User robotUser, int sessionId,
			int index) {
		return new DrawRobotClient(robotUser, sessionId, index);
	}

//	@Override
//	public AbstractRobotClient createRobotClient(String userId,
//			String nickName, String avatar, boolean gender, 
//			String location, int sessionId, int index) {
//		return new DiceRobotClient(userId, nickName, avatar, gender, location, sessionId, index);
//	}

	
}
