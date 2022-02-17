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
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.CFASimplifier;

final class SliceToCfaConverter {

  private static void replaceIrrelevantEdge(CfaMutableNetwork pGraph, CFAEdge pEdge) {

    EndpointPair<CFANode> endpoints = pGraph.incidentNodes(pEdge);
    CFANode nodeU = endpoints.nodeU();
    CFANode nodeV = endpoints.nodeV();

    pGraph.removeEdge(pEdge);
    CFAEdge replacementEdge =
        new BlankEdge("", pEdge.getFileLocation(), nodeU, nodeV, "slice-irrelevant");
    pGraph.addEdge(nodeU, nodeV, replacementEdge);
  }

  private static boolean isIrrelevantFunctionEdge(
      ImmutableSet<AFunctionDeclaration> pRelevantFunctions, CFAEdge pEdge) {
    return !pRelevantFunctions.contains(pEdge.getPredecessor().getFunction())
        || !pRelevantFunctions.contains(pEdge.getSuccessor().getFunction());
  }

  private static boolean isReplaceableEdge(CFAEdge pEdge) {
    return !(pEdge instanceof FunctionCallEdge) && !(pEdge instanceof FunctionReturnEdge);
  }

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

  public static CFA convert(Configuration pConfig, LogManager pLogger, Slice pSlice) {

    ImmutableSet<CFAEdge> relevantEdges = pSlice.getRelevantEdges();
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
