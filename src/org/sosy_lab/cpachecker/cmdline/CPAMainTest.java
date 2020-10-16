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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cmdline.CPAMain.MainOptions;

@SuppressWarnings("deprecation")
@RunWith(Parameterized.class)
public class CPAMainTest {

  @Parameters(name = "{0}")
  public static Object[] getAllLanguages() {
    return Language.values();
  }

  @Parameter(0)
  public Language language;

  private final LogManager logManager = LogManager.createTestLogManager();
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
  public void setUp() {
    configBuilder = Configuration.builder();
  }

  @Test
  public void testLanguageDetection_BasedOnFileSuffix_DetectsCorrectly()
      throws InvalidConfigurationException {
    // when language not given by user, right language based on file ending(s) must be detected
    for (Language languageToTest : ImmutableList.of(Language.C, Language.LLVM)) {
      for (String inputPrograms : languageToInputFile.get(languageToTest)) {
        configBuilder.setOption("analysis.programNames", inputPrograms);
        Configuration config = configBuilder.build();
        MainOptions options = new MainOptions();
        config.inject(options);

        Configuration newConfig =
            CPAMain.detectFrontendLanguageIfNecessary(options, config, logManager);

        assertThat(newConfig.getProperty("language")).isEqualTo(languageToTest.name());
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
      MainOptions options = new MainOptions();
      config.inject(options);

      Configuration newConfig =
          CPAMain.detectFrontendLanguageIfNecessary(options, config, logManager);

      assertThat(newConfig.getProperty("language")).isEqualTo(Language.JAVA.name());
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
      MainOptions options = new MainOptions();
      config.inject(options);

      Configuration newConfig =
          CPAMain.detectFrontendLanguageIfNecessary(options, config, logManager);

      assertThat(newConfig.getProperty("language")).isEqualTo(Language.JAVA.name());
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

        MainOptions options = new MainOptions();
        config.inject(options);
        Configuration newConfig =
            CPAMain.detectFrontendLanguageIfNecessary(options, config, logManager);

        assertThat(newConfig.getProperty("language")).isEqualTo(declLanguage);
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

    MainOptions options = new MainOptions();
    config.inject(options);

    CPAMain.detectFrontendLanguageIfNecessary(options, config, logManager);
    assert_().fail();
  }
}
