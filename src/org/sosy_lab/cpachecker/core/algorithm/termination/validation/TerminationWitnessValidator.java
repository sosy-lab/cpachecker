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
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.MapsDifference;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cmdline.CPAMain;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.DecreasingCardinalityChecker;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.ImplicitRankingChecker;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.WellFoundednessChecker;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
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
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "termination.validation")
public class TerminationWitnessValidator implements Algorithm {

  @Option(
      secure = true,
      name = "checkWithInfiniteSpace",
      description = "toggle to assume possible infinite state space in transition invariant")
  private boolean checkWithInfiniteSpace = false;

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
  private final Solver solver;
  private final Scope scope;
  private final WellFoundednessChecker wellFoundednessChecker;

  public TerminationWitnessValidator(
      final CFA pCfa,
      final ConfigurableProgramAnalysis pCPA,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final ImmutableSet<Path> pWitnessPath,
      final Specification pSpecification)
      throws InvalidConfigurationException {
    pConfig.inject(this);
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
    PredicateCPA predCpa =
        CPAs.retrieveCPAOrFail(pCPA, PredicateCPA.class, TerminationWitnessValidator.class);
    solver = predCpa.getSolver();
    pfmgr = predCpa.getPathFormulaManager();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();

    if (pWitnessPath.size() < 1) {
      throw new InvalidConfigurationException("Witness file is missing in specification.");
    }
    if (pWitnessPath.size() != 1) {
      throw new InvalidConfigurationException(
          "Expect that only violation witness is part of the specification.");
    }

    if (checkWithInfiniteSpace) {
      wellFoundednessChecker =
          new ImplicitRankingChecker(
              fmgr, bfmgr, logger, config, shutdownNotifier, pSpecification, scope, cfa);
    } else {
      wellFoundednessChecker = new DecreasingCardinalityChecker(fmgr, bfmgr, solver, scope, logger);
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

    // Check the supporting invariants first
    logger.log(Level.FINE, "Checking the supporting invariants.");
    if (hasSupportingInvariants(loopsToSupportingInvariants)) {
      ReachedSet reachedSet = checkSupportingInvariants();
      if (reachedSet.wasTargetReached()) {
        // Supporting invariants are not invariants
        pReachedSet.addNoWaitlist(
            DUMMY_TARGET_STATE, pReachedSet.getPrecision(pReachedSet.getFirstState()));
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
    }

    // Check that every candidate invariant is disjunctively well-founded and transition invariant
    for (LoopStructure.Loop loop : loops) {
      if (loop.getIncomingEdges().isEmpty()) {
        // The loop is not reachable due to prunning in CFA construction
        logger.log(Level.INFO, "A loop is not reachable !");
        continue;
      }
      BooleanFormula invariant = loopsToTransitionInvariants.get(loop);
      if (!checkWithInfiniteSpace && hasInfiniteSpace(invariant)) {
        throw new CPAException("The configuration does not support infinite state spaces.");
      }
      // Check the proper well-foundedness of the formula and if it succeeds, check R => T
      if (wellFoundednessChecker.isWellFounded(
              invariant, loopsToSupportingInvariants.get(loop), loop)
          && isCandidateInvariantTransitionInvariant(
              loop, loopsToTransitionInvariants.get(loop), loopsToSupportingInvariants.get(loop))) {
        continue;
      }

      // The formula is not well-founded, therefore we have to check for disjunctive
      // well-foundedness
      // And hence, we have to do check R^+ => T
      boolean isWellFounded =
          wellFoundednessChecker.isDisjunctivelyWellFounded(
              invariant, loopsToSupportingInvariants.get(loop), loop);
      // Our termination analysis might be unsound with respect to C semantics because it assumes
      // infinite state space.
      if (!isWellFounded && wellFoundednessChecker instanceof ImplicitRankingChecker) {
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
      }
      if (!isWellFounded
          || !isCandidateInvariantInductiveTransitionInvariant(
              loop, loopsToTransitionInvariants.get(loop), loopsToSupportingInvariants.get(loop))) {
        // The invariant is not disjunctively well-founded
        pReachedSet.addNoWaitlist(
            DUMMY_TARGET_STATE, pReachedSet.getPrecision(pReachedSet.getFirstState()));
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
    }
    pReachedSet.clear();
    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  private boolean hasSupportingInvariants(
      ImmutableMap<LoopStructure.Loop, ImmutableList<BooleanFormula>>
          pLoopsToSupportingInvariants) {
    return !pLoopsToSupportingInvariants.entrySet().stream()
        .filter(e -> !e.getValue().isEmpty())
        .toList()
        .isEmpty();
  }

  private ReachedSet checkSupportingInvariants() throws CPAException {
    try {
      Path invariantsSpecPath =
          Classes.getCodeLocation(TerminationWitnessValidator.class)
              .resolveSibling("config/properties/no-overflow.prp");
      Path validationConfigPath =
          Classes.getCodeLocation(TerminationWitnessValidator.class)
              .resolveSibling("config/witnessValidation.properties");
      Configuration generationConfig =
          CPAMain.createConfiguration(
                  new String[] {
                    "--witness",
                    witnessPath.toString(),
                    "--spec",
                    invariantsSpecPath.toString(),
                    "--config",
                    validationConfigPath.toString(),
                    "--no-output-files",
                  })
              .configuration();
      Specification invariantSpec =
          Specification.fromFiles(
              Arrays.asList(Path.of(invariantsSpecPath.toString()), witnessPath),
              cfa,
              generationConfig,
              logger,
              shutdownNotifier);
      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(
              generationConfig, logger, shutdownNotifier, AggregatedReachedSets.empty());
      ConfigurableProgramAnalysis supportingInvariantsCPA =
          coreComponents.createCPA(cfa, invariantSpec);
      Algorithm invariantCheckingAlgorithm =
          coreComponents.createAlgorithm(supportingInvariantsCPA, cfa, invariantSpec);

      ReachedSetFactory reachedSetFactory = new ReachedSetFactory(config, logger);
      ForwardingReachedSet reachedSet =
          new ForwardingReachedSet(reachedSetFactory.create(supportingInvariantsCPA));
      AbstractState initialState =
          supportingInvariantsCPA.getInitialState(
              cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
      Precision initialPrecision =
          supportingInvariantsCPA.getInitialPrecision(
              cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
      reachedSet.add(initialState, initialPrecision);

      // Running the algorithm
      invariantCheckingAlgorithm.run(reachedSet);
      return reachedSet;
    } catch (Exception e) {
      throw new CPAException(e.toString());
    }
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
      throws InterruptedException, CPATransferException {
    ImmutableMap.Builder<LoopStructure.Loop, BooleanFormula> builder = new ImmutableMap.Builder<>();

    for (LoopStructure.Loop loop : pLoops) {
      BooleanFormula invariantForTheLoop = bfmgr.makeTrue();
      PathFormula loopFormula = pfmgr.makeFormulaForPath(new ArrayList<>(loop.getInnerLoopEdges()));
      for (ExpressionTreeLocationInvariant invariant : pInvariants) {
        if (!invariant.isTransitionInvariant()) {
          continue;
        }

        if (isTheInvariantLocationInLoop(loop, invariant.getLocation())) {
          BooleanFormula invariantFormula;
          try {
            if (invariant.asExpressionTree().toString().equals("true")) {
              invariantFormula = bfmgr.makeTrue();
            } else {
              invariantFormula = invariant.getFormula(fmgr, pfmgr, loopFormula);
            }
          } catch (CPATransferException e) {
            invariantFormula = bfmgr.makeTrue();
          }
          invariantForTheLoop = bfmgr.and(invariantForTheLoop, invariantFormula);
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
   * Checks whether the path formula R given by the loop implies the candidate invariants, i.e.
   * R=>T. This check is sufficient if we checked before that T is well-founded.
   *
   * @param pLoop for which we construct the path formula
   * @param pCandidateInvariant that we need to check
   * @param pSupportingInvariants that help to strengthen the formula
   * @return true if the candidate invariant is a transition invariant, false otherwise
   * @throws InterruptedException If an interruption event happens
   * @throws CPATransferException If a satisfiability check fails
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
                TransitionInvariantUtils.setIndicesToDifferentValues(
                    pCandidateInvariant, 1, -1, fmgr, scope),
                MapsDifference.collectMapsDifferenceTo(new ArrayList<>())));
    BooleanFormula booleanLoopFormula = loopFormula.getFormula();

    // Strengthening the loop formula with the supporting invariants
    BooleanFormula strengtheningFormula = bfmgr.makeTrue();
    for (BooleanFormula supportingInvariant : pSupportingInvariants) {
      strengtheningFormula =
          bfmgr.and(
              strengtheningFormula,
              fmgr.instantiate(
                  supportingInvariant,
                  TransitionInvariantUtils.setIndicesToDifferentValues(
                      supportingInvariant, 1, 1, fmgr, scope)));
    }
    booleanLoopFormula = bfmgr.and(booleanLoopFormula, strengtheningFormula);

    // Instantiate __PREV variables to match the SSA indices of the variables in the loop.
    // In other words, add equivalences like x@1 = x__PREV@1
    booleanLoopFormula =
        bfmgr.and(
            booleanLoopFormula,
            TransitionInvariantUtils.makeStatesEquivalent(
                pCandidateInvariant, booleanLoopFormula, 1, 1, bfmgr, fmgr));

    boolean isTransitionInvariant;
    try {
      isTransitionInvariant = solver.implies(booleanLoopFormula, pCandidateInvariant);
    } catch (SolverException e) {
      logger.log(Level.WARNING, "Transition invariant check failed !");
      return false;
    }
    return isTransitionInvariant;
  }

  /**
   * Checks whether the path formula R given by the loop implies the candidate invariants, i.e.
   * R=>T. We also need to check that T(s,s') and R(s',s'') => T(s,s''), i.e. T is inductive because
   * it is not well-founded but disjunctively well-founded.
   *
   * @param pLoop for which we construct the path formula
   * @param pCandidateInvariant that we need to check
   * @param pSupportingInvariants that help to strengthen the formula
   * @return true if the candidate invariant is a transition invariant, false otherwise
   * @throws InterruptedException If an interruption event happens
   * @throws CPATransferException If a satisfiability check fails
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
    PathFormula loopFormula =
        TransitionInvariantUtils.makeLoopFormulaWithInitialSSAIndex(
            new ArrayList<>(pLoop.getInnerLoopEdges()), pfmgr);
    BooleanFormula booleanLoopFormula = loopFormula.getFormula();

    BooleanFormula firstStep =
        fmgr.instantiate(
            pCandidateInvariant,
            TransitionInvariantUtils.setIndicesToDifferentValues(
                pCandidateInvariant, 1, 2, fmgr, scope));
    firstStep =
        bfmgr.and(
            firstStep,
            TransitionInvariantUtils.makeStatesEquivalent(
                firstStep, booleanLoopFormula, 1, 2, bfmgr, fmgr));

    BooleanFormula secondStep =
        fmgr.instantiate(
            pCandidateInvariant,
            SSAMap.merge(
                loopFormula.getSsa(),
                TransitionInvariantUtils.setIndicesToDifferentValues(
                        pCandidateInvariant, 1, -1, fmgr, scope)
                    .withDefault(2),
                MapsDifference.collectMapsDifferenceTo(new ArrayList<>())));

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

  private boolean hasInfiniteSpace(BooleanFormula pInvariant) {
    Map<String, Formula> mapNamesToVariables = fmgr.extractVariables(pInvariant);
    for (Formula variable : mapNamesToVariables.values()) {
      if (fmgr.getFormulaType(variable).isArrayType()) {
        return true;
      }
    }
    return false;
  }
}
