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

package net.elytrium.limboauth.model;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.velocitypowered.api.proxy.Player;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.UUID;
import net.elytrium.limboauth.Settings;

@DatabaseTable(tableName = "AUTH")
public class RegisteredPlayer {

  public static final String UUID_FIELD = "UUID";
  public static final String NICKNAME_FIELD = "NICKNAME";
  public static final String LOWERCASE_NICKNAME_FIELD = "LOWERCASENICKNAME";
  public static final String HASH_FIELD = "HASH";
  public static final String IP_FIELD = "IP";
  public static final String LOGIN_IP_FIELD = "LOGINIP";
  public static final String TOTP_TOKEN_FIELD = "TOTPTOKEN";
  public static final String REG_DATE_FIELD = "REGDATE";
  public static final String LOGIN_DATE_FIELD = "LOGINDATE";
  public static final String PREMIUM_UUID_FIELD = "PREMIUMUUID";
  public static final String ACCOUNT_TYPE_FIELD = "ACCOUNTTYPE";
  public static final String TOKEN_ISSUED_AT_FIELD = "ISSUEDTIME";

  private static final BCrypt.Hasher HASHER = BCrypt.withDefaults();

  @DatabaseField(id = true, columnName = UUID_FIELD)
  private String uuid = "";

  @DatabaseField(canBeNull = false, columnName = NICKNAME_FIELD)
  private String nickname;

  @DatabaseField(columnName = LOWERCASE_NICKNAME_FIELD, index = true)
  private String lowercaseNickname;

  @DatabaseField(canBeNull = false, columnName = HASH_FIELD)
  private String hash = "";

  @DatabaseField(columnName = IP_FIELD, index = true)
  private String ip;

  @DatabaseField(columnName = TOTP_TOKEN_FIELD)
  private String totpToken = "";

  @DatabaseField(columnName = REG_DATE_FIELD)
  private Long regDate = System.currentTimeMillis();

  @DatabaseField(columnName = RegisteredPlayer.PREMIUM_UUID_FIELD, index = true)
  private String premiumUuid = "";

  @DatabaseField(columnName = ACCOUNT_TYPE_FIELD)
  private String accountType;

  @DatabaseField(columnName = LOGIN_IP_FIELD)
  private String loginIp;

  @DatabaseField(columnName = LOGIN_DATE_FIELD)
  private Long loginDate = System.currentTimeMillis();

  @DatabaseField(columnName = TOKEN_ISSUED_AT_FIELD)
  private Long tokenIssuedAt = System.currentTimeMillis();

  @Deprecated
  public RegisteredPlayer(String nickname, String lowercaseNickname,
      String hash, String ip, String totpToken, Long regDate, String uuid, String accountType, String premiumUuid, String loginIp, Long loginDate) {
    this.nickname = nickname;
    this.lowercaseNickname = lowercaseNickname;
    this.hash = hash;
    this.ip = ip;
    this.totpToken = totpToken;
    this.regDate = regDate;
    this.uuid = uuid;
    this.accountType = accountType;
    this.premiumUuid = premiumUuid;
    this.loginIp = loginIp;
    this.loginDate = loginDate;
  }

  public RegisteredPlayer(Player player, String accountType) {
    this(player.getUsername(), player.getUniqueId(), player.getRemoteAddress(), accountType);
  }
  public RegisteredPlayer(String nickname, UUID uuid, InetSocketAddress ip, String accountType) {
    this(nickname, uuid.toString(), ip.getAddress().getHostAddress(), accountType);
  }

  public RegisteredPlayer(String nickname, String uuid, String ip, String accountType) {
    this.nickname = nickname;
    this.lowercaseNickname = nickname.toLowerCase(Locale.ROOT);
    this.uuid = uuid;
    this.ip = ip;
    this.loginIp = ip;
    this.accountType = accountType;
  }

  public RegisteredPlayer() {

  }

  public static String genHash(String password) {
    return HASHER.hashToString(Settings.IMP.MAIN.BCRYPT_COST, password.toCharArray());
  }

  public RegisteredPlayer setNickname(String nickname) {
    this.nickname = nickname;
    this.lowercaseNickname = nickname.toLowerCase(Locale.ROOT);

    return this;
  }

  public String getNickname() {
    return this.nickname == null ? this.lowercaseNickname : this.nickname;
  }

  public String getLowercaseNickname() {
    return this.lowercaseNickname;
  }

  public RegisteredPlayer setPassword(String password) {
    this.hash = genHash(password);
    this.tokenIssuedAt = System.currentTimeMillis();

    return this;
  }

  public RegisteredPlayer setHash(String hash) {
    this.hash = hash;
    this.tokenIssuedAt = System.currentTimeMillis();

    return this;
  }

  public String getHash() {
    return this.hash == null ? "" : this.hash;
  }

  public RegisteredPlayer setIP(String ip) {
    this.ip = ip;

    return this;
  }

  public String getIP() {
    return this.ip == null ? "" : this.ip;
  }

  public RegisteredPlayer setTotpToken(String totpToken) {
    this.totpToken = totpToken;

    return this;
  }

  public String getTotpToken() {
    return this.totpToken == null ? "" : this.totpToken;
  }

  public RegisteredPlayer setRegDate(Long regDate) {
    this.regDate = regDate;

    return this;
  }

  public long getRegDate() {
    return this.regDate == null ? Long.MIN_VALUE : this.regDate;
  }

  public RegisteredPlayer setUuid(String uuid) {
    this.uuid = uuid;

    return this;
  }

  public String getUuid() {
    return this.uuid == null ? "" : this.uuid;
  }

  public RegisteredPlayer setPremiumUuid(String premiumUuid) {
    this.premiumUuid = premiumUuid;

    return this;
  }

  public RegisteredPlayer setAccountType(String accountType) {
    this.accountType = accountType;

    return this;
  }

  public String getAccountType() {
    return accountType;
  }

  public RegisteredPlayer setPremiumUuid(UUID premiumUuid) {
    this.premiumUuid = premiumUuid.toString();

    return this;
  }

  public String getPremiumUuid() {
    return this.premiumUuid == null ? "" : this.premiumUuid;
  }

  public String getLoginIp() {
    return this.loginIp == null ? "" : this.loginIp;
  }

  public RegisteredPlayer setLoginIp(String loginIp) {
    this.loginIp = loginIp;

    return this;
  }

  public long getLoginDate() {
    return this.loginDate == null ? Long.MIN_VALUE : this.loginDate;
  }

  public RegisteredPlayer setLoginDate(Long loginDate) {
    this.loginDate = loginDate;

    return this;
  }

  public long getTokenIssuedAt() {
    return this.tokenIssuedAt == null ? Long.MIN_VALUE : this.tokenIssuedAt;
  }

  public RegisteredPlayer setTokenIssuedAt(Long tokenIssuedAt) {
    this.tokenIssuedAt = tokenIssuedAt;

    return this;
  }

  @Override
  public String toString() {
    return "RegisteredPlayer{" +
            "uuid='" + uuid + '\'' +
            ", nickname='" + nickname + '\'' +
            ", lowercaseNickname='" + lowercaseNickname + '\'' +
            ", hash='" + hash + '\'' +
            ", ip='" + ip + '\'' +
            ", totpToken='" + totpToken + '\'' +
            ", regDate=" + regDate +
            ", premiumUuid='" + premiumUuid + '\'' +
            ", accountType='" + accountType + '\'' +
            ", loginIp='" + loginIp + '\'' +
            ", loginDate=" + loginDate +
            ", tokenIssuedAt=" + tokenIssuedAt +
            '}';
  }
}
