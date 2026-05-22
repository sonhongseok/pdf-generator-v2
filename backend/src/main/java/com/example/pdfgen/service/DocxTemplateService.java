// backend/src/main/java/com/example/pdfgen/service/DocxTemplateService.java
package com.example.pdfgen.service;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@Service
public class DocxTemplateService {

    // application.yml에서 템플릿 외부 경로를 주입받음
    @Value("${app.template.path:../certificate_template.docx}")
    private String templatePath;

    // 강제 적용할 검정색 HEX 코드
    private static final String BLACK_COLOR = "000000";

    /**
     * 외부 경로에 위치한 템플릿을 읽어 변수를 치환한 뒤 byte 배열로 반환합니다.
     */
    public byte[] fillTemplate(Map<String, String> variables) throws Exception {
        File templateFile = new File(templatePath);
        if (!templateFile.exists() || !templateFile.isFile()) {
            throw new RuntimeException("Word 템플릿 파일을 찾을 수 없습니다: " + templateFile.getAbsolutePath());
        }

        try (InputStream is = new FileInputStream(templateFile);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 단일 중괄호 { } 를 변수 태그로 인식하도록 설정
            Configure config = Configure.builder().buildGramer("{", "}").build();

            // poi-tl을 사용하면 텍스트 상자, 복잡한 표, 쪼개진 글자(Run) 등을 모두 알아서 병합 및 치환해 줍니다.
            XWPFTemplate template = XWPFTemplate.compile(is, config).render(variables);

            XWPFDocument doc = template.getXWPFDocument();

            // --- 후처리 1: 문서 본문(Body)의 일반 단락 글자색 → 검정 ---
            for (XWPFParagraph p : doc.getParagraphs()) {
                forceBlackColor(p.getRuns());
            }

            // --- 후처리 2: 모든 표(중첩 포함) 셀에 중앙 정렬 + 글자색 → 검정 ---
            for (XWPFTable table : doc.getTables()) {
                applyStyleRecursively(table);
            }

            template.write(out);
            template.close();

            return out.toByteArray();
        }
    }

    /**
     * 주어진 표(table)의 모든 셀에 수직/수평 중앙 정렬과 글자색 검정을 적용합니다.
     * 중첩된 표가 있으면 재귀적으로 동일하게 처리합니다.
     */
    private void applyStyleRecursively(XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {

                // 1. 수직 중앙 정렬 (Vertical Center)
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

                // 2. XML 레벨에서 셀 내부 패딩(상하좌우)을 완전히 0으로 강제 설정
                CTTcPr tcPr = cell.getCTTc().getTcPr() != null
                        ? cell.getCTTc().getTcPr()
                        : cell.getCTTc().addNewTcPr();

                CTTcMar tcMar = tcPr.getTcMar() != null
                        ? tcPr.getTcMar()
                        : tcPr.addNewTcMar();

                setMargin(tcMar.isSetTop()    ? tcMar.getTop()    : tcMar.addNewTop(),    0);
                setMargin(tcMar.isSetBottom() ? tcMar.getBottom() : tcMar.addNewBottom(), 0);
                setMargin(tcMar.isSetLeft()   ? tcMar.getLeft()   : tcMar.addNewLeft(),   0);
                setMargin(tcMar.isSetRight()  ? tcMar.getRight()  : tcMar.addNewRight(),  0);

                // 3. 수평 중앙 정렬 + 단락 앞뒤 간격 제거 + 글자색 검정 강제 적용
                for (XWPFParagraph p : cell.getParagraphs()) {
                    p.setAlignment(ParagraphAlignment.CENTER);
                    p.setSpacingAfter(0);
                    p.setSpacingBefore(0);
                    forceBlackColor(p.getRuns());
                }

                // 4. 셀 안에 중첩된 표가 있으면 재귀 적용
                for (XWPFTable nestedTable : cell.getTables()) {
                    applyStyleRecursively(nestedTable);
                }
            }
        }
    }

    /**
     * 주어진 Run 목록의 글자 색상을 모두 검정(#000000)으로 강제 설정합니다.
     * 템플릿 변수 자리에 남아있던 주황/파란 색상이 치환 후에도 남는 문제를 해결합니다.
     */
    private void forceBlackColor(List<XWPFRun> runs) {
        for (XWPFRun run : runs) {
            run.setColor(BLACK_COLOR);
        }
    }

    /**
     * CTTblWidth 마진 설정 헬퍼: 값과 타입(DXA)을 강제로 지정합니다.
     */
    private void setMargin(CTTblWidth margin, int value) {
        margin.setW(BigInteger.valueOf(value));
        margin.setType(STTblWidth.DXA);
    }
}
