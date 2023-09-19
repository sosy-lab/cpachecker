// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This class implements interpolation-based model checking algorithm by encoding a program as a
 * monolithic transition relation with a symbolic program counter.
 *
 * <p>It builds a transition relation of a program by iterating through all edges and constructing
 * the union of their path formulas. This class cannot handle programs with multiple functions.
 * Therefore, errors have to be inlined instead of calling reach_error().
 */
@Options(prefix = "imctr")
public class IMCSPCAlgorithm implements Algorithm, StatisticsProvider {

  @Option(secure = true, description = "enable interpolation-based model checking")
  private boolean interpolation = true;

  @Option(secure = true, description = "derive interpolants backward")
  private boolean backwardInterpolation = true;

  @Option(secure = true, description = "analyze the derived interpolants")
  private boolean analyzeInterpolant = false;

  @Option(
      secure = true,
      description =
          "check the union of safety properties after each transition (the original encoding in"
              + " McMillan's CAV 2003 paper)")
  private boolean checkUnionProperty = false;

  private final CFA cfa;
  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final PathFormulaManager pfmgr;
  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final IMCSPCStatistics stats;

  private static final String PROGRAM_COUNTER_NAME = "__pc";
  private static final CDeclaration PROGRAM_COUNTER_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          CNumericTypes.INT,
          PROGRAM_COUNTER_NAME,
          PROGRAM_COUNTER_NAME,
          PROGRAM_COUNTER_NAME,
          null);
  private static final CLeftHandSide PROGRAM_COUNTER =
      new CIdExpression(
          FileLocation.DUMMY, CNumericTypes.INT, PROGRAM_COUNTER_NAME, PROGRAM_COUNTER_DECLARATION);

  private static final DummyTargetState DUMMY_TARGET_STATE =
      DummyTargetState.withSimpleTargetInformation("IMC-SPC");

  public IMCSPCAlgorithm(
      CFA pCFA, Configuration pConfig, LogManager pLogger, ShutdownManager pShutdownManager)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    cfa = pCFA;
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownManager.getNotifier();
    solver = Solver.create(config, logger, shutdownNotifier);
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pfmgr =
        new PathFormulaManagerImpl(
            solver.getFormulaManager(),
            config,
            logger,
            shutdownNotifier,
            cfa,
            AnalysisDirection.FORWARD);
    binaryExpressionBuilder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);

    stats = new IMCSPCStatistics();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    try {
      AlgorithmStatus status = interpolationModelChecking();
      if (status == AlgorithmStatus.SOUND_AND_PRECISE) {
        // Clear waitlist such that the outcome is TRUE instead of UNKNOWN
        pReachedSet.clearWaitlist();
      } else if (status == AlgorithmStatus.UNSOUND_AND_PRECISE) {
        // Add a dummy target state such that the outcome is FALSE instead of UNKNOWN
        pReachedSet.add(new ARGState(DUMMY_TARGET_STATE, null), SingletonPrecision.getInstance());
      }
      return status;
    } catch (SolverException e) {
      throw new CPAException("Solver Failure " + e.getMessage(), e);
    }
  }

  /**
   * The main method for interpolation-based model checking.
   *
   * @return {@code AlgorithmStatus.UNSOUND_AND_PRECISE} if an error location is reached, i.e.,
   *     unsafe; {@code AlgorithmStatus.SOUND_AND_PRECISE} if a fixed point is derived, i.e., safe.
   */
  private AlgorithmStatus interpolationModelChecking()
      throws InterruptedException, CPAException, SolverException {
    List<BooleanFormula> bmcQuery = new ArrayList<>();
    int unrolledEdges = 0;

    // Initial-condition check: I && !P
    PathFormula initialCondition = buildInitialCondition();
    bmcQuery.add(initialCondition.getFormula());
    PathFormula errorCondition = buildErrorCondition(initialCondition);
    bmcQuery.add(errorCondition.getFormula());
    if (isBMCQuerySat(bmcQuery, unrolledEdges)) {
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    } else {
      removeProvedProperty(bmcQuery);
    }

    // BMC outer loop
    // Error conditions after each transition
    List<BooleanFormula> errorConditions = new ArrayList<>();
    // Set transition relation to initial condition for SSA indices
    PathFormula transitionRelation = initialCondition;
    while (true) {
      ++unrolledEdges;
      stats.numUnroll = unrolledEdges;
      transitionRelation = buildTransitionRelation(transitionRelation);
      bmcQuery.add(transitionRelation.getFormula());
      // Forward-condition check: I && T && ... && T
      if (solver.isUnsat(bfmgr.and(bmcQuery))) {
        logger.log(Level.INFO, "The program cannot be unrolled further: Property is safe");
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
      errorCondition = buildErrorCondition(transitionRelation);
      errorConditions.add(errorCondition.getFormula());
      // Assert either the union of all properties or only the last property
      // Namely, I && T && ... && T && (!P1 || !P2 || ...) or I && T && ... && T && !P
      bmcQuery.add(checkUnionProperty ? bfmgr.or(errorConditions) : errorCondition.getFormula());
      if (isBMCQuerySat(bmcQuery, unrolledEdges)) {
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      } else {
        // IMC inner loop
        if (interpolation) {
          try (InterpolatingProverEnvironment<?> itpProver =
              solver.newProverEnvironmentWithInterpolation(ProverOptions.GENERATE_MODELS)) {
            if (reachFixedPointByInterpolation(itpProver, bmcQuery, initialCondition.getSsa())) {
              return AlgorithmStatus.SOUND_AND_PRECISE;
            }
          }
        }
        removeProvedProperty(bmcQuery);
      }
    }
  }

  /**
   * Build a path formula for the initial condition __pc=l_0.
   *
   * <p>Start the SSA indices from the default value by calling {@link
   * PathFormulaManager#makeEmptyPathFormula()}
   *
   * @return a path formula __pc@1=l_0
   */
  private PathFormula buildInitialCondition() throws CPATransferException, InterruptedException {
    return pfmgr.makeAnd(
        pfmgr.makeEmptyPathFormula(), buildProgramCounterAssumeEdge(cfa.getMainFunction()));
  }

  /**
   * Build a transition relation starting from the given SSA indices.
   *
   * <p>For an edge (u,v) with operation op, call {@link PathFormulaManager#makeAnd(PathFormula,
   * CFAEdge)} to create a formula for op and {@link #buildProgramCounterAssumeEdge(CFANode)} and
   * {@link #buildProgramCounterAssignEdge(CFANode)} to create formulas for (__pc=u && __pc'=v).
   *
   * <p>Start the SSA indices from the given values by calling {@link
   * PathFormulaManager#makeEmptyPathFormulaWithContextFrom(PathFormula)}.
   *
   * <p>The transition relation of the whole CFA is the disjunction of all edges' formulas.
   *
   * @param pOldPathFormula provides the SSA indices for the variables
   * @return a path formula T(pc,x,pc',x')
   */
  private PathFormula buildTransitionRelation(final PathFormula pOldPathFormula)
      throws CPATransferException, InterruptedException {
    @SuppressWarnings("deprecation")
    PathFormula transitionRelation =
        PathFormula.createManually(
            bfmgr.makeFalse(), pOldPathFormula.getSsa(), pOldPathFormula.getPointerTargetSet(), 0);
    for (CFANode node : cfa.nodes()) {
      logger.log(Level.FINEST, node);
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        logger.log(Level.FINEST, edge);
        PathFormula edgeFormula =
            pfmgr.makeAnd(pfmgr.makeEmptyPathFormulaWithContextFrom(pOldPathFormula), edge);
        CFAEdge predEdge = buildProgramCounterAssumeEdge(edge.getPredecessor());
        edgeFormula = pfmgr.makeAnd(edgeFormula, predEdge);
        CFAEdge succEdge = buildProgramCounterAssignEdge(edge.getSuccessor());
        edgeFormula = pfmgr.makeAnd(edgeFormula, succEdge);
        logger.log(Level.ALL, "Edge formula", edgeFormula);
        transitionRelation = pfmgr.makeOr(transitionRelation, edgeFormula);
        logger.log(Level.ALL, "Current transition relation after makeOr()", transitionRelation);
      }
    }
    return transitionRelation;
  }

  /**
   * Build a path formula for the error condition __pc=l_E.
   *
   * <p>An error occurs if the program counter is at some error location. An error location is a
   * {@link CFALabelNode} node whose label is ERROR.
   *
   * @param pOldPathFormula provides the SSA indices for the variables
   * @return a path formula !P(pc,x)
   */
  private PathFormula buildErrorCondition(final PathFormula pOldPathFormula)
      throws CPATransferException, InterruptedException {
    ImmutableSet<CFANode> errorLocations =
        cfa.nodes().stream()
            .filter(
                n -> n instanceof CFALabelNode && ((CFALabelNode) n).getLabel().equals("__ERROR"))
            .collect(ImmutableSet.toImmutableSet());
    @SuppressWarnings("deprecation")
    PathFormula errorCondition =
        PathFormula.createManually(
            bfmgr.makeFalse(), pOldPathFormula.getSsa(), pOldPathFormula.getPointerTargetSet(), 0);
    for (CFANode node : errorLocations) {
      errorCondition =
          pfmgr.makeOr(
              errorCondition,
              pfmgr.makeAnd(
                  pfmgr.makeEmptyPathFormulaWithContextFrom(pOldPathFormula),
                  buildProgramCounterAssumeEdge(node)));
    }
    return errorCondition;
  }

  /**
   * Build an edge to assume the given node.
   *
   * <p>Given an edge (u,v), the transition through this edge can be described by (__pc=u &&
   * __pc'=v). The first part of the conjunction can be created by {@code
   * PathFormulaManager#makeAnd(PathFormula, CFAEdge)} if an assumption edge of [__pc==u] is
   * provided.
   *
   * <p>For the successor v, see {@link #buildProgramCounterAssignEdge(CFANode)}.
   *
   * @param pNode the predecessor node u
   * @return assumeEdge of [__pc==u]
   */
  private CFAEdge buildProgramCounterAssumeEdge(final CFANode pNode)
      throws UnrecognizedCodeException {
    String rawStatement = "[ " + PROGRAM_COUNTER_NAME + " == " + pNode.getNodeNumber() + " ]";
    CExpression assumption =
        binaryExpressionBuilder.buildBinaryExpression(
            PROGRAM_COUNTER,
            CIntegerLiteralExpression.createDummyLiteral(pNode.getNodeNumber(), CNumericTypes.INT),
            BinaryOperator.EQUALS);
    CFAEdge assumeEdge =
        new CAssumeEdge(
            rawStatement,
            FileLocation.DUMMY,
            CFANode.newDummyCFANode(),
            CFANode.newDummyCFANode(),
            assumption,
            true);
    return assumeEdge;
  }

  /**
   * Build an edge to assign the given node.
   *
   * <p>Given an edge (u,v), the transition through this edge can be described by (__pc=u &&
   * __pc'=v). The second part of the conjunction can be created by {@code
   * PathFormulaManager#makeAnd(PathFormula, CFAEdge)} if an assignment edge of __pc=v; is provided.
   *
   * <p>For the predecessor u, see {@link #buildProgramCounterAssumeEdge(CFANode)}.
   *
   * @param pNode the successor node v
   * @return assignEdge of __pc=v;
   */
  private CFAEdge buildProgramCounterAssignEdge(final CFANode pNode) {
    String rawStatement = PROGRAM_COUNTER_NAME + " = " + pNode.getNodeNumber() + ";";
    CExpressionAssignmentStatement assignment =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY,
            PROGRAM_COUNTER,
            CIntegerLiteralExpression.createDummyLiteral(pNode.getNodeNumber(), CNumericTypes.INT));
    CFAEdge assignEdge =
        new CStatementEdge(
            rawStatement,
            assignment,
            FileLocation.DUMMY,
            CFANode.newDummyCFANode(),
            CFANode.newDummyCFANode());
    return assignEdge;
  }

  /**
   * Solve a BMC query
   *
   * @param pBMC a list of formulas I, T, T, ..., T, !P
   * @return {@code true} if satisfiable; {@code false} if unsatisfiable
   */
  private boolean isBMCQuerySat(final List<BooleanFormula> pBMC, final int pUnroll)
      throws InterruptedException, SolverException {
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(bfmgr.and(pBMC));
      if (prover.isUnsat()) {
        logger.log(Level.INFO, "Program is safe up to " + pUnroll + " edges");
        return false;
      } else {
        logger.log(Level.INFO, "A Bug is found after " + pUnroll + " edges");
        analyzeCounterExample(prover);
        return true;
      }
    }
  }

  /**
   * Remove the proved property at the end of the BMC query
   *
   * @param pBMC a list of formulas I, T, T, ..., T, !P
   */
  private void removeProvedProperty(List<BooleanFormula> pBMC) {
    pBMC.remove(pBMC.size() - 1);
  }

  private void analyzeCounterExample(ProverEnvironment pProver) throws SolverException {
    try (Model model = pProver.getModel()) {
      logger.log(Level.INFO, model);
    }
  }

  /**
   * Perform the inner loop of interpolation-based model checking
   *
   * <p>It partitions the BMC query into the prefix, loop, and suffix formulas. The prefix formula
   * is I, the loop formula is T, and the suffix formula is the conjunction of the rest.
   *
   * @param pProver an interpolation environment
   * @param pBMC a list of formulas I, T, T, ..., T, !P
   * @param pPrefixSsaMap the SSA indices of the prefix formula
   * @return {@code true} if satisfiable; {@code false} if unsatisfiable
   */
  private <T> boolean reachFixedPointByInterpolation(
      InterpolatingProverEnvironment<T> pProver,
      final List<BooleanFormula> pBMC,
      final SSAMap pPrefixSsaMap)
      throws InterruptedException, SolverException {
    // The inner loop is entered with at least one unrolling, i.e., I, T, !P
    assert pBMC.size() >= 3;

    BooleanFormula prefixFormula = pBMC.get(0);
    BooleanFormula loopFormula = pBMC.get(1);
    BooleanFormula suffixFormula = bfmgr.and(pBMC.subList(2, pBMC.size()));

    List<T> formulaA = new ArrayList<>();
    List<T> formulaB = new ArrayList<>();
    formulaB.add(pProver.push(suffixFormula));
    formulaA.add(pProver.push(loopFormula));
    formulaA.add(pProver.push(prefixFormula));

    BooleanFormula currentImage = bfmgr.or(bfmgr.makeFalse(), prefixFormula);
    while (true) {
      logger.log(Level.ALL, "Current image:", currentImage);
      stats.itpQuery.start();
      if (!pProver.isUnsat()) {
        stats.itpQuery.stop();
        break;
      }
      BooleanFormula interpolant = getInterpolantFrom(pProver, formulaA, formulaB);
      stats.itpQuery.stop();
      stats.numItp = stats.numItp + 1;
      logger.log(Level.ALL, "Interpolant:", interpolant);
      interpolant = fmgr.instantiate(fmgr.uninstantiate(interpolant), pPrefixSsaMap);
      logger.log(Level.ALL, "After changing SSA indices:", interpolant);
      if (analyzeInterpolant) {
        analyzeFormula(interpolant);
      }
      if (solver.implies(interpolant, currentImage)) {
        logger.log(Level.INFO, "The current image reaches a fixed point");
        return true;
      }
      currentImage = bfmgr.or(currentImage, interpolant);
      pProver.pop();
      formulaA.remove(formulaA.size() - 1);
      formulaA.add(pProver.push(interpolant));
    }
    logger.log(
        Level.ALL,
        "Counterexample to the inner loop:",
        ImmutableList.sortedCopyOf(
            Comparator.<Model.ValueAssignment, String>comparing(n -> n.toString()),
            pProver.getModelAssignments()));
    return false;
  }

  private <T> BooleanFormula getInterpolantFrom(
      InterpolatingProverEnvironment<T> pProver, final List<T> pFormulaA, final List<T> pFormulaB)
      throws SolverException, InterruptedException {
    return backwardInterpolation
        ? bfmgr.not(pProver.getInterpolant(pFormulaB))
        : pProver.getInterpolant(pFormulaA);
  }

  private void analyzeFormula(final BooleanFormula pFormula) {
    ImmutableSet<BooleanFormula> atoms = fmgr.extractAtoms(pFormula, false);
    int numAtomsWithProgramCounter = 0;
    for (BooleanFormula atom : atoms) {
      boolean isAtomWithProgramCounter = false;
      Set<String> variableNames = fmgr.extractVariableNames(atom);
      for (String name : variableNames) {
        if (name.contains(PROGRAM_COUNTER_NAME)) {
          ++numAtomsWithProgramCounter;
          isAtomWithProgramCounter = true;
          break;
        }
      }
      if (!isAtomWithProgramCounter) {
        logger.log(Level.INFO, "  Interesting atom:", atom);
      }
    }
    if (atoms.size() != numAtomsWithProgramCounter) {
      logger.log(Level.INFO, "    #atoms with pc:", numAtomsWithProgramCounter);
      logger.log(Level.INFO, "    #atoms:", atoms.size());
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
