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
package org.sosy_lab.cpachecker.cpa.composite;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.NonMergeableAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.SolverException;

import java.util.Collections;
import java.util.Iterator;

public class CompositeMergeAgreeCPAEnabledAnalysisOperator implements MergeOperator {

  private static final Predicate<Object> NON_MERGEABLE_STATE = instanceOf(NonMergeableAbstractState.class);

  private final ImmutableList<MergeOperator> mergeOperators;
  private final ImmutableList<StopOperator> stopOperators;
  private final PredicateAbstractionManager abmgr;
  private Class<? extends AbstractState> enablerClass;
  private boolean isEnablerPredicate = false;

  CompositeMergeAgreeCPAEnabledAnalysisOperator(
      ImmutableList<MergeOperator> mergeOperators,
      ImmutableList<StopOperator> stopOperators,
      PredicateAbstractionManager pAbmgr) {
    this.mergeOperators = mergeOperators;
    this.stopOperators = stopOperators;
    this.abmgr = pAbmgr;
  }

  @Override
  public AbstractState merge(AbstractState successorState, AbstractState reachedState, Precision precision)
      throws CPAException,
      InterruptedException {
    // Merge Sep Code
    CompositeState compSuccessorState = (CompositeState) successorState;
    CompositeState compReachedState = (CompositeState) reachedState;
    CompositePrecision compPrecision = (CompositePrecision) precision;

    boolean mergeIfPredicateEnabler = false;

    assert (compSuccessorState.getNumberOfStates() == compReachedState.getNumberOfStates());

    AbstractState enablerReached = null;
    if (enablerClass != null) {
      enablerReached = AbstractStates.extractStateByType(compReachedState, enablerClass);
    }

    if (isEnablerPredicate) {
      PredicateAbstractState predSuccessorState =
          AbstractStates.extractStateByType(successorState, PredicateAbstractState.class);
      PredicateAbstractState predReachedState =
          AbstractStates.extractStateByType(reachedState, PredicateAbstractState.class);

      // check if have special case with same abstraction states then still merge in predicated analysis
      if (predSuccessorState != null && predReachedState != null && predSuccessorState.isAbstractionState()
          && predReachedState.isAbstractionState()) {
        // check if same abstraction state, test formula for equivalence
        try {
          if (predSuccessorState.getAbstractionFormula().asFormula()
                  == predReachedState.getAbstractionFormula().asFormula()
              || (abmgr.checkCoverage(
                      predSuccessorState.getAbstractionFormula(),
                      predReachedState.getAbstractionFormula())
                  && abmgr.checkCoverage(
                      predReachedState.getAbstractionFormula(),
                      predSuccessorState
                          .getAbstractionFormula()))) { // TODO do we need functional equivalence or is something else faster and sufficient?
            mergeIfPredicateEnabler = true;
          }
        } catch (SolverException e) {
          throw new CPAException("Solver Failure", e);
        }
      }
    }

    if (!mergeIfPredicateEnabler && (from(compSuccessorState.getWrappedStates()).anyMatch(NON_MERGEABLE_STATE)
        || from(compReachedState.getWrappedStates()).anyMatch(NON_MERGEABLE_STATE))) {
      // one CPA asks us to not merge at all
      return reachedState;
    }

    ImmutableList.Builder<AbstractState> mergedStates = ImmutableList.builder();
    Iterator<StopOperator> stopIter = stopOperators.iterator();
    Iterator<AbstractState> comp1Iter = compSuccessorState.getWrappedStates().iterator();
    Iterator<AbstractState> comp2Iter = compReachedState.getWrappedStates().iterator();
    Iterator<Precision> precIter = compPrecision.getWrappedPrecisions().iterator();
    boolean identicalStates = true;

    for (MergeOperator mergeOp : mergeOperators) {
      AbstractState absSuccessorState = comp1Iter.next();
      AbstractState absReachedState = comp2Iter.next();

      if (mergeIfPredicateEnabler && absReachedState instanceof PredicateAbstractState) {
        // TODO currently only save the first path to this state,
        // CEGAR in predicated analysis may take several rounds in case of loops, or maybe it may even fail (I do not know yet)
        // in future try to find a way to encode other paths also
        mergedStates.add(absReachedState);
        precIter.next();
        stopIter.next();
        continue;
      }

      Precision prec = precIter.next();
      StopOperator stopOp = stopIter.next();

      if (!isEnablerPredicate && absReachedState == enablerReached) {
        // check that both states are equal
        if (!(absSuccessorState.equals(absReachedState)
            || (stopOp.stop(absSuccessorState, Collections.singleton(absReachedState), prec) && stopOp.stop(
                absReachedState, Collections.singleton(absSuccessorState), prec)))) {
          // enabler states do not equal -> do not merge
          return reachedState;
        }
        mergedStates.add(absReachedState);
        continue;
      }

      AbstractState mergedState = mergeOp.merge(absSuccessorState, absReachedState, prec);

      // Check if 'mergedState' also covers 'absSuccessorState', i.e., if 'mergeOp' performed a join.
      // By definition of MergeOperator, we know it covers 'absReachedState'.
      if (!stopOp.stop(absSuccessorState, Collections.singleton(mergedState), prec)) {
        // the result of merge does not cover 'absSuccessorState'
        // (which is the successor state currently considered by the CPAAlgorithm
        // We prevent merging for all CPAs in this case, because the current successor
        // state would not be covered anyway, so widening other states is just a loss of precision.
        return reachedState;
      }

      if (mergedState != absReachedState) {
        identicalStates = false;
      }

      mergedStates.add(mergedState);
    }

    if (identicalStates) {
      return reachedState;
    } else {
      // TODO check if merge results in target state?
      return new CompositeState(mergedStates.build());
    }
  }

  public void setEnablerStateClass(Class<? extends AbstractState> pStateClass) {
    isEnablerPredicate = pStateClass.equals(PredicateAbstractState.class);
    enablerClass = pStateClass;
  }

}
