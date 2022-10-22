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
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.TreeMultimap;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFASecondPassBuilder;
import org.sosy_lab.cpachecker.cfa.CfaConnectedness;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.CfaPostProcessor;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * Builder for creating {@link CFA} instances for ({@link CfaNetwork}, {@link CfaMetadata}) pairs.
 *
 * <p>This class should only be used by CFA factory implementations. Other code should use CFA
 * factory implementations instead.
 */
public final class CfaBuilder {

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private final CfaNodeTransformer cfaNodeTransformer;
  private final CfaEdgeTransformer cfaEdgeTransformer;

  private final CfaNetwork cfaNetwork;

  private final Map<CFANode, CFANode> oldNodeToNewNode;
  private final Map<CFAEdge, CFAEdge> oldEdgeToNewEdge;

  // `null` before initialization and after a CFA has been built
  private @Nullable MutableCFA mutableCfa;

  private CfaBuilder(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CfaNodeTransformer pCfaNodeTransformer,
      CfaEdgeTransformer pCfaEdgeTransformer,
      CfaNetwork pCfaNetwork) {

    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

    cfaNodeTransformer = pCfaNodeTransformer;
    cfaEdgeTransformer = pCfaEdgeTransformer;

    cfaNetwork = pCfaNetwork;

    oldNodeToNewNode = new HashMap<>();
    oldEdgeToNewEdge = new HashMap<>();
  }

  /**
   * Returns a new {@link CfaBuilder} for further CFA manipulation and final CFA creation.
   *
   * @param pLogger the logger to use during CFA creation
   * @param pShutdownNotifier the shutdown notifier to use during CFA creation
   * @param pCfaNodeTransformer the {@link CfaNodeTransformer} to use during CFA creation
   * @param pCfaEdgeTransformer the {@link CfaEdgeTransformer} to use during CFA creation
   * @param pCfaNetwork the {@link CfaNetwork} to create a CFA for
   * @param pCfaMetadata the {@link CfaMetadata} of the specified {@link CfaNetwork}
   * @return a new {@link CfaBuilder} for further CFA manipulation and final CFA creation
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if the specified {@link CfaNetwork} contains edges that are
   *     not permitted by its connectedness (e.g., if an {@link
   *     CfaConnectedness#UNCONNECTED_FUNCTIONS unconnected function CFA} contains function call
   *     edges)
   * @throws IllegalArgumentException if the specified {@link CfaMetadata} doesn't fit the CFA
   *     represented by the {@link CfaNetwork} (e.g., if the {@link
   *     CfaMetadata#getMainFunctionEntry() function entry node} doesn't exist in the {@link
   *     CfaNetwork})
   */
  public static CfaBuilder builder(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CfaNodeTransformer pCfaNodeTransformer,
      CfaEdgeTransformer pCfaEdgeTransformer,
      CfaNetwork pCfaNetwork,
      CfaMetadata pCfaMetadata) {

    checkArgument(
        pCfaNetwork.nodes().contains(pCfaMetadata.getMainFunctionEntry()),
        "Function entry node is not part of the CFA: %s",
        pCfaMetadata.getMainFunctionEntry());

    if (pCfaMetadata.getConnectedness() == CfaConnectedness.UNCONNECTED_FUNCTIONS) {
      for (CFAEdge edge : pCfaNetwork.edges()) {
        if (edge instanceof FunctionCallEdge
            || edge instanceof FunctionReturnEdge
            || edge instanceof FunctionSummaryEdge) {
          throw new IllegalArgumentException(
              "CFA edge not allowed in unconnected function CFAs: " + edge);
        }
      }
    }

    return new CfaBuilder(
            checkNotNull(pLogger),
            checkNotNull(pShutdownNotifier),
            checkNotNull(pCfaNodeTransformer),
            checkNotNull(pCfaEdgeTransformer),
            checkNotNull(pCfaNetwork))
        .initializeMutableCfa(checkNotNull(pCfaMetadata));
  }

  /**
   * Returns the new CFA node for the old specified node.
   *
   * <p>New CFA nodes are stored. Only if the old node doesn't have a corresponding new node, the
   * new node is created.
   *
   * @param pOldNode the old CFA node to get the new node for
   * @return the new CFA node for the old specified node
   * @throws NullPointerException if {@code pOldNode == null}
   */
  private CFANode toNew(CFANode pOldNode) {

    @Nullable CFANode newNode = oldNodeToNewNode.get(checkNotNull(pOldNode));
    if (newNode != null) {
      return newNode;
    }

    newNode = cfaNodeTransformer.transform(pOldNode, cfaNetwork, this::toNew);
    oldNodeToNewNode.put(pOldNode, newNode);

    return newNode;
  }

  /**
   * Returns the new CFA edge for the old specified edge and adds the new edge to its endpoints.
   *
   * <p>New CFA edges are stored. Only if the old edge doesn't have a corresponding new edge, the
   * new edge is created.
   *
   * @param pOldEdge the old CFA edge to get the new edge for
   * @return the new CFA edge for the old specified edge
   * @throws NullPointerException if {@code pOldEdge == null}
   */
  private CFAEdge toNew(CFAEdge pOldEdge) {

    @Nullable CFAEdge newEdge = oldEdgeToNewEdge.get(checkNotNull(pOldEdge));
    if (newEdge != null) {
      return newEdge;
    }

    newEdge = cfaEdgeTransformer.transform(pOldEdge, cfaNetwork, this::toNew, this::toNew);
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

  /**
   * Initializes internal {@link CfaBuilder#mutableCfa}.
   *
   * @param pCfaMetadata the {@link CfaMetadata} to use for {@link CfaBuilder#mutableCfa}
   *     initialization
   * @return this {@link CfaBuilder} instance for further CFA manipulation and final CFA creation
   * @throws IllegalStateException if {@link CfaBuilder#mutableCfa} has already been initialized
   */
  private CfaBuilder initializeMutableCfa(CfaMetadata pCfaMetadata) {

    checkState(mutableCfa == null, "Internal MutableCFA already initialized");

    CfaMetadata cfaMetadata =
        pCfaMetadata
            .withLoopStructure(null)
            .withVariableClassification(null)
            .withLiveVariables(null);

    CFANode oldMainEntryNode = cfaMetadata.getMainFunctionEntry();

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

    cfaNetwork.edges().forEach(this::toNew);

    CfaMetadata newCfaMetadata =
        cfaMetadata.withMainFunctionEntry(
            (FunctionEntryNode) oldNodeToNewNode.get(oldMainEntryNode));

    mutableCfa = new MutableCFA(newFunctions, newNodes, newCfaMetadata);

    return this;
  }

  /**
   * Executes the specified CFA post-processor.
   *
   * @param pCfaPostProcessor the CFA post-processor to execute
   * @return this {@link CfaBuilder} instance for further CFA manipulation and final CFA creation
   */
  public CfaBuilder runPostProcessor(CfaPostProcessor pCfaPostProcessor) {

    mutableCfa = pCfaPostProcessor.execute(mutableCfa, logger, shutdownNotifier);

    return this;
  }

  /**
   * Builds the supergraph CFA.
   *
   * @return this {@link CfaBuilder} instance for further CFA manipulation and final CFA creation
   * @throws IllegalStateException if the CFA of this builder is already a supergraph
   * @throws IllegalStateException if CFA supergraph creation failed for some other reason
   */
  public CfaBuilder toSupergraph() {

    CfaMetadata cfaMetadata = mutableCfa.getMetadata();
    checkState(
        cfaMetadata.getConnectedness() != CfaConnectedness.SUPERGRAPH,
        "CFA is already a supergraph");

    try {

      Configuration defaultConfig = Configuration.defaultConfiguration();
      CFASecondPassBuilder supergraphCreator =
          new CFASecondPassBuilder(mutableCfa, cfaMetadata.getLanguage(), logger, defaultConfig);
      supergraphCreator.insertCallEdgesRecursively();
      cfaMetadata = mutableCfa.getMetadata().withConnectedness(CfaConnectedness.SUPERGRAPH);
      mutableCfa.setMetadata(cfaMetadata);

    } catch (InvalidConfigurationException | ParserException ex) {
      throw new IllegalStateException("Cannot create CFA supergraph", ex);
    }

    return this;
  }

  /**
   * Creates a new {@link CFA} instance created by this {@link CfaBuilder}.
   *
   * <p>This method can only be invoked once for every {@link CfaBuilder} instance.
   *
   * @return a new {@link CFA} instance created by this {@link CfaBuilder}
   * @throws IllegalStateException if this method was already invoked
   */
  public CFA createCfa() {

    checkState(mutableCfa != null, "Builder has already created a CFA and is not reusable");

    CFA cfa = mutableCfa.makeImmutableCFA(mutableCfa.getVarClassification());
    mutableCfa = null;

    return cfa;
  }
}
