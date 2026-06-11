package com.grimoriopathfinder.sqlite;

import com.grimoriopathfinder.dataset.SpellDatasetImportService;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SpellCatalogProjectionRebuilder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SpellCatalogProjectionRebuilder.class);

    private final SpellDatasetImportService importService;
    private final SpellCatalogSqliteRepository repository;
    private final Path generatedPath;
    private final Path overridesPath;
    private final boolean autoRebuild;

    public SpellCatalogProjectionRebuilder(
            SpellDatasetImportService importService,
            SpellCatalogSqliteRepository repository,
            @Value("${grimorio.dataset.generated-path}") String generatedPath,
            @Value("${grimorio.dataset.overrides-path}") String overridesPath,
            @Value("${grimorio.sqlite.auto-rebuild:true}") boolean autoRebuild
    ) {
        this.importService = importService;
        this.repository = repository;
        this.generatedPath = Path.of(generatedPath);
        this.overridesPath = Path.of(overridesPath);
        this.autoRebuild = autoRebuild;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!autoRebuild) {
            return;
        }
        var result = importService.importDataset(generatedPath, overridesPath);
        if (!result.warnings().isEmpty()) {
            result.warnings().forEach(warning -> log.warn("{}", warning));
        }
        repository.rebuild(result.effectiveSpells());
    }
}
