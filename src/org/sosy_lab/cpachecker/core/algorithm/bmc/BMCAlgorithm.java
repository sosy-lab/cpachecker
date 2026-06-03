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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariantCombination;
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
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.error.DummyErrorState;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
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
  private final Set<CandidateInvariant> directlyConfirmedNonTerminationCandidates = new HashSet<>();
  private final Map<CandidateInvariant, NonTerminationLoopScope> nonTerminationLoopScopes =
      new HashMap<>();
  private ImmutableSet<CandidateInvariant> lastModelEqualityStrengthenings = ImmutableSet.of();
  private ImmutableSet<CandidateInvariant> lastBranchConditionStrengthenings = ImmutableSet.of();
  private static final int MAX_MODEL_EQUALITY_STRENGTHENINGS = 8;
  private static final int MAX_BRANCH_CONDITION_STRENGTHENINGS = 8;

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
    directlyConfirmedNonTerminationCandidates.clear();
    nonTerminationLoopScopes.clear();
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
    boolean directlyNonTerminatingNoExitLoop =
        !pNegated && isDirectlyNonTerminatingNoExitLoop(pLoop);
    Set<CFANode> loopHeadsWithContinuationCandidate = new HashSet<>();
    addLoopHeadCandidates(pLoop, candidates, pNegated, loopHeadsWithContinuationCandidate);
    addInternalExitGuardCandidates(pLoop, candidates, pNegated);
    if (!pNegated) {
      addNoExitLoopHeadCandidates(pLoop, candidates);
      addLoopExitViolationCandidates(pLoop, candidates, loopHeadsWithContinuationCandidate);
    }
    ImmutableSet<CandidateInvariant> loopCandidates = candidates.build();
    if (loopCandidates.isEmpty()) {
      return loopCandidates;
    }
    CandidateInvariant loopCandidate =
        pNegated
            ? new StatewiseCandidateInvariantDisjunction(loopCandidates)
            : new StatewiseCandidateInvariantConjunction(loopCandidates);
    if (!pNegated) {
      nonTerminationLoopScopes.put(loopCandidate, NonTerminationLoopScope.of(pLoop));
      if (directlyNonTerminatingNoExitLoop) {
        directlyConfirmedNonTerminationCandidates.add(loopCandidate);
      }
    }
    return ImmutableSet.of(loopCandidate);
  }

  private void addLoopHeadCandidates(
      Loop pLoop,
      ImmutableSet.Builder<CandidateInvariant> pCandidates,
      boolean pNegated,
      Set<CFANode> pNodesWithContinuationCandidate) {
    for (CFANode loopHead : pLoop.getLoopHeads()) {
      addSingleLocationContinuationCandidatesAtNode(
          pLoop, loopHead, pCandidates, pNegated, pNodesWithContinuationCandidate);
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

  private void addSingleLocationContinuationCandidatesAtNode(
      Loop pLoop,
      CFANode pNode,
      ImmutableSet.Builder<CandidateInvariant> pCandidates,
      boolean pNegated,
      Set<CFANode> pNodesWithContinuationCandidate) {
    for (CFAEdge leavingEdge : pNode.getLeavingEdges()) {
      if (leavingEdge instanceof AssumeEdge assumeEdge
          && pLoop.getLoopNodes().contains(assumeEdge.getSuccessor())) {
        pCandidates.add(
            pNegated
                ? new FrontierEdgeFormulaNegation(pNode, assumeEdge)
                : new EdgeFormula(pNode, assumeEdge));
        pNodesWithContinuationCandidate.add(pNode);
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
      Loop pLoop,
      ImmutableSet.Builder<CandidateInvariant> pCandidates,
      Set<CFANode> pLoopHeadsWithContinuationCandidate) {
    for (CFAEdge outgoingEdge : pLoop.getOutgoingEdges()) {
      // Skip false@exit_succ only for AssumeEdge exits leaving a loop head whose canonical
      // continuation guard is already in the candidate: SSA then makes that exit branch
      // infeasible from any C-predecessor, so the successor-only false assertion is redundant.
      // We deliberately do not extend this to internal exit guards (if/break/continue/goto), as
      // multi-loop / goto topologies can make false@exit_succ load-bearing in ways that the
      // local continuation guard does not cover.
      if (outgoingEdge instanceof AssumeEdge
          && pLoopHeadsWithContinuationCandidate.contains(outgoingEdge.getPredecessor())) {
        continue;
      }
      pCandidates.add(
          SingleLocationFormulaInvariant.makeBooleanInvariant(outgoingEdge.getSuccessor(), false));
    }
  }

  private void addNoExitLoopHeadCandidates(
      Loop pLoop, ImmutableSet.Builder<CandidateInvariant> pCandidates) {
    if (!isNoExitLoopCandidate(pLoop)) {
      return;
    }
    for (CFANode loopHead : pLoop.getLoopHeads()) {
      pCandidates.add(SingleLocationFormulaInvariant.makeBooleanInvariant(loopHead, true));
    }
  }

  private boolean isDirectlyNonTerminatingNoExitLoop(Loop pLoop) {
    return pLoop.getOutgoingEdges().isEmpty() && isPureMainFunctionNoExitLoop(pLoop);
  }

  private boolean isNoExitLoopCandidate(Loop pLoop) {
    return pLoop.getOutgoingEdges().isEmpty()
        && hasNoCandidateBlockingTerminationInMainFunctionLoop(pLoop);
  }

  private boolean hasNoCandidateBlockingTerminationInMainFunctionLoop(Loop pLoop) {
    String mainFunctionName = cfa.getMainFunction().getFunctionName();
    for (CFANode loopHead : pLoop.getLoopHeads()) {
      if (!mainFunctionName.equals(loopHead.getFunctionName())) {
        return false;
      }
    }

    for (CFANode loopNode : pLoop.getLoopNodes()) {
      if (!mainFunctionName.equals(loopNode.getFunctionName())) {
        continue;
      }
      for (CFAEdge leavingEdge : loopNode.getLeavingEdges()) {
        if (!pLoop.getLoopNodes().contains(leavingEdge.getSuccessor())
            && !isVerifierAssertionCall(leavingEdge)) {
          return false;
        }
        if (mayTerminateWithoutLoopExitCandidate(leavingEdge)) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean isPureMainFunctionNoExitLoop(Loop pLoop) {
    String mainFunctionName = cfa.getMainFunction().getFunctionName();
    for (CFANode loopHead : pLoop.getLoopHeads()) {
      if (!mainFunctionName.equals(loopHead.getFunctionName())) {
        return false;
      }
    }

    for (CFANode loopNode : pLoop.getLoopNodes()) {
      if (!mainFunctionName.equals(loopNode.getFunctionName())) {
        return false;
      }
      for (CFAEdge leavingEdge : loopNode.getLeavingEdges()) {
        if (!pLoop.getLoopNodes().contains(leavingEdge.getSuccessor())) {
          return false;
        }
        if (mayTerminateWithoutLoopExit(leavingEdge)) {
          return false;
        }
        if (!(leavingEdge instanceof AssumeEdge)
            && leavingEdge.getEdgeType() != CFAEdgeType.BlankEdge) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean mayTerminateWithoutLoopExit(CFAEdge pEdge) {
    if (pEdge instanceof FunctionCallEdge) {
      return true;
    }
    if (pEdge.getEdgeType() == CFAEdgeType.ReturnStatementEdge) {
      return true;
    }

    String rawStatement = pEdge.getRawStatement().toLowerCase(Locale.ROOT);
    return rawStatement.contains("abort(")
        || rawStatement.contains("exit(")
        || rawStatement.contains("__assert_fail")
        || rawStatement.contains("__verifier_error")
        || rawStatement.contains("reach_error");
  }

  private boolean mayTerminateWithoutLoopExitCandidate(CFAEdge pEdge) {
    if (pEdge instanceof FunctionCallEdge) {
      return !isVerifierAssertionCall(pEdge);
    }
    if (pEdge.getEdgeType() == CFAEdgeType.ReturnStatementEdge) {
      return true;
    }

    String rawStatement = pEdge.getRawStatement().toLowerCase(Locale.ROOT);
    return rawStatement.contains("abort(")
        || rawStatement.contains("exit(")
        || rawStatement.contains("__assert_fail")
        || rawStatement.contains("__verifier_error")
        || rawStatement.contains("reach_error");
  }

  private boolean isVerifierAssertionCall(CFAEdge pEdge) {
    if (!(pEdge instanceof FunctionCallEdge functionCallEdge)) {
      return false;
    }
    String functionName =
        functionCallEdge.getFunctionCallExpression().getFunctionNameExpression().toASTString();
    return functionName.equals("__VERIFIER_assert");
  }

  private boolean checkNonTerminationBaseCase(
      ReachedSet pReachedSet,
      BasicProverEnvironment<?> pProver,
      CandidateInvariant pCandidateInvariant)
      throws CPATransferException, InterruptedException, SolverException {
    lastModelEqualityStrengthenings = ImmutableSet.of();
    lastBranchConditionStrengthenings = ImmutableSet.of();
    BooleanFormula baseCase = createNonTerminationBaseCaseFormula(pReachedSet, pCandidateInvariant);
    stats.satCheck.start();
    try {
      pProver.push(baseCase);
      boolean reachable = !pProver.isUnsat();
      if (reachable) {
        lastBranchConditionStrengthenings =
            createBranchConditionStrengthenings(pReachedSet, pCandidateInvariant);
        lastModelEqualityStrengthenings =
            createModelEqualityStrengthenings(
                pReachedSet, pCandidateInvariant, pProver.getModelAssignments());
      }
      return reachable;
    } finally {
      stats.satCheck.stop();
      pProver.pop();
    }
  }

  @Override
  protected Iterable<CandidateInvariant> getAdditionalCandidatesAfterSuccessfulBaseCase(
      ReachedSet pReachedSet, CandidateInvariant pCandidateInvariant) {
    return ImmutableSet.<CandidateInvariant>builder()
        .addAll(lastBranchConditionStrengthenings)
        .addAll(lastModelEqualityStrengthenings)
        .build();
  }

  @Override
  protected boolean isDirectlyConfirmedNonTerminationCandidate(
      CandidateInvariant pCandidateInvariant) {
    return directlyConfirmedNonTerminationCandidates.contains(pCandidateInvariant);
  }

  @Override
  protected Optional<NonTerminationLoopScope> getNonTerminationLoopScope(
      CandidateInvariant pCandidateInvariant) {
    return Optional.ofNullable(nonTerminationLoopScopes.get(pCandidateInvariant));
  }

  @Override
  protected void registerNonTerminationRefinement(
      CandidateInvariant pBaseCandidate, CandidateInvariant pRefinement) {
    NonTerminationLoopScope loopScope = nonTerminationLoopScopes.get(pBaseCandidate);
    if (loopScope != null) {
      nonTerminationLoopScopes.put(pRefinement, loopScope);
    }
  }

  private ImmutableSet<CandidateInvariant> createBranchConditionStrengthenings(
      ReachedSet pReachedSet, CandidateInvariant pCandidateInvariant) {
    Optional<NonTerminationLoopScope> loopScope = getNonTerminationLoopScope(pCandidateInvariant);
    if (loopScope.isEmpty()) {
      return ImmutableSet.of();
    }

    ImmutableSet<CandidateInvariant> baseParts =
        ImmutableSet.copyOf(CandidateInvariantCombination.getConjunctiveParts(pCandidateInvariant));
    ImmutableSet.Builder<CandidateInvariant> strengthenedCandidates = ImmutableSet.builder();
    int added = 0;
    NonTerminationLoopScope scope = loopScope.orElseThrow();

    for (CFANode loopNode : scope.loopNodes()) {
      if (scope.loopHeads().contains(loopNode)) {
        continue;
      }
      for (CFAEdge leavingEdge : loopNode.getLeavingEdges()) {
        if (!(leavingEdge instanceof AssumeEdge assumeEdge)
            || !scope.loopNodes().contains(assumeEdge.getSuccessor())
            || branchLeavesLoop(scope.loop(), assumeEdge)) {
          continue;
        }

        // Skip strengthenings whose assume branch is never explored in the unrolled reached
        // set: if no state was reached at the branch's successor, the branch is
        // program-unreachable (typically detected by ValueAnalysisCPA on constants like
        // `debug = 0; ... if (debug != 0) {...}`). Adding such a candidate would let
        // PredicateCPA's abstraction smuggle a vacuous "non-term proof" through symbolic
        // closure under a program-unreachable precondition.
        if (AbstractStates.filterLocations(
                pReachedSet, ImmutableSet.of(assumeEdge.getSuccessor()))
            .isEmpty()) {
          continue;
        }

        CandidateInvariant branchCandidate = new EdgeFormula(loopNode, assumeEdge);
        if (baseParts.contains(branchCandidate)) {
          continue;
        }
        ImmutableList.Builder<CandidateInvariant> refinedParts = ImmutableList.builder();
        refinedParts.addAll(baseParts);
        refinedParts.add(branchCandidate);
        addStrengthenedCandidate(
            strengthenedCandidates,
            pCandidateInvariant,
            new StatewiseCandidateInvariantConjunction(refinedParts.build()));
        added++;
        if (added >= MAX_BRANCH_CONDITION_STRENGTHENINGS) {
          return strengthenedCandidates.build();
        }
      }
    }
    return strengthenedCandidates.build();
  }

  private ImmutableSet<CandidateInvariant> createModelEqualityStrengthenings(
      ReachedSet pReachedSet,
      CandidateInvariant pCandidateInvariant,
      Iterable<ValueAssignment> pModelAssignments)
      throws CPATransferException, InterruptedException {
    if (!(pCandidateInvariant instanceof StatewiseCandidateInvariantConjunction)) {
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<CandidateInvariant> strengthenedCandidates = ImmutableSet.builder();
    int currentK =
        CPAs.retrieveCPA(analysisCpa, LoopIterationBounding.class).getMaxLoopIterations();
    Optional<NonTerminationLoopScope> loopScope = getNonTerminationLoopScope(pCandidateInvariant);
    List<ValueAssignment> modelAssignments =
        prioritizeModelAssignments(pCandidateInvariant, pModelAssignments);

    for (AbstractState stopState :
        getNonTerminationBaseCaseStates(pReachedSet, currentK, loopScope)) {
      if ((loopScope.isEmpty() && !isStopState(stopState))
          || !isRelevantForReachability(stopState)
          || !candidateAppliesToState(pCandidateInvariant, stopState)) {
        continue;
      }

      for (AbstractState state : getPathStatesTo(stopState)) {
        if (!candidateAppliesToState(pCandidateInvariant, state)) {
          continue;
        }
        PredicateAbstractState predicateState =
            AbstractStates.extractStateByType(state, PredicateAbstractState.class);
        if (predicateState == null) {
          continue;
        }

        for (CFANode location : AbstractStates.extractLocations(state)) {
          if (!pCandidateInvariant.appliesTo(location)) {
            continue;
          }
          addModelEqualityStrengtheningsAtLocation(
              strengthenedCandidates,
              pCandidateInvariant,
              predicateState.getPathFormula(),
              location,
              modelAssignments);
          if (strengthenedCandidates.build().size() >= MAX_MODEL_EQUALITY_STRENGTHENINGS) {
            return strengthenedCandidates.build();
          }
        }
      }
    }
    return strengthenedCandidates.build();
  }

  private List<ValueAssignment> prioritizeModelAssignments(
      CandidateInvariant pCandidateInvariant, Iterable<ValueAssignment> pModelAssignments) {
    String candidateText = pCandidateInvariant.toString();
    List<ValueAssignment> prioritized = new ArrayList<>();
    List<ValueAssignment> fallback = new ArrayList<>();
    for (ValueAssignment valueAssignment : pModelAssignments) {
      if (valueAssignment.isFunction() || !isSupportedModelEqualityValue(valueAssignment)) {
        continue;
      }
      String actualName = FormulaManagerView.parseName(valueAssignment.getName()).getFirst();
      if (candidateText.contains(actualName)) {
        prioritized.add(valueAssignment);
      } else {
        fallback.add(valueAssignment);
      }
    }
    prioritized.addAll(fallback);
    return prioritized;
  }

  private void addModelEqualityStrengtheningsAtLocation(
      ImmutableSet.Builder<CandidateInvariant> pStrengthenedCandidates,
      CandidateInvariant pCandidateInvariant,
      PathFormula pPathFormula,
      CFANode pLocation,
      Iterable<ValueAssignment> pModelAssignments) {
    Optional<Set<String>> modifiedVariables = getModifiedVariablesInLoopsContaining(pLocation);
    if (modifiedVariables.isEmpty()) {
      return;
    }

    int added = 0;
    List<CandidateInvariant> cumulativeEqualities = new ArrayList<>();
    for (ValueAssignment valueAssignment : pModelAssignments) {
      Pair<String, OptionalInt> parsedName =
          FormulaManagerView.parseName(valueAssignment.getName());
      String actualName = parsedName.getFirst();
      OptionalInt index = parsedName.getSecond();
      boolean modifiedInLoop = modifiedVariables.orElseThrow().contains(actualName);
      boolean onlyNoOpModifiedInLoop =
          modifiedInLoop && isOnlyNoOpModifiedInLoopsContaining(pLocation, actualName);
      // Filter steps:
      //  c1/c2/c3: assignment must be SSA-indexed and the index must match the variable's
      //            current SSA index at pLocation.
      //  c4: if the variable is modified in the loop body, the only safe case is when every
      //      assignment in the loop is a no-op (e.g. `x = x + 0`); otherwise the model's
      //      value is iteration-specific and not invariant.
      // Variables that are *never* assigned inside the loop are loop-invariant by definition
      // - their model value carries from the prologue (e.g. `c = 0` before `while (x >= 0)`),
      // which is exactly the class of facts a non-termination closure proof needs. We must
      // NOT additionally require a "repeated stable value across SSA indices", because such
      // variables only have a single SSA index and would always fail that check.
      if (index.isEmpty()
          || !pPathFormula.getSsa().containsVariable(actualName)
          || pPathFormula.getSsa().getIndex(actualName) != index.orElseThrow()
          || (modifiedInLoop && !onlyNoOpModifiedInLoop)) {
        continue;
      }

      BooleanFormula equality =
          getFormulaManager().uninstantiate(valueAssignment.getAssignmentAsFormula());
      CandidateInvariant equalityCandidate =
          SingleLocationFormulaInvariant.makeLocationInvariant(
              pLocation, equality, getFormulaManager());
      cumulativeEqualities.add(equalityCandidate);

      if (added < MAX_MODEL_EQUALITY_STRENGTHENINGS) {
        addStrengthenedCandidate(
            pStrengthenedCandidates,
            pCandidateInvariant,
            CandidateInvariantCombination.conjunction(
                ImmutableList.of(pCandidateInvariant, equalityCandidate)));
        added++;
      }

      if (cumulativeEqualities.size() > 1 && added < MAX_MODEL_EQUALITY_STRENGTHENINGS) {
        ImmutableList.Builder<CandidateInvariant> cumulativeCandidate = ImmutableList.builder();
        cumulativeCandidate.add(pCandidateInvariant);
        cumulativeCandidate.addAll(cumulativeEqualities);
        addStrengthenedCandidate(
            pStrengthenedCandidates,
            pCandidateInvariant,
            CandidateInvariantCombination.conjunction(cumulativeCandidate.build()));
        added++;
      }
      if (added >= MAX_MODEL_EQUALITY_STRENGTHENINGS) {
        return;
      }
    }
  }

  private void addStrengthenedCandidate(
      ImmutableSet.Builder<CandidateInvariant> pStrengthenedCandidates,
      CandidateInvariant pBaseCandidate,
      CandidateInvariant pStrengthenedCandidate) {
    pStrengthenedCandidates.add(pStrengthenedCandidate);
    NonTerminationLoopScope loopScope = nonTerminationLoopScopes.get(pBaseCandidate);
    if (loopScope != null) {
      nonTerminationLoopScopes.put(pStrengthenedCandidate, loopScope);
    }
  }

  private boolean isOnlyNoOpModifiedInLoopsContaining(CFANode pLocation, String pVariableName) {
    if (cfa.getLoopStructure().isEmpty()) {
      return false;
    }

    boolean foundAssignment = false;
    for (Loop loop : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
      if (!loop.getLoopNodes().contains(pLocation)) {
        continue;
      }
      for (CFANode loopNode : loop.getLoopNodes()) {
        for (CFAEdge leavingEdge : loopNode.getLeavingEdges()) {
          if (!(leavingEdge instanceof CStatementEdge statementEdge)
              || !(statementEdge.getStatement() instanceof CAssignment assignment)
              || !assignsVariable(assignment, pVariableName)) {
            continue;
          }
          foundAssignment = true;
          if (!isNoOpAssignment(assignment, pVariableName)) {
            return false;
          }
        }
      }
    }
    return foundAssignment;
  }

  private boolean assignsVariable(CAssignment pAssignment, String pVariableName) {
    if (!(pAssignment.getLeftHandSide() instanceof CIdExpression idExpression)) {
      return false;
    }
    return matchesVariable(idExpression, pVariableName);
  }

  private boolean isNoOpAssignment(CAssignment pAssignment, String pVariableName) {
    if (!(pAssignment.getLeftHandSide() instanceof CIdExpression idExpression)) {
      return false;
    }
    return isNoOpExpression(pAssignment.getRightHandSide(), idExpression, pVariableName);
  }

  private boolean isNoOpExpression(
      CRightHandSide pExpression, CIdExpression pIdExpression, String pVariableName) {
    if (pExpression instanceof CIdExpression rightIdExpression) {
      return matchesSameVariable(rightIdExpression, pIdExpression, pVariableName);
    }
    if (pExpression instanceof CBinaryExpression binaryExpression) {
      BinaryOperator operator = binaryExpression.getOperator();
      CExpression operand1 = binaryExpression.getOperand1();
      CExpression operand2 = binaryExpression.getOperand2();
      if (operator == BinaryOperator.PLUS) {
        return (matchesSameVariable(operand1, pIdExpression, pVariableName)
                && isZeroLiteral(operand2))
            || (isZeroLiteral(operand1)
                && matchesSameVariable(operand2, pIdExpression, pVariableName));
      }
      if (operator == BinaryOperator.MINUS) {
        return matchesSameVariable(operand1, pIdExpression, pVariableName)
            && isZeroLiteral(operand2);
      }
    }
    return false;
  }

  private boolean matchesSameVariable(
      CExpression pExpression, CIdExpression pIdExpression, String pVariableName) {
    return pExpression instanceof CIdExpression rightIdExpression
        && matchesVariable(rightIdExpression, pIdExpression.getName())
        && matchesVariable(rightIdExpression, pVariableName);
  }

  private boolean matchesVariable(CIdExpression pIdExpression, String pVariableName) {
    return pIdExpression.getName().equals(pVariableName)
        || (pIdExpression.getDeclaration() != null
            && pIdExpression.getDeclaration().getQualifiedName().equals(pVariableName));
  }

  private boolean isZeroLiteral(CExpression pExpression) {
    return pExpression instanceof CIntegerLiteralExpression integerLiteralExpression
        && integerLiteralExpression.getValue().signum() == 0;
  }

  private Optional<Set<String>> getModifiedVariablesInLoopsContaining(CFANode pLocation) {
    if (cfa.getLoopStructure().isEmpty()) {
      return Optional.of(ImmutableSet.of());
    }

    Set<String> modifiedVariables = new HashSet<>();
    for (Loop loop : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
      if (!loop.getLoopNodes().contains(pLocation)) {
        continue;
      }

      for (CFANode loopNode : loop.getLoopNodes()) {
        for (CFAEdge leavingEdge : loopNode.getLeavingEdges()) {
          if (!collectModifiedVariables(leavingEdge, modifiedVariables)) {
            return Optional.empty();
          }
        }
      }
    }
    return Optional.of(modifiedVariables);
  }

  private boolean collectModifiedVariables(CFAEdge pEdge, Set<String> pModifiedVariables) {
    if (pEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge
        || pEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge
        || pEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      return false;
    }

    if (pEdge instanceof CStatementEdge statementEdge) {
      if (statementEdge.getStatement() instanceof CAssignment assignment) {
        return addModifiedVariable(assignment.getLeftHandSide(), pModifiedVariables);
      }
      if (statementEdge.getStatement() instanceof CFunctionCall) {
        return false;
      }
    }
    return true;
  }

  private boolean addModifiedVariable(CLeftHandSide pLeftHandSide, Set<String> pModifiedVariables) {
    if (!(pLeftHandSide instanceof CIdExpression idExpression)) {
      return false;
    }
    pModifiedVariables.add(idExpression.getName());
    if (idExpression.getDeclaration() != null) {
      pModifiedVariables.add(idExpression.getDeclaration().getQualifiedName());
    }
    return true;
  }

  private boolean isSupportedModelEqualityValue(ValueAssignment pValueAssignment) {
    Object value = pValueAssignment.getValue();
    return value instanceof Number || value instanceof Boolean;
  }

  private BooleanFormula createNonTerminationBaseCaseFormula(
      Iterable<AbstractState> pReachedSet, CandidateInvariant pCandidateInvariant)
      throws CPATransferException, InterruptedException {
    // initially set to false, so that loops without applicable states yield a trivially unsat
    // formula
    BooleanFormulaManagerView bfmgr = getBooleanFormulaManager();
    BooleanFormula result = bfmgr.makeFalse();
    // get current k to only consider states from the first k iterations
    int currentK =
        CPAs.retrieveCPA(analysisCpa, LoopIterationBounding.class).getMaxLoopIterations();
    Optional<NonTerminationLoopScope> loopScope = getNonTerminationLoopScope(pCandidateInvariant);

    for (AbstractState state : getNonTerminationBaseCaseStates(pReachedSet, currentK, loopScope)) {
      if ((loopScope.isEmpty() && !isStopState(state))
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

  private Iterable<AbstractState> getNonTerminationBaseCaseStates(
      Iterable<AbstractState> pReachedSet,
      int pCurrentK,
      Optional<NonTerminationLoopScope> pLoopScope) {
    if (pLoopScope.isPresent()) {
      NonTerminationLoopScope loopScope = pLoopScope.orElseThrow();
      return BMCHelper.filterIterationsUpTo(
          AbstractStates.filterLocations(pReachedSet, loopScope.loopNodes()),
          pCurrentK,
          loopScope.loopHeads());
    }
    // almost useless, because the generated candidates in our approach always have a loop scope
    return BMCHelper.filterIteration(pReachedSet, pCurrentK, cfa.getAllLoopHeads().orElseThrow());
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
