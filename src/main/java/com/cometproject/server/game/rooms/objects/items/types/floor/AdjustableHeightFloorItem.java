package com.cometproject.server.game.rooms.objects.items.types.floor;

import com.cometproject.server.game.rooms.objects.entities.GenericEntity;
import com.cometproject.server.game.rooms.objects.entities.types.PlayerEntity;
import com.cometproject.server.game.rooms.objects.items.RoomItemFloor;
import com.cometproject.server.game.rooms.types.Room;
import org.apache.commons.lang.StringUtils;

public class AdjustableHeightFloorItem extends RoomItemFloor {
    public AdjustableHeightFloorItem(int id, int itemId, Room room, int owner, int x, int y, double z, int rotation, String data) {
        super(id, itemId, room, owner, x, y, z, rotation, data);
    }

    @Override
    public void onInteract(GenericEntity entity, int requestData, boolean isWiredTrigger) {
        if (!isWiredTrigger) {
            if (!(entity instanceof PlayerEntity)) {
                return;
            }

            PlayerEntity pEntity = (PlayerEntity) entity;

            if (!pEntity.getRoom().getRights().hasRights(pEntity.getPlayerId())
                    && !pEntity.getPlayer().getPermissions().hasPermission("room_full_control")) {
                return;
            }
        }

        this.toggleInteract(true);

        this.sendUpdate();

        // TODO: Move item saving to a queue for batch saving or something. :P
        this.saveData();
    }

    @Override
    public double getOverrideHeight() {
        double height;

            if (this.getExtraData().isEmpty() || !StringUtils.isNumeric(this.getExtraData())) {
            height = 0.5;
        } else {
            height = Double.parseDouble(this.getExtraData());
        }

        return height;
    }
}
