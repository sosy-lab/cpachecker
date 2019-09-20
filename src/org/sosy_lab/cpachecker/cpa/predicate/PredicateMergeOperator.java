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

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractEdge.FormulaDescription;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

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
  private final BooleanFormulaManager mngr;
  private final PredicateStatistics statistics;
  private final TimerWrapper totalMergeTimer;

  private final boolean abstractionLattice;
  private final boolean joinEffectsIntoUndef;

  public PredicateMergeOperator(
      LogManager pLogger,
      BooleanFormulaManager pMngr,
      PathFormulaManager pPfmgr,
      PredicateStatistics pStatistics,
      boolean pAbstractionLattice,
      boolean pJoin) {
    logger = pLogger;
    formulaManager = pPfmgr;
    statistics = pStatistics;
    mngr = pMngr;
    totalMergeTimer = statistics.totalMergeTime.getNewTimer();
    abstractionLattice = pAbstractionLattice;
    joinEffectsIntoUndef = pJoin;
  }

  @Override
  public AbstractState merge(AbstractState element1,
                               AbstractState element2, Precision precision) throws InterruptedException {

    if (element1 instanceof PredicateProjectedState) {

      PredicateProjectedState e1 = (PredicateProjectedState) element1;
      PredicateProjectedState e2 = (PredicateProjectedState) element2;

      BooleanFormula abs1 = e1.getGuard();
      BooleanFormula abs2 = e2.getGuard();

      AbstractEdge newEdge;
      BooleanFormula newAbs;

      try {
        totalMergeTimer.start();
        if (abstractionLattice) {
          if (!abs1.equals(abs2)) {
            return e2;
          } else {
            newAbs = abs2;
          }
        } else {
          if (mngr.isTrue(abs2)) {
            newAbs = abs2;
          } else if (mngr.isTrue(abs1)) {
            newAbs = abs1;
          } else {
            newAbs = mngr.or(abs1, abs2);
          }
        }

        AbstractEdge edge1 = e1.getAbstractEdge();
        AbstractEdge edge2 = e2.getAbstractEdge();
        newEdge = mergeEdges(edge1, edge2);

        if (newAbs == abs2 && newEdge == edge2) {
          return e2;
        } else {
          return new PredicateProjectedState(newEdge, newAbs);
        }
      } finally {
        totalMergeTimer.stop();
      }

    } else {

      PredicateAbstractState elem1 = (PredicateAbstractState) element1;
      PredicateAbstractState elem2 = (PredicateAbstractState) element2;

      PredicateAbstractState merged = merge(elem1, elem2);

      if (merged == elem2) {
        // Independently from class
        return elem2;
      }

      if (elem1.getClass() == elem2.getClass()) {
        if (elem1 instanceof AbstractStateWithEdge || elem2 instanceof AbstractStateWithEdge) {
          throw new UnsupportedOperationException("Should not be mergable");
        } else {
          return merged;
        }
      } else {
        return elem2;
      }
    }
  }

  private AbstractEdge mergeEdges(AbstractEdge edge1, AbstractEdge edge2) {

    if (edge1 == EmptyEdge.getInstance()) {
      return edge2;
    } else if (edge2 == EmptyEdge.getInstance()) {
      return edge1;
    }

    assert edge1 instanceof PredicateAbstractEdge && edge2 instanceof PredicateAbstractEdge;

    if (edge1 == PredicateAbstractEdge.getHavocEdgeInstance()
        || edge2 == PredicateAbstractEdge.getHavocEdgeInstance()) {
      return PredicateAbstractEdge.getHavocEdgeInstance();
    }

    PredicateAbstractEdge predEdge1 = ((PredicateAbstractEdge) edge1);
    PredicateAbstractEdge predEdge2 = ((PredicateAbstractEdge) edge2);
    Collection<FormulaDescription> desc1 = predEdge1.getFormulas();
    Collection<FormulaDescription> desc2 = predEdge2.getFormulas();

    Collection<CAssignment> formulas2 =
        from(desc2).transform(FormulaDescription::getAssignment).toList();

    Collection<FormulaDescription> newDesc1 =
        from(desc1)
            .filter(s -> !formulas2.contains(s.getAssignment()))
            .toSet();

    if (newDesc1.isEmpty()) {
      return edge2;
    } else {

      if (joinEffectsIntoUndef) {
        Collection<CAssignment> newFormulas1 =
            from(newDesc1).transform(FormulaDescription::getAssignment).toSet();
        Set<CLeftHandSide> newAssignments1 =
            from(newFormulas1).transform(CAssignment::getLeftHandSide).toSet();
        Set<CLeftHandSide> assignments2 =
            from(formulas2).transform(CAssignment::getLeftHandSide).toSet();
        Set<CLeftHandSide> commonPart = Sets.intersection(newAssignments1, assignments2);
        if (commonPart.isEmpty()) {
          Collection<CAssignment> newFormulas = Sets.newHashSet(formulas2);
          newFormulas.addAll(newFormulas1);
          return new PredicateAbstractEdge(null);
        } else {
          Collection<CAssignment> newFormulas = new HashSet<>();
          copyFormulas(newFormulas, newFormulas1, commonPart);
          copyFormulas(newFormulas, formulas2, commonPart);

          boolean newFormulasFound = false;
          // Base on the second to be close to merge def.
          for (CAssignment asgn : formulas2) {
            CLeftHandSide left = asgn.getLeftHandSide();
            if (commonPart.contains(left)) {
              CRightHandSide right = asgn.getRightHandSide();
              if (right.toASTString().contains("__VERIFIER_nondet")) {
                // TODO make it with a special class
                // Already nondet function, skip
              } else {
                CFunctionCallExpression fExp =
                    PredicateApplyOperator.prepareUndefFunctionFor(right);

                CAssignment newAssignement =
                    new CFunctionCallAssignmentStatement(fExp.getFileLocation(), left, fExp);
                newFormulas.add(newAssignement);
                newFormulasFound = true;
              }
            }
          }

          if (commonPart.equals(newAssignments1) && !newFormulasFound) {
            // Common part is covered
            return edge2;
          }

          return new PredicateAbstractEdge(null);
        }

      } else {
        Collection<FormulaDescription> newFormulas = Sets.newHashSet(desc2);
        newFormulas.addAll(newDesc1);
        return new PredicateAbstractEdge(newFormulas);
      }
    }
  }

  private void copyFormulas(
      Collection<CAssignment> result,
      Collection<CAssignment> origin,
      Set<CLeftHandSide> toSkip) {
    for (CAssignment asgn : origin) {
      CLeftHandSide left = asgn.getLeftHandSide();
      if (!toSkip.contains(left)) {
        result.add(asgn);
      }
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
