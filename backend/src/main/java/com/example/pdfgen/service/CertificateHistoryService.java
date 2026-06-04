// backend/src/main/java/com/example/pdfgen/service/CertificateHistoryService.java
package com.example.pdfgen.service;

import com.example.pdfgen.domain.CertificateHistory;
import com.example.pdfgen.domain.CertificateSerialMapping;
import com.example.pdfgen.repository.CertificateHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CertificateHistoryService {

    private final CertificateHistoryRepository certificateHistoryRepository;

    public CertificateHistoryService(CertificateHistoryRepository certificateHistoryRepository) {
        this.certificateHistoryRepository = certificateHistoryRepository;
    }

    /**
     * DB 저장 전용 트랜잭션 메서드 (PDF 생성 성공 후에만 호출됨)
     * 컨트롤러와 별도 빈(Bean)으로 분리하여 Spring AOP 프록시를 통해 트랜잭션이 정상 적용되도록 함.
     * 같은 클래스 내부에서 직접 호출(self-invocation)하면 @Transactional이 무시되는 문제 방지.
     */
    @Transactional
    public void saveToDatabase(String finalCertificateNo, LocalDate certDate, LocalDate calDate,
                               LocalDate expDate, List<String> serialNos, List<Integer> sequenceNos) {
        CertificateHistory history = new CertificateHistory(finalCertificateNo, certDate, calDate, expDate);

        for (int i = 0; i < serialNos.size(); i++) {
            String serialNo = serialNos.get(i);
            int seqNo = sequenceNos.get(i);
            int pageNum = i + 1; // 1-indexed page number
            CertificateSerialMapping mapping = new CertificateSerialMapping(serialNo, pageNum, seqNo);
            history.addSerialMapping(mapping);
        }

        // JPA cascade 옵션에 의해 Mapping 데이터도 연쇄 저장됨
        certificateHistoryRepository.save(history);
    }
}
