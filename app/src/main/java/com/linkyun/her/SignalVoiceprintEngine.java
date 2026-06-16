package com.linkyun.her;

final class SignalVoiceprintEngine implements VoiceprintEngine {
    static final int DIMENSION = 16;
    private static final int SAMPLE_RATE = 16000;
    private static final int WINDOW = 320;
    private static final int HOP = 160;
    private static final int[][] BANDS = {
            {90, 180}, {180, 300}, {300, 480}, {480, 720},
            {720, 1050}, {1050, 1500}, {1500, 2200}, {2200, 3200},
            {3200, 4500}, {4500, 6200}
    };

    @Override public int dimension() {
        return DIMENSION;
    }

    @Override public float[] embed(byte[] pcm16) {
        short[] samples = samples(pcm16);
        float[] features = new float[DIMENSION];
        if (samples.length < WINDOW) return features;
        int windows = 0;
        double rmsTotal = 0;
        double rmsSqTotal = 0;
        double zcrTotal = 0;
        double zcrSqTotal = 0;
        double centroidTotal = 0;
        double voiced = 0;
        for (int start = 0; start + WINDOW <= samples.length; start += HOP) {
            double rms = rms(samples, start, WINDOW);
            double zcr = zcr(samples, start, WINDOW);
            double centroid = 0;
            double bandEnergyTotal = 0.000001;
            for (int b = 0; b < BANDS.length; b++) {
                double energy = bandEnergy(samples, start, WINDOW, BANDS[b][0], BANDS[b][1]);
                double log = Math.log1p(energy);
                features[b] += (float) log;
                bandEnergyTotal += energy;
                centroid += energy * ((BANDS[b][0] + BANDS[b][1]) * 0.5);
            }
            rmsTotal += rms;
            rmsSqTotal += rms * rms;
            zcrTotal += zcr;
            zcrSqTotal += zcr * zcr;
            centroidTotal += centroid / bandEnergyTotal;
            if (rms > 450) voiced += 1.0;
            windows++;
        }
        if (windows == 0) return features;
        for (int i = 0; i < BANDS.length; i++) features[i] /= windows;
        double rmsMean = rmsTotal / windows;
        double zcrMean = zcrTotal / windows;
        features[10] = (float) Math.log1p(rmsMean);
        features[11] = (float) Math.sqrt(Math.max(0, rmsSqTotal / windows - rmsMean * rmsMean));
        features[12] = (float) zcrMean;
        features[13] = (float) Math.sqrt(Math.max(0, zcrSqTotal / windows - zcrMean * zcrMean));
        features[14] = (float) (centroidTotal / windows / SAMPLE_RATE);
        features[15] = (float) (voiced / windows);
        normalize(features);
        return features;
    }

    @Override public float score(float[] enrolled, float[] candidate) {
        if (enrolled == null || candidate == null) return 0;
        int n = Math.min(enrolled.length, candidate.length);
        if (n == 0) return 0;
        double dot = 0;
        double a = 0;
        double b = 0;
        for (int i = 0; i < n; i++) {
            dot += enrolled[i] * candidate[i];
            a += enrolled[i] * enrolled[i];
            b += candidate[i] * candidate[i];
        }
        if (a <= 0 || b <= 0) return 0;
        return (float) (dot / Math.sqrt(a * b));
    }

    private static short[] samples(byte[] pcm16) {
        if (pcm16 == null) return new short[0];
        short[] out = new short[pcm16.length / 2];
        for (int i = 0; i < out.length; i++) {
            int j = i * 2;
            out[i] = (short) ((pcm16[j] & 0xff) | (pcm16[j + 1] << 8));
        }
        return out;
    }

    private static double rms(short[] samples, int start, int count) {
        double sum = 0;
        for (int i = start; i < start + count; i++) {
            double v = samples[i];
            sum += v * v;
        }
        return Math.sqrt(sum / count);
    }

    private static double zcr(short[] samples, int start, int count) {
        int crossings = 0;
        for (int i = start + 1; i < start + count; i++) {
            if ((samples[i - 1] < 0 && samples[i] >= 0) ||
                    (samples[i - 1] >= 0 && samples[i] < 0)) crossings++;
        }
        return (double) crossings / Math.max(1, count - 1);
    }

    private static double bandEnergy(short[] samples, int start, int count, int lowHz, int highHz) {
        int lowBin = Math.max(1, Math.round(lowHz * count / (float) SAMPLE_RATE));
        int highBin = Math.max(lowBin, Math.round(highHz * count / (float) SAMPLE_RATE));
        double sum = 0;
        for (int bin = lowBin; bin <= highBin; bin++) {
            double real = 0;
            double imag = 0;
            double step = -2.0 * Math.PI * bin / count;
            for (int n = 0; n < count; n++) {
                double angle = step * n;
                double sample = samples[start + n];
                real += sample * Math.cos(angle);
                imag += sample * Math.sin(angle);
            }
            sum += real * real + imag * imag;
        }
        return sum / Math.max(1, highBin - lowBin + 1);
    }

    static void normalize(float[] values) {
        double norm = 0;
        for (float value : values) norm += value * value;
        if (norm <= 0) return;
        float scale = (float) (1.0 / Math.sqrt(norm));
        for (int i = 0; i < values.length; i++) values[i] *= scale;
    }
}
