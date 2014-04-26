package com.cometproject.server.network.codec;

import com.cometproject.server.network.messages.types.Composer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.apache.log4j.Logger;

import java.util.List;

public class MessageEncoder extends MessageToMessageEncoder<Composer> {
    private static Logger log = Logger.getLogger(MessageEncoder.class.getName());

    @Override
    protected void encode(ChannelHandlerContext ctx, Composer msg, List<Object> out) throws Exception {
        ByteBuf buf = msg.get();

        try {
            out.add(buf);
        } catch (Exception e) {
            log.error("Failed to encode message: ", e);
        }
    }
}
