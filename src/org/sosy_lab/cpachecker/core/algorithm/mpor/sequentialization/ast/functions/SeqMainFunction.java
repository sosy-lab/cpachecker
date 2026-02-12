// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.common.collect.Collections3.elementAndList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationFields;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterministicSimulationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportFunctionDefinition;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CStatementWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CWhileLoopStatement;

/** A class to represent the {@code main()} function in the sequentialization. */
public final class SeqMainFunction extends SeqFunctionDefinition {

  // CFunctionType

  private static final CFunctionType MAIN_FUNCTION_TYPE =
      new CFunctionType(CNumericTypes.INT, ImmutableList.of(), false);

  // CFunctionDeclaration

  public static final CFunctionDeclaration MAIN_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY, MAIN_FUNCTION_TYPE, "main", ImmutableList.of(), ImmutableSet.of());

  public SeqMainFunction(
      MPOROptions pOptions, SequentializationFields pFields, SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    super(
        new CExportFunctionDefinition(
            MAIN_FUNCTION_DECLARATION, buildBody(pOptions, pFields, pUtils)));
  }

  private static CCompoundStatement buildBody(
      MPOROptions pOptions, SequentializationFields pFields, SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<CExportStatement> rBody = ImmutableList.builder();

    // add main function argument non-deterministic assignments
    rBody.addAll(buildMainFunctionArgNondetAssignments(pFields, pUtils.logger()));

    if (pOptions.loopUnrolling()) {
      // when unrolling loops, add function calls to the respective thread simulation
      ImmutableList<CFunctionCallStatement> functionCallStatements =
          NondeterministicSimulationBuilder.buildThreadSimulationFunctionCallStatements(
              pOptions, pFields.threadSimulationFunctions.orElseThrow());
      functionCallStatements.forEach(statement -> rBody.add(new CStatementWrapper(statement)));

    } else {
      // otherwise include the thread simulations in the main function directly
      ImmutableList.Builder<CExportStatement> loopBlock = ImmutableList.builder();

      if (pOptions.reduceLastThreadOrder()) {
        // add LAST_THREAD = next_thread assignment (before setting next_thread)
        if (pOptions.nondeterminismSource().isNextThreadNondeterministic()) {
          CExpressionAssignmentStatement assignment =
              new CExpressionAssignmentStatement(
                  FileLocation.DUMMY, SeqIdExpressions.LAST_THREAD, SeqIdExpressions.NEXT_THREAD);
          loopBlock.add(new CStatementWrapper(assignment));
        }
      }

      // add if next_thread is a non-determinism source
      if (pOptions.nondeterminismSource().isNextThreadNondeterministic()) {
        if (pOptions.comments()) {
          loopBlock.add(SeqComment.NEXT_THREAD_NONDET);
        }
        // next_thread = __VERIFIER_nondet_...()
        CFunctionCallAssignmentStatement nextThreadAssignment =
            VerifierNondetFunctionType.buildNondetIntegerAssignment(
                pOptions, SeqIdExpressions.NEXT_THREAD);
        loopBlock.add(new CStatementWrapper(nextThreadAssignment));

        // assume(0 <= next_thread && next_thread < NUM_THREADS)
        CExportStatement nextThreadAssumption =
            SeqAssumeFunction.buildNextThreadAssumeCallFunctionCallStatement(
                pOptions.nondeterminismSigned(),
                pFields.numThreads,
                pUtils.binaryExpressionBuilder());
        loopBlock.add(nextThreadAssumption);

        // for scalar pc, this is done separately at the start of the respective thread
        if (!pOptions.scalarPc()) {
          // assumptions over next_thread being active: pc[next_thread] != 0
          if (pOptions.comments()) {
            loopBlock.add(SeqComment.NEXT_THREAD_ACTIVE);
          }
          CFunctionCallStatement nextThreadActiveAssumption =
              pFields.ghostElements.programCounterVariables().buildArrayPcUnequalExitPcAssumption();
          loopBlock.add(new CStatementWrapper(nextThreadActiveAssumption));
        }
      }

      // assumptions that at least one thread is still active: assume(thread_count > 0)
      if (pOptions.comments()) {
        loopBlock.add(SeqComment.ACTIVE_THREAD_COUNT);
      }
      // assume(thread_count > 0);
      CBinaryExpression countGreaterZeroExpression =
          pUtils
              .binaryExpressionBuilder()
              .buildBinaryExpression(
                  SeqIdExpressions.THREAD_COUNT,
                  SeqIntegerLiteralExpressions.INT_0,
                  BinaryOperator.GREATER_THAN);
      CFunctionCallStatement countAssumption =
          SeqAssumeFunction.buildAssumeFunctionCallStatement(countGreaterZeroExpression);
      loopBlock.add(new CStatementWrapper(countAssumption));

      // add all thread simulation control flow statements
      if (pOptions.comments()) {
        loopBlock.add(SeqComment.THREAD_SIMULATION_CONTROL_FLOW);
      }
      loopBlock.add(
          NondeterministicSimulationBuilder.buildNondeterministicSimulationBySource(
                  pOptions, pFields.memoryModel, pFields.ghostElements, pFields.clauses, pUtils)
              .buildAllThreadSimulations());

      // build the loop depending on settings, and include all statements in it
      CCompoundStatement compoundStatement = new CCompoundStatement(loopBlock.build());
      CWhileLoopStatement loopStatement =
          buildLoopStatement(pOptions, compoundStatement, pUtils.binaryExpressionBuilder());
      rBody.add(loopStatement);
    }
    return new CCompoundStatement(rBody.build());
  }

  /**
   * Adds the non-deterministic initializations of {@code main} function arguments, e.g. {@code arg
   * = __VERIFIER_nondet_int;}
   */
  private static ImmutableList<CExportStatement> buildMainFunctionArgNondetAssignments(
      SequentializationFields pFields, LogManager pLogger) {

    // first extract all accesses to main function arguments
    ImmutableSet<SubstituteEdge> allSubstituteEdges =
        SeqThreadStatementClauseUtil.collectAllSubstituteEdges(pFields.clauses);
    ImmutableSet<CVariableDeclaration> accessedMainFunctionArgs =
        SubstituteUtil.findAllMainFunctionArgs(allSubstituteEdges);

    // then add main function arg nondet assignments, if necessary
    ImmutableList.Builder<CExportStatement> rAssignments = ImmutableList.builder();

    for (var entry : pFields.mainSubstitution.mainFunctionArgSubstitutes.entrySet()) {
      // add assignment only if necessary, i.e. if it is accessed later (nondet is expensive)
      if (accessedMainFunctionArgs.contains(entry.getKey().asVariableDeclaration())) {
        CIdExpression mainArgSubstitute = entry.getValue();
        CType mainArgType = mainArgSubstitute.getExpressionType();
        Optional<CFunctionCallExpression> verifierNondet =
            VerifierNondetFunctionType.tryBuildFunctionCallExpressionByType(mainArgType);
        if (verifierNondet.isPresent()) {
          CFunctionCallAssignmentStatement assignment =
              new CFunctionCallAssignmentStatement(
                  FileLocation.DUMMY, mainArgSubstitute, verifierNondet.orElseThrow());
          rAssignments.add(new CStatementWrapper(assignment));
        } else {
          pLogger.log(
              Level.WARNING,
              "could not find __VERIFIER_nondet function "
                  + "for the following main function argument type: "
                  + mainArgType.toASTString(""));
        }
      }
    }
    return rAssignments.build();
  }

  private static CWhileLoopStatement buildLoopStatement(
      MPOROptions pOptions,
      CCompoundStatement pLoopBody,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(!pOptions.loopUnrolling(), "cannot build loop head, loopUnrolling is enabled");

    if (pOptions.loopIterations() == 0) {
      // infinite while (1) loop
      return new CWhileLoopStatement(
          new CExpressionWrapper(SeqIntegerLiteralExpressions.INT_1), pLoopBody);

    } else {
      // bounded while (i < N) loop
      CBinaryExpression loopExpression =
          pBinaryExpressionBuilder.buildBinaryExpression(
              SeqIdExpressions.ITERATION,
              SeqExpressionBuilder.buildIntegerLiteralExpression(pOptions.loopIterations()),
              BinaryOperator.LESS_THAN);
      CExpressionAssignmentStatement iterationIncrement =
          SeqStatementBuilder.buildIncrementStatement(
              SeqIdExpressions.ITERATION, pBinaryExpressionBuilder);
      // add the 'iteration++' statement as the first statement of the loop body
      CCompoundStatement loopBodyWithIncrement =
          new CCompoundStatement(
              elementAndList(new CStatementWrapper(iterationIncrement), pLoopBody.statements()));
      return new CWhileLoopStatement(new CExpressionWrapper(loopExpression), loopBodyWithIncrement);
    }
  }
}
