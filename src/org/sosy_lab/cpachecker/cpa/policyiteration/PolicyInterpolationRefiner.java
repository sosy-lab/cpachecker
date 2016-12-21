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
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.Tactic;

/**
 * Updating LPI precision using interpolants.
 */
@Options(prefix="cpa.lpi.refinement")
public class PolicyInterpolationRefiner implements Refiner {

  @Option(secure=true, description="Attempt to weaken interpolants in order to make them more "
      + "general")
  private boolean generalizeInterpolants = true;

  private final PathExtractor pathExtractor;
  private final ARGCPA argCpa;
  private final Solver solver;
  private final PolicyCPA policyCPA;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;

  /**
   * Cache for variable names contained in a formula.
   */
  private final Map<BooleanFormula, Set<String>> extractedVarsCache;

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
        config, policyCPA, pathExtractor, argCPA, solver
    );
  }

  private PolicyInterpolationRefiner(
      Configuration config,
      PolicyCPA pPolicyCPA,
      PathExtractor pPathExtractor,
      ARGCPA pArgCpa,
      Solver pSolver) throws InvalidConfigurationException {
    config.inject(this);
    fmgr = pSolver.getFormulaManager();
    solver = pSolver;
    policyCPA = pPolicyCPA;
    pathExtractor = pPathExtractor;
    argCpa = pArgCpa;
    extractedVarsCache = new HashMap<>();
    bfmgr = fmgr.getBooleanFormulaManager();
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

    try (InterpolatingProverEnvironment<?> itp = solver.newProverEnvironmentWithInterpolation()) {
      return injectPrecision(itp, policyState.asIntermediate());
    }
  }

  /**
   * Inject precision derived from interpolants into PolicyCPA.
   * Return whether the precision was changed.
   */
  private <T> boolean injectPrecision(
      InterpolatingProverEnvironment<T> itp,
      final PolicyIntermediateState iState) throws SolverException, InterruptedException {
    List<Set<T>> handles = new ArrayList<>();

    int pushed = 0;
    for (PolicyIntermediateState predecessor : iState.allStatesToRoot()) {
      T handle = itp.push(predecessor.getPathFormula().getFormula());
      assert handle != null;
      handles.add(ImmutableSet.of(handle));
      pushed++;
    }

    Preconditions.checkState(itp.isUnsat());

    List<BooleanFormula> interpolants = itp.getSeqInterpolants(handles);

    boolean changed = injectPrecisionFromInterpolants(interpolants, iState);

    for (int i=0; i<pushed; i++) {
      // todo: a nicer way to pop everything at one time.
      itp.pop();
    }

    if (generalizeInterpolants) {
      Optional<List<BooleanFormula>> weakerInterpolants = getGeneralizedInterpolants(iState, itp);
      if (weakerInterpolants.isPresent()) {
        changed |= injectPrecisionFromInterpolants(weakerInterpolants.get(), iState);
      }
    }

    return changed;
  }

  /**
   * Inject precision derived from {@code interpolants} to all states from
   * root to {@code iState}.
   */
  private boolean injectPrecisionFromInterpolants(
      List<BooleanFormula> interpolants,
      final PolicyIntermediateState iState) {

    boolean changed = false;

    for (BooleanFormula interpolant : interpolants) {
      for (PolicyIntermediateState iterState : iState.allStatesToRoot()) {
        CFANode node = iterState.getBackpointerState().getNode();
        Set<String> interpolantVars = fmgr.extractVariableNames(fmgr.uninstantiate(interpolant));
        boolean precisionChanged = policyCPA.injectPrecisionFromInterpolant(
            node, interpolantVars
        );
        changed |= precisionChanged;

      }
    }
    return changed;
  }


  /**
   * Attempt to generalize interpolants by weakening the input formulas.
   *
   * @return new interpolants or {@code Optional.empty()} if the generalized
   * conjunction is satisfiable.
   */
  private <T> Optional<List<BooleanFormula>> getGeneralizedInterpolants(
      final PolicyIntermediateState pState,
      InterpolatingProverEnvironment<T> itp
      ) throws SolverException, InterruptedException {

    List<Set<T>> handles = new ArrayList<>();

    for (PolicyIntermediateState predecessor : pState.allStatesToRoot()) {
      BooleanFormula f = predecessor.getPathFormula().getFormula();

      Set<String> varsToRemove = getRelevantInstantiatedVars(predecessor.getBackpointerState());
      BooleanFormula weakened = weaken(f, varsToRemove);

      T handle = itp.push(weakened);
      assert handle != null;
      handles.add(ImmutableSet.of(handle));
    }

    if (itp.isUnsat()) {
      return Optional.of(itp.getSeqInterpolants(handles));

    } else {
      return Optional.empty();
    }
  }

  /**
   * @return All instantiated variables mentioned in templates associated with {@code pState}.
   */
  private Set<String> getRelevantInstantiatedVars(PolicyAbstractedState pState) {
    Set<String> usedVars = pState.getAbstraction().keySet().stream()
        .flatMap(t -> t.getLinearExpression().getMap().keySet().stream())
        .map(id -> id.getDeclaration().getQualifiedName())
        .collect(Collectors.toSet());
    return fmgr.instantiate(usedVars, pState.getSSA());
  }


  /**
   * Weaken {@code input}, such that it no longer contains any variables in {@code varsToDrop}.
   */
  private BooleanFormula weaken(BooleanFormula input,
                                Set<String> varsToDrop) throws InterruptedException {
    if (varsToDrop.isEmpty()) {
      return input;
    }
    if (Sets.intersection(extractVariableNames(input), varsToDrop).isEmpty()) {
      return input;
    }
    input = fmgr.simplify(input);
    input = fmgr.applyTactic(input, Tactic.NNF);
    input = bfmgr.transformRecursively(input, new FormulaWeakeningManager(fmgr, varsToDrop));
    return fmgr.simplify(input);
  }

  /**
   * Drop all literals which have variables given in the constructor.
   */
  private class FormulaWeakeningManager extends BooleanFormulaTransformationVisitor {
    private final Set<String> varsToDrop;

    FormulaWeakeningManager(FormulaManagerView pFmgr,
                                      Set<String> pVarsToDrop) {
      super(pFmgr);
      varsToDrop = pVarsToDrop;
    }

    @Override
    public BooleanFormula visitAtom(BooleanFormula pAtom, FunctionDeclaration<BooleanFormula> decl) {
      if (!Sets.intersection(varsToDrop, extractVariableNames(pAtom)).isEmpty()) {
        return bfmgr.makeTrue();
      }
      return pAtom;
    }

    @Override
    public BooleanFormula visitNot(BooleanFormula input) {
      if (bfmgr.isTrue(input)) {

        // Hack for not having "visit literal".
        return bfmgr.makeTrue();
      }
      return input;
    }
  }

  /**
   * Extract stored variable names, store the results in cache.
   */
  private Set<String> extractVariableNames(BooleanFormula pFormula) {
    Set<String> cache = extractedVarsCache.get(pFormula);
    if (cache == null) {
      cache = fmgr.extractVariableNames(pFormula);
      extractedVarsCache.put(pFormula, cache);
    }
    return cache;
  }

  private void forceRestart(ReachedSet reached) {
    ARGState firstChild = Iterables
        .getOnlyElement(((ARGState)reached.getFirstState()).getChildren());

    new ARGReachedSet(reached).removeSubtree(firstChild);
  }
}
