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
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFASecondPassBuilder;
import org.sosy_lab.cpachecker.cfa.CfaConnectedness;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.CfaPostProcessor;
import org.sosy_lab.cpachecker.cfa.CfaPostProcessor.FunctionPostProcessor;
import org.sosy_lab.cpachecker.cfa.CfaPostProcessor.SupergraphPostProcessor;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/** Utility class for creating CFA instances from {@link CfaNetwork} instances. */
public final class CfaCreator {

  private final Map<CFANode, CFANode> oldNodeToNewNode;
  private final Map<CFAEdge, CFAEdge> oldEdgeToNewEdge;

  private CfaCreator() {

    oldNodeToNewNode = new HashMap<>();
    oldEdgeToNewEdge = new HashMap<>();
  }

  private static MutableCFA runFunctionPostProcessors(
      ImmutableList<CfaPostProcessor> pCfaPostProcessors,
      MutableCFA pMutableCfa,
      LogManager pLogger) {

    MutableCFA mutableCfa = pMutableCfa;

    for (CfaPostProcessor cfaPostProcessor : pCfaPostProcessors) {
      if (cfaPostProcessor instanceof FunctionPostProcessor) {
        mutableCfa = cfaPostProcessor.process(mutableCfa, pLogger);
      }
    }

    return mutableCfa;
  }

  private static MutableCFA runSupergraphPostProcessors(
      ImmutableList<CfaPostProcessor> pCfaPostProcessors,
      MutableCFA pMutableCfa,
      LogManager pLogger) {

    MutableCFA mutableCfa = pMutableCfa;

    for (CfaPostProcessor cfaPostProcessor : pCfaPostProcessors) {
      if (cfaPostProcessor instanceof SupergraphPostProcessor) {
        mutableCfa = cfaPostProcessor.process(mutableCfa, pLogger);
      }
    }

    return mutableCfa;
  }

  /**
   * Returns an independent function {@link CfaNetwork} view for the specified (supergraph) {@link
   * CfaNetwork}.
   *
   * @param pCfaNetwork the {@link CfaNetwork} to create an independent function {@link CfaNetwork}
   *     view for
   * @param pSummaryToStatementEdgeTransformer the CFA edge transformer to use for getting statement
   *     edges for function summary edges
   * @return an independent function {@link CfaNetwork} view for the specified (supergraph) {@link
   *     CfaNetwork}.
   * @throws NullPointerException if any parameter is {@code null}
   */
  public static CfaNetwork toIndependentFunctionCfaNetwork(
      CfaNetwork pCfaNetwork, CfaEdgeTransformer pSummaryToStatementEdgeTransformer) {

    checkNotNull(pCfaNetwork);
    checkNotNull(pSummaryToStatementEdgeTransformer);

    CfaNetwork cfaWithoutSuperEdges = pCfaNetwork.withoutSuperEdges();
    CfaNetwork independentFunctionCfa =
        cfaWithoutSuperEdges.transformEdges(
            edge -> {
              if (edge instanceof FunctionSummaryEdge) {
                return pSummaryToStatementEdgeTransformer.transform(
                    edge, cfaWithoutSuperEdges, node -> node, CfaEdgeSubstitution.UNSUPPORTED);
              }

              return edge;
            });

    return independentFunctionCfa;
  }

  /**
   * Returns a new supergraph {@link CFA} instance for the specified independent function CFA
   * represented as a {@link CfaNetwork}.
   *
   * @param pCfaPostProcessors the CFA post-processors to run after CFA construction
   * @param pNodeTransformer the CFA node transformer that creates new CFA nodes for nodes contained
   *     in the specified {@link CfaNetwork}
   * @param pEdgeTransformer the CFA edge transformer that creates new CFA edges for edges contained
   *     in the specified {@link CfaNetwork}
   * @param pCfa the CFA (represented as a {@link CfaNetwork}) to create a {@link CFA} instance for
   * @param pCfaMetadata the metadata of the specified CFA
   * @param pConfiguration the configuration to use during CFA construction
   * @param pLogger the logger to use during CFA construction
   * @return a new supergraph {@link CFA} instance for the specified independent function CFA
   *     represented as a {@link CfaNetwork}
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if the main function entry node is not part of the actual CFA
   * @throws IllegalArgumentException if the specified CFA is not an independent function CFA, but a
   *     supergraph CFA
   */
  public static CFA createCfa(
      List<CfaPostProcessor> pCfaPostProcessors,
      CfaNodeTransformer pNodeTransformer,
      CfaEdgeTransformer pEdgeTransformer,
      CfaNetwork pCfa,
      CfaMetadata pCfaMetadata,
      Configuration pConfiguration,
      LogManager pLogger) {

    checkArgument(
        pCfa.nodes().contains(pCfaMetadata.getMainFunctionEntry()),
        "Cannot use specified main function entry node because it is not part of the CFA: %s",
        pCfaMetadata.getMainFunctionEntry());

    checkArgument(
        pCfaMetadata.getConnectedness() == CfaConnectedness.INDEPENDENT_FUNCTIONS,
        "Cannot use specified CFA because it does not consist of independent functions");

    return new CfaCreator()
        .createSupergraphCfa(
            ImmutableList.copyOf(pCfaPostProcessors),
            checkNotNull(pNodeTransformer),
            checkNotNull(pEdgeTransformer),
            checkNotNull(pCfa),
            checkNotNull(pCfaMetadata),
            checkNotNull(pConfiguration),
            checkNotNull(pLogger));
  }

  private CFANode toNew(CFANode pOldNode, CfaNetwork pCfa, CfaNodeTransformer pNodeTransformer) {

    @Nullable CFANode newNode = oldNodeToNewNode.get(pOldNode);
    if (newNode != null) {
      return newNode;
    }

    newNode =
        pNodeTransformer.transform(
            pOldNode, pCfa, oldNode -> toNew(oldNode, pCfa, pNodeTransformer));
    oldNodeToNewNode.put(pOldNode, newNode);

    return newNode;
  }

  private CFAEdge toNew(
      CFAEdge pOldEdge,
      CfaNetwork pCfa,
      CfaNodeTransformer pNodeTransformer,
      CfaEdgeTransformer pEdgeTransformer) {

    @Nullable CFAEdge newEdge = oldEdgeToNewEdge.get(pOldEdge);
    if (newEdge != null) {
      return newEdge;
    }

    newEdge =
        pEdgeTransformer.transform(
            pOldEdge,
            pCfa,
            oldNode -> toNew(oldNode, pCfa, pNodeTransformer),
            oldEdge -> toNew(oldEdge, pCfa, pNodeTransformer, pEdgeTransformer));
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

  private MutableCFA createMutableCfa(
      CfaNetwork pCfa,
      CfaMetadata pCfaMetadata,
      CfaNodeTransformer pNodeTransformer,
      CfaEdgeTransformer pEdgeTransformer) {

    CFANode oldMainEntryNode = pCfaMetadata.getMainFunctionEntry();

    NavigableMap<String, FunctionEntryNode> newFunctions = new TreeMap<>();
    TreeMultimap<String, CFANode> newNodes = TreeMultimap.create();

    for (CFANode oldNode : pCfa.nodes()) {

      CFANode newNode = toNew(oldNode, pCfa, pNodeTransformer);
      String functionName = newNode.getFunction().getQualifiedName();

      if (newNode instanceof FunctionEntryNode) {
        newFunctions.put(functionName, (FunctionEntryNode) newNode);
      }

      newNodes.put(functionName, newNode);
    }

    pCfa.edges().forEach(oldEdge -> toNew(oldEdge, pCfa, pNodeTransformer, pEdgeTransformer));

    CfaMetadata newCfaMetadata =
        pCfaMetadata
            .withMainFunctionEntry((FunctionEntryNode) oldNodeToNewNode.get(oldMainEntryNode))
            .withConnectedness(CfaConnectedness.INDEPENDENT_FUNCTIONS);

    return new MutableCFA(newFunctions, newNodes, newCfaMetadata);
  }

  private CFA createSupergraphCfa(
      ImmutableList<CfaPostProcessor> pCfaPostProcessors,
      CfaNodeTransformer pNodeTransformer,
      CfaEdgeTransformer pEdgeTransformer,
      CfaNetwork pCfa,
      CfaMetadata pCfaMetadata,
      Configuration pConfig,
      LogManager pLogger) {

    CfaMetadata cfaMetadataWithoutOptionalAttributes =
        pCfaMetadata
            .withLoopStructure(null)
            .withVariableClassification(null)
            .withLiveVariables(null);

    MutableCFA newMutableCfa =
        createMutableCfa(
            pCfa, cfaMetadataWithoutOptionalAttributes, pNodeTransformer, pEdgeTransformer);

    newMutableCfa = runFunctionPostProcessors(pCfaPostProcessors, newMutableCfa, pLogger);

    try {

      CFASecondPassBuilder supergraphCreator =
          new CFASecondPassBuilder(newMutableCfa, pCfaMetadata.getLanguage(), pLogger, pConfig);
      supergraphCreator.insertCallEdgesRecursively();
      newMutableCfa.setMetadata(
          newMutableCfa.getMetadata().withConnectedness(CfaConnectedness.SUPERGRAPH));

    } catch (InvalidConfigurationException | ParserException ex) {
      pLogger.logException(
          Level.WARNING,
          ex,
          "CFA supergraph creation failed, continuing with independent function CFA");
    }

    newMutableCfa = runSupergraphPostProcessors(pCfaPostProcessors, newMutableCfa, pLogger);

    return newMutableCfa.makeImmutableCFA(newMutableCfa.getVarClassification());
  }
}
