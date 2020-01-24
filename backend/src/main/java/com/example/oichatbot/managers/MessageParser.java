package com.example.oichatbot.managers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse raw messages from DialogFlow to dynamically add/alter/remove blocks of text.
 * Singleton class.
 */
public class MessageParser {
    private static MessageParser instance = null;

    public MessageParser() {
    }

    public static MessageParser getInstance() {
        if (instance == null)
            instance = new MessageParser();

        return instance;
    }

    /**
     * Starting point for parsing messages. Checks for special tags, data to be formatted (e.g. timestamps), etc.
     * @param message Input message to be parsed and formatted.
     * @return Formatted (readable) output message.
     */
    public String parseMessage(String message) {
        String output = message;

        // First we format special tags/blocks to determine whether they need to be shown or hidden.
        // We repeat this step for every block we find (until none remain).
        while (output.contains("[")) {
            output = formatEmotionBlock(output);
        }

        // Now we check for other misc. text from DialogFlow that requires extra formatting, such as datetime.
        output = formatTime(output);

        // Finally return the fully formatted message.
        return output;
    }

    private String formatEmotionBlock(String message) {
        Integer startIndex = message.indexOf("[");
        Integer endIndex = message.indexOf("]");
        String emotionBlock = message.substring(startIndex + 1, endIndex);  // Without the [ ].
        String emotionBlock2 = message.substring(startIndex, endIndex + 1);  // With the [ ].
        System.out.println("Detected emotion block: " + emotionBlock);

        // Split the block up in its three individual segments.
        // 0: Emotion to check for, 1: Modifier (e.g. <0.5), 2: String to display.
        String[] segments = emotionBlock.split(";");
        if (checkModifier(segments[0], segments[1])) {
            // The text should be displayed.
            System.out.println("Modifier matches current emotion level, showing the block.");

            // Replace entire [ ] block with the to-be-displayed string.
            // Original string up to [ (exclusive), then segments[2], then original string after ] (exclusive).
            message = message.substring(0, startIndex) + segments[2] + message.substring(endIndex + 1);
            System.out.println(message);
        }
        else {
            // The text should not be displayed.
            System.out.println("Modifier does not match current emotion level, NOT showing the block.");
            // Clear entire [ ] block.
            // Original string up to [ (exclusive), then (empty), then original string after ] (exclusive).
            message = message.substring(0, startIndex) + "" + message.substring(endIndex + 1);
            System.out.println(message);
        }
        return message;
    }

    private String formatTime(String message) {
        // todo: format time from DialogFlow (e.g. to remove timezone and seconds).
        return message;
    }

    private Boolean checkModifier(String emotion, String modifier) {
        // Early check in case the given emotion name doesn't even exist.
        if (!PersonalityManager.getInstance().getEmotions().containsKey(emotion)) {
            // The given emotion name doesn't exist.
            System.out.println("Problem in MessageParser.checkModifier(): Emotion \"" + emotion + "\" doesn't exist.");
            return false;
        }

        String value = "undefined";     // Value to compare with. Stored as string since we need to find the index in the input string later (unformatted).
        Pattern p = Pattern.compile("[-+]?\\d*\\.\\d+|[-+]?\\d+");  // Catches every possible variant of a positive or negative decimal.
        Matcher m = p.matcher(modifier);
        // We could use a while loop but there shouldn't be more results than one.
        if (m.find()) {
            value = m.group();
            System.out.println("Value to compare with: " + value);
        }

        // Quick check to determine value has actually been set (and isn't still at its initial absurd value).
        if (value.equals("undefined")) {
            System.out.println("Problem in MessageParser.checkModifier(): No detected value to compare with.");
            return false;
        }

        // At this point we have the emotion and the value to compare with (as String).
        // Now just to determine the kind of check/operation and return true/false accordingly.
        String operation = modifier.substring(0, modifier.indexOf(value));
        System.out.println("Operation detected: " + operation);
        Float valueAsFloat = Float.valueOf(value);
        switch (operation) {
            case "==":
                // return (PersonalityManager.getInstance().getEmotions().get(emotion) == valueAsFloat);
                return (Math.abs(PersonalityManager.getInstance().getEmotions().get(emotion) - valueAsFloat) < 0.001);      // Comparing two floats with a small tolerance.

            case "<":
                return (PersonalityManager.getInstance().getEmotions().get(emotion) < valueAsFloat);

            case ">":
                return (PersonalityManager.getInstance().getEmotions().get(emotion) > valueAsFloat);

            case "<=":
                return (PersonalityManager.getInstance().getEmotions().get(emotion) <= valueAsFloat);

            case ">=":
                return (PersonalityManager.getInstance().getEmotions().get(emotion) >= valueAsFloat);

            default:
                // This shouldn't be reachable.
                System.out.println("Problem in MessageParser.checkModifier(): Default case in switch operation reached.");
                return false;
        }
    }
}
