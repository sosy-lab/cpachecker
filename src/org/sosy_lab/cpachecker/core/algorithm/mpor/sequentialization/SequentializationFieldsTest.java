// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

/**
 * Tests if {@link SequentializationFields} are expected depending on the input program, e.g. number
 * of threads.
 */
public class SequentializationFieldsTest {

  @Test
  public void test_13_privatized_04_priv_multi_true() throws Exception {
    // this program contains multiple loops whose condition only contains local variables
    Path path =
        Path.of("./test/programs/mpor/sequentialization/13-privatized_04-priv_multi_true.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.defaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads == 4).isTrue();
    assertThat(fields.memoryModel.isPresent()).isTrue();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    assertThat(memoryModel.getRelevantMemoryLocationAmount() == 4).isTrue();
  }

  private SequentializationFields getSequentializationFields(
      Path pInputFilePath, MPOROptions pOptions) throws Exception {

    // create cfa for test program pFileName
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    CFACreator creatorWithPreProcessor =
        new CFACreator(
            Configuration.builder().setOption("parser.usePreprocessor", "true").build(),
            logger,
            shutdownNotifier);
    CFA inputCfa =
        creatorWithPreProcessor.parseFileAndCreateCFA(ImmutableList.of(pInputFilePath.toString()));

    // create mpor algorithm and generate seq
    MPORAlgorithm algorithm = MPORAlgorithm.testInstance(logger, inputCfa, pOptions);
    String inputFileName = "test.i";
    Sequentialization sequentialization =
        algorithm.buildSequentialization(inputFileName, SeqToken.__MPOR_SEQ__ + inputFileName);
    return sequentialization.buildFields();
  }
}
