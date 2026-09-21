package com.timecapsule.export;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Clean printable PDF copy: serif titles, one section per entry. */
public class PdfExport implements ExportStrategy {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");

    @Override
    public String extension() {
        return "pdf";
    }

    @Override
    public void write(List<ExportedEntry> entries, String keeperName, Path target) throws java.io.IOException {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            PdfWriter.getInstance(document, Files.newOutputStream(target));
            document.open();
            document.add(new Paragraph("Time Capsule — a keeper's copy",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20)));
            document.add(new Paragraph("Copied for " + keeperName + " · " + DATE_FMT.format(LocalDate.now()),
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 11)));
            document.add(new Paragraph(" "));
            for (int i = 0; i < entries.size(); i++) {
                ExportedEntry entry = entries.get(i);
                document.add(new Paragraph(entry.title(),
                        FontFactory.getFont(FontFactory.TIMES_BOLD, 16)));
                String meta = formatDate(entry.createdAt()) + "  ·  " + entry.mood()
                        + (entry.tags().isEmpty() ? ""
                                : "  ·  " + String.join(", ", entry.tags()));
                document.add(new Paragraph(meta, FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10)));
                document.add(new Paragraph(" "));
                document.add(new Paragraph(entry.body(), FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
                if (i < entries.size() - 1) {
                    document.newPage();
                }
            }
        } catch (Exception e) {
            throw new java.io.IOException("The PDF copy would not take.", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private static String formatDate(Instant moment) {
        if (moment == null) {
            return "undated";
        }
        return DATE_FMT.format(moment.atZone(ZoneId.systemDefault()));
    }
}
