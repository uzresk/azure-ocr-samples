package jp.gr.java_conf.uzresk.azure.ocr.samples;

public class OcrFromRemoteSample {

    public static void main(String[] args) {
        String remoteTextImageURL = "https://raw.githubusercontent.com/Azure-Samples/cognitive-services-sample-data-files/master/ComputerVision/Images/printed_text.jpg";

        ComputerVisionUtils.readFromRemote(remoteTextImageURL);
    }
}
