// backend/src/main/java/com/example/pdfgen/dto/CertificateRequest.java
package com.example.pdfgen.dto;

import java.util.List;

public class CertificateRequest {

    private String certificateDate;
    private String expiryDate;
    private String certificateSeq;
    private List<String> serialNos;

    // 기본 생성자
    public CertificateRequest() {
    }

    // 편의 생성자
    public CertificateRequest(String certificateDate, String expiryDate, String certificateSeq, List<String> serialNos) {
        this.certificateDate = certificateDate;
        this.expiryDate = expiryDate;
        this.certificateSeq = certificateSeq;
        this.serialNos = serialNos;
    }

    // Getter and Setter
    public String getCertificateDate() {
        return certificateDate;
    }

    public void setCertificateDate(String certificateDate) {
        this.certificateDate = certificateDate;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCertificateSeq() {
        return certificateSeq;
    }

    public void setCertificateSeq(String certificateSeq) {
        this.certificateSeq = certificateSeq;
    }

    public List<String> getSerialNos() {
        return serialNos;
    }

    public void setSerialNos(List<String> serialNos) {
        this.serialNos = serialNos;
    }
}
