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
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationFields;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqInitializerBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqForExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqSingleControlExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqWhileExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.nondet_simulations.NondeterministicSimulationUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.declaration.SeqBitVectorDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.declaration.SeqBitVectorDeclarationBuilder;
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

  private final SequentializationFields fields;

  private final CIdExpression numThreadsVariable;

  // TODO make Optional
  private final CFunctionCallAssignmentStatement nextThreadAssignment;

  // TODO make Optional (and also shouldn't be a list)
  private final ImmutableList<CFunctionCallStatement> nextThreadAssumptions;

  private final Optional<CFunctionCallStatement> nextThreadActiveAssumption;

  private final Optional<CFunctionCallStatement> countAssumption;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final LogManager logger;

  public SeqMainFunction(
      MPOROptions pOptions,
      SequentializationFields pFields,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    options = pOptions;
    fields = pFields;
    numThreadsVariable = SeqExpressionBuilder.buildNumThreadsIdExpression(pFields.numThreads);

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
  public ImmutableList<String> buildBody() throws UnrecognizedCodeException {
    ImmutableList.Builder<String> rBody = ImmutableList.builder();

    // declare main() local variables NUM_THREADS, pc, next_thread
    // TODO its probably best to remove num threads entirely and just place the int
    rBody.addAll(
        buildThreadSimulationVariableDeclarations(
            options, fields, numThreadsVariable.getDeclaration()));

    // add main function argument non-deterministic assignments
    rBody.addAll(
        buildMainFunctionArgNondetAssignments(fields.mainSubstitution, fields.clauses, logger));

    // --- loop starts here ---
    SeqSingleControlExpression loopHead = buildLoopHead(options, binaryExpressionBuilder);
    rBody.add(SeqStringUtil.appendCurlyBracketLeft(loopHead.toASTString()));

    if (options.conflictReduction) {
      // add last_thread = next_thread assignment (before setting next_thread)
      if (options.nondeterminismSource.isNextThreadNondeterministic()) {
        CExpressionAssignmentStatement assignment =
            SeqStatementBuilder.buildLastThreadAssignment(SeqIdExpression.NEXT_THREAD);
        rBody.add(assignment.toASTString());
      }
    }

    // add if next_thread is a non-determinism source
    if (options.nondeterminismSource.isNextThreadNondeterministic()) {
      if (options.comments) {
        rBody.add(SeqComment.NEXT_THREAD_NONDET);
      }
      rBody.add(nextThreadAssignment.toASTString());
      for (CFunctionCallStatement nextThreadAssumption : nextThreadAssumptions) {
        rBody.add(nextThreadAssumption.toASTString());
      }
      // assumptions over next_thread being active (pc != -1)
      if (nextThreadActiveAssumption.isPresent()) {
        if (options.comments) {
          rBody.add(SeqComment.NEXT_THREAD_ACTIVE);
        }
        CFunctionCallStatement assumption = nextThreadActiveAssumption.orElseThrow();
        rBody.addAll(SeqStringUtil.splitOnNewline(assumption.toASTString()));
      }
    } else {
      if (options.comments) {
        rBody.add(SeqComment.ACTIVE_THREAD_COUNT);
      }
      rBody.add(countAssumption.orElseThrow().toASTString());
    }

    // add all thread simulation control flow statements
    if (options.comments) {
      rBody.add(SeqComment.THREAD_SIMULATION_CONTROL_FLOW);
    }
    rBody.addAll(
        NondeterministicSimulationUtil.buildThreadSimulationsByNondeterminismSource(
            options, fields.ghostElements, fields.clauses, binaryExpressionBuilder));
    rBody.add(SeqSyntax.CURLY_BRACKET_RIGHT);
    // --- loop ends here ---

    if (options.sequentializationErrors) {
      // end of main function, only reachable if thread simulation finished incorrectly -> error
      rBody.add(Sequentialization.outputReachErrorDummy);
    }
    return rBody.build();
  }

  @Override
  public CType getReturnType() {
    return CNumericTypes.INT;
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
  private ImmutableList<String> buildMainFunctionArgNondetAssignments(
      MPORSubstitution pMainSubstitution,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      LogManager pLogger) {

    // first extract all accesses to main function arguments
    ImmutableSet<SubstituteEdge> allSubstituteEdges =
        SeqThreadStatementClauseUtil.collectAllSubstituteEdges(pClauses);
    ImmutableSet<CParameterDeclaration> accessedMainFunctionArgs =
        SubstituteUtil.findAllMainFunctionArgs(allSubstituteEdges);

    // then add main function arg nondet assignments, if necessary
    ImmutableList.Builder<String> rMainArgAssignments = ImmutableList.builder();
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
          rMainArgAssignments.add(assignment.toASTString());
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

  /**
   * Returns the {@link String} for thread simulation variable declarations. These are local to the
   * {@code main} function. Variables that are used in other functions are declared beforehand as
   * global variables.
   */
  private static ImmutableList<String> buildThreadSimulationVariableDeclarations(
      MPOROptions pOptions,
      SequentializationFields pFields,
      CSimpleDeclaration pNumThreadDeclaration)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<String> rDeclarations = ImmutableList.builder();

    // NUM_THREADS
    rDeclarations.add(pNumThreadDeclaration.toASTString());

    // last_thread is always unsigned, we assign NUM_THREADS if the current thread terminates
    if (pOptions.conflictReduction) {
      CIntegerLiteralExpression numThreadsLiteral =
          SeqExpressionBuilder.buildIntegerLiteralExpression(pFields.numThreads);
      CInitializer lastThreadInitializer =
          SeqInitializerBuilder.buildInitializerExpression(numThreadsLiteral);
      CVariableDeclaration lastThreadDeclaration =
          SeqDeclarationBuilder.buildVariableDeclaration(
              false,
              CNumericTypes.UNSIGNED_INT,
              SeqIdExpression.LAST_THREAD.getName(),
              lastThreadInitializer);
      rDeclarations.add(lastThreadDeclaration.toASTString());
    }

    // next_thread
    if (pOptions.nondeterminismSource.isNextThreadNondeterministic()) {
      if (pOptions.nondeterminismSigned) {
        rDeclarations.add(SeqVariableDeclaration.NEXT_THREAD_SIGNED.toASTString());
      } else {
        rDeclarations.add(SeqVariableDeclaration.NEXT_THREAD_UNSIGNED.toASTString());
      }
    }

    // pc variable(s)
    if (pOptions.comments) {
      rDeclarations.add(SeqComment.PC_DECLARATION);
    }
    ImmutableList<CVariableDeclaration> pcDeclarations =
        SeqDeclarationBuilder.buildPcDeclarations(pOptions, pFields);
    for (CVariableDeclaration pcDeclaration : pcDeclarations) {
      rDeclarations.add(pcDeclaration.toASTString());
    }

    // if enabled: bit vectors
    if (pOptions.areBitVectorsEnabled()) {
      ImmutableList<SeqBitVectorDeclaration> bitVectorDeclarations =
          SeqBitVectorDeclarationBuilder.buildBitVectorDeclarationsByEncoding(pOptions, pFields);
      for (SeqBitVectorDeclaration bitVectorDeclaration : bitVectorDeclarations) {
        rDeclarations.add(bitVectorDeclaration.toASTString());
      }
    }

    // active_thread_count / cnt
    if (!pOptions.nondeterminismSource.isNextThreadNondeterministic()) {
      rDeclarations.add(SeqVariableDeclaration.CNT.toASTString());
    }

    // if enabled: K and r
    if (pOptions.nondeterminismSource.isNumStatementsNondeterministic()) {
      rDeclarations.add(SeqVariableDeclaration.R.toASTString());
      if (pOptions.nondeterminismSource.equals(
          NondeterminismSource.NEXT_THREAD_AND_NUM_STATEMENTS)) {
        if (pOptions.nondeterminismSigned) {
          rDeclarations.add(SeqVariableDeclaration.K_SIGNED.toASTString());
        } else {
          rDeclarations.add(SeqVariableDeclaration.K_UNSIGNED.toASTString());
        }
      }
      if (pOptions.nondeterminismSource.equals(NondeterminismSource.NUM_STATEMENTS)) {
        for (MPORThread thread : pFields.threads) {
          rDeclarations.add(thread.getKVariable().orElseThrow().getDeclaration().toASTString());
        }
      }
    }

    // thread synchronization variables (e.g. mutex_locked)
    if (pOptions.comments) {
      rDeclarations.add(SeqComment.THREAD_SIMULATION_VARIABLES);
    }
    for (CSimpleDeclaration declaration :
        pFields.ghostElements.getThreadSynchronizationVariables().getDeclarations(pOptions)) {
      rDeclarations.add(declaration.toASTString());
    }
    return rDeclarations.build();
  }
}
