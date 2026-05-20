// frontend/src/App.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';

export default function App() {
  // 1. 입력 필드 상태 관리
  const [certificateSequence, setCertificateSequence] = useState('');
  const [certificateDate, setCertificateDate] = useState(getTodayString());
  const [expiryDate, setExpiryDate] = useState('');
  const [serialNumbersText, setSerialNumbersText] = useState('');

  // 2. 파싱 및 비즈니스 상태 관리
  const [parsedSerialNumbers, setParsedSerialNumbers] = useState([]);
  const [previewCertificateNo, setPreviewCertificateNo] = useState('');
  
  // 3. UI 및 비동기 트랜잭션 상태 관리
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submissionError, setSubmissionError] = useState('');
  const [submissionSuccess, setSubmissionSuccess] = useState(false);

  // 오늘 날짜 문자열 YYYY-MM-DD 획득 함수
  function getTodayString() {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  // 실시간 Certificate NO 조합 규칙 (OP + yyyyMMdd + 일련번호 4자리 패딩)
  useEffect(() => {
    if (!certificateDate) {
      setPreviewCertificateNo('');
      return;
    }
    
    // 날짜 하이픈 제거 (yyyyMMdd 포맷)
    const formattedDate = certificateDate.replace(/-/g, '');
    
    // 일련번호 왼쪽 패딩 4자리 적용
    let paddedSequence = '0000';
    if (certificateSequence) {
      const sanitizedSequence = certificateSequence.replace(/[^0-9]/g, '');
      paddedSequence = sanitizedSequence.padStart(4, '0');
    }
    
    setPreviewCertificateNo(`OP${formattedDate}${paddedSequence}`);
  }, [certificateSequence, certificateDate]);

  // 실시간 Serial NO 파싱 (공백 기준 연속 공백 처리)
  useEffect(() => {
    if (!serialNumbersText) {
      setParsedSerialNumbers([]);
      return;
    }
    
    // 앞뒤 공백 제거 후 연속된 공백을 단일 공백으로 치환하여 분할
    const cleanedText = serialNumbersText.trim().replace(/\s+/g, ' ');
    if (cleanedText === '') {
      setParsedSerialNumbers([]);
      return;
    }
    
    const serialList = cleanedText.split(' ');
    setParsedSerialNumbers(serialList);
  }, [serialNumbersText]);

  // 일련번호 입력 핸들러 (숫자만 입력 허용, 최대 4자리)
  const handleSequenceChange = (event) => {
    const inputValue = event.target.value;
    const numericValue = inputValue.replace(/[^0-9]/g, '');
    
    const MAX_SEQUENCE_LENGTH = 4;
    if (numericValue.length <= MAX_SEQUENCE_LENGTH) {
      setCertificateSequence(numericValue);
    }
  };

  // PDF 생성 및 다운로드 API 연동 핸들러
  const handleGeneratePdf = async (event) => {
    event.preventDefault();
    
    // 클라이언트 측 유효성 검사
    if (!certificateSequence) {
      setSubmissionError('Certificate 일련번호를 입력해 주세요. (예: 1)');
      return;
    }
    if (!certificateDate) {
      setSubmissionError('Certificate Date를 선택해 주세요.');
      return;
    }
    if (!expiryDate) {
      setSubmissionError('Expiry Date를 선택해 주세요.');
      return;
    }
    if (parsedSerialNumbers.length === 0) {
      setSubmissionError('Serial NO를 하나 이상 입력해 주세요. (공백 구분)');
      return;
    }

    // 날짜 선후 관계 검증
    if (new Date(certificateDate) > new Date(expiryDate)) {
      setSubmissionError('만료일(Expiry Date)은 발행일(Certificate Date)보다 빠를 수 없습니다.');
      return;
    }

    setIsSubmitting(true);
    setSubmissionError('');
    setSubmissionSuccess(false);

    try {
      // API 전송 Payload
      const requestPayload = {
        certificateDate: certificateDate,
        expiryDate: expiryDate,
        certificateSeq: certificateSequence.padStart(4, '0'),
        serialNos: parsedSerialNumbers
      };

      // Axios 바이너리 다운로드 요청
      const response = await axios.post('/api/documents/certificates/pdf', requestPayload, {
        responseType: 'blob'
      });

      // 다운로드 파일명 획득 (서버 헤더 확인 후 없으면 조합된 기본 파일명 사용)
      const contentDisposition = response.headers['content-disposition'];
      let downloadFileName = `${previewCertificateNo}.pdf`;
      if (contentDisposition) {
        const fileNameMatch = contentDisposition.match(/filename="?([^"]+)"?/);
        if (fileNameMatch && fileNameMatch[1]) {
          downloadFileName = fileNameMatch[1];
        }
      }

      // Blob 객체를 이용한 즉시 브라우저 다운로드 실행
      const blobUrl = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const downloadAnchor = document.createElement('a');
      downloadAnchor.href = blobUrl;
      downloadAnchor.setAttribute('download', downloadFileName);
      document.body.appendChild(downloadAnchor);
      downloadAnchor.click();
      
      // 메모리 누수 방지 리소스 해제
      document.body.removeChild(downloadAnchor);
      window.URL.revokeObjectURL(blobUrl);

      setSubmissionSuccess(true);
    } catch (error) {
      console.error('PDF generation error:', error);
      let clientErrorMessage = '서버에서 PDF를 생성하는 중 알 수 없는 오류가 발생했습니다.';
      
      // Blob 형태의 에러인 경우 텍스트로 변환하여 에러 메시지 추출 시도
      if (error.response && error.response.data instanceof Blob) {
        const textError = await error.response.data.text();
        try {
          const parsedJsonError = JSON.parse(textError);
          clientErrorMessage = parsedJsonError.message || clientErrorMessage;
        } catch (jsonParseError) {
          clientErrorMessage = textError || clientErrorMessage;
        }
      } else if (error.response && error.response.data && error.response.data.message) {
        clientErrorMessage = error.response.data.message;
      } else if (error.message) {
        clientErrorMessage = `네트워크 오류: ${error.message}`;
      }
      
      setSubmissionError(clientErrorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div style={styles.appContainer}>
      <header style={styles.appHeader}>
        <div style={styles.headerIndicator}>CALIBRATION SYSTEM</div>
        <h1 style={styles.mainTitle}>EXPORT DOCUMENTS</h1>
        <p style={styles.subTitle}>성적서 PDF 생성 및 다페이지 자동 분할 발행</p>
      </header>

      <main style={styles.mainContent}>
        <div className="glass-container" style={styles.formCard}>
          <form onSubmit={handleGeneratePdf} style={styles.formContainer} noValidate>
            
            {/* 실시간 조합 Certificate NO 미리보기 영역 */}
            <div style={styles.previewBox}>
              <span style={styles.previewLabel}>GENERATED CERTIFICATE NO</span>
              <div style={styles.previewValue}>
                {previewCertificateNo || 'OPYYYYMMDD0000'}
              </div>
            </div>

            {/* 일련번호 (Certificate NO 조합용) */}
            <div className="form-group">
              <label htmlFor="cert-seq" className="form-label">
                Certificate NO (일련번호 입력)
              </label>
              <input
                id="cert-seq"
                type="text"
                className="form-input"
                placeholder="숫자 입력 (예: 1)"
                value={certificateSequence}
                onChange={handleSequenceChange}
                required
              />
              <span style={styles.inputGuide}>숫자만 최대 4자리 입력이 가능하며, 자동으로 4자리로 정규화됩니다. (예: 1 → 0001)</span>
            </div>

            {/* 날짜 입력 그리드 */}
            <div style={styles.gridTwoColumns}>
              {/* 발행일 */}
              <div className="form-group">
                <label htmlFor="cert-date" className="form-label">
                  Certificate Date (발행일)
                </label>
                <input
                  id="cert-date"
                  type="date"
                  className="form-input"
                  value={certificateDate}
                  onChange={(e) => setCertificateDate(e.target.value)}
                  required
                />
              </div>

              {/* 만료일 */}
              <div className="form-group">
                <label htmlFor="expiry-date" className="form-label">
                  Expiry Date (만료일)
                </label>
                <input
                  id="expiry-date"
                  type="date"
                  className="form-input"
                  value={expiryDate}
                  onChange={(e) => setExpiryDate(e.target.value)}
                  required
                />
              </div>
            </div>

            {/* Serial NO 다건 입력 */}
            <div className="form-group">
              <label htmlFor="serial-nos" className="form-label">
                Serial NO (다건 입력, 공백 구분)
              </label>
              <textarea
                id="serial-nos"
                className="form-input"
                rows="4"
                placeholder="예: 280A97 280A98 280A99"
                value={serialNumbersText}
                onChange={(e) => setSerialNumbersText(e.target.value)}
                style={styles.textAreaInput}
                required
              />
              <div style={styles.serialHelper}>
                <span>공백(스페이스) 기준으로 구분하여 작성해 주세요.</span>
                {parsedSerialNumbers.length > 0 && (
                  <span style={styles.badgeSuccess}>
                    총 {parsedSerialNumbers.length}페이지 생성 예정
                  </span>
                )}
              </div>
            </div>

            {/* 파싱된 Serial NO 배지 프리뷰 */}
            {parsedSerialNumbers.length > 0 && (
              <div style={styles.serialBadgeContainer}>
                {parsedSerialNumbers.map((serial, index) => (
                  <span key={`${serial}-${index}`} style={styles.serialBadge}>
                    P.{index + 1}: {serial}
                  </span>
                ))}
              </div>
            )}

            {/* 에러 및 성공 상태 알림 메시지 영역 */}
            {submissionError && (
              <div style={styles.alertDanger}>
                <span style={styles.alertIcon}>⚠️</span>
                <span>{submissionError}</span>
              </div>
            )}

            {submissionSuccess && (
              <div style={styles.alertSuccess}>
                <span style={styles.alertIcon}>✓</span>
                <span>성적서 PDF가 성공적으로 다운로드되었습니다. (파일명: {previewCertificateNo}.pdf)</span>
              </div>
            )}

            {/* 생성 버튼 */}
            <button
              type="submit"
              className="btn-primary"
              style={styles.submitBtn}
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <div className="loading-spinner" style={styles.spinnerSpacing}></div>
                  PDF 생성 및 다운로드 중...
                </>
              ) : (
                'EXIT (성적서 생성)'
              )}
            </button>
          </form>
        </div>
      </main>

      <footer style={styles.appFooter}>
        <p>© 2026 Calibration PDF Generator Inc. All rights reserved.</p>
      </footer>
    </div>
  );
}

// React Inline JS Styles (CSS 변수 기반 디자인 고도화 보완)
const styles = {
  appContainer: {
    maxWidth: '800px',
    margin: '0 auto',
    padding: '40px 20px',
    display: 'flex',
    flexDirection: 'column',
    minHeight: '100vh',
  },
  appHeader: {
    textAlign: 'center',
    marginBottom: '40px',
  },
  headerIndicator: {
    display: 'inline-block',
    fontSize: '0.75rem',
    fontWeight: '700',
    letterSpacing: '0.15em',
    color: '#6366f1',
    background: 'rgba(99, 102, 241, 0.1)',
    padding: '6px 12px',
    borderRadius: '20px',
    marginBottom: '16px',
    border: '1px solid rgba(99, 102, 241, 0.2)',
  },
  mainTitle: {
    fontSize: '2.5rem',
    fontWeight: '800',
    color: '#f8fafc',
    marginBottom: '8px',
  },
  subTitle: {
    fontSize: '1rem',
    color: '#94a3b8',
  },
  mainContent: {
    flex: '1 0 auto',
  },
  formCard: {
    padding: '40px',
  },
  formContainer: {
    display: 'flex',
    flexDirection: 'column',
    gap: '24px',
  },
  previewBox: {
    background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.15) 0%, rgba(139, 92, 246, 0.15) 100%)',
    border: '1px solid rgba(99, 102, 241, 0.3)',
    borderRadius: '12px',
    padding: '20px',
    textAlign: 'center',
    boxShadow: 'inset 0 2px 4px 0 rgba(0, 0, 0, 0.1)',
  },
  previewLabel: {
    fontSize: '0.75rem',
    fontWeight: '700',
    color: '#a5b4fc',
    letterSpacing: '0.1em',
    display: 'block',
    marginBottom: '6px',
  },
  previewValue: {
    fontFamily: "'Outfit', monospace",
    fontSize: '1.75rem',
    fontWeight: '800',
    color: '#ffffff',
    letterSpacing: '0.05em',
    textShadow: '0 0 10px rgba(99, 102, 241, 0.5)',
  },
  gridTwoColumns: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
    gap: '20px',
  },
  inputGuide: {
    fontSize: '0.8rem',
    color: '#64748b',
    marginTop: '2px',
  },
  textAreaInput: {
    resize: 'vertical',
    fontFamily: 'monospace',
  },
  serialHelper: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: '8px',
    fontSize: '0.8rem',
    color: '#64748b',
  },
  badgeSuccess: {
    background: 'rgba(16, 185, 129, 0.15)',
    color: '#10b981',
    border: '1px solid rgba(16, 185, 129, 0.3)',
    borderRadius: '6px',
    padding: '2px 8px',
    fontWeight: '600',
  },
  serialBadgeContainer: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: '8px',
    padding: '16px',
    background: 'rgba(15, 23, 42, 0.4)',
    border: '1px solid rgba(255, 255, 255, 0.05)',
    borderRadius: '10px',
  },
  serialBadge: {
    background: 'rgba(255, 255, 255, 0.06)',
    border: '1px solid rgba(255, 255, 255, 0.1)',
    color: '#cbd5e1',
    fontSize: '0.8rem',
    padding: '4px 10px',
    borderRadius: '6px',
    fontFamily: 'monospace',
  },
  alertDanger: {
    background: 'rgba(239, 68, 68, 0.1)',
    border: '1px solid rgba(239, 68, 68, 0.2)',
    borderRadius: '10px',
    color: '#fca5a5',
    padding: '16px',
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    fontSize: '0.9rem',
  },
  alertSuccess: {
    background: 'rgba(16, 185, 129, 0.1)',
    border: '1px solid rgba(16, 185, 129, 0.2)',
    borderRadius: '10px',
    color: '#a7f3d0',
    padding: '16px',
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    fontSize: '0.9rem',
  },
  alertIcon: {
    fontSize: '1.2rem',
    fontWeight: 'bold',
  },
  submitBtn: {
    padding: '16px',
    fontSize: '1.1rem',
    marginTop: '12px',
    width: '100%',
  },
  spinnerSpacing: {
    marginRight: '12px',
  },
  appFooter: {
    textAlign: 'center',
    marginTop: '40px',
    color: '#64748b',
    fontSize: '0.8rem',
  },
};
