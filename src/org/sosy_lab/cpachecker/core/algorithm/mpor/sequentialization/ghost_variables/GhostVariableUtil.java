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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.DenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.SparseBitVector;
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
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class GhostVariableUtil {

  // Bit Vectors ===================================================================================

  public static Optional<BitVectorVariables> buildBitVectorVariables(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    if (!pOptions.areBitVectorsEnabled()) {
      // no bit vector reduction -> no bit vector variables
      return Optional.empty();
    }
    // collect all global variables accessed in substitute edges, and assign unique ids
    ImmutableList<CVariableDeclaration> allGlobalVariables =
        SubstituteUtil.getAllGlobalVariables(pSubstituteEdges.values());
    ImmutableMap<CVariableDeclaration, Integer> globalVariableIds =
        assignGlobalVariableIds(allGlobalVariables);

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "reductionMode is not set, cannot build bit vector variables");
      case ACCESS_ONLY ->
          buildAccessOnlyBitVectorVariables(
              pOptions, pThreads, allGlobalVariables, globalVariableIds);
      case READ_AND_WRITE ->
          buildReadWriteBitVectorVariables(
              pOptions, pThreads, allGlobalVariables, globalVariableIds);
    };
  }

  private static Optional<BitVectorVariables> buildAccessOnlyBitVectorVariables(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableList<CVariableDeclaration> pAllGlobalVariables,
      ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds) {

    // create bit vector access variables for all threads, e.g. __uint8_t ba0
    Optional<ImmutableSet<DenseBitVector>> denseAccessBitVectors =
        buildDenseBitVectorsByAccessType(pOptions, pThreads, BitVectorAccessType.ACCESS);
    // create access variables for all global variables for all threads (for sparse bit vectors)
    Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>> sparseAccessBitVectors =
        buildSparseBitVectorsByAccessType(
            pOptions, pThreads, pAllGlobalVariables, BitVectorAccessType.ACCESS);
    return Optional.of(
        new BitVectorVariables(
            pGlobalVariableIds,
            denseAccessBitVectors,
            Optional.empty(),
            Optional.empty(),
            sparseAccessBitVectors,
            Optional.empty()));
  }

  private static Optional<BitVectorVariables> buildReadWriteBitVectorVariables(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableList<CVariableDeclaration> pAllGlobalVariables,
      ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds) {

    // create bit vector read + write variables for all threads, e.g. __uint8_t br0, bw0
    Optional<ImmutableSet<DenseBitVector>> denseAccessBitVectors =
        buildDenseBitVectorsByAccessType(pOptions, pThreads, BitVectorAccessType.ACCESS);
    Optional<ImmutableSet<DenseBitVector>> denseReadBitVectors =
        buildDenseBitVectorsByAccessType(pOptions, pThreads, BitVectorAccessType.READ);
    Optional<ImmutableSet<DenseBitVector>> denseWriteBitVectors =
        buildDenseBitVectorsByAccessType(pOptions, pThreads, BitVectorAccessType.WRITE);
    // create read + write variables (for sparse bit vectors)
    Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>> sparseAccessBitVectors =
        buildSparseBitVectorsByAccessType(
            pOptions, pThreads, pAllGlobalVariables, BitVectorAccessType.ACCESS);
    Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>> sparseWriteBitVectors =
        buildSparseBitVectorsByAccessType(
            pOptions, pThreads, pAllGlobalVariables, BitVectorAccessType.WRITE);
    return Optional.of(
        new BitVectorVariables(
            pGlobalVariableIds,
            denseAccessBitVectors,
            denseReadBitVectors,
            denseWriteBitVectors,
            sparseAccessBitVectors,
            sparseWriteBitVectors));
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

  // Dense / Sparse Bit Vectors ====================================================================

  private static Optional<ImmutableSet<DenseBitVector>> buildDenseBitVectorsByAccessType(
      MPOROptions pOptions, ImmutableList<MPORThread> pThreads, BitVectorAccessType pAccessType) {

    if (!pOptions.bitVectorEncoding.isDense) {
      return Optional.empty();
    }
    ImmutableSet.Builder<DenseBitVector> rBitVectors = ImmutableSet.builder();
    for (MPORThread thread : pThreads) {
      Optional<CIdExpression> directVariable =
          buildDenseDirectBitVectorByAccessType(pOptions, thread, pAccessType);
      Optional<CIdExpression> reachableVariable =
          buildDenseReachableBitVectorByAccessType(pOptions, thread, pAccessType);
      rBitVectors.add(
          new DenseBitVector(
              thread, directVariable, reachableVariable, pAccessType, pOptions.bitVectorEncoding));
    }
    return Optional.of(rBitVectors.build());
  }

  private static Optional<CIdExpression> buildDenseDirectBitVectorByAccessType(
      MPOROptions pOptions, MPORThread pThread, BitVectorAccessType pAccessType) {

    if (pOptions.kIgnoreZeroReduction) {
      String directReadName =
          SeqNameUtil.buildDirectBitVectorNameByAccessType(pOptions, pThread.id, pAccessType);
      // these declarations are not actually used, we only need it for the CIdExpression
      CVariableDeclaration directDeclaration =
          SeqDeclarationBuilder.buildVariableDeclaration(
              false, SeqSimpleType.INT, directReadName, SeqInitializer.INT_0);
      return Optional.of(SeqExpressionBuilder.buildIdExpression(directDeclaration));
    }
    return Optional.empty();
  }

  private static Optional<CIdExpression> buildDenseReachableBitVectorByAccessType(
      MPOROptions pOptions, MPORThread pThread, BitVectorAccessType pAccessType) {

    if (pAccessType.equals(BitVectorAccessType.READ)) {
      // we never need reachable read bit vectors
      return Optional.empty();
    }
    String reachableReadName =
        SeqNameUtil.buildReachableBitVectorNameByAccessType(pOptions, pThread.id, pAccessType);
    // these declarations are not actually used, we only need it for the CIdExpression
    CVariableDeclaration reachableDeclaration =
        SeqDeclarationBuilder.buildVariableDeclaration(
            false, SeqSimpleType.INT, reachableReadName, SeqInitializer.INT_0);
    return Optional.of(SeqExpressionBuilder.buildIdExpression(reachableDeclaration));
  }

  private static Optional<ImmutableMap<CVariableDeclaration, SparseBitVector>>
      buildSparseBitVectorsByAccessType(
          MPOROptions pOptions,
          ImmutableList<MPORThread> pThreads,
          ImmutableList<CVariableDeclaration> pAllGlobalVariables,
          BitVectorAccessType pAccessType) {

    if (pOptions.bitVectorEncoding.isDense) {
      return Optional.empty();
    }
    ImmutableMap.Builder<CVariableDeclaration, SparseBitVector> rAccessVariables =
        ImmutableMap.builder();
    for (CVariableDeclaration variableDeclaration : pAllGlobalVariables) {
      assert variableDeclaration.isGlobal() : "variable declaration for bit vector must be global";
      ImmutableMap<MPORThread, CIdExpression> variables =
          buildSparseBitVectorVariablesByAccessType(
              pOptions, pThreads, variableDeclaration, pAccessType);
      rAccessVariables.put(variableDeclaration, new SparseBitVector(variables, pAccessType));
    }
    return Optional.of(rAccessVariables.buildOrThrow());
  }

  private static ImmutableMap<MPORThread, CIdExpression> buildSparseBitVectorVariablesByAccessType(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      CVariableDeclaration pVariableDeclaration,
      BitVectorAccessType pAccessType) {

    ImmutableMap.Builder<MPORThread, CIdExpression> rAccessVariables = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      rAccessVariables.put(
          thread,
          BitVectorUtil.createSparseAccessVariable(
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
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return new ThreadSimulationVariables(
        buildMutexLockedVariables(pOptions, pThreads, pSubstituteEdges, pBinaryExpressionBuilder));
  }

  private static ImmutableMap<CIdExpression, MutexLocked> buildMutexLockedVariables(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

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
                      true, SeqSimpleType.UNSIGNED_CHAR, varName, SeqInitializer.INT_0);
              CBinaryExpression notLockedExpression =
                  pBinaryExpressionBuilder.buildBinaryExpression(
                      mutexLocked, SeqIntegerLiteralExpression.INT_0, BinaryOperator.EQUALS);
              rVars.put(pthreadMutexT, new MutexLocked(mutexLocked, notLockedExpression));
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
                threadEdge,
                parameterDeclaration,
                rightHandSide,
                parameterSubstitute,
                pSubstitution.substitute(
                    rightHandSide, threadEdge.callContext, false, false, false, Optional.empty()));
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
        for (var innerEntry : entry.getValue().entrySet()) {
          CExpression rightHandSide = PthreadUtil.extractStartRoutineArgument(callContext.cfaEdge);
          FunctionParameterAssignment parameterAssignment =
              new FunctionParameterAssignment(
                  callContext,
                  innerEntry.getKey(),
                  rightHandSide,
                  innerEntry.getValue(),
                  pSubstitution.substitute(
                      // the inner call context is the context in which pthread_create is called
                      rightHandSide,
                      callContext.callContext,
                      false,
                      false,
                      false,
                      Optional.empty()));
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
