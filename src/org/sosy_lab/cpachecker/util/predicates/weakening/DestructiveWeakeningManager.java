// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.weakening;

import static org.sosy_lab.java_smt.api.SolverContext.ProverOptions.GENERATE_UNSAT_CORE_OVER_ASSUMPTIONS;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.predicates.weakening.InductiveWeakeningManager.InductiveWeakeningStatistics;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

/** Perform weakening by destructive iterations. */
public class DestructiveWeakeningManager {

  private final Solver solver;
  private final BooleanFormulaManager bfmgr;
  private final SyntacticWeakeningManager swmgr;
  private final InductiveWeakeningStatistics statistics;
  private final WeakeningOptions options;

  public DestructiveWeakeningManager(
      Solver pSolver,
      FormulaManagerView pFmgr,
      WeakeningOptions pOptions,
      InductiveWeakeningStatistics pStatistics) {
    options = pOptions;

    solver = pSolver;
    bfmgr = pFmgr.getBooleanFormulaManager();
    swmgr = new SyntacticWeakeningManager(pFmgr);
    statistics = pStatistics;
  }

  /** Returns set of selectors which should be abstracted. */
  public Set<BooleanFormula> performWeakening(
      Map<BooleanFormula, BooleanFormula> selectionsVarsInfo,
      BooleanFormula fromState,
      PathFormula transition,
      BooleanFormula toState,
      SSAMap fromSSA,
      Set<BooleanFormula> pFromStateLemmas)
      throws SolverException, InterruptedException {
    Set<BooleanFormula> selectorsToAbstractOverApproximation;
    if (options.doPreRunSyntacticWeakening()) {
      selectorsToAbstractOverApproximation =
          swmgr.performWeakening(
              fromSSA, selectionsVarsInfo, transition.getSsa(), pFromStateLemmas);
    } else {
      selectorsToAbstractOverApproximation = selectionsVarsInfo.keySet();
    }
    BooleanFormula query = bfmgr.and(fromState, transition.getFormula(), bfmgr.not(toState));
    return destructiveWeakening(
        selectionsVarsInfo.keySet(), selectorsToAbstractOverApproximation, query);
  }

  private BooleanFormula generateNegations(
      Set<BooleanFormula> selectors, Set<BooleanFormula> toAbstract) {
    return selectors.stream()
        .filter(sel -> !toAbstract.contains(sel))
        .map(bfmgr::not)
        .collect(bfmgr.toConjunction());
  }

  /**
   * Implements the destructive algorithm for MUS extraction. Starts with everything abstracted
   * ("true" is inductive), remove selectors which can be removed while keeping the overall query
   * inductive.
   *
   * <p>This is a standard algorithm, however it pays the cost of N SMT calls upfront. Note that
   * since at every iteration the set of abstracted variables is inductive, the algorithm can be
   * terminated early.
   *
   * @param selectors All selection variables.
   * @param selectionVars List of selection variables, already determined to be inductive.
   * @return Set of selectors which correspond to atoms which *should* be abstracted.
   */
  public Set<BooleanFormula> destructiveWeakening(
      Set<BooleanFormula> selectors, Set<BooleanFormula> selectionVars, BooleanFormula query)
      throws SolverException, InterruptedException {

    Set<BooleanFormula> walked = new HashSet<>();
    Set<BooleanFormula> toWalk;
    Set<BooleanFormula> toAbstract;

    try (ProverEnvironment pe = solver.newProverEnvironment(GENERATE_UNSAT_CORE_OVER_ASSUMPTIONS)) {
      pe.push();
      pe.addConstraint(query);

      Optional<List<BooleanFormula>> core = pe.unsatCoreOverAssumptions(selectionVars);

      if (core.isPresent()) {

        List<BooleanFormula> unsatCore = core.orElseThrow();
        toWalk = new HashSet<>(unsatCore);
        toAbstract = new HashSet<>(unsatCore);
      } else {
        throw new IllegalStateException("Unexpected state");
      }

      int noIterations = 1;

      while (!walked.containsAll(toWalk)) {
        BooleanFormula toTest = toWalk.iterator().next();
        toAbstract.remove(toTest);
        walked.add(toTest);

        pe.push();

        // Force all selectors not in {@code toAbstract} to be {@code false}.
        pe.addConstraint(generateNegations(selectors, toAbstract));

        core = pe.unsatCoreOverAssumptions(toAbstract);
        noIterations++;

        if (core.isPresent()) {

          List<BooleanFormula> unsatCore = core.orElseThrow();
          toWalk = new HashSet<>(unsatCore);
          toAbstract = new HashSet<>(unsatCore);
        } else {
          toAbstract.add(toTest);
          toWalk.remove(toTest);
        }

        pe.pop();
      }
      statistics.iterationsNo.add(noIterations);
    }

    return toAbstract;
  }
}
