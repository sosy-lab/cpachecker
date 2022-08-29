// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.FluentIterable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CCfaTransformer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMutableNetwork;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

public class BlockGraphBuilder {

  private final CFADecomposer decomposer;
  private final CFA cfa;
  private final ShutdownManager shutdownManager;
  private final Configuration config;
  private final LogManager logger;

  public final static String DESCRIPTION = "Dummy edge for block decomposition";

  public BlockGraphBuilder(CFA pCfa, CFADecomposer pDecomposer, ShutdownManager pManager, Configuration pConfiguration, LogManager pLogger) {
    decomposer = pDecomposer;
    cfa = pCfa;
    shutdownManager = pManager;
    config = pConfiguration;
    logger = pLogger;
  }

  static boolean isBlockEnd(CFANode node) {
    return node.getNumLeavingEdges() == 0 || CFAUtils.allEnteringEdges(node).anyMatch(edge -> edge instanceof BlankEdge && edge.getDescription().equals(DESCRIPTION));
  }

  public Pair<BlockGraph, CFA> build() throws InterruptedException, InvalidConfigurationException {
    BlockGraph blockGraph = decomposer.decompose(cfa);
    CfaMutableNetwork network = CfaMutableNetwork.of(cfa);

    final Set<CFANode> hasEnd = new LinkedHashSet<>();
    for (BlockNode distinctNode : blockGraph.getDistinctNodes()) {
      CFANode lastNode = distinctNode.getLastNode();
      if (lastNode.getNumLeavingEdges() == 0) {
        continue;
      }
      Collection<CFAEdge> relevant = FluentIterable.from(distinctNode.getEdgesInBlock()).filter(edge -> edge.getSuccessor().equals(lastNode)).toSet();
      if (relevant.isEmpty()) {
        continue;
      }
      if (hasEnd.contains(lastNode)) {
        continue;
      }
      hasEnd.add(lastNode);
      // FileLocation loc = relevant.stream().findFirst().orElseThrow().getFileLocation();
      CFANode intermediate = new CFANode(lastNode.getFunction());
      network.insertPredecessor(intermediate, lastNode, new BlankEdge("", FileLocation.DUMMY, intermediate, lastNode, DESCRIPTION));
    }

    CFA mutant = CCfaTransformer.createCfa(config, logger, cfa, network, (edge, node) -> node, (n1, n2) -> n2);
    KnownBlockEndsDecomposition decomposition =
        new KnownBlockEndsDecomposition(shutdownManager.getNotifier(), (node, i) -> isBlockEnd(node));
    blockGraph = decomposition.decompose(mutant);
    blockGraph = blockGraph.prependDummyRoot(mutant, shutdownManager.getNotifier());
    return Pair.of(blockGraph, mutant);
  }


}
