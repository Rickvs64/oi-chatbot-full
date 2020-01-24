package com.example.oichatbot.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds debug information and methods (such as directly changing personality traits).
 * Singleton class.
 */
public class DebugManager {
    private Boolean inDebug = false;
    private List<String> entryPhrases;      // Possible phrases to be used by the user.
    private List<String> exitPhrases;       // Possible phrases to be used by the user.

    private List<String> introMessages;     // Possible messages to be used by the bot.
    private List<String> outroMessages;     // Possible messages to be used by the bot.

    private List<String> checkValueKeywords;    // Possible (first) words to use in a checkValue() request.
    private List<String> setValueKeywords;    // Possible (first) words to use in a setValue() request.
    private List<String> playMuteKeywords;      // Possible (first) words to use in a playMute() request.

    private String debugColor = "#DFDFDF";  // Default background color for debug messages in the front-end.

    private static DebugManager instance = null;

    public DebugManager() {
        initEntryPhrases();
        initExitPhrases();
        initIntroMessages();
        initOutroMessages();
        initCheckValueKeywords();
        initSetValueKeywords();
        initPlayMuteKeywords();
    }

    // Static method to maintain one persistent instance.
    public static DebugManager getInstance()
    {
        if (instance == null)
            instance = new DebugManager();

        return instance;
    }

    public Boolean wantsToEnterDebug(String input) {
        String parsed = input.trim();
        parsed = parsed.replaceAll("/^[A-Za-z]+$/", "");    // Letters and whitespaces.
        parsed = parsed.toLowerCase();
        return (entryPhrases.contains(parsed));
    }

    public Boolean wantsToExitDebug(String input) {
        String parsed = input.trim();
        parsed = parsed.replaceAll("/^[A-Za-z]+$/", "");    // Only letters and whitespaces.
        parsed = parsed.toLowerCase();
        return (exitPhrases.contains(parsed));
    }

    public String enterDebug() {
        inDebug = true;
        return getEntryMessage();
    }

    public String exitDebug() {
        inDebug = false;
        return getOutroMessage();
    }

    /**
     * Fill entryPhrases array with possible recognized commands.
     */
    private void initEntryPhrases() {
        entryPhrases = new ArrayList<>();
        entryPhrases.add("analysis");
        entryPhrases.add("enter analysis");
        entryPhrases.add("analysis mode");
        entryPhrases.add("enter analysis mode");
        entryPhrases.add("debug");
        entryPhrases.add("enter debug");
        entryPhrases.add("debug mode");
        entryPhrases.add("enter debug mode");
    }

    /**
     * Fill exitPhrases array with possible recognized commands.
     */
    private void initExitPhrases() {
        exitPhrases = new ArrayList<>();
        exitPhrases.add("exit");
        exitPhrases.add("exit analysis");
        exitPhrases.add("exit analysis mode");
        exitPhrases.add("exit debug");
        exitPhrases.add("exit debug mode");
        exitPhrases.add("normal mode");
        exitPhrases.add("resume");
        exitPhrases.add("back to normal");
        exitPhrases.add("go back to normal");
    }

    private void initIntroMessages() {
        introMessages = new ArrayList<>();
        introMessages.add("(DEBUG): Entering debug mode.");
        introMessages.add("(DEBUG): Entering analysis mode.");
        introMessages.add("(DEBUG): Beep boop. Analysis mode now enabled.");
        introMessages.add("(DEBUG): Analysis mode enabled.");
        introMessages.add("(DEBUG): Switched to debug mode.");
        introMessages.add("(DEBUG): Switched to analysis mode.");
    }

    private void initOutroMessages() {
        outroMessages = new ArrayList<>();
        outroMessages.add("Right! Where were we?");
        outroMessages.add("Huh? What just happened?");
        outroMessages.add("That was weird.");
        outroMessages.add("Get out of my head, you weirdo.");
        outroMessages.add("Huh? I feel different somehow.");
        outroMessages.add("Back to normal, then.");
        outroMessages.add("Alright then.");
    }

    private String getEntryMessage() {
        int randomIndex = new Random().nextInt(introMessages.size());
        return introMessages.get(randomIndex);
    }

    private String getOutroMessage() {
        int randomIndex = new Random().nextInt(outroMessages.size());
        return outroMessages.get(randomIndex);
    }

    /**
     * Detect possible debug command and execute it accordingly - EXCLUDING enter/exit debug mode commands.
     * @param input Command to try and execute.
     * @return Response from bot (usually a confirmation message).
     */
    public String parseCommand(String input) {
        // Could be a "checkValue" command.
        if (recognizeCommandAsCheckValue(input.trim().toLowerCase())) {
            return checkValue(input.trim().toLowerCase());
        }
        // Could be a "setValue" command.
        if (recognizeCommandAsSetValue(input.trim().toLowerCase())) {
            return setValue(input.trim().toLowerCase());
        }
        // Could be a "play/mute" command.
        if (recognizeCommandAsPlayMute(input.trim().toLowerCase())) {
            return playMute();
        }

        // No recognized command.
        return "(DEBUG): Warning! No known command recognized.";
    }


    public Boolean inDebug() {
        return inDebug;
    }

    public String getDebugColor() {
        return debugColor;
    }

    private Boolean recognizeCommandAsCheckValue(String input) {
        if (!input.contains(" ")) {
            // Doesn't even contain a single whitespace - can't possibly be a valid checkValue() request.
            return false;
        }

        int i = input.indexOf(" ");     // First whitespace.
        String firstWord = input.substring(0, i);   // First word.

        return (checkValueKeywords.contains(firstWord));
    }

    private void initCheckValueKeywords() {
        checkValueKeywords = new ArrayList<>();
        checkValueKeywords.add("how");
        checkValueKeywords.add("what");
        checkValueKeywords.add("what's");
        checkValueKeywords.add("get");
    }

    /**
     * Check the value of one specified character trait or emotion.
     * @param input Full input sentence, (hopefully) containing a trait or emotion.
     * @return Output message displaying the appropriate value or warning that no matching emotion/trait was found.
     */
    private String checkValue(String input) {
        String parsed = input.replaceAll("/^[A-Za-z]+$/", "");    // Only letters and whitespaces.

        // Right now 'parsed' is one raw sentence.
        // We will split it up into words and iterate through every single one until we detect a valid emotion/trait.
        String[] words = parsed.split(" ");
        String detectedMatch = "";  // Detected match, either a personality trait or an emotion.
        Integer matchingMap = -1;   // 0 if it's a personality trait, 1 if it's an emotion.
        Float value = 0.0f;
        for (String w: words) {
            // Make the first letter of the word uppercase, the rest stays lowercase.
            String parsedWord = w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase();
            if (PersonalityManager.getInstance().getPersonality().containsKey(parsedWord)) {
                detectedMatch = parsedWord;
                matchingMap = 0;
                value = PersonalityManager.getInstance().getPersonality().get(parsedWord);
                break;
            }
            else if (PersonalityManager.getInstance().getEmotions().containsKey(parsedWord)) {
                detectedMatch = parsedWord;
                matchingMap = 1;
                value = PersonalityManager.getInstance().getEmotions().get(parsedWord);
                break;
            }
        }

        // We've iterated through every word (or until we found a match).
        // If there is a match, we know which map it belongs to in PersonalityManager and its value.
        String response = "";
        switch (matchingMap) {
            case 0:
                response = "(DEBUG): Personality trait \"" + detectedMatch + "\" is currently set to: " + value.toString() + ".";
                break;

            case 1:
                response = "(DEBUG): Emotion \"" + detectedMatch + "\" is currently set to: " + value.toString() + ".";
                break;

            default:
                response = "(DEBUG): WARNING! Attempted to parse command as \"checkValue()\", but no matching trait or emotion was found.";
        }

        return response;
    }

    private Boolean recognizeCommandAsSetValue(String input) {
        if (!input.contains(" ")) {
            // Doesn't even contain a single whitespace - can't possibly be a valid setValue() request.
            return false;
        }

        int i = input.indexOf(" ");     // First whitespace.
        String firstWord = input.substring(0, i);   // First word.

        return (setValueKeywords.contains(firstWord));
    }

    private void initSetValueKeywords() {
        setValueKeywords = new ArrayList<>();
        setValueKeywords.add("set");
        setValueKeywords.add("change");
    }

    private String setValue(String input) {
        // We will split it up into words and iterate through every single one until we detect a valid emotion/trait.
        String[] words = input.split(" ");
        String detectedMatch = "";  // Detected match, either a personality trait or an emotion.
        Integer matchingMap = -1;   // 0 if it's a personality trait, 1 if it's an emotion.
        for (String w: words) {
            // Make the first letter of the word uppercase, the rest stays lowercase.
            String parsedWord = w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase();
            if (PersonalityManager.getInstance().getPersonality().containsKey(parsedWord)) {
                detectedMatch = parsedWord;
                matchingMap = 0;
                break;
            }
            else if (PersonalityManager.getInstance().getEmotions().containsKey(parsedWord)) {
                detectedMatch = parsedWord;
                matchingMap = 1;
                break;
            }
        }

        String response = "";

        // If we've not found a single matching emotion/trait, cancel this method early.
        if (matchingMap == -1) {
            response = "(DEBUG): WARNING! Attempted to parse command as \"setValue()\" but no matching trait or emotion was found.";
            return response;
        }

        // If we've successfully detected an emotion/trait, we now need to determine what value to set it to.
        Float value = -10.0f;
        // Pattern p = Pattern.compile("^-?\\d*\\.\\d+|\\d+");
        Pattern p = Pattern.compile("[-+]?\\d*\\.\\d+|[-+]?\\d+");  // Catches every possible variant of a positive or negative decimal.
        Matcher m = p.matcher(input);
        // We could use a while loop but there shouldn't be more results than one.
        if (m.find()) {
            value = Float.valueOf(m.group());
            System.out.println("Detected requested new value: " + value);
        }

        // If the float is still at its absurd initial value we know we failed to find an acceptable float value from the input string.
        // Cancel method early.
        if (value == -10.0f) {
            response = "(DEBUG): WARNING! Attempted to parse command as \"setValue()\". Emotion/trait \"" + detectedMatch + "\" was found but no valid float value could be detected in your command.";
            return response;
        }
        switch (matchingMap) {
            case 0:
                PersonalityManager.getInstance().getPersonality().put(detectedMatch, value);
                response = "(DEBUG): Personality trait altered: " + detectedMatch + " has been set to " + value.toString() + ".";
                break;

            case 1:
                PersonalityManager.getInstance().getEmotions().put(detectedMatch, value);
                response = "(DEBUG): Emotion altered: " + detectedMatch + " has been set to " + value.toString() + ".";
                break;

            default:
                response = "(DEBUG): WARNING! Unexpected default case occurred in DebugManager.setValue().";
                break;
        }
        return response;
    }

    private void initPlayMuteKeywords() {
        playMuteKeywords = new ArrayList<>();
        playMuteKeywords.add("mute");
        playMuteKeywords.add("unmute");
        playMuteKeywords.add("play");
        playMuteKeywords.add("toggle");
    }

    private boolean recognizeCommandAsPlayMute(String input) {
        int i = input.indexOf(" ");     // First whitespace.
        String firstWord = input.substring(0, i);   // First word.

        return (playMuteKeywords.contains(firstWord));
    }

    /**
     * Toggle audio output from SpeechManager.
     * Prevents unnecessary cost calculation from Google Cloud.
     */
    private String playMute() {
        if (SpeechManager.getInstance().toggleAudio()) {
            // Audio should now play.
            return "(DEBUG): Audio output has been enabled.";
        }
        else {
            // Audio should be muted.
            return "(DEBUG): Audio output has been disabled.";
        }
    }
}
