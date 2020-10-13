// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.mkAbstractionState;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractEdge.FormulaDescription;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
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
  private boolean mergeAbstractionStates;
  private final PredicateAbstractionManager predAbsManager;

  public PredicateMergeOperator(
      LogManager pLogger,
      BooleanFormulaManager pMngr,
      PathFormulaManager pPfmgr,
      PredicateStatistics pStatistics,
      boolean pMergeAbstractionStates,
      PredicateAbstractionManager pPredAbsManager,
      boolean pAbstractionLattice,
      boolean pJoin) {
    logger = pLogger;
    formulaManager = pPfmgr;
    statistics = pStatistics;
    mngr = pMngr;
    totalMergeTimer = statistics.totalMergeTime.getNewTimer();
    abstractionLattice = pAbstractionLattice;
    joinEffectsIntoUndef = pJoin;

    mergeAbstractionStates = pMergeAbstractionStates;
    predAbsManager = pPredAbsManager;
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

    Collection<FormulaDescription> newDesc1 =
        from(desc1)
            .filter(s -> !desc2.contains(s))
            .toSet();

    if (newDesc1.isEmpty()) {
      return edge2;
    } else {

      if (joinEffectsIntoUndef) {
        Collection<CAssignment> newFormulas1 =
            transformedImmutableSetCopy(newDesc1, FormulaDescription::getAssignment);
        Set<CLeftHandSide> newAssignments1 =
            transformedImmutableSetCopy(newFormulas1, CAssignment::getLeftHandSide);
        Set<CLeftHandSide> assignments2 =
            transformedImmutableSetCopy(desc2, s -> s.getAssignment().getLeftHandSide());
        Set<CLeftHandSide> commonPart = Sets.intersection(newAssignments1, assignments2);
        if (commonPart.isEmpty()) {
          Collection<FormulaDescription> newFormulas = new HashSet<>(desc2);
          newFormulas.addAll(newDesc1);
          return new PredicateAbstractEdge(newFormulas);
        } else {
          Collection<FormulaDescription> newFormulas = new HashSet<>();
          copyFormulas(newFormulas, newDesc1, commonPart);
          copyFormulas(newFormulas, desc2, commonPart);

          boolean newFormulasFound = false;
          // Base on the second to be close to merge def.
          for (FormulaDescription desc : desc2) {
            CAssignment asgn = desc.getAssignment();
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

                CFunctionDeclaration dummy =
                    new CFunctionDeclaration(
                    FileLocation.DUMMY,
                    CFunctionType.NO_ARGS_VOID_FUNCTION,
                        "dummy",
                        ImmutableList.of());

                CFAEdge fakeEdge =
                    new CStatementEdge(
                        "environment",
                        newAssignement,
                        newAssignement.getFileLocation(),
                        new CFANode(dummy),
                        new CFANode(dummy));

                PathFormula pFormula = formulaManager.makeEmptyPathFormula();

                try {
                  pFormula = formulaManager.makeAnd(pFormula, fakeEdge);
                } catch (CPATransferException | InterruptedException e) {
                  continue;
                }

                // TODO finish
                newFormulas
                    .add(new FormulaDescription(newAssignement, pFormula.getFormula(), null));
                newFormulasFound = true;
              }
            }
          }

          if (commonPart.equals(newAssignments1) && !newFormulasFound) {
            // Common part is covered
            return edge2;
          }

          return new PredicateAbstractEdge(newFormulas);
        }

      } else {
        Collection<FormulaDescription> newFormulas = new HashSet<>(desc2);
        newFormulas.addAll(newDesc1);
        return new PredicateAbstractEdge(newFormulas);
      }
    }
  }

  private void copyFormulas(
      Collection<FormulaDescription> result,
      Collection<FormulaDescription> origin,
      Set<CLeftHandSide> toSkip) {
    for (FormulaDescription asgn : origin) {
      CLeftHandSide left = asgn.getAssignment().getLeftHandSide();
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

    if (mergeAbstractionStates
        && pState1.isAbstractionState()
        && pState2.isAbstractionState()
        && !pState1.getAbstractionFormula().equals(pState2.getAbstractionFormula())) {
      if (pState1.getPreviousAbstractionState().equals(pState2.getPreviousAbstractionState())) {
        totalMergeTimer.start();
        AbstractionFormula newAbstractionFormula =
            predAbsManager.makeOr(pState1.getAbstractionFormula(), pState2.getAbstractionFormula());
        PathFormula newPathFormula =
            formulaManager.makeEmptyPathFormula(newAbstractionFormula.getBlockFormula());
        merged =
            mkAbstractionState(
                newPathFormula,
                newAbstractionFormula,
                pState2.getAbstractionLocationsOnPath(),
                pState2.getPreviousAbstractionState());
        pState1.setMergedInto(merged);
        totalMergeTimer.stop();
        return merged;
      }
    }
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

        merged =
            mkNonAbstractionStateWithNewPathFormula(
                pathFormula,
                pState1,
                pState2.getPreviousAbstractionState());

        // now mark elem1 so that coverage check can find out it was merged
        pState1.setMergedInto(merged);

        totalMergeTimer.stop();
      }
    }

    return merged;
  }
}
