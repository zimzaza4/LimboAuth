package net.elytrium.limboauth.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "AUTH_TYPE")
public class AuthTypeRecord {
    public static final String LOWERCASE_NICKNAME_FIELD = "LOWERCASENICKNAME";
    public static final String ONLINE = "ONLINE";
    public static final String IP = "IP";

    @DatabaseField(id = true, columnName = LOWERCASE_NICKNAME_FIELD)
    private String lowercaseNickname = "";

    @DatabaseField(columnName = ONLINE)
    private boolean online = false;

    @DatabaseField(columnName = IP, index = true)
    private String ip;


    public AuthTypeRecord() {
    }

    public AuthTypeRecord(String lowercaseNickname, boolean online, String ip) {
        this.lowercaseNickname = lowercaseNickname.toLowerCase();
        this.online = online;
        this.ip = ip;
    }

    public String getLowercaseNickname() {
        return lowercaseNickname;
    }

    public void setLowercaseNickname(String lowercaseNickname) {
        this.lowercaseNickname = lowercaseNickname;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }
}
