package com.esprit.services;

import com.clarifai.channel.ClarifaiChannel;
import com.clarifai.credentials.ClarifaiCallCredentials;
import com.clarifai.grpc.api.*;
import com.clarifai.grpc.api.status.StatusCode;
import com.google.protobuf.ByteString;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class ClarifaiImageValidationService {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("CLARIFAI_API_KEY");
    private static final String USER_ID = dotenv.get("CLARIFAI_USER_ID");
    private static final String APP_ID = dotenv.get("CLARIFAI_APP_ID");
    private static final String MODEL_ID = dotenv.get("CLARIFAI_MODEL_ID");
    private static final String MODEL_VERSION_ID = dotenv.get("CLARIFAI_MODEL_VERSION_ID");


    // List of allowed healthcare-related labels
    private static final List<String> HEALTHCARE_LABELS = Arrays.asList(
            "wheelchair", "crutches", "walker", "medical equipment", "hospital bed",
            "blood pressure monitor", "thermometer", "stethoscope", "medical device",
            "oxygen tank", "medical supplies", "bandage", "syringe", "pill", "medication",
            "healthcare", "hospital", "pharmacy", "doctor", "nurse", "patient", "clinic",
            "medical", "health", "medicine", "care", "treatment", "therapy", "aid",
            "mobility aid", "glucose meter", "inhaler", "nebulizer", "medical machine",
            "tensiometer", "blood sugar meter"
    );

    // Threshold for considering an image as healthcare-related
    private static final float HEALTHCARE_THRESHOLD = 0.60f;

    private final V2Grpc.V2BlockingStub stub;

    public ClarifaiImageValidationService() {
        stub = V2Grpc.newBlockingStub(ClarifaiChannel.INSTANCE.getGrpcChannel())
                .withCallCredentials(new ClarifaiCallCredentials(API_KEY));
    }

    /**
     * Validates if the image is healthcare-related
     * @param imageFile File object containing the image
     * @return true if the image is healthcare-related, false otherwise
     */
    public boolean isHealthcareRelated(File imageFile) throws IOException {
        if (imageFile == null) {
            return true; // If no image is provided, we don't need to validate
        }

        // Read the image file
        byte[] fileBytes = Files.readAllBytes(imageFile.toPath());
        return isHealthcareRelated(fileBytes);
    }

    /**
     * Validates if the image bytes are healthcare-related
     * @param imageBytes Byte array of image data
     * @return true if the image is healthcare-related, false otherwise
     */
    public boolean isHealthcareRelated(byte[] imageBytes) {
        try {
            // Create the request
            MultiOutputResponse response = stub.postModelOutputs(
                    PostModelOutputsRequest.newBuilder()
                            .setUserAppId(UserAppIDSet.newBuilder().setUserId(USER_ID).setAppId(APP_ID))
                            .setModelId(MODEL_ID)
                            .setVersionId(MODEL_VERSION_ID)
                            .addInputs(
                                    Input.newBuilder().setData(
                                            Data.newBuilder().setImage(
                                                    Image.newBuilder().setBase64(ByteString.copyFrom(imageBytes))
                                            )
                                    )
                            )
                            .build()
            );

            // Check for errors
            if (response.getStatus().getCode() != StatusCode.SUCCESS) {
                System.err.println("Error from Clarifai API: " + response.getStatus().getDescription());
                return false;
            }

            // Process predictions
            for (Output output : response.getOutputsList()) {
                for (Concept concept : output.getData().getConceptsList()) {
                    String name = concept.getName().toLowerCase();
                    float value = concept.getValue();

                    System.out.println("Concept: " + name + ", Value: " + value);

                    // Check if concept matches healthcare categories with good confidence
                    for (String healthcareLabel : HEALTHCARE_LABELS) {
                        if (name.contains(healthcareLabel) || healthcareLabel.contains(name)) {
                            if (value >= HEALTHCARE_THRESHOLD) {
                                System.out.println("Healthcare image detected: " + name + " with confidence " + value);
                                return true;
                            }
                        }
                    }
                }
            }

            // No healthcare-related concepts with sufficient confidence found
            return false;
        } catch (Exception e) {
            System.err.println("Error validating image: " + e.getMessage());
            e.printStackTrace();
            return false; // In case of errors, we might want to be lenient and allow the image
        }
    }
}