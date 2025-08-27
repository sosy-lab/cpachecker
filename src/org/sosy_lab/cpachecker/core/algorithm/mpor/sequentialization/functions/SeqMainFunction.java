// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqInitializerBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqForExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqSingleControlExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqWhileExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.nondet_simulations.NondeterministicSimulationUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.declaration.SeqBitVectorDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.declaration.SeqBitVectorDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_synchronization.ThreadSynchronizationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqMainFunction extends SeqFunction {

  private final MPOROptions options;

  private final MPORSubstitution mainSubstitution;

  private final int numThreads;

  private final CIdExpression numThreadsVariable;

  private final ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses;

  private final Optional<MemoryModel> memoryModel;

  // TODO make Optional
  private final CFunctionCallAssignmentStatement nextThreadAssignment;

  // TODO make Optional (and also shouldn't be a list)
  private final ImmutableList<CFunctionCallStatement> nextThreadAssumptions;

  private final Optional<CFunctionCallStatement> nextThreadActiveAssumption;

  private final Optional<CFunctionCallStatement> countAssumption;

  private final Optional<BitVectorVariables> bitVectorVariables;

  private final ProgramCounterVariables pcVariables;

  private final ThreadSynchronizationVariables threadSimulationVariables;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final LogManager logger;

  public SeqMainFunction(
      MPOROptions pOptions,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      Optional<BitVectorVariables> pBitVectorVariables,
      ProgramCounterVariables pPcVariables,
      Optional<MemoryModel> pMemoryModel,
      ThreadSynchronizationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    options = pOptions;
    mainSubstitution = SubstituteUtil.extractMainThreadSubstitution(pSubstitutions);
    numThreads = pSubstitutions.size();
    numThreadsVariable = SeqExpressionBuilder.buildNumThreadsIdExpression(numThreads);
    clauses = pClauses;
    bitVectorVariables = pBitVectorVariables;
    pcVariables = pPcVariables;
    memoryModel = pMemoryModel;
    threadSimulationVariables = pThreadSimulationVariables;

    binaryExpressionBuilder = pBinaryExpressionBuilder;
    logger = pLogger;

    nextThreadAssignment =
        SeqStatementBuilder.buildNextThreadAssignment(pOptions.nondeterminismSigned);
    nextThreadAssumptions =
        SeqAssumptionBuilder.buildNextThreadAssumption(
            pOptions.nondeterminismSigned, numThreadsVariable, binaryExpressionBuilder);
    nextThreadActiveAssumption =
        SeqAssumptionBuilder.buildNextThreadActiveAssumption(options, binaryExpressionBuilder);

    countAssumption =
        SeqAssumptionBuilder.buildCountGreaterZeroAssumption(options, binaryExpressionBuilder);
  }

  @Override
  public ImmutableList<LineOfCode> buildBody() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> rBody = ImmutableList.builder();

    // declare main() local variables NUM_THREADS, pc, next_thread
    // TODO its probably best to remove num threads entirely and just place the int
    rBody.addAll(
        buildThreadSimulationVariableDeclarations(
            options,
            clauses.keySet(),
            pcVariables,
            bitVectorVariables,
            numThreads,
            clauses,
            memoryModel,
            numThreadsVariable.getDeclaration(),
            threadSimulationVariables));

    // add main function argument non-deterministic assignments
    rBody.addAll(buildMainFunctionArgNondetAssignments(mainSubstitution, clauses, logger));

    // --- loop starts here ---
    SeqSingleControlExpression loopHead = buildLoopHead(options, binaryExpressionBuilder);
    rBody.add(LineOfCode.of(SeqStringUtil.appendCurlyBracketRight(loopHead.toASTString())));

    if (options.conflictReduction) {
      // add last_thread = next_thread assignment (before setting next_thread)
      if (options.nondeterminismSource.isNextThreadNondeterministic()) {
        CExpressionAssignmentStatement assignment =
            SeqStatementBuilder.buildLastThreadAssignment(SeqIdExpression.NEXT_THREAD);
        rBody.add(LineOfCode.of(assignment.toASTString()));
      }
    }

    // add if next_thread is a non-determinism source
    if (options.nondeterminismSource.isNextThreadNondeterministic()) {
      if (options.comments) {
        rBody.add(LineOfCode.of(SeqComment.NEXT_THREAD_NONDET));
      }
      rBody.add(LineOfCode.of(nextThreadAssignment.toASTString()));
      for (CFunctionCallStatement nextThreadAssumption : nextThreadAssumptions) {
        rBody.add(LineOfCode.of(nextThreadAssumption.toASTString()));
      }
      // assumptions over next_thread being active (pc != -1)
      if (nextThreadActiveAssumption.isPresent()) {
        if (options.comments) {
          rBody.add(LineOfCode.of(SeqComment.NEXT_THREAD_ACTIVE));
        }
        CFunctionCallStatement assumption = nextThreadActiveAssumption.orElseThrow();
        rBody.addAll(LineOfCodeUtil.buildLinesOfCodeFromCAstNodes(assumption.toASTString()));
      }
    } else {
      if (options.comments) {
        rBody.add(LineOfCode.of(SeqComment.ACTIVE_THREAD_COUNT));
      }
      rBody.add(LineOfCode.of(countAssumption.orElseThrow().toASTString()));
    }

    // add all thread simulation control flow statements
    if (options.comments) {
      rBody.add(LineOfCode.of(SeqComment.THREAD_SIMULATION_CONTROL_FLOW));
    }
    rBody.addAll(
        NondeterministicSimulationUtil.buildThreadSimulationsByNondeterminismSource(
            options, bitVectorVariables, pcVariables, clauses, binaryExpressionBuilder));
    rBody.add(LineOfCode.of(SeqSyntax.CURLY_BRACKET_RIGHT));
    // --- loop ends here ---

    if (options.sequentializationErrors) {
      // end of main function, only reachable if thread simulation finished incorrectly -> error
      rBody.add(LineOfCode.of(Sequentialization.outputReachErrorDummy));
    }
    return rBody.build();
  }

  @Override
  public CType getReturnType() {
    return SeqSimpleType.INT;
  }

  @Override
  public CIdExpression getFunctionName() {
    return SeqIdExpression.MAIN;
  }

  @Override
  public ImmutableList<CParameterDeclaration> getParameters() {
    return ImmutableList.of();
  }

  /**
   * Adds the non-deterministic initializations of {@code main} function arguments, e.g. {@code arg
   * = __VERIFIER_nondet_int;}
   */
  private ImmutableList<LineOfCode> buildMainFunctionArgNondetAssignments(
      MPORSubstitution pMainSubstitution,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      LogManager pLogger) {

    // first extract all accesses to main function arguments
    ImmutableSet<SubstituteEdge> allSubstituteEdges =
        SeqThreadStatementClauseUtil.collectAllSubstituteEdges(pClauses);
    ImmutableSet<CParameterDeclaration> accessedMainFunctionArgs =
        SubstituteUtil.findAllMainFunctionArgs(allSubstituteEdges);

    // then add main function arg nondet assignments, if necessary
    ImmutableList.Builder<LineOfCode> rMainArgAssignments = ImmutableList.builder();
    for (var entry : pMainSubstitution.mainFunctionArgSubstitutes.entrySet()) {
      // add assignment only if necessary, i.e. if it is accessed later (nondet is expensive)
      if (accessedMainFunctionArgs.contains(entry.getKey())) {
        CIdExpression mainArgSubstitute = entry.getValue();
        CType mainArgType = mainArgSubstitute.getExpressionType();
        Optional<CFunctionCallExpression> verifierNondet =
            VerifierNondetFunctionType.buildVerifierNondetByType(mainArgType);
        if (verifierNondet.isPresent()) {
          CFunctionCallAssignmentStatement assignment =
              SeqStatementBuilder.buildFunctionCallAssignmentStatement(
                  mainArgSubstitute, verifierNondet.orElseThrow());
          rMainArgAssignments.add(LineOfCode.of(assignment.toASTString()));
        } else {
          pLogger.log(
              Level.WARNING,
              "could not find __VERIFIER_nondet function "
                  + "for the following main function argument type: "
                  + mainArgType.toASTString(""));
        }
      }
    }
    return rMainArgAssignments.build();
  }

  private static SeqSingleControlExpression buildLoopHead(
      MPOROptions pOptions, CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    if (pOptions.loopIterations == 0) {
      return new SeqWhileExpression(SeqIntegerLiteralExpression.INT_1);
    } else {
      return new SeqForExpression(
          SeqIdExpression.I, pOptions.loopIterations, pBinaryExpressionBuilder);
    }
  }

  // TODO move to LOCUtil? thats also where the global thread sim var function is
  /**
   * Returns {@link LineOfCode} for thread simulation variable declarations. These are local to the
   * {@code main} function. Variables that are used in other functions are declared beforehand as
   * global variables.
   */
  private static ImmutableList<LineOfCode> buildThreadSimulationVariableDeclarations(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pThreads,
      ProgramCounterVariables pPcVariables,
      Optional<BitVectorVariables> pBitVectorVariables,
      int pNumThreads,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      Optional<MemoryModel> pMemoryModel,
      CSimpleDeclaration pNumThreadDeclaration,
      ThreadSynchronizationVariables pThreadSimulationVariables)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rDeclarations = ImmutableList.builder();

    // NUM_THREADS
    rDeclarations.add(LineOfCode.of(pNumThreadDeclaration.toASTString()));

    // last_thread is always unsigned, we assign NUM_THREADS if the current thread terminates
    if (pOptions.conflictReduction) {
      CIntegerLiteralExpression numThreadsLiteral =
          SeqExpressionBuilder.buildIntegerLiteralExpression(pNumThreads);
      CInitializer lastThreadInitializer =
          SeqInitializerBuilder.buildInitializerExpression(numThreadsLiteral);
      CVariableDeclaration lastThreadDeclaration =
          SeqDeclarationBuilder.buildVariableDeclaration(
              false,
              SeqSimpleType.UNSIGNED_INT,
              SeqIdExpression.LAST_THREAD.getName(),
              lastThreadInitializer);
      rDeclarations.add(LineOfCode.of(lastThreadDeclaration.toASTString()));
    }

    // next_thread
    if (pOptions.nondeterminismSource.isNextThreadNondeterministic()) {
      if (pOptions.nondeterminismSigned) {
        rDeclarations.add(LineOfCode.of(SeqVariableDeclaration.NEXT_THREAD_SIGNED.toASTString()));
      } else {
        rDeclarations.add(LineOfCode.of(SeqVariableDeclaration.NEXT_THREAD_UNSIGNED.toASTString()));
      }
    }

    // pc variable(s)
    if (pOptions.comments) {
      rDeclarations.add(LineOfCode.of(SeqComment.PC_DECLARATION));
    }
    ImmutableList<CVariableDeclaration> pcDeclarations =
        SeqDeclarationBuilder.buildPcDeclarations(pOptions, pPcVariables, pNumThreads);
    for (CVariableDeclaration pcDeclaration : pcDeclarations) {
      rDeclarations.add(LineOfCode.of(pcDeclaration.toASTString()));
    }

    // if enabled: bit vectors
    if (pOptions.areBitVectorsEnabled()) {
      ImmutableList<SeqBitVectorDeclaration> bitVectorDeclarations =
          SeqBitVectorDeclarationBuilder.buildBitVectorDeclarationsByEncoding(
              pOptions, pBitVectorVariables, pMemoryModel, pClauses);
      for (SeqBitVectorDeclaration bitVectorDeclaration : bitVectorDeclarations) {
        rDeclarations.add(LineOfCode.of(bitVectorDeclaration.toASTString()));
      }
    }

    // active_thread_count / cnt
    if (!pOptions.nondeterminismSource.isNextThreadNondeterministic()) {
      rDeclarations.add(LineOfCode.of(SeqVariableDeclaration.CNT.toASTString()));
    }

    // if enabled: K and r
    if (pOptions.nondeterminismSource.isNumStatementsNondeterministic()) {
      rDeclarations.add(LineOfCode.of(SeqVariableDeclaration.R.toASTString()));
      if (pOptions.nondeterminismSource.equals(
          NondeterminismSource.NEXT_THREAD_AND_NUM_STATEMENTS)) {
        if (pOptions.nondeterminismSigned) {
          rDeclarations.add(LineOfCode.of(SeqVariableDeclaration.K_SIGNED.toASTString()));
        } else {
          rDeclarations.add(LineOfCode.of(SeqVariableDeclaration.K_UNSIGNED.toASTString()));
        }
      }
      if (pOptions.nondeterminismSource.equals(NondeterminismSource.NUM_STATEMENTS)) {
        for (MPORThread thread : pThreads) {
          rDeclarations.add(
              LineOfCode.of(thread.getKVariable().orElseThrow().getDeclaration().toASTString()));
        }
      }
    }

    // thread synchronization variables (e.g. mutex_locked)
    if (pOptions.comments) {
      rDeclarations.add(LineOfCode.of(SeqComment.THREAD_SIMULATION_VARIABLES));
    }
    for (CIdExpression threadVariable : pThreadSimulationVariables.getIdExpressions()) {
      assert threadVariable.getDeclaration() instanceof CVariableDeclaration;
      CVariableDeclaration varDeclaration = (CVariableDeclaration) threadVariable.getDeclaration();
      rDeclarations.add(LineOfCode.of(varDeclaration.toASTString()));
    }
    return rDeclarations.build();
  }
}
