package com.example.pdfgen.repository;

import com.example.pdfgen.domain.CertificateHistory;
import com.example.pdfgen.domain.CertificateSerialMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CertificateHistoryRepositoryTest {

    @Autowired
    private CertificateHistoryRepository historyRepository;

    @Test
    @DisplayName("성적서 이력 및 시리얼 매핑이 정상적으로 DB에 저장되고 조회된다")
    void saveAndFindTest() {
        // given
        CertificateHistory history = new CertificateHistory();
        history.setCertificateNo("OP202605210001");
        history.setCertificateDate(LocalDate.of(2026, 5, 21));
        history.setCalibrationDate(LocalDate.of(2026, 5, 20));
        history.setExpiryDate(LocalDate.of(2027, 5, 20));
        history.setCreatedDate(LocalDateTime.now());

        CertificateSerialMapping mapping1 = new CertificateSerialMapping();
        mapping1.setSerialNo("SN001");
        mapping1.setPageNumber(1);
        history.addSerialMapping(mapping1);

        CertificateSerialMapping mapping2 = new CertificateSerialMapping();
        mapping2.setSerialNo("SN002");
        mapping2.setPageNumber(2);
        history.addSerialMapping(mapping2);

        // when
        CertificateHistory savedHistory = historyRepository.save(history);
        historyRepository.flush(); // 실제 DB 반영 쿼리 강제 실행

        // then
        assertThat(savedHistory.getId()).isNotNull();
        assertThat(savedHistory.getSerialMappings()).hasSize(2);
        assertThat(savedHistory.getSerialMappings().get(0).getSerialNo()).isEqualTo("SN001");

        // 조회 테스트
        CertificateHistory found = historyRepository.findById(savedHistory.getId()).orElseThrow();
        assertThat(found.getCertificateNo()).isEqualTo("OP202605210001");
    }

    @Test
    @DisplayName("발행일, 교정일, 만료일로 성적서를 정확히 조회할 수 있다 (중복 검증용)")
    void findByDatesTest() {
        // given
        LocalDate certDate = LocalDate.of(2026, 5, 21);
        LocalDate calDate = LocalDate.of(2026, 5, 20);
        LocalDate expDate = LocalDate.of(2027, 5, 20);

        CertificateHistory history1 = new CertificateHistory();
        history1.setCertificateNo("OP202605210001");
        history1.setCertificateDate(certDate);
        history1.setCalibrationDate(calDate);
        history1.setExpiryDate(expDate);
        historyRepository.save(history1);

        CertificateHistory history2 = new CertificateHistory();
        history2.setCertificateNo("OP202605210002");
        history2.setCertificateDate(certDate);
        history2.setCalibrationDate(calDate);
        history2.setExpiryDate(expDate);
        historyRepository.save(history2);

        // 다른 날짜 데이터
        CertificateHistory history3 = new CertificateHistory();
        history3.setCertificateNo("OP202605220001");
        history3.setCertificateDate(LocalDate.of(2026, 5, 22));
        history3.setCalibrationDate(calDate);
        history3.setExpiryDate(expDate);
        historyRepository.save(history3);

        // when
        List<CertificateHistory> foundList = historyRepository.findByCertificateDateAndCalibrationDateAndExpiryDate(certDate, calDate, expDate);

        // then
        assertThat(foundList).hasSize(2);
        assertThat(foundList).extracting("certificateNo")
                .containsExactlyInAnyOrder("OP202605210001", "OP202605210002");
    }

}
