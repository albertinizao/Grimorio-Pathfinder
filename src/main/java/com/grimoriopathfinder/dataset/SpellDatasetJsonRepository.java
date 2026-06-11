package com.grimoriopathfinder.dataset;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SpellDatasetJsonRepository {

    private final ObjectMapper objectMapper;

    public SpellDatasetJsonRepository() {
        this.objectMapper = new ObjectMapper().findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public SpellDatasetFile read(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Missing generated dataset: " + path);
        }
        return objectMapper.readValue(Files.readString(path), SpellDatasetFile.class);
    }
}
