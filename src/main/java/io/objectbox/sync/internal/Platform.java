package io.objectbox.sync.internal;

import io.objectbox.BoxStore;
import io.objectbox.sync.ConnectivityMonitor;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Provides access to platform-specific features.
 */
public class Platform {

    public static Platform findPlatform() {
        // Android
        Object contextInstance = BoxStore.getContext();
        if (contextInstance != null) {
            Throwable throwable = null;

            // Note: do not catch Exception as it will swallow exceptions useful for debugging.
            // Also can't catch ReflectiveOperationException, is K+ (19+) on Android.
            // noinspection TryWithIdenticalCatches Requires Android K+ (19+).
            try {
                Class<?> contextClass = Class.forName("android.content.Context");
                Class<?> platformClass = Class.forName("io.objectbox.android.internal.AndroidPlatform");
                Method create = platformClass.getMethod("create", contextClass);
                return (Platform) create.invoke(null, contextInstance);
            } catch (NoSuchMethodException e) {
                throwable = e;
            } catch (IllegalAccessException e) {
                throwable = e;
            } catch (InvocationTargetException e) {
                throwable = e;
            } catch (ClassNotFoundException ignored) {
                // Android API or library not in classpath.
            }

            if (throwable != null) {
                throw new RuntimeException("AndroidPlatform could not be created.", throwable);
            }
        }

        return new Platform();
    }

    @Nullable
    public ConnectivityMonitor getConnectivityMonitor() {
        return null;
    }
}
