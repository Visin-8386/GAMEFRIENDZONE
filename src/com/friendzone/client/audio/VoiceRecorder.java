package com.friendzone.client.audio;

import java.io.*;
import javax.sound.sampled.*;

/**
 * Utility class for recording voice messages
 */
public class VoiceRecorder {
    private TargetDataLine microphone;
    private AudioFormat format;
    private ByteArrayOutputStream recordedData;
    private boolean recording = false;
    private Thread recordingThread;
    
    // Audio format for voice message: 16kHz, 16-bit, mono
    private static final AudioFormat VOICE_FORMAT = new AudioFormat(
        16000f,  // Sample rate
        16,      // Sample size in bits
        1,       // Channels (mono)
        true,    // Signed
        false    // Little endian
    );
    
    public VoiceRecorder() {
        this.format = VOICE_FORMAT;
        this.recordedData = new ByteArrayOutputStream();
    }
    
    /**
     * Start recording audio from microphone
     */
    public boolean startRecording() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Microphone not supported");
                return false;
            }
            
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            
            recording = true;
            recordedData.reset();
            
            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (recording) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        recordedData.write(buffer, 0, bytesRead);
                    }
                }
            });
            recordingThread.start();
            
            return true;
        } catch (LineUnavailableException e) {
            System.err.println("Error starting recording: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Stop recording and return audio data as WAV bytes
     */
    public byte[] stopRecording() {
        if (!recording) return null;
        
        recording = false;
        
        try {
            if (recordingThread != null) {
                recordingThread.join(1000);
            }
        } catch (InterruptedException e) {
            // Ignore
        }
        
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
        
        // Convert raw audio data to WAV format
        return convertToWav(recordedData.toByteArray());
    }
    
    /**
     * Convert raw PCM audio data to WAV format
     */
    private byte[] convertToWav(byte[] audioData) {
        try {
            ByteArrayOutputStream wavOutput = new ByteArrayOutputStream();
            
            // Write WAV header
            writeWavHeader(wavOutput, audioData.length, format);
            
            // Write audio data
            wavOutput.write(audioData);
            
            return wavOutput.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Write WAV file header
     */
    private void writeWavHeader(ByteArrayOutputStream out, int audioDataLength, AudioFormat format) throws IOException {
        int sampleRate = (int) format.getSampleRate();
        int channels = format.getChannels();
        int bitsPerSample = format.getSampleSizeInBits();
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        
        // RIFF header
        out.write("RIFF".getBytes());
        writeInt(out, 36 + audioDataLength); // File size - 8
        out.write("WAVE".getBytes());
        
        // fmt chunk
        out.write("fmt ".getBytes());
        writeInt(out, 16); // Chunk size
        writeShort(out, 1); // Audio format (1 = PCM)
        writeShort(out, channels);
        writeInt(out, sampleRate);
        writeInt(out, byteRate);
        writeShort(out, blockAlign);
        writeShort(out, bitsPerSample);
        
        // data chunk
        out.write("data".getBytes());
        writeInt(out, audioDataLength);
    }
    
    private void writeInt(ByteArrayOutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }
    
    private void writeShort(ByteArrayOutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }
    
    /**
     * Get duration of recorded audio in seconds
     */
    public int getDuration(byte[] wavData) {
        if (wavData == null || wavData.length < 44) return 0;
        
        // Parse WAV header to get duration
        int dataSize = wavData.length - 44; // Subtract header size
        int sampleRate = (int) format.getSampleRate();
        int bytesPerSample = format.getSampleSizeInBits() / 8;
        int channels = format.getChannels();
        
        int samples = dataSize / (bytesPerSample * channels);
        return samples / sampleRate;
    }
    
    public boolean isRecording() {
        return recording;
    }
}
