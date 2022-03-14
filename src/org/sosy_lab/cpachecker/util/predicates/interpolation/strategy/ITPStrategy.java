// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.interpolation.strategy;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class ITPStrategy {

  protected final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  protected final FormulaManagerView fmgr;
  protected final BooleanFormulaManager bfmgr;

  ITPStrategy(LogManager pLogger, ShutdownNotifier pShutdownNotifier, FormulaManagerView pFmgr) {
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    fmgr = pFmgr;
    bfmgr = pFmgr.getBooleanFormulaManager();
  }

  /**
   * The implementation of this method specifies the interpolation strategy and computes
   * interpolants for the given formulae.
   *
   * @param interpolator is the interface towards the SMT-solver and contains an ITP-solver with all
   *     formulas asserted on its solver-stack.
   * @param formulasWithStateAndGroupId is a list of (F,E,T) where the path formula F starting at an
   *     abstract state E (abstraction state?) corresponds with the ITP-group T. We assume the
   *     sorting of the list matches the order of abstract states along the counterexample.
   * @return a list of (N-1) interpolants for a list of N formulae
   */
  public abstract <T> List<BooleanFormula> getInterpolants(
      final InterpolationManager.Interpolator<T> interpolator,
      final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStateAndGroupId)
      throws InterruptedException, SolverException;

  /**
   * This method checks the validity of the interpolants according to the current interpolation
   * strategy. The default interpolation strategy is sequential interpolation, i.e. we assume:
   * \forall i \in [1..n-1] : itp_{i-1} & f_i => itp_i This method can be overridden if the strategy
   * computes interpolants with a different strategy.
   *
   * @param solver is for checking satisfiability
   * @param formulasWithStatesAndGroupdIds is a list of (F,E,T) where the path formula F starting at
   *     an abstract state E corresponds with the ITP-group T. We assume the sorting of the list
   *     matches the order of abstract states along the counterexample.
   * @param interpolants computed with {@link #getInterpolants} and will be checked.
   */
  public <T> void checkInterpolants(
      final Solver solver,
      final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds,
      final List<BooleanFormula> interpolants)
      throws InterruptedException, SolverException {

    final List<BooleanFormula> formulas =
        Lists.transform(formulasWithStatesAndGroupdIds, Triple::getFirst);

    final int n = interpolants.size();
    assert n == (formulas.size() - 1);

    // The following three properties need to be checked:
    // (A)                          true      & f_0 => itp_0
    // (B) \forall i \in [1..n-1] : itp_{i-1} & f_i => itp_i
    // (C)                          itp_{n-1} & f_n => false

    // Check (A)
    if (!solver.implies(formulas.get(0), interpolants.get(0))) {
      throw new SolverException(
          String.format(
              "First interpolant '%s' is not implied by first formula '%s'.",
              interpolants.get(0), formulas.get(0)));
    }

    // Check (B).
    for (int i = 1; i <= (n - 1); i++) {
      BooleanFormula conjunct = bfmgr.and(interpolants.get(i - 1), formulas.get(i));
      if (!solver.implies(conjunct, interpolants.get(i))) {
        throw new SolverException(
            String.format(
                "Interpolant '%s' at index %d is not implied by previous part of the path",
                interpolants.get(i), i));
      }
    }

    // Check (C).
    BooleanFormula conjunct = bfmgr.and(interpolants.get(n - 1), formulas.get(n));
    if (!solver.implies(conjunct, bfmgr.makeFalse())) {
      throw new SolverException(
          String.format(
              "Last interpolant '%s' fails to prove infeasibility of the remaining path '%s'.",
              interpolants.get(n - 1), formulas.get(n)));
    }

    // Furthermore, check if the interpolants contains only the allowed variables
    final List<Set<String>> variablesInFormulas = new ArrayList<>(formulas.size());
    for (BooleanFormula f : formulas) {
      variablesInFormulas.add(fmgr.extractVariableNames(f));
    }

    for (int i = 0; i < interpolants.size(); i++) {

      Set<String> variablesInA = new HashSet<>();
      for (int j = 0; j <= i; j++) {
        // formula i is in group A
        variablesInA.addAll(variablesInFormulas.get(j));
      }

      Set<String> variablesInB = new HashSet<>();
      for (int j = i + 1; j < formulas.size(); j++) {
        // formula i is in group A
        variablesInB.addAll(variablesInFormulas.get(j));
      }

      Set<String> allowedVariables = Sets.intersection(variablesInA, variablesInB).immutableCopy();
      Set<String> variablesInInterpolant = fmgr.extractVariableNames(interpolants.get(i));

      if (!allowedVariables.containsAll(variablesInInterpolant)) {
        throw new SolverException(
            "Interpolant "
                + interpolants.get(i)
                + " contains forbidden variable(s) "
                + Sets.difference(variablesInInterpolant, allowedVariables));
      }
    }
  }

  protected static <T1, T2, T3> List<T3> projectToThird(final List<Triple<T1, T2, T3>> l) {
    return Lists.transform(l, Triple::getThird);
  }

  /**
   * Precondition: The solver-stack contains all formulas and is UNSAT. Get the interpolant between
   * the Sublist of formulas and the other formulas on the solver-stack. Each formula is identified
   * by its GroupId, The sublist is taken from the list of GroupIds, including both start and end of
   * A.
   */
  protected final <T> BooleanFormula getInterpolantFromSublist(
      final InterpolatingProverEnvironment<T> pItpProver,
      final List<T> itpGroupsIds,
      final int start_of_A,
      final int end_of_A)
      throws InterruptedException, SolverException {
    shutdownNotifier.shutdownIfNecessary();

    logger.log(Level.ALL, "Looking for interpolant for formulas from", start_of_A, "to", end_of_A);

    final BooleanFormula itp =
        pItpProver.getInterpolant(itpGroupsIds.subList(start_of_A, end_of_A + 1));

    logger.log(Level.ALL, "Received interpolant", itp);
    return itp;
  }
}
