package com.uglyeagle.controller;

import java.util.HashMap;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.uglyeagle.service.GeminiApiService;

@RestController
@RequestMapping("/gemini")
public class GeminiApiController {

    private static final Logger logger = Logger.getLogger(GeminiApiController.class.getName());

    @Autowired
    private GeminiApiService geminiApiService;

    @PostMapping("/process-image")
    public ResponseEntity<HashMap<String, Object>> processImage(@RequestParam("file") MultipartFile file,
                                                                @RequestParam("prompt") String prompt) {

        logger.info("\n\nINSIDE CLASS == GeminiApiController, METHOD == processImage(); ");

        try {
            JsonNode result = geminiApiService.getResponse(file, prompt);

            if (result != null) {
                logger.info("\nImage processed successfully.");
                logger.info("\nEXITING METHOD == processImage() OF CLASS == GeminiApiController \n\n");
                return getResponseFormat(HttpStatus.OK, "Image processed successfully", result);
            } else {
                logger.info("\nImage processing failed.");
                logger.info("\nEXITING METHOD == processImage() OF CLASS == GeminiApiController \n\n");
                return getResponseFormat(HttpStatus.INTERNAL_SERVER_ERROR, "Image processing failed", null);
            }
        } catch (Exception e) {
            logger.severe("\nError in processImage() method of GeminiApiController: " + e.getMessage());
            logger.info("\nEXITING METHOD == processImage() OF CLASS == GeminiApiController \n\n");
            return getResponseFormat(HttpStatus.INTERNAL_SERVER_ERROR, "Critical Error: " + e.getLocalizedMessage(),
                    null);
        }
    }

    public ResponseEntity<HashMap<String, Object>> getResponseFormat(HttpStatus status, String message, Object data) {
        int responseStatus = (status.equals(HttpStatus.OK)) ? 1 : 0;

        HashMap<String, Object> map = new HashMap<>();
        map.put("responseCode", responseStatus);
        map.put("message", message);
        map.put("data", data);
        return ResponseEntity.status(status).body(map);
    }
}
