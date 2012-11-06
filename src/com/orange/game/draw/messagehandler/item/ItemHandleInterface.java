package com.orange.game.draw.messagehandler.item;

import org.jboss.netty.channel.Channel;

import com.orange.game.draw.model.DrawGameSession;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.UseItemResponse;

public interface ItemHandleInterface {

	GameResultCode handleMessage(GameMessage message, Channel channel,
			DrawGameSession session, String userId, int itemId, UseItemResponse.Builder useItemResponseBuilder);

}
