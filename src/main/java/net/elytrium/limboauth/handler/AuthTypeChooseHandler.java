package net.elytrium.limboauth.handler;

import com.j256.ormlite.dao.Dao;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboauth.LimboAuth;
import net.elytrium.limboauth.Settings;
import net.elytrium.limboauth.model.AuthTypeRecord;
import net.elytrium.limboauth.model.SQLRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AuthTypeChooseHandler implements LimboSessionHandler {

    private final Dao<AuthTypeRecord, String> authTypeDao;
    private final Player proxyPlayer;
    private final LimboAuth plugin;

    private LimboPlayer player;


    private final long joinTime = System.currentTimeMillis();
    private ScheduledFuture<?> mainTask;
    private LimboFactory factory;

    private static Component timesUp;
    private static Component message;
    @Nullable
    private static Title title;

    public AuthTypeChooseHandler(Dao<AuthTypeRecord, String> authTypeDao, Player proxyPlayer, LimboAuth plugin) {
        this.authTypeDao = authTypeDao;
        this.proxyPlayer = proxyPlayer;
        this.plugin = plugin;
    }

    @Override
    public void onSpawn(Limbo server, LimboPlayer player) {
        this.player = player;
        this.factory = plugin.getLimboFactory();
        int authTime = Settings.IMP.MAIN.AUTH_TIME;
        String ip = proxyPlayer.getRemoteAddress().getAddress().getHostAddress();
        try {
            List<AuthTypeRecord> alreadyChosen = this.authTypeDao.queryForEq(AuthTypeRecord.IP, ip);
            if (alreadyChosen.size() >= Settings.IMP.MAIN.IP_LIMIT_REGISTRATIONS) {
                proxyPlayer.disconnect(AuthSessionHandler.ipLimitKick);
            }
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }

        mainTask = player.getScheduledExecutor().scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis() - this.joinTime > authTime) {
                this.proxyPlayer.disconnect(timesUp);
            }
        }, 1, 1, TimeUnit.SECONDS);

        sendMessage();
    }

    private void sendMessage() {
        proxyPlayer.sendMessage(message);
        proxyPlayer.showTitle(title);
    }

    @Override
    public void onChat(String chat) {
        if (Set.of("y", "yes", "/y", "/yes").contains(chat.toLowerCase())) {
            setAuthType(true);
        } else if (Set.of("n", "no", "/n", "/no").contains(chat.toLowerCase())) {
            setAuthType(false);
        } else {
            sendMessage();
        }
    }

    private void setAuthType(boolean online) {
        AuthTypeRecord record = new AuthTypeRecord(plugin.getOriginalName(proxyPlayer.getUsername()), online, proxyPlayer.getRemoteAddress().getAddress().getHostAddress());
        try {
            authTypeDao.createIfNotExists(record);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
        if (online) {
            if (proxyPlayer.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_20_5) >= 0) {
                proxyPlayer.getVirtualHost().ifPresent(proxyPlayer::transferToHost);
            }
            proxyPlayer.disconnect(Component.text("Set to online auth! please rejoin to auth").color(NamedTextColor.GREEN));
        } else {
            player.disconnect();
        }
    }

    @Override
    public void onDisconnect() {
        if (mainTask != null) {
            mainTask.cancel(true);
        }
    }

    public static void reload() {
        title = Title.title(
                MiniMessage.miniMessage().deserialize(Settings.IMP.MAIN.STRINGS_AUTH_CHOOSE.TITLE),
                MiniMessage.miniMessage().deserialize(Settings.IMP.MAIN.STRINGS_AUTH_CHOOSE.SUBTITLE),
                Settings.IMP.MAIN.CRACKED_TITLE_SETTINGS.toTimes()
        );
        message = MiniMessage.miniMessage().deserialize(Settings.IMP.MAIN.STRINGS_AUTH_CHOOSE.MESSAGE);
        timesUp = MiniMessage.miniMessage().deserialize(Settings.IMP.MAIN.STRINGS_AUTH_CHOOSE.TIMES_UP);
    }
}
