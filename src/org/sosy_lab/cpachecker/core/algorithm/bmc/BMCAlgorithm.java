// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serial;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.EdgeFormula;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.FrontierEdgeFormulaNegation;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.LoopScopedFrontierEdgeFormulaNegation;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SingleLocationFormulaInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.StatewiseCandidateInvariantConjunction;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.StatewiseCandidateInvariantDisjunction;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.defaults.PropertyTargetInformation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationBounding;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.InvariantProvider;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessExporter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessToOutputFormatsUtils;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.BiPredicates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.error.DummyErrorState;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

@Options
public class BMCAlgorithm extends AbstractBMCAlgorithm implements Algorithm {

  @Option(
      name = "bmc.checkTargetStates",
      secure = true,
      description =
          "Check reachability of target states after analysis "
              + "(classical BMC). The alternative is to check the reachability "
              + "as soon as the target states are discovered, which is done if "
              + "cpa.predicate.targetStateSatCheck=true.")
  private boolean checkTargetStates = true;

  @Option(
      name = "bmc.terminationMode",
      secure = true,
      description =
          "Switch BMC from target-state reachability checking to an experimental"
              + " termination-oriented mode.")
  private boolean terminationMode = false;

  @Option(
      name = "bmc.nonTerminationMode",
      secure = true,
      description =
          "Switch BMC to a strong non-termination mode that uses a SAT base case and"
              + " safety-style k-induction over loop-continuation conditions.")
  private boolean nonTerminationMode = false;

  // Option copied from PathChecker, keep in sync (and hopefully remove at some point)
  @Option(
      name = "counterexample.export.allowImpreciseCounterexamples",
      secure = true,
      description =
          "An imprecise counterexample of the Predicate CPA is usually a bug,"
              + " but expected in some configurations. Should it be treated as a bug or accepted?")
  private boolean allowImpreciseCounterexamples = false;

  @Option(
      name = "bmc.invariantsExport",
      secure = true,
      description = "Export auxiliary invariants used for induction.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  @Nullable
  private Path invariantsExport = null;

  private final Configuration config;
  private final CFA cfa;
  private final ConfigurableProgramAnalysis analysisCpa;

  private final WitnessExporter argWitnessExporter;

  private boolean terminationCandidatesIncomplete = false;
  private int loopsWithoutTerminationCandidates = 0;

  public BMCAlgorithm(
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

    if (terminationMode && nonTerminationMode) {
      throw new InvalidConfigurationException(
          "bmc.terminationMode and bmc.nonTerminationMode cannot be enabled at the same time.");
    }

    config = pConfig;
    cfa = pCFA;
    analysisCpa = pCPA;

    argWitnessExporter = new WitnessExporter(config, logger, specification, cfa);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet reachedSet)
      throws CPAException, InterruptedException {
    try {
      AlgorithmStatus status = super.run(reachedSet);
      if ((terminationMode || nonTerminationMode)
          && terminationCandidatesIncomplete
          && status.isSound()) {
        logger.log(
            Level.WARNING,
            terminationMode
                ? "Termination mode could not derive loop structure information; downgrading the"
                    + " result to UNKNOWN."
                : "Non-termination mode could not derive loop structure information; downgrading"
                    + " the result to UNKNOWN.");
        return status.withSound(false);
      }
      return status;
    } catch (SolverException e) {
      throw new CPAException("Solver Failure " + e.getMessage(), e);
    } finally {
      invariantGenerator.cancel();
    }
  }

  @Override
  protected boolean isTerminationMode() {
    return terminationMode;
  }

  @Override
  protected boolean isNonTerminationMode() {
    return nonTerminationMode;
  }

  @Override
  protected CandidateGenerator getCandidateInvariants() {
    if (terminationMode) {
      return createLoopContinuationCandidateGenerator(true);
    }
    if (nonTerminationMode) {
      return createLoopContinuationCandidateGenerator(false);
    }
    terminationCandidatesIncomplete = false;
    loopsWithoutTerminationCandidates = 0;
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
      final BasicProverEnvironment<?> pProver,
      CandidateInvariant pInductionProblem)
      throws CPATransferException, InterruptedException, SolverException {
    if (nonTerminationMode) {
      return checkNonTerminationBaseCase(pReachedSet, pProver, pInductionProblem);
    }
    if (!checkTargetStates) {
      return true;
    }

    return super.boundedModelCheck(pReachedSet, pProver, pInductionProblem);
  }

  @Override
  protected void reportConfirmedNonTermination(
      ReachedSet pReachedSet, CandidateInvariant pCandidateInvariant) {
    logger.logf(
        Level.INFO,
        "Non-termination mode: k-induction confirmed loop-continuation candidate %s.",
        pCandidateInvariant);
    pReachedSet.add(
        new DummyErrorState(pReachedSet.getLastState()) {
          @Serial private static final long serialVersionUID = 4603081304830409726L;

          @Override
          public Set<TargetInformation> getTargetInformation() {
            return PropertyTargetInformation.singleton(CommonVerificationProperty.TERMINATION);
          }
        },
        SingletonPrecision.getInstance());
  }

  private CandidateGenerator createLoopContinuationCandidateGenerator(boolean pNegated) {
    terminationCandidatesIncomplete = false;
    loopsWithoutTerminationCandidates = 0;
    if (!cfa.getLoopStructure().isPresent()) {
      terminationCandidatesIncomplete = true;
      logger.log(
          Level.WARNING,
          pNegated
              ? "Termination mode is enabled, but loop structure is unavailable."
              : "Non-termination mode is enabled, but loop structure is unavailable.");
      return CandidateGenerator.EMPTY_GENERATOR;
    }
    ImmutableSet.Builder<CandidateInvariant> candidates = ImmutableSet.builder();
    for (Loop loop : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
      ImmutableSet<CandidateInvariant> loopCandidates =
          getLoopContinuationCandidates(loop, pNegated);
      if (loopCandidates.isEmpty()) {
        loopsWithoutTerminationCandidates++;
        logger.logf(
            Level.FINE,
            pNegated
                ? "Termination mode could not derive a loop-continuation candidate for loop heads"
                    + " %s."
                : "Non-termination mode could not derive a loop-continuation candidate for loop"
                    + " heads %s.",
            loop.getLoopHeads());
      } else {
        candidates.addAll(loopCandidates);
      }
    }
    ImmutableSet<CandidateInvariant> continuationCandidates = candidates.build();
    if (continuationCandidates.isEmpty()) {
      if (loopsWithoutTerminationCandidates > 0) {
        logger.logf(
            Level.INFO,
            pNegated
                ? "Termination mode could not derive loop-continuation candidates for %d loop(s);"
                    + " relying on unwinding assertions only."
                : "Non-termination mode could not derive loop-continuation candidates for %d"
                    + " loop(s).",
            loopsWithoutTerminationCandidates);
      } else {
        logger.log(
            Level.INFO,
            pNegated
                ? "Termination mode is enabled, but no loop-continuation candidates were created."
                : "Non-termination mode is enabled, but no loop-continuation candidates were"
                    + " created.");
      }
      return CandidateGenerator.EMPTY_GENERATOR;
    }
    if (loopsWithoutTerminationCandidates > 0) {
      logger.logf(
          Level.INFO,
          pNegated
              ? "Termination mode could not derive loop-continuation candidates for %d loop(s);"
                  + " those loops will be handled via unwinding assertions."
              : "Non-termination mode could not derive loop-continuation candidates for %d"
                  + " loop(s).",
          loopsWithoutTerminationCandidates);
    }
    logger.logf(
        Level.INFO,
        pNegated
            ? "Termination mode is enabled; checking %d loop-continuation candidates."
            : "Non-termination mode is enabled; checking %d loop-continuation candidates.",
        continuationCandidates.size());
    return new StaticCandidateProvider(continuationCandidates);
  }

  private ImmutableSet<CandidateInvariant> getLoopContinuationCandidates(
      Loop pLoop, boolean pNegated) {
    ImmutableSet.Builder<CandidateInvariant> candidates = ImmutableSet.builder();
    addLoopHeadCandidates(pLoop, candidates, pNegated);
    addInternalExitGuardCandidates(pLoop, candidates, pNegated);
    if (!pNegated) {
      addUnconditionalLoopHeadCandidates(pLoop, candidates);
      addLoopExitViolationCandidates(pLoop, candidates);
    }
    ImmutableSet<CandidateInvariant> loopCandidates = candidates.build();
    if (loopCandidates.isEmpty()) {
      return loopCandidates;
    }
    return ImmutableSet.of(
        pNegated
            ? new StatewiseCandidateInvariantDisjunction(loopCandidates)
            : new StatewiseCandidateInvariantConjunction(loopCandidates));
  }

  private void addLoopHeadCandidates(
      Loop pLoop, ImmutableSet.Builder<CandidateInvariant> pCandidates, boolean pNegated) {
    for (CFANode loopHead : pLoop.getLoopHeads()) {
      addSingleLocationContinuationCandidatesAtNode(pLoop, loopHead, pCandidates, pNegated);
    }
  }

  private void addInternalExitGuardCandidates(
      Loop pLoop, ImmutableSet.Builder<CandidateInvariant> pCandidates, boolean pNegated) {
    for (CFANode loopNode : pLoop.getLoopNodes()) {
      if (pLoop.getLoopHeads().contains(loopNode)) {
        continue;
      }
      addLoopScopedContinuationCandidatesAtNode(pLoop, loopNode, pCandidates, pNegated);
    }
  }

  private void addUnconditionalLoopHeadCandidates(
      Loop pLoop, ImmutableSet.Builder<CandidateInvariant> pCandidates) {
    if (!pLoop.getOutgoingEdges().isEmpty()) {
      return;
    }
    for (CFANode loopHead : pLoop.getLoopHeads()) {
      pCandidates.add(SingleLocationFormulaInvariant.makeBooleanInvariant(loopHead, true));
    }
  }

  private void addSingleLocationContinuationCandidatesAtNode(
      Loop pLoop,
      CFANode pNode,
      ImmutableSet.Builder<CandidateInvariant> pCandidates,
      boolean pNegated) {
    for (CFAEdge leavingEdge : pNode.getLeavingEdges()) {
      if (leavingEdge instanceof AssumeEdge assumeEdge
          && pLoop.getLoopNodes().contains(assumeEdge.getSuccessor())) {
        pCandidates.add(
            pNegated
                ? new FrontierEdgeFormulaNegation(pNode, assumeEdge)
                : new EdgeFormula(pNode, assumeEdge));
      }
    }
  }

  private void addLoopScopedContinuationCandidatesAtNode(
      Loop pLoop,
      CFANode pNode,
      ImmutableSet.Builder<CandidateInvariant> pCandidates,
      boolean pNegated) {
    ImmutableSet<CFANode> loopNodes = ImmutableSet.copyOf(pLoop.getLoopNodes());
    boolean hasExitAlternative = false;
    for (CFAEdge leavingEdge : pNode.getLeavingEdges()) {
      if (leavingEdge instanceof AssumeEdge assumeEdge && branchLeavesLoop(pLoop, assumeEdge)) {
        hasExitAlternative = true;
        break;
      }
    }
    if (!hasExitAlternative) {
      return;
    }
    for (CFAEdge leavingEdge : pNode.getLeavingEdges()) {
      if (leavingEdge instanceof AssumeEdge assumeEdge
          && pLoop.getLoopNodes().contains(assumeEdge.getSuccessor())
          && !branchLeavesLoop(pLoop, assumeEdge)) {
        pCandidates.add(
            pNegated
                ? new LoopScopedFrontierEdgeFormulaNegation(pNode, loopNodes, assumeEdge)
                : new EdgeFormula(pNode, assumeEdge));
      }
    }
  }

  private void addLoopExitViolationCandidates(
      Loop pLoop, ImmutableSet.Builder<CandidateInvariant> pCandidates) {
    for (CFAEdge outgoingEdge : pLoop.getOutgoingEdges()) {
      pCandidates.add(
          SingleLocationFormulaInvariant.makeBooleanInvariant(outgoingEdge.getSuccessor(), false));
    }
  }

  private boolean checkNonTerminationBaseCase(
      ReachedSet pReachedSet,
      BasicProverEnvironment<?> pProver,
      CandidateInvariant pCandidateInvariant)
      throws CPATransferException, InterruptedException, SolverException {
    BooleanFormula baseCase = createNonTerminationBaseCaseFormula(pReachedSet, pCandidateInvariant);
    logger.log(Level.INFO, "Starting satisfiability check for non-termination base case...");
    stats.satCheck.start();
    try {
      pProver.push(baseCase);
      return !pProver.isUnsat();
    } finally {
      stats.satCheck.stop();
      pProver.pop();
    }
  }

  private BooleanFormula createNonTerminationBaseCaseFormula(
      Iterable<AbstractState> pReachedSet, CandidateInvariant pCandidateInvariant)
      throws CPATransferException, InterruptedException {
    BooleanFormulaManagerView bfmgr = getBooleanFormulaManager();
    BooleanFormula result = bfmgr.makeFalse();
    int currentK =
        CPAs.retrieveCPA(analysisCpa, LoopIterationBounding.class).getMaxLoopIterations();

    for (AbstractState state :
        BMCHelper.filterIteration(pReachedSet, currentK, cfa.getAllLoopHeads().orElseThrow())) {
      if (!isStopState(state)
          || !isRelevantForReachability(state)
          || !candidateAppliesToState(pCandidateInvariant, state)) {
        continue;
      }
      Optional<BooleanFormula> pathBaseCase =
          createNonTerminationPathBaseCaseFormula(state, pCandidateInvariant);
      result = bfmgr.or(result, pathBaseCase.orElse(bfmgr.makeFalse()));
    }
    return result;
  }

  private Optional<BooleanFormula> createNonTerminationPathBaseCaseFormula(
      AbstractState pStopState, CandidateInvariant pCandidateInvariant)
      throws CPATransferException, InterruptedException {
    BooleanFormulaManagerView bfmgr = getBooleanFormulaManager();
    BooleanFormula result = bfmgr.makeTrue();
    boolean foundApplicableState = false;

    for (AbstractState state : getPathStatesTo(pStopState)) {
      if (!candidateAppliesToState(pCandidateInvariant, state)) {
        continue;
      }
      Optional<BooleanFormula> stateFormula = createStateFormula(state);
      if (stateFormula.isEmpty()) {
        continue;
      }
      BooleanFormula stateAssertion =
          pCandidateInvariant.getAssertion(
              Collections.singleton(state), getFormulaManager(), getPathFormulaManager());
      result = bfmgr.and(result, stateFormula.orElseThrow(), stateAssertion);
      foundApplicableState = true;
    }

    return foundApplicableState ? Optional.of(result) : Optional.empty();
  }

  private ImmutableList<AbstractState> getPathStatesTo(AbstractState pState) {
    ARGState argState = AbstractStates.extractStateByType(pState, ARGState.class);
    if (argState == null) {
      return ImmutableList.of(pState);
    }

    return ImmutableList.copyOf(ARGUtils.getOnePathTo(argState).asStatesList());
  }

  private Optional<BooleanFormula> createStateFormula(AbstractState pState) {
    PredicateAbstractState predicateState =
        AbstractStates.extractStateByType(pState, PredicateAbstractState.class);
    if (predicateState == null) {
      return Optional.empty();
    }

    BooleanFormulaManagerView bfmgr = getBooleanFormulaManager();
    PathFormula pathFormula = predicateState.getPathFormula();
    BooleanFormula stateFormula =
        bfmgr.and(
            predicateState.getAbstractionFormula().getBlockFormula().getFormula(),
            pathFormula.getFormula());
    return bfmgr.isFalse(stateFormula) ? Optional.empty() : Optional.of(stateFormula);
  }

  private boolean candidateAppliesToState(
      CandidateInvariant pCandidateInvariant, AbstractState pState) {
    return pCandidateInvariant.filterApplicable(Collections.singleton(pState)).iterator().hasNext();
  }

  private boolean branchLeavesLoop(Loop pLoop, CFAEdge pEdge) {
    if (!pLoop.getLoopNodes().contains(pEdge.getSuccessor())) {
      return true;
    }
    return mustLeaveLoopOnStraightLine(pLoop, pEdge.getSuccessor());
  }

  private boolean mustLeaveLoopOnStraightLine(Loop pLoop, CFANode pStartNode) {
    Set<CFANode> visited = new HashSet<>();
    CFANode current = pStartNode;
    while (pLoop.getLoopNodes().contains(current) && visited.add(current)) {
      if (current.getNumLeavingEdges() != 1) {
        return false;
      }
      CFAEdge leavingEdge = current.getLeavingEdge(0);
      if (!pLoop.getLoopNodes().contains(leavingEdge.getSuccessor())) {
        return true;
      }
      current = leavingEdge.getSuccessor();
    }
    return false;
  }

  @Override
  protected void analyzeCounterexample(
      final BooleanFormula pCounterexampleFormula,
      final ReachedSet pReachedSet,
      final BasicProverEnvironment<?> pProver)
      throws CPATransferException, InterruptedException {

    analyzeCounterexample0(pCounterexampleFormula, pReachedSet, pProver)
        .ifPresentOrElse(
            cex -> cex.getTargetState().addCounterexampleInformation(cex),
            () -> {
              if (!allowImpreciseCounterexamples) {
                throw new AssertionError(
                    "Found imprecise counterexample with BMC. "
                        + "If this is expected for this configuration "
                        + "(e.g., because of UF-based heap encoding), "
                        + "set counterexample.export.allowImpreciseCounterexamples=true. "
                        + "Otherwise please report this as a bug.");
              }
            });
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
                  tmpExpressionTreeSupplier = invariantGenerator.getExpressionTreeSupplier();
                } catch (CPAException | InterruptedException e1) {
                  tmpExpressionTreeSupplier =
                      ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;
                }
              }
              final ExpressionTreeSupplier expSup = tmpExpressionTreeSupplier;
              try (Writer w = IO.openOutputFile(invariantsExport, StandardCharsets.UTF_8)) {
                final Witness generatedWitness =
                    argWitnessExporter.generateProofWitness(
                        rootState,
                        Predicates.alwaysTrue(),
                        BiPredicates.alwaysTrue(),
                        new InvariantProvider() {
                          @Override
                          public ExpressionTree<Object> provideInvariantFor(
                              CFAEdge pCFAEdge,
                              Optional<? extends Collection<? extends ARGState>> pStates)
                              throws InterruptedException {
                            CFANode node = pCFAEdge.getSuccessor();
                            ExpressionTree<Object> result = expSup.getInvariantFor(node);
                            if (ExpressionTrees.getFalse().equals(result) && !pStates.isPresent()) {
                              return ExpressionTrees.getTrue();
                            }
                            return result;
                          }
                        });
                WitnessToOutputFormatsUtils.writeToGraphMl(generatedWitness, w);
              } catch (IOException e) {
                logger.logUserException(
                    Level.WARNING, e, "Could not write invariants to file " + invariantsExport);
              } catch (InterruptedException e) {
                logger.logUserException(
                    Level.WARNING, e, "Could not export witness due to interruption");
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
