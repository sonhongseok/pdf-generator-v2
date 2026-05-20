// backend/src/main/java/com/example/pdfgen/repository/CertificateHistoryRepository.java
package com.example.pdfgen.repository;

import com.example.pdfgen.domain.CertificateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificateHistoryRepository extends JpaRepository<CertificateHistory, Long> {
    
    /**
     * 최종 Certificate NO의 데이터베이스 중복 등록 여부를 체크합니다.
     * @param certificateNo 최종 조합된 발급 번호
     * @return 중복 존재 여부
     */
    boolean existsByCertificateNo(String certificateNo);
}
