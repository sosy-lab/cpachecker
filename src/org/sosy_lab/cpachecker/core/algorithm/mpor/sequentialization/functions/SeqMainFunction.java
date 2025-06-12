// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.declaration.SeqBitVectorDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.declaration.SeqBitVectorDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.single.SeqSingleControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.single.SeqSingleControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.nondet_simulations.NondeterministicSimulationUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqMainFunction extends SeqFunction {

  private static final SeqSingleControlFlowStatement whileTrue =
      new SeqSingleControlFlowStatement(
          SeqIntegerLiteralExpression.INT_1, SeqControlFlowStatementType.WHILE);

  private final MPOROptions options;

  private final MPORSubstitution mainSubstitution;

  private final CIdExpression numThreadsVariable;

  /** The thread-specific clauses in the while loop. */
  private final ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> clauses;

  private final ImmutableList<SeqBitVectorDeclaration> bitVectorDeclarations;

  private final ImmutableList<CVariableDeclaration> pcDeclarations;

  // TODO make Optional
  private final CFunctionCallAssignmentStatement nextThreadAssignment;

  // TODO make Optional (and also shouldn't be a list)
  private final ImmutableList<CFunctionCallStatement> nextThreadAssumptions;

  private final Optional<CFunctionCallStatement> nextThreadActiveAssumption;

  private final Optional<CFunctionCallStatement> countAssumption;

  private final PcVariables pcVariables;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final LogManager logger;

  public SeqMainFunction(
      MPOROptions pOptions,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      Optional<BitVectorVariables> pBitVectorVariables,
      PcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    int numThreads = pSubstitutions.size();

    options = pOptions;
    mainSubstitution = SubstituteUtil.extractMainThreadSubstitution(pSubstitutions);
    numThreadsVariable =
        SeqExpressionBuilder.buildIdExpression(
            SeqDeclarationBuilder.buildVariableDeclaration(
                false,
                SeqSimpleType.CONST_INT,
                SeqToken.NUM_THREADS,
                SeqInitializerBuilder.buildInitializerExpression(
                    SeqExpressionBuilder.buildIntegerLiteralExpression(numThreads))));
    clauses = pClauses;
    pcVariables = pPcVariables;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    logger = pLogger;

    bitVectorDeclarations =
        SeqBitVectorDeclarationBuilder.buildBitVectorDeclarationsByEncoding(
            options, pBitVectorVariables, pClauses.keySet());
    pcDeclarations =
        SeqDeclarationBuilder.buildPcDeclarations(pcVariables, numThreads, pOptions.scalarPc);

    nextThreadAssignment = SeqStatementBuilder.buildNextThreadAssignment(pOptions.signedNondet);
    nextThreadAssumptions =
        SeqAssumptionBuilder.buildNextThreadAssumption(
            pOptions.signedNondet, numThreadsVariable, binaryExpressionBuilder);
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
            options, numThreadsVariable.getDeclaration(), bitVectorDeclarations, pcDeclarations));

    // add main function argument non-deterministic assignments
    rBody.addAll(buildMainFunctionArgNondetAssignments(mainSubstitution, logger));

    // --- loop starts here ---
    rBody.add(LineOfCode.of(1, SeqStringUtil.appendOpeningCurly(whileTrue.toASTString())));

    // add if next_thread is a non-determinism source
    if (options.nondeterminismSource.isNextThreadNondeterministic()) {
      if (options.comments) {
        rBody.add(LineOfCode.empty());
        rBody.add(LineOfCode.of(2, SeqComment.NEXT_THREAD_NONDET));
      }
      rBody.add(LineOfCode.of(2, nextThreadAssignment.toASTString()));
      if (options.comments) {
        rBody.add(LineOfCode.empty());
      }
      for (CFunctionCallStatement nextThreadAssumption : nextThreadAssumptions) {
        rBody.add(LineOfCode.of(2, nextThreadAssumption.toASTString()));
      }
      // assumptions over next_thread being active (pc != -1)
      if (nextThreadActiveAssumption.isPresent()) {
        if (options.comments) {
          rBody.add(LineOfCode.empty());
          rBody.add(LineOfCode.of(2, SeqComment.NEXT_THREAD_ACTIVE));
        }
        CFunctionCallStatement assumption = nextThreadActiveAssumption.orElseThrow();
        rBody.addAll(LineOfCodeUtil.buildLinesOfCode(2, assumption.toASTString()));
      }
    } else {
      if (options.comments) {
        rBody.add(LineOfCode.empty());
        rBody.add(LineOfCode.of(2, SeqComment.ACTIVE_THREAD_COUNT));
      }
      rBody.add(LineOfCode.of(2, countAssumption.orElseThrow().toASTString()));
      // TODO add assumption that ensures that at least one thread executes at least one statement:
      //  assume(K > 0 || K' > 0 || ...); -> also need separate K variables for each thread then
    }

    // add all thread simulation control flow statements
    if (options.comments) {
      rBody.add(LineOfCode.empty());
      rBody.add(LineOfCode.of(2, SeqComment.THREAD_SIMULATION_CONTROL_FLOW));
    }
    rBody.addAll(
        NondeterministicSimulationUtil.buildThreadSimulationsByNondeterminismSource(
            options, pcVariables, clauses, binaryExpressionBuilder));
    rBody.add(LineOfCode.of(1, SeqSyntax.CURLY_BRACKET_RIGHT));
    // --- loop ends here ---

    if (options.sequentializationErrors) {
      // end of main function, only reachable if thread simulation finished incorrectly -> error
      rBody.add(LineOfCode.of(1, Sequentialization.outputReachErrorDummy));
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
      MPORSubstitution pMainSubstitution, LogManager pLogger) {

    ImmutableList.Builder<LineOfCode> rMainArgAssignments = ImmutableList.builder();
    for (CIdExpression mainArg : pMainSubstitution.mainFunctionArgSubstitutes.values()) {
      CType mainArgType = mainArg.getExpressionType();
      Optional<CFunctionCallExpression> verifierNondet =
          VerifierNondetFunctionType.buildVerifierNondetByType(mainArgType);
      if (verifierNondet.isPresent()) {
        CFunctionCallAssignmentStatement assignment =
            SeqStatementBuilder.buildFunctionCallAssignmentStatement(
                mainArg, verifierNondet.orElseThrow());
        rMainArgAssignments.add(LineOfCode.of(1, assignment.toASTString()));
      } else {
        pLogger.log(
            Level.WARNING,
            "WARNING - could not find __VERIFIER_nondet function "
                + "for the following main function argument type: "
                + mainArgType.toASTString(""));
      }
    }
    return rMainArgAssignments.build();
  }

  /** Returns {@link LineOfCode} for thread simulation declarations. */
  private ImmutableList<LineOfCode> buildThreadSimulationVariableDeclarations(
      MPOROptions pOptions,
      CSimpleDeclaration pNumThreads,
      ImmutableList<SeqBitVectorDeclaration> pBitVectorDeclarations,
      ImmutableList<CVariableDeclaration> pPcDeclarations)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rDeclarations = ImmutableList.builder();

    // NUM_THREADS
    rDeclarations.add(LineOfCode.of(1, pNumThreads.toASTString()));

    // next_thread
    if (pOptions.nondeterminismSource.isNextThreadNondeterministic()) {
      if (pOptions.signedNondet) {
        rDeclarations.add(
            LineOfCode.of(1, SeqVariableDeclaration.NEXT_THREAD_SIGNED.toASTString()));
      } else {
        rDeclarations.add(
            LineOfCode.of(1, SeqVariableDeclaration.NEXT_THREAD_UNSIGNED.toASTString()));
      }
    }

    // pc
    if (pOptions.comments) {
      rDeclarations.add(LineOfCode.empty());
      rDeclarations.add(LineOfCode.of(1, SeqComment.PC_DECLARATION));
    }
    for (CVariableDeclaration pcDeclaration : pPcDeclarations) {
      rDeclarations.add(LineOfCode.of(1, pcDeclaration.toASTString()));
    }

    // if enabled: bit vectors (for partial order reductions)
    if (pOptions.bitVectorReduction.isEnabled() && pOptions.bitVectorEncoding.isEnabled()) {
      for (SeqBitVectorDeclaration bitVectorDeclaration : pBitVectorDeclarations) {
        rDeclarations.add(LineOfCode.of(1, bitVectorDeclaration.toASTString()));
      }
    }

    // active_thread_count / cnt
    if (!pOptions.nondeterminismSource.isNextThreadNondeterministic()) {
      rDeclarations.add(LineOfCode.of(1, SeqVariableDeclaration.CNT.toASTString()));
    }

    // if enabled: K and r (for thread loops iterations)
    if (pOptions.nondeterminismSource.isNumStatementsNondeterministic()) {
      rDeclarations.add(LineOfCode.of(1, SeqVariableDeclaration.R.toASTString()));
      if (pOptions.signedNondet) {
        rDeclarations.add(LineOfCode.of(1, SeqVariableDeclaration.K_SIGNED.toASTString()));
      } else {
        rDeclarations.add(LineOfCode.of(1, SeqVariableDeclaration.K_UNSIGNED.toASTString()));
      }
    }

    return rDeclarations.build();
  }
}
