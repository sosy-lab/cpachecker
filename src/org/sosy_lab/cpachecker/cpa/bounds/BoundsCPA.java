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
package org.sosy_lab.cpachecker.cpa.bounds;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

@Options(prefix="cpa.bounds")
public class BoundsCPA extends AbstractCPA implements ReachedSetAdjustingCPA, StatisticsProvider, Statistics {

  private final LogManager logger;

  @Option(secure=true,
      description="Number of loop iterations before the loop counter is"
          + " abstracted. Zero is equivalent to no limit.")
  private int loopIterationsBeforeAbstraction = 0;

  @Option(secure=true, description="threshold for unrolling loops of the program (0 is infinite)\n"
  + "works only if assumption storage CPA is enabled, because otherwise it would be unsound")
  private int maxLoopIterations = 0;

  @Option(secure=true, description="threshold for unwinding recursion (0 is infinite)\n"
  + "works only if assumption storage CPA is enabled, because otherwise it would be unsound")
  private int maxRecursionDepth = 0;

  @Option(secure=true, description="this option controls how the maxLoopIterations condition is adjusted when a condition adjustment is invoked.")
  private MaxLoopIterationAdjusters maxLoopIterationAdjusterFactory = MaxLoopIterationAdjusters.STATIC;

  @Option(secure=true, description="threshold for adjusting the threshold for unrolling loops of the program (0 is infinite).\n"
  + "only relevant in combination with a non-static maximum loop iteration adjuster.")
  private int maxLoopIterationsUpperBound = 0;

  private final DelegatingTransferRelation transferRelation;

  public static CPAFactory factory() {
    return new BoundsCPAFactory();
  }

  private final LoopStructure loopStructure;

  public BoundsCPA(Configuration config, CFA pCfa, LogManager pLogger) throws InvalidConfigurationException, CPAException {
    this(config, pCfa, pLogger, new DelegatingTransferRelation());
  }

  private BoundsCPA(Configuration config, CFA pCfa, LogManager pLogger, DelegatingTransferRelation pDelegatingTransferRelation) throws InvalidConfigurationException, CPAException {
    super("sep", "sep", pDelegatingTransferRelation);
    if (!pCfa.getLoopStructure().isPresent()) {
      throw new CPAException("UnifiedLoopstackCPA cannot work without loop-structure information in CFA.");
    }
    loopStructure = pCfa.getLoopStructure().get();
    config.inject(this);
    this.transferRelation = pDelegatingTransferRelation;
    this.transferRelation.setDelegate(new BoundsTransferRelation(
        loopIterationsBeforeAbstraction, maxLoopIterations, maxRecursionDepth, loopStructure));
    this.logger = pLogger;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    if (pNode instanceof FunctionEntryNode) {
      // shortcut for the common case, a function start node can never be in a loop
      // (loops don't span across functions)
      return new BoundsState();
    }

    BoundsState e = new BoundsState(); // the bottom element of the stack

    for (Loop loop : loopStructure.getAllLoops()) {
      if (loop.getLoopHeads().contains(pNode)) {
        e = e.enter(loop);
      }
    }
    return e;
  }

  @Override
  public TransferRelation getTransferRelation() {
    if (this.transferRelation == null) {
      return super.getTransferRelation();
    }
    return this.transferRelation;
  }

  @Override
  public boolean adjustPrecision() {
    MaxLoopIterationAdjuster maxLoopIterationAdjuster = this.maxLoopIterationAdjusterFactory.getMaxLoopIterationAdjuster(this);
    if (maxLoopIterationAdjuster.canAdjust(maxLoopIterations)) {
      int maxLoopIterations = maxLoopIterationAdjuster.adjust(this.maxLoopIterations);
      logger.log(Level.INFO, "Adjusting maxLoopIterations to " + maxLoopIterations);
      setMaxLoopIterations(maxLoopIterations);
      return true;
    }
    return false;
  }

  @Override
  public void adjustReachedSet(final ReachedSet pReachedSet) {
    Set<AbstractState> toRemove = from(pReachedSet).filter(new Predicate<AbstractState>() {

      @Override
      public boolean apply(@Nullable AbstractState pArg0) {
        if (pArg0 == null) {
          return false;
        }
        BoundsState loopstackState = extractStateByType(pArg0, BoundsState.class);
        return loopstackState != null && loopstackState.mustDumpAssumptionForAvoidance();
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

    pReachedSet.removeAll(toRemove);
    for (ARGState s : from(toRemove).filter(ARGState.class)) {
      s.removeFromARG();
    }

    // Add the new waitlist
    for (AbstractState s : waitlist) {
      pReachedSet.reAddToWaitlist(s);
    }
  }

  private static interface MaxLoopIterationAdjuster {

    int adjust(int currentValue);

    boolean canAdjust(int currentValue);

  }

  private static interface MaxLoopIterationAdjusterFactory {

    MaxLoopIterationAdjuster getMaxLoopIterationAdjuster(BoundsCPA pCPA);

  }

  private static enum MaxLoopIterationAdjusters implements MaxLoopIterationAdjusterFactory {

    STATIC {

      @Override
      public MaxLoopIterationAdjuster getMaxLoopIterationAdjuster(BoundsCPA pCPA) {
        return StaticLoopIterationAdjuster.INSTANCE;
      }

    },

    INCREMENT {

      @Override
      public MaxLoopIterationAdjuster getMaxLoopIterationAdjuster(BoundsCPA pCPA) {
        return new IncrementalLoopIterationAdjuster(pCPA);
      }

    },

    DOUBLE {

      @Override
      public MaxLoopIterationAdjuster getMaxLoopIterationAdjuster(BoundsCPA pCPA) {
        return new DoublingLoopIterationAdjuster(pCPA);
      }


    }

  }

  private static enum StaticLoopIterationAdjuster implements MaxLoopIterationAdjuster {

    INSTANCE;

    @Override
    public int adjust(int pCurrentValue) {
      return pCurrentValue;
    }

    @Override
    public boolean canAdjust(int pCurrentValue) {
      return false;
    }

  }

  private static class IncrementalLoopIterationAdjuster implements MaxLoopIterationAdjuster {

    private final BoundsCPA cpa;

    public IncrementalLoopIterationAdjuster(BoundsCPA pCPA) {
      this.cpa = pCPA;
    }

    @Override
    public int adjust(int pCurrentValue) {
      return ++pCurrentValue;
    }

    @Override
    public boolean canAdjust(int pCurrentValue) {
      return cpa.maxLoopIterationsUpperBound <= 0 || pCurrentValue < cpa.maxLoopIterationsUpperBound;
    }

  }

  private static class DoublingLoopIterationAdjuster implements MaxLoopIterationAdjuster {

    private final BoundsCPA cpa;

    public DoublingLoopIterationAdjuster(BoundsCPA pCPA) {
      this.cpa = pCPA;
    }

    @Override
    public int adjust(int pCurrentValue) {
      return 2 * pCurrentValue;
    }

    @Override
    public boolean canAdjust(int pCurrentValue) {
      return cpa.maxLoopIterationsUpperBound <= 0 || pCurrentValue * 2 <= cpa.maxLoopIterationsUpperBound;
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
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
    writer.put("Bound k", this.maxLoopIterations);
    int maximumLoopIterationReached = 0;
    for (AbstractState state : pReached) {
      BoundsState loopstackState = AbstractStates.extractStateByType(state, BoundsState.class);
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

  public void setMaxLoopIterations(int pMaxLoopIterations) {
    this.maxLoopIterations = pMaxLoopIterations;
    this.transferRelation.setDelegate(new BoundsTransferRelation(
        loopIterationsBeforeAbstraction,
        maxLoopIterations, maxRecursionDepth, loopStructure));
  }

  public int getMaxLoopIterations() {
    return this.maxLoopIterations;
  }
}