// backend/src/main/java/com/example/pdfgen/service/PdfGenerationService.java
package com.example.pdfgen.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PdfGenerationService {

    private static final String FONT_PATH = "src/main/resources/fonts/NanumGothic.ttf";
    private static final String LOGO_PATH = "src/main/resources/images/logo.png";
    private static final String SIG_PATH = "src/main/resources/images/signature.png";
    private static final String STAMP_PATH = "src/main/resources/images/stamp.png";

    private static final Color THEME_BLUE = new Color(47, 85, 151); // #2F5597
    private static final Color LIGHT_GRAY = new Color(242, 242, 242); // #F2F2F2

    // ── 레이아웃 상수 (수치 변경은 여기서만) ────────────────────────────────
    // A4 높이 842pt, 상하여백 각 25pt → 실가용: 842 - 50 = 792pt
    private static final int CELL_PADDING = 6; // 표 셀 패딩 (전체 공통)
    private static final float LOGO_MAX_WIDTH = 200f; // 로고 가로 최대 (충분히 크게 설정해야 짤리지 않음)
    private static final float LOGO_MAX_HEIGHT = 55f; // 로고 세로 최대
    private static final float TITLE_FONT_SIZE = 26f;
    private static final int TITLE_PAD_V = 12; // 배너 상하 패딩
    private static final int SPACE_AFTER_LOGO = 14; // 로고 아래 여백
    private static final int SPACE_AFTER_TITLE = 18; // 배너 아래 여백
    private static final int SPACE_AFTER_INFO = 18; // 정보표 아래 여백
    private static final int SPACE_AFTER_REF = 18; // 기준기 표 아래 여백
    private static final int CERT_TEXT_LEADING = 13; // 인증문단 줄간격
    private static final int SPACE_AFTER_CERT = 26; // 인증문단 아래 여백
    private static final int SPACE_AFTER_PROD = 26; // 제품표 아래 여백
    private static final int SPACE_AFTER_CAL_HDR = 6; // 교정결과 헤더 아래 여백
    private static final int SPACE_AFTER_CAL = 28; // 교정표 아래 여백
    private static final int SPACE_AFTER_SIGN = 70; // 서명표 아래 여백 (서명~주소 사이 가장 큰 여유)
    // 서명 이미지: 4번째 컬럼(비율2.0/10 × 535pt=107pt) - 패딩(5×2=10) = 97pt 이내로 반드시 제한
    private static final float SIG_MAX_WIDTH = 85f; // 97pt 셀 콘텐츠폭 안에 완전히 수용
    private static final float SIG_MAX_HEIGHT = 30f;
    private static final float STAMP_MAX_WIDTH = 130f;
    private static final float STAMP_MAX_HEIGHT = 55f;

    public byte[] generateCertificatePdf(String certNo, String certDate, String calDate, String expDate,
            List<String> serialNos) throws DocumentException {
        // A4, 좌우여백 30pt, 상하여백 25pt
        Document document = new Document(PageSize.A4, 30, 30, 25, 25);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        try {
            BaseFont base = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font fTitle = new Font(base, TITLE_FONT_SIZE, Font.BOLD, Color.WHITE);
            Font fBold = new Font(base, 10, Font.BOLD, Color.BLACK);
            Font fNormal = new Font(base, 10, Font.NORMAL, Color.BLACK);
            Font fSmall = new Font(base, 9, Font.NORMAL, Color.BLACK);

            for (int i = 0; i < serialNos.size(); i++) {
                if (i > 0) {
                    document.newPage();
                }
                String pageCertNo = generatePageCertNo(certNo, i);
                drawSinglePage(document, pageCertNo, certDate, calDate, expDate,
                        serialNos.get(i), i + 1, serialNos.size(),
                        fTitle, fBold, fNormal, fSmall);
            }
        } catch (IOException exception) {
            throw new DocumentException("폰트 로딩 중 예외 발생: " + exception.getMessage());
        }

        document.close();
        return out.toByteArray();
    }

    /**
     * Certificate NO의 맨 마지막 4자리 숫자를 페이지 오프셋만큼 증가.
     * (\d{4})$ 로 마지막 4자리만 잡아 Integer 범위 초과를 방지.
     */
    private String generatePageCertNo(String certNo, int pageOffset) {
        if (pageOffset == 0)
            return certNo;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d{4})$");
        java.util.regex.Matcher matcher = pattern.matcher(certNo);
        if (matcher.find()) {
            String numStr = matcher.group(1);
            int nextNumber = Integer.parseInt(numStr) + pageOffset;
            String nextNumStr = String.format("%0" + numStr.length() + "d", nextNumber);
            return certNo.substring(0, matcher.start()) + nextNumStr;
        }
        return certNo;
    }

    private void drawSinglePage(Document doc, String certNo, String certDate, String calDate, String expDate,
            String serial, int pageIdx, int totalPages,
            Font fTitle, Font fBold, Font fNormal, Font fSmall)
            throws DocumentException, IOException {

        // ── 1. 로고 (~55pt) ───────────────────────────────────────────
        Image logo = loadAndFitImage(LOGO_PATH, LOGO_MAX_WIDTH, LOGO_MAX_HEIGHT);
        logo.setAlignment(Element.ALIGN_LEFT);
        logo.setSpacingAfter(SPACE_AFTER_LOGO);
        doc.add(logo);

        // ── 2. 배너 타이틀 (~12+26+12 = 50pt) ──────────────────────────────
        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);
        PdfPCell titleCell = new PdfPCell(new Phrase("Calibration Certificate", fTitle));
        titleCell.setBackgroundColor(THEME_BLUE);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleCell.setPaddingTop(TITLE_PAD_V);
        titleCell.setPaddingBottom(TITLE_PAD_V);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleTable.addCell(titleCell);
        titleTable.setSpacingAfter(SPACE_AFTER_TITLE);
        doc.add(titleTable);

        // ── 3. Certificate NO / Date 정보표 (1행, ~22pt) ────────────────────
        PdfPTable infoTable = new PdfPTable(new float[] { 1.8f, 3.2f, 2.0f, 3.0f });
        infoTable.setWidthPercentage(100);
        addCell(infoTable, "Certificate NO.", fBold, Element.ALIGN_LEFT, LIGHT_GRAY);
        addCell(infoTable, certNo, fNormal, Element.ALIGN_LEFT, Color.WHITE);
        addCell(infoTable, "Certificate Date :", fBold, Element.ALIGN_LEFT, LIGHT_GRAY);
        addCell(infoTable, certDate.replace("-", "/"), fNormal, Element.ALIGN_LEFT, Color.WHITE);
        infoTable.setSpacingAfter(SPACE_AFTER_INFO);
        doc.add(infoTable);

        // ── 4. Reference Instrumentation (제목 + 2행 표, ~14+44 = 58pt) ────
        Paragraph refTitle = new Paragraph("Reference Instrumentation", fBold);
        refTitle.setSpacingAfter(6);
        doc.add(refTitle);

        PdfPTable refTable = new PdfPTable(new float[] { 2.5f, 1.5f, 2.2f, 1.8f, 2.0f });
        refTable.setWidthPercentage(100);
        for (String header : new String[] { "Device", "Manufacturer", "Equipment", "Accuracy", "Calibration Date" }) {
            addCell(refTable, header, fBold, Element.ALIGN_CENTER, LIGHT_GRAY);
        }
        addCell(refTable, "Temperature & Humidity Chamber", fNormal, Element.ALIGN_CENTER, Color.WHITE);
        addCell(refTable, "GTPS", fNormal, Element.ALIGN_CENTER, Color.WHITE);
        addCell(refTable, "GTPS-1TH500ES", fNormal, Element.ALIGN_CENTER, Color.WHITE);
        addCell(refTable, "±0.2℃", fNormal, Element.ALIGN_CENTER, Color.WHITE);
        addCell(refTable, "2025/07/10", fNormal, Element.ALIGN_CENTER, Color.WHITE);
        refTable.setSpacingAfter(SPACE_AFTER_REF);
        doc.add(refTable);

        // ── 5. 인증 문단 (4줄 × 13pt = 52pt + after14 = 66pt) ──────────────
        Paragraph certText = new Paragraph(
                "Optilo Co., Ltd. certifies that the devices mentioned on this certificate have been " +
                        "thoroughly tested, validated and reached performance accuracy specifications over the " +
                        "specific ranges. These device were assembled, tested and calibrated in accordance with " +
                        "its specifications. All reference instruments are calibrated by Optilo Corporate-affiliated research institutes.",
                fSmall);
        certText.setAlignment(Element.ALIGN_JUSTIFIED);
        certText.setLeading(CERT_TEXT_LEADING);
        certText.setSpacingAfter(SPACE_AFTER_CERT);
        doc.add(certText);

        // ── 6. Product Information (제목 + 2행 표, ~14+44 = 58pt) ──────────
        Paragraph prodTitle = new Paragraph("Product Information", fBold);
        prodTitle.setSpacingAfter(6);
        doc.add(prodTitle);

        PdfPTable prodTable = new PdfPTable(new float[] { 1.8f, 1.5f, 1.7f, 1.8f, 1.8f, 1.4f });
        prodTable.setWidthPercentage(100);
        for (String header : new String[] { "Product Name", "Model", "Serial No.", "Calibration Date", "Expiry Date",
                "Quantity" }) {
            addCell(prodTable, header, fBold, Element.ALIGN_CENTER, LIGHT_GRAY);
        }
        addCell(prodTable, "A-10", fNormal, Element.ALIGN_CENTER, Color.WHITE);
        addCell(prodTable, "A-10", fNormal, Element.ALIGN_CENTER, Color.WHITE);
        addCell(prodTable, serial, fBold, Element.ALIGN_CENTER, Color.WHITE);
        addCell(prodTable, calDate.replace("-", "/"), fNormal, Element.ALIGN_CENTER, Color.WHITE);
        addCell(prodTable, expDate.replace("-", "/"), fNormal, Element.ALIGN_CENTER, Color.WHITE);
        addCell(prodTable, "1PCS", fNormal, Element.ALIGN_CENTER, Color.WHITE);
        prodTable.setSpacingAfter(SPACE_AFTER_PROD);
        doc.add(prodTable);

        // ── 7. Calibration Results 헤더 행 (2열, ~14pt) ─────────────────────
        PdfPTable calTitleTable = new PdfPTable(new float[] { 5.0f, 5.0f });
        calTitleTable.setWidthPercentage(100);

        PdfPCell calLeftCell = new PdfPCell(new Phrase("Calibration Results", fBold));
        calLeftCell.setBorder(Rectangle.NO_BORDER);
        calLeftCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        calTitleTable.addCell(calLeftCell); // ← 반드시 addCell 필요

        PdfPCell calRightCell = new PdfPCell(
                new Phrase("(Environment Conditions: Air Temperature 26.1℃, Relative Humidity 43.5%)", fSmall));
        calRightCell.setBorder(Rectangle.NO_BORDER);
        calRightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        calRightCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        calTitleTable.addCell(calRightCell); // ← 이전 코드에서 누락되어 레이아웃 깨짐

        calTitleTable.setSpacingAfter(SPACE_AFTER_CAL_HDR);
        doc.add(calTitleTable);

        // ── 8. Calibration Results 표 (4행 × ~22pt = 88pt) ─────────────────
        PdfPTable calTable = new PdfPTable(new float[] { 4.0f, 3.0f, 3.0f });
        calTable.setWidthPercentage(100);
        for (String header : new String[] { "Validation Points", "Tolerance", "Result" }) {
            addCell(calTable, header, fBold, Element.ALIGN_CENTER, LIGHT_GRAY);
        }
        for (String[] row : new String[][] {
                { "0℃", "±0.5", "0.3℃ / PASS" },
                { "5℃", "±0.5", "4.8℃ / PASS" },
                { "10℃", "±0.5", "10.2℃ / PASS" } }) {
            addCell(calTable, row[0], fNormal, Element.ALIGN_CENTER, Color.WHITE);
            addCell(calTable, row[1], fNormal, Element.ALIGN_CENTER, Color.WHITE);
            addCell(calTable, row[2], fBold, Element.ALIGN_CENTER, Color.WHITE);
        }
        calTable.setSpacingAfter(SPACE_AFTER_CAL);
        doc.add(calTable);

        // ── 9. 서명 / 합격 표 (~40pt) ──────────────────────────────────
        PdfPTable signTable = new PdfPTable(new float[] { 2.5f, 2.0f, 3.5f, 2.0f });
        signTable.setWidthPercentage(100);
        addCell(signTable, "Test Result Pass/Fail", fBold, Element.ALIGN_CENTER, LIGHT_GRAY);
        addCell(signTable, "Pass", fBold, Element.ALIGN_CENTER, Color.WHITE);
        addCell(signTable, "Signature of Calibration Technician", fBold, Element.ALIGN_CENTER, LIGHT_GRAY);

        // 서명 이미지: loadAndFitImage로 DPI 보정 + 시케일 적용 후 Chunk 래핑으로 정확한 크기로 셀에 삽입
        // 시그니쳐 콘텐츠 폭: (2.0/10) × 535pt = 107pt - 패딩(5×2) = 97pt
        Image signatureImage = loadAndFitImage(SIG_PATH, SIG_MAX_WIDTH, SIG_MAX_HEIGHT);
        Chunk sigChunk = new Chunk(signatureImage, 0, 0);
        PdfPCell sigCell = new PdfPCell(new Phrase(sigChunk));
        sigCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        sigCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        sigCell.setPadding(5);
        sigCell.setBorderWidth(0.5f);
        sigCell.setBorderColor(Color.BLACK);
        signTable.addCell(sigCell);
        signTable.setSpacingAfter(SPACE_AFTER_SIGN);
        doc.add(signTable);

        // ── 10. 하단 주소 + 도장 (~60pt) ────────────────────────────────────
        PdfPTable footerTable = new PdfPTable(new float[] { 7.0f, 3.0f });
        footerTable.setWidthPercentage(100);

        PdfPCell addressCell = new PdfPCell();
        addressCell.setBorder(Rectangle.NO_BORDER);
        addressCell.addElement(new Paragraph("Optilo Co., Ltd", fNormal));
        addressCell.addElement(new Paragraph(
                "1703, Smart Valley Building D, 30, Songdomirae-Ro, Yeonsu-gu, Incheon, Republic of Korea, 21990",
                fSmall));
        Paragraph contactInfo = new Paragraph("Tel: 070-5143-8585  Fax: 050-4886-5157  Email: info@optilol.net  Web: ",
                fSmall);
        Anchor webLink = new Anchor("www.optilo.net", new Font(fSmall.getFamily(), 9, Font.UNDERLINE, Color.BLUE));
        webLink.setReference("http://www.optilo.net");
        contactInfo.add(webLink);
        addressCell.addElement(contactInfo);
        Paragraph pageNum = new Paragraph("Page " + pageIdx + " of " + totalPages, fSmall);
        pageNum.setSpacingBefore(8);
        addressCell.addElement(pageNum);
        footerTable.addCell(addressCell);

        // 도장 이미지: loadAndFitImage로 DPI 보정 + Chunk 래핑으로 정확한 크기로 셀에 삽입
        // 도장 콘텐츠 폭: (3.0/10) × 535pt = 160.5pt - 기본패딩(2×2) = 156.5pt
        Image stampImage = loadAndFitImage(STAMP_PATH, STAMP_MAX_WIDTH, STAMP_MAX_HEIGHT);
        Chunk stampChunk = new Chunk(stampImage, 0, 0);
        PdfPCell stampCell = new PdfPCell(new Phrase(stampChunk));
        stampCell.setBorder(Rectangle.NO_BORDER);
        stampCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        stampCell.setVerticalAlignment(Element.ALIGN_TOP);
        footerTable.addCell(stampCell);
        doc.add(footerTable);
    }

    /**
     * DPI 보정을 포함한 안전한 이미지 로더
     * iText/OpenPDF는 72DPI 기준으로 렌더링하지만 이미지 파일은 96DPI로 저장될 수 있으므로,
     * 실제 플에이스먼트 크기를 DPI에 따른 스케일 보정으로 계산한 후 scaleAbsolute로 적용.
     */
    private Image loadAndFitImage(String path, float maxWidth, float maxHeight) throws IOException, DocumentException {
        Image image = Image.getInstance(path);
        // iText는 이미지의 DPI 정보를 기반으로 실제 pt 크기를 결정.
        // DPI가 72이 아닌 경우 비례 보정
        float dpiX = image.getDpiX() > 0 ? image.getDpiX() : 72f;
        float dpiY = image.getDpiY() > 0 ? image.getDpiY() : 72f;
        float naturalWidthPt = image.getWidth() * 72f / dpiX; // 실제 pt 폭
        float naturalHeightPt = image.getHeight() * 72f / dpiY; // 실제 pt 높이
        // maxWidth/maxHeight 안에 들어오도록 비례 유지 스케일 인수 계산
        float scaleRatio = Math.min(maxWidth / naturalWidthPt, maxHeight / naturalHeightPt);
        if (scaleRatio > 1.0f)
            scaleRatio = 1.0f; // 확대는 하지 않음
        image.scaleAbsolute(naturalWidthPt * scaleRatio, naturalHeightPt * scaleRatio);
        return image;
    }

    private void addCell(PdfPTable table, String text, Font font, int align, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(CELL_PADDING);
        cell.setBackgroundColor(bgColor);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(Color.BLACK);
        table.addCell(cell);
    }
}
