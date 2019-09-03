package de.dytanic.cloudnetwrapper.handlers;

import de.dytanic.cloudnetwrapper.CloudNetWrapper;
import de.dytanic.cloudnetwrapper.screen.AbstractScreenService;
import de.dytanic.cloudnetwrapper.server.BungeeCord;
import de.dytanic.cloudnetwrapper.server.CloudGameServer;
import de.dytanic.cloudnetwrapper.server.GameServer;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ReadConsoleLogHandler implements IWrapperHandler {

    private final StringBuffer stringBuffer = new StringBuffer();

    private final byte[] buffer = new byte[1024];

    @Override
    public void run(final CloudNetWrapper obj) {
        for (final CloudGameServer cloudGameServer : obj.getCloudServers().values()) {
            if (cloudGameServer.isAlive() && cloudGameServer.getInstance() != null) {
                readConsoleLog(cloudGameServer);
            }
        }

        for (final BungeeCord bungeeCord : obj.getProxys().values()) {
            if (bungeeCord.isAlive() && bungeeCord.getInstance() != null) {
                readConsoleLog(bungeeCord);
            }
        }

        for (final GameServer gameServer : obj.getServers().values()) {
            if (gameServer.isAlive() && gameServer.getInstance() != null) {
                readConsoleLog(gameServer);
            }
        }
    }

    private synchronized void readConsoleLog(final AbstractScreenService server) {
        // We can't close the input stream, as we need it for the next run.
        final InputStream inputStream = server.getInstance().getInputStream();
        if (server.getInstance().isAlive() && inputStream != null) {
            readStream(server, inputStream);
            readStream(server, server.getInstance().getErrorStream());
        }

    }

    private synchronized void readStream(final AbstractScreenService screenService, final InputStream inputStream) {
        try {
            int len;
            while (inputStream.available() > 0 && (len = inputStream.read(buffer, 0, buffer.length)) != -1) {
                stringBuffer.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
            }

            final String stringText = stringBuffer.toString();
            if (!stringText.contains("\n") && !stringText.contains("\r")) {
                return;
            }

            for (final String input : stringText.split("\r")) {
                for (final String text : input.split("\n")) {
                    if (!text.trim().isEmpty()) {
                        screenService.addCachedItem(text);
                    }
                }
            }

            stringBuffer.setLength(0);

        } catch (final Exception ignored) {
            stringBuffer.setLength(0);
        }
    }

    @Override
    public int getTicks() {
        return 40;
    }
}
