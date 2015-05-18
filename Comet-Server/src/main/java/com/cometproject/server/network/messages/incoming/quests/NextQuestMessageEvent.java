package com.cometproject.server.network.messages.incoming.quests;

import com.cometproject.server.game.quests.Quest;
import com.cometproject.server.game.quests.QuestManager;
import com.cometproject.server.network.messages.incoming.Event;
import com.cometproject.server.network.messages.types.MessageEvent;
import com.cometproject.server.network.sessions.Session;

public class NextQuestMessageEvent implements Event {
    @Override
    public void handle(Session client, MessageEvent msg) throws Exception {
        if(client.getPlayer().getData().getQuestId() == 0) {
            return;
        }

        Quest quest = QuestManager.getInstance().getById(client.getPlayer().getData().getQuestId());

        if(quest == null) {
            return;
        }

        Quest nextQuest = QuestManager.getInstance().getNextQuestInSeries(quest);

        if(nextQuest != null) {
            client.getPlayer().getQuests().startQuest(nextQuest);
        }
    }
}
