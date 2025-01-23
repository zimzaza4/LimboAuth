/*
 * Copyright (C) 2021 - 2024 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboauth.listener;

import com.j256.ormlite.dao.Dao;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.InitialInboundConnection;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.elytrium.limboauth.LimboAuth;
import net.elytrium.limboauth.LimboAuth.CachedPremiumUser;
import net.elytrium.limboauth.LimboAuth.PremiumState;
import net.elytrium.limboauth.Settings;
import net.elytrium.limboauth.floodgate.FloodgateApiHolder;
import net.elytrium.limboauth.model.AccountType;
import net.elytrium.limboauth.model.RegisteredPlayer;
import net.elytrium.limboauth.model.SQLRuntimeException;
import net.kyori.adventure.text.Component;

// TODO: Customizable events priority
public class AuthListener {

  private static final MethodHandle DELEGATE_FIELD;
  //private static final MethodHandle LOGIN_FIELD;

  private final LimboAuth plugin;
  private final Dao<RegisteredPlayer, String> playerDao;
  private final FloodgateApiHolder floodgateApi;
  private final Component errorOccurred;
  private final Component kickSameName;

  public AuthListener(LimboAuth plugin, Dao<RegisteredPlayer, String> playerDao, FloodgateApiHolder floodgateApi) {
    this.plugin = plugin;
    this.playerDao = playerDao;
    this.floodgateApi = floodgateApi;

    this.errorOccurred = LimboAuth.getSerializer().deserialize(Settings.IMP.MAIN.STRINGS.ERROR_OCCURRED);
    this.kickSameName = LimboAuth.getSerializer().deserialize(Settings.IMP.MAIN.STRINGS.KICK_SAME_NAME);
  }

  @Subscribe(order = PostOrder.LATE)
  public void onPreLoginEvent(PreLoginEvent event) {
    // Ignore this event if it is was denied by other plugin
    if (!event.getResult().isAllowed()) {
      return;
    }

    try {
      String username = event.getUsername();
      Optional<InetSocketAddress> host = event.getConnection().getVirtualHost();
      String hostString = "";
      if (host.isPresent()) {
        hostString = host.get().getHostString();
      }
      if (!event.getResult().isForceOfflineMode()) {
        if (this.plugin.isPremium(hostString, event.getUsername())) {
          event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());

          try {
            if (!Settings.IMP.MAIN.ONLINE_MODE_NEED_AUTH_STRICT) {
              CachedPremiumUser premiumUser = this.plugin.getPremiumCache(username);
              MinecraftConnection connection = this.getConnection(event.getConnection());
              if (!connection.isClosed() && premiumUser != null && !premiumUser.isForcePremium()
                  && this.plugin.isPremiumInternal(username.toLowerCase(Locale.ROOT)).getState() == PremiumState.UNKNOWN) {
                this.plugin.getPendingLogins().add(username);

                // As Velocity doesnt have any events for our usecase, just inject into netty
                connection.getChannel().closeFuture().addListener(future -> {
                  // Player has failed premium verfication client-side, mark as offline-mode
                  if (this.plugin.getPendingLogins().remove(username)) {
                    this.plugin.setPremiumCacheLowercased(username.toLowerCase(Locale.ROOT), false);
                  }
                });
              }
            }
          } catch (Throwable throwable) {
            throw new IllegalStateException("failed to track authentication process", throwable);
          }
        } else {
          event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
        }
      } else {
        try {
          MinecraftConnection connection = this.getConnection(event.getConnection());
          if (!connection.isClosed()) {
            this.plugin.saveForceOfflineMode(username);

            // As Velocity doesnt have any events for our usecase, just inject into netty
            connection.getChannel().closeFuture().addListener(future -> {
              this.plugin.unsetForcedPreviously(username);
            });
          }
        } catch (Throwable throwable) {
          throw new IllegalStateException("failed to track client disconnection", throwable);
        }
      }
    } catch (Throwable throwable) {
      event.setResult(PreLoginComponentResult.denied(this.errorOccurred));
      throw throwable;
    }
  }

  private MinecraftConnection getConnection(InboundConnection inbound) throws Throwable {
    LoginInboundConnection inboundConnection = (LoginInboundConnection) inbound;
    InitialInboundConnection initialInbound = (InitialInboundConnection) DELEGATE_FIELD.invokeExact(inboundConnection);
    return initialInbound.getConnection();
  }

  // Temporarily disabled because some clients send UUID version 4 (random UUID) even if the player is cracked
  /*
  private boolean isPremiumByIdentifiedKey(InboundConnection inbound) throws Throwable {
    LoginInboundConnection inboundConnection = (LoginInboundConnection) inbound;
    InitialInboundConnection initialInbound = (InitialInboundConnection) DELEGATE_FIELD.invokeExact(inboundConnection);
    MinecraftConnection connection = initialInbound.getConnection();
    InitialLoginSessionHandler handler = (InitialLoginSessionHandler) connection.getSessionHandler();

    ServerLogin packet = (ServerLogin) LOGIN_FIELD.invokeExact(handler);
    if (packet == null) {
      return false;
    }

    UUID holder = packet.getHolderUuid();
    if (holder == null) {
      return false;
    }

    return holder.version() != 3;
  }
  */

  @Subscribe
  public void onPostLogin(PostLoginEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();
    Runnable postLoginTask = this.plugin.getPostLoginTasks().remove(uuid);
    if (postLoginTask != null) {
      // We need to delay for player's client to finish switching the server, it takes a little time.
      this.plugin.getServer().getScheduler()
          .buildTask(this.plugin, postLoginTask)
          .delay(Settings.IMP.MAIN.PREMIUM_AND_FLOODGATE_MESSAGES_DELAY, TimeUnit.MILLISECONDS)
          .schedule();
    }
  }

  @Subscribe
  public void onLoginLimboRegister(LoginLimboRegisterEvent event) {
    // Player has completed online-mode authentication, can be sure that the player has premium account
    if (event.getPlayer().isOnlineMode()) {
      CachedPremiumUser premiumUser = this.plugin.getPremiumCache(event.getPlayer().getUsername());
      if (premiumUser != null) {
        premiumUser.setForcePremium(true);
      }

      this.plugin.getPendingLogins().remove(event.getPlayer().getUsername());
    }

    if (this.plugin.needChooseAuthType(event.getPlayer())) {
      event.addOnJoinCallback(() -> this.plugin.chooseAuthTypeForPlayer(event.getPlayer()));
    }

    if (this.plugin.needAuth(event.getPlayer())) {
      event.addOnJoinCallback(() -> this.plugin.authPlayer(event.getPlayer()));
    }
  }

  @Subscribe(order = PostOrder.FIRST)
  public void onGameProfileRequest(GameProfileRequestEvent event) {
    if (Settings.IMP.MAIN.FORCE_OFFLINE_UUID) {
      event.setGameProfile(event.getOriginalProfile().withId(UuidUtils.generateOfflinePlayerUuid(event.getUsername())));
    }

    if (event.isOnlineMode()) {
      for (Player player : this.plugin.getServer().getAllPlayers()) {
        if (!player.isOnlineMode()) {
          if (player.getUsername().equalsIgnoreCase(event.getOriginalProfile().getName())) {
            player.disconnect(kickSameName);
            updateOfflineName(event.getOriginalProfile().getName());
          }
        }
      }
      if (!Settings.IMP.MAIN.ONLINE_MODE_PREFIX.isEmpty()) {
        event.setGameProfile(event.getOriginalProfile().withName(Settings.IMP.MAIN.ONLINE_MODE_PREFIX + event.getUsername()));
      }
    }

    if (!event.isOnlineMode()) {
      UUID uuid = UuidUtils.generateOfflinePlayerUuid(event.getUsername().toLowerCase(Locale.ROOT));
      event.setGameProfile(event.getOriginalProfile().withId(uuid));
      event.setGameProfile(event.getOriginalProfile().withName(getOfflinePlayerName(uuid, event.getUsername())));
    }
  }

  private void updateOfflineName(String name) {
      try {
        List<RegisteredPlayer> onlinePlayers = playerDao.queryForFieldValues(Map.of(RegisteredPlayer.LOWERCASE_NICKNAME_FIELD, name.toLowerCase(), RegisteredPlayer.ACCOUNT_TYPE_FIELD, AccountType.OFFLINE));
        for (RegisteredPlayer player : onlinePlayers) {
          if (!player.getNickname().startsWith(Settings.IMP.MAIN.OFFLINE_MODE_PREFIX)) {
            player.setNickname(Settings.IMP.MAIN.OFFLINE_MODE_PREFIX + player.getNickname());
          }
          playerDao.update(player);
        }
      } catch (SQLException e) {
          throw new SQLRuntimeException(e);
      }
  }

  public String getOfflinePlayerName(UUID uuid, String name) {
      try {
        RegisteredPlayer player = playerDao.queryForId(uuid.toString());
        List<RegisteredPlayer> onlinePlayers = playerDao.queryForFieldValues(Map.of(RegisteredPlayer.LOWERCASE_NICKNAME_FIELD, name.toLowerCase(), RegisteredPlayer.ACCOUNT_TYPE_FIELD, AccountType.ONLINE));
        String result = name;
        if (player != null) {
          result = player.getNickname();
        }
        if (!onlinePlayers.isEmpty()) {
          if (!result.startsWith(Settings.IMP.MAIN.OFFLINE_MODE_PREFIX)) {
            result = Settings.IMP.MAIN.OFFLINE_MODE_PREFIX + result;
          }
        } else {
          if (result.startsWith(Settings.IMP.MAIN.OFFLINE_MODE_PREFIX)) {
            result = result.substring(Settings.IMP.MAIN.OFFLINE_MODE_PREFIX.length());
          }
        }
        return result;
      } catch (SQLException e) {
          throw new SQLRuntimeException(e);
      }
  }

  static {
    try {
      DELEGATE_FIELD = MethodHandles.privateLookupIn(LoginInboundConnection.class, MethodHandles.lookup())
          .findGetter(LoginInboundConnection.class, "delegate", InitialInboundConnection.class);
      //LOGIN_FIELD = MethodHandles.privateLookupIn(InitialLoginSessionHandler.class, MethodHandles.lookup())
      //    .findGetter(InitialLoginSessionHandler.class, "login",ServerLoginPacket.class);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new ReflectionException(e);
    }
  }
}
