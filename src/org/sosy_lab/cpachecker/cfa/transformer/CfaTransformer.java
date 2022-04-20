// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.TreeMultimap;
import java.util.HashMap;
import java.util.Iterator;
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
import org.sosy_lab.cpachecker.cfa.CfaProcessor.SupergraphCreator;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

public abstract class CfaTransformer {

  public CfaTransformer from(CfaTransformer pTransformer, CfaTransformer... pTransformers) {

    checkNotNull(pTransformer);

    ImmutableList<CfaTransformer> transformers = ImmutableList.copyOf(pTransformers);

    return new CfaTransformer() {

      @Override
      public CFA transform(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata, LogManager pLogger) {

        CFA transformedCfa = pTransformer.transform(pCfaNetwork, pCfaMetadata, pLogger);
        for (CfaTransformer transformer : transformers) {
          transformedCfa = transformer.transform(transformedCfa, pCfaMetadata, pLogger);
        }

        return transformedCfa;
      }
    };
  }

  protected final CFA createCfa(
      CfaNetwork pCfaNetwork,
      CfaMetadata pCfaMetadata,
      LogManager pLogger,
      ImmutableList<CfaProcessor> pCfaProcessors,
      NodeConverter pNodeConverter,
      EdgeConverter pEdgeConverter) {
    return new CfaCreator(pCfaProcessors, pCfaNetwork, pNodeConverter, pEdgeConverter)
        .createCfa(pCfaMetadata, pLogger);
  }

  public abstract CFA transform(
      CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata, LogManager pLogger);

  public final CFA transform(CFA pCfa, CfaMetadata pCfaMetadata, LogManager pLogger) {
    return transform(CfaNetwork.of(pCfa), pCfaMetadata, pLogger);
  }

  public interface Substitution {

    CFANode toSubstitute(CFANode pOriginalNode);

    CFAEdge toSubstitute(CFAEdge pOriginalEdge);
  }

  @FunctionalInterface
  protected interface NodeConverter {

    CFANode convertNode(CFANode pCfaNode, CfaNetwork pCfaNetwork, Substitution pSubstitution);
  }

  @FunctionalInterface
  protected interface EdgeConverter {

    @Nullable CFAEdge convertEdge(
        CFAEdge pCfaEdge,
        CfaNetwork pCfaNetwork,
        Substitution pSubstitution,
        CfaConnectedness pConnectedness);
  }

  private static final class CfaCreator implements Substitution {

    private final ImmutableList<CfaProcessor> cfaProcessors;

    private final CfaNetwork cfaNetwork;

    private final NodeConverter nodeConverter;
    private final EdgeConverter edgeConverter;

    private final Map<CFANode, CFANode> oldNodeToNewNode;
    private final Map<CFAEdge, CFAEdge> oldEdgeToNewEdge;

    private CfaConnectedness connectedness;

    private CfaCreator(
        ImmutableList<CfaProcessor> pCfaProcessors,
        CfaNetwork pCfaNetwork,
        NodeConverter pNodeConverter,
        EdgeConverter pEdgeConverter) {

      cfaProcessors = pCfaProcessors;

      cfaNetwork = pCfaNetwork;

      nodeConverter = pNodeConverter;
      edgeConverter = pEdgeConverter;

      oldNodeToNewNode = new HashMap<>();
      oldEdgeToNewEdge = new HashMap<>();
    }

    @Override
    public CFANode toSubstitute(CFANode pOldNode) {

      @Nullable CFANode newNode = oldNodeToNewNode.get(pOldNode);
      if (newNode != null) {
        return newNode;
      }

      return nodeConverter.convertNode(pOldNode, cfaNetwork, this);
    }

    @Override
    public CFAEdge toSubstitute(CFAEdge pOldEdge) {

      @Nullable CFAEdge newEdge = oldEdgeToNewEdge.get(pOldEdge);
      if (newEdge != null) {
        return newEdge;
      }

      newEdge = edgeConverter.convertEdge(pOldEdge, cfaNetwork, this, connectedness);

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

        CFANode newNode = toSubstitute(oldNode);
        String functionName = newNode.getFunction().getQualifiedName();

        if (newNode instanceof FunctionEntryNode) {
          newFunctions.put(functionName, (FunctionEntryNode) newNode);
        }

        newNodes.put(functionName, newNode);
      }

      connectedness = CfaConnectedness.INDEPENDENT_FUNCTIONS;
      cfaNetwork.edges().forEach(this::toSubstitute);

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
              ((ModifyingIndependentFunctionPostProcessor) cfaProcessor)
                  .process(mutableCfa, pLogger);
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
          mutableCfa =
              ((ModifyingSupergraphPostProcessor) cfaProcessor).process(mutableCfa, pLogger);
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

      if (pCfaMetadata.getConnectedness() == CfaConnectedness.SUPERGRAPH) {

        boolean supergraphCreated = false;

        for (CfaProcessor cfaProcessor : cfaProcessors) {
          if (cfaProcessor instanceof SupergraphCreator) {
            newMutableCfa = ((SupergraphCreator) cfaProcessor).process(newMutableCfa, pLogger);
            supergraphCreated = true;
          }
        }

        if (!supergraphCreated) {

          removePlaceholderEdges();

          connectedness = CfaConnectedness.SUPERGRAPH;
          cfaNetwork.edges().forEach(this::toSubstitute);
        }

        newMutableCfa.setMetadata(
            newMutableCfa.getMetadata().withConnectedness(CfaConnectedness.SUPERGRAPH));
      }

      newMutableCfa = runModifyingSupergraphPostProcessors(newMutableCfa, pLogger);
      runReadOnlySupergraphPostProcessors(newMutableCfa, pLogger);

      return newMutableCfa.makeImmutableCFA(newMutableCfa.getVarClassification());
    }
  }
}
