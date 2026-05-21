// backend/src/main/java/com/example/pdfgen/domain/CertificateHistory.java
package com.example.pdfgen.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "certificate_history")
public class CertificateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "certificate_no", nullable = false, unique = true, length = 50)
    private String certificateNo;

    @Column(name = "certificate_date", nullable = false)
    private LocalDate certificateDate;

    @Column(name = "calibration_date")
    private LocalDate calibrationDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @OneToMany(mappedBy = "certificateHistory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CertificateSerialMapping> serialMappings = new ArrayList<>();

    // JPA 기본 생성자
    public CertificateHistory() {
        this.createdDate = LocalDateTime.now();
    }

    // 편의 생성자
    public CertificateHistory(String certificateNo, LocalDate certificateDate, LocalDate calibrationDate, LocalDate expiryDate) {
        this.certificateNo = certificateNo;
        this.certificateDate = certificateDate;
        this.calibrationDate = calibrationDate;
        this.expiryDate = expiryDate;
        this.createdDate = LocalDateTime.now();
    }

    // 연관관계 편의 메서드
    public void addSerialMapping(CertificateSerialMapping serialMapping) {
        this.serialMappings.add(serialMapping);
        serialMapping.setCertificateHistory(this);
    }

    // Getter and Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCertificateNo() {
        return certificateNo;
    }

    public void setCertificateNo(String certificateNo) {
        this.certificateNo = certificateNo;
    }

    public LocalDate getCertificateDate() {
        return certificateDate;
    }

    public void setCertificateDate(LocalDate certificateDate) {
        this.certificateDate = certificateDate;
    }

    public LocalDate getCalibrationDate() {
        return calibrationDate;
    }

    public void setCalibrationDate(LocalDate calibrationDate) {
        this.calibrationDate = calibrationDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public List<CertificateSerialMapping> getSerialMappings() {
        return serialMappings;
    }

    public void setSerialMappings(List<CertificateSerialMapping> serialMappings) {
        this.serialMappings = serialMappings;
    }
}
