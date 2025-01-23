package net.elytrium.limboauth.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "AUTH_TYPE")
public class AuthTypeRecord {
    public static final String LOWERCASE_NICKNAME_FIELD = "LOWERCASENICKNAME";
    public static final String ONLINE = "ONLINE";

    @DatabaseField(id = true, columnName = LOWERCASE_NICKNAME_FIELD)
    private String lowercaseNickname = "";

    @DatabaseField(columnName = ONLINE)
    private boolean online = false;

    public AuthTypeRecord() {
    }

    public AuthTypeRecord(String lowercaseNickname, boolean online) {
        this.lowercaseNickname = lowercaseNickname.toLowerCase();
        this.online = online;
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
}
