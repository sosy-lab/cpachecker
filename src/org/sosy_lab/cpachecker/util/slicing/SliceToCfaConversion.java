// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeMultimap;
import com.google.common.graph.EndpointPair;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CCfaTransformer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMutableNetwork;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.CFASimplifier;

/** Utility class for turning {@link Slice} instances into {@link CFA} instances. */
final class SliceToCfaConversion {

  private SliceToCfaConversion() {}

  /** Replaces the specified edge by a no-op blank edge in the specified mutable network. */
  private static void replaceIrrelevantEdge(CfaMutableNetwork pGraph, CFAEdge pEdge) {

    EndpointPair<CFANode> endpoints = pGraph.incidentNodes(pEdge);
    CFANode nodeU = endpoints.nodeU();
    CFANode nodeV = endpoints.nodeV();

    pGraph.removeEdge(pEdge);
    CFAEdge replacementEdge =
        new BlankEdge("", pEdge.getFileLocation(), nodeU, nodeV, "slice-irrelevant");
    pGraph.addEdge(nodeU, nodeV, replacementEdge);
  }

  /**
   * Returns whether the specified edge has at least one endpoint in an irrelevant function (i.e., a
   * function that contains no edges that are part of the slice).
   */
  private static boolean isIrrelevantFunctionEdge(
      ImmutableSet<AFunctionDeclaration> pRelevantFunctions, CFAEdge pEdge) {
    return !pRelevantFunctions.contains(pEdge.getPredecessor().getFunction())
        || !pRelevantFunctions.contains(pEdge.getSuccessor().getFunction());
  }

  /**
   * Returns whether the specified edge should be replaced by a no-op blank edge if the edge is not
   * part of the slice.
   */
  private static boolean isReplaceableEdge(CFAEdge pEdge) {
    // Replacing function call/return edges leads to invalid CFAs.
    // Irrelevant assume edges are replaced during CFA simplification, which requires assume edges
    // with conditions instead of blank edges to work properly.
    return !(pEdge instanceof FunctionCallEdge)
        && !(pEdge instanceof FunctionReturnEdge)
        && !(pEdge instanceof AssumeEdge);
  }

  /**
   * Creates a simplified CFA for the specified CFA using {@link
   * CFASimplifier#simplifyCFA(MutableCFA)}.
   */
  private static CFA createSimplifiedCfa(CFA pCfa) {

    NavigableMap<String, FunctionEntryNode> functionEntryNodes = new TreeMap<>();
    TreeMultimap<String, CFANode> allNodes = TreeMultimap.create();

    for (CFANode node : pCfa.getAllNodes()) {

      String functionName = node.getFunction().getQualifiedName();
      allNodes.put(functionName, node);

      if (node instanceof FunctionEntryNode) {
        functionEntryNodes.put(functionName, (FunctionEntryNode) node);
      }
    }

    MutableCFA mutableSliceCfa =
        new MutableCFA(
            pCfa.getMachineModel(),
            functionEntryNodes,
            allNodes,
            pCfa.getMainFunction(),
            pCfa.getFileNames(),
            pCfa.getLanguage());

    CFASimplifier.simplifyCFA(mutableSliceCfa);

    return mutableSliceCfa.makeImmutableCFA(mutableSliceCfa.getVarClassification());
  }

  /**
   * Creates a {@link CFA} that matches the specified {@link Slice} as closely as possible.
   *
   * @param pConfig the configuration to use
   * @param pLogger the logger to use during conversion
   * @param pSlice the slice to create a CFA for
   * @return the CFA created for the specified slice
   */
  public static CFA convert(Configuration pConfig, LogManager pLogger, Slice pSlice) {

    ImmutableSet<CFAEdge> relevantEdges = pSlice.getRelevantEdges();
    // relevant functions are functions with at least one relevant edge
    ImmutableSet<AFunctionDeclaration> relevantFunctions =
        relevantEdges.stream()
            .map(edge -> edge.getSuccessor().getFunction())
            .collect(ImmutableSet.toImmutableSet());

    CfaMutableNetwork graph = CfaMutableNetwork.of(pSlice.getOriginalCfa());

    ImmutableList<CFAEdge> irrelevantFunctionEdges =
        graph.edges().stream()
            .filter(edge -> isIrrelevantFunctionEdge(relevantFunctions, edge))
            .collect(ImmutableList.toImmutableList());
    irrelevantFunctionEdges.forEach(graph::removeEdge);

    ImmutableList<CFAEdge> irrelevantEdges =
        graph.edges().stream()
            .filter(edge -> !relevantEdges.contains(edge) && isReplaceableEdge(edge))
            .collect(ImmutableList.toImmutableList());
    irrelevantEdges.forEach(edge -> replaceIrrelevantEdge(graph, edge));

    ImmutableList<CFANode> irrelevantNodes =
        graph.nodes().stream()
            .filter(node -> graph.adjacentNodes(node).isEmpty())
            .collect(ImmutableList.toImmutableList());
    irrelevantNodes.forEach(graph::removeNode);

    CFA sliceCfa =
        CCfaTransformer.createCfa(
            pConfig,
            pLogger,
            pSlice.getOriginalCfa(),
            graph,
            (edge, astNode) -> (CAstNode) pSlice.getRelevantAstNode(edge).orElse(null));

    return createSimplifiedCfa(sliceCfa);
  }
}
