package com.example.oichatbot.domains;

import java.io.Serializable;

/**
 * Holds information about a specific term or phrase and how it affects the bot's emotions.
 * For example, using the word "please" may increase its "patience" emotion by a small amount.
 */
public class EmotionModifier implements Serializable {
    private String relevantEmotion;
    private String relevantWord;
    private Float modification;

    public EmotionModifier() {
    }

    public EmotionModifier(String relevantEmotion, String relevantWord, Float modification) {
        this.relevantEmotion = relevantEmotion;
        this.relevantWord = relevantWord;
        this.modification = modification;
    }

    public String getRelevantEmotion() {
        return relevantEmotion;
    }

    public void setRelevantEmotion(String relevantEmotion) {
        this.relevantEmotion = relevantEmotion;
    }

    public String getRelevantWord() {
        return relevantWord;
    }

    public void setRelevantWord(String relevantWord) {
        this.relevantWord = relevantWord;
    }

    public Float getModification() {
        return modification;
    }

    public void setModification(Float modification) {
        this.modification = modification;
    }
}
