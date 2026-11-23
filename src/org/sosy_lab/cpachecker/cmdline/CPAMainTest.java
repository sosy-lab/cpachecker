// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cmdline;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cmdline.CPAMain.BootstrapLanguageOptions;
import org.sosy_lab.cpachecker.util.test.TestUtils;

@SuppressWarnings("deprecation")
@RunWith(Parameterized.class)
public class CPAMainTest {

  @Parameters(name = "{0}")
  public static Object[] getAllLanguages() {
    return Language.values();
  }

  @Parameter(0)
  public Language language;

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private final Multimap<Language, String> languageToInputFile = HashMultimap.create();

  private ConfigurationBuilder configBuilder;

  @Before
  public void init() {
    languageToInputFile.put(Language.C, "test.c");
    languageToInputFile.put(Language.C, "test.i");
    languageToInputFile.put(Language.C, "test.h");
    languageToInputFile.put(Language.C, "test.c, test.i, test.h");
    languageToInputFile.put(Language.JAVA, "Test");
    languageToInputFile.put(Language.LLVM, "test.ll");
    languageToInputFile.put(Language.LLVM, "test.bc");
    languageToInputFile.put(Language.LLVM, "test.ll, test.bc");
  }

  @Before
  public void setUp() throws InvalidConfigurationException {
    configBuilder = TestUtils.configurationForTest();
  }

  @Test
  public void testLanguageDetection_BasedOnFileSuffix_DetectsCorrectly()
      throws InvalidConfigurationException {
    // when language not given by user, right language based on file ending(s) must be detected
    for (Language languageToTest : ImmutableList.of(Language.C, Language.LLVM)) {
      for (String inputPrograms : languageToInputFile.get(languageToTest)) {
        configBuilder.setOption("analysis.programNames", inputPrograms);
        Configuration config = configBuilder.build();
        BootstrapLanguageOptions options = new BootstrapLanguageOptions(config);

        Language detectedLanguage = CPAMain.detectFrontendLanguageIfNecessary(options, config);

        assertThat(detectedLanguage).isEqualTo(languageToTest);
      }
    }
  }

  @Test
  public void testLanguageDetection_WithJavaClassPath_DetectsCorrectly()
      throws InvalidConfigurationException {
    // when language not given by user, right language based on file ending(s) must be detected
    for (String inputPrograms : languageToInputFile.get(Language.JAVA)) {
      configBuilder.setOption("analysis.programNames", inputPrograms);
      configBuilder.setOption("java.classpath", "lib");
      Configuration config = configBuilder.build();
      BootstrapLanguageOptions options = new BootstrapLanguageOptions(config);

      Language detectedLanguage = CPAMain.detectFrontendLanguageIfNecessary(options, config);

      assertThat(detectedLanguage).isEqualTo(Language.JAVA);
    }
  }

  @Test
  public void testLanguageDetection_WithJavaSourcePath_DetectsCorrectly()
      throws InvalidConfigurationException {
    // when language not given by user, right language based on file ending(s) must be detected
    for (String inputPrograms : languageToInputFile.get(Language.JAVA)) {
      configBuilder.setOption("analysis.programNames", inputPrograms);
      configBuilder.setOption("java.classpath", "src");
      Configuration config = configBuilder.build();
      BootstrapLanguageOptions options = new BootstrapLanguageOptions(config);

      Language detectedLanguage = CPAMain.detectFrontendLanguageIfNecessary(options, config);

      assertThat(detectedLanguage).isEqualTo(Language.JAVA);
    }
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testLanguageDetection_GivenByUser_IsNotOverwritten()
      throws InvalidConfigurationException {
    // detection of language declared by user
    String declLanguage = language.name();

    for (Language fileLanguage : Language.values()) {
      for (String file : languageToInputFile.get(fileLanguage)) {
        configBuilder.setOption("language", declLanguage);
        configBuilder.setOption("analysis.programNames", file);
        Configuration config = configBuilder.build();

        BootstrapLanguageOptions options = new BootstrapLanguageOptions(config);
        Language detectedLanguage = CPAMain.detectFrontendLanguageIfNecessary(options, config);

        assertThat(detectedLanguage).isEqualTo(language);
      }
    }
  }

  @Test(expected = InvalidConfigurationException.class)
  public void testLanguageDetection_MultipleLanguagesGiven_Fails()
      throws InvalidConfigurationException {
    // detection of mixed file languages should throw an Invalid Configuration Exception
    configBuilder.setOption(
        "analysis.programNames", "test.c, test.i, test.h, test.java, test.ll, test.bc");
    Configuration config = configBuilder.build();

    BootstrapLanguageOptions options = new BootstrapLanguageOptions(config);

    CPAMain.detectFrontendLanguageIfNecessary(options, config);
    assert_().fail();
  }

  @Test
  public void testHasPortfolioOptionConfigured_NoPortfolioOptions_ReturnsFalse()
      throws InvalidConfigurationException {
    Configuration config = Configuration.builder().build();
    assertThat(CPAMain.hasPortfolioOptionConfigured(config)).isFalse();
  }

  @Test
  public void testHasPortfolioOptionConfigured_RestartAlgorithm_ReturnsTrue()
      throws InvalidConfigurationException {
    Configuration config =
        Configuration.builder().setOption("restartAlgorithm.configFiles", "a.properties").build();
    assertThat(CPAMain.hasPortfolioOptionConfigured(config)).isTrue();
  }

  @Test
  public void testHasPortfolioOptionConfigured_ParallelAlgorithm_ReturnsTrue()
      throws InvalidConfigurationException {
    Configuration config =
        Configuration.builder().setOption("parallelAlgorithm.configFiles", "a.properties").build();
    assertThat(CPAMain.hasPortfolioOptionConfigured(config)).isTrue();
  }

  @Test
  public void testHasPortfolioOptionConfigured_MpiAlgorithm_ReturnsTrue()
      throws InvalidConfigurationException {
    Configuration config =
        Configuration.builder().setOption("mpiAlgorithm.configFiles", "a.properties").build();
    assertThat(CPAMain.hasPortfolioOptionConfigured(config)).isTrue();
  }

  @Test
  public void testHasPortfolioOptionConfigured_CompositionAlgorithm_ReturnsTrue()
      throws InvalidConfigurationException {
    Configuration config =
        Configuration.builder()
            .setOption("compositionAlgorithm.configFiles", "a.properties")
            .build();
    assertThat(CPAMain.hasPortfolioOptionConfigured(config)).isTrue();
  }

  @Test
  public void testHasPortfolioOptionConfigured_HeuristicSelection_ReturnsTrue()
      throws InvalidConfigurationException {
    Configuration config =
        Configuration.builder().setOption("heuristicSelection.loopConfig", "a.properties").build();
    assertThat(CPAMain.hasPortfolioOptionConfigured(config)).isTrue();
  }

  @Test
  public void testSpecificationMismatch_NullCmdLineSpec_ReturnsFalse() throws IOException {
    assertThat(CPAMain.specificationMismatch(null, "spec/x.spc", tempFolder.getRoot().toPath()))
        .isFalse();
  }

  @Test
  public void testSpecificationMismatch_SameFile_SameRelativeSpelling_ReturnsFalse()
      throws IOException {
    Path specFile = tempFolder.newFile("x.spc").toPath();
    assertThat(
            CPAMain.specificationMismatch(
                specFile.toString(), specFile.toString(), tempFolder.getRoot().toPath()))
        .isFalse();
  }

  @Test
  public void testSpecificationMismatch_SameFile_DifferentRelativeSpelling_ReturnsFalse()
      throws IOException {
    File specDir = tempFolder.newFolder("specification");
    File specFile = new File(specDir, "x.spc");
    assertThat(specFile.createNewFile()).isTrue();
    File configDir = tempFolder.newFolder("components");

    assertThat(
            CPAMain.specificationMismatch(
                specFile.getCanonicalPath(), "../specification/x.spc", configDir.toPath()))
        .isFalse();
  }

  @Test
  public void testSpecificationMismatch_DifferentFiles_ReturnsTrue() throws IOException {
    Path specFileA = tempFolder.newFile("a.spc").toPath();
    Path specFileB = tempFolder.newFile("b.spc").toPath();
    assertThat(
            CPAMain.specificationMismatch(
                specFileA.toString(), specFileB.toString(), tempFolder.getRoot().toPath()))
        .isTrue();
  }

  @Test
  public void testSpecificationMismatch_MultipleFiles_SameSetDifferentOrder_ReturnsFalse()
      throws IOException {
    Path specFileA = tempFolder.newFile("a.spc").toPath();
    Path specFileB = tempFolder.newFile("b.spc").toPath();
    assertThat(
            CPAMain.specificationMismatch(
                specFileA + "," + specFileB,
                specFileB + "," + specFileA,
                tempFolder.getRoot().toPath()))
        .isFalse();
  }

  @Test
  public void testSpecificationMismatch_NonExistentFile_ReturnsFalse() throws IOException {
    Path specFileA = tempFolder.newFile("a.spc").toPath();
    Path nonExistent = tempFolder.getRoot().toPath().resolve("does-not-exist.spc");
    assertThat(
            CPAMain.specificationMismatch(
                specFileA.toString(), nonExistent.toString(), tempFolder.getRoot().toPath()))
        .isFalse();
  }

  private Path writeSpecFile(String name) throws IOException {
    return tempFolder.newFile(name).toPath();
  }

  @Test
  public void testSpecMismatchMessage_ExplicitSpecContradictsConfig_ReturnsMessage()
      throws IOException, InvalidConfigurationException {
    Path specA = writeSpecFile("a.spc");
    Path specB = writeSpecFile("b.spc");

    Optional<String> message =
        CPAMain.specificationMismatchMessageIfApplicable(
            specA.toString(),
            specB.toString(),
            tempFolder.getRoot().toPath(),
            /* specBeforeHandlers= */ specA.toString(),
            /* specAfterHandlers= */ specA.toString(),
            Configuration.builder().build(),
            /* propertyFileGiven= */ false,
            /* languageSubconfigConfigured= */ false,
            "my.properties");

    assertThat(message).isPresent();
    assertThat(message.orElseThrow()).contains(specB.toString());
    assertThat(message.orElseThrow()).contains(specA.toString());
  }

  @Test
  public void testSpecMismatchMessage_PortfolioConfigured_Suppressed()
      throws IOException, InvalidConfigurationException {
    Path specA = writeSpecFile("a.spc");
    Path specB = writeSpecFile("b.spc");
    Configuration portfolioConfig =
        Configuration.builder().setOption("restartAlgorithm.configFiles", "x.properties").build();

    Optional<String> message =
        CPAMain.specificationMismatchMessageIfApplicable(
            specA.toString(),
            specB.toString(),
            tempFolder.getRoot().toPath(),
            specA.toString(),
            specA.toString(),
            portfolioConfig,
            /* propertyFileGiven= */ false,
            /* languageSubconfigConfigured= */ false,
            "my.properties");

    assertThat(message).isEmpty();
  }

  @Test
  public void testSpecMismatchMessage_SpecChangedByHandlers_Suppressed()
      throws IOException, InvalidConfigurationException {
    // Simulates a witness/property/language-driven config switch that rewrote `specification`.
    Path specA = writeSpecFile("a.spc");
    Path specB = writeSpecFile("b.spc");
    Path specC = writeSpecFile("c.spc");

    Optional<String> message =
        CPAMain.specificationMismatchMessageIfApplicable(
            specA.toString(),
            specB.toString(),
            tempFolder.getRoot().toPath(),
            /* specBeforeHandlers= */ specA.toString(),
            /* specAfterHandlers= */ specC.toString(), // changed -> special case fired
            Configuration.builder().build(),
            /* propertyFileGiven= */ false,
            /* languageSubconfigConfigured= */ false,
            "my.properties");

    assertThat(message).isEmpty();
  }

  @Test
  public void testSpecMismatchMessage_NoConfigFileSpec_ReturnsEmpty()
      throws IOException, InvalidConfigurationException {
    Optional<String> message =
        CPAMain.specificationMismatchMessageIfApplicable(
            writeSpecFile("a.spc").toString(),
            /* configFileOwnSpec= */ null,
            tempFolder.getRoot().toPath(),
            null,
            null,
            Configuration.builder().build(),
            /* propertyFileGiven= */ false,
            /* languageSubconfigConfigured= */ false,
            "my.properties");

    assertThat(message).isEmpty();
  }

  @Test
  public void testSpecMismatchMessage_PropertyFileGiven_Suppressed()
      throws IOException, InvalidConfigurationException {
    // Simulates a .prp file being given on the command line: handlePropertyOptions() may switch
    // configs for memsafety/overflow/datarace/memcleanup/termination categories, but does so by
    // re-applying the original cmdline options on top of the new config, so the before/after
    // `specification` text diff alone would not catch it (Important Finding #2).
    Path specA = writeSpecFile("a.spc");
    Path specB = writeSpecFile("b.spc");

    Optional<String> message =
        CPAMain.specificationMismatchMessageIfApplicable(
            specA.toString(),
            specB.toString(),
            tempFolder.getRoot().toPath(),
            specA.toString(),
            specA.toString(),
            Configuration.builder().build(),
            /* propertyFileGiven= */ true,
            /* languageSubconfigConfigured= */ false,
            "my.properties");

    assertThat(message).isEmpty();
  }

  @Test
  public void testSpecMismatchMessage_LanguageSubconfigConfigured_Suppressed()
      throws IOException, InvalidConfigurationException {
    // Simulates "c.config"/"java.config"/"llvm.config"/"svlib.config" being set:
    // handleFrontendLanguageOptions() may switch configs for the detected language, again
    // without changing the `specification` text itself (Important Finding #2).
    Path specA = writeSpecFile("a.spc");
    Path specB = writeSpecFile("b.spc");

    Optional<String> message =
        CPAMain.specificationMismatchMessageIfApplicable(
            specA.toString(),
            specB.toString(),
            tempFolder.getRoot().toPath(),
            specA.toString(),
            specA.toString(),
            Configuration.builder().build(),
            /* propertyFileGiven= */ false,
            /* languageSubconfigConfigured= */ true,
            "my.properties");

    assertThat(message).isEmpty();
  }

  @Test
  public void testSpecMismatchMessage_ConfigFileInheritsDefaultSpec_Suppressed()
      throws IOException, InvalidConfigurationException {
    // Most shipped configs (predicateAnalysis.properties, valueAnalysis.properties,
    // bmc-interpolation.properties, ...) don't set `specification` themselves -- they reach
    // specification/default.spc by inheriting it through an #include chain. That should not be
    // reported as a hard-coded, deliberately-distinct specification (Important Finding #4).
    Path specA = writeSpecFile("a.spc");

    Optional<String> message =
        CPAMain.specificationMismatchMessageIfApplicable(
            specA.toString(),
            "specification/default.spc",
            tempFolder.getRoot().toPath(),
            specA.toString(),
            specA.toString(),
            Configuration.builder().build(),
            false,
            false,
            "my.properties");

    assertThat(message).isEmpty();
  }

  @Test
  public void testCreateConfiguration_BareConfigFlag_ResolvesToAbsolutePath_NoCrash()
      throws Exception {
    // Regression guard for Review Focus item 4: a bare convenience flag like "-predicateAnalysis"
    // resolves `configuration.file` to an absolute path before createConfiguration() runs; the
    // configFileOwnSpecDir computation (Path.of(configFile).toAbsolutePath().getParent()) must
    // handle an already-absolute path without error.
    //
    // "-noout" (old-style alias of "--no-output-files") is added, instead of the new-style
    // "--no-output-files", to avoid tripping CmdLineArguments' "mix of old- and new-style
    // arguments" fatal error (which calls System.exit and would kill the test JVM) -- it is old
    // style like "-predicateAnalysis" itself, so no mixing occurs.
    Path program = tempFolder.newFile("test.c").toPath();
    Files.writeString(program, "int main() { return 0; }");
    CPAMain.Config result =
        CPAMain.createConfiguration(
            new String[] {"-predicateAnalysis", "-noout", program.toString()});
    assertThat(result.configuration()).isNotNull();
  }

  @Test
  public void testCreateConfiguration_WitnessAppended_NoCrashAndSpecChanges() throws Exception {
    // Review Focus item 2: appendWitnessToSpecificationOption always changes the specification
    // entry; createConfiguration() must not throw and must not need a mismatch message for this.
    //
    // "--config config/witnessValidation.properties" is required because handleWitnessOptions()
    // needs "witness.validation.violation.config" (or the correctness equivalent) set to resolve
    // a witness of the detected type -- see AutomatonWitnessV2d0ValidationIntegrationTest for the
    // same real-world pattern. The witness content below is a minimal GraphML witness with a
    // "witness-type" data entry, which is the only thing getWitnessType() requires to classify
    // it (full semantic parsing of the witness happens later, outside createConfiguration()).
    Path program = tempFolder.newFile("test.c").toPath();
    Files.writeString(program, "int main() { return 0; }");
    Path witness = tempFolder.newFile("witness.graphml").toPath();
    Files.writeString(
        witness,
        "<?xml version=\"1.0\"?><graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\">"
            + "<graph edgedefault=\"directed\">"
            + "<data key=\"witness-type\">violation_witness</data>"
            + "</graph></graphml>");
    CPAMain.Config result =
        CPAMain.createConfiguration(
            new String[] {
              "--witness",
              witness.toString(),
              "--config",
              "config/witnessValidation.properties",
              "--no-output-files",
              program.toString()
            });
    assertThat(result.configuration()).isNotNull();
  }
}
