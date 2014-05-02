package com.cometproject.server.game.items;

import com.cometproject.server.game.items.interactions.InteractionManager;
import com.cometproject.server.game.items.types.ItemDefinition;
import com.cometproject.server.storage.queries.items.ItemDao;
import com.cometproject.server.storage.queries.items.TeleporterDao;
import com.cometproject.server.storage.queries.rooms.RoomItemDao;
import javolution.util.FastMap;
import org.apache.log4j.Logger;

import java.util.Map;

public class ItemManager {
    private FastMap<Integer, ItemDefinition> itemDefinitions;
    private Map<Integer, Integer> teleportPairs;
    private InteractionManager interactions;

    private Logger log = Logger.getLogger(ItemManager.class.getName());

    public ItemManager() {
        this.itemDefinitions = new FastMap<>();
        this.interactions = new InteractionManager();
        this.teleportPairs = new FastMap<>();

        this.loadItemDefinitions();
    }

    public void loadItemDefinitions() {
        if (this.getItemDefinitions().size() >= 1) {
            this.getItemDefinitions().clear();
        }

        try {
            this.itemDefinitions = ItemDao.getDefinitions();
        } catch (Exception e) {
            log.error("Error while loading item definitions", e);
        }

        log.info("Loaded " + this.getItemDefinitions().size() + " item definitions");
    }

    public int getTeleportPartner(int itemId) {
        if (this.teleportPairs.containsKey(itemId)) {
            return teleportPairs.get(itemId);
        } else {
            try {
                int pairId = TeleporterDao.getPairId(itemId);
                this.teleportPairs.put(itemId, pairId);
                this.teleportPairs.put(pairId, itemId);

                return pairId;
            } catch (Exception e) {
                log.error("Error while searching for teleport partner", e);
            }
        }

        return 0;
    }

    public int roomIdByItemId(int itemId) {
        try {
            return RoomItemDao.getRoomIdById(itemId);
        } catch (Exception e) {
            return 0;
        }
    }

    public ItemDefinition getDefintion(int itemId) {
        if (this.getItemDefinitions().containsKey(itemId)) {
            return this.getItemDefinitions().get(itemId);
        }

        log.error("Couldn't find item definition for item: " + itemId + ", make sure the database is complete! (`furniture` table)");

        return null;
    }

    public InteractionManager getInteractions() {
        return this.interactions;
    }

    public FastMap<Integer, ItemDefinition> getItemDefinitions() {
        return this.itemDefinitions;
    }
}
