// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection.InputRejectionMessage;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class InputRejectionTest {

  /**
   * Tests if {@link MPORAlgorithm} throws a {@link UnsupportedCodeException} when invoked with the
   * program in {@code pInputFilePath}.
   */
  private void testExpectedRejection(
      MPOROptions pOptions, Path pInputFilePath, InputRejectionMessage pExpected) throws Exception {

    // create cfa for test program pFileName
    Configuration testConfig = TestDataTools.configurationForTest().build();
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    CFACreator cfaCreator = MPORUtil.buildTestCfaCreatorWithPreprocessor(logger, shutdownNotifier);
    CFA inputCfa = cfaCreator.parseFileAndCreateCFA(ImmutableList.of(pInputFilePath.toString()));

    // test if MPORAlgorithm rejects program with correct throwable and pErrorMessage
    UnsupportedCodeException unsupportedCodeException =
        assertThrows(
            UnsupportedCodeException.class, () -> InputRejection.handleRejections(inputCfa));
    assertThat(unsupportedCodeException.getMessage()).contains(pExpected.message);
  }

  @Test
  public void testRejectLanguageNotC() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor/input_rejections/HelloJava.java");
    // create cfa for test program pFileName
    LogManager logger = LogManager.createTestLogManager();
    CFACreator cfaCreator = MPORUtil.buildTestCfaCreator(logger, ShutdownNotifier.createDummy());
    String program = Files.readString(inputFilePath);
    CParserException exception =
        assertThrows(CParserException.class, () -> cfaCreator.parseSourceAndCreateCFA(program));
    assertThat(exception).isNotNull();
  }

  @Test
  public void testRejectNotParallel() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor/input_rejections/sequential-program.c");
    testExpectedRejection(
        MPOROptions.getDefaultTestInstance(), inputFilePath, InputRejectionMessage.NOT_CONCURRENT);
  }

  @Test
  public void testRejectUnsupportedFunction() throws Exception {
    // this program uses pthread_getspecific
    Path inputFilePath =
        Path.of("./test/programs/mpor/input_rejections/unsupported-function-pthread_key_create.c");
    testExpectedRejection(
        MPOROptions.getDefaultTestInstance(),
        inputFilePath,
        InputRejectionMessage.UNSUPPORTED_FUNCTION);
  }

  @Test
  public void testRejectPthreadArrayIdentifiers() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor/input_rejections/pthread_t-array.c");
    testExpectedRejection(
        MPOROptions.getDefaultTestInstance(),
        inputFilePath,
        InputRejectionMessage.NO_PTHREAD_OBJECT_ARRAYS);
  }

  @Ignore
  @Test
  public void testRejectPthreadReturnValue() throws Exception {
    Path inputFilePath =
        Path.of("./test/programs/mpor/input_rejections/pthread-function-return-value.c");
    testExpectedRejection(
        MPOROptions.getDefaultTestInstance(),
        inputFilePath,
        InputRejectionMessage.PTHREAD_RETURN_VALUE);
  }

  @Test
  public void testRejectPthreadCreateLoop() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor/input_rejections/pthread-create-loop.c");
    testExpectedRejection(
        MPOROptions.getDefaultTestInstance(),
        inputFilePath,
        InputRejectionMessage.PTHREAD_CREATE_LOOP);
  }

  @Test
  public void testRejectDirectRecursion() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor/input_rejections/direct-recursion.c");
    testExpectedRejection(
        MPOROptions.getDefaultTestInstance(),
        inputFilePath,
        InputRejectionMessage.RECURSIVE_FUNCTION);
  }

  @Test
  public void testRejectIndirectRecursion() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor/input_rejections/indirect-recursion.c");
    testExpectedRejection(
        MPOROptions.getDefaultTestInstance(),
        inputFilePath,
        InputRejectionMessage.RECURSIVE_FUNCTION);
  }

  @Test
  public void testRejectPointerWrite() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor/input_rejections/pointer-write.c");

    // create test config and MPOROptions instance
    Configuration config =
        TestDataTools.configurationForTest()
            .setOption("analysis.algorithm.MPOR.allowPointerWrites", "false")
            .build();
    MPOROptions customOptions = new MPOROptions(config);

    // create cfa for test program pFileName
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    LogManager logger = LogManager.createTestLogManager();
    CFACreator cfaCreator = MPORUtil.buildTestCfaCreatorWithPreprocessor(logger, shutdownNotifier);
    CFA cfa = cfaCreator.parseFileAndCreateCFA(ImmutableList.of(inputFilePath.toString()));

    SequentializationUtils utils = SequentializationUtils.of(cfa, config, logger, shutdownNotifier);
    // test if MPORAlgorithm rejects program with correct error message
    UnsupportedCodeException throwable =
        assertThrows(
            UnsupportedCodeException.class,
            () -> Sequentialization.tryBuildProgramString(customOptions, cfa, utils));
    String expectedMessage = InputRejectionMessage.POINTER_WRITE.message;
    assertThat(throwable.getMessage()).contains(expectedMessage);
  }
}
