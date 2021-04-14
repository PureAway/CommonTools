package io.objectbox.sync.server;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.sync.Sync;
import io.objectbox.sync.listener.SyncChangeListener;

import javax.annotation.Nullable;
import java.io.Closeable;

/**
 * ObjectBox sync server. Build a server with {@link Sync#server}.
 */
@SuppressWarnings("unused")
@Experimental
public interface SyncServer extends Closeable {

    /**
     * Gets the URL the server is running at.
     */
    String getUrl();

    /**
     * Gets the port the server has bound to.
     */
    int getPort();

    /**
     * Returns if the server is up and running.
     */
    boolean isRunning();

    /**
     * Gets some statistics from the sync server.
     */
    String getStatsString();

    /**
     * Sets a {@link SyncChangeListener}. Replaces a previously set listener.
     * Set to {@code null} to remove the listener.
     */
    void setSyncChangeListener(@Nullable SyncChangeListener listener);

    /**
     * Starts the server (e.g. bind to port) and gets everything operational.
     */
    void start();

    /**
     * Stops the server.
     */
    void stop();

    /**
     * Closes and cleans up all resources used by this sync server.
     * It can no longer be used afterwards, build a new sync server instead.
     * Does nothing if this sync server has already been closed.
     */
    void close();

}
