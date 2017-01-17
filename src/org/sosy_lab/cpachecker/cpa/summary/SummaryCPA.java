/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.summary;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.blocks.BlockManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Top-level summary CPA.
 */
public class SummaryCPA implements ConfigurableProgramAnalysis,
                                   WrapperCPA,
                                   StatisticsProvider,
                                   TransferRelation,
                                   MergeOperator {

  private final CPAWithSummarySupport wrapped;
  private final SummaryManager wrappedSummaryManager;
  private final Multimap<CFANode, AbstractState> mapping;
  private final TransferRelation wrappedTransferRelation;
  private final MergeOperator wrappedMergeOperator;
  private final BlockManager blockManager;

  public SummaryCPA(
      ConfigurableProgramAnalysis pWrapped,
      CFA pCFA,
      Configuration pConfiguration,
      LogManager pLogger) throws InvalidConfigurationException, CPATransferException {

    wrapped = (CPAWithSummarySupport) pWrapped;
    blockManager = new BlockManager(pCFA, pConfiguration, pLogger);
    wrapped.setBlockManager(blockManager);
    wrappedSummaryManager = wrapped.getSimpleSummaryManager();
    wrappedTransferRelation = wrapped.getTransferRelation();
    wrappedMergeOperator = wrapped.getMergeOperator();
    mapping = HashMultimap.create();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return wrapped.getAbstractDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return this;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState state, Precision precision)
        throws CPATransferException, InterruptedException {

    try {
      return getAbstractSuccessors0(state, precision);
    } catch (CPAException pE) {
      throw new CPATransferException("Got CPAException: " + pE, pE);
    }
  }

  private  Collection<? extends AbstractState> getAbstractSuccessors0(
      AbstractState state, Precision precision)
      throws CPAException,
             InterruptedException {

    CFANode node = AbstractStates.extractLocation(state);
    mapping.put(node, state);

    Block block = blockManager.getBlockForNode(node);
    Optional<CFAEdge> callEdge = blockManager.findCallToBlock(node);

    if (callEdge.isPresent()) {
      List<AbstractState> out = new ArrayList<>();
      CFAEdge e = callEdge.get();
      Block calledBlock = blockManager.getBlockForNode(e.getSuccessor());

      // Update entry states.
      out.addAll(wrappedSummaryManager.getEntryStates(
          state,
          e.getPredecessor(),
          calledBlock));

      // Get existing summaries.
      for (AbstractState s : mapping.get(calledBlock.getExitNode())) {
        if (wrappedSummaryManager.isSummaryApplicable(
            state, s, node, calledBlock
        )) {
          out.addAll(wrappedSummaryManager.applyFunctionSummary(
              state, s, node, calledBlock
          ));
        }
      }

      return out;

    } else if (block.getExitNode() == node) {

      // Apply the newly generated summaries.
      Stream<CFANode> callsites =
          block.getCallEdges().stream().map(c -> c.getPredecessor());
      List<AbstractState> out = new ArrayList<>();

      for (CFAEdge cEdge : block.getCallEdges()) {
        CFANode predecessor = cEdge.getPredecessor();
        for (AbstractState callState : mapping.get(predecessor)) {
          if (wrappedSummaryManager.isSummaryApplicable(
              callState, state, AbstractStates.extractLocation(callState), block
          )) {
            out.addAll(
                wrappedSummaryManager.applyFunctionSummary(
                    callState, state, AbstractStates.extractLocation(callState), block
                )
            );
          }
        }
      }
      return out;

    } else {

      // Intraprocedural case.
      return wrappedTransferRelation.getAbstractSuccessors(state, precision);
    }
  }

  @Override
  public MergeOperator getMergeOperator() {
    return this;
  }

  @Override
  public AbstractState merge(
      AbstractState state1, AbstractState state2, Precision precision)
      throws CPAException, InterruptedException {
    AbstractState out = wrappedMergeOperator.merge(state1, state2, precision);
    CFANode loc1 = AbstractStates.extractLocation(state1);
    CFANode loc2 = AbstractStates.extractLocation(state2);
    if (loc1 == loc2 && out != state2) {
      mapping.get(loc1).remove(state2);
    }
    return out;
  }

  @Override
  public StopOperator getStopOperator() {
    return wrapped.getStopOperator();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return wrapped.getPrecisionAdjustment();
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return wrapped.getInitialState(node, partition);
  }

  @Override
  public Precision getInitialPrecision(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return wrapped.getInitialPrecision(node, partition);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    if (wrapped instanceof StatisticsProvider) {
      ((StatisticsProvider) wrapped).collectStatistics(statsCollection);
    }
  }

  @Nullable
  @Override
  public <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    } else if (pType.isAssignableFrom(wrapped.getClass())) {
      return pType.cast(wrapped);
    } else if (wrapped instanceof WrapperCPA) {
      return ((WrapperCPA) wrapped).retrieveWrappedCpa(pType);
    } else {
      return null;
    }
  }

  @Override
  public Iterable<ConfigurableProgramAnalysis> getWrappedCPAs() {
    return Collections.singleton(wrapped);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SummaryCPA.class);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    throw new UnsupportedOperationException("Unexpected call");
  }
}
