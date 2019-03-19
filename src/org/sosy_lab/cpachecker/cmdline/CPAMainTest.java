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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.junit.Test;
import org.sosy_lab.common.log.LoggingOptions;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cmdline.CPAMain.MainOptions;

public class CPAMainTest {

  Configuration config;
  ConfigurationBuilder configBuilder;
  LoggingOptions logOptions;
  LogManager logManager;
  MainOptions options;


  @Before
  public void setup() throws InvalidConfigurationException {
    // setup config
    configBuilder = Configuration.builder();
    config = configBuilder.build();
    // setup logManager
    logOptions = new LoggingOptions(config);
    logManager = BasicLogManager.create(logOptions);
    config.enableLogging(logManager);
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testLanguageDetection() throws InvalidConfigurationException {
    // when language not given by user, right language based on file ending(s) must be detected
    for (Language language : Language.values()) {

      options = new MainOptions();

      switch(language) {
        case C:
          configBuilder.setOption("analysis.programNames", "test.c, test.i ,test.h");
          break;
        case JAVA:
          configBuilder.setOption("analysis.programNames", "test.java");
          break;
        case LLVM:
          configBuilder.setOption("analysis.programNames", "test.ll, test.bc");
          break;
        default:
          break;
      }

      config = configBuilder.build();
      config.inject(options);

      Configuration newConfig = CPAMain.extractFrontendfromFileending(options, config, logManager);

      assertThat(newConfig.getProperty("language")).contains(language.toString());
   }
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testDeclaredLanguageDetection() throws InvalidConfigurationException {
    // detection of language declared by user
    String declLanguage = "";
    for (Language declaredLanguage : Language.values()) {
      options = new MainOptions();

      for (Language fileLanguage : Language.values()) {
        switch (fileLanguage) {
          case C:
            configBuilder.setOption("analysis.programNames", "test.c, test.i ,test.h");
            break;
          case JAVA:
            configBuilder.setOption("analysis.programNames", "test.java");
            break;
          case LLVM:
            configBuilder.setOption("analysis.programNames", "test.ll, test.bc");
            break;
          default:
            break;
        }

        if(declaredLanguage.toString() == "LLVM IR") {
          declLanguage = "LLVM";
        } else {
          declLanguage = declaredLanguage.toString();
        }

        configBuilder.setOption("language", declLanguage);

        config = configBuilder.build();
        config.inject(options);
        Configuration newConfig = CPAMain.extractFrontendfromFileending(options, config, logManager);

        assertThat(newConfig.getProperty("language")).contains(declLanguage);
      }
    }
  }

  @Test(expected=InvalidConfigurationException.class)
  public void testMultipleLanguagesDetected() throws InvalidConfigurationException {
    // detection of mixed file languages should throw an Invalid Configuration Exception
    options = new MainOptions();
    configBuilder.setOption("analysis.programNames","test.c, test.i, test.h, test.java, test.ll, test.bc");
    config = configBuilder.build();
    config.inject(options);
    CPAMain.extractFrontendfromFileending(options, config, logManager);
    fail();
  }
}
