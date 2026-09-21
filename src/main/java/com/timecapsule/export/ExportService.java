package com.timecapsule.export;

import com.timecapsule.audit.AuditLogger;
import com.timecapsule.auth.AuthException;
import com.timecapsule.auth.AuthService;
import com.timecapsule.auth.User;
import com.timecapsule.core.JournalManager;
import com.timecapsule.core.Memory;
import com.timecapsule.export.ExportStrategy.ExportedEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Copies the signed-in keeper's diary to disk (Module: Data Export &
 * Persistence). Sealed entries travel as "[sealed until …]" markers —
 * plaintext never leaves early and ciphertext never leaks at all.
 */
public final class ExportService {

    public enum Format {
        JSON, PDF
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final DateTimeFormatter FILE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

    private ExportService() {
    }

    /** Exports to {@code directory}, returning the written file. */
    public static Path exportCurrentUser(Format format, Path directory) throws IOException {
        User user = AuthService.currentUser();
        if (user == null) {
            throw new AuthException("No keeper is signed in.");
        }
        ExportStrategy strategy = format == Format.PDF ? new PdfExport() : new JsonExport();
        List<ExportedEntry> views = JournalManager.listEntries().stream()
                .map(ExportService::toView)
                .collect(Collectors.toList());
        Files.createDirectories(directory);
        Path target = directory.resolve("time-capsule-" + FILE_FMT.format(Instant.now())
                + "." + strategy.extension());
        strategy.write(views, user.getUsername(), target);
        AuditLogger.log(user.getId(), "export." + strategy.extension() + " " + target.getFileName());
        return target;
    }

    /** Default shelf for copies: {@code ~/.timecapsule/exports/}. */
    public static Path defaultDirectory() {
        return Path.of(System.getProperty("user.home"), ".timecapsule", "exports");
    }

    private static ExportedEntry toView(Memory memory) {
        String body;
        if (memory.getBody() != null) {
            body = memory.getBody();
        } else if (memory.isLocked() && memory.getUnlockDate() != null) {
            body = "[sealed until " + DATE_FMT.format(
                    memory.getUnlockDate().atZone(ZoneId.systemDefault())) + "]";
        } else {
            body = "[empty]";
        }
        return new ExportedEntry(
                memory.getTitle(),
                body,
                memory.getMood() == null ? "" : memory.getMood().getLabel(),
                memory.getTags(),
                memory.getCreatedAt(),
                memory.getUnlockDate());
    }
}
