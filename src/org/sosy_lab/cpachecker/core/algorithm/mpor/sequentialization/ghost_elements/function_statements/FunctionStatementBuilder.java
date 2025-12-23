// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitutionTracker;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record FunctionStatementBuilder(
    ImmutableList<MPORThread> threads,
    ImmutableList<MPORSubstitution> substitutions,
    ImmutableMap<CFAEdgeForThread, SubstituteEdge> substituteEdges) {

  public ImmutableMap<MPORThread, FunctionStatements> buildFunctionStatements()
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<MPORThread, FunctionStatements> rFunctionStatements =
        ImmutableMap.builder();
    for (MPORSubstitution substitution : substitutions) {
      for (MPORThread thread : threads) {
        if (substitution.thread.equals(thread)) {
          rFunctionStatements.put(thread, buildFunctionStatements(thread, substitution));
        }
      }
    }
    return rFunctionStatements.buildOrThrow();
  }

  private FunctionStatements buildFunctionStatements(
      MPORThread pThread, MPORSubstitution pSubstitution) throws UnrecognizedCodeException {

    return new FunctionStatements(
        buildParameterAssignments(pSubstitution),
        buildStartRoutineArgAssignments(pSubstitution),
        buildReturnValueAssignments(pThread),
        buildStartRoutineExitAssignments(pThread));
  }

  // Function Parameter Assignments ================================================================

  /**
   * Maps {@link CFAEdgeForThread}s whose {@link CFAEdge} is a {@link CFunctionCallEdge} to a list
   * of {@link FunctionParameterAssignment}s.
   *
   * <p>E.g. {@code func(&paramA, paramB);} in thread 0 is linked to {@code __t0_0_paramA = &paramA
   * ;} and {@code __t0_1_paramB = paramB ;}. Both substitution variables are declared in {@link
   * MPORSubstitution#parameterSubstitutes}.
   */
  private ImmutableListMultimap<CFAEdgeForThread, FunctionParameterAssignment>
      buildParameterAssignments(MPORSubstitution pSubstitution) throws UnrecognizedCodeException {

    ImmutableListMultimap.Builder<CFAEdgeForThread, FunctionParameterAssignment> rAssignments =
        ImmutableListMultimap.builder();
    // for each function call edge (= calling context)
    for (CFAEdgeForThread callContext : pSubstitution.parameterSubstitutes.rowKeySet()) {
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) callContext.cfaEdge;
      rAssignments.putAll(
          callContext,
          buildFunctionParameterAssignments(functionCallEdge, callContext, pSubstitution));
    }
    return rAssignments.build();
  }

  private ImmutableList<FunctionParameterAssignment> buildFunctionParameterAssignments(
      CFunctionCallEdge pFunctionCallEdge,
      CFAEdgeForThread pCallContext,
      MPORSubstitution pSubstitution)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<FunctionParameterAssignment> rAssignments = ImmutableList.builder();
    CFunctionDeclaration functionDeclaration =
        pFunctionCallEdge.getFunctionCallExpression().getDeclaration();
    for (int i = 0; i < functionDeclaration.getParameters().size(); i++) {
      CParameterDeclaration parameterDeclaration =
          MPORUtil.getParameterDeclarationByIndex(i, functionDeclaration);
      ImmutableList<CIdExpression> parameterSubstitutes =
          pSubstitution.getParameterDeclarationSubstitute(pCallContext, parameterDeclaration);
      // go through all parameter substitutes. if there is more than one, the function is variadic
      for (int j = 0; j < parameterSubstitutes.size(); j++) {
        CExpression argument = pFunctionCallEdge.getArguments().get(i + j);
        MPORSubstitutionTracker dummyTracker = new MPORSubstitutionTracker();
        CExpression argumentSubstitute =
            pSubstitution.substitute(
                // use "outer" call context, since pCallContext is the given pFunctionCallEdge
                argument, pCallContext.callContext, false, false, false, false, dummyTracker);
        CIdExpression parameterSubstitute = parameterSubstitutes.get(j);
        rAssignments.add(
            new FunctionParameterAssignment(pCallContext, parameterSubstitute, argumentSubstitute));
      }
    }
    return rAssignments.build();
  }

  private ImmutableMap<CFAEdgeForThread, FunctionParameterAssignment>
      buildStartRoutineArgAssignments(MPORSubstitution pSubstitution)
          throws UnrecognizedCodeException {

    ImmutableMap.Builder<CFAEdgeForThread, FunctionParameterAssignment> rAssignments =
        ImmutableMap.builder();
    Set<CFAEdgeForThread> visited = new HashSet<>();
    for (var cell : pSubstitution.startRoutineArgSubstitutes.cellSet()) {
      // this call context is the call to pthread_create
      CFAEdgeForThread callContext = cell.getRowKey();
      if (visited.add(callContext)) {
        // only the thread calling pthread_create assigns the start_routine arg
        if (pSubstitution.thread.id() == callContext.threadId) {
          CIdExpression leftHandSide = cell.getValue();
          Optional<CFunctionCall> functionCall =
              PthreadUtil.tryGetFunctionCallFromCfaEdge(callContext.cfaEdge);
          if (functionCall.isPresent()) {
            CExpression rightHandSide =
                PthreadUtil.extractStartRoutineArg(functionCall.orElseThrow());
            MPORSubstitutionTracker dummyTracker = new MPORSubstitutionTracker();
            FunctionParameterAssignment startRoutineArgAssignment =
                new FunctionParameterAssignment(
                    callContext,
                    leftHandSide,
                    pSubstitution.substitute(
                        // the inner call context is the context in which pthread_create is called
                        rightHandSide,
                        callContext.callContext,
                        false,
                        false,
                        false,
                        false,
                        dummyTracker));
            rAssignments.put(callContext, startRoutineArgAssignment);
          }
        }
      }
    }
    return rAssignments.buildOrThrow();
  }

  // return value assignments ======================================================================

  /**
   * Maps {@link CFAEdgeForThread}s whose {@link CFAEdge} is a {@link CReturnStatementEdge} to
   * {@link FunctionReturnValueAssignment}s where the CPAchecker_TMP vars are assigned the return
   * value.
   *
   * <p>The return statement may be linked to multiple function calls, thus the inner set.
   *
   * <p>Note that {@code main} functions and start_routines of threads oftentimes do not have
   * corresponding {@link CFunctionSummaryEdge}s.
   */
  private ImmutableMap<CFAEdgeForThread, FunctionReturnValueAssignment> buildReturnValueAssignments(
      MPORThread pThread) {

    ImmutableMap.Builder<CFAEdgeForThread, FunctionReturnValueAssignment> rReturnStatements =
        ImmutableMap.builder();
    for (CFAEdgeForThread threadEdge : pThread.cfa().threadEdges) {
      assert substituteEdges.containsKey(threadEdge)
          : "substituteEdges must contain all threadEdges";
      // consider only edges with call context, e.g. return 0; in main has no call context
      if (threadEdge.callContext.isPresent()) {
        SubstituteEdge substituteEdge = Objects.requireNonNull(substituteEdges.get(threadEdge));
        if (substituteEdge.cfaEdge instanceof CReturnStatementEdge returnStatementEdge) {
          CFAEdgeForThread callContext = threadEdge.callContext.orElseThrow();
          Optional<SubstituteEdge> functionSummaryEdge =
              tryGetFunctionSummaryEdgeByReturnStatementEdge(
                  pThread, returnStatementEdge, callContext);
          if (functionSummaryEdge.isPresent()) {
            Optional<FunctionReturnValueAssignment> assignment =
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

    for (CFAEdgeForThread threadEdge : pThread.cfa().threadEdges) {
      assert substituteEdges.containsKey(threadEdge)
          : "substituteEdges must contain all threadEdges";
      // consider only threadEdges with callContext, CReturnStatementEdges always have call contexts
      if (threadEdge.callContext.isPresent()) {
        CFAEdgeForThread callContext = threadEdge.callContext.orElseThrow();
        if (callContext.equals(pCallContext)) {
          FluentIterable<? extends FunctionCallEdge> functionCallEdges =
              pReturnStatementEdge.getSuccessor().getEntryNode().getEnteringCallEdges();
          for (FunctionCallEdge functionCallEdge : functionCallEdges) {
            if (callContext.cfaEdge.equals(functionCallEdge)) {
              // use the call contexts predecessor, which is used by the functionSummaryEdge
              Optional<CFAEdgeForThread> predecessorCallContext =
                  callContext.getPredecessor().callContext;
              return Optional.of(
                  getSubstituteEdgeByCfaEdgeAndCallContext(
                      functionCallEdge.getSummaryEdge(), predecessorCallContext));
            }
          }
        }
      }
    }
    // happens e.g. for return 0; in the start_routine
    return Optional.empty();
  }

  private Optional<FunctionReturnValueAssignment> tryBuildReturnValueAssignment(
      SubstituteEdge pFunctionSummaryEdge, CReturnStatementEdge pReturnStatementEdge) {

    assert pFunctionSummaryEdge.cfaEdge instanceof CFunctionSummaryEdge;
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
        return Optional.of(new FunctionReturnValueAssignment(assignmentStatement));
      }
    }
    return Optional.empty();
  }

  // start_routine exit ============================================================================

  /**
   * Links {@link CFAEdgeForThread}s that call {@code pthread_exit} to {@link
   * FunctionReturnValueAssignment} where the {@code retval} is stored in an intermediate value that
   * can be retrieved by other threads calling {@code pthread_join}.
   */
  private ImmutableMap<CFAEdgeForThread, FunctionReturnValueAssignment>
      buildStartRoutineExitAssignments(MPORThread pThread) {

    ImmutableMap.Builder<CFAEdgeForThread, FunctionReturnValueAssignment>
        rStartRoutineExitAssignments = ImmutableMap.builder();
    for (CFAEdgeForThread threadEdge : pThread.cfa().threadEdges) {
      PthreadUtil.tryGetFunctionCallFromCfaEdge(threadEdge.cfaEdge)
          .ifPresent(
              functionCall -> {
                if (PthreadUtil.isCallToPthreadFunction(
                    functionCall, PthreadFunctionType.PTHREAD_EXIT)) {
                  assert pThread.startRoutineExitVariable().isPresent()
                      : "thread calls pthread_exit but has no intermediateExitVariable";
                  SubstituteEdge substituteEdge =
                      Objects.requireNonNull(substituteEdges.get(threadEdge));
                  CFunctionCall substituteFunctionCall =
                      PthreadUtil.tryGetFunctionCallFromCfaEdge(substituteEdge.cfaEdge)
                          .orElseThrow();
                  CExpressionAssignmentStatement assignmentStatement =
                      SeqStatementBuilder.buildExpressionAssignmentStatement(
                          pThread.startRoutineExitVariable().orElseThrow(),
                          PthreadUtil.extractExitReturnValue(substituteFunctionCall));
                  FunctionReturnValueAssignment assignment =
                      new FunctionReturnValueAssignment(assignmentStatement);
                  rStartRoutineExitAssignments.put(threadEdge, assignment);
                }
              });
    }
    return rStartRoutineExitAssignments.buildOrThrow();
  }

  private SubstituteEdge getSubstituteEdgeByCfaEdgeAndCallContext(
      CFAEdge pCfaEdge, Optional<CFAEdgeForThread> pCallContext) {

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
