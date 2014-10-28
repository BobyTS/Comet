package com.cometproject.server.network.messages.outgoing.catalog.data;

import com.cometproject.server.network.messages.headers.Composers;
import com.cometproject.server.network.messages.types.Composer;

public class GiftWrappingConfigurationMessageComposer {
    public static Composer compose() {
        Composer msg = new Composer(Composers.GiftWrappingConfigurationMessageComposer);

        msg.writeBoolean(true);
        msg.writeInt(1);
        msg.writeInt(10);

        for (int i = 8882; i < 8892; i++) {
            msg.writeInt(i);
        }

        msg.writeInt(8);
        msg.writeInt(0);
        msg.writeInt(1);
        msg.writeInt(2);
        msg.writeInt(3);
        msg.writeInt(4);
        msg.writeInt(5);
        msg.writeInt(6);
        msg.writeInt(8);
        msg.writeInt(11);
        msg.writeInt(0);
        msg.writeInt(1);
        msg.writeInt(2);
        msg.writeInt(3);
        msg.writeInt(4);
        msg.writeInt(5);
        msg.writeInt(6);
        msg.writeInt(7);
        msg.writeInt(8);
        msg.writeInt(9);
        msg.writeInt(10);
        msg.writeInt(7);

        msg.writeInt(204);
        msg.writeInt(205);
        msg.writeInt(206);
        msg.writeInt(207);
        msg.writeInt(208);
        msg.writeInt(209);
        msg.writeInt(210);

        return msg;
    }
}
