// frontend/src/components/CertificateForm.jsx
import React, { useState } from 'react';
import axios from 'axios';
import '../App.css';

export default function CertificateForm() {
  const [certificateDateText, setCertificateDateText] = useState(getTodayString());
  const [calibrationDateText, setCalibrationDateText] = useState(getTodayString());
  const [expiryDateText, setExpiryDateText] = useState('');
  const [serialNoText, setSerialNoText] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [statusMessage, setStatusMessage] = useState('Ready');
  const [errorMessage, setErrorMessage] = useState('');

  function getTodayString() {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  const normalizeDate = (text) => {
    if (!text) return '';
    const clean = text.replace(/[^0-9]/g, '');
    if (clean.length === 8) {
      return `${clean.substring(0, 4)}-${clean.substring(4, 6)}-${clean.substring(6, 8)}`;
    }
    return text;
  };

  const handleExit = () => {
    if (window.confirm("프로그램을 종료하시겠습니까?")) {
      window.close();
    }
  };

  const handleSave = async (event) => {
    if (event) {
      event.preventDefault();
    }

    const rawCertDate = certificateDateText.trim();
    const rawCalDate = calibrationDateText.trim();
    const rawExpDate = expiryDateText.trim();
    const rawSerials = serialNoText.trim();

    if (!rawCertDate) {
      setErrorMessage('Certificate Date를 입력해 주세요.');
      setStatusMessage('Error: Missing Certificate Date');
      return;
    }
    if (!rawCalDate) {
      setErrorMessage('Calibration Date를 입력해 주세요.');
      setStatusMessage('Error: Missing Calibration Date');
      return;
    }
    if (!rawExpDate) {
      setErrorMessage('Expiry Date를 입력해 주세요.');
      setStatusMessage('Error: Missing Expiry Date');
      return;
    }
    if (!rawSerials) {
      setErrorMessage('Serial No를 입력해 주세요.');
      setStatusMessage('Error: Missing Serial No');
      return;
    }

    const normCertDate = normalizeDate(rawCertDate);
    const normCalDate = normalizeDate(rawCalDate);
    const normExpDate = normalizeDate(rawExpDate);

    const datePattern = /^\d{4}-\d{2}-\d{2}$/;
    if (!datePattern.test(normCertDate) || !datePattern.test(normCalDate) || !datePattern.test(normExpDate)) {
      setErrorMessage('날짜 형식이 올바르지 않습니다. (예: 2026-05-21 또는 20260521)');
      setStatusMessage('Error: Invalid Date Format');
      return;
    }

    if (new Date(normCalDate) > new Date(normExpDate) || new Date(normCertDate) > new Date(normExpDate)) {
      setErrorMessage('만료일은 발행일/교정일보다 빠를 수 없습니다.');
      setStatusMessage('Error: Invalid Date Range');
      return;
    }

    const serialList = rawSerials.split(/\s+/).filter(Boolean);
    if (serialList.length === 0) {
      setErrorMessage('최소 하나 이상의 Serial No가 필요합니다.');
      setStatusMessage('Error: Empty Serial List');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');
    setStatusMessage('Generating PDF Certificates...');

    try {
      const payload = {
        certificateDate: normCertDate,
        calibrationDate: normCalDate,
        expiryDate: normExpDate,
        serialNos: serialList
      };

      const response = await axios.post('/api/documents/certificates/pdf', payload, {
        responseType: 'blob'
      });

      const certNoFormatted = `OP${normCertDate.replace(/-/g, '')}0001`;
      const blobUrl = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const anchor = document.createElement('a');
      anchor.href = blobUrl;
      anchor.setAttribute('download', `${certNoFormatted}.pdf`);
      document.body.appendChild(anchor);
      anchor.click();

      document.body.removeChild(anchor);
      window.URL.revokeObjectURL(blobUrl);

      setStatusMessage('Success: PDF generated successfully.');
    } catch (error) {
      console.error(error);
      let errMsg = 'PDF 생성에 실패했습니다.';
      if (error.response && error.response.data instanceof Blob) {
        const text = await error.response.data.text();
        try {
          const json = JSON.parse(text);
          errMsg = json.message || errMsg;
        } catch (exception) {
          errMsg = text || errMsg;
        }
      }
      setErrorMessage(errMsg);
      setStatusMessage('Error: Operation failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="client-area">
      <div className="form-grid">
        <div className="label-cell">Certificate Date</div>
        <div className="input-cell">
          <input
            type="text"
            value={certificateDateText}
            onChange={(e) => setCertificateDateText(e.target.value)}
            placeholder="YYYY-MM-DD"
            className="classic-input"
          />
        </div>

        <div className="label-cell">Calibration Date</div>
        <div className="input-cell">
          <input
            type="text"
            value={calibrationDateText}
            onChange={(e) => setCalibrationDateText(e.target.value)}
            placeholder="YYYY-MM-DD"
            className="classic-input"
          />
        </div>

        <div className="label-cell">Expiry Date</div>
        <div className="input-cell">
          <input
            type="text"
            value={expiryDateText}
            onChange={(e) => setExpiryDateText(e.target.value)}
            placeholder="YYYY-MM-DD"
            className="classic-input"
          />
        </div>

        <div className="label-cell">Serial No</div>
        <div className="input-cell">
          <input
            type="text"
            value={serialNoText}
            onChange={(e) => setSerialNoText(e.target.value)}
            placeholder="Separate with space"
            className="classic-input"
          />
        </div>
      </div>

      <div className="status-box">
        {isSubmitting ? (
          <div className="progress-bar-fill" />
        ) : (
          <span className="status-text">{statusMessage}</span>
        )}
      </div>

      {errorMessage && (
        <div className="error-bubble">
          <strong>Warning:</strong> {errorMessage}
        </div>
      )}

      <div className="button-area">
        <button onClick={handleSave} disabled={isSubmitting} className="main-btn">
          성적서 생성
        </button>
        <button onClick={handleExit} className="sub-btn">
          종료
        </button>
      </div>
    </div>
  );
}
