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
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;

public class InputRejectionsTest {

  /**
   * Tests for pInputFilePath if it throws an {@link RuntimeException} with the message
   * pInputRejection in it.
   */
  private void testExpectedRejection(Path pInputFilePath, String pInputRejection) throws Exception {
    // create cfa for test program pFileName
    LogManager logger = LogManager.createTestLogManager();
    CFACreator creator =
        new CFACreator(Configuration.builder().build(), logger, ShutdownNotifier.createDummy());
    String program = Files.readString(pInputFilePath);
    CFA inputCfa = creator.parseSourceAndCreateCFA(program);
    // test if MPORAlgorithm rejects program with correct pInputRejection
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> MPORAlgorithm.testInstance(logger, inputCfa));
    assertThat(exception.getMessage().contains(pInputRejection)).isTrue();
  }

  @Test
  public void testRejectLanguageNotC() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor_seq/input_rejections/HelloJava.java");
    testExpectedRejection(inputFilePath, InputRejections.LANGUAGE_NOT_C);
  }

  @Test
  public void testRejectNotParallel() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor_seq/input_rejections/relax-1.i");
    testExpectedRejection(inputFilePath, InputRejections.NOT_PARALLEL);
  }

  // TODO the pthread_create call is nested inside binary expression(s) -> need to handle
  @Test
  public void testRejectPthreadReturnValue() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor_seq/input_rejections/twostage_3.i");
    testExpectedRejection(inputFilePath, InputRejections.PTHREAD_RETURN_VALUE);
  }

  @Test
  public void testRejectUnsupportedFunction() throws Exception {
    // this program uses pthread_cond_wait and pthread_cond_signal
    Path inputFilePath =
        Path.of("./test/programs/mpor_seq/input_rejections/unsupported_function.i");
    testExpectedRejection(inputFilePath, InputRejections.UNSUPPORTED_FUNCTION);
  }

  @Test
  public void testRejectPthreadCreateLoop() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor_seq/input_rejections/pthread_create_loop.i");
    testExpectedRejection(inputFilePath, InputRejections.PTHREAD_CREATE_LOOP);
  }

  @Test
  public void testRejectDirectRecursion() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor_seq/input_rejections/direct_recursion.i");
    testExpectedRejection(inputFilePath, InputRejections.RECURSIVE_FUNCTION);
  }

  @Test
  public void testRejectIndirectRecursion() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor_seq/input_rejections/indirect_recursion.i");
    testExpectedRejection(inputFilePath, InputRejections.RECURSIVE_FUNCTION);
  }
}
