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
package org.sosy_lab.cpachecker.cpa.policyiteration;


import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Updating LPI precision using interpolants.
 */
public class PolicyInterpolationRefiner implements Refiner {

  private final LogManager logger;
  private final PathExtractor pathExtractor;
  private final ARGCPA argCpa;
  private final Solver solver;
  private final PolicyCPA policyCPA;
  private final FormulaManagerView fmgr;

  public static PolicyInterpolationRefiner create(
      ConfigurableProgramAnalysis pConfigurableProgramAnalysis
  ) throws InvalidConfigurationException {
    PolicyCPA policyCPA = CPAs.retrieveCPA(pConfigurableProgramAnalysis,
        PolicyCPA.class);
    Preconditions.checkNotNull(policyCPA);
    ARGCPA argCPA = CPAs.retrieveCPA(pConfigurableProgramAnalysis,
        ARGCPA.class);
    Preconditions.checkNotNull(argCPA);

    Configuration config = policyCPA.getConfig();
    LogManager logger = policyCPA.getLogger();
    Solver solver = policyCPA.getSolver();
    PathExtractor pathExtractor = new PathExtractor(logger, config);
    return new PolicyInterpolationRefiner(
        policyCPA, logger, pathExtractor, argCPA, solver
    );
  }

  private PolicyInterpolationRefiner(
      PolicyCPA pPolicyCPA,
      LogManager pLogger,
      PathExtractor pPathExtractor,
      ARGCPA pArgCpa,
      Solver pSolver) throws InvalidConfigurationException {
    fmgr = pSolver.getFormulaManager();
    solver = pSolver;
    policyCPA = pPolicyCPA;
    logger = pLogger;
    pathExtractor = pPathExtractor;
    argCpa = pArgCpa;
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    ARGReachedSet argReached = new ARGReachedSet(pReached, argCpa);
    Collection<ARGState> targets = pathExtractor.getTargetStates(argReached);

    for (ARGState target : targets) {
      try {
        if (handleTarget(target)) {

          // todo: invalidate only the path to the error, like in other refiners.
          forceRestart(pReached);
          return true;
        }
      } catch (SolverException pE) {
        throw new CPAException("Got solver exception during interpolation", pE);
      }
    }

    forceRestart(pReached);
    return false;
  }

  private boolean handleTarget(ARGState target) throws SolverException, InterruptedException {
    PolicyState policyState = AbstractStates.extractStateByType(target, PolicyState.class);
    assert policyState != null;

    // todo: verify the precondition below on benchmarks.
    Preconditions.checkState(!policyState.isAbstract(),
        "Property violation should be associated with an intermediate state.");

    PolicyIntermediateState iPolicyState = policyState.asIntermediate();
    List<PathFormula> assertions = new ArrayList<>();
    assertions.add(iPolicyState.getPathFormula());

    while (iPolicyState.getBackpointerState().getGeneratingState().isPresent()) {
      iPolicyState = iPolicyState.getBackpointerState().getGeneratingState().get();
      assertions.add(iPolicyState.getPathFormula());
    }

    List<BooleanFormula> formulas = assertions.stream().map(
        pf -> pf.getFormula()
    ).collect(Collectors.toList());

    try (InterpolatingProverEnvironment<?> itp = solver.newProverEnvironmentWithInterpolation()) {
      return injectPrecision(itp, formulas, policyState);
    }
  }

  /**
   * Inject precision derived from interpolants into PolicyCPA.
   * Return whether the precision was changed.
   */
  private <T> boolean injectPrecision(
      InterpolatingProverEnvironment<T> itp,
      List<BooleanFormula> formulas,
      PolicyState policyState) throws SolverException, InterruptedException {
    List<Set<T>> handles = new ArrayList<>();

    for (BooleanFormula f : formulas) {
      T handle = itp.push(f);
      assert handle != null;
      handles.add(ImmutableSet.of(handle));
    }

    // todo: What if it's not? though presumably counterexample checker
    // should take care of that first.
    Preconditions.checkState(itp.isUnsat());
    List<BooleanFormula> interpolants = itp.getSeqInterpolants(handles);
    logger.log(Level.FINE, "Got interpolants", interpolants);

    PolicyIntermediateState iState = policyState.asIntermediate();
    boolean changed = false;
    for (BooleanFormula interpolant : interpolants) {
      CFANode node = iState.getBackpointerState().getNode();
      changed |= policyCPA.injectPrecisionFromInterpolant(
          node,
          fmgr.extractVariableNames(fmgr.uninstantiate(interpolant))
      );
    }
    return changed;
  }

  private void forceRestart(ReachedSet reached) {
    ARGState firstChild = Iterables
        .getOnlyElement(((ARGState)reached.getFirstState()).getChildren());

    new ARGReachedSet(reached).removeSubtree(firstChild);
  }
}
