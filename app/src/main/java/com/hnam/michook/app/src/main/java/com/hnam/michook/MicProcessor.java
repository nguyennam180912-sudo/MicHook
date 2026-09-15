package com.hnam.michook;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

public class MicProcessor {

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static AudioRecord audioRecord;
    private static AudioTrack audioTrack;
    private static Thread processThread;
    private static volatile boolean running = false;
    private static volatile float gainValue = 10f;

    public static boolean isRunning() { return running; }

    public static void start(Context context) {
        if (running) return;

        int minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, FORMAT);

        audioRecord = new AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE, CHANNEL, FORMAT, minBuf * 2);

        audioTrack = new AudioTrack(
            AudioManager.STREAM_VOICE_CALL,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            FORMAT, minBuf * 2, AudioTrack.MODE_STREAM);

        audioRecord.startRecording();
        audioTrack.play();
        running = true;

        processThread = new Thread(() -> {
            short[] buffer = new short[minBuf];
            while (running) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0) {
                    for (int i = 0; i < read; i++) {
                        int sample = (int) (buffer[i] * gainValue);
                        if (sample > Short.MAX_VALUE) sample = Short.MAX_VALUE;
                        if (sample < Short.MIN_VALUE) sample = Short.MIN_VALUE;
                        buffer[i] = (short) sample;
                    }
                    audioTrack.write(buffer, 0, read);
                }
            }
        });
        processThread.start();
    }

    public static void stop() {
        running = false;
        try { if (processThread != null) processThread.join(500); } catch (Exception ignored) {}
        if (audioRecord != null) { try { audioRecord.stop(); } catch (Exception e) {} audioRecord.release(); audioRecord = null; }
        if (audioTrack != null) { try { audioTrack.stop(); } catch (Exception e) {} audioTrack.release(); audioTrack = null; }
    }
        }
