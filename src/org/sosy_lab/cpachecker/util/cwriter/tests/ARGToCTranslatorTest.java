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
 */
package org.sosy_lab.cpachecker.util.cwriter.tests;

import static org.junit.Assert.assertNotNull;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.logging.Level;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.ConsoleLogFormatter;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.StringBuildingLogHandler;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

@RunWith(Parameterized.class)
public class ARGToCTranslatorTest {

  private static final String TEST_DIR_PATH = "test/programs/argtoctranslator/";

  private final ARGToCTranslator translator;
  private final Configuration config;
  private final Configuration reConfig;
  private final Path residualProgramPath;
  private final String verdict;
  private final String program;

  public ARGToCTranslatorTest(String pProgram, String pVerdict, boolean header)
      throws InvalidConfigurationException, IOException {
    program = pProgram;
    verdict = pVerdict;
    residualProgramPath =
        TempFile.builder().prefix("residual").suffix(".c").create().toAbsolutePath();
    config =
        TestDataTools.configurationForTest()
            .loadFromResource(ARGToCTranslatorTest.class, "inline-errorlabel.properties")
            .setOption("cpa.arg.export.code.handleTargetStates", "VERIFIERERROR")
            .setOption("cpa.arg.export.code.header", Boolean.toString(header).toLowerCase())
            .build();
    reConfig =
        TestDataTools.configurationForTest()
            .loadFromResource(ARGToCTranslatorTest.class, "predicateAnalysis.properties")
            .setOption("parser.usePreprocessor", "true")
            .build();
    StringBuildingLogHandler stringLogHandler = new StringBuildingLogHandler();
    stringLogHandler.setLevel(Level.ALL);
    stringLogHandler.setFormatter(ConsoleLogFormatter.withoutColors());
    LogManager logger = BasicLogManager.createWithHandler(stringLogHandler);
    translator = new ARGToCTranslator(logger, config);
  }

  @Parameters(name = "{0}:{1}:{2}")
  public static Collection<Object[]> data() {
    ImmutableList.Builder<Object[]> b = ImmutableList.builder();
    b.add(new Object[] {"main.c", "true", false});
    b.add(new Object[] {"main2.c", "false", false});
    b.add(new Object[] {"functionreturn.c", "false", false});
    b.add(new Object[] {"main.c", "true", true});
    b.add(new Object[] {"main2.c", "false", true});
    b.add(new Object[] {"functionreturn.c", "false", true});
    return b.build();
  }

  @Test
  public void testSimple() throws Exception {
    String fullPath = Paths.get(TEST_DIR_PATH, program).toString();

    // generate C program:
    TestResults results = null;
    try {
      results = CPATestRunner.run(config, fullPath);
    } catch (NoClassDefFoundError | UnsatisfiedLinkError e) {
      throw new AssertionError(e);
    }
    assertNotNull(results);
    UnmodifiableReachedSet reached = results.getCheckerResult().getReached();
    assertNotNull(reached.getFirstState());
    String res = translator.translateARG((ARGState) reached.getFirstState(), false);
    Files.write(residualProgramPath, res.getBytes("utf-8"));

    // test whether C program still gives correct verdict:
    results = null;
    try {
      results = CPATestRunner.run(reConfig, residualProgramPath.toString());
    } catch (NoClassDefFoundError | UnsatisfiedLinkError e) {
      throw new AssertionError(e);
    }
    assertNotNull(results);
    if (verdict.equals("true")) {
      results.assertIsSafe();
    } else {
      results.assertIsUnsafe();
    }
  }
}
