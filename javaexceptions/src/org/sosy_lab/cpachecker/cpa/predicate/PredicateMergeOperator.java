// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.mkAbstractionState;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula;

import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

/**
 * Merge operator for symbolic predicate abstraction. This is not a trivial merge operator in the
 * sense that it implements mergeSep and mergeJoin together. If the abstract state is on an
 * abstraction location we don't merge, otherwise we merge two elements and update the {@link
 * PredicateAbstractState}'s pathFormula.
 */
public class PredicateMergeOperator implements MergeOperator {

  private final LogManager logger;
  private final PathFormulaManager formulaManager;
  private final PredicateStatistics statistics;
  private final TimerWrapper totalMergeTimer;

  private boolean mergeAbstractionStates;
  private final PredicateAbstractionManager predAbsManager;

  public PredicateMergeOperator(
      LogManager pLogger,
      PathFormulaManager pPfmgr,
      PredicateStatistics pStatistics,
      boolean pMergeAbstractionStates,
      PredicateAbstractionManager pPredAbsManager) {
    logger = pLogger;
    formulaManager = pPfmgr;
    statistics = pStatistics;
    totalMergeTimer = statistics.totalMergeTime.getNewTimer();

    mergeAbstractionStates = pMergeAbstractionStates;
    predAbsManager = pPredAbsManager;
  }

  @Override
  public AbstractState merge(AbstractState element1, AbstractState element2, Precision precision)
      throws InterruptedException {

    PredicateAbstractState elem1 = (PredicateAbstractState) element1;
    PredicateAbstractState elem2 = (PredicateAbstractState) element2;

    // this will be the merged element
    PredicateAbstractState merged;

    if (mergeAbstractionStates
        && elem1.isAbstractionState()
        && elem2.isAbstractionState()
        && !elem1.getAbstractionFormula().equals(elem2.getAbstractionFormula())) {
      if (elem1.getPreviousAbstractionState().equals(elem2.getPreviousAbstractionState())) {
        totalMergeTimer.start();
        AbstractionFormula newAbstractionFormula =
            predAbsManager.makeOr(elem1.getAbstractionFormula(), elem2.getAbstractionFormula());
        PathFormula newPathFormula =
            formulaManager.makeEmptyPathFormulaWithContextFrom(
                newAbstractionFormula.getBlockFormula());
        merged =
            mkAbstractionState(
                newPathFormula,
                newAbstractionFormula,
                elem2.getAbstractionLocationsOnPath(),
                elem2.getPreviousAbstractionState());
        elem1.setMergedInto(merged);
        totalMergeTimer.stop();
        return merged;
      }
    }
    if (elem1.isAbstractionState() || elem2.isAbstractionState()) {
      // we don't merge if this is an abstraction location
      merged = elem2;
    } else {
      // don't merge if the elements are in different blocks (they have different abstraction
      // formulas)
      // or if the path formulas are equal (no new information would be added)
      if (!elem1.getAbstractionFormula().equals(elem2.getAbstractionFormula())
          || elem1.getPathFormula().equals(elem2.getPathFormula())) {
        merged = elem2;

      } else {
        totalMergeTimer.start();
        assert elem1.getAbstractionLocationsOnPath().equals(elem2.getAbstractionLocationsOnPath());
        // create a new state

        logger.log(Level.FINEST, "Merging two non-abstraction nodes.");

        PathFormula pathFormula =
            formulaManager.makeOr(elem1.getPathFormula(), elem2.getPathFormula());

        logger.log(Level.ALL, "New path formula is", pathFormula);

        merged =
            mkNonAbstractionStateWithNewPathFormula(
                pathFormula, elem1, elem2.getPreviousAbstractionState());

        // now mark elem1 so that coverage check can find out it was merged
        elem1.setMergedInto(merged);

        totalMergeTimer.stop();
      }
    }

    return merged;
  }
}
