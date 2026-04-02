package dev.agrp.contract.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class PdfTextExtractor {

    public String extract(InputStream pdf) {
        byte[] bytes;
        try {
            bytes = pdf.readAllBytes();
        } catch (IOException e) {
            throw new PdfExtractionException("Failed to read PDF input stream", e);
        }

        if (bytes.length == 0) {
            throw new PdfExtractionException("PDF input stream is empty", null);
        }

        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new PdfExtractionException("Failed to extract text from PDF", e);
        }
    }
}
