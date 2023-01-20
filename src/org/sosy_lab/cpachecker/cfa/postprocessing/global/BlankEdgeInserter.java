// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.global;

import com.google.common.collect.Iterables;
import de.uni_freiburg.informatik.ultimate.util.datastructures.ImmutableSet;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.FlexCfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.transformer.CfaTransformer;
import org.sosy_lab.cpachecker.cfa.transformer.c.CCfaFactory;

/**
 * CFA transformer that adds new blank edges before and after all function entry and exit nodes.
 *
 * <p>This is useful for finding out whether components can handle these new blank edges.
 */
public final class BlankEdgeInserter implements CfaTransformer {

  @Override
  public CFA transform(
      CfaNetwork pCfaNetwork,
      CfaMetadata pCfaMetadata,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {
    FlexCfaNetwork flexCfaNetwork = FlexCfaNetwork.copy(pCfaNetwork);

    ImmutableSet<FunctionEntryNode> functionEntryNodes =
        flexCfaNetwork.nodes().stream()
            .filter(node -> node instanceof FunctionEntryNode)
            .map(node -> (FunctionEntryNode) node)
            .collect(ImmutableSet.collector());
    for (FunctionEntryNode functionEntryNode : functionEntryNodes) {
      flexCfaNetwork.insertPredecessor(
          CFANode.newDummyCFANode(functionEntryNode.getFunctionName()),
          new BlankEdge(
              "",
              FileLocation.DUMMY,
              CFANode.newDummyCFANode(),
              CFANode.newDummyCFANode(),
              "before function entry node"),
          functionEntryNode);
      flexCfaNetwork.insertSuccessor(
          functionEntryNode,
          new BlankEdge(
              "",
              FileLocation.DUMMY,
              CFANode.newDummyCFANode(),
              CFANode.newDummyCFANode(),
              "after function entry node"),
          CFANode.newDummyCFANode(functionEntryNode.getFunctionName()));
    }

    ImmutableSet<FunctionExitNode> functionExitNodes =
        flexCfaNetwork.nodes().stream()
            .filter(node -> node instanceof FunctionExitNode)
            .map(node -> (FunctionExitNode) node)
            .collect(ImmutableSet.collector());
    for (FunctionExitNode functionExitNode : functionExitNodes) {
      flexCfaNetwork.insertPredecessor(
          CFANode.newDummyCFANode(functionExitNode.getFunctionName()),
          new BlankEdge(
              "",
              FileLocation.DUMMY,
              CFANode.newDummyCFANode(),
              CFANode.newDummyCFANode(),
              "before function exit node"),
          functionExitNode);
      flexCfaNetwork.insertSuccessor(
          functionExitNode,
          new BlankEdge(
              "",
              FileLocation.DUMMY,
              CFANode.newDummyCFANode(),
              CFANode.newDummyCFANode(),
              "after function exit node"),
          CFANode.newDummyCFANode(functionExitNode.getFunctionName()));
    }

    // remove dead code
    ImmutableSet<CFANode> nodes = ImmutableSet.copyOf(flexCfaNetwork.nodes());
    for (CFANode node : nodes) {
      if (flexCfaNetwork.inDegree(node) == 1
          && flexCfaNetwork.outDegree(node) == 0
          && !(node instanceof FunctionExitNode)
          && Iterables.getOnlyElement(flexCfaNetwork.inEdges(node)).getEdgeType()
              == CFAEdgeType.BlankEdge) {
        flexCfaNetwork.removeNode(node);
      } else if (flexCfaNetwork.inDegree(node) == 0
          && flexCfaNetwork.outDegree(node) == 1
          && !(node instanceof FunctionEntryNode)
          && Iterables.getOnlyElement(flexCfaNetwork.outEdges(node)).getEdgeType()
              == CFAEdgeType.BlankEdge) {
        flexCfaNetwork.removeNode(node);
      }
    }

    return CCfaFactory.CLONER.createCfa(flexCfaNetwork, pCfaMetadata, pLogger, pShutdownNotifier);
  }
}
