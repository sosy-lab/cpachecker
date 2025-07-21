// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.MapsDifference;
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
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor.InvalidWitnessException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.QuantifiedFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.SolverException;

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
    ImmutableMap<LoopStructure.Loop, ImmutableList<BooleanFormula>> loopsToSupportingInvariants =
        mapSupportingInvariantsToLoops(loops, invariants);

    // Check that every candidate invariant is disjunctively well-founded and transition invariant
    for (LoopStructure.Loop loop : loops) {
      BooleanFormula invariant = loopsToTransitionInvariants.get(loop);
      // Check the proper well-foundedness of the formula and if it succeeds, check R => T
      if (isTheFormulaWellFounded(loopsToTransitionInvariants.get(loop))
          && isCandidateInvariantTransitionInvariant(
              loop, loopsToTransitionInvariants.get(loop), loopsToSupportingInvariants.get(loop))) {
        continue;
      }

      // The formula is not well-founded, therefore we have to check for disjunctive
      // well-foundedness
      // And hence, we have to do check R^+ => T
      if (!isDisjunctivelyWellFounded(invariant)
          && isCandidateInvariantInductiveTransitionInvariant(
              loop, loopsToTransitionInvariants.get(loop), loopsToSupportingInvariants.get(loop))) {
        // The invariant is not disjunctively well-founded
        pReachedSet.add(new ARGState(DUMMY_TARGET_STATE, null), SingletonPrecision.getInstance());
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
    }
    pReachedSet.clear();
    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  private ImmutableMap<Loop, ImmutableList<BooleanFormula>> mapSupportingInvariantsToLoops(
      ImmutableCollection<LoopStructure.Loop> pLoops,
      Set<ExpressionTreeLocationInvariant> pInvariants)
      throws InterruptedException {
    ImmutableMap.Builder<Loop, ImmutableList<BooleanFormula>> builder =
        new ImmutableMap.Builder<>();

    for (LoopStructure.Loop loop : pLoops) {
      ImmutableList.Builder<BooleanFormula> builder1 = new ImmutableList.Builder<>();
      for (ExpressionTreeLocationInvariant invariant : pInvariants) {
        if (!invariant.isTransitionInvariant()) {
          // The check for supporting invariants is not yet supported
          // TODO: Implement checking that the invariants are really invariants
          if (isTheInvariantLocationInLoop(loop, invariant.getLocation())) {
            BooleanFormula invariantFormula;
            try {
              invariantFormula = invariant.getFormula(fmgr, pfmgr, pfmgr.makeEmptyPathFormula());
            } catch (CPATransferException e) {
              invariantFormula = bfmgr.makeFalse();
            }
            builder1.add(invariantFormula);
          }
        }
      }
      builder.put(loop, builder1.build());
    }
    return builder.buildOrThrow();
  }

  private ImmutableMap<LoopStructure.Loop, BooleanFormula> mapTransitionInvariantsToLoops(
      ImmutableCollection<LoopStructure.Loop> pLoops,
      Set<ExpressionTreeLocationInvariant> pInvariants)
      throws InterruptedException {
    ImmutableMap.Builder<LoopStructure.Loop, BooleanFormula> builder = new ImmutableMap.Builder<>();

    for (LoopStructure.Loop loop : pLoops) {
      BooleanFormula invariantForTheLoop = bfmgr.makeFalse();
      for (ExpressionTreeLocationInvariant invariant : pInvariants) {
        if (!invariant.isTransitionInvariant()) {
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
    return builder.buildOrThrow();
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
   * It does it using the check T(s,s') => [∃s1.T(s,s1) ∧ ¬T(s',s1)] ∧ [∀s2.T(s',s2) => T(s,s2)] If
   * this holds, it means that the number of states reachable from s is decreasing. In other words,
   * the cardinality of the set of reachable states is rank for every state.
   *
   * @param pFormula representing the transition invariant
   * @return true if the formula really is well-founded, false otherwise
   */
  private boolean isTheFormulaWellFounded(BooleanFormula pFormula) throws InterruptedException {
    SSAMap ssaMap = setIndicesToDifferentValues(pFormula, 1, 2);

    // T(s,s')
    BooleanFormula oneStep = fmgr.instantiate(pFormula, ssaMap);

    // T(s,s1), ¬T(s',s1)
    SSAMap ssaMapForS = setIndicesToDifferentValues(pFormula, 1, 3);
    BooleanFormula stepFromS = fmgr.instantiate(pFormula, ssaMapForS);
    SSAMap ssaMapForSPrime = setIndicesToDifferentValues(pFormula, 4, 3);
    BooleanFormula stepFromSPrime = fmgr.instantiate(pFormula, ssaMapForSPrime);
    stepFromSPrime = fmgr.makeNot(stepFromSPrime);

    // ∃s1. T(s,s1) ∧ ¬T(s',s1)
    BooleanFormula middleStep = fmgr.makeAnd(stepFromS, stepFromSPrime);
    middleStep = qfmgr.exists(collectAllCurrVariables(stepFromS), middleStep);

    // T(s,s2), T(s',s2)
    SSAMap ssaMapForS2 = setIndicesToDifferentValues(pFormula, 1, 5);
    BooleanFormula stepFromS2 = fmgr.instantiate(pFormula, ssaMapForS2);
    SSAMap ssaMapForSPrime2 = setIndicesToDifferentValues(pFormula, 4, 5);
    BooleanFormula stepFromSPrime2 = fmgr.instantiate(pFormula, ssaMapForSPrime2);

    // ∀s2.T(s',s2) => T(s,s2)
    BooleanFormula middleStep2 = bfmgr.implication(stepFromSPrime2, stepFromS2);
    middleStep2 = qfmgr.forall(collectAllCurrVariables(stepFromS2), middleStep2);

    // T(s,s') => [∃s1.T(s,s1) ∧ ¬T(s',s1)] ∧ [∀s2.T(s',s2) => T(s,s2)]
    BooleanFormula conclusion = bfmgr.and(middleStep, middleStep2);
    oneStep = bfmgr.and(makeStatesEquivalent(stepFromSPrime, oneStep, 4, 2), oneStep);
    BooleanFormula wellFoundedness = bfmgr.implication(oneStep, conclusion);
    wellFoundedness = bfmgr.not(wellFoundedness);

    boolean isWellfounded = false;
    try {
      isWellfounded = solver.isUnsat(wellFoundedness);
    } catch (SolverException e) {
      logger.log(
          Level.WARNING,
          "Well-Foundedness check failed ! Continuing with further division of the formula.");
    }

    return isWellfounded;
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
      if (currIndex < 0 && !var.contains("__PREV")) {
        continue;
      }
      builder.setIndex(
          var, scope.lookupVariable(var).getType(), var.contains("__PREV") ? prevIndex : currIndex);
    }
    return builder.build();
  }

  /**
   * Collects all the variables without the __PREV suffix.
   *
   * @param pFormula containing all the variables
   * @return List of the variables without __PREV suffix.
   */
  private ImmutableList<Formula> collectAllCurrVariables(Formula pFormula) {
    ImmutableList.Builder<Formula> builder = ImmutableList.builder();
    Map<String, Formula> mapNamesToVariables = fmgr.extractVariables(pFormula);
    for (Map.Entry<String, Formula> entry : mapNamesToVariables.entrySet()) {
      if (!entry.getKey().contains("__PREV")) {
        builder.add(mapNamesToVariables.get(entry.getKey()));
      }
    }
    return builder.build();
  }

  /**
   * Constructs formulas to make some states equivalent. For example, s' occurs in the formulas both
   * in T(.,s') and T(s',.), so we have to make the corresponding variables equivalent.
   *
   * @param pPrevFormula with the previous variables (i.e. variables like x__PREV)
   * @param pCurrFormula with the current variables (i.e. variables like x)
   * @param prevIndex the index of the variables from the previous state
   * @param currIndex the index of the current variables
   * @return the formula with terms like x__PREV@1 <==> x@2
   */
  private BooleanFormula makeStatesEquivalent(
      BooleanFormula pPrevFormula, BooleanFormula pCurrFormula, int prevIndex, int currIndex) {
    BooleanFormula equivalence = bfmgr.makeTrue();
    Map<String, Formula> prevMapNamesToVars = fmgr.extractVariables(pPrevFormula);
    Map<String, Formula> currMapNamesToVars = fmgr.extractVariables(pCurrFormula);

    for (Map.Entry<String, Formula> entry : prevMapNamesToVars.entrySet()) {
      String prevVar = entry.getKey();
      if (prevVar.contains("__PREV") && prevVar.contains("@" + prevIndex)) {
        String prevVarPure = prevVar.replace("__PREV", "");
        prevVarPure = prevVarPure.replace("@" + prevIndex, "");
        String currVar = "";
        for (String var : currMapNamesToVars.keySet()) {
          if (var.replace("@" + currIndex, "").equals(prevVarPure)) {
            currVar = var;
            break;
          }
        }
        if (!currVar.isEmpty()) {
          equivalence =
              fmgr.makeAnd(
                  equivalence,
                  fmgr.makeEqual(prevMapNamesToVars.get(prevVar), currMapNamesToVars.get(currVar)));
        }
      }
    }
    return equivalence;
  }

  /**
   * Checks whether the path formula R given by the loop implies the candidate invariants, i.e.
   * R=>T. This check is sufficient if we checked before that T is well-founded.
   *
   * @param pLoop for which we construct the path formula
   * @param pCandidateInvariant that we need to check
   * @return true if the candidate invariant is a transition invariant, false otherwise
   */
  private boolean isCandidateInvariantTransitionInvariant(
      LoopStructure.Loop pLoop,
      BooleanFormula pCandidateInvariant,
      ImmutableList<BooleanFormula> pSupportingInvariants)
      throws InterruptedException, CPATransferException {
    PathFormula loopFormula = pfmgr.makeFormulaForPath(new ArrayList<>(pLoop.getInnerLoopEdges()));
    pCandidateInvariant =
        fmgr.instantiate(
            pCandidateInvariant,
            SSAMap.merge(
                loopFormula.getSsa(),
                setIndicesToDifferentValues(pCandidateInvariant, 1, -1),
                MapsDifference.collectMapsDifferenceTo(new ArrayList<>())));
    BooleanFormula booleanLoopFormula = loopFormula.getFormula();

    // Strengthening the loop formula with the supporting invariants
    BooleanFormula strengtheningFormula = bfmgr.makeTrue();
    for (BooleanFormula supportingInvariant : pSupportingInvariants) {
      strengtheningFormula =
          bfmgr.and(
              strengtheningFormula,
              fmgr.instantiate(
                  supportingInvariant, setIndicesToDifferentValues(supportingInvariant, 1, 1)));
    }
    booleanLoopFormula = bfmgr.and(booleanLoopFormula, strengtheningFormula);

    // Instantiate __PREV variables to match the SSA indices of the variables in the loop.
    // In other words, add equivalences like x@1 = x__PREV@1
    booleanLoopFormula =
        bfmgr.and(
            booleanLoopFormula,
            makeStatesEquivalent(pCandidateInvariant, booleanLoopFormula, 1, 1));

    boolean isTransitionInvariant;
    try {
      isTransitionInvariant =
          solver.isUnsat(bfmgr.not(bfmgr.implication(booleanLoopFormula, pCandidateInvariant)));
    } catch (SolverException e) {
      logger.log(Level.WARNING, "Transition invariant check failed !");
      return false;
    }
    return isTransitionInvariant;
  }

  /**
   * Checks whether the formula can be divided into disjunction of formulas expressing relations
   * that are well-founded. We do it by transformation into DNF and then checking each respective
   * subformula.
   *
   * @param pFormula that is to be checked for disjunctive well-foundedness.
   * @return true if the formula is disjunctively well-founded, false otherwise.
   */
  private boolean isDisjunctivelyWellFounded(BooleanFormula pFormula) throws InterruptedException {
    Set<BooleanFormula> invariantInDNF = bfmgr.toDisjunctionArgs(pFormula, true);

    for (BooleanFormula candidateInvariant : invariantInDNF) {
      if (!isTheFormulaSimplyWellFounded(candidateInvariant)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks whether the path formula R given by the loop implies the candidate invariants, i.e.
   * R=>T. We also need to check that T(s,s') and R(s',s'') => T(s,s''), i.e. T is inductive because
   * it is not well-founded but disjunctively well-founded.
   *
   * @param pLoop for which we construct the path formula
   * @param pCandidateInvariant that we need to check
   * @return true if the candidate invariant is a transition invariant, false otherwise
   */
  private boolean isCandidateInvariantInductiveTransitionInvariant(
      LoopStructure.Loop pLoop,
      BooleanFormula pCandidateInvariant,
      ImmutableList<BooleanFormula> pSupportingInvariants)
      throws InterruptedException, CPATransferException {
    if (!isCandidateInvariantTransitionInvariant(
        pLoop, pCandidateInvariant, pSupportingInvariants)) {
      return false;
    }

    PathFormula loopFormula = pfmgr.makeFormulaForPath(new ArrayList<>(pLoop.getInnerLoopEdges()));
    BooleanFormula firstStep =
        fmgr.instantiate(
            pCandidateInvariant, setIndicesToDifferentValues(pCandidateInvariant, 0, 1));
    BooleanFormula secondStep =
        fmgr.instantiate(
            pCandidateInvariant,
            SSAMap.merge(
                loopFormula.getSsa(),
                setIndicesToDifferentValues(pCandidateInvariant, 0, -1),
                MapsDifference.collectMapsDifferenceTo(new ArrayList<>())));
    BooleanFormula booleanLoopFormula = loopFormula.getFormula();

    boolean isTransitionInvariant;
    try {
      isTransitionInvariant =
          solver.isUnsat(
              bfmgr.not(bfmgr.implication(bfmgr.and(firstStep, booleanLoopFormula), secondStep)));
    } catch (SolverException e) {
      logger.log(Level.WARNING, "Transition invariant check failed !");
      return false;
    }
    return isTransitionInvariant;
  }

  /**
   * Checks whether the formula is simply well-founded, i.e. T(s,s) holds. We can do it as R^+ => T
   * means that T(s,s) violates well-foundedness on reachable states.
   */
  private boolean isTheFormulaSimplyWellFounded(BooleanFormula pFormula)
      throws InterruptedException {
    pFormula = fmgr.instantiate(pFormula, setIndicesToDifferentValues(pFormula, 1, 1));
    pFormula = bfmgr.implication(makeStatesEquivalent(pFormula, pFormula, 1, 1), pFormula);

    try {
      // Checks well-foundedness as
      if (solver.isUnsat(bfmgr.not(pFormula))) {
        return false;
      }
    } catch (SolverException e) {
      logger.log(Level.WARNING, "Disjunctive well-foundedness check failed !");
      return false;
    }
    return true;
  }
}
