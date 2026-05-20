// backend/src/test/java/com/example/pdfgen/controller/CertificateControllerTest.java
package com.example.pdfgen.controller;

import com.example.pdfgen.domain.CertificateHistory;
import com.example.pdfgen.domain.CertificateSerialMapping;
import com.example.pdfgen.repository.CertificateHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CertificateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CertificateHistoryRepository certificateHistoryRepository;

    @Test
    @DisplayName("성적서 발급 API를 호출하면, 데이터베이스에 이력이 완벽히 저장되고 PDF 바이너리가 응답되어야 한다.")
    void shouldCreateCertificateAndSaveToDatabase() throws Exception {
        // given
        // 테스트용 입력 JSON Payload 정의 (2026-05-20 날짜에 9999 일련번호 조합 -> OP202605209999)
        String requestJsonPayload = "{"
                + "\"certificateSeq\":\"9999\","
                + "\"certificateDate\":\"2026-05-20\","
                + "\"expiryDate\":\"2027-05-20\","
                + "\"serialNos\":[\"TEST-SERIAL-A\", \"TEST-SERIAL-B\", \"TEST-SERIAL-C\"]"
                + "}";

        String expectedCertificateNo = "OP202605209999";

        // when
        // MockMvc를 이용하여 API 컨트롤러 호출 테스트 진행
        mockMvc.perform(post("/api/documents/certificates/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonPayload))
                .andExpect(status().isOk());

        // then
        // 1. 데이터베이스 마스터 테이블에 이력이 정상 적재되었는지 조회 검증
        boolean isExists = certificateHistoryRepository.existsByCertificateNo(expectedCertificateNo);
        assertThat(isExists).isTrue();

        // 2. 데이터베이스 상세 이력 및 1:N 관계 매핑 데이터 정합성 검증
        CertificateHistory history = certificateHistoryRepository.findAll().stream()
                .filter(h -> h.getCertificateNo().equals(expectedCertificateNo))
                .findFirst()
                .orElseThrow(() -> new AssertionError("데이터베이스에 저장된 성적서 이력을 찾을 수 없습니다."));

        assertThat(history.getCertificateDate().toString()).isEqualTo("2026-05-20");
        assertThat(history.getExpiryDate().toString()).isEqualTo("2027-05-20");

        // 시리얼 번호 매핑 리스트 데이터 검증
        List<CertificateSerialMapping> mappings = history.getSerialMappings();
        assertThat(mappings).hasSize(3); // 3개의 시리얼을 보냈으므로 3개 매핑이 보존되어야 함

        // 상세 시리얼 내용 및 페이지 할당 정합성 검증
        assertThat(mappings.get(0).getSerialNo()).isEqualTo("TEST-SERIAL-A");
        assertThat(mappings.get(0).getPageNumber()).isEqualTo(1);

        assertThat(mappings.get(1).getSerialNo()).isEqualTo("TEST-SERIAL-B");
        assertThat(mappings.get(1).getPageNumber()).isEqualTo(2);

        assertThat(mappings.get(2).getSerialNo()).isEqualTo("TEST-SERIAL-C");
        assertThat(mappings.get(2).getPageNumber()).isEqualTo(3);
    }
}
