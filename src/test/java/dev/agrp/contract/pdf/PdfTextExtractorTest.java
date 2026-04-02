package dev.agrp.contract.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    void extract_returnsTextFromValidPdf() throws Exception {
        byte[] pdf = buildPdf("Sample contract text");

        String result = extractor.extract(new ByteArrayInputStream(pdf));

        assertThat(result).contains("Sample contract text");
    }

    @Test
    void extract_throwsOnEmptyInputStream() {
        assertThatThrownBy(() -> extractor.extract(new ByteArrayInputStream(new byte[0])))
                .isInstanceOf(PdfExtractionException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void extract_throwsOnCorruptBytes() {
        byte[] garbage = "not a valid pdf".getBytes();

        assertThatThrownBy(() -> extractor.extract(new ByteArrayInputStream(garbage)))
                .isInstanceOf(PdfExtractionException.class);
    }

    private byte[] buildPdf(String text) throws Exception {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(100, 700);
                content.showText(text);
                content.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
