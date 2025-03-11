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
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection.InputRejectionMessage;
import org.sosy_lab.cpachecker.exceptions.CParserException;

public class InputRejectionTest {

  /**
   * Tests for pInputFilePath if it throws an {@link RuntimeException} with the message
   * pErrorMessage in it.
   */
  private <T extends Throwable> void testExpectedRejection(
      Path pInputFilePath, Class<T> pExpectedThrowable, InputRejectionMessage pExpected)
      throws Exception {

    // create cfa for test program pFileName
    LogManager logger = LogManager.createTestLogManager();
    CFACreator creatorWithPreProcessor =
        new CFACreator(
            Configuration.builder().setOption("parser.usePreprocessor", "true").build(),
            logger,
            ShutdownNotifier.createDummy());
    CFA inputCfa =
        creatorWithPreProcessor.parseFileAndCreateCFA(ImmutableList.of(pInputFilePath.toString()));

    // test if MPORAlgorithm rejects program with correct throwable and pErrorMessage
    MPOROptions options =
        MPOROptions.testInstance(false, false, false, false, false, false, false, false);
    T throwable =
        assertThrows(
            pExpectedThrowable, () -> MPORAlgorithm.testInstance(options, logger, inputCfa));
    assertThat(pExpectedThrowable.isInstance(throwable)).isTrue();
    assertThat(throwable.getMessage().contains(pExpected.message)).isTrue();
  }

  @Test
  public void testRejectLanguageNotC() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor_seq/input_rejections/HelloJava.java");
    // create cfa for test program pFileName
    LogManager logger = LogManager.createTestLogManager();
    CFACreator creator =
        new CFACreator(Configuration.builder().build(), logger, ShutdownNotifier.createDummy());
    String program = Files.readString(inputFilePath);
    CParserException exception =
        assertThrows(CParserException.class, () -> creator.parseSourceAndCreateCFA(program));
    assertThat(exception != null).isTrue();
  }

  @Test
  public void testRejectNotParallel() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor_seq/input_rejections/relax-1.c");
    testExpectedRejection(
        inputFilePath, RuntimeException.class, InputRejectionMessage.NOT_CONCURRENT);
  }

  @Test
  public void testRejectUnsupportedFunction() throws Exception {
    // this program uses pthread_cond_wait and pthread_cond_signal
    Path inputFilePath = Path.of("./test/programs/mpor_seq/input_rejections/sync01.c");
    testExpectedRejection(
        inputFilePath, RuntimeException.class, InputRejectionMessage.UNSUPPORTED_FUNCTION);
  }

  @Test
  public void testRejectPthreadArrayIdentifiers() throws Exception {
    Path inputFilePath =
        Path.of("./test/programs/mpor_seq/input_rejections/indexer-no-pthread-exit.c");
    testExpectedRejection(
        inputFilePath, RuntimeException.class, InputRejectionMessage.NO_PTHREAD_OBJECT_ARRAYS);
  }

  // TODO also create a test for pthread_create(...) != 0

  // TODO the pthread_create call is nested inside binary expression(s) -> need to handle
  @Ignore
  @Test
  public void testRejectPthreadReturnValue() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor_seq/input_rejections/twostage_3.c");
    testExpectedRejection(
        inputFilePath, RuntimeException.class, InputRejectionMessage.PTHREAD_RETURN_VALUE);
  }

  @Test
  public void testRejectPthreadCreateLoop() throws Exception {
    Path inputFilePath =
        Path.of("./test/programs/mpor_seq/input_rejections/queue_longest-pthread-create-loop.c");
    testExpectedRejection(
        inputFilePath, RuntimeException.class, InputRejectionMessage.PTHREAD_CREATE_LOOP);
  }

  @Test
  public void testRejectDirectRecursion() throws Exception {
    Path inputFilePath =
        Path.of("./test/programs/mpor_seq/input_rejections/queue_longest-direct-recursion.c");
    testExpectedRejection(
        inputFilePath, RuntimeException.class, InputRejectionMessage.RECURSIVE_FUNCTION);
  }

  @Test
  public void testRejectIndirectRecursion() throws Exception {
    Path inputFilePath =
        Path.of("./test/programs/mpor_seq/input_rejections/queue_longest-indirect-recursion.c");
    testExpectedRejection(
        inputFilePath, RuntimeException.class, InputRejectionMessage.RECURSIVE_FUNCTION);
  }
}
