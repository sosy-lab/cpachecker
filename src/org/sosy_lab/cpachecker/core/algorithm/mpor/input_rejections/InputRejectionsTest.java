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

  private void testExpectedRejection(Path pInputFilePath, String pInputRejection) throws Exception {
    // create cfa for test program pFileName
    LogManager logger = LogManager.createTestLogManager();
    CFACreator creator =
        new CFACreator(Configuration.builder().build(), logger, ShutdownNotifier.createDummy());
    String program = Files.readString(pInputFilePath);
    CFA inputCfa = creator.parseSourceAndCreateCFA(program);

    // create seq with mpor algorithm
    MPORAlgorithm algorithm = MPORAlgorithm.testInstance(logger, inputCfa);
    Exception exception =
        assertThrows(
            RuntimeException.class, () -> algorithm.buildSequentialization("mpor_seq__test.i"));
    assertThat(exception.getMessage().contains(pInputRejection)).isTrue();
  }

  @Test
  public void testRejectDirectRecursion() throws Exception {
    Path inputFilePath = Path.of("./test/programs/mpor_seq/input_rejections/direction_recursion.i");
    testExpectedRejection(inputFilePath, InputRejections.RECURSIVE_FUNCTION);
  }

  @Test
  public void testRejectIndirectRecursion() throws Exception {
    Path inputFilePath =
        Path.of("./test/programs/mpor_seq/input_rejections/indirection_recursion.i");
    testExpectedRejection(inputFilePath, InputRejections.RECURSIVE_FUNCTION);
  }
}
