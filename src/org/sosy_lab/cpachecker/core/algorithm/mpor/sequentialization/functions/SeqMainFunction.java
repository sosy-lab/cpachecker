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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqForExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqSingleControlExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqWhileExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.nondet_simulations.NondeterministicSimulationUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadSimulationVariables;
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

  private final MPOROptions options;

  private final MPORSubstitution mainSubstitution;

  private final CIdExpression numThreadsVariable;

  /** The thread-specific clauses in the while loop. */
  private final ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> clauses;

  private final ImmutableList<CVariableDeclaration> pcDeclarations;

  // TODO make Optional
  private final CFunctionCallAssignmentStatement nextThreadAssignment;

  // TODO make Optional (and also shouldn't be a list)
  private final ImmutableList<CFunctionCallStatement> nextThreadAssumptions;

  private final Optional<CFunctionCallStatement> nextThreadActiveAssumption;

  private final Optional<CFunctionCallStatement> countAssumption;

  private final PcVariables pcVariables;

  private final ThreadSimulationVariables threadSimulationVariables;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final LogManager logger;

  public SeqMainFunction(
      MPOROptions pOptions,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables,
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
    threadSimulationVariables = pThreadSimulationVariables;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    logger = pLogger;

    pcDeclarations = SeqDeclarationBuilder.buildPcDeclarations(options, pcVariables, numThreads);

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
        buildLocalThreadSimulationVariableDeclarations(
            options,
            numThreadsVariable.getDeclaration(),
            threadSimulationVariables));

    // add main function argument non-deterministic assignments
    rBody.addAll(buildMainFunctionArgNondetAssignments(mainSubstitution, logger));

    // --- loop starts here ---
    SeqSingleControlExpression loopHead = buildLoopHead(options, binaryExpressionBuilder);
    rBody.add(LineOfCode.of(1, SeqStringUtil.appendOpeningCurlyBrackets(loopHead.toASTString())));

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
  private ImmutableList<LineOfCode> buildLocalThreadSimulationVariableDeclarations(
      MPOROptions pOptions,
      CSimpleDeclaration pNumThreadDeclaration,
      ThreadSimulationVariables pThreadSimulationVariables) {

    ImmutableList.Builder<LineOfCode> rDeclarations = ImmutableList.builder();

    // NUM_THREADS
    rDeclarations.add(LineOfCode.of(1, pNumThreadDeclaration.toASTString()));

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

    // thread synchronization variables (e.g. mutex_locked)
    if (pOptions.comments) {
      rDeclarations.add(LineOfCode.of(1, SeqComment.THREAD_SIMULATION_VARIABLES));
    }
    for (CIdExpression threadVariable : pThreadSimulationVariables.getIdExpressions()) {
      assert threadVariable.getDeclaration() instanceof CVariableDeclaration;
      CVariableDeclaration varDeclaration = (CVariableDeclaration) threadVariable.getDeclaration();
      rDeclarations.add(LineOfCode.of(1, varDeclaration.toASTString()));
    }
    if (pOptions.comments) {
      rDeclarations.add(LineOfCode.empty());
    }

    return rDeclarations.build();
  }
}
