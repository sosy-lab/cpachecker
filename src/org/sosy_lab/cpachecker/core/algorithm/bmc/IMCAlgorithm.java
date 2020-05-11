/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.filterAncestors;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantGenerator;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.InvariantProvider;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessExporter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessToOutputFormatsUtils;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundCPA;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.BiPredicates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.AssignmentToPathAllocator;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.invariants.ExpressionTreeInvariantSupplier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix="bmc")
public class IMCAlgorithm extends AbstractBMCAlgorithm implements Algorithm {

  @Option(secure = true, description = "try using interpolation to verify programs with loops")
  private boolean interpolation = false;

  @Option(secure = true, description = "toggle deriving the interpolants from suffix formulas")
  private boolean deriveInterpolantFromSuffix = false;

  @Option(
    secure = true,
    description = "Check reachability of target states after analysis "
        + "(classical BMC). The alternative is to check the reachability "
        + "as soon as the target states are discovered, which is done if "
        + "cpa.predicate.targetStateSatCheck=true.")
  private boolean checkTargetStates = true;

  @Option(secure=true, description="Export auxiliary invariants used for induction.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  @Nullable
  private Path invariantsExport = null;

  private final ConfigurableProgramAnalysis cpa;

  private final FormulaManagerView fmgr;
  private final PathFormulaManager pmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;

  private final Configuration config;
  private final CFA cfa;
  private final AssignmentToPathAllocator assignmentToPathAllocator;

  private final WitnessExporter argWitnessExporter;

  public IMCAlgorithm(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCPA,
      Configuration pConfig,
      LogManager pLogger,
      ReachedSetFactory pReachedSetFactory,
      ShutdownManager pShutdownManager,
      CFA pCFA,
      final Specification specification,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(
        pAlgorithm,
        pCPA,
        pConfig,
        pLogger,
        pReachedSetFactory,
        pShutdownManager,
        pCFA,
        specification,
        new BMCStatistics(),
        false /* no invariant generator */,
        pAggregatedReachedSets);
    pConfig.inject(this);

    cpa = pCPA;
    config = pConfig;
    cfa = pCFA;

    @SuppressWarnings("resource")
    PredicateCPA predCpa = CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, IMCAlgorithm.class);
    solver = predCpa.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pmgr = predCpa.getPathFormulaManager();
    MachineModel machineModel = pCFA.getMachineModel();

    assignmentToPathAllocator = new AssignmentToPathAllocator(config, shutdownNotifier, pLogger, machineModel);
    argWitnessExporter = new WitnessExporter(config, logger, specification, cfa);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    try {
      return interpolationModelChecking(pReachedSet);
    } catch (SolverException e) {
      throw new CPAException("Solver Failure " + e.getMessage(), e);
    } finally {
      invariantGenerator.cancel();
    }
  }

  /**
   * The run method for interpolation-based model checking
   *
   * @param pReachedSet Abstract Reachability Graph (ARG)
   *
   * @return {@code AlgorithmStatus.UNSOUND_AND_PRECISE} if an error location is reached, i.e.,
   *         unsafe; {@code AlgorithmStatus.SOUND_AND_PRECISE} if a fixed point is reached, i.e.,
   *         safe.
   */
  private AlgorithmStatus interpolationModelChecking(final ReachedSet pReachedSet)
      throws CPAException, SolverException, InterruptedException {
    Preconditions.checkState(
        cfa.getAllLoopHeads().isPresent() && cfa.getAllLoopHeads().orElseThrow().size() <= 1,
        "programs with multiple loops are not supported yet");

    logger.log(Level.FINE, "Performing interpolation-based model checking");
    try (ProverEnvironmentWithFallback prover =
        new ProverEnvironmentWithFallback(solver, ProverOptions.GENERATE_MODELS)) {
      PathFormula prefixFormula = pmgr.makeEmptyPathFormula();
      BooleanFormula loopFormula = bfmgr.makeTrue();
      BooleanFormula tailFormula = bfmgr.makeTrue();
      do {
        int maxLoopIterations = CPAs.retrieveCPA(cpa, LoopBoundCPA.class).getMaxLoopIterations();

        shutdownNotifier.shutdownIfNecessary();
        logger.log(Level.FINE, "Unrolling with LBE, maxLoopIterations = " + maxLoopIterations);
        stats.bmcPreparation.start();
        BMCHelper.unroll(logger, pReachedSet, algorithm, cpa);
        stats.bmcPreparation.stop();
        shutdownNotifier.shutdownIfNecessary();

        if (noLoopToUnroll(cfa)) {
          logger.log(Level.FINE, "The program has no loop to unroll");
          if (formulaCheckSat(
              prover,
              bfmgr.or(getErrorFormula(pReachedSet, -1), getErrorFormula(pReachedSet, 0)))) {
            logger.log(Level.INFO, "An error is reached by BMC");
            return AlgorithmStatus.UNSOUND_AND_PRECISE;
          } else {
            logger.log(Level.INFO, "No error can be reached");
            if (pReachedSet.hasViolatedProperties()) {
              TargetLocationCandidateInvariant.INSTANCE.assumeTruth(pReachedSet);
            }
            return AlgorithmStatus.SOUND_AND_PRECISE;
          }
        }

        logger.log(Level.FINE, "Collecting prefix, loop, and suffix formulas");
        if (maxLoopIterations == 1) {
          if (formulaCheckSat(prover, getErrorFormula(pReachedSet, -1))) {
            logger.log(Level.INFO, "There exist reachable errors before the loop");
            return AlgorithmStatus.UNSOUND_AND_PRECISE;
          }
          prefixFormula = getLoopHeadFormula(pReachedSet, maxLoopIterations - 1);
        } else if (maxLoopIterations == 2) {
          loopFormula = getLoopHeadFormula(pReachedSet, maxLoopIterations - 1).getFormula();
        } else {
          tailFormula =
              bfmgr.and(
                  tailFormula,
                  getLoopHeadFormula(pReachedSet, maxLoopIterations - 1).getFormula());
        }
        BooleanFormula suffixFormula =
            bfmgr.and(tailFormula, getErrorFormula(pReachedSet, maxLoopIterations - 1));
        logger.log(Level.ALL, "The prefix is " + prefixFormula.getFormula().toString());
        logger.log(Level.ALL, "The loop is " + loopFormula.toString());
        logger.log(Level.ALL, "The suffix is " + suffixFormula.toString());

        BooleanFormula reachErrorFormula =
            bfmgr.and(prefixFormula.getFormula(), loopFormula, suffixFormula);
        if (formulaCheckSat(prover, reachErrorFormula)) {
          logger.log(Level.INFO, "An error is reached by BMC");
          return AlgorithmStatus.UNSOUND_AND_PRECISE;
        } else {
          logger.log(
              Level.FINE,
              "No error is found up to maxLoopIterations = " + maxLoopIterations);
          if (pReachedSet.hasViolatedProperties()) {
            TargetLocationCandidateInvariant.INSTANCE.assumeTruth(pReachedSet);
          }
          BooleanFormula forwardConditionFormula =
              bfmgr.and(
                  prefixFormula.getFormula(),
                  loopFormula,
                  tailFormula,
                  getLoopHeadFormula(pReachedSet, maxLoopIterations).getFormula());
          if (!formulaCheckSat(prover, forwardConditionFormula)) {
            logger.log(Level.INFO, "The program is safe as it cannot be further unrolled");
            return AlgorithmStatus.SOUND_AND_PRECISE;
          }
        }

        if (interpolation && maxLoopIterations > 1) {
          logger.log(Level.FINE, "Computing fixed points by interpolation");
          if (reachFixedPointByInterpolation(prover, prefixFormula, loopFormula, suffixFormula)) {
            return AlgorithmStatus.SOUND_AND_PRECISE;
          }
        }
      } while (adjustConditions());
    }
    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  private boolean noLoopToUnroll(CFA pCfa) {
    if (pCfa.getAllLoopHeads().orElseThrow().isEmpty()) {
      return true;
    }
    CFANode pNode = pCfa.getAllLoopHeads().orElseThrow().iterator().next();
    if (pNode.hasEdgeTo(pNode)) {
      return true;
    }
    if (pNode.getNumLeavingEdges() > 0 && pNode.getLeavingEdge(0).getSuccessor().hasEdgeTo(pNode)) {
      return true;
    }
    return false;
  }

  private PathFormula getLoopHeadFormula(ReachedSet pReachedSet, int numEncounterLoopHead) {
    List<AbstractState> loopHead =
        from(pReachedSet)
            .filter(
                e -> AbstractStates.extractStateByType(e, LocationState.class)
                    .getLocationNode()
                    .isLoopStart())
            .filter(
                e -> AbstractStates.extractStateByType(e, LoopBoundState.class)
                    .getDeepestIteration()
                    - 1 == numEncounterLoopHead)
            .toList();
    checkState(loopHead.size() == 1, "The number of loop heads in ARG is " + "%s", loopHead.size());
    return PredicateAbstractState.getPredicateState(loopHead.get(0))
        .getAbstractionFormula()
        .getBlockFormula();
  }

  private BooleanFormula getErrorFormula(ReachedSet pReachedSet, int numEncounterLoopHead) {
    List<AbstractState> errorLocations =
        AbstractStates.getTargetStates(pReachedSet)
            .filter(
                e -> AbstractStates.extractStateByType(e, LoopBoundState.class)
                    .getDeepestIteration()
                    - 1 == numEncounterLoopHead)
            .toList();
    BooleanFormula formulaToErrorLocations = bfmgr.makeFalse();
    for (AbstractState pErrorState : errorLocations) {
      formulaToErrorLocations =
          bfmgr.or(
              formulaToErrorLocations,
              PredicateAbstractState.getPredicateState(pErrorState)
                  .getAbstractionFormula()
                  .getBlockFormula()
                  .getFormula());
    }
    return formulaToErrorLocations;
  }

  private boolean formulaCheckSat(ProverEnvironmentWithFallback pProver, BooleanFormula pFormula)
      throws InterruptedException, SolverException {
    while (!pProver.isEmpty()) {
      pProver.pop();
    }
    pProver.push(pFormula);
    return !pProver.isUnsat();
  }

  private boolean reachFixedPointCheck(
      ProverEnvironmentWithFallback pProver,
      BooleanFormula pInterpolantFormula,
      BooleanFormula pCurrentImageFormula)
      throws InterruptedException, SolverException {
    BooleanFormula pNotImplicationFormula =
        bfmgr.not(bfmgr.implication(pInterpolantFormula, pCurrentImageFormula));
    return !formulaCheckSat(pProver, pNotImplicationFormula);
  }

  private BooleanFormula getInterpolantFrom(
      ProverEnvironmentWithFallback proverStack,
      ArrayDeque<Object> formulaA,
      ArrayDeque<Object> formulaB)
      throws SolverException, InterruptedException {
    if (deriveInterpolantFromSuffix) {
      logger
          .log(Level.FINE, "deriving the interpolant from suffix (formula B) and negate it");
      return bfmgr.not(proverStack.getInterpolant(formulaB));
    } else {
      logger.log(Level.FINE, "deriving the interpolant from prefix and loop (formula A)");
      return proverStack.getInterpolant(formulaA);
    }
  }

  /**
   * Compute fixed points by interpolation
   *
   * @param pProver SMT solver to check whether a fixed point is reached
   * @param pPrefixPathFormula the prefix path formula with SSA map
   * @param pLoopFormula the loop formula
   * @param pSuffixFormula the suffix formula
   * @return {@code true} if a fixed point is reached, i.e., property is proved; {@code false} if
   *         the current over-approximation is unsafe
   */
  private boolean reachFixedPointByInterpolation(
      ProverEnvironmentWithFallback pProver,
      PathFormula pPrefixPathFormula,
      BooleanFormula pLoopFormula,
      BooleanFormula pSuffixFormula)
      throws InterruptedException, SolverException {
    try (ProverEnvironmentWithFallback proverStack =
        new ProverEnvironmentWithFallback(solver, ProverOptions.GENERATE_UNSAT_CORE)) {

      BooleanFormula pPrefixFormula = pPrefixPathFormula.getFormula();
      SSAMap prefixSsaMap = pPrefixPathFormula.getSsa();
      logger.log(Level.ALL, "The SSA map is " + prefixSsaMap.toString());
      BooleanFormula currentImage = bfmgr.makeFalse();
      currentImage = bfmgr.or(currentImage, pPrefixFormula);
      BooleanFormula interpolant = null;

      ArrayDeque<Object> formulaA = new ArrayDeque<>();
      ArrayDeque<Object> formulaB = new ArrayDeque<>();
      formulaB.addFirst(proverStack.push(pSuffixFormula));
      formulaA.addFirst(proverStack.push(pLoopFormula));
      formulaA.addFirst(proverStack.push(pPrefixFormula));

      while (proverStack.isUnsat()) {
        logger.log(Level.ALL, "The current image is " + currentImage.toString());
        interpolant = getInterpolantFrom(proverStack, formulaA, formulaB);
        logger.log(Level.ALL, "The interpolant is " + interpolant.toString());
        interpolant = fmgr.instantiate(fmgr.uninstantiate(interpolant), prefixSsaMap);
        logger.log(Level.ALL, "After changing SSA " + interpolant.toString());
        boolean reachFixedPoint = reachFixedPointCheck(pProver, interpolant, currentImage);
        if (reachFixedPoint) {
          logger.log(Level.INFO, "The current image reaches a fixed point, property proved");
          return true;
        }
        currentImage = bfmgr.or(currentImage, interpolant);
        proverStack.pop();
        formulaA.removeFirst();
        formulaA.addFirst(proverStack.push(interpolant));
      }
      logger.log(Level.FINE, "The overapproximation is unsafe, going back to BMC phase");
      return false;
    }
  }

  @Override
  protected CandidateGenerator getCandidateInvariants() {
    if (getTargetLocations().isEmpty() || !cfa.getAllLoopHeads().isPresent()) {
      return CandidateGenerator.EMPTY_GENERATOR;
    } else {
      return new StaticCandidateProvider(
          Collections.singleton(TargetLocationCandidateInvariant.INSTANCE));
    }
  }

  @Override
  protected boolean boundedModelCheck(
      final ReachedSet pReachedSet,
      final ProverEnvironmentWithFallback pProver,
      CandidateInvariant pInductionProblem)
      throws CPATransferException, InterruptedException, SolverException {
    if (!checkTargetStates) {
      return true;
    }

    return super.boundedModelCheck(pReachedSet, pProver, pInductionProblem);
  }

  /**
   * This method tries to find a feasible path to (one of) the target state(s). It does so by asking
   * the solver for a satisfying assignment.
   */
  @SuppressWarnings("resource")
  @Override
  protected void analyzeCounterexample(
      final BooleanFormula pCounterexampleFormula,
      final ReachedSet pReachedSet,
      final ProverEnvironmentWithFallback pProver)
      throws CPATransferException, InterruptedException {
    if (!(cpa instanceof ARGCPA)) {
      logger.log(Level.INFO, "Error found, but error path cannot be created without ARGCPA");
      return;
    }

    stats.errorPathCreation.start();
    try {
      logger.log(Level.INFO, "Error found, creating error path");

      Set<ARGState> targetStates =
          from(pReachedSet).filter(AbstractStates::isTargetState).filter(ARGState.class).toSet();
      Set<ARGState> redundantStates = filterAncestors(targetStates, AbstractStates::isTargetState);
      redundantStates.forEach(state -> {
        state.removeFromARG();
      });
      pReachedSet.removeAll(redundantStates);
      targetStates = Sets.difference(targetStates, redundantStates);

      final boolean shouldCheckBranching;
      if (targetStates.size() == 1) {
        ARGState state = Iterables.getOnlyElement(targetStates);
        while (state.getParents().size() == 1 && state.getChildren().size() <= 1) {
          state = Iterables.getOnlyElement(state.getParents());
        }
        shouldCheckBranching = (state.getParents().size() > 1)
            || (state.getChildren().size() > 1);
      } else {
        shouldCheckBranching = true;
      }

      if (shouldCheckBranching) {
        Set<ARGState> arg = from(pReachedSet).filter(ARGState.class).toSet();

        // get the branchingFormula
        // this formula contains predicates for all branches we took
        // this way we can figure out which branches make a feasible path
        BooleanFormula branchingFormula = pmgr.buildBranchingFormula(arg);

        if (bfmgr.isTrue(branchingFormula)) {
          logger.log(Level.WARNING, "Could not create error path because of missing branching information!");
          return;
        }

        // add formula to solver environment
        pProver.push(branchingFormula);
      }

      List<ValueAssignment> model;
      try {
        // need to ask solver for satisfiability again,
        // otherwise model doesn't contain new predicates
        boolean stillSatisfiable = !pProver.isUnsat();

        if (!stillSatisfiable) {
          // should not occur
          logger.log(Level.WARNING, "Could not create error path information because of inconsistent branching information!");
          return;
        }

        model = pProver.getModelAssignments();

      } catch (SolverException e) {
        logger.log(Level.WARNING, "Solver could not produce model, cannot create error path.");
        logger.logDebugException(e);
        return;

      } finally {
        if (shouldCheckBranching) {
          pProver.pop(); // remove branchingFormula
        }
      }


      // get precise error path
      Map<Integer, Boolean> branchingInformation = pmgr.getBranchingPredicateValuesFromModel(model);
      ARGState root = (ARGState)pReachedSet.getFirstState();

      ARGPath targetPath;
      try {
        Set<AbstractState> arg = pReachedSet.asCollection();
        targetPath = ARGUtils.getPathFromBranchingInformation(root, arg, branchingInformation);
      } catch (IllegalArgumentException e) {
        logger.logUserException(Level.WARNING, e, "Could not create error path");
        return;
      }

      BooleanFormula cexFormula = pCounterexampleFormula;

      // replay error path for a more precise satisfying assignment
      PathChecker pathChecker;
      try {
        Solver solverForPathChecker = this.solver;
        PathFormulaManager pmgrForPathChecker = this.pmgr;

        if (solverForPathChecker.getVersion().toLowerCase().contains("smtinterpol")) {
          // SMTInterpol does not support reusing the same solver
          solverForPathChecker = Solver.create(config, logger, shutdownNotifier);
          FormulaManagerView formulaManager = solverForPathChecker.getFormulaManager();
          pmgrForPathChecker =
              new PathFormulaManagerImpl(
                  formulaManager, config, logger, shutdownNotifier, cfa, AnalysisDirection.FORWARD);
          // cannot dump pCounterexampleFormula, PathChecker would use wrong FormulaManager for it
          cexFormula = solverForPathChecker.getFormulaManager().getBooleanFormulaManager().makeTrue();
        }

        pathChecker =
            new PathChecker(
                config,
                logger,
                pmgrForPathChecker,
                solverForPathChecker,
                assignmentToPathAllocator);

      } catch (InvalidConfigurationException e) {
        // Configuration has somehow changed and can no longer be used to create the solver and path formula manager
        logger.logUserException(Level.WARNING, e, "Could not replay error path to get a more precise model");
        return;
      }

      CounterexampleTraceInfo cexInfo =
          CounterexampleTraceInfo.feasible(
              ImmutableList.of(cexFormula), model, branchingInformation);
      CounterexampleInfo counterexample = pathChecker.createCounterexample(targetPath, cexInfo);
      counterexample.getTargetState().addCounterexampleInformation(counterexample);

    } finally {
      stats.errorPathCreation.stop();
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    super.collectStatistics(pStatsCollection);
    pStatsCollection.add(
        new Statistics() {

          @Override
          public void printStatistics(
              PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
            // apparently there is nothing to do here.
          }

          @Override
          public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
            if (pResult == Result.FALSE) {
              return;
            }
            ARGState rootState =
                AbstractStates.extractStateByType(pReached.getFirstState(), ARGState.class);
            if (rootState != null && invariantsExport != null) {
              ExpressionTreeSupplier tmpExpressionTreeSupplier =
                  ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;
              if (invariantGenerator.isStarted()) {
                try {
                  if (invariantGenerator instanceof KInductionInvariantGenerator) {
                    tmpExpressionTreeSupplier =
                        ((KInductionInvariantGenerator) invariantGenerator)
                            .getExpressionTreeSupplier();
                  } else {
                    tmpExpressionTreeSupplier =
                        new ExpressionTreeInvariantSupplier(invariantGenerator.get(), cfa);
                  }
                } catch (CPAException | InterruptedException e1) {
                  tmpExpressionTreeSupplier =
                      ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;
                }
              }
              final ExpressionTreeSupplier expSup = tmpExpressionTreeSupplier;
              final Witness generatedWitness =
                  argWitnessExporter.generateProofWitness(
                      rootState,
                      Predicates.alwaysTrue(),
                      BiPredicates.alwaysTrue(),
                      new InvariantProvider() {
                        @Override
                        public ExpressionTree<Object> provideInvariantFor(
                            CFAEdge pCFAEdge,
                            Optional<? extends Collection<? extends ARGState>> pStates) {
                          CFANode node = pCFAEdge.getSuccessor();
                          ExpressionTree<Object> result = expSup.getInvariantFor(node);
                          if (ExpressionTrees.getFalse().equals(result) && !pStates.isPresent()) {
                            return ExpressionTrees.getTrue();
                          }
                          return result;
                        }
                      });
              try (Writer w = IO.openOutputFile(invariantsExport, StandardCharsets.UTF_8)) {
                WitnessToOutputFormatsUtils.writeToGraphMl(generatedWitness, w);
              } catch (IOException e) {
                logger.logUserException(
                    Level.WARNING, e, "Could not write invariants to file " + invariantsExport);
              }
            }
          }

          @Override
          public String getName() {
            return null; // return null because we do not print statistics
          }
        });
  }
}
