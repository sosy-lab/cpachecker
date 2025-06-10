// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables;

import static org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType.PTHREAD_MUTEX_LOCK;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorReduction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.DenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.ScalarBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function_statements.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function_statements.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function_statements.FunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.MutexLocked;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadSimulationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class GhostVariableUtil {

  // Bit Vectors ===================================================================================

  public static Optional<BitVectorVariables> buildBitVectorVariables(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    if (pOptions.bitVectorReduction.equals(BitVectorReduction.NONE)) {
      return Optional.empty();
    }
    // collect all global variables accessed in substitute edges, and assign unique ids
    ImmutableList<CVariableDeclaration> allGlobalVariables =
        SubstituteUtil.getAllGlobalVariables(pSubstituteEdges.values());
    ImmutableMap<CVariableDeclaration, Integer> globalVariableIds =
        assignGlobalVariableIds(allGlobalVariables);
    if (pOptions.bitVectorReduction.equals(BitVectorReduction.ACCESS_ONLY)) {
      return buildAccessOnlyBitVectorVariables(
          pOptions, pThreads, allGlobalVariables, globalVariableIds);
    } else {
      return buildReadWriteBitVectorVariables(
          pOptions, pThreads, allGlobalVariables, globalVariableIds);
    }
  }

  private static Optional<BitVectorVariables> buildAccessOnlyBitVectorVariables(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableList<CVariableDeclaration> pAllGlobalVariables,
      ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds) {

    // create bit vector access variables for all threads, e.g. __uint8_t ba0
    Optional<ImmutableSet<DenseBitVector>> denseAccessBitVectors =
        buildDenseBitVectorsByAccessType(pOptions, pThreads, BitVectorAccessType.ACCESS);
    // create access variables for all global variables for all threads (for scalar bit vectors)
    Optional<ImmutableMap<CVariableDeclaration, ScalarBitVector>> scalarAccessBitVectors =
        buildScalarBitVectorsByAccessType(
            pOptions, pThreads, pAllGlobalVariables, BitVectorAccessType.ACCESS);
    return Optional.of(
        new BitVectorVariables(
            pGlobalVariableIds,
            denseAccessBitVectors,
            Optional.empty(),
            Optional.empty(),
            scalarAccessBitVectors,
            Optional.empty(),
            Optional.empty()));
  }

  private static Optional<BitVectorVariables> buildReadWriteBitVectorVariables(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableList<CVariableDeclaration> pAllGlobalVariables,
      ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds) {

    // create bit vector read + write variables for all threads, e.g. __uint8_t br0, bw0
    Optional<ImmutableSet<DenseBitVector>> denseReadBitVectors =
        buildDenseBitVectorsByAccessType(pOptions, pThreads, BitVectorAccessType.READ);
    Optional<ImmutableSet<DenseBitVector>> denseWriteBitVectors =
        buildDenseBitVectorsByAccessType(pOptions, pThreads, BitVectorAccessType.WRITE);
    // create read + write variables (for scalar bit vectors)
    Optional<ImmutableMap<CVariableDeclaration, ScalarBitVector>> scalarReadBitVectors =
        buildScalarBitVectorsByAccessType(
            pOptions, pThreads, pAllGlobalVariables, BitVectorAccessType.READ);
    Optional<ImmutableMap<CVariableDeclaration, ScalarBitVector>> scalarWriteBitVectors =
        buildScalarBitVectorsByAccessType(
            pOptions, pThreads, pAllGlobalVariables, BitVectorAccessType.WRITE);
    return Optional.of(
        new BitVectorVariables(
            pGlobalVariableIds,
            Optional.empty(),
            denseReadBitVectors,
            denseWriteBitVectors,
            Optional.empty(),
            scalarReadBitVectors,
            scalarWriteBitVectors));
  }

  private static ImmutableMap<CVariableDeclaration, Integer> assignGlobalVariableIds(
      ImmutableList<CVariableDeclaration> pAllGlobalVariables) {

    ImmutableMap.Builder<CVariableDeclaration, Integer> rVariables = ImmutableMap.builder();
    int id = BitVectorUtil.RIGHT_INDEX;
    for (CVariableDeclaration variableDeclaration : pAllGlobalVariables) {
      assert variableDeclaration.isGlobal() : "variable declaration must be global";
      rVariables.put(variableDeclaration, id++);
    }
    return rVariables.buildOrThrow();
  }

  // Dense / Scalar Bit Vectors ====================================================================

  private static Optional<ImmutableSet<DenseBitVector>> buildDenseBitVectorsByAccessType(
      MPOROptions pOptions, ImmutableList<MPORThread> pThreads, BitVectorAccessType pAccessType) {

    if (!pOptions.bitVectorEncoding.isDense) {
      return Optional.empty();
    }
    ImmutableSet.Builder<DenseBitVector> rBitVectors = ImmutableSet.builder();
    for (MPORThread thread : pThreads) {
      String directReadName =
          SeqNameUtil.buildDirectBitVectorNameByAccessType(pOptions, thread.id, pAccessType);
      String reachableReadName =
          SeqNameUtil.buildReachableBitVectorNameByAccessType(pOptions, thread.id, pAccessType);
      // these declarations are not actually used, we only need it for the CIdExpression
      CVariableDeclaration directReadDeclaration =
          SeqDeclarationBuilder.buildVariableDeclaration(
              false, SeqSimpleType.INT, directReadName, SeqInitializer.INT_0);
      CVariableDeclaration reachableReadDeclaration =
          SeqDeclarationBuilder.buildVariableDeclaration(
              false, SeqSimpleType.INT, reachableReadName, SeqInitializer.INT_0);
      rBitVectors.add(
          new DenseBitVector(
              thread,
              SeqExpressionBuilder.buildIdExpression(directReadDeclaration),
              SeqExpressionBuilder.buildIdExpression(reachableReadDeclaration),
              pAccessType,
              pOptions.bitVectorEncoding));
    }
    return Optional.of(rBitVectors.build());
  }

  private static Optional<ImmutableMap<CVariableDeclaration, ScalarBitVector>>
      buildScalarBitVectorsByAccessType(
          MPOROptions pOptions,
          ImmutableList<MPORThread> pThreads,
          ImmutableList<CVariableDeclaration> pAllGlobalVariables,
          BitVectorAccessType pAccessType) {

    if (pOptions.bitVectorEncoding.isDense) {
      return Optional.empty();
    }
    ImmutableMap.Builder<CVariableDeclaration, ScalarBitVector> rAccessVariables =
        ImmutableMap.builder();
    for (CVariableDeclaration variableDeclaration : pAllGlobalVariables) {
      assert variableDeclaration.isGlobal() : "variable declaration for bit vector must be global";
      ImmutableMap<MPORThread, CIdExpression> variables =
          buildScalarBitVectorVariablesByAccessType(
              pOptions, pThreads, variableDeclaration, pAccessType);
      rAccessVariables.put(variableDeclaration, new ScalarBitVector(variables, pAccessType));
    }
    return Optional.of(rAccessVariables.buildOrThrow());
  }

  private static ImmutableMap<MPORThread, CIdExpression> buildScalarBitVectorVariablesByAccessType(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      CVariableDeclaration pVariableDeclaration,
      BitVectorAccessType pAccessType) {

    ImmutableMap.Builder<MPORThread, CIdExpression> rAccessVariables = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      rAccessVariables.put(
          thread,
          BitVectorUtil.createScalarAccessVariable(
              pOptions, thread, pVariableDeclaration, pAccessType));
    }
    return rAccessVariables.buildOrThrow();
  }

  // Function Variables ============================================================================

  public static FunctionStatements buildFunctionVariables(
      MPORThread pThread,
      MPORSubstitution pSubstitution,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    return new FunctionStatements(
        buildParameterAssignments(pSubstitution),
        buildStartRoutineArgumentAssignments(pSubstitution),
        buildReturnValueAssignments(pThread, pSubstituteEdges),
        buildStartRoutineExitAssignments(pThread, pSubstituteEdges));
  }

  public static ThreadSimulationVariables buildThreadSimulationVariables(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    return new ThreadSimulationVariables(
        buildMutexLockedVariables(pOptions, pThreads, pSubstituteEdges));
  }

  private static ImmutableMap<CIdExpression, MutexLocked> buildMutexLockedVariables(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    ImmutableMap.Builder<CIdExpression, MutexLocked> rVars = ImmutableMap.builder();
    Set<CIdExpression> lockedVariables = new HashSet<>();
    for (MPORThread thread : pThreads) {
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        if (pSubstituteEdges.containsKey(threadEdge)) {
          SubstituteEdge substituteEdge = Objects.requireNonNull(pSubstituteEdges.get(threadEdge));
          CFAEdge cfaEdge = substituteEdge.cfaEdge;

          // extract pthread_mutex_t based on function calls to pthread_mutex_lock
          if (PthreadUtil.callsPthreadFunction(cfaEdge, PTHREAD_MUTEX_LOCK)) {
            CIdExpression pthreadMutexT = PthreadUtil.extractPthreadMutexT(threadEdge.cfaEdge);
            if (lockedVariables.add(pthreadMutexT)) { // add mutex only once
              String varName = SeqNameUtil.buildMutexLockedName(pOptions, pthreadMutexT.getName());
              // use unsigned char (8 bit), we only need values 0 and 1
              CIdExpression mutexLocked =
                  SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
                      false, BitVectorDataType.__UINT8_T.simpleType, varName, SeqInitializer.INT_0);
              rVars.put(pthreadMutexT, new MutexLocked(mutexLocked));
            }
          }
        }
      }
    }
    return rVars.buildOrThrow();
  }

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge} is a {@link CFunctionCallEdge} to a list of
   * {@link FunctionParameterAssignment}s.
   *
   * <p>E.g. {@code func(&paramA, paramB);} in thread 0 is linked to {@code __t0_0_paramA = &paramA
   * ;} and {@code __t0_1_paramB = paramB ;}. Both substitution variables are declared in {@link
   * MPORSubstitution#parameterSubstitutes}.
   */
  private static ImmutableMap<ThreadEdge, ImmutableList<FunctionParameterAssignment>>
      buildParameterAssignments(MPORSubstitution pSubstitution) {

    ImmutableMap.Builder<ThreadEdge, ImmutableList<FunctionParameterAssignment>> rAssignments =
        ImmutableMap.builder();

    // for each function call edge (= calling context)
    for (var entryA : pSubstitution.parameterSubstitutes.entrySet()) {
      ThreadEdge threadEdge = entryA.getKey();
      assert threadEdge.cfaEdge instanceof CFunctionCallEdge;
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) entryA.getKey().cfaEdge;

      ImmutableList.Builder<FunctionParameterAssignment> assignments = ImmutableList.builder();
      List<CParameterDeclaration> parameterDeclarations =
          functionCallEdge.getSuccessor().getFunctionDefinition().getParameters();

      // for each parameter, assign the param substitute to the param expression in funcCall
      for (int i = 0; i < parameterDeclarations.size(); i++) {
        CParameterDeclaration parameterDeclaration = parameterDeclarations.get(i);
        CExpression rightHandSide =
            functionCallEdge.getFunctionCallExpression().getParameterExpressions().get(i);
        CIdExpression parameterSubstitute =
            pSubstitution.getParameterSubstituteByCallContext(threadEdge, parameterDeclaration);
        FunctionParameterAssignment parameterAssignment =
            new FunctionParameterAssignment(
                SeqStatementBuilder.buildExpressionAssignmentStatement(
                    parameterSubstitute,
                    pSubstitution.substitute(
                        rightHandSide,
                        threadEdge.callContext,
                        false,
                        false,
                        Optional.empty(),
                        Optional.empty(),
                        ImmutableSet.builder())));
        assignments.add(parameterAssignment);
      }
      rAssignments.put(threadEdge, assignments.build());
    }
    return rAssignments.buildOrThrow();
  }

  private static ImmutableMap<ThreadEdge, FunctionParameterAssignment>
      buildStartRoutineArgumentAssignments(MPORSubstitution pSubstitution) {

    ImmutableMap.Builder<ThreadEdge, FunctionParameterAssignment> rAssignments =
        ImmutableMap.builder();
    for (var entry : pSubstitution.startRoutineArgSubstitutes.entrySet()) {
      // this call context is the call to pthread_create
      ThreadEdge callContext = entry.getKey();
      // only the thread calling pthread_create assigns the start_routine arg
      if (pSubstitution.thread.id == callContext.threadId) {
        assert entry.getValue().size() == 1 : "start_routines must have exactly 1 parameter";
        for (CIdExpression parameterSubstitute : entry.getValue().values()) {
          CExpression rightHandSide = PthreadUtil.extractStartRoutineArgument(callContext.cfaEdge);
          FunctionParameterAssignment parameterAssignment =
              new FunctionParameterAssignment(
                  SeqStatementBuilder.buildExpressionAssignmentStatement(
                      parameterSubstitute,
                      pSubstitution.substitute(
                          // the inner call context is the context in which pthread_create is called
                          rightHandSide,
                          callContext.callContext,
                          false,
                          false,
                          Optional.empty(),
                          Optional.empty(),
                          ImmutableSet.builder())));
          rAssignments.put(callContext, parameterAssignment);
        }
      }
    }
    return rAssignments.buildOrThrow();
  }

  /**
   * Maps {@link ThreadEdge}s whose {@link CFAEdge} is a {@link CReturnStatementEdge} to {@link
   * FunctionReturnValueAssignment}s where the CPAchecker_TMP vars are assigned the return value.
   *
   * <p>The return statement may be linked to multiple function calls, thus the inner set.
   *
   * <p>Note that {@code main} functions and start_routines of threads oftentimes do not have
   * corresponding {@link CFunctionSummaryEdge}s.
   */
  private static ImmutableMap<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>>
      buildReturnValueAssignments(
          MPORThread pThread, ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    ImmutableMap.Builder<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>>
        rReturnStatements = ImmutableMap.builder();
    for (ThreadEdge threadEdgeA : pThread.cfa.threadEdges) {
      if (pSubstituteEdges.containsKey(threadEdgeA)) {
        SubstituteEdge substituteEdgeA = Objects.requireNonNull(pSubstituteEdges.get(threadEdgeA));

        if (substituteEdgeA.cfaEdge instanceof CReturnStatementEdge returnStatementEdge) {
          ImmutableSet.Builder<FunctionReturnValueAssignment> assigns = ImmutableSet.builder();
          for (ThreadEdge threadEdgeB : pThread.cfa.threadEdges) {
            if (pSubstituteEdges.containsKey(threadEdgeB)) {
              SubstituteEdge substituteEdgeB =
                  Objects.requireNonNull(pSubstituteEdges.get(threadEdgeB));

              if (substituteEdgeB.cfaEdge instanceof CFunctionSummaryEdge functionSummary) {
                // if the summary edge is of the form value = func(); (i.e. an assignment)
                if (functionSummary.getExpression()
                    instanceof CFunctionCallAssignmentStatement assignmentStatement) {
                  AFunctionDeclaration functionDeclarationA =
                      returnStatementEdge.getSuccessor().getFunction();
                  AFunctionType functionTypeA = functionDeclarationA.getType();
                  AFunctionType functionTypeB =
                      functionSummary.getFunctionEntry().getFunction().getType();
                  // TODO compare declarations here instead?
                  if (functionTypeA.equals(functionTypeB)) {
                    assert functionDeclarationA instanceof CFunctionDeclaration;
                    FunctionReturnValueAssignment assign =
                        new FunctionReturnValueAssignment(
                            assignmentStatement.getLeftHandSide(),
                            returnStatementEdge.getExpression().orElseThrow());
                    assigns.add(assign);
                  }
                }
              }
            }
          }
          rReturnStatements.put(threadEdgeA, assigns.build());
        }
      }
    }
    return rReturnStatements.buildOrThrow();
  }

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
