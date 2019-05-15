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
package org.sosy_lab.cpachecker.cpa.predicate;

import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula;

import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

/**
 * Merge operator for symbolic predicate abstraction.
 * This is not a trivial merge operator in the sense that it implements
 * mergeSep and mergeJoin together. If the abstract state is on an
 * abstraction location we don't merge, otherwise we merge two elements
 * and update the {@link PredicateAbstractState}'s pathFormula.
 */
public class PredicateMergeOperator implements MergeOperator {

  private final LogManager logger;
  private final PathFormulaManager formulaManager;
  private final PredicateStatistics statistics;
  private final TimerWrapper totalMergeTimer;

  public PredicateMergeOperator(
      LogManager pLogger, PathFormulaManager pPfmgr, PredicateStatistics pStatistics) {
    logger = pLogger;
    formulaManager = pPfmgr;
    statistics = pStatistics;
    totalMergeTimer = statistics.totalMergeTime.getNewTimer();
  }

  @Override
  public AbstractState merge(AbstractState element1,
                               AbstractState element2, Precision precision) throws InterruptedException {

    PredicateAbstractState elem1 = (PredicateAbstractState)element1;
    PredicateAbstractState elem2 = (PredicateAbstractState)element2;

    PredicateAbstractState merged = merge(elem1, elem2);

    if (merged == elem2) {
      // Independently from class
      return elem2;
    }

    if (elem1.getClass() == elem2.getClass()) {
      if (elem1 instanceof AbstractStateWithEdge && elem2 instanceof AbstractStateWithEdge) {
        AbstractEdge edge1 = ((AbstractStateWithEdge) elem1).getAbstractEdge();
        AbstractEdge edge2 = ((AbstractStateWithEdge) elem2).getAbstractEdge();

        if (edge1 instanceof PredicateAbstractEdge && edge2 instanceof PredicateAbstractEdge) {
          Collection<CAssignment> formulas1 = ((PredicateAbstractEdge) edge1).getAssignments();
          Collection<CAssignment> formulas2 = ((PredicateAbstractEdge) edge2).getAssignments();

          PredicateAbstractEdge newEdge;

          if (formulas2.containsAll(formulas1)) {
            newEdge = (PredicateAbstractEdge) edge2;
          } else {
            Collection<CAssignment> newFormulas = new HashSet<>();
            newFormulas.addAll(formulas1);
            newFormulas.addAll(formulas2);
            newEdge = new PredicateAbstractEdge(newFormulas);
          }

          return copyStateWithEdge(elem2, merged, newEdge);
        } else if (edge1.equals(edge2)) {
          return copyStateWithEdge(elem2, merged, edge2);
        } else {
          return elem2;
        }
      } else {
        return merged;
      }
    } else {
      return elem2;
    }
  }

  private PredicateAbstractState copyStateWithEdge(
      PredicateAbstractState base,
      PredicateAbstractState merged,
      AbstractEdge edge) {
    if (base instanceof PredicateNonAbstractionStateWithEdge) {
      return new PredicateNonAbstractionStateWithEdge(merged, edge);
    } else if (base instanceof PredicateAbstractionStateWithEdge) {
      return new PredicateNonAbstractionStateWithEdge(merged, edge);
    } else {
      throw new UnsupportedOperationException("Unknown state with edge: " + base.getClass());
    }
  }

  private PredicateAbstractState
      merge(PredicateAbstractState pState1, PredicateAbstractState pState2)
          throws InterruptedException {
    // this will be the merged element
    PredicateAbstractState merged;

    if (pState1.isAbstractionState() || pState2.isAbstractionState()) {
      // we don't merge if this is an abstraction location
      merged = pState2;
    } else {
      // don't merge if the elements are in different blocks (they have different abstraction formulas)
      // or if the path formulas are equal (no new information would be added)
      if (!pState1.getAbstractionFormula().equals(pState2.getAbstractionFormula())
          || pState1.getPathFormula().equals(pState2.getPathFormula())) {
        merged = pState2;

      } else {
        totalMergeTimer.start();
        assert pState1.getAbstractionLocationsOnPath()
            .equals(pState2.getAbstractionLocationsOnPath());
        // create a new state

        logger.log(Level.FINEST, "Merging two non-abstraction nodes.");

        PathFormula pathFormula =
            formulaManager.makeOr(pState1.getPathFormula(), pState2.getPathFormula());

        logger.log(Level.ALL, "New path formula is", pathFormula);

        merged = mkNonAbstractionStateWithNewPathFormula(pathFormula, pState1);

        // now mark elem1 so that coverage check can find out it was merged
        pState1.setMergedInto(merged);

        totalMergeTimer.stop();
      }
    }

    return merged;
  }

}
