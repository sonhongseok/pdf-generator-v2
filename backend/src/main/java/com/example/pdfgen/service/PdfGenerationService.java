// backend/src/main/java/com/example/pdfgen/service/PdfGenerationService.java
package com.example.pdfgen.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class PdfGenerationService {

    /**
     * 입력받은 데이터를 기반으로 다페이지 임시 PDF 바이너리를 실시간 생성합니다.
     * 3단계에서 폰트 임베딩 및 고정 디자인 템플릿 렌더링이 여기에 세부 적용됩니다.
     * 
     * @param certificateNo 생성된 최종 인증서 번호
     * @param certificateDate 발행일
     * @param expiryDate 만료일
     * @param serialNos 시리얼 번호 목록 (각 시리얼당 1페이지씩 생성)
     * @return PDF 파일 바이트 배열
     * @throws DocumentException PDF 빌드 도중 발생하는 에러
     */
    public byte[] generateCertificatePdf(String certificateNo, String certificateDate, String expiryDate, List<String> serialNos) throws DocumentException {
        Document document = new Document();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        
        // OpenPDF Writer 인스턴스 획득
        PdfWriter.getInstance(document, byteArrayOutputStream);
        
        document.open();
        
        // 시리얼 번호 개수만큼 루프를 돌며 페이지를 분할 생성합니다.
        for (int i = 0; i < serialNos.size(); i++) {
            // 두 번째 페이지부터는 페이지 분할(New Page)을 처리합니다.
            if (i > 0) {
                document.newPage();
            }
            
            String currentSerial = serialNos.get(i);
            int pageIndex = i + 1;
            
            // 임시 페이지 내용 렌더링
            document.add(new Paragraph("========================================="));
            document.add(new Paragraph("         CALIBRATION CERTIFICATE         "));
            document.add(new Paragraph("========================================="));
            document.add(new Paragraph("Certificate NO: " + certificateNo));
            document.add(new Paragraph("Certificate Date: " + certificateDate));
            document.add(new Paragraph("Expiry Date: " + expiryDate));
            document.add(new Paragraph("Serial NO: " + currentSerial));
            document.add(new Paragraph("Page Number: Page " + pageIndex + " of " + serialNos.size()));
            document.add(new Paragraph("========================================="));
            document.add(new Paragraph("Status: DRAFT PREVIEW (Step 2 Baseline)"));
        }
        
        document.close();
        return byteArrayOutputStream.toByteArray();
    }
}
