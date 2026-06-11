package com.grimoriopathfinder;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "com.grimoriopathfinder",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class ArchitectureBoundariesTest {

  @ArchTest
  static final ArchRule layeredBoundaries =
      layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .layer("Domain").definedBy("com.grimoriopathfinder.domain..")
          .layer("Application").definedBy("com.grimoriopathfinder.application..")
          .layer("Infrastructure").definedBy("com.grimoriopathfinder.infrastructure..")
          .layer("Web").definedBy("com.grimoriopathfinder.web..")
          .whereLayer("Domain").mayOnlyBeAccessedByLayers(
              "Application", "Infrastructure", "Web")
          .whereLayer("Application").mayOnlyBeAccessedByLayers(
              "Infrastructure", "Web")
          .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Web")
          .whereLayer("Web").mayNotBeAccessedByAnyLayer();
}
