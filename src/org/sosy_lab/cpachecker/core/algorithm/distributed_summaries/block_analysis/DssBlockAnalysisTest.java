// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.Collection;
import org.junit.Test;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.TestUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.ImportDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class DssBlockAnalysisTest {

  @Test
  public void test() throws Exception {

    var test = getAnalysisResponse("doc/examples/example.c", "output/block_analysis", "L2", 40);

    fail("Not yet implemented");
  }

  public record Responses(
      Collection<DssMessage> postAnalysis, Collection<DssMessage> violationAnalysis) {}

  public static Responses getAnalysisResponse(
      String program, String blockAnalysisFolder, String blockID, int lastMessageNumber)
      throws Exception {
    CFA cfa = TestUtil.buildTestCFA(program);
    BlockNode node = getBlockNode(cfa, blockAnalysisFolder, blockID);
    DssBlockAnalysis analysis = createAnalysis(cfa, node);

    DssViolationConditionMessage lastViolation = null;
    DssPostConditionMessage lastPrecondition = null;
    for(int i = 0; i <= lastMessageNumber; i++) {
      DssMessage curr =
          DssMessage.fromJson(Path.of(blockAnalysisFolder, "messages", "M" + i + ".json"));

      if (curr instanceof DssPostConditionMessage p
          && node.getPredecessorIds().contains(p.getSenderId())) {
        analysis.storePrecondition(p);
        lastPrecondition = p;
      } else if (curr instanceof DssViolationConditionMessage v
          && node.getSuccessorIds().contains(v.getSenderId())) {
        analysis.storeViolationCondition(v);
        lastViolation = v;
      }
    }

    Collection<DssMessage> postAnalysis;
    if (lastPrecondition == null) {
      postAnalysis = ImmutableList.of();
    } else {
      postAnalysis = analysis.analyzePrecondition(lastPrecondition.getSenderId());
    }

    Collection<DssMessage> violationAnalysis;
    if (lastViolation == null) {
      violationAnalysis = ImmutableList.of();
    } else {
      violationAnalysis = analysis.analyzeViolationCondition(lastViolation.getSenderId());
    }

    return new Responses(postAnalysis, violationAnalysis);
  }

  private static DssBlockAnalysis createAnalysis(CFA pCfa, BlockNode pNode) throws Exception {

    Configuration config =
        TestDataTools.configurationForTest()
            .loadFromFile(TestUtil.DSS_FORWARD_CONFIGURATION_FILE)
            .build();
    DssAnalysisOptions options = new DssAnalysisOptions(config);
    DssMessageFactory messageFactory = new DssMessageFactory(options);

    LogManager log = LogManager.createTestLogManager();
    ShutdownManager shut = ShutdownManager.create();

    Specification spec =
        Specification.fromFiles(
            ImmutableList.of(Path.of("config/specification/default.spc")),
            pCfa,
            config,
            log,
            ShutdownNotifier.createDummy());

    return new DssBlockAnalysis(log, pNode, pCfa, spec, config, options, messageFactory, shut);
  }

  private static BlockNode getBlockNode(CFA pCfa, String pBlockAnalysisFolder, String pBlockID)
      throws Exception {
    ImportDecomposition decomp =
        new ImportDecomposition(Path.of(pBlockAnalysisFolder, "blocks.json"));
    BlockGraph graph = decomp.decompose(pCfa);

    BlockNode blockNode =
        graph.getNodes().stream()
            .filter(b -> b.getId().equals(pBlockID))
            .findAny()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No block with id '" + pBlockID + "' found in the block graph."));

    return blockNode;
  }


}
