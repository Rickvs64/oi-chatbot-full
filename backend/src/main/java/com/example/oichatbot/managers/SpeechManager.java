package com.example.oichatbot.managers;

import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for text-to-speech conversion and audio output.
 * Singleton class.
 */
public class SpeechManager {
    private boolean shouldPlayAudio = true;
    private Map<String, SsmlVoiceGender> voiceTypes;
    private Map<String, Double> basePitches;
    private Map<String, Double> baseRates;
    private Float maxAdditionalPitch = 3.0f;        // Max amount of additional pitch based on emotions.
    private Float maxAdditionalRate = 0.3f;         // Max amount of additional rate based on emotions.
    private Float maxAdditionalVolume = 16.0f;         // Max amount of additional volume based on emotions.

    private static SpeechManager instance = null;

    public SpeechManager() {
        initVoiceTypes();
        initBasePitches();
        initBaseRates();
    }

    // Static method to maintain one persistent instance.
    public static SpeechManager getInstance()
    {
        if (instance == null)
            instance = new SpeechManager();

        return instance;
    }

    /**
     * Toggle audio output (inverts boolean).
     * @return Boolean (true if should play, false if muted).
     */
    public boolean toggleAudio() {
        shouldPlayAudio = !shouldPlayAudio;
        return shouldPlayAudio;
    }

    public String say(String inputText, String fileName) {
        String audioContent = "";

        // Instantiates a client.
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
            // Set the text input to be synthesized.
            SynthesisInput input = SynthesisInput.newBuilder()
                    .setText(inputText)
                    .build();

            // Build the voice request, select the language code ("en-US") and the ssml voice gender.
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode("en-US")
                    .setSsmlGender(determineVoiceType())
                    .build();

            // Select the type of audio file you want returned.
            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.MP3)
                    .setPitch(determineBasePitch() + determineAdditionalPitch())
                    .setSpeakingRate(determineBaseRate() + determineAdditionalRate())
                    .setVolumeGainDb(determineAdditionalVolume())
                    .build();

            // Perform the text-to-speech request on the text input with the selected voice parameters and
            // audio file type.
            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

            // Get the audio contents from the response.
            ByteString audioContents = response.getAudioContent();

            // Write the response to the output file.
            try (OutputStream out = new FileOutputStream(fileName)) {
                out.write(audioContents.toByteArray());
                System.out.println("Audio content written to file \"" + fileName + "\"");

                // This used to be handled in the back-end as a temporary test method.
                // playAudio(fileName);
                // Now we encode the audio file to Base64 so it can be sent in the original POST response.
                audioContent = encodeFileToBase64(new File(fileName));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return audioContent;
    }

    /**
     * Encode an audio file (.mp3)'s content to Base64.
     * @param file Audio file (preferably .mp3 format).
     * @return The Base64 content as String.
     */
    private String encodeFileToBase64(File file) {
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            throw new IllegalStateException("could not read file " + file, e);
        }
    }

    /**
     * Determine the right text-to-speech voice type depending on the current leading personality.
     * @return The recommended SsmlVoiceGender enum value.
     */
    private SsmlVoiceGender determineVoiceType() {
        return voiceTypes.get(PersonalityManager.getInstance().getLeadingPersonality());
    }

    /**
     * Initialize the voiceTypes Map with voice type recommendations.
     */
    private void initVoiceTypes() {
        voiceTypes = new HashMap<>();
        voiceTypes.put("Default", SsmlVoiceGender.NEUTRAL);
        voiceTypes.put("Desire", SsmlVoiceGender.FEMALE);
        voiceTypes.put("Curiosity", SsmlVoiceGender.MALE);
    }

    /**
     * Determine a recommended voice pitch based on the current leading personality trait.
     * @return The suggested (base) voice pitch.
     */
    private Double determineBasePitch() {
        return basePitches.get(PersonalityManager.getInstance().getLeadingPersonality());
    }

    /**
     * Initialize the basePitches Map with voice pitch recommendations.
     */
    private void initBasePitches() {
        basePitches = new HashMap<>();
        basePitches.put("Default", 0.0d);
        basePitches.put("Desire", 0.0d);
        basePitches.put("Curiosity", 2.5d);
    }

    /**
     * Determine a recommended speaking rate based on the current leading personality trait.
     * @return The suggested (base) speaking rate.
     */
    private Double determineBaseRate() {
        return baseRates.get(PersonalityManager.getInstance().getLeadingPersonality());
    }

    /**
     * Initialize the baseRates Map with speaking rate recommendations.
     */
    private void initBaseRates() {
        baseRates = new HashMap<>();
        baseRates.put("Default", 1.0d);
        baseRates.put("Desire", 0.7d);
        baseRates.put("Curiosity", 1.05d);
    }

    /**
     * Calculate the amount of extra pitch to be added (or substracted) to the final speech output.
     * Based on its current emotions.
     * @return The final addition to its base pitch.
     */
    private Double determineAdditionalPitch() {
        // Add an additional value based on the 'Patience' emotion.
        // In the future more emotions may be considered.
        Double value = 0.0d;

        // Multiplied by -1 since LOW patience requires HIGH pitch.
        value += (PersonalityManager.getInstance().getEmotions().get("Patience") * maxAdditionalPitch * -1.0f);

        System.out.println("Final voice pitch: " + value);
        return value;
    }

    /**
     * Calculate the amount of extra speed to be added to the final speech output.
     * Based on its current emotions.
     * @return The final addition to its base speaking rate.
     */
    private Double determineAdditionalRate() {
        // Add an additional value based on the 'Patience' emotion.
        // In the future more emotions may be considered.
        Double value = 0.0d;

        // Multiplied by -1 since LOW patience requires HIGH speaking rate.
        value += (PersonalityManager.getInstance().getEmotions().get("Patience") * maxAdditionalRate * -1.0f);

        value = clamp(value, 0.0d, 999.0d);

        System.out.println("Final speaking rate: " + value);
        return value;
    }

    /**
     * Calculate the amount of extra volume to be added to the final speech output.
     * Based on its current emotions.
     * @return The final addition to its base voice volume.
     */
    private Double determineAdditionalVolume() {
        // Add an additional value based on the 'Patience' emotion.
        // In the future more emotions may be considered.
        Double value = 0.0d;

        // Multiplied by -1 since LOW patience requires HIGH volume.
        value += (PersonalityManager.getInstance().getEmotions().get("Patience") * maxAdditionalVolume * -1.0f);

        value = clamp(value, 0.0d, 16.0d);      // +16db is Google's maximum audio gain.

        System.out.println("Final volume gain: " + value);
        return value;
    }

    /**
     * Clamp a float value between min and max. Interesting that Java does not have this by default.
     * @param value Value to clamp.
     * @param min Minimum.
     * @param max Maximum.
     * @return
     */
    private Double clamp(Double value, Double min, Double max) {
        return Math.max(min, Math.min(max, value));
    }

    public boolean shouldPlayAudio() {
        return shouldPlayAudio;
    }

    public void setShouldPlayAudio(boolean shouldPlayAudio) {
        this.shouldPlayAudio = shouldPlayAudio;
    }
}
