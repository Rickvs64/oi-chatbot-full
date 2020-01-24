package com.example.oichatbot.resources;

import com.example.oichatbot.managers.DebugManager;
import com.example.oichatbot.managers.DialogFlowBridge;
import com.example.oichatbot.domains.Message;
import com.example.oichatbot.managers.ExceptionManager;
import com.example.oichatbot.managers.PersonalityManager;
import com.google.api.gax.paging.Page;
import com.google.cloud.dialogflow.v2.*;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.web.bind.annotation.*;

import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.io.*;
import java.net.MalformedURLException;


import java.io.File;
import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/test")
public class TestResource {

    private Integer incrementTest = 0;

    /**
     * Authentication test method for Google Cloud functionality.
     * Will throw exceptions if the Google Cloud project isn't properly linked.
     * @return The (useless) eventual object display name.
     */
    @RequestMapping("/auth")
    public String auth() {
        // If you don't specify credentials when constructing the client, the
        // client library will look for credentials in the environment.

        Storage storage = StorageOptions.getDefaultInstance().getService();


        Page<Bucket> buckets = storage.list();
        for (Bucket bucket : buckets.iterateAll()) {
            // Display iterated buckets?
            // No actions necessary for now.
        }
        return buckets.toString();
    }

    /**
     * Simple increment request method to confirm persistent variable values throughout different requests.
     * @return The same value as last time, plus one.
     */
    @RequestMapping("/increment")
    public Integer increment() {
        incrementTest ++;
        return incrementTest;
    }

    /**
     * Determine the one best reply for any given singular chat message.
     * @param message Message to respond to.
     * @return Message object containing the text response.
     * @throws Exception
     */
    @PostMapping(path = "/chat/post", consumes = "application/json", produces = "application/json")
    public Message chatSimple(@RequestBody Message message) throws Exception {
        // First check whether we're (already) in DEBUG, which means the back-end directly handles this request without DialogFlow.
        if (DebugManager.getInstance().inDebug()) {
            if (DebugManager.getInstance().wantsToExitDebug(message.getContent())) {
                // Exit DEBUG mode.
                String content = DebugManager.getInstance().exitDebug();
                Message output = new Message(content, true, PersonalityManager.getInstance().determineSuggestedColor());
                System.out.println("Exiting DEBUG mode.");
                return output;
            }
            else {
                // Detect DEBUG intent and do stuff.
                String content = DebugManager.getInstance().parseCommand(message.getContent());
                Message output = new Message(content, true, DebugManager.getInstance().getDebugColor());
                System.out.println("Attempted to recognize as debug command: " + message.getContent());
                System.out.println("Debug mode output: " + content);
                return output;
            }
        }
        else if (DebugManager.getInstance().wantsToEnterDebug(message.getContent())) {
            // Enter DEBUG mode.
            String content = DebugManager.getInstance().enterDebug();
            Message output = new Message(content, true, DebugManager.getInstance().getDebugColor());
            System.out.println("Entering DEBUG mode.");
            return output;
        }
        else {
            // User is not currently in DEBUG and doesn't want to be, so we send their message to DialogFlow.
            // UNLESS we determine it's time for an extreme response/exception.
            if (ExceptionManager.getInstance().shouldRespondExtreme()) {
                return ExceptionManager.getInstance().chatExtreme(message.getContent());
            }
            else {
                return DialogFlowBridge.getInstance().chat(message.getContent(), "en-US");
            }
        }
    }

    /**
     * Display a list of possible intents recognized by the DialogFlow API.
     * @return List of recognized intents and possible responses.
     * @throws Exception
     */
    @RequestMapping("/list")
    public String listIntents() throws Exception {
        return listIntents("openinno").toString();
    }


    /**
     * Retrieve a list of all possible recognized intents (commands) via the DialogFlow API.
     * @param projectId Project ID, default is "openinno".
     * @return List of possible intents the API can respond to.
     * @throws Exception
     */
    private static List<Intent> listIntents(String projectId) throws Exception {
        return DialogFlowBridge.getInstance().listIntents(projectId);
    }

    /**
     * Test method to download mp3 file.
     * @return
     */
    @RequestMapping("/luuk")
    public ResponseEntity<Resource> serveFile (){

        File file = new File("output.mp3");
        Resource resource = null;
        try {
            resource = new UrlResource(file.toURI());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"").body(resource);

    }

}
