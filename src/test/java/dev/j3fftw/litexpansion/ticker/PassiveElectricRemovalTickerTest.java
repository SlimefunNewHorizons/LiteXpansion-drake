package dev.j3fftw.litexpansion.ticker;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PassiveElectricRemovalTickerTest {

    @AfterEach
    void resetBukkitServer() throws Exception {
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, null);
    }

    private void setServerProxy(boolean primaryThread, AtomicBoolean onlinePlayersQueried) throws Exception {
        Server proxy = (Server) Proxy.newProxyInstance(
            Server.class.getClassLoader(),
            new Class<?>[]{Server.class},
            (p, method, args) -> {
                switch (method.getName()) {
                    case "isPrimaryThread":
                        return primaryThread;
                    case "getOnlinePlayers":
                        onlinePlayersQueried.set(true);
                        return Collections.emptyList();
                    case "getLogger":
                        return Logger.getAnonymousLogger();
                    case "getName":
                    case "getVersion":
                    case "getBukkitVersion":
                        return "TestServer";
                    default:
                        return null;
                }
            }
        );
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, proxy);
    }

    @Test
    void doesNotReadOnlinePlayerInventoriesOffPrimaryThread() throws Exception {
        AtomicBoolean onlinePlayersQueried = new AtomicBoolean(false);
        setServerProxy(false, onlinePlayersQueried);

        new PassiveElectricRemovalTicker().run();

        assertFalse(onlinePlayersQueried.get(),
            "PassiveElectricRemovalTicker must not read player inventories off the primary thread");
    }

    @Test
    void ticksOnlinePlayersDirectlyOnPrimaryThread() throws Exception {
        AtomicBoolean onlinePlayersQueried = new AtomicBoolean(false);
        setServerProxy(true, onlinePlayersQueried);

        new PassiveElectricRemovalTicker().run();

        assertTrue(onlinePlayersQueried.get(),
            "PassiveElectricRemovalTicker should scan online players when invoked on primary thread");
    }
}
