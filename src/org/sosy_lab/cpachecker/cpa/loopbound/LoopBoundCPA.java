/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.loopbound;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationBounding;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "cpa.loopbound")
public class LoopBoundCPA extends AbstractCPA
    implements ReachedSetAdjustingCPA, StatisticsProvider, Statistics, LoopIterationBounding {

  @Option(secure=true,
      description="Number of loop iterations before the loop counter is"
          + " abstracted. Zero is equivalent to no limit.")
  private int loopIterationsBeforeAbstraction = 0;

  @Option(secure=true, description="enable stack-based tracking of loops")
  private boolean trackStack = false;

  private final DelegatingTransferRelation transferRelation;

  public static CPAFactory factory() {
    return new LoopBoundCPAFactory();
  }

  private final LoopStructure loopStructure;

  private final LoopBoundPrecisionAdjustment precisionAdjustment;

  public LoopBoundCPA(Configuration config, CFA pCfa, LogManager pLogger) throws InvalidConfigurationException, CPAException {
    this(config, pCfa, pLogger, new DelegatingTransferRelation());
  }

  private LoopBoundCPA(Configuration pConfig, CFA pCfa, LogManager pLogger, DelegatingTransferRelation pDelegatingTransferRelation) throws InvalidConfigurationException, CPAException {
    super("sep", "sep", LoopBoundDomain.INSTANCE, pDelegatingTransferRelation);
    if (!pCfa.getLoopStructure().isPresent()) {
      throw new CPAException("LoopBoundCPA cannot work without loop-structure information in CFA.");
    }
    loopStructure = pCfa.getLoopStructure().get();
    pConfig.inject(this);
    this.transferRelation = pDelegatingTransferRelation;
    this.transferRelation.setDelegate(new LoopBoundTransferRelation(
        loopIterationsBeforeAbstraction, loopStructure));
    precisionAdjustment = new LoopBoundPrecisionAdjustment(pConfig, pLogger);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    LoopBoundState initialState = new LoopBoundState();
    Set<Loop> loopsAtLocation = loopStructure.getLoopsForLoopHead(pNode);
    for (Loop loop : loopsAtLocation) {
      initialState = initialState.visitLoopHead(new LoopEntry(pNode, loop));
    }
    return initialState;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new LoopBoundPrecision(trackStack, precisionAdjustment.getMaxLoopIterations());
  }

  @Override
  public TransferRelation getTransferRelation() {
    if (this.transferRelation == null) {
      return super.getTransferRelation();
    }
    return this.transferRelation;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment ;
  }

  @Override
  public boolean adjustPrecision() {
    return precisionAdjustment.nextState();
  }

  @Override
  public void adjustReachedSet(final ReachedSet pReachedSet) {
    Set<AbstractState> toRemove = from(pReachedSet).filter(new Predicate<AbstractState>() {

      @Override
      public boolean apply(@Nullable AbstractState pArg0) {
        if (pArg0 == null) {
          return false;
        }
        LoopBoundState loopBoundState = extractStateByType(pArg0, LoopBoundState.class);
        return loopBoundState != null && loopBoundState.mustDumpAssumptionForAvoidance();
      }}).toSet();

    // Never delete the first state
    if (toRemove.contains(pReachedSet.getFirstState())) {
      pReachedSet.clear();
      return;
    }

    List<AbstractState> waitlist = from(toRemove).transformAndConcat(new Function<AbstractState, Iterable<? extends AbstractState>>() {

      @Override
      public Iterable<? extends AbstractState> apply(@Nullable AbstractState pArg0) {
        if (pArg0 == null) {
          return Collections.emptyList();
        }
        ARGState argState = extractStateByType(pArg0, ARGState.class);
        if (argState == null) {
          return Collections.emptyList();
        }
        return argState.getParents();
      }

    }).toSet().asList();

    // Add the new waitlist
    for (AbstractState s : waitlist) {
      pReachedSet.reAddToWaitlist(s);
    }

    pReachedSet.removeAll(toRemove);
    for (ARGState s : from(toRemove).filter(ARGState.class)) {
      s.removeFromARG();
    }
  }

  private static class DelegatingTransferRelation extends SingleEdgeTransferRelation {

    private TransferRelation delegate = null;

    public DelegatingTransferRelation() {
      this(null);
    }

    public DelegatingTransferRelation(TransferRelation pDelegate) {
      this.delegate = pDelegate;
    }

    public void setDelegate(TransferRelation pNewDelegate) {
      this.delegate = pNewDelegate;
    }

    @Override
    public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
        AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
            throws CPATransferException, InterruptedException {
      Preconditions.checkState(delegate != null);
      return this.delegate.getAbstractSuccessorsForEdge(pState, pPrecision, pCfaEdge);
    }

    @Override
    public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
        CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
      return this.delegate.strengthen(pState, pOtherStates, pCfaEdge, pPrecision);
    }

  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
    writer.put("Bound k", precisionAdjustment.getMaxLoopIterations());
    int maximumLoopIterationReached = 0;
    for (AbstractState state : pReached) {
      LoopBoundState loopstackState = AbstractStates.extractStateByType(state, LoopBoundState.class);
      if (loopstackState != null) {
        maximumLoopIterationReached = Math.max(maximumLoopIterationReached, loopstackState.getDeepestIteration());
      }
    }
    writer.put("Maximum loop iteration reached", maximumLoopIterationReached);
    writer.spacer();
  }

  @Override
  public String getName() {
    return "Bounds CPA";
  }

  @Override
  public void setMaxLoopIterations(int pMaxLoopIterations) {
    precisionAdjustment.setMaxLoopIterations(pMaxLoopIterations);
    this.transferRelation.setDelegate(new LoopBoundTransferRelation(
        loopIterationsBeforeAbstraction,
        loopStructure));
  }

  @Override
  public int getMaxLoopIterations() {
    return precisionAdjustment.getMaxLoopIterations();
  }

  public void incrementLoopIterationsBeforeAbstraction() {
    loopIterationsBeforeAbstraction++;
    this.transferRelation.setDelegate(new LoopBoundTransferRelation(
        loopIterationsBeforeAbstraction,
        loopStructure));
  }
}