// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejections;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

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
import org.sosy_lab.cpachecker.exceptions.CParserException;

public class InputRejectionsTest {

  /**
   * Tests for pInputFilePath if it throws an {@link RuntimeException} with the message
   * pErrorMessage in it.
   */
  private <T extends Throwable> void testExpectedRejection(
      Path pInputFilePath, Class<T> pExpectedThrowable, String pExpectedMessage) throws Exception {

    // create cfa for test program pFileName
    LogManager logger = LogManager.createTestLogManager();
    CFACreator creator =
        new CFACreator(Configuration.builder().build(), logger, ShutdownNotifier.createDummy());
    String program = Files.readString(pInputFilePath);
    CFA inputCfa = creator.parseSourceAndCreateCFA(program);

    // test if MPORAlgorithm rejects program with correct throwable and pErrorMessage
    T throwable =
        assertThrows(pExpectedThrowable, () -> MPORAlgorithm.testInstance(logger, inputCfa, true));
    assertThat(pExpectedThrowable.isInstance(throwable)).isTrue();
    assertThat(throwable.getMessage().contains(pExpectedMessage)).isTrue();
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
    Path inputFilePath = Path.of("./test/programs/mpor_seq/input_rejections/relax-1.i");
    testExpectedRejection(inputFilePath, RuntimeException.class, InputRejections.NOT_PARALLEL);
  }

  @Test
  public void testRejectUnsupportedFunction() throws Exception {
    // this program uses pthread_cond_wait and pthread_cond_signal
    Path inputFilePath = Path.of("./test/programs/mpor_seq/input_rejections/sync01.i");
    testExpectedRejection(
        inputFilePath, RuntimeException.class, InputRejections.UNSUPPORTED_FUNCTION);
  }

  @Test
  public void testRejectPthreadArrayIdentifiers() throws Exception {
    Path inputFilePath =
        Path.of("./test/programs/mpor_seq/input_rejections/indexer-no-pthread-exit.i");
    testExpectedRejection(
        inputFilePath, RuntimeException.class, InputRejections.NO_PTHREAD_OBJECT_ARRAYS);
  }

  // TODO also create a test for pthread_create(...) != 0

  // TODO the pthread_create call is nested inside binary expression(s) -> need to handle
  @Ignore
  @Test
  public void testRejectPthreadReturnValue() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor_seq/input_rejections/twostage_3.i");
    testExpectedRejection(
        inputFilePath, RuntimeException.class, InputRejections.PTHREAD_RETURN_VALUE);
  }

  @Test
  public void testRejectPthreadCreateLoop() throws Exception {
    Path inputFilePath =
        Path.of("./test/programs/mpor_seq/input_rejections/queue_longest-pthread-create-loop.i");
    testExpectedRejection(
        inputFilePath, RuntimeException.class, InputRejections.PTHREAD_CREATE_LOOP);
  }

  @Test
  public void testRejectDirectRecursion() throws Exception {
    Path inputFilePath =
        Path.of("./test/programs/mpor_seq/input_rejections/queue_longest-direct-recursion.i");
    testExpectedRejection(
        inputFilePath, RuntimeException.class, InputRejections.RECURSIVE_FUNCTION);
  }

  @Test
  public void testRejectIndirectRecursion() throws Exception {
    Path inputFilePath =
        Path.of("./test/programs/mpor_seq/input_rejections/queue_longest-indirect-recursion.i");
    testExpectedRejection(
        inputFilePath, RuntimeException.class, InputRejections.RECURSIVE_FUNCTION);
  }
}
