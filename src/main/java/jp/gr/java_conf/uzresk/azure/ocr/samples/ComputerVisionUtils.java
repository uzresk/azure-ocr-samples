package jp.gr.java_conf.uzresk.azure.ocr.samples;

import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVision;
import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVisionClient;
import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVisionManager;
import com.microsoft.azure.cognitiveservices.vision.computervision.implementation.ComputerVisionImpl;
import com.microsoft.azure.cognitiveservices.vision.computervision.models.*;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

public class ComputerVisionUtils {

    static String SUBSCRIPTION_KEY = System.getenv("SUBSCRIPTION_KEY");
    static String ENDPOINT = System.getenv("ENDPOINT");

    private static ComputerVisionClient Authenticate() {
        // Create an authenticated Computer Vision client.
        return ComputerVisionManager.authenticate(SUBSCRIPTION_KEY).withEndpoint(ENDPOINT);
    }


    public static void readFromRemote(String url) {

        ComputerVisionClient client = ComputerVisionUtils.Authenticate();
        try {
            // Cast Computer Vision to its implementation to expose the required methods
            ComputerVisionImpl vision = (ComputerVisionImpl) client.computerVision();

            ReadHeaders responseHeader = vision.readWithServiceResponseAsync(url, null)
                    .toBlocking()
                    .single()
                    .headers();

            // Extract the operation Id from the operationLocation header
            String operationLocation = responseHeader.operationLocation();
            System.out.println("Operation Location:" + operationLocation);

            ComputerVisionUtils.getAndPrintReadResult(vision, operationLocation);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


    public static void readFromLocal(String localFilePath) {

        ComputerVisionClient client = ComputerVisionUtils.Authenticate();
        try {
            // Cast Computer Vision to its implementation to expose the required methods
            ComputerVisionImpl vision = (ComputerVisionImpl) client.computerVision();

            File file = new File(localFilePath);
            byte[] localImageBytes = Files.readAllBytes(file.toPath());
            ReadInStreamHeaders responseHeader =
                    vision.readInStreamWithServiceResponseAsync(localImageBytes, null)
                            .toBlocking()
                            .single()
                            .headers();

            // Extract the operation Id from the operationLocation header
            String operationLocation = responseHeader.operationLocation();
            System.out.println("Operation Location:" + operationLocation);

            getAndPrintReadResult(vision, operationLocation);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    static String extractOperationIdFromOpLocation(String operationLocation) {
        if (operationLocation != null && !operationLocation.isEmpty()) {
            String[] splits = operationLocation.split("/");

            if (splits != null && splits.length > 0) {
                return splits[splits.length - 1];
            }
        }
        throw new IllegalStateException("Something went wrong: Couldn't extract the operation id from the operation location");
    }

    static void getAndPrintReadResult(ComputerVision vision, String operationLocation) throws InterruptedException {
        System.out.println("Polling for Read results ...");

        // Extract OperationId from Operation Location
        String operationId = extractOperationIdFromOpLocation(operationLocation);

        boolean pollForResult = true;
        ReadOperationResult readResults = null;

        while (pollForResult) {
            // Poll for result every second
            Thread.sleep(1000);
            readResults = vision.getReadResult(UUID.fromString(operationId));

            // The results will no longer be null when the service has finished processing the request.
            if (readResults != null) {
                // Get request status
                OperationStatusCodes status = readResults.status();

                if (status == OperationStatusCodes.FAILED || status == OperationStatusCodes.SUCCEEDED) {
                    pollForResult = false;
                }
            }
        }

        // Print read results, page per page
        for (ReadResult pageResult : readResults.analyzeResult().readResults()) {
            System.out.println("");
            System.out.println("Printing Read results for page " + pageResult.page());
            StringBuilder builder = new StringBuilder();

            for (Line line : pageResult.lines()) {
                // 単語ごとにconfidenceを出力
                line.words().stream().forEach(w -> System.out.println(w.text() + "\tConfidence[" + w.confidence() + "] BoundingBox" + w.boundingBox()));
                // 一行ごとにtextを抽出
                builder.append(line.text());
                builder.append("\n");
            }

            System.out.println(builder.toString());
        }
    }
}
