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
import com.google.common.truth.Truth;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraphModification;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraphModification.Modification;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

@RunWith(Parameterized.class)
public class DssBlockAnalysisTest {

  @Parameters(name = "{2} in {0} with ({1} until M{3}.json)")
  public static List<Object[]> getParameters() {
    return ImmutableList.of(
        new Object[] {
          "doc/examples/example.c", "output/block_analysis", "L2", 40, new Responses(null, null)
        });
  }

  @Parameter(0)
  public String program;

  @Parameter(1)
  public String folder;

  @Parameter(2)
  public String blockID;

  @Parameter(3)
  public int lastMessage;

  // TODO load from files as well?
  @Parameter(4)
  public Responses expected;

  @Test
  public void test() throws Exception {

    ReplayResult result = getAnalysisResult(program, folder, blockID, lastMessage);
    Responses rs = result.responses;

    if(expected.postAnalysis != null) {
      expected.postAnalysis.forEach(
          exp ->
              Truth.assertWithMessage(
                      "Expected postcondition analysis to cover message %s, but got %s",
                      exp, rs.postAnalysis)
                  .that(rs.postAnalysis.stream().anyMatch(act -> covers(exp, act, result.dcpa)))
                  .isTrue());
    }

    if (expected.violationAnalysis != null) {
      expected.violationAnalysis.forEach(
          exp ->
              Truth.assertWithMessage(
                      "Expected violation analysis to cover message %s, but got %s",
                      exp, rs.violationAnalysis)
                  .that(rs.violationAnalysis.stream().anyMatch(act -> covers(exp, act, result.dcpa)))
                  .isTrue());
    }

    fail("Not yet implemented");
  }

  private boolean covers(DssMessage pExp, DssMessage pAct, DistributedConfigurableProgramAnalysis dcpa) {
    // TODO how to do this?? I do not want to enforce everything to be the same!
    return true;
  }

  public record Responses(
      Collection<DssMessage> postAnalysis, Collection<DssMessage> violationAnalysis) {}

  public record ReplayResult(
      Responses responses, DistributedConfigurableProgramAnalysis dcpa) {}
  /**
   * repeats the analysis for a given block with given received messages
   *
   * <p>How to create a usable folder:
   *
   * <p>cpachecker --dss --option distributedSummaries.debug=true <file>
   *
   * <p>cpachecker --dss --option distributedSummaries.decomposition.generateBlockGraphOnly=true
   * <file>
   *
   * <p>The second part is needed because debug exports the blocks after modifying the graph, which
   * means we can not import it.
   *
   * @param program the program that is analyzed
   * @param blockAnalysisFolder the folder where the debug information is stored
   * @param blockID the block for which to run the analysis
   * @param lastMessageNumber analyze after this message
   * @return the responses to the last postCondition and the last violation condition message in the
   *     folder
   */
  public static ReplayResult getAnalysisResult(
      String program, String blockAnalysisFolder, String blockID, int lastMessageNumber)
      throws Exception {
    CFA cfa = TestUtil.buildTestCFA(program);
    Modification mod = getModifiedBlockGraph(cfa, blockAnalysisFolder);

    BlockNode node =
        mod.blockGraph().getNodes().stream()
            .filter(b -> b.getId().equals(blockID))
            .findAny()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No block with id '" + blockID + "' found in the block graph."));

    DssBlockAnalysis analysis = createAnalysis(mod.cfa(), node);

    DssViolationConditionMessage lastViolation = null;
    DssPostConditionMessage lastPrecondition = null;
    for(int i = 0; i <= lastMessageNumber; i++) {
      Path path = Path.of(blockAnalysisFolder, "messages", "M" + i + ".json");
      if (!path.toFile().exists()) {
        continue;
      }
      DssMessage curr = DssMessage.fromJson(path);

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

    Responses responses = new Responses(postAnalysis, violationAnalysis);
    return new ReplayResult(responses, analysis.getDcpa());
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

  private static Modification getModifiedBlockGraph(CFA pCfa, String pBlockAnalysisFolder)
      throws Exception {
    ImportDecomposition decomp =
        new ImportDecomposition(Path.of(pBlockAnalysisFolder, "blocks.json"));
    BlockGraph graph = decomp.decompose(pCfa);

    Configuration config =
        TestDataTools.configurationForTest()
            .loadFromFile(TestUtil.DSS_FORWARD_CONFIGURATION_FILE)
            .build();
    Modification mod =
        BlockGraphModification.instrumentCFA(
            pCfa, graph, config, LogManager.createTestLogManager());

    return mod;
  }


}
