package com.timecapsule.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Pretty-printed JSON copy of the diary. */
public class JsonExport implements ExportStrategy {

    @Override
    public String extension() {
        return "json";
    }

    @Override
    public void write(List<ExportedEntry> entries, String keeperName, Path target) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("keeper", keeperName);
        root.put("exportedAt", Instant.now().toString());
        ArrayNode array = root.putArray("entries");
        for (ExportedEntry entry : entries) {
            ObjectNode node = array.addObject();
            node.put("title", entry.title());
            node.put("body", entry.body());
            node.put("mood", entry.mood());
            node.putPOJO("tags", entry.tags());
            node.put("createdAt", entry.createdAt() == null ? null : entry.createdAt().toString());
            node.put("unlockDate", entry.unlockDate() == null ? null : entry.unlockDate().toString());
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), root);
    }
}
