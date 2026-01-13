// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cmdline.CPAMain;
import org.sosy_lab.cpachecker.cmdline.InvalidCmdlineArgumentException;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.DecreasingCardinalityChecker;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.ImplicitRankingChecker;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.CurrStateIndices;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.PrevStateIndices;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.WellFoundednessChecker;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor.InvalidWitnessException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.ExpressionTreeLocationTransitionInvariant;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "termination.validation")
public class TerminationWitnessValidator implements Algorithm {

  @Option(
      secure = true,
      name = "checkWithInfiniteSpace",
      description =
          "This option can be set to run an analysis that supports infinite state spaces of"
              + " programs.The analysis will automatically run the ImplicitRankingChecker to check"
              + " the well-foundedness of an invariant.If the option is not set, then such analysis"
              + " is ran only if the infinite state space is detected.")
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
    if (!cfa.getLanguage().equals(Language.C)) {
      throw new InvalidConfigurationException(
          "The validation of termination witnesses does not support other language than C.");
    }
    scope = new CProgramScope(cfa, logger);

    @SuppressWarnings("resource")
    PredicateCPA predCpa =
        CPAs.retrieveCPAOrFail(pCPA, PredicateCPA.class, TerminationWitnessValidator.class);
    solver = predCpa.getSolver();
    pfmgr = predCpa.getPathFormulaManager();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();

    if (pWitnessPath.size() != 1) {
      throw new InvalidConfigurationException(
          "Expected exactly one correctness witness as input of the algorithm.");
    }

    if (checkWithInfiniteSpace) {
      wellFoundednessChecker =
          new ImplicitRankingChecker(
              fmgr, bfmgr, logger, config, shutdownNotifier, pSpecification, scope, cfa);
    } else {
      wellFoundednessChecker = new DecreasingCardinalityChecker(fmgr, bfmgr, solver, scope);
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
    ImmutableListMultimap<LoopStructure.Loop, BooleanFormula> loopsToSupportingInvariants =
        mapSupportingInvariantsToLoops(loops, invariants);

    // Check the supporting invariants first
    logger.log(Level.FINE, "Checking the supporting invariants.");
    if (hasSupportingInvariants(loopsToSupportingInvariants)) {
      if (areSupportingInvariantsCorrect()) {
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
      if (!loopsToTransitionInvariants.containsKey(loop)
          && !loopsToSupportingInvariants.containsKey(loop)) {
        return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
      }

      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> mapPrevVarsToCurrVars =
          joinPrevDeclarationMapsForLoop(loop, invariants);
      BooleanFormula invariant = loopsToTransitionInvariants.get(loop);
      ImmutableList<BooleanFormula> supportingInvariants = loopsToSupportingInvariants.get(loop);

      if (!checkWithInfiniteSpace) {
        logger.log(
            Level.INFO,
            "The chosen configuration does not support infinite state space in the invariant. Make"
                + " sure the invariant does not contain variables with potential infinite"
                + " domains.");
      }
      int k = 1;
      // Check the proper well-foundedness of the formula and if it succeeds, check R => T
      if (wellFoundednessChecker.isWellFounded(
              invariant, supportingInvariants, loop, mapPrevVarsToCurrVars)
          && isCandidateInvariantTransitionInvariant(
              loop,
              loopsToTransitionInvariants.get(loop),
              supportingInvariants,
              loopsToSupportingInvariants,
              mapPrevVarsToCurrVars,
              k)) {
        continue;
      }

      // The formula is not well-founded, therefore we have to check for disjunctive
      // well-foundedness
      // And hence, we have to do check R^+ => T
      boolean isWellFounded =
          wellFoundednessChecker.isDisjunctivelyWellFounded(
              invariant, supportingInvariants, loop, mapPrevVarsToCurrVars);
      // Our termination analysis might be unsound because of a different possible division to
      // disjunctions
      if (!isWellFounded) {
        return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
      }
      // Do k-inductivity checks for k > 1
      while (isCandidateInvariantTransitionInvariant(
          loop,
          loopsToTransitionInvariants.get(loop),
          supportingInvariants,
          loopsToSupportingInvariants,
          mapPrevVarsToCurrVars,
          k)) {
        if (isCandidateInvariantInductiveTransitionInvariant(
            loop,
            loopsToTransitionInvariants.get(loop),
            loopsToSupportingInvariants,
            supportingInvariants,
            mapPrevVarsToCurrVars,
            k)) {
          break;
        }
        k++;
      }
    }
    pReachedSet.clear();

    // The analysis might be imprecise due to the usage of cfa.getInnerEdges in the candidate
    // invariant check,
    // this might sometimes return also edges that are not really inside the loop. This behaviour is
    // overapproximating.
    return AlgorithmStatus.SOUND_AND_IMPRECISE;
  }

  private boolean hasSupportingInvariants(
      ImmutableListMultimap<Loop, BooleanFormula> pLoopsToSupportingInvariants) {
    return !pLoopsToSupportingInvariants.keys().isEmpty();
  }

  private boolean areSupportingInvariantsCorrect() throws CPAException, InterruptedException {
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
              ImmutableList.of(Path.of(invariantsSpecPath.toString()), witnessPath),
              cfa,
              generationConfig,
              logger,
              shutdownNotifier);
      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(
              generationConfig, logger, shutdownNotifier, AggregatedReachedSets.empty(), cfa);
      ConfigurableProgramAnalysis supportingInvariantsCPA = coreComponents.createCPA(invariantSpec);
      Algorithm invariantCheckingAlgorithm =
          coreComponents.createAlgorithm(supportingInvariantsCPA, invariantSpec);

      ReachedSet reachedSet =
          coreComponents.createInitializedReachedSet(
              supportingInvariantsCPA, cfa.getMainFunction());

      // Running the algorithm
      invariantCheckingAlgorithm.run(reachedSet);
      return reachedSet.wasTargetReached();
    } catch (InvalidConfigurationException | InvalidCmdlineArgumentException | IOException e) {
      throw new CPAException("Supporting invariants check failed: ", e);
    }
  }

  private ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> joinPrevDeclarationMapsForLoop(
      Loop pLoop, Set<ExpressionTreeLocationInvariant> invariants) {
    ImmutableMap.Builder<CSimpleDeclaration, CSimpleDeclaration> builder = ImmutableMap.builder();

    FluentIterable.from(invariants)
        .filter(
            invariant ->
                invariant instanceof ExpressionTreeLocationTransitionInvariant
                    && isTheInvariantLocationInLoop(pLoop, invariant.getLocation()))
        .transform(ExpressionTreeLocationTransitionInvariant.class::cast)
        .transformAndConcat(inv -> inv.getMapPrevVarsToCurrent().entrySet())
        .forEach(e -> builder.put(e.getKey(), e.getValue()));

    return builder.buildOrThrow();
  }

  private ImmutableListMultimap<Loop, BooleanFormula> mapSupportingInvariantsToLoops(
      ImmutableCollection<LoopStructure.Loop> pLoops,
      Set<ExpressionTreeLocationInvariant> pInvariants)
      throws InterruptedException {
    ImmutableListMultimap.Builder<Loop, BooleanFormula> builder =
        new ImmutableListMultimap.Builder<>();

    for (LoopStructure.Loop loop : pLoops) {
      for (ExpressionTreeLocationInvariant invariant : pInvariants) {
        if (!(invariant instanceof ExpressionTreeLocationTransitionInvariant)) {
          if (isTheInvariantLocationInLoop(loop, invariant.getLocation())) {
            BooleanFormula invariantFormula;
            try {
              invariantFormula = invariant.getFormula(fmgr, pfmgr, pfmgr.makeEmptyPathFormula());
            } catch (CPATransferException e) {
              invariantFormula = bfmgr.makeTrue();
            }
            builder.put(loop, invariantFormula);
          }
        }
      }
    }
    return builder.build();
  }

  private ImmutableMap<LoopStructure.Loop, BooleanFormula> mapTransitionInvariantsToLoops(
      ImmutableCollection<LoopStructure.Loop> pLoops,
      Set<ExpressionTreeLocationInvariant> pInvariants)
      throws InterruptedException, CPATransferException {
    ImmutableMap.Builder<LoopStructure.Loop, BooleanFormula> builder = new ImmutableMap.Builder<>();

    for (LoopStructure.Loop loop : pLoops) {
      BooleanFormula invariantForTheLoop = bfmgr.makeTrue();
      boolean isTrivial = true;
      PathFormula loopFormula = pfmgr.makeFormulaForPath(loop.getInnerLoopEdges().asList());
      for (ExpressionTreeLocationInvariant invariant : pInvariants) {
        if (!(invariant instanceof ExpressionTreeLocationTransitionInvariant)) {
          continue;
        }

        if (isTheInvariantLocationInLoop(loop, invariant.getLocation())) {
          BooleanFormula invariantFormula;
          try {
            if (invariant.asExpressionTree().equals(ExpressionTrees.getTrue())) {
              invariantFormula = bfmgr.makeTrue();
            } else {
              invariantFormula = invariant.getFormula(fmgr, pfmgr, loopFormula);
            }
          } catch (CPATransferException e) {
            invariantFormula = bfmgr.makeTrue();
          }
          invariantForTheLoop = bfmgr.and(invariantForTheLoop, invariantFormula);
          isTrivial = false;
        }
      }
      if (!isTrivial) {
        builder.put(loop, invariantForTheLoop);
      }
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
   * R^k=>T. The check with R^1=>T is sufficient if we checked before that T is well-founded.
   *
   * @param pLoop for which we construct the path formula
   * @param pCandidateInvariant that we need to check
   * @param pSupportingInvariants that help to strengthen the formula
   * @param k is an index determining how many loop unrollings we need to take into account
   * @return true if the candidate invariant is a transition invariant, false otherwise
   * @throws InterruptedException If an interruption event happens
   * @throws CPATransferException If a satisfiability check fails
   */
  private boolean isCandidateInvariantTransitionInvariant(
      LoopStructure.Loop pLoop,
      BooleanFormula pCandidateInvariant,
      ImmutableList<BooleanFormula> pSupportingInvariants,
      ImmutableListMultimap<Loop, BooleanFormula> pLoopsToSupportingInvariants,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars,
      int k)
      throws InterruptedException, CPATransferException {

    // We first construct the loop formula, i.e. R^k, where k is at least 1
    PathFormula loopFormula =
        constructPathFormulaForLoop(
            pLoop.getInnerLoopEdges(),
            pLoop.getLoopHeads(),
            SSAMap.emptySSAMap(),
            pLoopsToSupportingInvariants);

    // Strengthening the loop formula with the supporting invariants
    BooleanFormula strengtheningFormula = bfmgr.makeTrue();

    // Strengthening the loop formula with the supporting invariants
    for (BooleanFormula supportingInvariant : pSupportingInvariants) {
      strengtheningFormula =
          bfmgr.and(
              strengtheningFormula,
              fmgr.instantiate(
                  supportingInvariant,
                  TransitionInvariantUtils.setIndicesToDifferentValues(
                      pCandidateInvariant,
                      PrevStateIndices.INDEX_FIRST,
                      CurrStateIndices.INDEX_MIDDLE,
                      fmgr,
                      scope,
                      pMapPrevToCurrVars)));
    }
    for (int i = 1; i < k; i++) {
      loopFormula =
          pfmgr.makeConjunction(
              ImmutableList.of(
                  loopFormula,
                  constructPathFormulaForLoop(
                      pLoop.getInnerLoopEdges(),
                      pLoop.getLoopHeads(),
                      loopFormula.getSsa(),
                      pLoopsToSupportingInvariants)));

      // Strengthening the loop formula with the supporting invariants
      for (BooleanFormula supportingInvariant : pSupportingInvariants) {
        strengtheningFormula =
            bfmgr.and(
                strengtheningFormula, fmgr.instantiate(supportingInvariant, loopFormula.getSsa()));
      }
    }
    SSAMap fullSSAMap =
        SSAMap.merge(
            loopFormula.getSsa(),
            TransitionInvariantUtils.setIndicesToDifferentValues(
                pCandidateInvariant,
                PrevStateIndices.INDEX_FIRST,
                CurrStateIndices.INDEX_LATEST,
                fmgr,
                scope,
                pMapPrevToCurrVars),
            MapsDifference.collectMapsDifferenceTo(new ArrayList<>()));
    SSAMap oneStepSSAMap =
        TransitionInvariantUtils.setIndicesToDifferentValues(
            pCandidateInvariant,
            PrevStateIndices.INDEX_FIRST,
            CurrStateIndices.INDEX_MIDDLE,
            fmgr,
            scope,
            pMapPrevToCurrVars);

    pCandidateInvariant = fmgr.instantiate(pCandidateInvariant, fullSSAMap);
    BooleanFormula booleanLoopFormula = loopFormula.getFormula();

    booleanLoopFormula = bfmgr.and(booleanLoopFormula, strengtheningFormula);

    // Instantiate __PREV variables to match the SSA indices of the variables in the loop.
    // In other words, add equivalences like x@1 = x__PREV@1
    booleanLoopFormula =
        bfmgr.and(
            booleanLoopFormula,
            fmgr.instantiate(
                fmgr.uninstantiate(
                    TransitionInvariantUtils.makeStatesEquivalent(
                        pCandidateInvariant, booleanLoopFormula, bfmgr, fmgr, pMapPrevToCurrVars)),
                oneStepSSAMap));

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
   * This function assumes that the loop formula R applied k times implies the candidate transition
   * invariant T, i.e. R^k => T. We also need to check that T(s,s') and R^k(s',s'') => T(s,s''),
   * i.e. T is k-inductive because it is not well-founded but disjunctively well-founded.
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
      ImmutableListMultimap<Loop, BooleanFormula> pLoopsToSupportingInvariants,
      ImmutableList<BooleanFormula> pSupportingInvariants,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars,
      int k)
      throws InterruptedException, CPATransferException {

    // We first construct the loop formula, i.e. R^k, where k is at least 1
    PathFormula loopFormula =
        constructPathFormulaForLoop(
            pLoop.getInnerLoopEdges(),
            pLoop.getLoopHeads(),
            SSAMap.emptySSAMap(),
            pLoopsToSupportingInvariants);

    // The one that is used with the supporting invariants
    BooleanFormula strengtheningFormula = bfmgr.makeTrue();

    // Strengthening the loop formula with the supporting invariants
    for (BooleanFormula supportingInvariant : pSupportingInvariants) {
      strengtheningFormula =
          bfmgr.and(
              strengtheningFormula,
              fmgr.instantiate(
                  supportingInvariant,
                  TransitionInvariantUtils.setIndicesToDifferentValues(
                      pCandidateInvariant,
                      PrevStateIndices.INDEX_FIRST,
                      CurrStateIndices.INDEX_MIDDLE,
                      fmgr,
                      scope,
                      pMapPrevToCurrVars)));
    }
    for (int i = 1; i < k; i++) {
      loopFormula =
          pfmgr.makeConjunction(
              ImmutableList.of(
                  loopFormula,
                  constructPathFormulaForLoop(
                      pLoop.getInnerLoopEdges(),
                      pLoop.getLoopHeads(),
                      loopFormula.getSsa(),
                      pLoopsToSupportingInvariants)));

      // Strengthening the loop formula with the supporting invariants
      for (BooleanFormula supportingInvariant : pSupportingInvariants) {
        strengtheningFormula =
            bfmgr.and(
                strengtheningFormula, fmgr.instantiate(supportingInvariant, loopFormula.getSsa()));
      }
      strengtheningFormula = bfmgr.and(strengtheningFormula, strengtheningFormula);
    }
    BooleanFormula booleanLoopFormula = bfmgr.and(loopFormula.getFormula(), strengtheningFormula);

    BooleanFormula firstStep =
        fmgr.instantiate(
            pCandidateInvariant,
            TransitionInvariantUtils.setIndicesToDifferentValues(
                pCandidateInvariant,
                PrevStateIndices.INDEX_FIRST,
                CurrStateIndices.INDEX_MIDDLE,
                fmgr,
                scope,
                pMapPrevToCurrVars));

    BooleanFormula secondStep =
        fmgr.instantiate(
            pCandidateInvariant,
            SSAMap.merge(
                loopFormula.getSsa(),
                TransitionInvariantUtils.setIndicesToDifferentValues(
                    pCandidateInvariant,
                    PrevStateIndices.INDEX_FIRST,
                    CurrStateIndices.INDEX_LATEST,
                    fmgr,
                    scope,
                    pMapPrevToCurrVars),
                MapsDifference.collectMapsDifferenceTo(new ArrayList<>())));
    boolean isTransitionInvariant;
    try {
      isTransitionInvariant = solver.implies(bfmgr.and(firstStep, booleanLoopFormula), secondStep);
    } catch (SolverException e) {
      logger.log(Level.WARNING, "Transition invariant check failed !");
      return false;
    }
    return isTransitionInvariant;
  }

  private PathFormula constructPathFormulaForLoop(
      ImmutableSet<CFAEdge> pEdges,
      ImmutableSet<CFANode> pLoopHeads,
      SSAMap pContextSSAMap,
      ImmutableListMultimap<Loop, BooleanFormula> pLoopsToSupportingInvariants)
      throws CPATransferException, InterruptedException {
    List<List<CFAEdge>> listOfAllPaths = collectAllThePaths(pEdges, pLoopHeads);
    return constructFormulaForPaths(pContextSSAMap, pLoopsToSupportingInvariants, listOfAllPaths);
  }

  private PathFormula constructFormulaForPaths(
      SSAMap pContextSSAMap,
      ImmutableListMultimap<Loop, BooleanFormula> pLoopsToSupportingInvariants,
      List<List<CFAEdge>> listOfAllPaths)
      throws CPATransferException, InterruptedException {
    PathFormula formulaForLoop = pfmgr.makeEmptyPathFormula();
    formulaForLoop =
        formulaForLoop.withContext(pContextSSAMap, PointerTargetSet.emptyPointerTargetSet());

    ImmutableSet<LoopStructure.Loop> AllLoops = pLoopsToSupportingInvariants.keySet();
    boolean initialized = false;
    for (List<CFAEdge> path : listOfAllPaths) {
      PathFormula anotherPath = pfmgr.makeEmptyPathFormula();
      anotherPath =
          anotherPath.withContext(pContextSSAMap, PointerTargetSet.emptyPointerTargetSet());
      boolean followingDifferentLoop = false;
      for (CFAEdge edge : path) {
        ImmutableSet<LoopStructure.Loop> loopsForEdge =
            AllLoops.stream()
                .filter(l -> l.getInnerLoopEdges().contains(edge))
                .collect(ImmutableSet.toImmutableSet());
        if (loopsForEdge.size() <= 1) {
          anotherPath = pfmgr.makeAnd(anotherPath, edge);
          followingDifferentLoop = false;
        } else if (!followingDifferentLoop) {
          BooleanFormula overapproximatingState = bfmgr.makeTrue();
          for (LoopStructure.Loop loop : loopsForEdge) {
            overapproximatingState =
                bfmgr.and(
                    overapproximatingState, bfmgr.and(pLoopsToSupportingInvariants.get(loop)));
          }
          if (overapproximatingState.equals(bfmgr.makeTrue())) {
            return pfmgr.makeEmptyPathFormula();
          }
          anotherPath = pfmgr.makeAnd(anotherPath, overapproximatingState);
          followingDifferentLoop = true;
        }
      }
      if (!initialized) {
        initialized = true;
        formulaForLoop = anotherPath;
      } else {
        formulaForLoop = pfmgr.makeOr(formulaForLoop, anotherPath);
      }
    }
    return formulaForLoop;
  }

  private List<List<CFAEdge>> collectAllThePaths(
      ImmutableSet<CFAEdge> pEdges, ImmutableSet<CFANode> pLoopHeads) {
    List<List<CFAEdge>> listOfAllPaths = new ArrayList<>();
    for (CFAEdge edge : pEdges) {
      if (pLoopHeads.contains(edge.getPredecessor())) {
        listOfAllPaths.add(new ArrayList<>());
        listOfAllPaths.getLast().add(edge);
      }
    }
    boolean updated = true;
    while (updated) {
      updated = false;
      List<List<CFAEdge>> newPaths = new ArrayList<>();
      for (List<CFAEdge> path : listOfAllPaths) {
        CFAEdge lastEdge = path.getLast();
        List<CFAEdge> succEdges =
            pEdges.stream()
                .filter(
                    e ->
                        e.getPredecessor().equals(lastEdge.getSuccessor())
                            && !pLoopHeads.contains(e.getPredecessor()))
                .toList();
        if (!succEdges.isEmpty()) {
          if (succEdges.size() > 1) {
            for (int i = 1; i < succEdges.size(); i++) {
              newPaths.add(new ArrayList<>(path));
              newPaths.getLast().add(succEdges.get(i));
            }
          }
          updated = true;
          path.add(succEdges.getFirst());
        }
      }
      if (!newPaths.isEmpty()) {
        listOfAllPaths.addAll(newPaths);
      }
    }
    return listOfAllPaths;
  }
}
