// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.MapsDifference;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
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
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
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
import org.sosy_lab.cpachecker.cpa.terminationviamemory.NestedLoopSummarizer;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
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
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
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

  @Option(
      secure = true,
      description =
          "maximal k for the k-induction that checks whether a disjunctively well-founded"
              + " candidate is a transition invariant, i.e., R^+ => T")
  @IntegerOption(min = 1)
  private int maxInductionDepth = 5;

  private static final DummyTargetState DUMMY_TARGET_STATE =
      DummyTargetState.withSimpleTargetInformation("termination");

  // The SSA index of variables that are not assigned yet in a path formula
  private static final int INITIAL_SSA_INDEX = 1;

  private final Path witnessPath;
  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;
  private final Specification specification;
  private final NestedLoopSummarizer nestedLoopSummarizer;
  // The scope and the checker are created in run(), since the scope has to contain the
  // declarations of the __PREV variables from the witness
  private Scope scope;
  private WellFoundednessChecker wellFoundednessChecker;

  // The invariants from the witness, which are used to summarize nested loops. They are set in
  // run().
  private ImmutableSet<ExpressionTreeLocationInvariant> witnessInvariants = ImmutableSet.of();
  private ImmutableMap<Loop, BooleanFormula> transitionInvariantsOfLoops = ImmutableMap.of();
  private final Set<String> functionsOnCallStack = new HashSet<>();
  private ImmutableListMultimap<Loop, BooleanFormula> supportingInvariantsOfLoops =
      ImmutableListMultimap.of();

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
    specification = pSpecification;

    @SuppressWarnings("resource")
    PredicateCPA predCpa =
        CPAs.retrieveCPAOrFail(pCPA, PredicateCPA.class, TerminationWitnessValidator.class);
    solver = predCpa.getSolver();
    pfmgr = predCpa.getPathFormulaManager();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    nestedLoopSummarizer = new NestedLoopSummarizer(fmgr, bfmgr, cfa);

    if (pWitnessPath.isEmpty()) {
      throw new InvalidConfigurationException("Witness file is missing in specification.");
    }
    if (pWitnessPath.size() != 1) {
      throw new InvalidConfigurationException(
          "Expected exactly one correctness witness as input of the algorithm.");
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

    scope =
        new CProgramScope(cfa, logger)
            .withAdditionalDeclarations(collectPrevVariableDeclarations(invariants));
    if (checkWithInfiniteSpace) {
      wellFoundednessChecker =
          new ImplicitRankingChecker(
              fmgr, bfmgr, logger, config, shutdownNotifier, specification, scope, cfa);
    } else {
      wellFoundednessChecker = new DecreasingCardinalityChecker(fmgr, bfmgr, solver, scope);
    }

    ImmutableMap<LoopStructure.Loop, BooleanFormula> loopsToTransitionInvariants =
        mapTransitionInvariantsToLoops(loops, invariants);
    ImmutableListMultimap<LoopStructure.Loop, BooleanFormula> loopsToSupportingInvariants =
        mapSupportingInvariantsToLoops(loops, invariants);
    witnessInvariants = ImmutableSet.copyOf(invariants);
    transitionInvariantsOfLoops = loopsToTransitionInvariants;
    supportingInvariantsOfLoops = loopsToSupportingInvariants;

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

    try {
      return checkTransitionInvariants(
          pReachedSet, loops, invariants, loopsToTransitionInvariants, loopsToSupportingInvariants);
    } catch (UnsupportedLoopException e) {
      logger.logUserException(Level.INFO, e, "Transition invariants cannot be checked");
      return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
    }
  }

  private AlgorithmStatus checkTransitionInvariants(
      ReachedSet pReachedSet,
      ImmutableCollection<Loop> loops,
      Set<ExpressionTreeLocationInvariant> invariants,
      ImmutableMap<Loop, BooleanFormula> loopsToTransitionInvariants,
      ImmutableListMultimap<Loop, BooleanFormula> loopsToSupportingInvariants)
      throws CPAException, InterruptedException {
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
      // Check the proper well-foundedness of the formula and if it succeeds, check R => T
      if (wellFoundednessChecker.isWellFounded(
              invariant, supportingInvariants, loop, mapPrevVarsToCurrVars)
          && isCandidateInvariantTransitionInvariant(
              loop,
              loopsToTransitionInvariants.get(loop),
              supportingInvariants,
              mapPrevVarsToCurrVars,
              // k = 1, for R^1 => T check
              1)) {
        continue;
      }

      // The formula is not well-founded, therefore we have to check for disjunctive
      // well-foundedness
      // And hence, we have to do check R^+ => T
      boolean isWellFounded =
          wellFoundednessChecker.isDisjunctivelyWellFounded(
              invariant, supportingInvariants, loop, mapPrevVarsToCurrVars);
      // Our termination analysis might be unsound because of a different possible division to
      // disjunctions. We divide the candidate transition invariant into a disjunction using
      // transformation to flat DNF. It might be, there exists another different division
      // that could be well-founded, and we just did not find it. For example, assume a DNF form
      // of the formula (x <= x') || (y < y'),
      // it can also be divided into (x = x') || (x < x') || (y < y'), which we do not check.
      if (!isWellFounded) {
        return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
      }
      // Do k-inductivity checks for k > 1
      for (int k = 1; true; k++) {
        if (k > maxInductionDepth) {
          logger.logf(
              Level.INFO,
              "The transition invariant of the loop could not be proven with k-induction up to k ="
                  + " %d.",
              maxInductionDepth);
          return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
        }
        // Base case of the induction, i.e. R^k(s,s') => T(s,s')
        if (!isCandidateInvariantTransitionInvariant(
            loop,
            loopsToTransitionInvariants.get(loop),
            supportingInvariants,
            mapPrevVarsToCurrVars,
            k)) {
          return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
        }

        // Step case of the induction, i.e.
        // T(s,t_0) && R(t_0,t_1) && ... && R(t_{k-1},t_k) && T(s,t_1) && ... && T(s,t_{k-1})
        //   => T(s,t_k)
        if (isCandidateInvariantInductiveTransitionInvariant(
            loop,
            loopsToTransitionInvariants.get(loop),
            supportingInvariants,
            mapPrevVarsToCurrVars,
            k)) {
          break;
        }
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

  /** Collects the declarations of the __PREV variables of all transition invariants. */
  private static ImmutableSet<CSimpleDeclaration> collectPrevVariableDeclarations(
      Set<ExpressionTreeLocationInvariant> invariants) {
    return FluentIterable.from(invariants)
        .filter(ExpressionTreeLocationTransitionInvariant.class)
        .transformAndConcat(inv -> inv.getMapPrevVarsToCurrent().keySet())
        .toSet();
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
    for (CFANode loopNode : pLoop.getLoopHeads()) {
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
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars,
      int k)
      throws InterruptedException, CPATransferException {

    // We first construct the loop formula, i.e. R^k, where k is at least 1
    Preconditions.checkArgument(k >= 1);
    PathFormula loopFormula = constructStrengthenedLoopFormulaForK(pLoop, pSupportingInvariants, k);

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
            MapsDifference.ignoreMapsDifference());
    SSAMap oneStepSSAMap =
        TransitionInvariantUtils.setIndicesToDifferentValues(
            pCandidateInvariant,
            PrevStateIndices.INDEX_FIRST,
            CurrStateIndices.INDEX_MIDDLE,
            fmgr,
            scope,
            pMapPrevToCurrVars);

    pCandidateInvariant = fmgr.instantiate(pCandidateInvariant, fullSSAMap);

    // Instantiate __PREV variables to match the SSA indices of the variables in the loop.
    // In other words, add equivalences like x@1 = x__PREV@1
    BooleanFormula booleanLoopFormula =
        bfmgr.and(
            loopFormula.getFormula(),
            fmgr.instantiate(
                fmgr.uninstantiate(
                    TransitionInvariantUtils.makeStatesEquivalent(
                        pCandidateInvariant,
                        loopFormula.getFormula(),
                        bfmgr,
                        fmgr,
                        pMapPrevToCurrVars)),
                oneStepSSAMap));

    boolean isTransitionInvariant;
    try {
      isTransitionInvariant = solver.implies(booleanLoopFormula, pCandidateInvariant);
    } catch (SolverException e) {
      logger.logUserException(Level.WARNING, e, "Transition invariant check failed!");
      return false;
    }
    return isTransitionInvariant;
  }

  /**
   * This function assumes that the loop formula R applied j times implies the candidate transition
   * invariant T for all j <= k, i.e. R^j => T. Then it checks the step case of the k-induction:
   *
   * <p>T(s,t_0) && R(t_0,t_1) && ... && R(t_{k-1},t_k) && T(s,t_1) && ... && T(s,t_{k-1}) =>
   * T(s,t_k)
   *
   * <p>Together, this implies R^+ => T, i.e. T is a transition invariant. This is needed, because T
   * is not well-founded but only disjunctively well-founded.
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
      ImmutableList<BooleanFormula> pSupportingInvariants,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars,
      int k)
      throws InterruptedException, CPATransferException {

    // We first construct the loop formula, i.e. R^k, where k is at least 1. The states t_0, ...,
    // t_k are given by their SSA maps.
    Preconditions.checkArgument(k >= 1);
    List<SSAMap> states = new ArrayList<>();
    PathFormula loopFormula =
        constructStrengthenedLoopFormulaForK(pLoop, pSupportingInvariants, k, states);

    // The state s is the first state of a pair in R^+, so it is a reachable loop head that has a
    // successor, i.e., the supporting invariants hold in s and there is u with R(s,u)
    BooleanFormula startStateConstraint =
        buildStartStateConstraint(pLoop, pSupportingInvariants, pMapPrevToCurrVars);
    // T(s,t_0) && T(s,t_1) && ... && T(s,t_{k-1})
    BooleanFormula assumedTransitionInvariants = bfmgr.makeTrue();
    for (SSAMap state : states.subList(0, k)) {
      assumedTransitionInvariants =
          bfmgr.and(
              assumedTransitionInvariants,
              instantiateTransitionInvariant(pCandidateInvariant, state, pMapPrevToCurrVars));
    }
    // T(s,t_k)
    BooleanFormula transitionInvariantAtLastState =
        instantiateTransitionInvariant(pCandidateInvariant, states.get(k), pMapPrevToCurrVars);

    boolean isTransitionInvariant;
    try {
      isTransitionInvariant =
          solver.implies(
              bfmgr.and(
                  startStateConstraint, assumedTransitionInvariants, loopFormula.getFormula()),
              transitionInvariantAtLastState);
    } catch (SolverException e) {
      logger.logUserException(Level.WARNING, e, "Transition invariant check failed!");
      return false;
    }
    return isTransitionInvariant;
  }

  private static final String START_STATE_SUFFIX = "__start_successor";

  /**
   * Builds the formula I(s) && R(s,u), where s is the start state of a transition invariant T(s,t),
   * which is given by the previous variables with the index {@link PrevStateIndices#INDEX_FIRST}.
   * The variables that do not have a previous variable, and the variables of the successor u, are
   * renamed to fresh variables.
   */
  private BooleanFormula buildStartStateConstraint(
      Loop pLoop,
      ImmutableList<BooleanFormula> pSupportingInvariants,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars)
      throws CPATransferException, InterruptedException {
    Map<String, String> currentToPrevious = new HashMap<>();
    for (Map.Entry<CSimpleDeclaration, CSimpleDeclaration> entry : pMapPrevToCurrVars.entrySet()) {
      currentToPrevious.put(entry.getValue().getQualifiedName(), entry.getKey().getQualifiedName());
    }

    // R(s,u): the loop formula starts with the initial SSA index
    PathFormula step =
        constructPathFormulaForLoop(
            pLoop, SSAMap.emptySSAMap(), PointerTargetSet.emptyPointerTargetSet());
    ImmutableMap.Builder<Formula, Formula> substitution = ImmutableMap.builder();
    for (Map.Entry<String, Formula> variable :
        fmgr.extractVariables(step.getFormula()).entrySet()) {
      Pair<String, OptionalInt> nameAndIndex = FormulaManagerView.parseName(variable.getKey());
      String name = nameAndIndex.getFirst();
      OptionalInt index = nameAndIndex.getSecond();
      FormulaType<Formula> type = fmgr.getFormulaType(variable.getValue());
      Formula renamed;
      if (index.isPresent()
          && index.orElseThrow() == INITIAL_SSA_INDEX
          && currentToPrevious.containsKey(name)) {
        renamed =
            fmgr.makeVariable(
                type, currentToPrevious.get(name), PrevStateIndices.INDEX_FIRST.getIndex());
      } else if (index.isPresent()) {
        renamed = fmgr.makeVariable(type, name + START_STATE_SUFFIX, index.orElseThrow());
      } else {
        renamed = fmgr.makeVariable(type, name + START_STATE_SUFFIX);
      }
      substitution.put(variable.getValue(), renamed);
    }
    BooleanFormula constraint = fmgr.substitute(step.getFormula(), substitution.buildOrThrow());

    // I(s)
    for (BooleanFormula supportingInvariant : pSupportingInvariants) {
      ImmutableMap.Builder<Formula, Formula> invariantSubstitution = ImmutableMap.builder();
      for (Map.Entry<String, Formula> variable :
          fmgr.extractVariables(supportingInvariant).entrySet()) {
        String name = variable.getKey();
        FormulaType<Formula> type = fmgr.getFormulaType(variable.getValue());
        invariantSubstitution.put(
            variable.getValue(),
            currentToPrevious.containsKey(name)
                ? fmgr.makeVariable(
                    type, currentToPrevious.get(name), PrevStateIndices.INDEX_FIRST.getIndex())
                : fmgr.makeVariable(type, name + START_STATE_SUFFIX, INITIAL_SSA_INDEX));
      }
      constraint =
          bfmgr.and(
              constraint,
              fmgr.substitute(supportingInvariant, invariantSubstitution.buildOrThrow()));
    }
    return constraint;
  }

  /**
   * Instantiates the (uninstantiated) transition invariant T(s,t) such that s is the state given by
   * the previous variables with the index {@link PrevStateIndices#INDEX_FIRST} and t is the state
   * given by the SSA map. Variables that are not in the SSA map have the {@link
   * #INITIAL_SSA_INDEX}.
   */
  private BooleanFormula instantiateTransitionInvariant(
      BooleanFormula pTransitionInvariant,
      SSAMap pState,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars) {
    ImmutableMap.Builder<Formula, Formula> substitution = ImmutableMap.builder();
    for (Map.Entry<String, Formula> variable :
        fmgr.extractVariables(pTransitionInvariant).entrySet()) {
      String name = variable.getKey();
      int index =
          TransitionInvariantUtils.isPrevVariable(name, pMapPrevToCurrVars)
              ? PrevStateIndices.INDEX_FIRST.getIndex()
              : getIndexOrInitial(pState, name);
      substitution.put(
          variable.getValue(),
          fmgr.makeVariable(fmgr.getFormulaType(variable.getValue()), name, index));
    }
    return fmgr.substitute(pTransitionInvariant, substitution.buildOrThrow());
  }

  private PathFormula constructStrengthenedLoopFormulaForK(
      Loop pLoop, ImmutableList<BooleanFormula> pSupportingInvariants, int k)
      throws CPATransferException, InterruptedException {
    return constructStrengthenedLoopFormulaForK(pLoop, pSupportingInvariants, k, new ArrayList<>());
  }

  /**
   * Constructs the formula R^k strengthened with the supporting invariants at the start of each
   * iteration.
   *
   * @param pStates the list to which the SSA maps of the states t_0, ..., t_k between the
   *     iterations are added
   */
  private PathFormula constructStrengthenedLoopFormulaForK(
      Loop pLoop, ImmutableList<BooleanFormula> pSupportingInvariants, int k, List<SSAMap> pStates)
      throws CPATransferException, InterruptedException {
    pStates.add(SSAMap.emptySSAMap());
    PathFormula loopFormula =
        constructPathFormulaForLoop(
            pLoop, SSAMap.emptySSAMap(), PointerTargetSet.emptyPointerTargetSet());
    pStates.add(loopFormula.getSsa());

    // The supporting invariants hold at the start of each iteration. The first iteration starts
    // in the state where all variables have the initial SSA index.
    BooleanFormula strengtheningFormula =
        instantiateAtStateOf(pSupportingInvariants, SSAMap.emptySSAMap());
    for (int i = 1; i < k; i++) {
      // The next iteration starts in the state where the previous one ended
      strengtheningFormula =
          bfmgr.and(
              strengtheningFormula,
              instantiateAtStateOf(pSupportingInvariants, loopFormula.getSsa()));
      loopFormula =
          pfmgr.makeConjunction(
              ImmutableList.of(
                  loopFormula,
                  constructPathFormulaForLoop(
                      pLoop, loopFormula.getSsa(), loopFormula.getPointerTargetSet())));
      pStates.add(loopFormula.getSsa());
    }
    // The strengthening formula is already instantiated, so it is conjoined directly
    return loopFormula.withFormula(bfmgr.and(loopFormula.getFormula(), strengtheningFormula));
  }

  /**
   * Instantiates the given (uninstantiated) formulas such that they describe the state given by the
   * SSA map. Variables that are not in the SSA map have the {@link #INITIAL_SSA_INDEX}.
   */
  private BooleanFormula instantiateAtStateOf(
      ImmutableList<BooleanFormula> pFormulas, SSAMap pSsa) {
    BooleanFormula result = bfmgr.makeTrue();
    for (BooleanFormula formula : pFormulas) {
      ImmutableMap.Builder<Formula, Formula> substitution = ImmutableMap.builder();
      for (Map.Entry<String, Formula> variable : fmgr.extractVariables(formula).entrySet()) {
        substitution.put(
            variable.getValue(),
            fmgr.makeVariable(
                fmgr.getFormulaType(variable.getValue()),
                variable.getKey(),
                getIndexOrInitial(pSsa, variable.getKey())));
      }
      result = bfmgr.and(result, fmgr.substitute(formula, substitution.buildOrThrow()));
    }
    return result;
  }

  /**
   * Constructs the formula R(s,s') of one iteration of the given loop as the disjunction of the
   * formulas of all paths from a loop head back to a loop head.
   *
   * <p>The loops nested in the given loop are summarized, see {@link #summarizeNestedLoop}.
   */
  private PathFormula constructPathFormulaForLoop(
      Loop pLoop, SSAMap pContextSSAMap, PointerTargetSet pContextPointerSet)
      throws CPATransferException, InterruptedException {
    List<List<CFAEdge>> listOfAllPaths =
        collectAllThePaths(
            getInnerLoopEdgesWithSummaryEdges(pLoop), pLoop.getLoopHeads(), pLoop.getLoopHeads());
    ImmutableSet<Loop> nestedLoops =
        FluentIterable.from(cfa.getLoopStructure().orElseThrow().getAllLoops())
            .filter(
                loop ->
                    !loop.equals(pLoop) && pLoop.getLoopNodes().containsAll(loop.getLoopHeads()))
            .toSet();
    return constructFormulaForPaths(
        pContextSSAMap, pContextPointerSet, nestedLoops, listOfAllPaths);
  }

  private PathFormula constructFormulaForPaths(
      SSAMap pContextSSAMap,
      PointerTargetSet pContextPointerTargetSet,
      ImmutableSet<Loop> pNestedLoops,
      List<List<CFAEdge>> listOfAllPaths)
      throws CPATransferException, InterruptedException {
    PathFormula formulaForLoop = pfmgr.makeEmptyPathFormula();
    formulaForLoop = formulaForLoop.withContext(pContextSSAMap, pContextPointerTargetSet);

    boolean initialized = false;
    for (List<CFAEdge> path : listOfAllPaths) {
      PathFormula anotherPath = pfmgr.makeEmptyPathFormula();
      anotherPath = anotherPath.withContext(pContextSSAMap, pContextPointerTargetSet);
      // The nested loop whose edges are currently replaced by its summary
      Optional<Loop> summarizedLoop = Optional.empty();
      for (CFAEdge edge : path) {
        if (summarizedLoop.isPresent()
            && summarizedLoop.orElseThrow().getInnerLoopEdges().contains(edge)) {
          continue;
        }
        summarizedLoop = getOutermostLoopWithEdge(pNestedLoops, edge);
        if (summarizedLoop.isPresent()) {
          anotherPath = summarizeNestedLoop(anotherPath, summarizedLoop.orElseThrow());
        } else {
          anotherPath = makeStep(anotherPath, edge);
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

  /** Returns the outermost of the given loops that contains the given edge, if there is one. */
  private static Optional<Loop> getOutermostLoopWithEdge(ImmutableSet<Loop> pLoops, CFAEdge pEdge) {
    return FluentIterable.from(pLoops)
        .filter(loop -> loop.getInnerLoopEdges().contains(pEdge))
        .stream()
        .max(Comparator.comparingInt(loop -> loop.getLoopNodes().size()));
  }

  /**
   * Extends the given path formula, which ends at a head of the given nested loop, by a summary of
   * arbitrarily many iterations of the nested loop. The variables that are assigned in the nested
   * loop get new SSA indices, and their values after the loop are constrained by
   *
   * <p>s_exit = s_entry ∨ T(s_entry, s_exit) ∨ ¬I(s_entry),
   *
   * <p>where T is the transition invariant and I are the supporting invariants of the nested loop
   * from the witness. The transition invariant is used before it is validated, which is sound
   * because the validation only succeeds if the transition invariants of all loops are validated,
   * and the transition invariant of the nested loop is validated with respect to its supporting
   * invariants only.
   */
  private PathFormula summarizeNestedLoop(PathFormula pPath, Loop pNestedLoop)
      throws CPATransferException, InterruptedException {
    // Collect the variables assigned in the nested loop: an edge assigns a variable iff the SSA
    // index of the variable increases. The formula for all edges makes all variables known first.
    ImmutableSet<CFAEdge> steps = getInnerLoopEdgesWithSummaryEdges(pNestedLoop);
    PathFormula allEdges = pPath;
    for (CFAEdge edge : steps) {
      allEdges = makeStep(allEdges, edge);
    }
    Map<String, Formula> instantiatedVariables = fmgr.extractVariables(allEdges.getFormula());
    Map<String, Formula> uninstantiatedVariables = new HashMap<>();
    for (Map.Entry<String, Formula> variable : instantiatedVariables.entrySet()) {
      uninstantiatedVariables.put(
          FormulaManagerView.parseName(variable.getKey()).getFirst(),
          fmgr.uninstantiate(variable.getValue()));
    }
    Set<String> assignedVariables = new HashSet<>();
    for (CFAEdge edge : steps) {
      PathFormula oneEdge = makeStep(pfmgr.makeEmptyPathFormulaWithContextFrom(allEdges), edge);
      for (String variable : oneEdge.getSsa().allVariables()) {
        if (oneEdge.getSsa().getIndex(variable) > allEdges.getSsa().getIndex(variable)) {
          assignedVariables.add(variable);
        }
      }
    }

    BooleanFormula transitionInvariant = transitionInvariantsOfLoops.get(pNestedLoop);
    ImmutableList<BooleanFormula> supportingInvariants =
        supportingInvariantsOfLoops.get(pNestedLoop);
    ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> mapPrevToCurrVars =
        joinPrevDeclarationMapsForLoop(pNestedLoop, witnessInvariants);

    // The state at the entry of the nested loop. Variables that are not used before get an index.
    SSAMap.SSAMapBuilder entryBuilder = pPath.getSsa().builder();
    for (String variable : assignedVariables) {
      if (entryBuilder.getIndex(variable) < 0) {
        entryBuilder.setIndex(variable, allEdges.getSsa().getType(variable), INITIAL_SSA_INDEX);
      }
    }
    SSAMap entrySsa = entryBuilder.build();

    // The state at the exit of the nested loop, where the assigned variables have new values
    SSAMap.SSAMapBuilder exitBuilder = entrySsa.builder();
    for (String variable : assignedVariables) {
      exitBuilder.setIndex(
          variable, allEdges.getSsa().getType(variable), exitBuilder.getFreshIndex(variable));
    }
    SSAMap exitSsa = exitBuilder.build();

    // s_exit = s_entry for the assigned variables, i.e., the loop is not entered
    BooleanFormula identity = bfmgr.makeTrue();
    ImmutableMap.Builder<String, Formula> exits = ImmutableMap.builder();
    for (String variable : assignedVariables) {
      Formula uninstantiated = uninstantiatedVariables.get(variable);
      if (uninstantiated == null) {
        // Not a variable, e.g., a function encoding the heap, so it is only havocked
        continue;
      }
      FormulaType<Formula> type = fmgr.getFormulaType(uninstantiated);
      Formula exit = fmgr.makeVariable(type, variable, exitSsa.getIndex(variable));
      exits.put(variable, exit);
      identity =
          bfmgr.and(
              identity,
              fmgr.makeEqual(exit, fmgr.makeVariable(type, variable, entrySsa.getIndex(variable))));
    }

    if (transitionInvariant == null) {
      // Without a transition invariant, the assigned variables are havocked
      return pPath.withContext(exitSsa, allEdges.getPointerTargetSet());
    }

    // T(s_entry, s_exit)
    BooleanFormula summary =
        bfmgr.or(
            identity,
            nestedLoopSummarizer.instantiateTransitionInvariant(
                toTerminationToSafetyNaming(transitionInvariant, mapPrevToCurrVars),
                exits.buildOrThrow(),
                entrySsa));

    // ¬I(s_entry), the transition invariant is only valid for states satisfying I
    if (!supportingInvariants.isEmpty()) {
      summary =
          bfmgr.or(summary, bfmgr.not(fmgr.instantiate(bfmgr.and(supportingInvariants), entrySsa)));
    }

    // The summary is already instantiated, so it is conjoined directly
    return pPath
        .withContext(exitSsa, allEdges.getPointerTargetSet())
        .withFormula(bfmgr.and(pPath.getFormula(), summary));
  }

  /**
   * Renames the variables of the transition invariant T(s,t) of the witness as in the transition
   * invariants of TerminationToSafety, i.e., the variables of s and of t contain {@link
   * TransitionInvariantUtils#PREV_KEYWORD} and {@link TransitionInvariantUtils#CURR_KEYWORD}.
   */
  private BooleanFormula toTerminationToSafetyNaming(
      BooleanFormula pTransitionInvariant,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars) {
    Set<String> variables = fmgr.extractVariableNames(pTransitionInvariant);
    return fmgr.renameFreeVariablesAndUFs(
        pTransitionInvariant,
        name -> {
          if (!variables.contains(name)) {
            return name;
          }
          if (TransitionInvariantUtils.isPrevVariable(name, pMapPrevToCurrVars)) {
            return pMapPrevToCurrVars
                    .get(TransitionInvariantUtils.getPrevDeclaration(name, pMapPrevToCurrVars))
                    .getQualifiedName()
                + TransitionInvariantUtils.PREV_KEYWORD;
          }
          return name + TransitionInvariantUtils.CURR_KEYWORD;
        });
  }

  /** Variables that do not occur in a path formula yet have the {@link #INITIAL_SSA_INDEX}. */
  private static int getIndexOrInitial(SSAMap pSsa, String pVariable) {
    return pSsa.containsVariable(pVariable) ? pSsa.getIndex(pVariable) : INITIAL_SSA_INDEX;
  }

  /**
   * Returns the inner edges of the loop, where the calls of functions in the loop are given by
   * their summary edges.
   */
  private static ImmutableSet<CFAEdge> getInnerLoopEdgesWithSummaryEdges(Loop pLoop) {
    return FluentIterable.from(pLoop.getLoopNodes())
        .transformAndConcat(CFANode::getAllLeavingEdges)
        .filter(edge -> pLoop.getLoopNodes().contains(edge.getSuccessor()))
        .toSet();
  }

  /**
   * Collects all paths from a start node to an end node that consist of the given edges. A call of
   * a function is represented by its summary edge, which has to be among the given edges. Paths
   * that end in a node without successors, e.g., after abort(), are omitted, since they do not
   * reach an end node.
   *
   * @throws UnsupportedLoopException if a path ends in another node
   */
  private List<List<CFAEdge>> collectAllThePaths(
      ImmutableSet<CFAEdge> pEdges, ImmutableSet<CFANode> pStartNodes, Set<CFANode> pEndNodes)
      throws UnsupportedLoopException {
    List<List<CFAEdge>> listOfAllPaths = new ArrayList<>();
    for (CFAEdge edge : pEdges) {
      if (pStartNodes.contains(edge.getPredecessor())) {
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
        // An edge is taken at most once on a path. Otherwise, a path through a nested loop would
        // be extended forever, since only the end nodes end a path. The edges of the nested loops
        // are overapproximated when the formula for the paths is constructed.
        List<CFAEdge> succEdges =
            pEdges.stream()
                .filter(
                    e ->
                        e.getPredecessor().equals(lastEdge.getSuccessor())
                            && !pEndNodes.contains(e.getPredecessor())
                            && !path.contains(e))
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

    List<List<CFAEdge>> completePaths = new ArrayList<>();
    for (List<CFAEdge> path : listOfAllPaths) {
      CFANode last = path.getLast().getSuccessor();
      if (pEndNodes.contains(last)) {
        completePaths.add(path);
      } else if (!last.getAllLeavingEdges().isEmpty()) {
        throw new UnsupportedLoopException("A path ends unexpectedly in " + last);
      }
    }
    return completePaths;
  }

  /**
   * Extends the path formula by a call of a function: the function call edge, all paths through the
   * called function, and the matching function return edge.
   *
   * @throws UnsupportedLoopException if the called function contains a loop or is recursive
   */
  private PathFormula makeFunctionCall(PathFormula pPath, FunctionSummaryEdge pSummaryEdge)
      throws CPATransferException, InterruptedException {
    FunctionEntryNode entry = pSummaryEdge.getFunctionEntry();
    String function = entry.getFunctionName();
    if (!cfa.getLoopStructure().orElseThrow().getLoopsForFunction(function).isEmpty()
        || !functionsOnCallStack.add(function)) {
      throw new UnsupportedLoopException(
          "The function " + function + " contains a loop or is recursive");
    }
    try {
      PathFormula result = pPath;
      for (CFAEdge edge : pSummaryEdge.getPredecessor().getLeavingEdges()) {
        if (edge instanceof FunctionCallEdge) {
          result = pfmgr.makeAnd(result, edge);
        }
      }
      if (entry.getExitNode().isEmpty()) {
        // The function never returns
        return result.withFormula(bfmgr.makeFalse());
      }
      FunctionExitNode exit = entry.getExitNode().orElseThrow();

      // The edges of the called function, calls in it are represented by their summary edges
      EdgeCollectingCFAVisitor edgeCollector = new EdgeCollectingCFAVisitor();
      CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(entry, edgeCollector);
      List<List<CFAEdge>> paths =
          collectAllThePaths(
              ImmutableSet.copyOf(edgeCollector.getVisitedEdges()),
              ImmutableSet.of(entry),
              ImmutableSet.of(exit));
      if (paths.isEmpty()) {
        return result.withFormula(bfmgr.makeFalse());
      }
      PathFormula body =
          constructFormulaForPaths(
              result.getSsa(), result.getPointerTargetSet(), ImmutableSet.of(), paths);
      result = pfmgr.makeConjunction(ImmutableList.of(result, body));

      for (CFAEdge edge : exit.getLeavingReturnEdges()) {
        if (edge instanceof FunctionReturnEdge returnEdge
            && returnEdge.getSummaryEdge().equals(pSummaryEdge)) {
          result = pfmgr.makeAnd(result, edge);
        }
      }
      return result;
    } finally {
      functionsOnCallStack.remove(function);
    }
  }

  /** Extends the path formula by the edge, function calls are given by their summary edges. */
  private PathFormula makeStep(PathFormula pPath, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    if (pEdge instanceof FunctionSummaryEdge summaryEdge) {
      return makeFunctionCall(pPath, summaryEdge);
    }
    return pfmgr.makeAnd(pPath, pEdge);
  }

  /** Thrown if the formula for a loop cannot be constructed soundly. */
  private static final class UnsupportedLoopException extends CPATransferException {
    private static final long serialVersionUID = 1L;

    UnsupportedLoopException(String pMessage) {
      super(pMessage);
    }
  }
}
