// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryLocationFinder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingMap;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunctionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.LocalVariableDeclarationSubstitute;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFANodeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFANodeForThread.BlockType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.SeqCallContext;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatementElement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CGotoStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CLabelStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CLogicalNotExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CStatementWrapper;

public class SeqInstrumentationBuilder {

  public static SeqInstrumentation buildBitVectorUpdateStatement(
      CIdExpression pBitVectorVariable, CIntegerLiteralExpression pValue) {

    CExpressionAssignmentStatement assignmentStatement =
        new CExpressionAssignmentStatement(FileLocation.DUMMY, pBitVectorVariable, pValue);
    return new SeqInstrumentation(
        SeqInstrumentationType.BIT_VECTOR_UPDATE, new CStatementWrapper(assignmentStatement));
  }

  public static SeqInstrumentation buildGotoBlockLabelStatement(
      CLabelStatement pBlockLabelStatement) {

    CGotoStatement gotoBlockLabel = new CGotoStatement(pBlockLabelStatement);
    return new SeqInstrumentation(SeqInstrumentationType.GOTO_BLOCK_LABEL, gotoBlockLabel);
  }

  public static SeqInstrumentation buildGuardedGotoStatement(
      CExpression pCondition,
      ImmutableList<CStatement> pPrecedingStatements,
      CLabelStatement pLabelStatement) {

    ImmutableList<CCompoundStatementElement> ifStatements =
        ImmutableList.<CCompoundStatementElement>builder()
            .addAll(pPrecedingStatements.stream().map(s -> new CStatementWrapper(s)).iterator())
            .add(new CGotoStatement(pLabelStatement))
            .build();
    CCompoundStatement compoundStatement = new CCompoundStatement(ifStatements);
    CIfStatement ifStatement =
        new CIfStatement(new CExpressionWrapper(pCondition), compoundStatement);
    return new SeqInstrumentation(SeqInstrumentationType.GUARDED_GOTO, ifStatement);
  }

  public static SeqInstrumentation buildPrevThreadUpdateStatement(
      CExpressionAssignmentStatement pPrevThreadUpdate) {

    CStatementWrapper prevThreadUpdateWrapped = new CStatementWrapper(pPrevThreadUpdate);
    return new SeqInstrumentation(
        SeqInstrumentationType.PREV_THREAD_UPDATE, prevThreadUpdateWrapped);
  }

  public static SeqInstrumentation buildPrevBitVectorUpdateStatement(
      ImmutableList<CExpressionAssignmentStatement> pPrevBitVectorUpdates) {

    ImmutableList.Builder<CCompoundStatementElement> exportStatements = ImmutableList.builder();
    for (CExpressionAssignmentStatement prevBitVectorUpdate : pPrevBitVectorUpdates) {
      exportStatements.add(new CStatementWrapper(prevBitVectorUpdate));
    }
    return new SeqInstrumentation(
        SeqInstrumentationType.PREV_BIT_VECTOR_UPDATE,
        new CCompoundStatement(exportStatements.build()));
  }

  public static SeqInstrumentation buildProgramCounterUpdate(
      CExpressionAssignmentStatement pPcUpdate) {

    CStatementWrapper pcUpdate = new CStatementWrapper(pPcUpdate);
    return new SeqInstrumentation(SeqInstrumentationType.PROGRAM_COUNTER_UPDATE, pcUpdate);
  }

  public static SeqInstrumentation buildThreadCountUpdateStatement(
      CExpressionAssignmentStatement pThreadCountUpdate) {

    CStatementWrapper threadCountUpdate = new CStatementWrapper(pThreadCountUpdate);
    return new SeqInstrumentation(SeqInstrumentationType.THREAD_COUNT_UPDATE, threadCountUpdate);
  }

  public static SeqInstrumentation buildThreadSyncUpdateStatement(
      CIdExpression pSyncVariable, CIntegerLiteralExpression pValue) {

    CExpressionAssignmentStatement syncUpdate =
        SeqStatementBuilder.buildExpressionAssignmentStatement(pSyncVariable, pValue);
    return new SeqInstrumentation(
        SeqInstrumentationType.THREAD_SYNC_UPDATE, new CStatementWrapper(syncUpdate));
  }

  public static SeqInstrumentation buildExecuteUntilConflictReductionStatement(
      NondeterminismSource pNondeterminismSource,
      Optional<CExportExpression> pBitVectorEvaluationExpression,
      CLabelStatement pTargetGoto) {

    SeqInstrumentationType type = SeqInstrumentationType.EXECUTE_UNTIL_CONFLICT_REDUCTION;
    // no evaluation due to no global accesses -> just goto
    if (pBitVectorEvaluationExpression.isEmpty()) {
      return new SeqInstrumentation(type, new CGotoStatement(pTargetGoto));
    }
    // for next_thread nondeterminism, we use goto instead of assume, if there is no conflict
    if (pNondeterminismSource.equals(NondeterminismSource.NEXT_THREAD)) {
      CLogicalNotExpression ifExpression = pBitVectorEvaluationExpression.orElseThrow().negate();
      CGotoStatement gotoStatement = new CGotoStatement(pTargetGoto);
      CCompoundStatement compoundStatement = new CCompoundStatement(gotoStatement);
      return new SeqInstrumentation(type, new CIfStatement(ifExpression, compoundStatement));
    }
    return new SeqInstrumentation(
        type,
        SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
            pBitVectorEvaluationExpression.orElseThrow()));
  }

  /**
   * Returns the {@link SeqInstrumentation} where a {@link PthreadObjectType#PTHREAD_ONCE_T} is
   * marked as unlocked, if any statement in {@code pStatements} exits an {@code init_routine}
   * specified in a call to {@link PthreadFunctionType#PTHREAD_ONCE}. If no {@code init_routine} is
   * exited, then {@link Optional#empty()} is returned.
   *
   * <p>For example {@code once} has to be marked as unlocked after {@code init_routine} returns.
   * This is only implicit in the input program and has to be instrumented explicitly in the output
   * program.
   *
   * <pre>{@code
   * void init_routine() {
   *    // do something ...
   *    // when this function exits, mark 'once' as unlocked
   * }
   * int main() {
   *   pthread_once_t once;
   *   pthread_once(once, &init_routine);
   * }
   * }</pre>
   */
  static Optional<SeqInstrumentation> tryBuildOnceUnlockInstrumentation(
      ImmutableMap<CFAEdgeForThread, SubstituteEdge> pSubstituteEdges,
      ImmutableList<SeqThreadStatement> pStatements) {

    if (pStatements.size() != 1) {
      return Optional.empty();
    }

    SeqThreadStatement statement = pStatements.getFirst();
    if (statement.data().getSubstituteEdges().size() != 1) {
      return Optional.empty();
    }

    CFAEdgeForThread threadEdge =
        Iterables.getOnlyElement(statement.data().getSubstituteEdges()).getThreadEdge();
    if (threadEdge.getSuccessor().leavingEdges().size() != 1) {
      return Optional.empty();
    }

    CFAEdgeForThread successorThreadEdge = threadEdge.getSuccessor().leavingEdges().getFirst();
    ImmutableMap<SeqCallContext, BlockType> blockTypes = threadEdge.getSuccessor().blockTypes;

    // The block types have to change on function exit if this exits a pthread_once init_routine.
    if (!(successorThreadEdge.getSuccessor().getCfaNode() instanceof FunctionExitNode)
        || !blockTypes.containsKey(threadEdge.callContext)
        || !BlockType.PTHREAD_ONCE_ROUTINE.equals(blockTypes.get(threadEdge.callContext))
        || successorThreadEdge.getSuccessor().blockTypes.containsKey(threadEdge.callContext)) {
      return Optional.empty();
    }

    CFANodeForThread returnNode =
        successorThreadEdge.getSuccessor().leavingEdges().getFirst().getSuccessor();
    // There are exactly two entering edges: CFunctionSummaryEdge and FunctionReturnEdge.
    checkArgument(returnNode.enteringEdges().size() == 2);
    CFAEdgeForThread functionSummaryEdge = returnNode.enteringEdges().getFirst();
    checkArgument(functionSummaryEdge.cfaEdge instanceof CFunctionSummaryEdge);

    SubstituteEdge substituteEdge =
        Objects.requireNonNull(
            pSubstituteEdges.get(
                Iterables.getOnlyElement(functionSummaryEdge.getPredecessor().enteringEdges())));
    CExportStatement onceFinishedAssignment =
        PthreadFunctionSubstitution.buildOnceUnlockStatement(
            SeqThreadStatementBuilder.getUpdatedParameterExpressions(substituteEdge.cfaEdge));

    return Optional.of(
        new SeqInstrumentation(SeqInstrumentationType.ONCE_UNLOCK, onceFinishedAssignment));
  }

  /**
   * Returns the {@link SeqInstrumentation} where dangling pointers after the leaving edges of
   * {@code pThreadNode} are executed. If no pointers are dangling, then {@link Optional#empty()} is
   * returned.
   *
   * <p>For example the pointer {@code ptr} has to be reset if it still points to any local stack
   * address after {@code function} returns:
   *
   * <pre>{@code
   * int* ptr;
   * void function() {
   *    int local = 0;
   *    ptr = & local;
   *    // sequentialization needs to inject the following here:
   *    // if (ptr == & local) {
   *    //    ptr = (void*)0;
   *    // }
   * }
   * }</pre>
   */
  static Optional<SeqInstrumentation> tryBuildResetDanglingPointerInstrumentation(
      CFANodeForThread pThreadNode,
      SeqPointerAliasingMap pPointerAliasingMap,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CFANode cfaNode = pThreadNode.getCfaNode();
    if (!MPORUtil.requiresPointerResets(cfaNode)) {
      return Optional.empty();
    }

    List<CIfStatement> ifStatements = new ArrayList<>();

    for (SeqPointerAssignment pointerAssignment : pPointerAliasingMap.pointerAssignments) {
      SeqMemoryLocation leftHandSide = pointerAssignment.leftHandSideMemoryLocation();

      // dangling pointers can only be reset by the thread that contain the pointer assignment
      if (pointerAssignment.getThread().id() == pThreadNode.threadId
          && pointerAssignment.leftHandSideMemoryLocation().isDeclarationPointerType()) {
        ImmutableSet<SeqMemoryLocation> dereferencedRightHandSides =
            SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
                leftHandSide, pPointerAliasingMap.pointerAssignments);

        for (SeqMemoryLocation rightHandSide : dereferencedRightHandSides) {
          // only consider stack allocated memory locations, since heap allocations are not dangling
          if (rightHandSide.isStackAllocated()) {

            CIdExpression leftHandSideExpression =
                pointerAssignment.leftHandSideMemoryLocation().toCIdExpression();
            CType rightHandSideType = Objects.requireNonNull(rightHandSide.declaration()).getType();
            CExpression rightHandSideExpression =
                rightHandSideType instanceof CPointerType
                    ? rightHandSide.toCIdExpression()
                    : new CUnaryExpression(
                        FileLocation.DUMMY,
                        new CPointerType(rightHandSideType.getQualifiers(), rightHandSideType),
                        rightHandSide.toCIdExpression(),
                        UnaryOperator.AMPER);

            // create the expression 'if (ptr == &local)'
            CBinaryExpression ifExpression =
                pBinaryExpressionBuilder.buildBinaryExpression(
                    leftHandSideExpression, rightHandSideExpression, BinaryOperator.EQUALS);
            CExpressionAssignmentStatement resetAssignmentStatement =
                new CExpressionAssignmentStatement(
                    FileLocation.DUMMY,
                    leftHandSideExpression,
                    CCastExpression.POINTER_TO_VOID_CAST);

            // create the statement 'ptr = (void*)0;'
            ifStatements.add(
                new CIfStatement(
                    new CExpressionWrapper(ifExpression),
                    new CCompoundStatement(new CStatementWrapper(resetAssignmentStatement))));
          }
        }
      }
    }

    if (ifStatements.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        new SeqInstrumentation(
            SeqInstrumentationType.RESET_DANGLING_POINTER,
            new CCompoundStatement(ImmutableList.copyOf(ifStatements))));
  }

  /**
   * Returns the {@link SeqInstrumentation} where pointers that go out of scope after the leaving
   * edges of {@code pThreadNode} are executed. If no pointers go out of scope, then {@link
   * Optional#empty()} is returned.
   *
   * <p>For example the local pointer {@code ptr} has to be reset after {@code function} returns.
   * All functions are inlined and {@code ptr} becomes global in the sequentialization, so it is not
   * reset implicitly but has to be reset explicitly:
   *
   * <pre>{@code
   * void function() {
   *    int* ptr = malloc(sizeof(int));
   *    // sequentialization needs to inject 'ptr = (void*)0;' here
   * }
   * }</pre>
   */
  static Optional<SeqInstrumentation> tryBuildResetOutOfScopePointerInstrumentation(
      MPORSubstitution pSubstitution,
      CFANodeForThread pThreadNode,
      AstCfaRelation pAstCfaRelation) {

    CFANode cfaNode = pThreadNode.getCfaNode();
    if (!MPORUtil.requiresPointerResets(cfaNode)) {
      return Optional.empty();
    }

    List<CCompoundStatementElement> resetAssignments = new ArrayList<>();

    // All local pointer variables need to be reset when inlining, for example:
    // void function() {
    //    int* ptr = malloc(sizeof(int));
    // }
    ImmutableTable<SeqCallContext, CVariableDeclaration, LocalVariableDeclarationSubstitute>
        localVariableSubstituteTable = pSubstitution.getLocalVariableSubstituteTable();
    ImmutableSet<AVariableDeclaration> localVariablesInScope =
        pAstCfaRelation.getDeclaredLocalVariablesByCfaNode(cfaNode);

    for (AVariableDeclaration localVariableInScope : localVariablesInScope) {
      if (localVariableInScope.getType() instanceof CPointerType) {

        LocalVariableDeclarationSubstitute localVariableDeclarationSubstitute =
            Objects.requireNonNull(
                localVariableSubstituteTable.get(pThreadNode.callContext, localVariableInScope));
        CExpressionAssignmentStatement assignmentStatement =
            new CExpressionAssignmentStatement(
                FileLocation.DUMMY,
                localVariableDeclarationSubstitute.idExpression(),
                CCastExpression.POINTER_TO_VOID_CAST);
        resetAssignments.add(new CStatementWrapper(assignmentStatement));
      }
    }

    // All pointer parameters need to be reset when inlining, for example:
    // void function(int* ptr) {
    //    ptr = malloc(sizeof(int));
    // }
    for (AParameterDeclaration parameterInScope :
        pAstCfaRelation.getDeclaredParametersByCfaNode(cfaNode)) {
      if (parameterInScope.getType() instanceof CPointerType
          && pThreadNode.callContext.cfaEdgeForThread().isPresent()) {

        ImmutableList<CIdExpression> parameterSubstitutes =
            pSubstitution.getAllParameterIdExpressions(
                pThreadNode.callContext, (CParameterDeclaration) parameterInScope);

        for (CIdExpression parameterIdExpression : parameterSubstitutes) {
          CExpressionAssignmentStatement assignmentStatement =
              new CExpressionAssignmentStatement(
                  FileLocation.DUMMY, parameterIdExpression, CCastExpression.POINTER_TO_VOID_CAST);
          resetAssignments.add(new CStatementWrapper(assignmentStatement));
        }
      }
    }

    if (resetAssignments.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        new SeqInstrumentation(
            SeqInstrumentationType.RESET_OUT_OF_SCOPE_POINTER,
            new CCompoundStatement(ImmutableList.copyOf(resetAssignments))));
  }
}
