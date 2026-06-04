// frontend/src/components/CertificateHistoryList.jsx
import React, { useState, useEffect } from 'react';
import './CertificateHistoryList.css';

export default function CertificateHistoryList() {
    const [histories, setHistories] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [downloadingId, setDownloadingId] = useState(null);
    const [expandedId, setExpandedId] = useState(null);
    const [searchTerm, setSearchTerm] = useState("");

    useEffect(() => {
        fetchHistories();
    }, []);

    const fetchHistories = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await fetch('/api/documents/certificates');
            if (!response.ok) {
                throw new Error('이력 데이터를 불러오는데 실패했습니다.');
            }
            const data = await response.json();
            setHistories(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleDownload = async (event, id, certificateNo, mode) => {
        // 행 클릭(펼치기)과 버튼 클릭 이벤트가 겹치지 않도록 이벤트 전파 차단
        event.stopPropagation();
        try {
            setDownloadingId(id);
            const response = await fetch(`/api/documents/certificates/${id}/download?mode=${mode}`);
            if (!response.ok) {
                const errData = await response.json();
                throw new Error(errData.message || '다운로드에 실패했습니다.');
            }
            
            // 파일명 추출 (Content-Disposition 헤더 활용, 없으면 fallback)
            let downloadFilename = mode === 'INDIVIDUAL' ? `${certificateNo}.zip` : `${certificateNo}.pdf`;
            const contentDisposition = response.headers.get('content-disposition');
            if (contentDisposition) {
                const filenameMatch = contentDisposition.match(/filename="?([^";]+)"?/);
                if (filenameMatch && filenameMatch[1]) {
                    downloadFilename = filenameMatch[1];
                }
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = downloadFilename;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch (err) {
            alert(err.message);
        } finally {
            setDownloadingId(null);
        }
    };

    const handleRowClick = (id) => {
        setExpandedId(prevId => (prevId === id ? null : id));
    };

    if (loading) return <div className="history-status">로딩 중...</div>;
    if (error) return <div className="error-bubble">{error}</div>;

    // 검색어 필터링 로직
    const filteredHistories = histories.filter(h => {
        if (!searchTerm) return true;
        const lowerSearch = searchTerm.toLowerCase();
        // 발급 번호에 포함되거나
        if (h.certificateNo.toLowerCase().includes(lowerSearch)) return true;
        // 시리얼 번호 중 하나라도 포함되면
        if (h.serialNos?.some(serial => serial.toLowerCase().includes(lowerSearch))) return true;
        return false;
    });

    return (
        <div className="history-container">
            {/* 검색창 UI */}
            <div className="history-search-bar">
                <input
                    type="text"
                    placeholder="발급 번호 또는 시리얼 번호 검색..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="history-search-input"
                />
            </div>

            {histories.length === 0 ? (
                <div className="history-status">발급된 성적서 이력이 없습니다.</div>
            ) : (
                <div className="table-wrapper">
                    <table className="history-table">
                        <thead>
                            <tr>
                                <th className="col-arrow"></th>
                                <th>발급 번호</th>
                                <th>발급일</th>
                                <th>교정일</th>
                                <th>만료일</th>
                                <th>시리얼 개수</th>
                                <th>다운로드</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredHistories.length === 0 ? (
                                <tr>
                                    <td colSpan="7" className="history-status" style={{ border: 'none', paddingTop: '30px' }}>
                                        검색 결과가 없습니다.
                                    </td>
                                </tr>
                            ) : (
                                filteredHistories.map(h => {
                                const isExpanded = expandedId === h.id;
                                const serialCount = h.serialNos?.length || 0;
                                return (
                                    <React.Fragment key={h.id}>
                                        {/* 메인 행 */}
                                        <tr
                                            className={`history-row ${isExpanded ? 'expanded' : ''}`}
                                            onClick={() => handleRowClick(h.id)}
                                        >
                                            <td className="col-arrow">
                                                <span className={`arrow-icon ${isExpanded ? 'open' : ''}`}>▶</span>
                                            </td>
                                            <td>{h.certificateNo}</td>
                                            <td>{h.certificateDate}</td>
                                            <td>{h.calibrationDate}</td>
                                            <td>{h.expiryDate}</td>
                                            <td>{serialCount}개</td>
                                            <td>
                                                <div style={{ display: 'flex', gap: '4px', justifyContent: 'center' }}>
                                                    <button
                                                        className="sub-btn download-btn"
                                                        onClick={(e) => handleDownload(e, h.id, h.certificateNo, 'MERGED')}
                                                        disabled={downloadingId === h.id}
                                                        style={{ padding: '6px 8px', fontSize: '11px', flex: 1 }}
                                                    >
                                                        {downloadingId === h.id ? '생성 중...' : '통합'}
                                                    </button>
                                                    <button
                                                        className="sub-btn download-btn"
                                                        onClick={(e) => handleDownload(e, h.id, h.certificateNo, 'INDIVIDUAL')}
                                                        disabled={downloadingId === h.id}
                                                        style={{ padding: '6px 8px', fontSize: '11px', flex: 1 }}
                                                    >
                                                        {downloadingId === h.id ? '생성 중...' : '개별(ZIP)'}
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>

                                        {/* 펼쳐진 시리얼 번호 행 */}
                                        {isExpanded && (
                                            <tr className="serial-detail-row">
                                                <td colSpan={7}>
                                                    <div className="serial-detail-container">
                                                        <span className="serial-detail-label">시리얼 번호 목록</span>
                                                        <div className="serial-chip-list">
                                                            {h.serialNos?.map((serial, idx) => (
                                                                <span key={idx} className="serial-chip">
                                                                    {serial}
                                                                </span>
                                                            ))}
                                                        </div>
                                                    </div>
                                                </td>
                                            </tr>
                                        )}
                                    </React.Fragment>
                                );
                            }))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}
