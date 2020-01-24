package com.example.oichatbot.managers;

import com.example.oichatbot.domains.Message;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

/**
 * Much like DialogFlowBridge, ExceptionManager is responsible for handling user input and determining the appropriate response.
 * Instead of relying on the DialogFlow API, this class simply returns an extreme (agitated) response regardless of user intent.
 * Singleton class.
 */
public class ExceptionManager {
    private Float maxExtremeChance = 0.5f;      // At the lowest possible Patience level, this is the max chance of an extreme response.
    private Float maxPatience = -0.3f;          // Patience has to be lower than this for an extreme response to even be considered.
    private Float maxPatienceCapslock = -0.9f;  // Response will be shown in all-caps if Patience is lower than this value.

    private Integer extremeFontSize = 3;
    private String extremeFont = "Comic Sans MS";
    private String extremeColor = "#ff0000";
    private List<String> responses;

    private static ExceptionManager instance = null;

    private ExceptionManager() {
        initRandomResponses();
    }

    // Static method to maintain one persistent instance.
    public static ExceptionManager getInstance() {
        if (instance == null)
            instance = new ExceptionManager();

        return instance;
    }

    /**
     * Determine if the application/bot should return an extreme response, based on current emotions.
     * @return True if an extreme response is recommended, false if not (and DialogFlow should be used instead).
     */
    public Boolean shouldRespondExtreme() {
        Float patience = PersonalityManager.getInstance().getEmotions().get("Patience");

        // Is patience currently too high for an extreme to be considered?
        if (patience > maxPatience)
            return false;

        // The lower patience drops, the lower it becomes as a normalized value (0-1).
        // Thus also making it MORE likely for a random normalized float to be higher.
        return ((new Random().nextFloat() * maxExtremeChance) > normalize(patience, -1.0f, maxPatience));
    }

    /**
     * Return a (random) extreme response/exception. Also slightly increases Patience afterwards.
     * Note that this method completely bypasses the usage of DialogFlowBridge.
     * Therefore it's important (like in DialogFlowBridge) we also e.g. check the input for emotion modifiers.
     * @return The complete message object (including font and audio properties).
     */
    public Message chatExtreme(String input) {
        System.out.println("Decided to show an EXTREME.");

        // Alter emotion values based on user input.
        PersonalityManager.getInstance().alterEmotions(input);

        // First we fetch a random string response to display in the front-end.
        Message output = new Message(getRandomStringResponse(), true);
        // If Patience is particularly low, the string response will be completely capitalized.
        if (PersonalityManager.getInstance().getEmotions().get("Patience") <= maxPatienceCapslock)
            output.setContent(output.getContent().toUpperCase());

        // Set the message object's properties like font and color.
        output.setFont(extremeFont);
        output.setFontSize(extremeFontSize);
        output.setSuggestedColor(extremeColor);

        // Set the message object's audio property (Base64) based on the relevant .mp3 file.
        if (SpeechManager.getInstance().shouldPlayAudio())
            output.setAudioFile(readAudioContent(output.getContent()));

        System.out.println("Returning extreme: " + output.getContent());

        // Slightly increase Patience to reduce the amount of successive extremes.
        PersonalityManager.getInstance().incrementEmotion("Patience", 0.2f);
        return output;
    }

    /**
     * Get a random extreme response string (e.g. "Go away!").
     * @return The randomly chosen string.
     */
    private String getRandomStringResponse() {
        int randomIndex = new Random().nextInt(responses.size());
        return responses.get(randomIndex);
    }

    /**
     * Initialize the randomResponses list.
     */
    private void initRandomResponses() {
        responses = new ArrayList<>();
        responses.add("Fuck you!");
        responses.add("Fuck off!");
        responses.add("Piss off!");
        responses.add("Not now!");
        responses.add("Go away!");
        responses.add("Shut up!");
        responses.add("Stop it!");
        responses.add("I'm done!");
    }

    /**
     * Based on the response string, read the relevant .mp3 file and return its contents as Base64.
     * @param response The response as string, e.g. "Go away!".
     * @return The Base64 file content of the relevant .mp3 audio file.
     */
    private String readAudioContent(String response) {
        // Converting the response string to an audio file name.
        // Set everything to lowercase, replace special characters (including whitespaces) and add .mp3 to its name.
        String fileName = response.toLowerCase();
        fileName = fileName.replaceAll("[^a-zA-Z0-9]", "");

        // Try to read an audio file with this exact matching name.
        return encodeFileToBase64(new File("extremes/" + fileName + ".mp3"));
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
     * Normalize a float to any given range.
     * @param value Value to normalize.
     * @param min Minimum range.
     * @param max Maximum range.
     * @return The normalized output of the input parameter (0-1).
     */
    private float normalize(float value, float min, float max) {
        return (value - min) / (max - min);
    }
}
