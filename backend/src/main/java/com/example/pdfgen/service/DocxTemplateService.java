// backend/src/main/java/com/example/pdfgen/service/DocxTemplateService.java
package com.example.pdfgen.service;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

@Service
public class DocxTemplateService {

    private static final String TEMPLATE_PATH = "templates/certificate_template.docx";

    /**
     * 템플릿을 읽어 변수를 치환한 뒤 byte 배열로 반환합니다. (poi-tl 사용)
     */
    public byte[] fillTemplate(Map<String, String> variables) throws Exception {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        if (!resource.exists()) {
            throw new RuntimeException("Word 템플릿 파일을 찾을 수 없습니다: " + TEMPLATE_PATH);
        }

        try (InputStream is = resource.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 단일 중괄호 { } 를 변수 태그로 인식하도록 설정
            Configure config = Configure.builder().buildGramer("{", "}").build();

            // poi-tl을 사용하면 텍스트 상자, 복잡한 표, 쪼개진 글자(Run) 등을 모두 알아서 병합 및 치환해 줍니다.
            XWPFTemplate template = XWPFTemplate.compile(is, config).render(variables);
            
            template.write(out);
            template.close();
            
            return out.toByteArray();
        }
    }
}
