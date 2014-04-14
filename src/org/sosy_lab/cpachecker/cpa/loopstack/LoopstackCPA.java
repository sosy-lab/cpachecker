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
package org.sosy_lab.cpachecker.cpa.loopstack;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFASingleLoopTransformation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.InvalidCFAException;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableCollection;

@Options(prefix="cpa.loopstack")
public class LoopstackCPA extends AbstractCPA implements ReachedSetAdjustingCPA {

  private final LogManager logger;

  @Option(description="threshold for unrolling loops of the program (0 is infinite)\n"
  + "works only if assumption storage CPA is enabled, because otherwise it would be unsound")
  private int maxLoopIterations = 0;

  @Option(description="this option controls how the maxLoopIterations condition is adjusted when a condition adjustment is invoked.")
  private MaxLoopIterationAdjusters maxLoopIterationAdjusterFactory = MaxLoopIterationAdjusters.STATIC;

  @Option(description="threshold for adjusting the threshold for unrolling loops of the program (0 is infinite).\n"
  + "only relevant in combination with a non-static maximum loop iteration adjuster.")
  private int maxLoopIterationsUpperBound = 0;

  private final DelegatingTransferRelation transferRelation;

  public static CPAFactory factory() {
    return new LoopstackCPAFactory();
  }

  private final CFA cfa;

  public LoopstackCPA(Configuration config, CFA pCfa, LogManager pLogger) throws InvalidConfigurationException, CPAException {
    this(config, pCfa, pLogger, new DelegatingTransferRelation());
  }

  private LoopstackCPA(Configuration config, CFA pCfa, LogManager pLogger, DelegatingTransferRelation pDelegatingTransferRelation) throws InvalidConfigurationException, InvalidCFAException {
    super("sep", "sep", pDelegatingTransferRelation);
    config.inject(this);
    this.transferRelation = pDelegatingTransferRelation;
    this.transferRelation.setDelegate(new LoopstackTransferRelation(maxLoopIterations, pCfa));
    this.logger = pLogger;
    cfa = pCfa;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    if (pNode instanceof FunctionEntryNode) {
      // shortcut for the common case, a function start node can never be in a loop
      // (loops don't span across functions)
      return new LoopstackState();
    }

    Loop loop = null;
    Set<String> functionNames = new HashSet<>();
    functionNames.add(pNode.getFunctionName());
    functionNames.add(CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME);
    for (String functionName : functionNames) {
      ImmutableCollection<Loop> loops = cfa.getLoopStructure().get().get(functionName);
      if (loops != null) {
        for (Loop l : loops) {
          if (l.getLoopNodes().contains(pNode)) {
            Preconditions.checkState(loop == null, "Cannot create initial nodes for locations in nested loops");
            loop = l;
          }
        }
        if (loop != null) {
          break;
        }
      }
    }

    LoopstackState e = new LoopstackState(); // the bottom element of the stack

    if (loop != null) {
      // if loop is present, push one element on the stack for it
      e = new LoopstackState(e, loop, 0, false);
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
      maxLoopIterations = maxLoopIterationAdjuster.adjust(maxLoopIterations);
      logger.log(Level.INFO, "Adjusting maxLoopIterations to " + maxLoopIterations);
      try {
        this.transferRelation.setDelegate(new LoopstackTransferRelation(maxLoopIterations, this.cfa));
      } catch (InvalidCFAException e) {
        logger.logException(Level.WARNING, e,
            "Exception while trying to adjust the maximum amount of loop iterations.");
        return false;
      }
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
        LoopstackState loopstackState = extractStateByType(pArg0, LoopstackState.class);
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

    List<Precision> waitlistPrecisions = new ArrayList<>(waitlist.size());
    for (AbstractState s : waitlist) {
      waitlistPrecisions.add(pReachedSet.getPrecision(s));
    }

    pReachedSet.removeAll(waitlist);

    // Add the new waitlist
    pReachedSet.addAll(Pair.zipList(waitlist, waitlistPrecisions));
    pReachedSet.removeAll(toRemove);
  }

  private static interface MaxLoopIterationAdjuster {

    int adjust(int currentValue);

    boolean canAdjust(int currentValue);

  }

  private static interface MaxLoopIterationAdjusterFactory {

    MaxLoopIterationAdjuster getMaxLoopIterationAdjuster(LoopstackCPA pCPA);

  }

  private static enum MaxLoopIterationAdjusters implements MaxLoopIterationAdjusterFactory {

    STATIC {

      @Override
      public MaxLoopIterationAdjuster getMaxLoopIterationAdjuster(LoopstackCPA pCPA) {
        return StaticLoopIterationAdjuster.INSTANCE;
      }

    },

    INCREMENT {

      @Override
      public MaxLoopIterationAdjuster getMaxLoopIterationAdjuster(LoopstackCPA pCPA) {
        return new IncrementalLoopIterationAdjuster(pCPA);
      }

    },

    DOUBLE {

      @Override
      public MaxLoopIterationAdjuster getMaxLoopIterationAdjuster(LoopstackCPA pCPA) {
        return new DoublingLoopIterationAdjuster(pCPA);
      }


    };

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

    private final LoopstackCPA cpa;

    public IncrementalLoopIterationAdjuster(LoopstackCPA pCPA) {
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

    private final LoopstackCPA cpa;

    public DoublingLoopIterationAdjuster(LoopstackCPA pCPA) {
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

  private static class DelegatingTransferRelation implements TransferRelation {

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
    public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState pState, Precision pPrecision,
        CFAEdge pCfaEdge) throws CPATransferException, InterruptedException {
      Preconditions.checkState(delegate != null);
      return this.delegate.getAbstractSuccessors(pState, pPrecision, pCfaEdge);
    }

    @Override
    public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
        CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
      return this.delegate.strengthen(pState, pOtherStates, pCfaEdge, pPrecision);
    }

  }
}