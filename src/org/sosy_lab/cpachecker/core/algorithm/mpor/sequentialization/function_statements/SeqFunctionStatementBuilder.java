// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_statements;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_statements.SeqFunctionStatements.SeqFunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_statements.SeqFunctionStatements.SeqFunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_statements.SeqFunctionStatements.SeqMainFunctionArgAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.SeqCallContext;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public record SeqFunctionStatementBuilder(
    ImmutableList<MPORThread> threads,
    ImmutableList<MPORSubstitution> substitutions,
    ImmutableMap<CFAEdgeForThread, SubstituteEdge> substituteEdges,
    LogManager logger) {

  public ImmutableMap<MPORThread, SeqFunctionStatements> buildFunctionStatements()
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<MPORThread, SeqFunctionStatements> rFunctionStatements =
        ImmutableMap.builder();
    for (MPORSubstitution substitution : substitutions) {
      for (MPORThread thread : threads) {
        if (substitution.getThread().equals(thread)) {
          rFunctionStatements.put(
              thread,
              new SeqFunctionStatements(
                  buildParameterAssignments(substitution),
                  buildMainFunctionArgAssignments(thread, substitution),
                  buildStartRoutineArgAssignments(substitution),
                  buildReturnValueAssignments(thread),
                  buildStartRoutineExitAssignments(thread)));
        }
      }
    }
    return rFunctionStatements.buildOrThrow();
  }

  // Function Parameter Assignments ================================================================

  /**
   * Maps {@link CFAEdgeForThread}s whose {@link CFAEdge} is a {@link CFunctionCallEdge} to a list
   * of {@link SeqFunctionParameterAssignment}s.
   *
   * <p>E.g. {@code func(&paramA, paramB);} in thread 0 is linked to {@code __t0_0_paramA = &paramA
   * ;} and {@code __t0_1_paramB = paramB ;}. Both substitution variables are declared in {@link
   * MPORSubstitution#getParameterSubstitutes()}.
   */
  private ImmutableListMultimap<CFAEdgeForThread, SeqFunctionParameterAssignment>
      buildParameterAssignments(MPORSubstitution pSubstitution) {

    ImmutableListMultimap.Builder<CFAEdgeForThread, SeqFunctionParameterAssignment> rAssignments =
        ImmutableListMultimap.builder();
    // for each function call edge (= calling context)
    for (SeqCallContext callContext : pSubstitution.getParameterSubstitutes().rowKeySet()) {
      // exclude the main function parameters, they are handled separately
      if (callContext.cfaEdgeForThread().isPresent()) {
        // exclude start routine arg assignments, they are handled separately
        if (!callContext.isStartRoutineCallContext()) {
          CFAEdgeForThread cfaEdgeForThread = callContext.cfaEdgeForThread().orElseThrow();
          CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) cfaEdgeForThread.cfaEdge;
          rAssignments.putAll(
              cfaEdgeForThread,
              buildFunctionParameterAssignments(
                  functionCallEdge, callContext, cfaEdgeForThread, pSubstitution));
        }
      }
    }
    return rAssignments.build();
  }

  private ImmutableList<SeqFunctionParameterAssignment> buildFunctionParameterAssignments(
      CFunctionCallEdge pFunctionCallEdge,
      SeqCallContext pCallContext,
      CFAEdgeForThread pCfaEdgeForThread,
      MPORSubstitution pSubstitution) {

    ImmutableList.Builder<SeqFunctionParameterAssignment> rAssignments = ImmutableList.builder();
    CFunctionDeclaration functionDeclaration =
        pFunctionCallEdge.getFunctionCallExpression().getDeclaration();
    for (int i = 0; i < functionDeclaration.getParameters().size(); i++) {
      CParameterDeclaration parameterDeclaration =
          MPORUtil.getParameterDeclarationByIndex(i, functionDeclaration);
      ImmutableList<CIdExpression> parameterSubstitutes =
          pSubstitution.getParameterDeclarationSubstitute(pCallContext, parameterDeclaration);
      // for normal function calls the call context is always a CFunctionCallEdge
      CFunctionCallEdge callContextSubstitute =
          (CFunctionCallEdge)
              Objects.requireNonNull(substituteEdges.get(pCfaEdgeForThread)).cfaEdge;
      // go through all parameter substitutes. if there is more than one, the function is variadic
      for (int j = 0; j < parameterSubstitutes.size(); j++) {
        CExpressionAssignmentStatement expressionAssignmentStatement =
            SeqStatementBuilder.buildExpressionAssignmentStatement(
                parameterSubstitutes.get(j), callContextSubstitute.getArguments().get(i + j));
        rAssignments.add(
            new SeqFunctionParameterAssignment(
                expressionAssignmentStatement, pCallContext, pCfaEdgeForThread.callContext));
      }
    }
    return rAssignments.build();
  }

  private ImmutableList<SeqMainFunctionArgAssignment> buildMainFunctionArgAssignments(
      MPORThread pThread, MPORSubstitution pSubstitution) {

    if (!pThread.isMain()) {
      return ImmutableList.of();
    }

    ImmutableList.Builder<SeqMainFunctionArgAssignment> rAssignments = ImmutableList.builder();
    for (CParameterDeclaration parameterDeclaration : pThread.startRoutine().getParameters()) {
      CIdExpression argSubstitute =
          Objects.requireNonNull(
              pSubstitution.getMainFunctionArgSubstitutes().get(parameterDeclaration));
      CType argType = argSubstitute.getExpressionType();
      Optional<CFunctionCallExpression> nondetFunctionCallExpression =
          VerifierNondetFunctionType.tryBuildFunctionCallExpressionByType(argType);
      if (nondetFunctionCallExpression.isPresent()) {
        CFunctionCallAssignmentStatement assignment =
            new CFunctionCallAssignmentStatement(
                FileLocation.DUMMY, argSubstitute, nondetFunctionCallExpression.orElseThrow());
        rAssignments.add(new SeqMainFunctionArgAssignment(assignment));
      } else {
        logger.log(
            Level.WARNING,
            "Could not find __VERIFIER_nondet function "
                + "for the following main function argument type: "
                + argType.toASTString(""));
      }
    }
    return rAssignments.build();
  }

  private ImmutableMap<CFAEdgeForThread, SeqFunctionParameterAssignment>
      buildStartRoutineArgAssignments(MPORSubstitution pSubstitution)
          throws UnrecognizedCodeException {

    ImmutableMap.Builder<CFAEdgeForThread, SeqFunctionParameterAssignment> rAssignments =
        ImmutableMap.builder();
    for (var cell : pSubstitution.getStartRoutineArgSubstitutes().cellSet()) {
      // this call context is the call to pthread_create
      SeqCallContext callContext = cell.getRowKey();

      if (callContext.isStartRoutineCallContext()) {
        CFAEdgeForThread cfaEdgeForThread = callContext.cfaEdgeForThread().orElseThrow();
        SubstituteEdge substituteEdge =
            Objects.requireNonNull(substituteEdges.get(cfaEdgeForThread));
        Optional<CFunctionCall> functionCallSubstitute =
            PthreadUtil.tryGetFunctionCallFromCfaEdge(substituteEdge.cfaEdge);

        if (functionCallSubstitute.isPresent()
            && PthreadUtil.isCallToAnyPthreadFunctionWithObjectType(
                functionCallSubstitute.orElseThrow(), PthreadObjectType.START_ROUTINE_ARGUMENT)) {
          CExpression rightHandSideSubstitute =
              PthreadUtil.extractStartRoutineArg(functionCallSubstitute.orElseThrow());
          CExpressionAssignmentStatement expressionAssignmentStatement =
              SeqStatementBuilder.buildExpressionAssignmentStatement(
                  // start routines cannot take variadic arguments, so there should be only one
                  cell.getValue(), rightHandSideSubstitute);
          SeqFunctionParameterAssignment startRoutineArgAssignment =
              new SeqFunctionParameterAssignment(
                  expressionAssignmentStatement, callContext, cfaEdgeForThread.callContext);
          rAssignments.put(cfaEdgeForThread, startRoutineArgAssignment);
        }
      }
    }
    return rAssignments.buildOrThrow();
  }

  // return value assignments ======================================================================

  /**
   * Maps {@link CFAEdgeForThread}s whose {@link CFAEdge} is a {@link CReturnStatementEdge} to
   * {@link SeqFunctionReturnValueAssignment}s where the CPAchecker_TMP vars are assigned the return
   * value.
   *
   * <p>The return statement may be linked to multiple function calls, thus the inner set.
   *
   * <p>Note that {@code main} functions and start_routines of threads oftentimes do not have
   * corresponding {@link CFunctionSummaryEdge}s.
   */
  private ImmutableMap<CFAEdgeForThread, SeqFunctionReturnValueAssignment>
      buildReturnValueAssignments(MPORThread pThread) {

    ImmutableMap.Builder<CFAEdgeForThread, SeqFunctionReturnValueAssignment> rReturnStatements =
        ImmutableMap.builder();
    for (CFAEdgeForThread threadEdge : pThread.cfa().threadEdges) {
      checkState(
          substituteEdges.containsKey(threadEdge), "substituteEdges must contain all threadEdges");
      // consider only edges with call context, e.g. return 0; in main has no call context
      if (threadEdge.callContext.cfaEdgeForThread().isPresent()) {
        SubstituteEdge substituteEdge = Objects.requireNonNull(substituteEdges.get(threadEdge));
        if (substituteEdge.cfaEdge instanceof CReturnStatementEdge returnStatementEdge) {
          CFAEdgeForThread cfaEdgeForThread =
              threadEdge.callContext.cfaEdgeForThread().orElseThrow();
          Optional<SubstituteEdge> functionSummaryEdge =
              tryGetFunctionSummaryEdgeByReturnStatementEdge(
                  pThread, returnStatementEdge, cfaEdgeForThread);
          if (functionSummaryEdge.isPresent()) {
            Optional<SeqFunctionReturnValueAssignment> assignment =
                tryBuildReturnValueAssignment(
                    functionSummaryEdge.orElseThrow(), returnStatementEdge);
            if (assignment.isPresent()) {
              rReturnStatements.put(threadEdge, assignment.orElseThrow());
            }
          }
        }
      }
    }
    return rReturnStatements.buildOrThrow();
  }

  private Optional<SubstituteEdge> tryGetFunctionSummaryEdgeByReturnStatementEdge(
      MPORThread pThread,
      CReturnStatementEdge pReturnStatementEdge,
      CFAEdgeForThread pCallContext) {

    pThread
        .cfa()
        .threadEdges
        .forEach(
            e ->
                checkState(
                    substituteEdges.containsKey(e),
                    "Edge %s missing from substitute map",
                    e.cfaEdge.getCode()));
    for (CFAEdgeForThread threadEdge : pThread.cfa().threadEdges) {
      // consider only threadEdges with callContext, CReturnStatementEdges always have call contexts
      if (threadEdge.callContext.cfaEdgeForThread().isPresent()) {
        CFAEdgeForThread cfaEdgeForThread = threadEdge.callContext.cfaEdgeForThread().orElseThrow();
        if (cfaEdgeForThread.equals(pCallContext)) {
          FluentIterable<? extends FunctionCallEdge> functionCallEdges =
              pReturnStatementEdge.getSuccessor().getEntryNode().getEnteringCallEdges();
          for (FunctionCallEdge functionCallEdge : functionCallEdges) {
            if (cfaEdgeForThread.cfaEdge.equals(functionCallEdge)) {
              return Optional.of(
                  getSubstituteEdgeByCfaEdgeAndCallContext(
                      functionCallEdge.getSummaryEdge(),
                      // use the call contexts predecessor, which is used by the functionSummaryEdge
                      cfaEdgeForThread.getPredecessor().callContext));
            }
          }
        }
      }
    }
    // happens e.g. for return 0; in the start_routine
    return Optional.empty();
  }

  private Optional<SeqFunctionReturnValueAssignment> tryBuildReturnValueAssignment(
      SubstituteEdge pFunctionSummaryEdge, CReturnStatementEdge pReturnStatementEdge) {

    CFunctionSummaryEdge functionSummaryEdge = (CFunctionSummaryEdge) pFunctionSummaryEdge.cfaEdge;
    // if the summary edge is of the form value = func(); (i.e. an assignment)
    if (functionSummaryEdge.getExpression()
        instanceof CFunctionCallAssignmentStatement functionCallAssignmentStatement) {
      CFunctionDeclaration functionDeclarationA =
          (CFunctionDeclaration) pReturnStatementEdge.getSuccessor().getFunction();
      CFunctionDeclaration functionDeclarationB =
          functionSummaryEdge.getExpression().getFunctionCallExpression().getDeclaration();
      if (functionDeclarationA.equals(functionDeclarationB)) {
        CExpressionAssignmentStatement assignmentStatement =
            SeqStatementBuilder.buildExpressionAssignmentStatement(
                functionCallAssignmentStatement.getLeftHandSide(),
                pReturnStatementEdge.getExpression().orElseThrow());
        return Optional.of(
            new SeqFunctionReturnValueAssignment(
                assignmentStatement,
                pFunctionSummaryEdge.getCallContext(),
                new SeqCallContext(Optional.of(pFunctionSummaryEdge.getThreadEdge()))));
      }
    }
    return Optional.empty();
  }

  // start_routine exit ============================================================================

  /**
   * Links {@link CFAEdgeForThread}s that call {@code pthread_exit} to {@link
   * SeqFunctionReturnValueAssignment} where the {@code retval} is stored in an intermediate value
   * that can be retrieved by other threads calling {@code pthread_join}.
   */
  private ImmutableMap<CFAEdgeForThread, SeqFunctionReturnValueAssignment>
      buildStartRoutineExitAssignments(MPORThread pThread) throws UnsupportedCodeException {

    ImmutableMap.Builder<CFAEdgeForThread, SeqFunctionReturnValueAssignment>
        rStartRoutineExitAssignments = ImmutableMap.builder();
    for (CFAEdgeForThread threadEdge : pThread.cfa().threadEdges) {
      Optional<CFunctionCall> functionCall =
          PthreadUtil.tryGetFunctionCallFromCfaEdge(threadEdge.cfaEdge);
      if (functionCall.isPresent()) {
        if (PthreadUtil.isCallToPthreadExitWithReturnValue(functionCall.orElseThrow())) {
          SubstituteEdge substituteEdge = Objects.requireNonNull(substituteEdges.get(threadEdge));
          CFunctionCall substituteFunctionCall =
              PthreadUtil.tryGetFunctionCallFromCfaEdge(substituteEdge.cfaEdge).orElseThrow();
          CExpressionAssignmentStatement assignmentStatement =
              SeqStatementBuilder.buildExpressionAssignmentStatement(
                  pThread.startRoutineExitVariable().orElseThrow(),
                  PthreadUtil.extractExitReturnValue(substituteFunctionCall));
          CFAEdgeForThread cfaEdgeForThread =
              substituteEdge.getCallContext().cfaEdgeForThread().orElseThrow();
          SeqFunctionReturnValueAssignment assignment =
              new SeqFunctionReturnValueAssignment(
                  assignmentStatement,
                  cfaEdgeForThread.callContext,
                  substituteEdge.getCallContext());
          rStartRoutineExitAssignments.put(threadEdge, assignment);
        }
      }
    }
    return rStartRoutineExitAssignments.buildOrThrow();
  }

  private SubstituteEdge getSubstituteEdgeByCfaEdgeAndCallContext(
      CFAEdge pCfaEdge, SeqCallContext pCallContext) {

    for (CFAEdgeForThread threadEdge : substituteEdges.keySet()) {
      if (threadEdge.cfaEdge.equals(pCfaEdge)) {
        if (threadEdge.callContext.equals(pCallContext)) {
          return Objects.requireNonNull(substituteEdges.get(threadEdge));
        }
      }
    }
    throw new IllegalArgumentException(
        String.format(
            "could not find pCfaEdge of type %s and pCallContext in substituteEdges",
            pCfaEdge.getEdgeType()));
  }
}
