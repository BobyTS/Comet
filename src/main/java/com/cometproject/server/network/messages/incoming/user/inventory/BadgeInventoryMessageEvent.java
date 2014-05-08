package com.cometproject.server.network.messages.incoming.user.inventory;

import com.cometproject.server.network.messages.incoming.IEvent;
import com.cometproject.server.network.messages.outgoing.user.inventory.BadgeInventoryMessageComposer;
import com.cometproject.server.network.messages.outgoing.user.profile.UserBadgesMessageComposer;
import com.cometproject.server.network.messages.types.Event;
import com.cometproject.server.network.sessions.Session;
import com.cometproject.server.storage.queries.player.PlayerDao;
import com.cometproject.server.storage.queries.player.inventory.InventoryDao;

public class BadgeInventoryMessageEvent implements IEvent {
    public void handle(Session client, Event msg) {
        int userId = msg.readInt();

        if(userId == client.getPlayer().getId()) {
            client.send(BadgeInventoryMessageComposer.compose(client.getPlayer().getInventory().getBadges()));
            client.send(UserBadgesMessageComposer.compose(userId, client.getPlayer().getInventory().getBadges()));
            return;
        }

        client.send(UserBadgesMessageComposer.compose(userId, InventoryDao.getWornBadgesByPlayerId(userId)));
    }
}
