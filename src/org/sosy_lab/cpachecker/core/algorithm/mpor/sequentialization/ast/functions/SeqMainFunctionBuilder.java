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
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterministicSimulationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector.CommutingThreadsFirstInjector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatementElement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportFunctionDefinition;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CStatementWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CWhileLoopStatement;

/** A class to represent the {@code main()} function in the sequentialization. */
public final class SeqMainFunctionBuilder {

  // CFunctionType

  private static final CFunctionType MAIN_FUNCTION_TYPE =
      new CFunctionType(CNumericTypes.INT, ImmutableList.of(), false);

  // CFunctionDeclaration

  public static final CFunctionDeclaration MAIN_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY, MAIN_FUNCTION_TYPE, "main", ImmutableList.of(), ImmutableSet.of());

  public static CExportFunctionDefinition buildFunctionDefinition(
      MPOROptions pOptions, SequentializationFields pFields, SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return new CExportFunctionDefinition(
        MAIN_FUNCTION_DECLARATION, buildBody(pOptions, pFields, pUtils));
  }

  private static CCompoundStatement buildBody(
      MPOROptions pOptions, SequentializationFields pFields, SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<CCompoundStatementElement> rBody = ImmutableList.builder();

    // add main function argument non-deterministic assignments
    rBody.addAll(buildMainFunctionArgNondetAssignments(pFields, pUtils.logger()));

    if (pOptions.threadSimulationUnrolling()) {
      // when unrolling loops, add function calls to the respective thread simulation
      ImmutableList<CFunctionCallStatement> functionCallStatements =
          NondeterministicSimulationBuilder.buildThreadSimulationFunctionCallStatements(
              pOptions, pFields.threadSimulationFunctions.orElseThrow());
      functionCallStatements.forEach(statement -> rBody.add(new CStatementWrapper(statement)));

    } else {
      // otherwise include the thread simulations in the main function directly
      ImmutableList.Builder<CCompoundStatementElement> loopBlock = ImmutableList.builder();

      // assumptions that at least one thread is still active: assume(thread_count > 0)
      if (pOptions.executeSingleActiveThreadFirst()) {
        if (pOptions.comments()) {
          loopBlock.add(SeqComment.ACTIVE_THREAD_COUNT);
        }
        // assume(thread_count > 0);
        CBinaryExpression countGreaterZeroExpression =
            pUtils
                .binaryExpressionBuilder()
                .buildBinaryExpression(
                    SeqIdExpressions.THREAD_COUNT,
                    CIntegerLiteralExpression.ZERO,
                    BinaryOperator.GREATER_THAN);
        CFunctionCallStatement countAssumption =
            SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(countGreaterZeroExpression);
        loopBlock.add(new CStatementWrapper(countAssumption));
      }

      // add if next_thread is a non-determinism source
      if (pOptions.nondeterminismSource().isNextThreadNondeterministic()) {
        if (pOptions.comments()) {
          loopBlock.add(SeqComment.NEXT_THREAD_NONDET);
        }
        if (pOptions.executeCommutingThreadsFirst()) {
          // with executeCommutingThreadsFirst enabled, the nondeterministic next_thread assignment
          // is embedded into the reduction instrumentation, not added on top
          loopBlock.add(
              CommutingThreadsFirstInjector
                  .buildCommutingThreadsFirstInstrumentationForNextThreadNondeterminism(
                      pOptions, pFields, pUtils));
        } else {
          // with executeCommutingThreadsFirst disabled, the nondeterministic next_thread choice is
          // placed in isolation: next_thread = __VERIFIER_nondet_uint();
          loopBlock.add(
              buildNextThreadNondeterministicStatements(
                  pOptions,
                  pFields.numThreads,
                  pFields.ghostElements,
                  pUtils.binaryExpressionBuilder()));
        }
      }

      // add all thread simulation control flow statements
      if (pOptions.comments()) {
        loopBlock.add(SeqComment.THREAD_SIMULATION_CONTROL_FLOW);
      }
      loopBlock.add(
          NondeterministicSimulationBuilder.buildNondeterministicSimulationBySource(
                  pOptions,
                  pFields.machineModel,
                  pFields.pointerAliasingMap,
                  pFields.ghostElements,
                  pFields.clauses,
                  pUtils)
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
        allSubstituteEdges.stream()
            .flatMap(substituteEdge -> substituteEdge.accessedMainFunctionArgs.stream())
            .collect(ImmutableSet.toImmutableSet());

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

    checkArgument(
        !pOptions.threadSimulationUnrolling(),
        "cannot build loop head, threadSimulationUnrolling is enabled");

    if (pOptions.threadSimulationIterations() == 0) {
      // infinite while (1) loop
      return new CWhileLoopStatement(
          new CExpressionWrapper(CIntegerLiteralExpression.ONE), pLoopBody);

    } else {
      // bounded while (i < N) loop
      CBinaryExpression loopExpression =
          pBinaryExpressionBuilder.buildBinaryExpression(
              SeqIdExpressions.ITERATION,
              SeqExpressionBuilder.buildIntegerLiteralExpression(
                  pOptions.threadSimulationIterations()),
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

  public static CCompoundStatement buildNextThreadNondeterministicStatements(
      MPOROptions pOptions,
      int pNumThreads,
      GhostElements pGhostElements,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<CCompoundStatementElement> rStatements = ImmutableList.builder();

    // next_thread = __VERIFIER_nondet_...()
    CFunctionCallAssignmentStatement nextThreadAssignment =
        VerifierNondetFunctionType.buildNondetIntegerAssignment(
            pOptions, SeqIdExpressions.NEXT_THREAD);
    rStatements.add(new CStatementWrapper(nextThreadAssignment));

    // assume(0 <= next_thread && next_thread < NUM_THREADS)
    CExportStatement nextThreadAssumption =
        SeqAssumeFunctionBuilder.buildNextThreadAssumeCallFunctionCallStatement(
            pOptions.nondeterminismSigned(), pNumThreads, pBinaryExpressionBuilder);
    rStatements.add(nextThreadAssumption);

    // for scalar pc, this is done separately at the start of the respective thread
    if (!pOptions.scalarProgramCounters()) {
      // assumptions over next_thread being active: pc[next_thread] != 0
      if (pOptions.comments()) {
        rStatements.add(SeqComment.NEXT_THREAD_ACTIVE);
      }
      CFunctionCallStatement nextThreadActiveAssumption =
          pGhostElements.programCounterVariables().buildArrayPcUnequalExitPcAssumption();
      rStatements.add(new CStatementWrapper(nextThreadActiveAssumption));
    }
    return new CCompoundStatement(rStatements.build());
  }
}
