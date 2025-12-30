package com.friendzone.model;

import java.util.Map;

public class NetworkMessage {
    private String command;
    private Map<String, String> data;

    public NetworkMessage() {}

    public NetworkMessage(String command, Map<String, String> data) {
        this.command = command;
        this.data = data;
    }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public Map<String, String> getData() { return data; }
    public void setData(Map<String, String> data) { this.data = data; }
}
