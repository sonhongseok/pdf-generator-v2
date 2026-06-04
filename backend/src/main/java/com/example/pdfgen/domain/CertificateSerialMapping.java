// backend/src/main/java/com/example/pdfgen/domain/CertificateSerialMapping.java
package com.example.pdfgen.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "certificate_serial_mapping")
public class CertificateSerialMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_history_id", nullable = false)
    private CertificateHistory certificateHistory;

    @Column(name = "serial_no", nullable = false, length = 100)
    private String serialNo;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "sequence_no")
    private Integer sequenceNo;

    // JPA 기본 생성자
    public CertificateSerialMapping() {
    }

    // 편의 생성자
    public CertificateSerialMapping(String serialNo, Integer pageNumber, Integer sequenceNo) {
        this.serialNo = serialNo;
        this.pageNumber = pageNumber;
        this.sequenceNo = sequenceNo;
    }

    // Getter and Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CertificateHistory getCertificateHistory() {
        return certificateHistory;
    }

    public void setCertificateHistory(CertificateHistory certificateHistory) {
        this.certificateHistory = certificateHistory;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Integer sequenceNo) {
        this.sequenceNo = sequenceNo;
    }
}
