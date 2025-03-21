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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

/**
 * Merge operator for symbolic predicate abstraction. This is not a trivial merge operator in the
 * sense that it implements mergeSep and mergeJoin together. If the abstract state is on an
 * abstraction location we don't merge, otherwise we merge two elements and update the {@link
 * PredicateAbstractState}'s pathFormula.
 */
@Options(prefix = "cpa.predicate.merge")
final class PredicateMergeOperator implements MergeOperator {

  @Option(
      secure = true,
      name = "mergeAbstractionStatesWithSamePredecessor",
      description =
          "merge two abstraction states if their preceeding abstraction states are the same")
  private boolean mergeAbstractionStates = false;

  private final LogManager logger;
  private final PathFormulaManager formulaManager;

  private final PredicateAbstractionManager predAbsManager;

  // Statistics
  final StatTimer totalMergeTime = new StatTimer("Time for merge operator");

  PredicateMergeOperator(
      Configuration pConfig,
      LogManager pLogger,
      PathFormulaManager pPfmgr,
      PredicateAbstractionManager pPredAbsManager)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    formulaManager = pPfmgr;

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
        totalMergeTime.start();
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
        totalMergeTime.stop();
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
        totalMergeTime.start();
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

        totalMergeTime.stop();
      }
    }

    return merged;
  }
}
