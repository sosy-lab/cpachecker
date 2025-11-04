// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationFields;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqVariableDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.CSeqLoopStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqForLoopStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqWhileLoopStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterministicSimulationUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public final class SeqMainFunction extends SeqFunction {

  private final MPOROptions options;

  private final SequentializationFields fields;

  private final SequentializationUtils utils;

  public SeqMainFunction(
      MPOROptions pOptions, SequentializationFields pFields, SequentializationUtils pUtils) {

    options = pOptions;
    fields = pFields;
    utils = pUtils;
  }

  @Override
  public String buildBody() throws UnrecognizedCodeException {
    StringBuilder rBody = new StringBuilder();

    // add main function argument non-deterministic assignments
    rBody.append(
        buildMainFunctionArgNondetAssignments(
            fields.mainSubstitution, fields.clauses, utils.logger()));

    if (options.loopUnrolling()) {
      // when unrolling loops, add function calls to the respective thread simulation
      ImmutableList<CFunctionCallStatement> functionCallStatements =
          NondeterministicSimulationUtil.buildThreadSimulationFunctionCallStatements(
              options, fields);
      functionCallStatements.forEach(statement -> rBody.append(statement.toASTString()));

    } else {
      // otherwise include the thread simulations in the main function directly
      ImmutableList.Builder<String> loopBlock = ImmutableList.builder();

      if (options.reduceLastThreadOrder()) {
        // add last_thread = next_thread assignment (before setting next_thread)
        if (options.nondeterminismSource().isNextThreadNondeterministic()) {
          CExpressionAssignmentStatement assignment =
              SeqStatementBuilder.buildLastThreadAssignment(SeqIdExpressions.NEXT_THREAD);
          loopBlock.add(assignment.toASTString());
        }
      }

      // add if next_thread is a non-determinism source
      if (options.nondeterminismSource().isNextThreadNondeterministic()) {
        if (options.comments()) {
          loopBlock.add(SeqComment.NEXT_THREAD_NONDET);
        }
        // next_thread = __VERIFIER_nondet_...()
        CFunctionCallAssignmentStatement nextThreadAssignment =
            SeqStatementBuilder.buildNondetIntegerAssignment(options, SeqIdExpressions.NEXT_THREAD);
        loopBlock.add(nextThreadAssignment.toASTString());

        // assume(0 <= next_thread && next_thread < NUM_THREADS)
        ImmutableList<CFunctionCallStatement> nextThreadAssumption =
            SeqAssumptionBuilder.buildNextThreadAssumption(
                options.nondeterminismSigned(), fields, utils.binaryExpressionBuilder());
        nextThreadAssumption.forEach(assumption -> loopBlock.add(assumption.toASTString()));

        // for scalar pc, this is done separately at the start of the respective thread
        if (!options.scalarPc()) {
          // assumptions over next_thread being active: pc[next_thread] != 0
          if (options.comments()) {
            loopBlock.add(SeqComment.NEXT_THREAD_ACTIVE);
          }
          CFunctionCallStatement nextThreadActiveAssumption =
              SeqAssumptionBuilder.buildNextThreadActiveAssumption(utils.binaryExpressionBuilder());
          loopBlock.add(nextThreadActiveAssumption.toASTString());
        }
      }

      if (options.isThreadCountRequired()) {
        // assumptions that at least one thread is still active: assume(cnt > 0)
        if (options.comments()) {
          loopBlock.add(SeqComment.ACTIVE_THREAD_COUNT);
        }
        CFunctionCallStatement countAssumption =
            SeqAssumptionBuilder.buildCountGreaterZeroAssumption(utils.binaryExpressionBuilder());
        loopBlock.add(countAssumption.toASTString());
      }

      // add all thread simulation control flow statements
      if (options.comments()) {
        loopBlock.add(SeqComment.THREAD_SIMULATION_CONTROL_FLOW);
      }
      loopBlock.add(
          NondeterministicSimulationUtil.buildThreadSimulationsByNondeterminismSource(
              options, fields, utils));

      // build the loop depending on settings, and include all statements in it
      CSeqLoopStatement loopStatement =
          buildLoopStatement(options, loopBlock.build(), utils.binaryExpressionBuilder());
      rBody.append(loopStatement.toASTString());
    }
    return rBody.toString();
  }

  @Override
  public CType getReturnType() {
    return CNumericTypes.INT;
  }

  @Override
  public CIdExpression getFunctionName() {
    return SeqIdExpressions.MAIN;
  }

  @Override
  public ImmutableList<CParameterDeclaration> getParameterDeclarations() {
    return ImmutableList.of();
  }

  /**
   * Adds the non-deterministic initializations of {@code main} function arguments, e.g. {@code arg
   * = __VERIFIER_nondet_int;}
   */
  private String buildMainFunctionArgNondetAssignments(
      MPORSubstitution pMainSubstitution,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      LogManager pLogger) {

    // first extract all accesses to main function arguments
    ImmutableSet<SubstituteEdge> allSubstituteEdges =
        SeqThreadStatementClauseUtil.collectAllSubstituteEdges(pClauses);
    ImmutableSet<CParameterDeclaration> accessedMainFunctionArgs =
        SubstituteUtil.findAllMainFunctionArgs(allSubstituteEdges);

    // then add main function arg nondet assignments, if necessary
    StringJoiner rAssignments = new StringJoiner(SeqSyntax.NEWLINE);
    for (var entry : pMainSubstitution.mainFunctionArgSubstitutes.entrySet()) {
      // add assignment only if necessary, i.e. if it is accessed later (nondet is expensive)
      if (accessedMainFunctionArgs.contains(entry.getKey())) {
        CIdExpression mainArgSubstitute = entry.getValue();
        CType mainArgType = mainArgSubstitute.getExpressionType();
        Optional<CFunctionCallExpression> verifierNondet =
            VerifierNondetFunctionType.tryBuildFunctionCallExpressionByType(mainArgType);
        if (verifierNondet.isPresent()) {
          CFunctionCallAssignmentStatement assignment =
              SeqStatementBuilder.buildFunctionCallAssignmentStatement(
                  mainArgSubstitute, verifierNondet.orElseThrow());
          rAssignments.add(assignment.toASTString());
        } else {
          pLogger.log(
              Level.WARNING,
              "could not find __VERIFIER_nondet function "
                  + "for the following main function argument type: "
                  + mainArgType.toASTString(""));
        }
      }
    }
    return rAssignments.toString();
  }

  private static CSeqLoopStatement buildLoopStatement(
      MPOROptions pOptions,
      ImmutableList<String> pLoopBody,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(!pOptions.loopUnrolling(), "cannot build loop head, loopUnrolling is enabled");

    if (pOptions.loopIterations() == 0) {
      // infinite while (1) loop
      return new SeqWhileLoopStatement(SeqIntegerLiteralExpressions.INT_1, pLoopBody);

    } else {
      // bounded for (...) loop
      CBinaryExpression forExpression =
          pBinaryExpressionBuilder.buildBinaryExpression(
              SeqIdExpressions.ITERATION,
              SeqExpressionBuilder.buildIntegerLiteralExpression(pOptions.loopIterations()),
              BinaryOperator.LESS_THAN);
      CExpressionAssignmentStatement forIterationUpdate =
          SeqStatementBuilder.buildIncrementStatement(
              SeqIdExpressions.ITERATION, pBinaryExpressionBuilder);
      return new SeqForLoopStatement(
          SeqVariableDeclarations.ITERATION, forExpression, forIterationUpdate, pLoopBody);
    }
  }
}
