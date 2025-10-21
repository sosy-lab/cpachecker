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
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationFields;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqForExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqSingleControlExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqWhileExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterministicSimulationUtil;
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

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final LogManager logger;

  public SeqMainFunction(
      MPOROptions pOptions,
      SequentializationFields pFields,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger) {

    options = pOptions;
    fields = pFields;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    logger = pLogger;
  }

  @Override
  public ImmutableList<String> buildBody() throws UnrecognizedCodeException {
    ImmutableList.Builder<String> rBody = ImmutableList.builder();

    // add main function argument non-deterministic assignments
    rBody.addAll(
        buildMainFunctionArgNondetAssignments(fields.mainSubstitution, fields.clauses, logger));

    // --- loop starts here ---
    Optional<SeqSingleControlExpression> loopHead = buildLoopHead(options, binaryExpressionBuilder);
    if (loopHead.isPresent()) {
      rBody.add(SeqStringUtil.appendCurlyBracketLeft(loopHead.orElseThrow().toASTString()));
    }

    if (options.reduceLastThreadOrder) {
      // add last_thread = next_thread assignment (before setting next_thread)
      if (options.nondeterminismSource.isNextThreadNondeterministic()) {
        CExpressionAssignmentStatement assignment =
            SeqStatementBuilder.buildLastThreadAssignment(SeqIdExpressions.NEXT_THREAD);
        rBody.add(assignment.toASTString());
      }
    }

    // add if next_thread is a non-determinism source
    if (options.nondeterminismSource.isNextThreadNondeterministic()) {
      if (options.comments) {
        rBody.add(SeqComment.NEXT_THREAD_NONDET);
      }
      // next_thread = __VERIFIER_nondet_...()
      CFunctionCallAssignmentStatement nextThreadAssignment =
          SeqStatementBuilder.buildNextThreadAssignment(options.nondeterminismSigned);
      rBody.add(nextThreadAssignment.toASTString());

      // assume(0 <= next_thread && next_thread < NUM_THREADS)
      ImmutableList<CFunctionCallStatement> nextThreadAssumption =
          SeqAssumptionBuilder.buildNextThreadAssumption(
              options.nondeterminismSigned, fields, binaryExpressionBuilder);
      nextThreadAssumption.forEach(assumption -> rBody.add(assumption.toASTString()));

      // for scalar pc, this is done separately at the start of the respective thread
      if (!options.scalarPc) {
        // assumptions over next_thread being active: pc[next_thread] != 0
        if (options.comments) {
          rBody.add(SeqComment.NEXT_THREAD_ACTIVE);
        }
        CFunctionCallStatement nextThreadActiveAssumption =
            SeqAssumptionBuilder.buildNextThreadActiveAssumption(binaryExpressionBuilder);
        rBody.add(nextThreadActiveAssumption.toASTString());
      }
    }

    if (options.isThreadCountRequired()) {
      // assumptions that at least one thread is still active: assume(cnt > 0)
      if (options.comments) {
        rBody.add(SeqComment.ACTIVE_THREAD_COUNT);
      }
      CFunctionCallStatement countAssumption =
          SeqAssumptionBuilder.buildCountGreaterZeroAssumption(binaryExpressionBuilder);
      rBody.add(countAssumption.toASTString());
    }

    // add all thread simulation control flow statements
    if (options.comments) {
      rBody.add(SeqComment.THREAD_SIMULATION_CONTROL_FLOW);
    }
    rBody.addAll(
        NondeterministicSimulationUtil.buildThreadSimulationsByNondeterminismSource(
            options, fields, binaryExpressionBuilder));

    if (loopHead.isPresent()) {
      rBody.add(SeqSyntax.CURLY_BRACKET_RIGHT);
    }
    // --- loop ends here ---
    return rBody.build();
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
            VerifierNondetFunctionType.tryBuildFunctionCallExpressionByType(mainArgType);
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

  private static Optional<SeqSingleControlExpression> buildLoopHead(
      MPOROptions pOptions, CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    if (pOptions.loopUnrolling) {
      return Optional.empty();
    }
    if (pOptions.loopIterations == 0) {
      return Optional.of(new SeqWhileExpression(SeqIntegerLiteralExpressions.INT_1));
    } else {
      return Optional.of(
          new SeqForExpression(
              SeqIdExpressions.ITERATION, pOptions.loopIterations, pBinaryExpressionBuilder));
    }
  }
}
