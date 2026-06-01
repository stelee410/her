package com.linkyun.her;

final class Message {
    final String id;
    final String role;
    String text;
    final long timestamp = System.currentTimeMillis();

    Message(String id, String role, String text) {
        this.id = id;
        this.role = role;
        this.text = text;
    }
}
