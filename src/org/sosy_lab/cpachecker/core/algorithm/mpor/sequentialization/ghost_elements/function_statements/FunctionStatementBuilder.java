// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class FunctionStatementBuilder {

  // Function Statements ===========================================================================

  public static ImmutableMap<MPORThread, FunctionStatements> buildFunctionStatements(
      ImmutableList<MPORThread> pThreads,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    ImmutableMap.Builder<MPORThread, FunctionStatements> rFunctionStatements =
        ImmutableMap.builder();
    for (MPORSubstitution substitution : pSubstitutions) {
      for (MPORThread thread : pThreads) {
        if (substitution.thread.equals(thread)) {
          rFunctionStatements.put(
              thread, buildFunctionStatements(thread, substitution, pSubstituteEdges));
        }
      }
    }
    return rFunctionStatements.buildOrThrow();
  }

  private static FunctionStatements buildFunctionStatements(
      MPORThread pThread,
      MPORSubstitution pSubstitution,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    return new FunctionStatements(
        buildParameterAssignments(pSubstitution),
        buildStartRoutineArgAssignments(pSubstitution),
        buildReturnValueAssignments(pThread, pSubstituteEdges),
        buildStartRoutineExitAssignments(pThread, pSubstituteEdges));
  }

  // Function Parameter Assignments ================================================================

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge} is a {@link CFunctionCallEdge} to a list of
   * {@link FunctionParameterAssignment}s.
   *
   * <p>E.g. {@code func(&paramA, paramB);} in thread 0 is linked to {@code __t0_0_paramA = &paramA
   * ;} and {@code __t0_1_paramB = paramB ;}. Both substitution variables are declared in {@link
   * MPORSubstitution#parameterSubstitutes}.
   */
  private static ImmutableListMultimap<ThreadEdge, FunctionParameterAssignment>
      buildParameterAssignments(MPORSubstitution pSubstitution) {

    ImmutableListMultimap.Builder<ThreadEdge, FunctionParameterAssignment> rAssignments =
        ImmutableListMultimap.builder();

    // for each function call edge (= calling context)
    for (ThreadEdge callContext : pSubstitution.parameterSubstitutes.rowKeySet()) {
      assert callContext.cfaEdge instanceof CFunctionCallEdge;
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) callContext.cfaEdge;
      List<CParameterDeclaration> parameterDeclarations =
          functionCallEdge.getSuccessor().getFunctionDefinition().getParameters();

      // for each parameter, assign the param substitute to the param expression in funcCall
      for (int i = 0; i < parameterDeclarations.size(); i++) {
        CParameterDeclaration parameterDeclaration = parameterDeclarations.get(i);
        CExpression rightHandSide =
            functionCallEdge.getFunctionCallExpression().getParameterExpressions().get(i);
        CIdExpression parameterSubstitute =
            pSubstitution.getSubstituteParameterDeclarationByCallContext(
                callContext, parameterDeclaration);
        FunctionParameterAssignment parameterAssignment =
            new FunctionParameterAssignment(
                callContext,
                parameterSubstitute,
                pSubstitution.substitute(
                    rightHandSide,
                    callContext.callContext,
                    false,
                    false,
                    false,
                    false,
                    false,
                    Optional.empty()));
        rAssignments.put(callContext, parameterAssignment);
      }
    }
    return rAssignments.build();
  }

  private static ImmutableMap<ThreadEdge, FunctionParameterAssignment>
      buildStartRoutineArgAssignments(MPORSubstitution pSubstitution) {

    ImmutableMap.Builder<ThreadEdge, FunctionParameterAssignment> rAssignments =
        ImmutableMap.builder();
    Set<ThreadEdge> visited = new HashSet<>();
    for (var cell : pSubstitution.startRoutineArgSubstitutes.cellSet()) {
      // this call context is the call to pthread_create
      ThreadEdge callContext = cell.getRowKey();
      if (visited.add(callContext)) {
        // only the thread calling pthread_create assigns the start_routine arg
        if (pSubstitution.thread.id == callContext.threadId) {
          CIdExpression leftHandSide = cell.getValue();
          CExpression rightHandSide = PthreadUtil.extractStartRoutineArg(callContext.cfaEdge);
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
                      false,
                      Optional.empty()));
          rAssignments.put(callContext, startRoutineArgAssignment);
        }
      }
    }
    return rAssignments.buildOrThrow();
  }

  // return value assignments ======================================================================

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge} is a {@link CReturnStatementEdge} to {@link
   * FunctionReturnValueAssignment}s where the CPAchecker_TMP vars are assigned the return value.
   *
   * <p>The return statement may be linked to multiple function calls, thus the inner set.
   *
   * <p>Note that {@code main} functions and start_routines of threads oftentimes do not have
   * corresponding {@link CFunctionSummaryEdge}s.
   */
  private static ImmutableMap<ThreadEdge, FunctionReturnValueAssignment>
      buildReturnValueAssignments(
          MPORThread pThread, ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    ImmutableMap.Builder<ThreadEdge, FunctionReturnValueAssignment> rReturnStatements =
        ImmutableMap.builder();
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      assert pSubstituteEdges.containsKey(threadEdge)
          : "pSubstituteEdges must contain all threadEdges";
      SubstituteEdge substituteEdge = Objects.requireNonNull(pSubstituteEdges.get(threadEdge));
      if (substituteEdge.cfaEdge instanceof CReturnStatementEdge returnStatementEdge) {
        Optional<SubstituteEdge> functionSummaryEdge =
            tryGetFunctionSummaryEdgeByReturnStatementEdge(
                pThread,
                pSubstituteEdges,
                returnStatementEdge,
                threadEdge.callContext.orElseThrow());
        if (functionSummaryEdge.isPresent()) {
          Optional<FunctionReturnValueAssignment> assignment =
              tryBuildReturnValueAssignment(functionSummaryEdge.orElseThrow(), returnStatementEdge);
          if (assignment.isPresent()) {
            rReturnStatements.put(threadEdge, assignment.orElseThrow());
          }
        }
      }
    }
    return rReturnStatements.buildOrThrow();
  }

  private static Optional<SubstituteEdge> tryGetFunctionSummaryEdgeByReturnStatementEdge(
      MPORThread pThread,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      CReturnStatementEdge pReturnStatementEdge,
      ThreadEdge pCallContext) {

    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      assert pSubstituteEdges.containsKey(threadEdge)
          : "pSubstituteEdges must contain all threadEdges";
      // consider only threadEdges with callContext, CReturnStatementEdges always have call contexts
      if (threadEdge.callContext.isPresent()) {
        ThreadEdge callContext = threadEdge.callContext.orElseThrow();
        if (callContext.equals(pCallContext)) {
          ImmutableSet<CFunctionCallEdge> functionCallEdges =
              CFAUtils.getFunctionCallEdgesByReturnStatementEdge(pReturnStatementEdge);
          for (CFunctionCallEdge functionCallEdge : functionCallEdges) {
            if (callContext.cfaEdge.equals(functionCallEdge)) {
              // use the call contexts predecessor, which is used by the functionSummaryEdge
              Optional<ThreadEdge> predecessorCallContext =
                  callContext.getPredecessor().callContext;
              return Optional.of(
                  SubstituteUtil.getSubstituteEdgeByCfaEdgeAndCallContext(
                      functionCallEdge.getSummaryEdge(), predecessorCallContext, pSubstituteEdges));
            }
          }
        }
      }
    }
    // happens e.g. for return 0; in the start_routine
    return Optional.empty();
  }

  private static Optional<FunctionReturnValueAssignment> tryBuildReturnValueAssignment(
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
        return Optional.of(
            new FunctionReturnValueAssignment(
                functionCallAssignmentStatement.getLeftHandSide(),
                pReturnStatementEdge.getExpression().orElseThrow()));
      }
    }
    return Optional.empty();
  }

  // start_routine exit ============================================================================

  /**
   * Links {@link ThreadEdge}s that call {@code pthread_exit} to {@link
   * FunctionReturnValueAssignment} where the {@code retval} is stored in an intermediate value that
   * can be retrieved by other threads calling {@code pthread_join}.
   */
  private static ImmutableMap<ThreadEdge, FunctionReturnValueAssignment>
      buildStartRoutineExitAssignments(
          MPORThread pThread, ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    ImmutableMap.Builder<ThreadEdge, FunctionReturnValueAssignment> rStartRoutineExitAssignments =
        ImmutableMap.builder();
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (PthreadUtil.callsPthreadFunction(threadEdge.cfaEdge, PthreadFunctionType.PTHREAD_EXIT)) {
        assert pThread.startRoutineExitVariable.isPresent()
            : "thread calls pthread_exit but has no intermediateExitVariable";
        SubstituteEdge substituteEdge = Objects.requireNonNull(pSubstituteEdges.get(threadEdge));
        FunctionReturnValueAssignment assignment =
            new FunctionReturnValueAssignment(
                pThread.startRoutineExitVariable.orElseThrow(),
                PthreadUtil.extractExitReturnValue(substituteEdge.cfaEdge));
        rStartRoutineExitAssignments.put(threadEdge, assignment);
      }
    }
    return rStartRoutineExitAssignments.buildOrThrow();
  }
}
