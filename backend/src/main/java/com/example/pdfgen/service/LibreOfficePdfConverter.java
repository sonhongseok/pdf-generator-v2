// backend/src/main/java/com/example/pdfgen/service/LibreOfficePdfConverter.java
package com.example.pdfgen.service;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class LibreOfficePdfConverter {

    // application.yml에서 주입받을 수 있도록 처리. 기본값은 Windows의 전형적인 경로
    @Value("${libreoffice.path:C:/Program Files/LibreOffice/program/soffice.exe}")
    private String sofficePath;

    /**
     * 하나의 docx 바이트 배열을 PDF 바이트 배열로 변환합니다.
     */
    public byte[] convertToPdf(byte[] docxContent) throws Exception {
        // 1. 임시 디렉토리 및 파일 생성
        Path tempDir = Files.createTempDirectory("pdfgen");
        String uuid = UUID.randomUUID().toString();
        Path docxPath = tempDir.resolve(uuid + ".docx");
        Path pdfPath = tempDir.resolve(uuid + ".pdf");

        try {
            // 2. docx 파일 쓰기
            Files.write(docxPath, docxContent);

            // 3. LibreOffice headless 명령어 실행
            // soffice --headless --convert-to pdf --outdir <출력디렉토리> <입력파일>
            ProcessBuilder pb = new ProcessBuilder(
                    sofficePath,
                    "--headless",
                    "--convert-to", "pdf",
                    "--outdir", tempDir.toString(),
                    docxPath.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 출력을 읽어주어야 프로세스가 블록되지 않음
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[LibreOffice] " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("LibreOffice 변환 실패. 종료 코드: " + exitCode);
            }

            if (!Files.exists(pdfPath)) {
                throw new RuntimeException("LibreOffice가 PDF 파일을 생성하지 못했습니다: " + pdfPath);
            }

            // 4. 생성된 PDF 읽기
            return Files.readAllBytes(pdfPath);
        } finally {
            // 5. 임시 파일 삭제 정리
            Files.deleteIfExists(docxPath);
            Files.deleteIfExists(pdfPath);
            Files.deleteIfExists(tempDir);
        }
    }

    /**
     * 여러 개의 PDF 바이트 배열을 하나의 PDF로 병합합니다. (Apache PDFBox 사용)
     */
    public byte[] mergePdfs(List<byte[]> pdfList) throws Exception {
        if (pdfList == null || pdfList.isEmpty()) {
            throw new IllegalArgumentException("병합할 PDF가 없습니다.");
        }
        if (pdfList.size() == 1) {
            return pdfList.get(0);
        }

        PDFMergerUtility mergerUtility = new PDFMergerUtility();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mergerUtility.setDestinationStream(out);

        for (byte[] pdfBytes : pdfList) {
            mergerUtility.addSource(new ByteArrayInputStream(pdfBytes));
        }

        mergerUtility.mergeDocuments(org.apache.pdfbox.io.MemoryUsageSetting.setupMainMemoryOnly());
        return out.toByteArray();
    }
}
