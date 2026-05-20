// backend/src/main/java/com/example/pdfgen/controller/CertificateController.java
package com.example.pdfgen.controller;

import com.example.pdfgen.domain.CertificateHistory;
import com.example.pdfgen.domain.CertificateSerialMapping;
import com.example.pdfgen.dto.CertificateRequest;
import com.example.pdfgen.repository.CertificateHistoryRepository;
import com.lowagie.text.DocumentException;
import com.example.pdfgen.service.PdfGenerationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/documents/certificates")
public class CertificateController {

    private final CertificateHistoryRepository certificateHistoryRepository;
    private final PdfGenerationService pdfGenerationService;

    // 생성자 주입
    public CertificateController(CertificateHistoryRepository certificateHistoryRepository,
            PdfGenerationService pdfGenerationService) {
        this.certificateHistoryRepository = certificateHistoryRepository;
        this.pdfGenerationService = pdfGenerationService;
    }

    /**
     * 성적서 PDF 생성 및 다운로드 API 엔드포인트
     * 프론트엔드 요청 데이터를 수신해 유효성 검사 후, DB에 발급 내역을 저장하고 PDF 스트림을 반환.
     */
    @PostMapping("/pdf")
    @Transactional
    public ResponseEntity<?> generateAndDownloadCertificate(@RequestBody CertificateRequest request) {
        try {
            // 1. 필수 입력값 존재 여부 유효성 검증
            if (request.getCertificateSeq() == null || request.getCertificateSeq().trim().isEmpty()) {
                return buildErrorResponse("Certificate 일련번호는 필수 입력 항목입니다.", HttpStatus.BAD_REQUEST);
            }
            if (request.getCertificateDate() == null || request.getCertificateDate().trim().isEmpty()) {
                return buildErrorResponse("Certificate Date는 필수 선택 항목입니다.", HttpStatus.BAD_REQUEST);
            }
            if (request.getExpiryDate() == null || request.getExpiryDate().trim().isEmpty()) {
                return buildErrorResponse("Expiry Date는 필수 선택 항목입니다.", HttpStatus.BAD_REQUEST);
            }
            if (request.getSerialNos() == null || request.getSerialNos().isEmpty()) {
                return buildErrorResponse("Serial NO는 최소 하나 이상 입력되어야 합니다.", HttpStatus.BAD_REQUEST);
            }

            // 2. 날짜 파싱 및 검증
            LocalDate certDate;
            LocalDate expDate;
            try {
                certDate = LocalDate.parse(request.getCertificateDate());
                expDate = LocalDate.parse(request.getExpiryDate());
            } catch (DateTimeParseException e) {
                return buildErrorResponse("날짜 포맷이 올바르지 않습니다. YYYY-MM-DD 형식이어야 합니다.", HttpStatus.BAD_REQUEST);
            }

            // 날짜 논리 오류 검사 (만료일이 발행일보다 빠른지 여부)
            if (certDate.isAfter(expDate)) {
                return buildErrorResponse("만료일(Expiry Date)은 발행일(Certificate Date)보다 빠를 수 없습니다.",
                        HttpStatus.BAD_REQUEST);
            }

            // 3. Certificate NO 조합 (OP + yyyyMMdd + sequence 4자리)
            String formattedDateStr = request.getCertificateDate().replace("-", "");
            String sanitizedSeq = request.getCertificateSeq().replaceAll("[^0-9]", "");
            String paddedSeq = String.format("%04d", Integer.parseInt(sanitizedSeq));
            String finalCertificateNo = "OP" + formattedDateStr + paddedSeq;

            // 4. 데이터베이스 중복 가입 발급 내역 검증 (MySQL Workbench 대응)
            if (certificateHistoryRepository.existsByCertificateNo(finalCertificateNo)) {
                return buildErrorResponse("이미 동일한 발급 번호(" + finalCertificateNo + ")로 생성된 성적서가 존재합니다. 일련번호를 변경해 주세요.",
                        HttpStatus.BAD_REQUEST);
            }

            // 5. 엔티티 생성 및 데이터베이스 매핑 저장
            CertificateHistory history = new CertificateHistory(finalCertificateNo, certDate, expDate);

            // 페이지별 Serial Mapping 관계 조율
            for (int i = 0; i < request.getSerialNos().size(); i++) {
                String serialNo = request.getSerialNos().get(i);
                int pageNum = i + 1; // 1-indexed page index

                CertificateSerialMapping mapping = new CertificateSerialMapping(serialNo, pageNum);
                history.addSerialMapping(mapping);
            }

            // 데이터베이스 영속화 (JPA cascade 옵션에 의해 Mapping 데이터도 연쇄 저장됨)
            certificateHistoryRepository.save(history);

            // 6. PDF 파일 바이너리 생성 호출
            byte[] pdfContent = pdfGenerationService.generateCertificatePdf(
                    finalCertificateNo,
                    request.getCertificateDate(),
                    request.getExpiryDate(),
                    request.getSerialNos());

            // 7. 바이너리 스트림 다운로드 헤더 정의 및 ResponseEntity 반환
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", finalCertificateNo + ".pdf");
            headers.setContentLength(pdfContent.length);

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);

        } catch (DocumentException e) {
            return buildErrorResponse("PDF 생성 중 라이브러리 예외가 발생했습니다: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NumberFormatException e) {
            return buildErrorResponse("일련번호가 올바른 숫자 형식이 아닙니다.", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return buildErrorResponse("서버 내부 에러로 PDF 생성을 진행할 수 없습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 공통 오류 JSON 포맷 빌더
     */
    private ResponseEntity<Map<String, String>> buildErrorResponse(String message, HttpStatus status) {
        Map<String, String> errorResponseBody = new HashMap<>();
        errorResponseBody.put("message", message);
        return new ResponseEntity<>(errorResponseBody, status);
    }
}
