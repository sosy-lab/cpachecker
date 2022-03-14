// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Comparator.comparingInt;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition.getDefaultPartition;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.LassoAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.LassoAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelation;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.SimpleTargetInformation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets.AggregatedReachedSetManager;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.waitlist.AlwaysEmptyWaitlist;
import org.sosy_lab.cpachecker.cpa.alwaystop.AlwaysTopCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPathBuilder;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.termination.TerminationCPA;
import org.sosy_lab.cpachecker.cpa.termination.TerminationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAEdgeUtils;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/** Algorithm that uses a safety-analysis to prove (non-)termination. */
@Options(prefix = "termination")
public class TerminationAlgorithm implements Algorithm, AutoCloseable, StatisticsProvider {

  private static final ImmutableSet<TargetInformation> TERMINATION_PROPERTY =
      SimpleTargetInformation.singleton("termination");

  private enum ResetReachedSetStrategy {
    REMOVE_TARGET_STATE,
    REMOVE_LOOP,
    RESET
  }

  @Option(
      secure = true,
      description =
          "Strategy used to prepare reched set and ARG for next iteration "
              + "after successful refinement of the termination argument.")
  private ResetReachedSetStrategy resetReachedSetStrategy = ResetReachedSetStrategy.REMOVE_LOOP;

  @Option(
      secure = true,
      description =
          "maximal number of repeated ranking functions per loop before stopping analysis")
  @IntegerOption(min = 1)
  private int maxRepeatedRankingFunctionsPerLoop = 10;

  @Option(
      secure = true,
      description =
          "consider counterexamples for loops for which only pointer variables are relevant or"
              + " which check that pointer is unequal to null pointer to be imprecise")
  private boolean useCexImpreciseHeuristic = false;

  @Option(secure = true, description = "enable to also analyze whether recursive calls terminate")
  private boolean considerRecursion = false;

  private final TerminationStatistics statistics;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;
  private final ReachedSetFactory reachedSetFactory;
  private final Algorithm safetyAlgorithm;
  private final ConfigurableProgramAnalysis safetyCPA;

  private final LassoAnalysis lassoAnalysis;
  private final TerminationLoopInformation terminationInformation;
  private final Set<CVariableDeclaration> globalDeclaration;
  private final SetMultimap<String, CVariableDeclaration> localDeclarations;

  private final AggregatedReachedSetManager aggregatedReachedSetManager;

  public TerminationAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      ReachedSetFactory pReachedSetFactory,
      AggregatedReachedSetManager pAggregatedReachedSetManager,
      Algorithm pSafetyAlgorithm,
      ConfigurableProgramAnalysis pSafetyCPA)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = checkNotNull(pLogger);
    shutdownNotifier = pShutdownNotifier;
    cfa = checkNotNull(pCfa);
    reachedSetFactory = checkNotNull(pReachedSetFactory);
    aggregatedReachedSetManager = checkNotNull(pAggregatedReachedSetManager);
    safetyAlgorithm = checkNotNull(pSafetyAlgorithm);
    safetyCPA = checkNotNull(pSafetyCPA);

    TerminationCPA terminationCpa =
        CPAs.retrieveCPAOrFail(pSafetyCPA, TerminationCPA.class, TerminationAlgorithm.class);
    terminationInformation = terminationCpa.getTerminationInformation();

    DeclarationCollectionCFAVisitor visitor = new DeclarationCollectionCFAVisitor();
    for (CFANode function : cfa.getAllFunctionHeads()) {
      CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(function, visitor);
    }
    localDeclarations = ImmutableSetMultimap.copyOf(visitor.localDeclarations);
    globalDeclaration = ImmutableSet.copyOf(visitor.globalDeclarations);

    LoopStructure loopStructure =
        cfa.getLoopStructure()
            .orElseThrow(
                () ->
                    new InvalidConfigurationException(
                        "Loop structure is not present, but required for termination analysis."));

    statistics =
        new TerminationStatistics(pConfig, logger, loopStructure.getAllLoops().size(), pCfa);
    lassoAnalysis = LassoAnalysis.create(pLogger, pConfig, pShutdownNotifier, pCfa, statistics);
  }

  @Override
  public void close() {
    lassoAnalysis.close();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {

    statistics.algorithmStarted();
    try {
      return run0(pReachedSet);

    } finally {
      statistics.algorithmFinished();
    }
  }

  private AlgorithmStatus run0(ReachedSet pReachedSet) throws InterruptedException, CPAException {
    logger.log(Level.INFO, "Starting termination algorithm.");

    if (cfa.getLanguage() != Language.C) {
      logger.log(WARNING, "Termination analysis supports only C.");
      return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
    }

    CFANode initialLocation = AbstractStates.extractLocation(pReachedSet.getFirstState());
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_IMPRECISE;

    List<Loop> allLoops = new ArrayList<>(cfa.getLoopStructure().orElseThrow().getAllLoops());
    Collections.sort(allLoops, comparingInt(l -> l.getInnerLoopEdges().size()));

    if (considerRecursion) {
      List<Loop> allRecursions = new ArrayList<>(LoopStructure.getRecursions(cfa));
      Collections.sort(allRecursions, comparingInt(l -> l.getInnerLoopEdges().size()));
      allLoops.addAll(allRecursions);
    }

    for (Loop loop : allLoops) {
      shutdownNotifier.shutdownIfNecessary();
      statistics.analysisOfLoopStarted(loop);

      if (considerRecursion) {
        setExplicitAbstractionNodes(ImmutableSet.of());
      }
      resetReachedSet(pReachedSet, initialLocation);
      CPAcheckerResult.Result loopTermination =
          proveLoopTermination(pReachedSet, loop, initialLocation);

      if (loopTermination == Result.FALSE) {
        logger.logf(Level.FINE, "Proved non-termination of %s.", loop);
        return AlgorithmStatus.UNSOUND_AND_PRECISE;

      } else if (loopTermination != Result.TRUE) {
        logger.logf(FINE, "Could not prove (non-)termination of %s.", loop);
        status = status.withSound(false);
      }

      statistics.analysisOfLoopFinished(loop);
    }

    if (status.isSound() && !considerRecursion) {
      status = status.update(checkRecursion(initialLocation));
    }

    // We did not find a non-terminating loop.
    logger.log(Level.INFO, "Termination algorithm did not find a non-terminating loop.");
    while (status.isSound() && pReachedSet.hasWaitingState()) {
      pReachedSet.popFromWaitlist();
    }
    return status;
  }

  private Result proveLoopTermination(ReachedSet pReachedSet, Loop pLoop, CFANode initialLocation)
      throws CPAException, InterruptedException {

    logger.logf(Level.FINE, "Prooving (non)-termination of %s", pLoop);
    Set<RankingRelation> rankingRelations = new HashSet<>();
    int totalRepeatedRankingFunctions = 0;
    int repeatedRankingFunctionsSinceSuccessfulIteration = 0;

    // Pass current loop and relevant variables to TerminationCPA.
    Set<CVariableDeclaration> relevantVariables = getRelevantVariables(pLoop);
    terminationInformation.setProcessedLoop(pLoop, relevantVariables);

    if (considerRecursion) {
      setExplicitAbstractionNodes(pLoop);
    }

    Result result = Result.TRUE;
    Optional<CounterexampleInfo> foundCounterexample = Optional.empty();
    while (pReachedSet.hasWaitingState() && result != Result.FALSE) {
      shutdownNotifier.shutdownIfNecessary();
      statistics.safetyAnalysisStarted(pLoop);
      AlgorithmStatus status = safetyAlgorithm.run(pReachedSet);
      terminationInformation.resetCfa();
      statistics.safetyAnalysisFinished(pLoop);
      shutdownNotifier.shutdownIfNecessary();

      boolean targetReached =
          pReachedSet.asCollection().stream().anyMatch(AbstractStates::isTargetState);
      Optional<ARGState> targetStateWithCounterExample =
          pReachedSet.stream()
              .filter(AbstractStates::isTargetState)
              .map(s -> AbstractStates.extractStateByType(s, ARGState.class))
              .filter(s -> s.getCounterexampleInformation().isPresent())
              .findAny();

      // potential non-termination
      if (status.isPrecise() && targetStateWithCounterExample.isPresent()) {

        ARGState targetState = targetStateWithCounterExample.orElseThrow();
        CounterexampleInfo originalCounterexample =
            targetState.getCounterexampleInformation().orElseThrow();
        ARGState loopHeadState = Iterables.getOnlyElement(targetState.getParents());
        ARGState nonTerminationLoopHead = createNonTerminationState(loopHeadState);
        CounterexampleInfo counterexample =
            removeDummyLocationsFromCounterExample(originalCounterexample, nonTerminationLoopHead);
        LassoAnalysisResult lassoAnalysisResult =
            lassoAnalysis.checkTermination(pLoop, counterexample, relevantVariables);

        if (lassoAnalysisResult.hasNonTerminationArgument()) {
          removeIntermediateStates(pReachedSet, targetState);
          result = Result.FALSE;
          foundCounterexample = Optional.of(counterexample);

          statistics.setNonterminatingLoop(pLoop);

        } else if (lassoAnalysisResult.hasTerminationArgument()) {
          RankingRelation rankingRelation = lassoAnalysisResult.getTerminationArgument();

          // Do not add a ranking relation twice
          if (rankingRelations.add(rankingRelation)) {
            terminationInformation.addRankingRelation(rankingRelation);
            // Prepare reached set for next iteration.
            prepareForNextIteration(pReachedSet, targetState, initialLocation);
            addInvariantsToAggregatedReachedSet(loopHeadState, rankingRelation);
            // a ranking relation was synthesized and the reached set was reseted
            result = Result.TRUE;
            repeatedRankingFunctionsSinceSuccessfulIteration = 0;

          } else {
            totalRepeatedRankingFunctions++;
            repeatedRankingFunctionsSinceSuccessfulIteration++;
            logger.logf(WARNING, "Repeated ranking relation %s for %s", rankingRelation, pLoop);

            // Do not use the first reached target state again and again
            // if we cannot synthesis new termination arguments from it.
            if (repeatedRankingFunctionsSinceSuccessfulIteration
                > maxRepeatedRankingFunctionsPerLoop / 5) {
              removeTargetState(pReachedSet, targetState);
              result = Result.UNKNOWN;

            } else if (totalRepeatedRankingFunctions >= maxRepeatedRankingFunctionsPerLoop) {
              // stop analysis for this loop because there is no progress
              removeTargetState(pReachedSet, targetState);
              return Result.UNKNOWN;

            } else {
              // Prepare reached set for next iteration.
              prepareForNextIteration(pReachedSet, targetState, initialLocation);
              // a ranking relation was synthesized and the reached set was reseted
              result = Result.TRUE;
            }
          }

        } else { // no termination argument and no non-termination argument could be synthesized
          logger.logf(WARNING, "Could not synthesize a termination or non-termination argument.");
          removeTargetState(pReachedSet, targetState);
          result = Result.UNKNOWN;
        }

      } else if (!status.isSound() || targetReached || pReachedSet.hasWaitingState()) {
        result = Result.UNKNOWN; // unsound, but still precise
      }
    }

    if (useCexImpreciseHeuristic && result == Result.FALSE) {
      if (allRelevantVarsArePointers(relevantVariables)
          || doesImpreciseOperationOccur(foundCounterexample)) {
        logger.logf(INFO, "Counterexample to termination found, but deemed imprecise");
        return Result.UNKNOWN;
      } else {
        for (CFAEdge edge : pLoop.getOutgoingEdges()) {
          if (edge instanceof CAssumeEdge
              && possiblyNotEqualsNullPointer(((CAssumeEdge) edge).getExpression())) {
            return Result.UNKNOWN;
          }
        }
      }
    }

    return result;
  }

  private boolean doesImpreciseOperationOccur(Optional<CounterexampleInfo> pCounterexampleInfo) {
    if (pCounterexampleInfo.isEmpty()) {
      return false;
    }
    CounterexampleInfo cex = pCounterexampleInfo.orElseThrow();

    List<CFAEdge> edgesOnCex = cex.getTargetPath().getFullPath();
    for (CFAEdge edge : edgesOnCex) {
      CRightHandSide expression = CFAEdgeUtils.getRightHandSide(edge);
      if (expression != null && containsBinaryOperation(expression)) {
        return true;
      }
      if (edge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        if (containsBinaryOperation(((CAssumeEdge) edge).getExpression())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean containsBinaryOperation(CRightHandSide expression) {
    if (expression instanceof CBinaryExpression) {
      CBinaryExpression binaryExp = (CBinaryExpression) expression;
      BinaryOperator operator = binaryExp.getOperator();

      if (isBitwiseBinaryOperation(operator)
          || isMultiplicationWithEqualFactors(operator, binaryExp)) {
        return true;
      }

      return containsBinaryOperation(binaryExp.getOperand1())
          || containsBinaryOperation(binaryExp.getOperand2());
    }
    if (expression instanceof CUnaryExpression) {
      CUnaryExpression unaryExp = (CUnaryExpression) expression;
      UnaryOperator operator = unaryExp.getOperator();

      if (operator.equals(UnaryOperator.TILDE)) {
        return true;
      }
      return containsBinaryOperation(unaryExp.getOperand());
    }
    return false;
  }

  private boolean isBitwiseBinaryOperation(BinaryOperator pOperator) {
    switch (pOperator) {
      case BINARY_AND:
      case BINARY_XOR:
      case BINARY_OR:
        return true;
      default:
        return false;
    }
  }

  /**
   * This method filters out multiplication operations that contain equal factors, since LassoRanker
   * is not able to compute precise counterexamples for those.
   *
   * <p>As an example, the operation (n * m * n <= x) is filtered out due to the factor n being
   * there twice (multiplied with itself). An operation (n * m * k <= x) is however unproblematic
   * and therefore left in place.
   */
  private boolean isMultiplicationWithEqualFactors(
      BinaryOperator pOperator, CBinaryExpression pExpression) {

    if (pOperator == BinaryOperator.MULTIPLY) {
      Set<CIdExpression> factors = new HashSet<>();
      return containsEqualFactors(pExpression, factors);
    }

    return false;
  }

  private boolean containsEqualFactors(CRightHandSide pExpression, Set<CIdExpression> pFactors) {
    if (pExpression instanceof CIdExpression) {
      if (!pFactors.add((CIdExpression) pExpression)) {
        // The expression was not added to the set because there is already one that is equal
        return true;
      }
    }

    boolean equalFactorsFound = false;
    if (pExpression instanceof CBinaryExpression) {

      CBinaryExpression binaryExpression = (CBinaryExpression) pExpression;
      if (binaryExpression.getOperator() == BinaryOperator.MULTIPLY) {
        equalFactorsFound |= containsEqualFactors(binaryExpression.getOperand1(), pFactors);
        equalFactorsFound |= containsEqualFactors(binaryExpression.getOperand2(), pFactors);
      }
    }

    return equalFactorsFound;
  }

  private boolean allRelevantVarsArePointers(final Set<CVariableDeclaration> pRelevantVariables) {
    if (pRelevantVariables.isEmpty()) {
      return false;
    }
    boolean allPointers = true;
    for (CVariableDeclaration var : pRelevantVariables) {
      if (!(var.getType() instanceof CPointerType)) {
        allPointers = false;
        break;
      }
    }
    return allPointers;
  }

  private boolean possiblyNotEqualsNullPointer(final CExpression expr) {
    if (expr instanceof CBinaryExpression) {
      CBinaryExpression binExpr = (CBinaryExpression) expr;
      if (binExpr.getOperator() == BinaryOperator.NOT_EQUALS
          && binExpr.getOperand2() instanceof CCastExpression
          && binExpr.getOperand2().getExpressionType() instanceof CPointerType
          && ((CCastExpression) binExpr.getOperand2()).getOperand() instanceof CLiteralExpression) {
        return true;
      }
    }
    return false;
  }

  private void addInvariantsToAggregatedReachedSet(
      ARGState loopHeadState, RankingRelation rankingRelation) {
    // Create dummy reached set as holder for invariants. We do not use the reached-set factory
    // from configuration because that could require features that our dummy states do not support.
    // We use LocationMappedReachedSet because that seems useful to callers.
    // Using AlwaysTopCPA as dummy is ok as long as UnmodifiableReachedSet does not have getCPA(),
    // because the AggregatedReachedSet only exposes reached sets as UnmodifiableReachedSet,
    // so no other code will be able to call dummy.getCPA().
    ReachedSet dummy =
        new LocationMappedReachedSet(AlwaysTopCPA.INSTANCE, AlwaysEmptyWaitlist.factory());
    CFANode location = AbstractStates.extractLocation(loopHeadState);
    FormulaManagerView fmgr = rankingRelation.getFormulaManager();

    rankingRelation.getSupportingInvariants().stream()
        .map(invariant -> new TerminationInvariantSupplierState(location, invariant, fmgr))
        .forEach(s -> dummy.addNoWaitlist(s, SingletonPrecision.getInstance()));

    aggregatedReachedSetManager.addReachedSet(dummy);
  }

  private static class TerminationInvariantSupplierState
      implements AbstractStateWithLocation, FormulaReportingState {

    private final CFANode location;
    private final FormulaManagerView fmgr;
    private final BooleanFormula invariant;

    public TerminationInvariantSupplierState(
        CFANode pLocation, BooleanFormula pInvariant, FormulaManagerView pFmgr) {
      location = checkNotNull(pLocation);
      invariant = checkNotNull(pInvariant);
      fmgr = checkNotNull(pFmgr);
    }

    @Override
    public BooleanFormula getFormulaApproximation(FormulaManagerView pManager) {
      return pManager.translateFrom(invariant, fmgr);
    }

    @Override
    public CFANode getLocationNode() {
      return location;
    }

    @Override
    public String toString() {
      return TerminationInvariantSupplierState.class.getSimpleName() + "[" + invariant + "]";
    }
  }

  private Set<CVariableDeclaration> getRelevantVariables(Loop pLoop) {
    CFANode firstLoopHead = pLoop.getLoopHeads().iterator().next();
    if (firstLoopHead instanceof FunctionEntryNode) {
      ImmutableSet.Builder<CVariableDeclaration> relVarBuilder = ImmutableSet.builder();
      relVarBuilder.addAll(globalDeclaration);
      for (CFANode entryNode :
          FluentIterable.from(pLoop.getLoopNodes()).filter(FunctionEntryNode.class)) {
        relVarBuilder.addAll(localDeclarations.get(entryNode.getFunctionName()));
      }
      return relVarBuilder.build();
    } else {
      String function = firstLoopHead.getFunctionName();
      Set<CVariableDeclaration> relevantVariabels =
          ImmutableSet.<CVariableDeclaration>builder()
              .addAll(globalDeclaration)
              .addAll(localDeclarations.get(function))
              .build();
      return relevantVariabels;
    }
  }

  private void removeIntermediateStates(ReachedSet pReachedSet, AbstractState pTargetState) {
    Preconditions.checkArgument(AbstractStates.isTargetState(pTargetState));
    Preconditions.checkArgument(!cfa.getAllNodes().contains(extractLocation(pTargetState)));
    ARGState targetState = AbstractStates.extractStateByType(pTargetState, ARGState.class);
    Preconditions.checkArgument(targetState.getCounterexampleInformation().isPresent());
    CounterexampleInfo counterexample = targetState.getCounterexampleInformation().orElseThrow();

    // Remove dummy target state from ARG and replace loop head with new target state
    ARGState loopHead = Iterables.getOnlyElement(targetState.getParents());
    ARGState newTargetState = createNonTerminationState(loopHead);
    targetState.removeFromARG();
    loopHead.replaceInARGWith(newTargetState);

    // Remove dummy target state from reached set and replace loop head with new target state
    pReachedSet.addNoWaitlist(newTargetState, pReachedSet.getPrecision(loopHead));
    pReachedSet.remove(pTargetState);
    pReachedSet.remove(loopHead);

    CounterexampleInfo newCounterexample =
        removeDummyLocationsFromCounterExample(counterexample, newTargetState);
    newTargetState.addCounterexampleInformation(newCounterexample);
  }

  private ARGState createNonTerminationState(AbstractState loopHead) {
    TerminationState terminationState = extractStateByType(loopHead, TerminationState.class);
    AbstractState newTerminationState =
        terminationState.withTargetInformation(TERMINATION_PROPERTY);
    ARGState newTargetState = new ARGState(newTerminationState, null);
    return newTargetState;
  }

  /**
   * Removes all intermediate states from the counterexample.
   *
   * @param counterexample the {@link CounterexampleInfo} to remove the states from
   * @param newTargetState the new target state which is the last state of the counterexample
   * @return the created {@link CounterexampleInfo}
   */
  private CounterexampleInfo removeDummyLocationsFromCounterExample(
      CounterexampleInfo counterexample, ARGState newTargetState) {

    // The value assignments are not valid for a counterexample that witnesses non-termination.
    ARGPath targetPath = counterexample.getTargetPath();
    PathIterator targetPathIt = targetPath.fullPathIterator();
    ARGPathBuilder builder = ARGPath.builder();
    Optional<ARGState> lastStateInCfa = Optional.empty();

    do {
      // the last state has not outgoing edge
      if (targetPathIt.hasNext()) {
        CFAEdge outgoingEdge = targetPathIt.getOutgoingEdge();
        CFANode location = outgoingEdge.getPredecessor();
        CFANode nextLocation = outgoingEdge.getSuccessor();

        if (cfa.getAllNodes().contains(location) && cfa.getAllNodes().contains(nextLocation)) {

          if (targetPathIt.isPositionWithState() || lastStateInCfa.isPresent()) {
            ARGState state = lastStateInCfa.orElseGet(targetPathIt::getAbstractState);
            ARGState nextAbstractState = targetPathIt.getNextAbstractState();

            @Nullable CFAEdge edgeToNextState = null;
            // use only edges matching the next location
            if (AbstractStates.extractLocation(nextAbstractState).equals(nextLocation)) {
              edgeToNextState = state.getEdgeToChild(nextAbstractState);
            }
            builder.add(state, edgeToNextState);
          }

          lastStateInCfa = Optional.empty();

        } else if (cfa.getAllNodes().contains(location) && targetPathIt.isPositionWithState()) {
          lastStateInCfa = Optional.of(targetPathIt.getAbstractState());
        }
      }
    } while (targetPathIt.advanceIfPossible());

    ARGPath newTargetPath = builder.build(newTargetState);
    CounterexampleInfo newCounterexample = CounterexampleInfo.feasibleImprecise(newTargetPath);
    return newCounterexample;
  }

  private AlgorithmStatus checkRecursion(CFANode initialLocation)
      throws CPAException, InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    statistics.analysisOfRecursionStarted();

    // the safety analysis will fail if the program is recursive
    try {
      terminationInformation.reset();
      ReachedSet reachedSet = reachedSetFactory.create(safetyCPA);
      resetReachedSet(reachedSet, initialLocation);
      return safetyAlgorithm.run(reachedSet);
    } finally {
      statistics.analysisOfRecursionFinished();
    }
  }

  private void prepareForNextIteration(
      ReachedSet pReachedSet, ARGState pTargetState, CFANode pInitialLocation)
      throws InterruptedException {

    switch (resetReachedSetStrategy) {
      case REMOVE_TARGET_STATE:
        pTargetState.getParents().forEach(pReachedSet::reAddToWaitlist);
        removeTargetState(pReachedSet, pTargetState);
        break;

      case REMOVE_LOOP:
        removeLoop(pReachedSet, pTargetState);
        break;

      case RESET:
        resetReachedSet(pReachedSet, pInitialLocation);
        break;

      default:
        throw new AssertionError(resetReachedSetStrategy);
    }
  }

  /** Removes <code>pTargetState</code> from reached set and ARG. */
  private void removeTargetState(ReachedSet pReachedSet, ARGState pTargetState) {
    assert pTargetState.isTarget();
    pReachedSet.remove(pTargetState);
    pTargetState.removeFromARG();
  }

  private void removeLoop(ReachedSet pReachedSet, ARGState pTargetState)
      throws InterruptedException {
    Deque<ARGState> workList = new ArrayDeque<>();
    workList.add(pTargetState);
    Set<ARGState> seen = new HashSet<>();
    List<ARGState> firstLoopStates = new ArrayList<>();

    // get all loop states having only stem predecessors
    while (!workList.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      ARGState next = workList.poll();
      if (!seen.add(next)) {
        continue; // already seen
      }

      Collection<ARGState> parentLoopStates =
          next.getParents().stream()
              .filter(p -> extractStateByType(p, TerminationState.class).isPartOfLoop())
              .collect(ImmutableList.toImmutableList());

      if (parentLoopStates.isEmpty()) {
        firstLoopStates.add(next);
      } else {
        workList.addAll(parentLoopStates);
      }
    }

    ARGReachedSet argReachedSet = new ARGReachedSet(pReachedSet);
    for (ARGState state : firstLoopStates) {
      argReachedSet.removeSubtree(state);
    }
  }

  private void resetReachedSet(ReachedSet pReachedSet, CFANode pInitialLocation)
      throws InterruptedException {
    AbstractState initialState = safetyCPA.getInitialState(pInitialLocation, getDefaultPartition());
    Precision initialPrecision =
        safetyCPA.getInitialPrecision(pInitialLocation, getDefaultPartition());
    pReachedSet.clear();
    pReachedSet.add(initialState, initialPrecision);
  }

  private void setExplicitAbstractionNodes(final Loop pLoop) {
    CFANode firstLoopHead = pLoop.getLoopHeads().iterator().next();
    if (firstLoopHead instanceof FunctionEntryNode) {
      setExplicitAbstractionNodes(ImmutableSet.of(firstLoopHead));
    }
  }

  private void setExplicitAbstractionNodes(final ImmutableSet<CFANode> newAbsLocs) {
    @SuppressWarnings("resource")
    PredicateCPA predCPA = CPAs.retrieveCPA(safetyCPA, PredicateCPA.class);
    if (predCPA != null) {
      predCPA.changeExplicitAbstractionNodes(newAbsLocs);
    }
  }

  private static class DeclarationCollectionCFAVisitor extends DefaultCFAVisitor {

    private final Set<CVariableDeclaration> globalDeclarations = new LinkedHashSet<>();

    private final Multimap<String, CVariableDeclaration> localDeclarations =
        MultimapBuilder.hashKeys().linkedHashSetValues().build();

    @Override
    public TraversalProcess visitNode(CFANode pNode) {

      if (pNode instanceof CFunctionEntryNode) {
        String functionName = pNode.getFunctionName();
        List<CParameterDeclaration> parameters =
            ((CFunctionEntryNode) pNode).getFunctionParameters();
        parameters.stream()
            .map(CParameterDeclaration::asVariableDeclaration)
            .forEach(localDeclarations.get(functionName)::add);
      }
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge pEdge) {

      if (pEdge instanceof CDeclarationEdge) {
        CDeclaration declaration = ((CDeclarationEdge) pEdge).getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          CVariableDeclaration variableDeclaration = (CVariableDeclaration) declaration;

          if (variableDeclaration.isGlobal()) {
            globalDeclarations.add(variableDeclaration);

          } else {
            String functionName = pEdge.getPredecessor().getFunctionName();
            localDeclarations.put(functionName, variableDeclaration);
          }
        }
      }
      return TraversalProcess.CONTINUE;
    }
  }
}
