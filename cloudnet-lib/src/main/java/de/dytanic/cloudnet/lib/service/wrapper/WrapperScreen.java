/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.lib.service.wrapper;

/**
 * Created by Tareko on 23.09.2017.
 */
public class WrapperScreen {

    private final String wrapperId;

    private final String consoleLine;

    public WrapperScreen(final String wrapperId, final String consoleLine) {
        this.wrapperId = wrapperId;
        this.consoleLine = consoleLine;
    }

    public String getWrapperId() {
        return wrapperId;
    }

    public String getConsoleLine() {
        return consoleLine;
    }
}
