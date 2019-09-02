/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnetcore.command;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.CommandSender;
import de.dytanic.cloudnetcore.CloudNet;
import jline.console.ConsoleReader;

import java.io.IOException;

public final class CommandClear extends Command {

    public CommandClear() {
        super("clear", "cloudnet.command.clear");

        description = "Clears the console";

    }

    @Override
    public void onExecuteCommand(CommandSender sender, String[] args) {
        try {
            try (ConsoleReader reader = CloudNet.getLogger().getReader()) {
                reader.clearScreen();
            }
        } catch (IOException e) {
        }
    }
}
