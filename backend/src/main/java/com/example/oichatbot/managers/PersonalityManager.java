package com.example.oichatbot.managers;

import com.example.oichatbot.domains.EmotionModifier;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.List;

/**
 * Tracks all current character qualities and emotions.
 * Singleton class - may seem redundant for now since DialogFlowBridge is a singleton too.
 */
public class PersonalityManager {

    private Map<String, Float> emotions;    // Emotions that range from -1.0f to 1.0f.
    private Map<String, Float> personality;     // Personality traits that range from 0.0f to 1.0f.
    private Map<String, String> colors;     // Colors assigned to specific (extreme) emotions.

    private boolean allowDynamicEmotions = false;
    private boolean allowDynamicPersonality = false;

    private List<EmotionModifier> modifiers;    // Emotion modifiers that alter the chatbot's behavior based on input terms.
    private Float globalModifyMultiplier = 2.0f;    // Global emotion modifier scale. Higher values mean more significant "mood swings".

    private static PersonalityManager instance = null;

    private PersonalityManager() {
        initEmotions();
        initPersonality();
        initColors();
        modifiers = readEmotionModifiersFromFile("modifiers.json");
        temp();
    }

    // Static method to maintain one persistent instance.
    public static PersonalityManager getInstance() {
        if (instance == null)
            instance = new PersonalityManager();

        return instance;
    }

    private void initEmotions() {
        emotions = new HashMap<>();

        // Patience (1.0f) <---> Frustration (-1.0f).
        emotions.put("Patience", 0.0f);
    }

    private void initPersonality() {
        personality = new HashMap<>();

        // Desire -> high values lead to expressing attraction and occasional dirty talk.
        personality.put("Desire", 0.0f);
        // Curiosity -> high values lead to asking many questions and potentially coming across as "nosy".
        personality.put("Curiosity", 0.0f);

        // Default threshold -> other emotions need to be higher than this value otherwise the default intents will be used.
        personality.put("Default", 0.2f);
    }

    public String getLeadingPersonality() {
        return getHighestKeyInMap(personality);
    }

    private String getHighestKeyInMap(Map<String, Float> map) {
        // Separate into two arrays.
        List<String> strings = new ArrayList<String>();
        List<Float> floats = new ArrayList<Float>();
        for (Map.Entry<String, Float> entry : map.entrySet()) {
            strings.add(entry.getKey());
            floats.add(entry.getValue());
        }

        Float highestValue = 0.0f;
        int highestIndex = 0;
        // Turns values positive first with Math.abs (so an emotion at -0.8 is stronger than 0.7).
        for (int i = 0; i < strings.size(); i++){
            if (floats.get(i) > Math.abs(highestValue)) {
                highestValue = floats.get(i);
                highestIndex = i;
            }
        }

        return strings.get(highestIndex);
    }

    private void initColors() {
        colors = new HashMap<>();

        // Every possible emotion needs a LOW and HIGH defined extreme.
        colors.put("Patience_LOW", "#f08080");
        colors.put("Patience_HIGH", "#b0e0e6");
    }

    public String determineSuggestedColor() {
        // First check for the currently strongest emotion.
        String emotion = getHighestKeyInMap(emotions);

        // Get LOW and HIGH color variant of the chosen emotion.
        Color colorLow = Color.decode(colors.get(emotion + "_LOW"));
        Color colorHigh = Color.decode(colors.get(emotion + "_HIGH"));

        // Interpolate between LOW and HIGH variants to get the appropriate color value.
        // First we need to convert emotions' range (-1 to 1) to a standard lerp alpha (0 to 1).
        Float alpha = normalizeToRange(emotions.get(emotion), -1.0f, 1.0f);
        // Parse as HEX string, does NOT support transparency.
        return "#" + Integer.toHexString(lerpColors(colorHigh, colorLow, alpha).getRGB()).substring(2);
    }

    private Color lerpColors(Color c1, Color c2, Float alpha) {
        Float inverse = 1.0f - alpha;
        int r = (int) (c1.getRed() * alpha + c2.getRed() * inverse);
        int g = (int) (c1.getGreen() * alpha + c2.getGreen() * inverse);
        int b = (int) (c1.getBlue() * alpha + c2.getBlue() * inverse);
        return new Color(r, g, b);
    }

    private Float normalizeToRange(float value, float min, float max) {
        return (value - min) / (max - min);
    }

    /**
     * Read a list of EmotionModifiers from a locally stored file.
     * @param fileName Name of file to open.
     * @return List of EmotionModifiers read from the given file.
     */
    private List<EmotionModifier> readEmotionModifiersFromFile(String fileName) {
        try {
            InputStream is = new FileInputStream(fileName);
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            String line = buf.readLine();
            StringBuilder sb = new StringBuilder();
            while (line != null) {
                sb.append(line).append("\n");
                line = buf.readLine();
            }
            String fileAsString = sb.toString();

            EmotionModifier[] modifierArray = new Gson().fromJson(fileAsString, EmotionModifier[].class);
            return Arrays.asList(modifierArray);
        }
        catch (Exception e) {
            System.out.println(e);
            return new ArrayList<EmotionModifier>();
        }

    }

    /**
     * Write a list of standard emotions to a file.
     * @param fileName Name of file to write to.
     */
    private void writeEmotionModifiersToFile(String fileName, List<EmotionModifier> emotionModifiers) {
        try {
            Gson gson = new Gson();
            Writer writer = new FileWriter(fileName);

            gson.toJson(emotionModifiers, writer);
            writer.flush();
            writer.close();
        }
        catch (Exception e) {
            // ...
        }
    }

    /**
     * Called by DialogFlowBridge, this method checks an input query for registered phrases and changes emotions accordingly.
     * E.g. using many curse words will lower the bot's patience value.
     * @param input Input sentence to scan for registered phrases.
     */
    public void alterEmotions(String input) {
        List<String> words = Arrays.asList(input.split("\\s+"));
        for (String word: words) {
            // Iterate through every word from the input sentence (rather than iterating through modifiers.json)
            checkForRegisteredPhrase(word);
        }
    }

    /**
     * Check a word for a potential match in modifiers.json and alter relevant emotion accordingly.
     * @param word Input phrase.
     */
    private void checkForRegisteredPhrase(String word) {
        for (EmotionModifier modifier: modifiers) {
            if (word.toLowerCase().equals(modifier.getRelevantWord())) {
                Float prevValue = emotions.get(modifier.getRelevantEmotion());
                Float newValue = clamp((prevValue + (modifier.getModification() * globalModifyMultiplier)), -1.0f, 1.0f);
                emotions.put(modifier.getRelevantEmotion(), newValue);
                System.out.println("Detected phrase: " + modifier.getRelevantWord());
                System.out.println("Modified emotion \"" + modifier.getRelevantEmotion() + "\": " + prevValue + "->" + newValue);
            }
        }
    }

    /**
     * Increment emotion by specified amount. Can be negative.
     * @param emotion Emotion to modify.
     * @param amount Amount to add/substract.
     */
    public void incrementEmotion(String emotion, Float amount) {
        Float prevValue = emotions.get(emotion);
        Float newValue = clamp((prevValue + amount), -1.0f, 1.0f);
        emotions.put(emotion, newValue);
    }

    /**
     * Clamp a float value between min and max. Interesting that Java does not have this by default.
     * @param value Value to clamp.
     * @param min Minimum.
     * @param max Maximum.
     * @return
     */
    private Float clamp(Float value, Float min, Float max) {
        return Math.max(min, Math.min(max, value));
    }

    public Map<String, Float> getEmotions() {
        return emotions;
    }

    public Map<String, Float> getPersonality() {
        return personality;
    }

    public Float getGlobalModifyMultiplier() {
        return globalModifyMultiplier;
    }

    public void setGlobalModifyMultiplier(Float globalModifyMultiplier) {
        this.globalModifyMultiplier = globalModifyMultiplier;
    }

    /**
     * Temporary test method to set default personality extremes.
     */
    private void temp() {
        // Just sets a high default character trait for testing.
        // Using map.put() is fine since duplicates aren't allowed.
        personality.put("Desire", 1.0f);    // Set 'Desire' as leading trait.

        emotions.put("Patience", 1.0f);    // Extremely patient.
    }

}