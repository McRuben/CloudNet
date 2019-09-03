/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.api.database;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.api.CloudAPI;
import de.dytanic.cloudnet.api.database.packet.out.*;
import de.dytanic.cloudnet.lib.NetworkUtils;
import de.dytanic.cloudnet.lib.database.Database;
import de.dytanic.cloudnet.lib.network.protocol.packet.result.Result;
import de.dytanic.cloudnet.lib.utility.document.Document;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Created by Tareko on 24.08.2017.
 */
public class DatabaseImpl implements Database {

    private final String name;
    private Map<String, Document> docs = NetworkUtils.newConcurrentHashMap();

    public DatabaseImpl(final String name) {
        this.name = name;
    }

    @Override
    public Database loadDocuments() {
        final Result result = CloudAPI.getInstance().getNetworkConnection().getPacketManager().sendQuery(new PacketDBOutGetDocument(name),
                                                                                                         CloudAPI.getInstance()
                                                                                                                 .getNetworkConnection());
        this.docs = result.getResult().getObject("docs", new TypeToken<Map<String, Document>>() {}.getType());
        return this;
    }

    @Override
    public Collection<Document> getDocs() {
        return docs.values();
    }

    @Override
    public Document getDocument(final String name) {
        final Result result = CloudAPI.getInstance().getNetworkConnection().getPacketManager().sendQuery(new PacketDBOutGetDocument(name,
                                                                                                                                    this.name),
                                                                                                         CloudAPI.getInstance()
                                                                                                                 .getNetworkConnection());
        final Document document = result.getResult().getDocument("result");

        this.docs.put(document.getString(Database.UNIQUE_NAME_KEY), document);

        return document;
    }

    @Override
    public Database insert(final Document... documents) {
        CloudAPI.getInstance().getNetworkConnection().sendPacket(new PacketDBOutInsertDocument(name, documents));
        return this;
    }

    @Override
    public Database delete(final String name) {
        CloudAPI.getInstance().getNetworkConnection().sendPacket(new PacketDBOutDeleteDocument(name, this.name));
        return this;
    }

    @Override
    public Database delete(final Document document) {
        CloudAPI.getInstance().getNetworkConnection().sendPacket(new PacketDBOutDeleteDocument(document, this.name));
        return this;
    }

    @Override
    public Document load(final String name) {
        return getDocument(name);
    }

    @Override
    public boolean contains(final Document document) {
        return contains(document.getString(Database.UNIQUE_NAME_KEY));
    }

    @Override
    public boolean contains(final String name) {
        final Result result = CloudAPI.getInstance().getNetworkConnection().getPacketManager().sendQuery(new PacketDBOutExistsDocument(name,
                                                                                                                                       this.name),
                                                                                                         CloudAPI.getInstance()
                                                                                                                 .getNetworkConnection());
        return result.getResult().getBoolean("exists");
    }

    @Override
    public int size() {
        final Result result = CloudAPI.getInstance().getNetworkConnection().getPacketManager().sendQuery(new PacketDBOutGetSize(name),
                                                                                                         CloudAPI.getInstance()
                                                                                                                 .getNetworkConnection());
        return result.getResult().getInt("size");
    }

    @Override
    public boolean containsDoc(final String name) {
        final Result result = CloudAPI.getInstance().getNetworkConnection().getPacketManager().sendQuery(new PacketDBOutExistsDocument(name,
                                                                                                                                       this.name),
                                                                                                         CloudAPI.getInstance()
                                                                                                                 .getNetworkConnection());
        return result.getResult().getBoolean("exists");
    }

    @Override
    public Database insertAsync(final Document... documents) {
        return insert(documents);
    }

    @Override
    public Database deleteAsync(final String name) {
        return delete(name);
    }

    @Deprecated
    @Override
    public FutureTask<Document> getDocumentAsync(final String name) {
        return new FutureTask<>(new Callable<Document>() {
            @Override
            public Document call() throws Exception {
                return getDocument(name);
            }
        });
    }
}
