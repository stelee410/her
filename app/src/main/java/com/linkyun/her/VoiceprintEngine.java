package com.linkyun.her;

interface VoiceprintEngine {
    int dimension();
    float[] embed(byte[] pcm16);
    float score(float[] enrolled, float[] candidate);
}
