// backend/src/main/java/com/example/pdfgen/repository/CertificateHistoryRepository.java
package com.example.pdfgen.repository;

import com.example.pdfgen.domain.CertificateHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateHistoryRepository extends JpaRepository<CertificateHistory, Long> {

    /**
     * 최종 Certificate NO의 데이터베이스 중복 등록 여부를 체크.
     *
     * @param certificateNo 최종 조합된 발급 번호
     * @return 중복 존재 여부
     */
    boolean existsByCertificateNo(String certificateNo);

    /**
     * 특정 날짜(접두사)로 발급된 성적서 중 가장 마지막 번호의 이력을 조회.
     * 시리얼 개수에 따른 다음 발급 번호 계산을 위해 연관된 매핑(serialMappings)도 함께 조회.
     */
    @EntityGraph(attributePaths = { "serialMappings" })
    Optional<CertificateHistory> findTopByCertificateNoStartingWithOrderByCertificateNoDesc(String prefix);

    /**
     * 발행일 + 교정일 + 만료일이 모두 동일한 발급 이력 목록을 조회.
     * 4가지 입력값 완전 중복 여부 검사에 사용.
     * serialMappings도 함께 로딩하여 시리얼 번호 비교에 사용.
     */
    @EntityGraph(attributePaths = { "serialMappings" })
    List<CertificateHistory> findByCertificateDateAndCalibrationDateAndExpiryDate(
            LocalDate certificateDate, LocalDate calibrationDate, LocalDate expiryDate);
}
