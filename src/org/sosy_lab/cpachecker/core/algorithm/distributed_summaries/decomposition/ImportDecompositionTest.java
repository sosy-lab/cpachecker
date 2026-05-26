// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.Test;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.ImportedBlock;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class ImportDecompositionTest {

  private static final String PROGRAM = "doc/examples/example.c";

  private Map<String, ImportedBlock> getExportDataFrom(CFA pCfa)
      throws IOException, InvalidConfigurationException, InterruptedException {

    ConfigurationBuilder decompositionOptions =
        TestDataTools.configurationForTest()
            .setOption(
                "distributedSummaries.decomposition.decompositionType", "MERGE_DECOMPOSITION");

    for (String enable : ImmutableList.of("alwaysAtJoin", "alwaysAtBranch")) {
      decompositionOptions.setOption("cpa.predicate.blk" + "." + enable, "true");
    }

    DssBlockDecomposition configuredDecomposition =
        new DssDecompositionOptions(decompositionOptions.build(), pCfa)
            .getConfiguredDecomposition();

    // serialize and deserialize the block graph
    Map<String, Map<String, Object>> exportData =
        configuredDecomposition.decompose(pCfa).getExportData(pCfa);

    StringBuilder appender = new StringBuilder();
    JSON.writeJSONString(exportData, appender);
    ObjectMapper objectMapper = new ObjectMapper();

    return objectMapper.readValue(appender.toString(), new TypeReference<>() {});
  }

  /**
   * Tests that {@link ImportDecomposition} can decompose a {@link CFA} to a {@link BlockGraph} when
   * the {@link CFA} starts with a non-zero node ID.
   */
  @Test
  public void testCanDecomposeCfaWithNodeIdThatStartsAtNonZero() throws Exception {
    String programText = Files.readString(Path.of(PROGRAM), StandardCharsets.UTF_8);

    // read the same CFA twice (with different ids)
    CFA originalCFA = TestDataTools.makeCFA(programText);
    CFA shiftedCFA = TestDataTools.makeCFA(programText);

    // If the CFAs have the same nodes, then they were not shifted and this test is not valid
    assertThat(originalCFA.nodes()).isNotEmpty();
    assertThat(originalCFA.nodes()).containsNoneIn(shiftedCFA.nodes());

    // check whether the imported and the original block graph are the same.
    ImportDecomposition decomposition = new ImportDecomposition(getExportDataFrom(originalCFA));
    BlockGraph blockGraphWithOriginalCFA = decomposition.decompose(originalCFA);
    BlockGraph blockGraphWithShiftedCFA = decomposition.decompose(shiftedCFA);

    assertThat(blockGraphWithShiftedCFA).isEqualTo(blockGraphWithOriginalCFA);
  }

  @Test
  public void testValidImportDecomposition() throws Exception {
    String programText = Files.readString(Path.of(PROGRAM), StandardCharsets.UTF_8);
    CFA originalCFA = TestDataTools.makeCFA(programText);

    ImportDecomposition decomposition = new ImportDecomposition(getExportDataFrom(originalCFA));
    BlockGraph graph = decomposition.decompose(originalCFA);

    DecompositionTestBase.checkBlockGraph(graph, originalCFA);
  }
}
