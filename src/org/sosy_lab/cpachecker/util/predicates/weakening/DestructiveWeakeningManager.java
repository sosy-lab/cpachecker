/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.weakening;

import static org.sosy_lab.java_smt.api.SolverContext.ProverOptions.GENERATE_UNSAT_CORE_OVER_ASSUMPTIONS;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.predicates.weakening.InductiveWeakeningManager.InductiveWeakeningStatistics;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.ProverEnvironment;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Perform weakening by destructive iterations.
 */
@Options(prefix="cpa.slicing")
public class DestructiveWeakeningManager {
  @Option(secure=true, description="Pre-run syntactic weakening")
  private boolean preRunSyntacticWeakening = true;

  private final Solver solver;
  private final BooleanFormulaManager bfmgr;
  private final SyntacticWeakeningManager swmgr;
  private final InductiveWeakeningStatistics statistics;

  public DestructiveWeakeningManager(
      Solver pSolver,
      FormulaManagerView pFmgr,
      Configuration pConfiguration,
      InductiveWeakeningStatistics pStatistics) throws InvalidConfigurationException {
    pConfiguration.inject(this);

    solver = pSolver;
    bfmgr = pFmgr.getBooleanFormulaManager();
    swmgr = new SyntacticWeakeningManager(pFmgr);
    statistics = pStatistics;
  }

  /**
   * @return Set of selectors which should be abstracted.
   */
  public Set<BooleanFormula> performWeakening(
      Map<BooleanFormula, BooleanFormula> selectionsVarsInfo,
      BooleanFormula fromState,
      PathFormula transition,
      BooleanFormula toState,
      SSAMap fromSSA,
      Set<BooleanFormula> pFromStateLemmas
  ) throws SolverException, InterruptedException {
    Set<BooleanFormula> selectorsToAbstractOverApproximation;
    if (preRunSyntacticWeakening) {
      selectorsToAbstractOverApproximation = swmgr.performWeakening(
          fromSSA, selectionsVarsInfo, transition, pFromStateLemmas);
    } else {
      selectorsToAbstractOverApproximation = selectionsVarsInfo.keySet();
    }
    BooleanFormula query = bfmgr.and(
        fromState, transition.getFormula(), bfmgr.not(toState)
    );
    return destructiveWeakening(
        selectionsVarsInfo,
        selectorsToAbstractOverApproximation,
        query
    );
  }

  private BooleanFormula generateNegations(
      Set<BooleanFormula> selectors,
      Set<BooleanFormula> toAbstract
  ) {
    return bfmgr.and(
        selectors.stream().filter(
            sel -> !toAbstract.contains(sel)
        ).map(bfmgr::not).collect(Collectors.toList())
    );
  }

  /**
   * Implements the destructive algorithm for MUS extraction.
   * Starts with everything abstracted ("true" is inductive),
   * remove selectors which can be removed while keeping the overall query
   * inductive.
   *
   * <p>This is a standard algorithm, however it pays the cost of N SMT calls
   * upfront.
   * Note that since at every iteration the set of abstracted variables is
   * inductive, the algorithm can be terminated early.
   *
   * @param selectionInfo Mapping from selection variables
   *    to the atoms (possibly w/ negation) they represent.
   * @param selectionVars List of selection variables, already determined to
   *    be inductive.
   * @return Set of selectors which correspond to atoms which *should*
   *   be abstracted.
   */
  public Set<BooleanFormula> destructiveWeakening(
      Map<BooleanFormula, BooleanFormula> selectionInfo,
      Set<BooleanFormula> selectionVars,
      BooleanFormula query) throws SolverException, InterruptedException {

    Set<BooleanFormula> walked = new HashSet<>();
    Set<BooleanFormula> toWalk;
    Set<BooleanFormula> toAbstract;

    try (ProverEnvironment pe = solver.newProverEnvironment(
        GENERATE_UNSAT_CORE_OVER_ASSUMPTIONS)) {
      pe.push();
      pe.addConstraint(query);

      Optional<List<BooleanFormula>> core =
          pe.unsatCoreOverAssumptions(selectionVars);

      if (core.isPresent()) {

        List<BooleanFormula> unsatCore = core.get();
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
        pe.addConstraint(generateNegations(selectionInfo.keySet(), toAbstract));

        core = pe.unsatCoreOverAssumptions(toAbstract);
        noIterations++;

        if (core.isPresent()) {

          List<BooleanFormula> unsatCore = core.get();
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
