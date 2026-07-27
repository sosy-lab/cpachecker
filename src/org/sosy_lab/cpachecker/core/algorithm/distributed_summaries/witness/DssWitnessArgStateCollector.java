// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.witness;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatisticsMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraphModification.Modification;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.witnesses.RelevantArgStatesCollector;

public class DssWitnessArgStateCollector implements RelevantArgStatesCollector {

  private final Multimap<CFANode, ARGState> collectedLoopHeadPreconditions = HashMultimap.create();
  private boolean allStatsContainedStates = true;

  private final LogManager logger;
  private final DssBlockAnalysis analysis;
  private final BlockGraph blockGraph;
  private final ImmutableMap<String, BlockNode> idToNode;

  private final Modification modification;

  /** from how many messages we collected information */
  private int messages;

  public DssWitnessArgStateCollector(
      DssAnalysisOptions options,
      BlockGraph pBlockGraph,
      Modification pModification,
      Specification spec,
      LogManager pLogger)
      throws InvalidConfigurationException, IOException, CPAException, InterruptedException {

    Configuration forwardConfiguration =
        Configuration.builder().loadFromFile(options.getForwardConfiguration()).build();
    analysis =
        new DssBlockAnalysis(
            LogManager.createNullLogManager(),
            pBlockGraph.getRoot(),
            pModification.cfa(),
            spec,
            forwardConfiguration,
            options,
            new DssMessageFactory(options),
            ShutdownManager.create());

    blockGraph = pBlockGraph;
    idToNode = Maps.uniqueIndex(pBlockGraph.getNodes(), BlockNode::getId);
    logger = pLogger;
    modification = pModification;
  }

  public void collectFromMessage(DssStatisticsMessage message) {

    messages++;

    BlockNode senderBlock = Preconditions.checkNotNull(idToNode.get(message.getSenderId()));

    if (senderBlock.getInitialLocation().isLoopStart()) {

      allStatsContainedStates &= message.getNumberOfContainedStates().isPresent();

      if (allStatsContainedStates) {
        Collection<ARGState> states;
        try {
          states =
              transformedImmutableListCopy(
                  analysis.deserialize(message),
                  sp -> AbstractStates.extractStateByType(sp.state(), ARGState.class));
          collectedLoopHeadPreconditions.putAll(senderBlock.getInitialLocation(), states);
        } catch (InterruptedException e) {
          allStatsContainedStates = false;
          logger.logUserException(
              Level.WARNING, e, "Could not collect states for witness due to interruption");
        }
      }
    }
  }

  public boolean receivedAllStates() {
    // We need to make sure that if we add some ARG states for a CFA node, we add all possible ARG
    // states to stay sound
    // currently this is the case: we only collect the information for loop heads, which will always
    // be entry nodes and never in the middle of one block
    // also, we ensure that we waited for all blocks to return a statistic message
    return allStatsContainedStates && blockGraph.getNodes().size() == messages;
  }

  /**
   * Converts the collected information to the original CFA, so that the parsing information can be
   * used by the witness export
   *
   * <p>TODO will just mapping the keys be enough or not?
   */
  private ImmutableListMultimap<CFANode, ARGState> convertToOriginalCFA(
      Multimap<CFANode, ARGState> toConvert) {

    ImmutableListMultimap.Builder<CFANode, ARGState> builder =
        ImmutableListMultimap.builderWithExpectedKeys(toConvert.keySet().size());

    BiMap<CFANode, CFANode> instrumentedToOriginal =
        modification.metadata().mappingInfo().originalToInstrumentedNodes().inverse();
    for (Entry<CFANode, Collection<ARGState>> entry : toConvert.asMap().entrySet()) {
      CFANode original = instrumentedToOriginal.get(entry.getKey());
      // We should never have collect states for nodes inserted by the modification
      Preconditions.checkNotNull(original);
      builder.putAll(original, entry.getValue());
    }

    return builder.build();
  }

  @Override
  public CollectedARGStates getRelevantStates(ARGState pRootState) {
    return new CollectedARGStates(
        convertToOriginalCFA(collectedLoopHeadPreconditions),
        ImmutableListMultimap.of(),
        ImmutableListMultimap.of(),
        ImmutableListMultimap.of());
  }
}
