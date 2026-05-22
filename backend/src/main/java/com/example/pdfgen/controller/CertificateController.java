// backend/src/main/java/com/example/pdfgen/controller/CertificateController.java
package com.example.pdfgen.controller;

import com.example.pdfgen.domain.CertificateHistory;
import com.example.pdfgen.domain.CertificateSerialMapping;
import com.example.pdfgen.dto.CertificateRequest;
import com.example.pdfgen.repository.CertificateHistoryRepository;
import com.example.pdfgen.service.DocxTemplateService;
import com.example.pdfgen.service.MsWordPdfConverter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.example.pdfgen.util.ResourceInitializer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents/certificates")
public class CertificateController {

    private final CertificateHistoryRepository certificateHistoryRepository;
    private final DocxTemplateService docxTemplateService;
    private final MsWordPdfConverter msWordPdfConverter;

    // 생성자 주입 및 기동 시 리소스 초기화
    public CertificateController(CertificateHistoryRepository certificateHistoryRepository,
            DocxTemplateService docxTemplateService,
            MsWordPdfConverter msWordPdfConverter) {
        this.certificateHistoryRepository = certificateHistoryRepository;
        this.docxTemplateService = docxTemplateService;
        this.msWordPdfConverter = msWordPdfConverter;

        // 3단계 필수 리소스(한글 폰트 및 이미지 자산) 자동 준비
        ResourceInitializer.initializeResources();
    }

    /**
     * 성적서 PDF 생성 및 다운로드 API 엔드포인트
     * 입력값 유효성 검사 → PDF 생성 성공 후 → DB 저장 순서로 처리하여
     * PDF 생성 실패 시 DB에 데이터가 남는 버그를 원천 차단합니다.
     */
    @PostMapping("/pdf")
    public ResponseEntity<?> generateAndDownloadCertificate(@RequestBody CertificateRequest request) {
        // 1. 필수 입력값 존재 여부 유효성 검증
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
        LocalDate calDate;
        LocalDate expDate;
        try {
            certDate = LocalDate.parse(request.getCertificateDate());
            calDate = LocalDate.parse(request.getCalibrationDate());
            expDate = LocalDate.parse(request.getExpiryDate());
        } catch (DateTimeParseException e) {
            return buildErrorResponse("날짜 포맷이 올바르지 않습니다. YYYY-MM-DD 형식이어야 합니다.", HttpStatus.BAD_REQUEST);
        }

        // 날짜 논리 오류 검사 (만료일이 발행일보다 빠른지 여부)
        if (certDate.isAfter(expDate)) {
            return buildErrorResponse("만료일(Expiry Date)은 발행일(Certificate Date)보다 빠를 수 없습니다.",
                    HttpStatus.BAD_REQUEST);
        }

        // 3. 4가지 입력값 완전 중복 발급 검사 (날짜 3개 + 시리얼 번호 목록 모두 동일 시 차단)
        Set<String> requestedSerialSet = new HashSet<>(request.getSerialNos());
        List<CertificateHistory> sameDateHistories = certificateHistoryRepository
                .findByCertificateDateAndCalibrationDateAndExpiryDate(certDate, calDate, expDate);

        boolean isExactDuplicate = sameDateHistories.stream().anyMatch(history -> {
            Set<String> savedSerialSet = history.getSerialMappings().stream()
                    .map(CertificateSerialMapping::getSerialNo)
                    .collect(Collectors.toSet());
            return savedSerialSet.equals(requestedSerialSet);
        });

        if (isExactDuplicate) {
            return buildErrorResponse(
                    "동일한 발행일, 교정일, 만료일, 시리얼 번호로 이미 발급된 성적서가 존재합니다. 입력 정보를 다시 확인해 주세요.",
                    HttpStatus.BAD_REQUEST);
        }

        // 4. Certificate NO 조합용 날짜 문자열
        String formattedDateStr = request.getCertificateDate().replace("-", "");

        // 5. Word 템플릿 치환 및 PDF 변환 (시리얼 번호별로 각각 수행 후 병합)
        byte[] finalPdfContent;
        try {
            List<byte[]> pdfPages = new ArrayList<>();
            List<String> serialNos = request.getSerialNos();
            for (int i = 0; i < serialNos.size(); i++) {
                String serialNo = serialNos.get(i);
                
                // 페이지별로 0001부터 시작하여 증가 (예: 0001, 0002, 0003...)
                String pageCertNo = String.format("OP%s%04d", formattedDateStr, (i + 1));

                Map<String, String> variables = new HashMap<>();
                variables.put("cno", pageCertNo);
                variables.put("cedate", request.getCertificateDate().replace("-", "/"));
                variables.put("pdate", request.getCalibrationDate().replace("-", "/"));
                variables.put("edate", request.getExpiryDate().replace("-", "/"));
                variables.put("sno", serialNo);

                // 템플릿 치환하여 .docx 바이트 배열 생성
                byte[] docxBytes = docxTemplateService.fillTemplate(variables);
                // LibreOffice로 PDF 변환
                byte[] pdfBytes = msWordPdfConverter.convertToPdf(docxBytes);
                
                pdfPages.add(pdfBytes);
            }
            
            // 병합
            finalPdfContent = msWordPdfConverter.mergePdfs(pdfPages);

        } catch (Exception e) {
            e.printStackTrace();
            return buildErrorResponse("서버 내부 에러로 PDF 생성을 진행할 수 없습니다: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 6. PDF 생성이 성공한 경우에만 DB에 발급 내역 저장
        // (대표 식별자로 첫 번째 페이지의 인증번호 0001을 저장)
        String baseCertificateNo = "OP" + formattedDateStr + "0001";
        saveToDatabase(baseCertificateNo, certDate, calDate, expDate, request.getSerialNos());

        // 7. 바이너리 스트림 다운로드 헤더 정의 및 ResponseEntity 반환
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", baseCertificateNo + ".pdf");
        headers.setContentLength(finalPdfContent.length);

        return new ResponseEntity<>(finalPdfContent, headers, HttpStatus.OK);
    }

    /**
     * DB 저장 전용 트랜잭션 메서드 (PDF 생성 성공 후에만 호출됨)
     */
    @Transactional
    protected void saveToDatabase(String finalCertificateNo, LocalDate certDate, LocalDate calDate, LocalDate expDate, List<String> serialNos) {
        CertificateHistory history = new CertificateHistory(finalCertificateNo, certDate, calDate, expDate);

        for (int i = 0; i < serialNos.size(); i++) {
            String serialNo = serialNos.get(i);
            int pageNum = i + 1; // 1-indexed page index
            CertificateSerialMapping mapping = new CertificateSerialMapping(serialNo, pageNum);
            history.addSerialMapping(mapping);
        }

        // JPA cascade 옵션에 의해 Mapping 데이터도 연쇄 저장됨
        certificateHistoryRepository.save(history);
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
