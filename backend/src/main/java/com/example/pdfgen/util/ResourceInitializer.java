// backend/src/main/java/com/example/pdfgen/util/ResourceInitializer.java
package com.example.pdfgen.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ResourceInitializer {

    private static final String FONT_URL = "https://github.com/google/fonts/raw/main/ofl/nanumgothic/NanumGothic-Regular.ttf";
    private static final String FONT_PATH = "src/main/resources/fonts/NanumGothic.ttf";

    private static final String SOURCE_IMAGE_PATH = "C:\\Users\\seoky\\.gemini\\antigravity-ide\\brain\\73299ab6-a665-43a8-84e5-ffafe4d21c8a\\media__1779321515585.png";
    private static final String LOGO_PATH = "src/main/resources/images/logo.png";
    private static final String SIGNATURE_PATH = "src/main/resources/images/signature.png";
    private static final String STAMP_PATH = "src/main/resources/images/stamp.png";

    private static final int LOGO_X = 35;
    private static final int LOGO_Y = 22;
    private static final int LOGO_WIDTH = 230;
    private static final int LOGO_HEIGHT = 70;

    private static final int SIGNATURE_X = 470;
    private static final int SIGNATURE_Y = 625;
    private static final int SIGNATURE_WIDTH = 110;
    private static final int SIGNATURE_HEIGHT = 40;

    private static final int STAMP_X = 395;
    private static final int STAMP_Y = 700;
    private static final int STAMP_WIDTH = 185;
    private static final int STAMP_HEIGHT = 75;

    public static void initializeResources() {
        try {
            createDirectories();
            downloadFontIfNeeded();
            cropImagesIfNeeded();
        } catch (Exception exception) {
            System.err.println("[ResourceInitializer Error] 리소스 초기화에 실패하였습니다: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    private static void createDirectories() throws IOException {
        Files.createDirectories(Paths.get("src/main/resources/fonts"));
        Files.createDirectories(Paths.get("src/main/resources/images"));
    }

    private static void downloadFontIfNeeded() {
        File fontFile = new File(FONT_PATH);
        if (fontFile.exists() && fontFile.length() > 0) {
            return;
        }

        System.out.println("[ResourceInitializer] 한글 나눔고딕 폰트 다운로드 시작...");
        try (BufferedInputStream inputStream = new BufferedInputStream(new URL(FONT_URL).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(FONT_PATH)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
            System.out.println("[ResourceInitializer] 한글 나눔고딕 폰트 다운로드 완료!");
        } catch (IOException ioException) {
            System.err.println("[ResourceInitializer Error] 폰트 다운로드 중 오류 발생: " + ioException.getMessage());
        }
    }

    private static void cropImagesIfNeeded() {
        File logoFile = new File(LOGO_PATH);
        File signatureFile = new File(SIGNATURE_PATH);
        File stampFile = new File(STAMP_PATH);

        if (logoFile.exists() && signatureFile.exists() && stampFile.exists()) {
            return;
        }

        File sourceFile = new File(SOURCE_IMAGE_PATH);
        if (!sourceFile.exists()) {
            System.err.println("[ResourceInitializer Error] 원본 이미지 파일이 존재하지 않습니다: " + SOURCE_IMAGE_PATH);
            return;
        }

        System.out.println("[ResourceInitializer] 원본 이미지에서 리소스 크롭 시작...");
        try {
            BufferedImage originalImage = ImageIO.read(sourceFile);

            BufferedImage logoImage = originalImage.getSubimage(LOGO_X, LOGO_Y, LOGO_WIDTH, LOGO_HEIGHT);
            ImageIO.write(logoImage, "png", new File(LOGO_PATH));

            BufferedImage signatureImage = originalImage.getSubimage(SIGNATURE_X, SIGNATURE_Y, SIGNATURE_WIDTH, SIGNATURE_HEIGHT);
            ImageIO.write(signatureImage, "png", new File(SIGNATURE_PATH));

            BufferedImage stampImage = originalImage.getSubimage(STAMP_X, STAMP_Y, STAMP_WIDTH, STAMP_HEIGHT);
            ImageIO.write(stampImage, "png", new File(STAMP_PATH));

            System.out.println("[ResourceInitializer] 리소스 이미지 크롭 및 저장 완료!");
        } catch (Exception exception) {
            System.err.println("[ResourceInitializer Error] 이미지 크롭 중 오류 발생: " + exception.getMessage());
        }
    }
}
