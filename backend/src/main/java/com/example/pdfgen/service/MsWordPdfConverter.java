// backend/src/main/java/com/example/pdfgen/service/LibreOfficePdfConverter.java
package com.example.pdfgen.service;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MsWordPdfConverter {

    // documents4j 변환 타임아웃 60초 설정
    private static final long CONVERSION_TIMEOUT_SECONDS = 60L;

    /**
     * 하나의 docx 바이트 배열을 PDF 바이트 배열로 변환.
     * documents4j 라이브러리를 통해 MS Word COM 자동화를 사용.
     * (PC에 Microsoft Word가 설치되어 있어야 합니다.)
     */
    public byte[] convertToPdf(byte[] docxContent) throws Exception {
        // 1. 임시 디렉토리 및 파일 경로 생성
        Path tempDir = Files.createTempDirectory("pdfgen");
        String uuid = UUID.randomUUID().toString();
        Path docxPath = tempDir.resolve(uuid + ".docx");
        Path pdfPath = tempDir.resolve(uuid + ".pdf");

        try {
            // 2. docx 바이트를 임시 파일로 저장
            Files.write(docxPath, docxContent);

            // 3. 커스텀 VBScript를 호출하여 PDF 변환 (documents4j 버그 우회)
            File vbsScript = new File("docx2pdf.vbs");
            if (!vbsScript.exists()) {
                throw new RuntimeException("변환 스크립트를 찾을 수 없습니다: " + vbsScript.getAbsolutePath());
            }

            ProcessBuilder pb = new ProcessBuilder(
                    "cscript.exe",
                    "//nologo",
                    vbsScript.getAbsolutePath(),
                    docxPath.toAbsolutePath().toString(),
                    pdfPath.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 최대 60초 대기
            boolean finished = process.waitFor(CONVERSION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("MS Word PDF 변환 시간이 초과되었습니다.");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                // 스크립트 에러 내용 읽기
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder errorMsg = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    errorMsg.append(line).append(" ");
                }
                throw new RuntimeException("MS Word 변환 스크립트 에러 (코드 " + exitCode + "): " + errorMsg.toString());
            }

            if (!Files.exists(pdfPath) || Files.size(pdfPath) == 0) {
                throw new RuntimeException("변환된 PDF 파일이 생성되지 않았습니다.");
            }

            // 4. 생성된 PDF 바이트 배열로 읽어 반환
            return Files.readAllBytes(pdfPath);

        } finally {
            // 5. 임시 파일 정리
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
