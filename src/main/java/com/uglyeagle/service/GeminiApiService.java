package com.uglyeagle.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiApiService {

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${google.api.upload-url}")
    private String uploadUrl;

    @Value("${google.api.generate-url}")
    private String generateUrl;

    public JsonNode getResponse(MultipartFile file, String prompt) throws Exception {
        String fileUri = uploadFileToGoogle(file);
        return generateContent(fileUri, prompt);
    }

    private String uploadFileToGoogle(MultipartFile file) throws Exception {
        String requestUrl = uploadUrl + "?key=" + apiKey;

        HttpHeaders startHeaders = new HttpHeaders();
        startHeaders.set("X-Goog-Upload-Protocol", "resumable");
        startHeaders.set("X-Goog-Upload-Command", "start");
        startHeaders.set("X-Goog-Upload-Header-Content-Length", String.valueOf(file.getSize()));
        startHeaders.set("X-Goog-Upload-Header-Content-Type", file.getContentType());
        startHeaders.setContentType(MediaType.APPLICATION_JSON);

        String jsonBody = "{\"file\": {\"display_name\": \"" + file.getOriginalFilename() + "\"}}";

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> startRequest = new HttpEntity<>(jsonBody, startHeaders);
        ResponseEntity<String> startResponse = restTemplate.exchange(requestUrl, HttpMethod.POST, startRequest,
                String.class);

        String sessionUri = startResponse.getHeaders().getFirst("X-Goog-Upload-URL");
        if (sessionUri == null || sessionUri.isEmpty()) {
            throw new Exception("Failed to obtain upload session URI.");
        }

        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.set("X-Goog-Upload-Protocol", "resumable");
        uploadHeaders.set("X-Goog-Upload-Command", "upload, finalize");
        uploadHeaders.set("X-Goog-Upload-Offset", "0");
        uploadHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        HttpEntity<byte[]> uploadRequest = new HttpEntity<>(file.getBytes(), uploadHeaders);
        ResponseEntity<String> uploadResponse = restTemplate.exchange(sessionUri, HttpMethod.POST, uploadRequest,
                String.class);

        return new ObjectMapper().readTree(uploadResponse.getBody()).path("file").path("uri").asText();
    }

    public JsonNode generateContent(String fileUri, String prompt) throws Exception {
        String requestUrl = generateUrl + "?key=" + apiKey;

        String requestBody = "{ \"contents\": [ { \"role\": \"user\", \"parts\": [ { \"fileData\": { \"fileUri\": \""
                + fileUri + "\" } }, { \"text\": \"" + prompt + "\" } ] } ] }";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, entity, String.class);

        return new ObjectMapper().readTree(response.getBody());
    }
}
