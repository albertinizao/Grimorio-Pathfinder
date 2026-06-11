package com.grimoriopathfinder.overrides;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
public class SpellOverridesJsonRepository {

    private final ObjectMapper objectMapper;

    public SpellOverridesJsonRepository() {
        this.objectMapper = new ObjectMapper().findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public SpellOverridesFile read(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new SpellOverridesFile(1, null, Map.of());
        }
        return objectMapper.readValue(Files.readString(path), SpellOverridesFile.class);
    }

    public void write(Path path, SpellOverridesFile file) throws IOException {
        var parentDirectory = path.toAbsolutePath().getParent();
        if (parentDirectory != null) {
            Files.createDirectories(parentDirectory);
        }
        var directory = parentDirectory;
        var tempFile = directory == null
                ? Files.createTempFile(path.getFileName().toString(), ".tmp")
                : Files.createTempFile(directory, path.getFileName().toString(), ".tmp");
        try {
            Files.writeString(tempFile, objectMapper.writeValueAsString(file));
            try {
                Files.move(tempFile, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailure) {
                Files.move(tempFile, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
