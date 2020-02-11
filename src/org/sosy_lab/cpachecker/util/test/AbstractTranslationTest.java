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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sosy_lab.common.ProcessExecutor;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.ConsoleLogFormatter;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.StringBuildingLogHandler;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * This abstract class serves as a base for writing tests that check various approaches for
 * translating ARGs, CFAs, etc. into other formats such as automata or C programs
 */
@Ignore("prevent this abstract class being executed as testcase by ant")
@RunWith(Parameterized.class)
public abstract class AbstractTranslationTest {
  public static final String TEST_DIR_PATH = "test/programs/programtranslation/";

  /** Compiler executable to use in tests. */
  private static final String COMPILER = "gcc";
  /** Compile parameter that tells gcc/clang to not perform linking. */
  private static final String PARAM_NO_LINKING = "-c";

  protected String filePrefix = "tmp";
  protected final LogManager logger;

  public AbstractTranslationTest() {
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

  /**
   * Checks the verdict of the configuration run on the given program.
   * Fails with an AssertionError if the computed verdict is different from the
   * given, expected verdict.
   *
   * @param config config to run
   * @param program program to run config on
   * @param expectedVerdict expected verdict
   */
  protected static void check(Configuration config, Path program, boolean expectedVerdict) throws Exception {
    TestResults results = run0(config, program);
    if (expectedVerdict) {
      results.assertIsSafe();
    } else {
      results.assertIsUnsafe();
    }
  }

  /**
   * Checks that the given program can be parsed by CPAchecker. Fails with an AssertionError if the
   * given program can not be parsed by CPAchecker.
   *
   * @param program program to parse
   */
  protected static void checkProgramValid(final Path program)
      throws InterruptedException, InvalidConfigurationException {
    final LogManager logger = LogManager.createTestLogManager();
    final ShutdownNotifier shutdown = ShutdownNotifier.createDummy();
    final Configuration parseConfig =
        Configuration.builder().setOption("analysis.useLoopStructure", "false").build();
    final CFACreator cfaCreator = new CFACreator(parseConfig, logger, shutdown);

    try {
      cfaCreator.parseFileAndCreateCFA(Lists.newArrayList(program.toString()));

    } catch (IOException | ParserException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * Checks that the given program can be compiled. Linking is not attempted, so programs may
   * contain undefined functions. Fails with an AssertionError if the compilation fails.
   *
   * @param program program to compile
   */
  protected static void checkProgramCompilable(final Path program)
      throws IOException, InterruptedException {
    final LogManager logger = LogManager.createTestLogManager();
    final List<String> compileCommandList =
        ImmutableList.of(COMPILER, PARAM_NO_LINKING, "-o", "/dev/null", program.toString());
    final String[] compileCommand = compileCommandList.toArray(new String[0]);

    final CompilerExecutor exec = new CompilerExecutor(logger, compileCommand);
    final int returnCode = exec.join();

    assertThat(returnCode).isEqualTo(0);
  }

  private static class CompilerExecutor extends ProcessExecutor<IOException> {

    public CompilerExecutor(LogManager pLogger, String... cmd) throws IOException {
      super(pLogger, IOException.class, cmd);
    }
  }
}
