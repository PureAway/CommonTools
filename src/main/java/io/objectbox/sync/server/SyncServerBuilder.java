package io.objectbox.sync.server;

import io.objectbox.BoxStore;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.sync.SyncCredentials;
import io.objectbox.sync.listener.SyncChangeListener;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a {@link SyncServer} and allows to set additional configuration.
 */
@SuppressWarnings({"unused", "UnusedReturnValue", "WeakerAccess"})
@Experimental
public class SyncServerBuilder {

    final BoxStore boxStore;
    final String url;
    final List<SyncCredentials> credentials = new ArrayList<>();
    final List<PeerInfo> peers = new ArrayList<>();

    @Nullable String certificatePath;
    SyncChangeListener changeListener;

    public SyncServerBuilder(BoxStore boxStore, String url, SyncCredentials authenticatorCredentials) {
        checkNotNull(boxStore, "BoxStore is required.");
        checkNotNull(url, "Sync server URL is required.");
        checkNotNull(authenticatorCredentials, "Authenticator credentials are required.");
        if (!BoxStore.isSyncServerAvailable()) {
            throw new IllegalStateException(
                    "This library does not include ObjectBox Sync Server. " +
                            "Please visit https://objectbox.io/sync/ for options.");
        }
        this.boxStore = boxStore;
        this.url = url;
        authenticatorCredentials(authenticatorCredentials);
    }

    public SyncServerBuilder certificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
        return this;
    }

    /**
     * Adds additional authenticator credentials to authenticate clients with.
     */
    public SyncServerBuilder authenticatorCredentials(SyncCredentials authenticatorCredentials) {
        checkNotNull(authenticatorCredentials, "Authenticator credentials must not be null.");
        credentials.add(authenticatorCredentials);
        return this;
    }

    /**
     * Sets a listener to observe fine granular changes happening during sync.
     * <p>
     * This listener can also be {@link SyncServer#setSyncChangeListener(SyncChangeListener) set or removed}
     * on the Sync server directly.
     */
    public SyncServerBuilder changeListener(SyncChangeListener changeListener) {
        this.changeListener = changeListener;
        return this;
    }

    /**
     * Adds a server peer, to which this server should connect to as a client using {@link SyncCredentials#none()}.
     */
    public SyncServerBuilder peer(String url) {
        return peer(url, SyncCredentials.none());
    }

    /**
     * Adds a server peer, to which this server should connect to as a client using the given credentials.
     */
    public SyncServerBuilder peer(String url, SyncCredentials credentials) {
        peers.add(new PeerInfo(url, credentials));
        return this;
    }

    /**
     * Builds and returns a Sync server ready to {@link SyncServer#start()}.
     *
     * Note: this clears all previously set authenticator credentials.
     */
    public SyncServer build() {
        if (credentials.isEmpty()) {
            throw new IllegalStateException("At least one authenticator is required.");
        }
        return new SyncServerImpl(this);
    }

    /**
     * Builds, {@link SyncServer#start() starts} and returns a Sync server.
     */
    public SyncServer buildAndStart() {
        SyncServer syncServer = build();
        syncServer.start();
        return syncServer;
    }

    private void checkNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

}
