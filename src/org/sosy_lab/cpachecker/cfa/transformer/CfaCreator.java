// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.TreeMultimap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaConnectedness;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.CfaProcessor;
import org.sosy_lab.cpachecker.cfa.CfaProcessor.ModifyingIndependentFunctionPostProcessor;
import org.sosy_lab.cpachecker.cfa.CfaProcessor.ModifyingSupergraphPostProcessor;
import org.sosy_lab.cpachecker.cfa.CfaProcessor.ReadOnlyIndependentFunctionPostProcessor;
import org.sosy_lab.cpachecker.cfa.CfaProcessor.ReadOnlySupergraphPostProcessor;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

public final class CfaCreator {

  private final ImmutableList<CfaProcessor> cfaProcessors;

  private final CfaNetwork cfaNetwork;

  private final CfaNodeTransformer nodeTransformer;
  private final CfaEdgeTransformer edgeTransformer;

  private final Map<CFANode, CFANode> oldNodeToNewNode;
  private final Map<CFAEdge, CFAEdge> oldEdgeToNewEdge;

  private CfaConnectedness connectedness;

  private CfaCreator(
      ImmutableList<CfaProcessor> pCfaProcessors,
      CfaNetwork pCfaNetwork,
      CfaNodeTransformer pNodeTransformer,
      CfaEdgeTransformer pEdgeTransformer) {

    cfaProcessors = pCfaProcessors;

    cfaNetwork = pCfaNetwork;

    nodeTransformer = pNodeTransformer;
    edgeTransformer = pEdgeTransformer;

    oldNodeToNewNode = new HashMap<>();
    oldEdgeToNewEdge = new HashMap<>();
  }

  public static CFA createCfa(
      List<CfaProcessor> pCfaProcessors,
      CfaNetwork pCfaNetwork,
      CfaNodeTransformer pNodeTransformer,
      CfaEdgeTransformer pEdgeTransformer,
      CfaMetadata pCfaMetadata,
      LogManager pLogger) {

    checkArgument(
        pCfaNetwork.nodes().contains(pCfaMetadata.getMainFunctionEntry()),
        "cannot use specified main function entry node because it is not part of the CFA: %s",
        pCfaMetadata.getMainFunctionEntry());

    return new CfaCreator(
            ImmutableList.copyOf(pCfaProcessors),
            checkNotNull(pCfaNetwork),
            checkNotNull(pNodeTransformer),
            checkNotNull(pEdgeTransformer))
        .createCfa(checkNotNull(pCfaMetadata), checkNotNull(pLogger));
  }

  private CFANode toNew(CFANode pOldNode) {

    @Nullable CFANode newNode = oldNodeToNewNode.get(pOldNode);
    if (newNode != null) {
      return newNode;
    }

    newNode = nodeTransformer.transform(pOldNode, cfaNetwork, this::toNew);
    oldNodeToNewNode.put(pOldNode, newNode);

    return newNode;
  }

  private CFAEdge toNew(CFAEdge pOldEdge) {

    @Nullable CFAEdge newEdge = oldEdgeToNewEdge.get(pOldEdge);
    if (newEdge != null) {
      return newEdge;
    }

    newEdge =
        edgeTransformer.transform(pOldEdge, cfaNetwork, this::toNew, this::toNew, connectedness);
    oldEdgeToNewEdge.put(pOldEdge, newEdge);

    if (newEdge instanceof FunctionSummaryEdge) {
      FunctionSummaryEdge newSummaryEdge = (FunctionSummaryEdge) newEdge;
      newEdge.getPredecessor().addLeavingSummaryEdge(newSummaryEdge);
      newEdge.getSuccessor().addEnteringSummaryEdge(newSummaryEdge);
    } else {
      newEdge.getPredecessor().addLeavingEdge(newEdge);
      newEdge.getSuccessor().addEnteringEdge(newEdge);
    }

    return newEdge;
  }

  private MutableCFA createIndependentFunctionCfa(CfaMetadata pCfaMetadata) {

    CFANode oldMainEntryNode = pCfaMetadata.getMainFunctionEntry();

    NavigableMap<String, FunctionEntryNode> newFunctions = new TreeMap<>();
    TreeMultimap<String, CFANode> newNodes = TreeMultimap.create();

    for (CFANode oldNode : cfaNetwork.nodes()) {

      CFANode newNode = toNew(oldNode);
      String functionName = newNode.getFunction().getQualifiedName();

      if (newNode instanceof FunctionEntryNode) {
        newFunctions.put(functionName, (FunctionEntryNode) newNode);
      }

      newNodes.put(functionName, newNode);
    }

    connectedness = CfaConnectedness.INDEPENDENT_FUNCTIONS;
    for (CFAEdge oldEdge : cfaNetwork.edges()) {
      // We create independent CFAs for each function, so we ignore call and return edges.
      // Function summary edges are not ignored, because statement edges are created for them.
      if (!(oldEdge instanceof FunctionCallEdge) && !(oldEdge instanceof FunctionReturnEdge)) {
        toNew(oldEdge);
      }
    }

    CfaMetadata cfaMetadata =
        pCfaMetadata
            .withMainFunctionEntry((FunctionEntryNode) oldNodeToNewNode.get(oldMainEntryNode))
            .withConnectedness(CfaConnectedness.INDEPENDENT_FUNCTIONS);

    return new MutableCFA(newFunctions, newNodes, cfaMetadata);
  }

  /** Removes all placeholder edges that were inserted instead of function calls. */
  private void removePlaceholderEdges() {

    Iterator<Map.Entry<CFAEdge, CFAEdge>> oldEdgeToNewEdgeIterator =
        oldEdgeToNewEdge.entrySet().iterator();

    while (oldEdgeToNewEdgeIterator.hasNext()) {

      Map.Entry<CFAEdge, CFAEdge> entry = oldEdgeToNewEdgeIterator.next();
      CFAEdge oldEdge = entry.getKey();
      CFAEdge newEdge = entry.getValue();

      if (oldEdge instanceof FunctionSummaryEdge) {
        oldEdgeToNewEdgeIterator.remove();
        newEdge.getPredecessor().removeLeavingEdge(newEdge);
        newEdge.getSuccessor().removeEnteringEdge(newEdge);
      }
    }
  }

  private MutableCFA runModifyingIndependentFunctionPostProcessors(
      MutableCFA pMutableCfa, LogManager pLogger) {

    MutableCFA mutableCfa = pMutableCfa;

    for (CfaProcessor cfaProcessor : cfaProcessors) {
      if (cfaProcessor instanceof ModifyingIndependentFunctionPostProcessor) {
        mutableCfa =
            ((ModifyingIndependentFunctionPostProcessor) cfaProcessor).process(mutableCfa, pLogger);
      }
    }

    return mutableCfa;
  }

  private void runReadOnlyIndependentFunctionPostProcessors(
      MutableCFA pMutableCfa, LogManager pLogger) {

    for (CfaProcessor cfaProcessor : cfaProcessors) {
      if (cfaProcessor instanceof ReadOnlyIndependentFunctionPostProcessor) {
        ((ReadOnlyIndependentFunctionPostProcessor) cfaProcessor).process(pMutableCfa, pLogger);
      }
    }
  }

  private MutableCFA runModifyingSupergraphPostProcessors(
      MutableCFA pMutableCfa, LogManager pLogger) {

    MutableCFA mutableCfa = pMutableCfa;

    for (CfaProcessor cfaProcessor : cfaProcessors) {
      if (cfaProcessor instanceof ModifyingSupergraphPostProcessor) {
        mutableCfa = ((ModifyingSupergraphPostProcessor) cfaProcessor).process(mutableCfa, pLogger);
      }
    }

    return mutableCfa;
  }

  private void runReadOnlySupergraphPostProcessors(MutableCFA pMutableCfa, LogManager pLogger) {

    for (CfaProcessor cfaProcessor : cfaProcessors) {
      if (cfaProcessor instanceof ReadOnlySupergraphPostProcessor) {
        ((ReadOnlySupergraphPostProcessor) cfaProcessor).process(pMutableCfa, pLogger);
      }
    }
  }

  private CFA createCfa(CfaMetadata pCfaMetadata, LogManager pLogger) {

    MutableCFA newMutableCfa =
        createIndependentFunctionCfa(
            pCfaMetadata
                .withLoopStructure(null)
                .withVariableClassification(null)
                .withLiveVariables(null));

    newMutableCfa = runModifyingIndependentFunctionPostProcessors(newMutableCfa, pLogger);
    runReadOnlyIndependentFunctionPostProcessors(newMutableCfa, pLogger);

    removePlaceholderEdges();
    connectedness = CfaConnectedness.SUPERGRAPH;
    cfaNetwork.edges().forEach(this::toNew);

    newMutableCfa.setMetadata(
        newMutableCfa.getMetadata().withConnectedness(CfaConnectedness.SUPERGRAPH));

    newMutableCfa = runModifyingSupergraphPostProcessors(newMutableCfa, pLogger);
    runReadOnlySupergraphPostProcessors(newMutableCfa, pLogger);

    return newMutableCfa.makeImmutableCFA(newMutableCfa.getVarClassification());
  }
}
