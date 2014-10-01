package com.cometproject.server.game.rooms.objects.items.types.floor.boutique;

import com.cometproject.server.game.rooms.objects.entities.GenericEntity;
import com.cometproject.server.game.rooms.objects.entities.types.PlayerEntity;
import com.cometproject.server.game.rooms.objects.items.RoomItemFloor;
import com.cometproject.server.game.rooms.objects.items.data.MannequinData;
import com.cometproject.server.game.rooms.types.Room;
import com.cometproject.server.network.messages.outgoing.room.avatar.UpdateInfoMessageComposer;

public class MannequinFloorItem extends RoomItemFloor {
    public MannequinFloorItem(int id, int itemId, Room room, int owner, int x, int y, double z, int rotation, String data) {
        super(id, itemId, room, owner, x, y, z, rotation, data);
    }

    @Override
    public void onInteract(GenericEntity entity, int requestData, boolean isWiredTrigger) {
        if (!(entity instanceof PlayerEntity))
            return;

        PlayerEntity playerEntity = (PlayerEntity) entity;

        // TODO: Move all this data to class properties
        MannequinData data = MannequinData.get(this.getExtraData());

        if (data == null) {
            // There's no data to use!!
            return;
        }

        playerEntity.getPlayer().getData().setFigure(data.getFigure());
        playerEntity.getPlayer().getData().setGender(data.getGender());

        playerEntity.getPlayer().getData().save();

        entity.getRoom().getEntities().broadcastMessage(UpdateInfoMessageComposer.compose(playerEntity));
        ((PlayerEntity) entity).getPlayer().getSession().send(UpdateInfoMessageComposer.compose(true, playerEntity));
    }
}

