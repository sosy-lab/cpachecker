/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cmdline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import com.google.common.collect.HashMultimap;
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

  private final ConfigurationBuilder configBuilder = Configuration.builder();
  private final LogManager logManager = LogManager.createTestLogManager();
  private final Multimap<Language, String> map = HashMultimap.create();

  @Before
  public void init() {
    map.put(Language.C, "test.c");
    map.put(Language.C, "test.i");
    map.put(Language.C, "test.h");
    map.put(Language.C, "test.c, test.i, test.h");
    map.put(Language.JAVA, "test.java");
    map.put(Language.LLVM, "test.ll");
    map.put(Language.LLVM, "test.bc");
    map.put(Language.LLVM, "test.ll, test.bc");
  }

  @Test
  public void testLanguageDetection() throws InvalidConfigurationException {
    // when language not given by user, right language based on file ending(s) must be detected
    for (Language fileLanguage : Language.values()) {
      for (String file : map.get(fileLanguage)) {
        configBuilder.setOption("analysis.programNames", file);
        Configuration config = configBuilder.build();

        MainOptions options = new MainOptions();
        config.inject(options);
        Configuration newConfig =
            CPAMain.extractFrontendfromFileending(options, config, logManager);

        assertFalse(config.hasProperty("language"));
        assertEquals(fileLanguage.toString(), newConfig.getProperty("language"));
      }
    }
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testDeclaredLanguageDetection() throws InvalidConfigurationException {
    // detection of language declared by user
    String declLanguage = language == Language.LLVM ? "LLVM" : language.toString();

    for (Language fileLanguage : Language.values()) {
      for (String file : map.get(fileLanguage)) {
        configBuilder.setOption("language", declLanguage);
        configBuilder.setOption("analysis.programNames", file);
        Configuration config = configBuilder.build();

        MainOptions options = new MainOptions();
        config.inject(options);
        Configuration newConfig =
            CPAMain.extractFrontendfromFileending(options, config, logManager);

        assertEquals(declLanguage, newConfig.getProperty("language"));
      }
    }
  }

  @Test
  public void testMultipleLanguagesDetected() throws InvalidConfigurationException {
    // user-given language should override detection of mixed file languages
    String declLanguage = language == Language.LLVM ? "LLVM" : language.toString();

    configBuilder.setOption("language", declLanguage);
    configBuilder.setOption(
        "analysis.programNames", "test.c, test.i, test.h, test.java, test.ll, test.bc");
    Configuration config = configBuilder.build();

    MainOptions options = new MainOptions();
    config.inject(options);
    Configuration newConfig = CPAMain.extractFrontendfromFileending(options, config, logManager);

    assertEquals(declLanguage, newConfig.getProperty("language"));
  }

  @Test(expected = InvalidConfigurationException.class)
  public void testMultipleLanguagesDetectedFail() throws InvalidConfigurationException {
    // detection of mixed file languages should throw an Invalid Configuration Exception
    configBuilder.setOption(
        "analysis.programNames", "test.c, test.i, test.h, test.java, test.ll, test.bc");
    Configuration config = configBuilder.build();

    MainOptions options = new MainOptions();
    config.inject(options);

    CPAMain.extractFrontendfromFileending(options, config, logManager);
    fail();
  }
}
