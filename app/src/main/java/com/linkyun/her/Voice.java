package com.linkyun.her;

final class Voice {
    final String id;
    final String label;
    final String gender;
    final String resource;

    Voice(String id, String label, String gender) {
        this(id, label, gender, "");
    }

    Voice(String id, String label, String gender, String resource) {
        this.id = id;
        this.label = label;
        this.gender = gender;
        this.resource = resource == null ? "" : resource;
    }
}
