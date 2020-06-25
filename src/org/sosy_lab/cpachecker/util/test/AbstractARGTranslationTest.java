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
package org.sosy_lab.cpachecker.util.test;

import static com.google.common.truth.Truth.assert_;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.ConsoleLogFormatter;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.StringBuildingLogHandler;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

/**
 * This abstract class serves as a base for writing tests that check various approaches for
 * translating ARGs into other formats such as automata or C programs
 */
@Ignore("prevent this abstract class being executed as testcase by ant")
@RunWith(Parameterized.class)
public abstract class AbstractARGTranslationTest {
  public static final String TEST_DIR_PATH = "test/programs/programtranslation/";
  protected String filePrefix = "tmp";
  protected final LogManager logger;

  public AbstractARGTranslationTest() {
    StringBuildingLogHandler stringLogHandler = new StringBuildingLogHandler();
    stringLogHandler.setLevel(Level.ALL);
    stringLogHandler.setFormatter(ConsoleLogFormatter.withoutColors());
    logger = BasicLogManager.createWithHandler(stringLogHandler);
  }

  protected Path newTempFile() throws IOException {
    return TempFile.builder().prefix(filePrefix).suffix(".spc").create().toAbsolutePath();
  }

  protected static TestResults run0(Configuration config, Path program) throws Exception {
    TestResults results;
    try {
      results = CPATestRunner.run(config, program.toString());
    } catch (NoClassDefFoundError | UnsatisfiedLinkError e) {
      throw new AssertionError(e);
    }
    return results;
  }

  protected static ARGState run(Configuration config, Path program) throws Exception {
    TestResults results = run0(config, program);
    UnmodifiableReachedSet reached = results.getCheckerResult().getReached();
    assert_()
        .withMessage(
            "reached set: %s\nlog: %s\nfirst state of reached set", reached, results.getLog())
        .that(reached.getFirstState())
        .isNotNull();
    return (ARGState) reached.getFirstState();
  }

}
