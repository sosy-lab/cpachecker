// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import de.uni_freiburg.informatik.ultimate.util.datastructures.HashDeque;
import java.nio.file.Path;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.IMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor.InvalidWitnessException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.QuantifiedFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class TerminationWitnessValidator implements Algorithm {

  private static final DummyTargetState DUMMY_TARGET_STATE =
      DummyTargetState.withSimpleTargetInformation("termination");

  private final Path witnessPath;
  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final QuantifiedFormulaManagerView qfmgr;
  private final Solver solver;
  private final Scope scope;

  public TerminationWitnessValidator(
      final CFA pCfa,
      final ConfigurableProgramAnalysis pCPA,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final ImmutableSet<Path> pWitnessPath)
      throws InvalidConfigurationException {
    cfa = pCfa;
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    scope =
        switch (cfa.getLanguage()) {
          case C -> new CProgramScope(cfa, logger);
          default -> DummyScope.getInstance();
        };

    @SuppressWarnings("resource")
    PredicateCPA predCpa = CPAs.retrieveCPAOrFail(pCPA, PredicateCPA.class, IMCAlgorithm.class);
    solver = predCpa.getSolver();
    pfmgr = predCpa.getPathFormulaManager();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    qfmgr = fmgr.getQuantifiedFormulaManager();

    if (pWitnessPath.size() < 1) {
      throw new InvalidConfigurationException("Witness file is missing in specification.");
    }
    if (pWitnessPath.size() != 1) {
      throw new InvalidConfigurationException(
          "Expect that only violation witness is part of the specification.");
    }

    witnessPath = pWitnessPath.stream().findAny().orElseThrow();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    Set<ExpressionTreeLocationInvariant> invariants;
    ImmutableCollection<LoopStructure.Loop> loops =
        cfa.getLoopStructure().orElseThrow().getAllLoops();
    try {
      WitnessInvariantsExtractor invariantsExtractor =
          new WitnessInvariantsExtractor(config, logger, cfa, shutdownNotifier, witnessPath);
      invariants = invariantsExtractor.extractInvariantsFromReachedSet();
    } catch (InvalidConfigurationException e) {
      throw new CPAException(
          "Invalid Configuration while analyzing witness:\n" + e.getMessage(), e);
    } catch (InvalidWitnessException e) {
      throw new CPAException("Invalid witness:\n" + e.getMessage(), e);
    }

    ImmutableMap<LoopStructure.Loop, BooleanFormula> loopsToTransitionInvariants =
        mapTransitionInvariantsToLoops(loops, invariants);

    // Check that every transition invariant is disjunctively well-founded
    for (LoopStructure.Loop loop : loops) {
      Deque<Formula> waitlist = new HashDeque<>();
      waitlist.add(loopsToTransitionInvariants.get(loop));

      while (!waitlist.isEmpty()) {
        Formula invariant = waitlist.remove();
        // Check the well-foundedness of the subformula
        if (!isTheFormulaWellFounded(loop, loopsToTransitionInvariants.get(loop))
            && !canBeDecomposedByDNFRules(invariant, waitlist)) {
          // The invariant is not disjunctively well-founded
          pReachedSet.add(new ARGState(DUMMY_TARGET_STATE, null), SingletonPrecision.getInstance());
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
      }
    }

    // Check that every invariant is actually a transition invariant, i.e. R^+ => T
    // TODO: Consider, implementing these checks in two different configurations and run them in
    // parallel.
    for (LoopStructure.Loop loop : loops) {
      if (!isTheInvariantTransitionInvariant(loop, loopsToTransitionInvariants.get(loop))) {
        pReachedSet.add(new ARGState(DUMMY_TARGET_STATE, null), SingletonPrecision.getInstance());
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
    }
    pReachedSet.clear();
    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  private ImmutableMap<LoopStructure.Loop, BooleanFormula> mapTransitionInvariantsToLoops(
      ImmutableCollection<LoopStructure.Loop> pLoops,
      Set<ExpressionTreeLocationInvariant> pInvariants)
      throws InterruptedException {
    ImmutableMap.Builder<LoopStructure.Loop, BooleanFormula> builder = new Builder<>();

    for (LoopStructure.Loop loop : pLoops) {
      BooleanFormula invariantForTheLoop = bfmgr.makeFalse();
      for (ExpressionTreeLocationInvariant invariant : pInvariants) {
        if (!invariant.isTransitionInvariant()) {
          // The check for supporting invariants is not yet supported
          // TODO: Implement checking that the invariants are really invariants
          continue;
        }

        if (isTheInvariantLocationInLoop(loop, invariant.getLocation())) {
          BooleanFormula invariantFormula;
          try {
            invariantFormula = invariant.getFormula(fmgr, pfmgr, pfmgr.makeEmptyPathFormula());
          } catch (CPATransferException e) {
            invariantFormula = bfmgr.makeFalse();
          }
          invariantForTheLoop = bfmgr.or(invariantForTheLoop, invariantFormula);
        }
      }
      builder.put(loop, invariantForTheLoop);
    }
    return builder.build();
  }

  private boolean isTheInvariantLocationInLoop(
      LoopStructure.Loop pLoop, CFANode pInvariantLocation) {
    for (CFANode loopNode : pLoop.getLoopNodes()) {
      if (loopNode.equals(pInvariantLocation)) {
        return true;
      }
    }
    return false;
  }

  /**
   * This method checks whether one concrete subformula from transition invariant is well-founded.
   * It does it using the check T(s,s') => [∃s1.T(s,s1) ∧ ¬T(s',s1)] ∧ [∀s2.T(s,s2) => T(s',s2)] If
   * this holds, it means that the number of states reachable from s is decreasing. In other words,
   * the cardinality of the set of reachable states is rank for every state.
   *
   * @param pLoop that the transition invariant overapproximates
   * @param pFormula representing the transition invariant
   * @return true if the formula really is well-founded, false otherwise
   */
  private boolean isTheFormulaWellFounded(LoopStructure.Loop pLoop, BooleanFormula pFormula) {
    SSAMap ssaMap = setIndicesToDifferentValues(pFormula, 1, 2);

    // T(s,s')
    BooleanFormula oneStep = fmgr.instantiate(pFormula, ssaMap);

    // ∃s1.T(s,s1) ∧ ¬T(s',s1)
    makeStatesEquivalent(oneStep, 1, 2);

    return true;
  }

  /**
   * Constructs a new SSAMap, where the __PREV variables and normal variables are instantiated
   * differently.
   *
   * @param pFormula given on input
   * @param prevIndex to which should the __PREV variables be instantiated
   * @param currIndex to which should the normal variables be instantiated
   * @return instantiated ssaMap
   */
  private SSAMap setIndicesToDifferentValues(Formula pFormula, int prevIndex, int currIndex) {
    SSAMapBuilder builder = SSAMap.emptySSAMap().builder();
    for (String var : fmgr.extractVariableNames(pFormula)) {
      builder.setIndex(
          var, scope.lookupVariable(var).getType(), var.contains("__PREV") ? prevIndex : currIndex);
    }
    return builder.build();
  }

  /**
   * Constructs formulas to make some states equivalent. For example, s' occurs in the formulas both
   * in T(.,s') and T(s',.), so we have to make the corresponding variables equivalent.
   *
   * @param pFormula on the input
   * @param prevIndex the index of the variables from the previous state
   * @param currIndex the index of the current variables
   * @return the formula with terms like x_PREV@1 <==> x@2
   */
  private BooleanFormula makeStatesEquivalent(
      BooleanFormula pFormula, int prevIndex, int currIndex) {
    BooleanFormula equivalence = bfmgr.makeTrue();
    Map<String, Formula> mapNamesToVars = fmgr.extractVariables(pFormula);
    for (String prevVar : fmgr.extractVariableNames(pFormula)) {
      if (prevVar.contains("__PREV") && prevVar.contains("@" + prevIndex)) {
        String prevVarPure = prevVar.replace("__PREV", "");
        prevVarPure = prevVarPure.replace("@" + prevIndex, "");
        String currVar = "";
        for (String var : fmgr.extractVariableNames(pFormula)) {
          if (var.replace("@" + currIndex, "").equals(prevVarPure)) {
            currVar = var;
            break;
          }
        }
        if (!currVar.isEmpty()) {
          equivalence =
              fmgr.makeAnd(
                  equivalence,
                  fmgr.makeEqual(mapNamesToVars.get(prevVar), mapNamesToVars.get(currVar)));
        }
      }
    }
    return equivalence;
  }

  // TODO: Write the method that decomposes the formula with rewrite rules and puts the formulas in
  // the waitlist.
  private boolean canBeDecomposedByDNFRules(Formula pFormula, Deque<Formula> pWaitlist) {
    return false;
  }

  // TODO: Write this method
  private boolean isTheInvariantTransitionInvariant(LoopStructure.Loop pLoop, Formula pInvariant) {
    return true;
  }
}
