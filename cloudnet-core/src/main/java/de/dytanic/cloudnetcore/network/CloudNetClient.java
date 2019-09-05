/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnetcore.network;

import de.dytanic.cloudnet.lib.CloudNetwork;
import de.dytanic.cloudnet.lib.network.protocol.packet.Packet;
import de.dytanic.cloudnetcore.CloudNet;
import de.dytanic.cloudnetcore.api.event.network.ChannelInitEvent;
import de.dytanic.cloudnetcore.api.event.network.WrapperChannelDisconnectEvent;
import de.dytanic.cloudnetcore.api.event.network.WrapperChannelInitEvent;
import de.dytanic.cloudnetcore.database.StatisticManager;
import de.dytanic.cloudnetcore.network.components.INetworkComponent;
import de.dytanic.cloudnetcore.network.components.MinecraftServer;
import de.dytanic.cloudnetcore.network.components.ProxyServer;
import de.dytanic.cloudnetcore.network.components.Wrapper;
import de.dytanic.cloudnetcore.network.packet.out.PacketOutCloudNetwork;
import de.dytanic.cloudnetcore.network.packet.out.PacketOutOnlineServer;
import de.dytanic.cloudnetcore.network.wrapper.WrapperSession;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;
import java.util.UUID;

/**
 * This is the SimpleChannelInboundHandler of netty handled for a networkComponent
 */
public class CloudNetClient extends SimpleChannelInboundHandler {

    private final INetworkComponent networkComponent;
    private Channel channel;

    public CloudNetClient(final INetworkComponent iNetworkComponent, final Channel channel) {
        this.networkComponent = iNetworkComponent;
        this.networkComponent.setChannel(channel);
        this.channel = channel;

        System.out.println(
            "Channel connected [" + channel.remoteAddress().toString() + "/serverId=" + networkComponent.getServerId() + ']');

        if (networkComponent instanceof Wrapper) {
            StatisticManager.getInstance().wrapperConnections();
            System.out.println("Wrapper [" + networkComponent.getServerId() + "] is connected.");
            CloudNet.getInstance().getEventManager().callEvent(new WrapperChannelInitEvent((Wrapper) networkComponent, channel));
            CloudNet.getInstance().getDbHandlers().getWrapperSessionDatabase().addSession(new WrapperSession(UUID.randomUUID(),
                                                                                                             ((Wrapper) networkComponent)
                                                                                                                 .getNetworkInfo(),
                                                                                                             System.currentTimeMillis()));
            ((Wrapper) networkComponent).updateWrapper();
        }

        final CloudNetwork cloudNetwork = CloudNet.getInstance().getNetworkManager().newCloudNetwork();
        channel.writeAndFlush(new PacketOutCloudNetwork(cloudNetwork));

        if (networkComponent instanceof MinecraftServer) {
            ((MinecraftServer) networkComponent).setChannelLostTime(0L);
            networkComponent.getWrapper().sendPacket(new PacketOutOnlineServer(((MinecraftServer) networkComponent).getServerInfo()));
        }
        if (networkComponent instanceof ProxyServer) {
            ((ProxyServer) networkComponent).setChannelLostTime(0L);
        }
        CloudNet.getInstance().getEventManager().callEvent(new ChannelInitEvent(channel, networkComponent));
        init(cloudNetwork);
    }

    public void init(final CloudNetwork cloudNetwork) {
        CloudNet.getInstance().getScheduler().runTaskAsync(new Runnable() {
            @Override
            public void run() {
                CloudNet.getInstance().getNetworkManager().sendAll(new PacketOutCloudNetwork(cloudNetwork));
            }
        });
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        this.channel = ctx.channel();
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        if ((!channel.isActive() || !channel.isOpen() || !channel.isWritable())) {
            System.out.println(
                "Channel disconnected [" + channel.remoteAddress().toString() + "/serverId=" + networkComponent.getServerId() + ']');
            ctx.close().syncUninterruptibly();
            if (networkComponent instanceof MinecraftServer) {
                ((MinecraftServer) networkComponent).setChannelLostTime(System.currentTimeMillis());
            }
            if (networkComponent instanceof ProxyServer) {
                ((ProxyServer) networkComponent).setChannelLostTime(System.currentTimeMillis());
            }
            if (networkComponent instanceof Wrapper) {
                try {
                    ((Wrapper) networkComponent).disconnct();
                } catch (final Exception ex) {

                    ((Wrapper) networkComponent).getServers().clear();
                    ((Wrapper) networkComponent).getProxys().clear();

                }

                CloudNet.getInstance().getEventManager().callEvent(new WrapperChannelDisconnectEvent(((Wrapper) networkComponent)));

            }
            networkComponent.setChannel(null);
        }
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {

        if (!(cause instanceof IOException)) {
            cause.printStackTrace();
        }
        //TODO:

    }

    @Override
    protected void channelRead0(final ChannelHandlerContext channelHandlerContext, final Object obj) throws Exception {

        if (!(obj instanceof Packet)) {
            return;
        }

        final Packet packet = (Packet) obj;
        CloudNet.getLogger().debug("Receiving Packet (id=" + CloudNet.getInstance().getPacketManager().packetId(packet) + ";dataLength=" +
                                   CloudNet.getInstance().getPacketManager().packetData(packet).size() + ") by " +
                                   networkComponent.getServerId());
        CloudNet.getInstance().getPacketManager().dispatchPacket(packet, networkComponent);
    }

    public INetworkComponent getNetworkComponent() {
        return networkComponent;
    }
}
