package com.timecapsule.export;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * One export format (Strategy Pattern). Strategies render plain views —
 * {@link ExportService} resolves sealed bodies to markers first, so no
 * format can ever leak an early plaintext.
 */
public interface ExportStrategy {

    /** File entry: body already marker-resolved, never early plaintext. */
    record ExportedEntry(String title, String body, String mood, List<String> tags,
                         Instant createdAt, Instant unlockDate) {
    }

    String extension();

    void write(List<ExportedEntry> entries, String keeperName, Path target) throws IOException;
}
