package com.grimoriopathfinder.overrides;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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
        Files.createDirectories(path.getParent());
        Files.writeString(path, objectMapper.writeValueAsString(file));
    }
}
