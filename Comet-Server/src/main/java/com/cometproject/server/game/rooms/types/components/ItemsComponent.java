package com.cometproject.server.game.rooms.types.components;

import com.cometproject.server.config.CometSettings;
import com.cometproject.server.game.items.types.ItemDefinition;
import com.cometproject.server.game.players.components.types.inventory.InventoryItem;
import com.cometproject.server.game.players.types.Player;
import com.cometproject.server.game.rooms.objects.entities.GenericEntity;
import com.cometproject.server.game.rooms.objects.entities.pathfinding.AffectedTile;
import com.cometproject.server.game.rooms.objects.items.RoomItem;
import com.cometproject.server.game.rooms.objects.items.RoomItemFactory;
import com.cometproject.server.game.rooms.objects.items.RoomItemFloor;
import com.cometproject.server.game.rooms.objects.items.RoomItemWall;
import com.cometproject.server.game.rooms.objects.items.types.floor.GiftFloorItem;
import com.cometproject.server.game.rooms.objects.items.types.wall.MoodlightWallItem;
import com.cometproject.server.game.rooms.objects.misc.Position;
import com.cometproject.server.game.rooms.types.RoomInstance;
import com.cometproject.server.game.rooms.types.mapping.Tile;
import com.cometproject.server.network.messages.outgoing.room.engine.UpdateStackMapMessageComposer;
import com.cometproject.server.network.messages.outgoing.room.items.RemoveFloorItemMessageComposer;
import com.cometproject.server.network.messages.outgoing.room.items.RemoveWallItemMessageComposer;
import com.cometproject.server.network.messages.outgoing.room.items.SendFloorItemMessageComposer;
import com.cometproject.server.network.messages.outgoing.room.items.SendWallItemMessageComposer;
import com.cometproject.server.network.messages.outgoing.user.inventory.UpdateInventoryMessageComposer;
import com.cometproject.server.network.sessions.Session;
import com.cometproject.server.storage.queries.rooms.RoomItemDao;
import com.google.common.collect.Lists;
import javolution.util.FastMap;
import javolution.util.FastTable;
import org.apache.log4j.Logger;

import java.util.*;


public class ItemsComponent {
    private RoomInstance room;
    private final Logger log;

    private final Map<Integer, RoomItemFloor> floorItems = new FastMap<Integer, RoomItemFloor>().shared();
    private final FastTable<RoomItemWall> wallItems = new FastTable<RoomItemWall>().shared();

    private Map<Class<? extends RoomItemFloor>, Set<Integer>> itemClassIndex = new FastMap<Class<? extends RoomItemFloor>, Set<Integer>>().shared();
    private Map<String, Set<Integer>> itemInteractionIndex = new FastMap<String, Set<Integer>>().shared();

    private int moodlightId;

    public ItemsComponent(RoomInstance room) {
        this.room = room;
        this.log = Logger.getLogger("RoomInstance Items Component [" + room.getData().getName() + "]");

        RoomItemDao.getItems(this.room, this.floorItems, this.wallItems);

        for(RoomItemFloor floorItem : this.floorItems.values()) {
            this.indexItem(floorItem);
        }
    }

    public void onLoaded() {
        for (RoomItemFloor floorItem : floorItems.values()) {
            floorItem.onLoad();
        }

        for (RoomItemWall wallItem : wallItems) {
            wallItem.onLoad();
        }
    }

    public void dispose() {
        for (RoomItemFloor floorItem : floorItems.values()) {
            floorItem.onUnload();
        }

        for (RoomItemWall wallItem : wallItems) {
            wallItem.onUnload();
        }

        this.floorItems.clear();
        this.wallItems.clear();

        for(Set<Integer> itemIds : this.itemClassIndex.values()) {
            itemIds.clear();
        }

        this.itemClassIndex.clear();
    }

    public boolean setMoodlight(int moodlight) {
        if (this.moodlightId != 0)
            return false;

        this.moodlightId = moodlight;
        return true;
    }

    public boolean removeMoodlight() {
        if (this.moodlightId == 0) {
            return false;
        }

        this.moodlightId = 0;
        return true;
    }

    public boolean isMoodlightMatches(RoomItem item) {
        if (this.moodlightId == 0) {
            return false;
        }
        return (this.moodlightId == item.getId());
    }

    public MoodlightWallItem getMoodlight() {
        return (MoodlightWallItem) this.getWallItem(this.moodlightId);
    }

    public RoomItemFloor addFloorItem(int id, int baseId, RoomInstance room, int ownerId, int x, int y, int rot, double height, String data) {
        RoomItemFloor floor = RoomItemFactory.createFloor(id, baseId, room, ownerId, x, y, height, rot, data);

        this.floorItems.put(floor.getId(), floor);
        this.indexItem(floor);

        return floor;
    }

    public RoomItemWall addWallItem(int id, int baseId, RoomInstance room, int ownerId, String position, String data) {
        RoomItemWall wall = RoomItemFactory.createWall(id, baseId, room, ownerId, position, data);
        this.getWallItems().add(wall);

        return wall;
    }

    public List<RoomItemFloor> getItemsOnSquare(int x, int y) {
        Tile tile = this.getRoom().getMapping().getTile(x, y);

        if(tile == null) {
            return Lists.newArrayList();
        }

        return new ArrayList<>(tile.getItems());
    }

    public RoomItemFloor getFloorItem(int id) {
        return this.floorItems.get(id);
    }

    public RoomItemWall getWallItem(int id) {
        for (RoomItemWall item : this.getWallItems()) {
            if (item.getId() == id) {
                return item;
            }
        }

        return null;
    }

    public List<RoomItemFloor> getByInteraction(String interaction) {
        List<RoomItemFloor> items = new ArrayList<>();

        for (RoomItemFloor floorItem : this.floorItems.values()) {
            if (floorItem == null || floorItem.getDefinition() == null) continue;

            if (floorItem.getDefinition().getInteraction().equals(interaction)) {
                items.add(floorItem);
            } else if (interaction.contains("%")) {
                if (interaction.startsWith("%") && floorItem.getDefinition().getInteraction().endsWith(interaction.replace("%", ""))) {
                    items.add(floorItem);
                } else if (interaction.endsWith("%") && floorItem.getDefinition().getInteraction().startsWith(interaction.replace("%", ""))) {
                    items.add(floorItem);
                }
            }
        }

        return items;
    }

    public List<RoomItemFloor> getByClass(Class<? extends RoomItemFloor> clazz) {
        List<RoomItemFloor> items = new ArrayList<>();

        if(this.itemClassIndex.containsKey(clazz)) {
            for(Integer itemId : this.itemClassIndex.get(clazz)) {
                RoomItemFloor floorItem = this.getFloorItem(itemId);

                if(floorItem == null || floorItem.getDefinition() == null) continue;

                items.add(this.getFloorItem(itemId));
            }
        }

        return items;
    }

    public void removeItem(RoomItemWall item, int ownerId, Session client) {
        RoomItemDao.removeItemFromRoom(item.getId(), ownerId);

        room.getEntities().broadcastMessage(new RemoveWallItemMessageComposer(item.getId(), ownerId));
        this.getWallItems().remove(item);

        if(client != null && client.getPlayer() != null) {
            client.getPlayer().getInventory().add(item.getId(), item.getItemId(), item.getExtraData());
            client.send(new UpdateInventoryMessageComposer());
        }
    }

    public void removeItem(RoomItemFloor item, Session client) {
        removeItem(item, client, true, false);
    }

    public void removeItem(RoomItemFloor item, Session client, boolean toInventory, boolean delete) {
        List<GenericEntity> affectEntities = room.getEntities().getEntitiesAt(item.getPosition());
        List<Position> tilesToUpdate = new ArrayList<>();

        tilesToUpdate.add(new Position(item.getPosition().getX(), item.getPosition().getY(), 0d));

        for (GenericEntity entity : affectEntities) {
            item.onEntityStepOff(entity);
        }

        for (AffectedTile tile : AffectedTile.getAffectedTilesAt(item.getDefinition().getLength(), item.getDefinition().getWidth(), item.getPosition().getX(), item.getPosition().getY(), item.getRotation())) {
            List<GenericEntity> affectEntities0 = room.getEntities().getEntitiesAt(new Position(tile.x, tile.y));
            tilesToUpdate.add(new Position(tile.x, tile.y, 0d));

            for (GenericEntity entity0 : affectEntities0) {
                item.onEntityStepOff(entity0);
            }
        }

        this.getRoom().getEntities().broadcastMessage(new RemoveFloorItemMessageComposer(item.getId(), client != null ? client.getPlayer().getId() : 0));
        this.getFloorItems().remove(item);

        if (toInventory && client != null) {
            RoomItemDao.removeItemFromRoom(item.getId(), client.getPlayer().getId());

            client.getPlayer().getInventory().add(item.getId(), item.getItemId(), item.getExtraData(), item instanceof GiftFloorItem ? ((GiftFloorItem) item).getGiftData() : null);
            client.send(new UpdateInventoryMessageComposer());
        } else {
            if(delete)
                RoomItemDao.deleteItem(item.getId());
        }

        for (Position tileToUpdate : tilesToUpdate) {
            final Tile tileInstance = this.room.getMapping().getTile(tileToUpdate.getX(), tileToUpdate.getY());

            if(tileInstance != null) {
                tileInstance.reload();

                room.getEntities().broadcastMessage(new UpdateStackMapMessageComposer(tileInstance));
            }
        }
    }

    public void removeItem(RoomItemWall item, Session client, boolean toInventory) {
        this.getRoom().getEntities().broadcastMessage(new RemoveWallItemMessageComposer(item.getId(), client.getPlayer().getId()));
        this.getWallItems().remove(item);

        if (toInventory) {
            RoomItemDao.removeItemFromRoom(item.getId(), client.getPlayer().getId());

            client.getPlayer().getInventory().add(item.getId(), item.getItemId(), item.getExtraData());
            client.send(new UpdateInventoryMessageComposer());
        } else {
            RoomItemDao.deleteItem(item.getId());
        }
    }

    public boolean moveFloorItem(int itemId, Position newPosition, int rotation, boolean save) {
        RoomItemFloor item = this.getFloorItem(itemId);
        if (item == null) return false;

        Tile tile = this.getRoom().getMapping().getTile(newPosition.getX(), newPosition.getY());

        if(!this.verifyItemPosition(item.getDefinition(), tile, item.getPosition())) {
            return false;
        }

        double height = tile.getStackHeight(item);

        List<RoomItemFloor> floorItemsAt = this.getItemsOnSquare(newPosition.getX(), newPosition.getY());

        for (RoomItemFloor stackItem : floorItemsAt) {
            if (item.getId() != stackItem.getId()) {
                stackItem.onItemAddedToStack(item);
            }
        }

        item.onPositionChanged(newPosition);

        List<GenericEntity> affectEntities0 = room.getEntities().getEntitiesAt(item.getPosition());

        for (GenericEntity entity0 : affectEntities0) {
            item.onEntityStepOff(entity0);
        }

        List<Position> tilesToUpdate = new ArrayList<>();

        tilesToUpdate.add(new Position(item.getPosition().getX(), item.getPosition().getY()));
        tilesToUpdate.add(new Position(newPosition.getX(), newPosition.getY()));

        // Catch this so the item still updates!
        try {
            for (AffectedTile affectedTile : AffectedTile.getAffectedTilesAt(item.getDefinition().getLength(), item.getDefinition().getWidth(), item.getPosition().getX(), item.getPosition().getY(), item.getRotation())) {
                tilesToUpdate.add(new Position(affectedTile.x, affectedTile.y));

                List<GenericEntity> affectEntities1 = room.getEntities().getEntitiesAt(new Position(affectedTile.x, affectedTile.y));

                for (GenericEntity entity1 : affectEntities1) {
                    item.onEntityStepOff(entity1);
                }
            }

            for (AffectedTile affectedTile : AffectedTile.getAffectedTilesAt(item.getDefinition().getLength(), item.getDefinition().getWidth(), newPosition.getX(), newPosition.getY(), rotation)) {
                tilesToUpdate.add(new Position(affectedTile.x, affectedTile.y));

                List<GenericEntity> affectEntities2 = room.getEntities().getEntitiesAt(new Position(affectedTile.x, affectedTile.y));

                for (GenericEntity entity2 : affectEntities2) {
                    item.onEntityStepOn(entity2);
                }
            }
        } catch (Exception e) {
            log.error("Failed to update entity positions for changing item position", e);
        }

        item.getPosition().setX(newPosition.getX());
        item.getPosition().setY(newPosition.getY());

        item.getPosition().setZ(height);
        item.setRotation(rotation);

        List<GenericEntity> affectEntities3 = room.getEntities().getEntitiesAt(newPosition);

        for (GenericEntity entity3 : affectEntities3) {
            item.onEntityStepOn(entity3);
        }

        if (save)
            RoomItemDao.saveItemPosition(newPosition.getX(), newPosition.getY(), height, rotation, itemId);

        for (Position tileToUpdate : tilesToUpdate) {
            final Tile tileInstance = this.room.getMapping().getTile(tileToUpdate.getX(), tileToUpdate.getY());

            if(tileInstance != null) {
                tileInstance.reload();

                room.getEntities().broadcastMessage(new UpdateStackMapMessageComposer(tileInstance));
            }
        }

        tilesToUpdate.clear();
        return true;
    }

    private boolean verifyItemPosition(ItemDefinition item, Tile tile, Position currentPosition) {
        if (tile != null) {
            if(currentPosition != null && currentPosition.getX() == tile.getPosition().getX() && currentPosition.getY() == tile.getPosition().getY())
                return true;

            if (!tile.canPlaceItemHere()) {
                return false;
            }

            if (!tile.canStack() && tile.getTopItem() != 0 && tile.getTopItem() != item.getId()) {
                if(!item.getItemName().startsWith(RoomItemFactory.STACK_TOOL))
                    return false;
            }

            if (!item.getInteraction().equals(RoomItemFactory.TELEPORT_PAD) && tile.getPosition().getX() == this.getRoom().getModel().getDoorX() && tile.getPosition().getY() == this.getRoom().getModel().getDoorY()) {
                return false;
            }

            if(!CometSettings.placeItemOnEntity) {
                if (tile.getEntities().size() != 0) {
                    return false;
                }
            }
        } else {
            return false;
        }

        return true;
    }

    public void placeWallItem(InventoryItem item, String position, Player player) {
        int roomId = this.room.getId();

        RoomItemDao.placeWallItem(roomId, position, item.getExtraData().trim().isEmpty() ? "0" : item.getExtraData(), item.getId());
        player.getInventory().removeWallItem(item.getId());

        RoomItemWall wallItem = this.addWallItem(item.getId(), item.getBaseId(), this.room, player.getId(), position, (item.getExtraData().isEmpty() || item.getExtraData().equals(" ")) ? "0" : item.getExtraData());

        this.room.getEntities().broadcastMessage(
                new SendWallItemMessageComposer(wallItem)
        );

        wallItem.onPlaced();
    }

    public RoomInstance getRoom() {
        return this.room;
    }

    public Collection<RoomItemFloor> getFloorItems() {
        return this.floorItems.values();
    }

    public Collection<RoomItemWall> getWallItems() {
        return this.wallItems;
    }

    public void placeFloorItem(InventoryItem item, int x, int y, int rot, Player player) {
        Tile tile = room.getMapping().getTile(x, y);

        if (tile == null)
            return;

        double height = tile.getStackHeight();

        if(!this.verifyItemPosition(item.getDefinition(), tile, null))
            return;

        List<RoomItemFloor> floorItems = room.getItems().getItemsOnSquare(x, y);

        if (item.getDefinition() != null && item.getDefinition().getInteraction() != null) {
            if (item.getDefinition().getInteraction().equals("mannequin")) {
                rot = 2;
            }
        }

        RoomItemDao.placeFloorItem(room.getId(), x, y, height, rot, (item.getExtraData().isEmpty() || item.getExtraData().equals(" ")) ? "0" : item.getExtraData(), item.getId());
        player.getInventory().removeFloorItem(item.getId());

        RoomItemFloor floorItem = room.getItems().addFloorItem(item.getId(), item.getBaseId(), room, player.getId(), x, y, rot, height, (item.getExtraData().isEmpty() || item.getExtraData().equals(" ")) ? "0" : item.getExtraData());
        List<Position> tilesToUpdate = new ArrayList<>();

        for (RoomItemFloor stackItem : floorItems) {
            if (item.getId() != stackItem.getId()) {
                stackItem.onItemAddedToStack(floorItem);
            }
        }

        tilesToUpdate.add(new Position(floorItem.getPosition().getX(), floorItem.getPosition().getY(), 0d));

        for (AffectedTile affTile : AffectedTile.getAffectedBothTilesAt(item.getDefinition().getLength(), item.getDefinition().getWidth(), floorItem.getPosition().getX(), floorItem.getPosition().getY(), floorItem.getRotation())) {
            tilesToUpdate.add(new Position(affTile.x, affTile.y, 0d));

            List<GenericEntity> affectEntities0 = room.getEntities().getEntitiesAt(new Position(affTile.x, affTile.y));

            for (GenericEntity entity0 : affectEntities0) {
                floorItem.onEntityStepOn(entity0);
            }
        }

        floorItem.onPlaced();

        RoomItemDao.placeFloorItem(room.getId(), x, y, height, rot, (item.getExtraData().isEmpty() || item.getExtraData().equals(" ")) ? "0" : item.getExtraData(), item.getId());

        for (Position tileToUpdate : tilesToUpdate) {
            final Tile tileInstance = this.room.getMapping().getTile(tileToUpdate.getX(), tileToUpdate.getY());

            if(tileInstance != null) {
                tileInstance.reload();

                room.getEntities().broadcastMessage(new UpdateStackMapMessageComposer(tileInstance));
            }
        }

        room.getEntities().broadcastMessage(new SendFloorItemMessageComposer(floorItem));

        floorItem.saveData();
    }

    private void indexItem(RoomItemFloor floorItem) {
        if(!this.itemClassIndex.containsKey(floorItem.getClass())) {
            itemClassIndex.put(floorItem.getClass(), new HashSet<>());
        }

        if(!this.itemInteractionIndex.containsKey(floorItem.getDefinition().getInteraction())) {
            this.itemInteractionIndex.put(floorItem.getDefinition().getInteraction(), new HashSet<>());
        }

        this.itemClassIndex.get(floorItem.getClass()).add(floorItem.getId());
        this.itemInteractionIndex.get(floorItem.getDefinition().getInteraction()).add(floorItem.getId());
    }
}