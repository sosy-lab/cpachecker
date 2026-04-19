// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.common.collect.Collections3.elementsAndList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model.SeqMemoryLocationFinder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunctionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.CondSignaledFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.MutexLockedFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.RwLockNumReadersWritersFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.ThreadSyncFlags;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFANodeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThreadUtil;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CComment;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatementElement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionStatementWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CStatementWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CVariableDeclarationWrapper;

public record SeqThreadStatementBuilder(
    MPORThread thread,
    ImmutableList<MPORThread> allThreads,
    ImmutableMap<CFAEdgeForThread, SubstituteEdge> substituteEdges,
    MemoryModel memoryModel,
    FunctionStatements functionStatements,
    ThreadSyncFlags threadSyncFlags,
    CLeftHandSide pcLeftHandSide,
    ProgramCounterVariables pcVariables,
    CBinaryExpressionBuilder binaryExpressionBuilder) {

  private static final String REACH_ERROR_FUNCTION_NAME = "reach_error";

  private static final CFunctionTypeWithNames REACH_ERROR_FUNCTION_TYPE =
      new CFunctionTypeWithNames(CVoidType.VOID, ImmutableList.of(), false);

  public static final CFunctionDeclaration REACH_ERROR_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          REACH_ERROR_FUNCTION_TYPE,
          REACH_ERROR_FUNCTION_NAME,
          ImmutableList.of(),
          ImmutableSet.of(FunctionAttribute.NO_RETURN));

  private static final CIdExpression REACH_ERROR_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, REACH_ERROR_FUNCTION_DECLARATION);

  private static final CFunctionCallExpression REACH_ERROR_FUNCTION_CALL_EXPRESSION =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          CVoidType.VOID,
          REACH_ERROR_ID_EXPRESSION,
          ImmutableList.of(),
          REACH_ERROR_FUNCTION_DECLARATION);

  private static final CFunctionCallStatement REACH_ERROR_FUNCTION_CALL_STATEMENT =
      new CFunctionCallStatement(FileLocation.DUMMY, REACH_ERROR_FUNCTION_CALL_EXPRESSION);

  public ImmutableList<SeqThreadStatement> buildStatementsFromThreadNode(
      CFANodeForThread pThreadNode, Set<CFANodeForThread> pCoveredNodes)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatement> rStatements = ImmutableList.builder();
    for (CFAEdgeForThread threadEdge : pThreadNode.leavingEdges()) {
      // handle const CPAchecker_TMP first because it requires successor nodes and edges
      if (MPORUtil.isConstCpaCheckerTmpDeclaration(threadEdge.cfaEdge)) {
        rStatements.add(buildConstCpaCheckerTmpStatement(threadEdge, pCoveredNodes));

      } else if (MPORUtil.isCpaCheckerTmpDeclarationWithoutInitializer(threadEdge.cfaEdge)) {
        rStatements.add(buildCpaCheckerTmpWithoutInitializerStatement(threadEdge));

      } else {
        // exclude all function summaries, the calling context is handled by return edges
        if (!isExcludedSummaryEdge(threadEdge.cfaEdge)) {
          if (substituteEdges.containsKey(threadEdge)) {
            SubstituteEdge substitute = Objects.requireNonNull(substituteEdges.get(threadEdge));
            rStatements.add(buildStatementFromThreadEdge(threadEdge, substitute));
          }
        }
      }
    }
    return rStatements.build();
  }

  // const CPAchecker_TMP ==========================================================================

  private SeqThreadStatement buildConstCpaCheckerTmpStatement(
      CFAEdgeForThread pThreadEdge, Set<CFANodeForThread> pCoveredNodes)
      throws UnsupportedCodeException {

    SubstituteEdge constCpaCheckerTmpEdge =
        Objects.requireNonNull(substituteEdges.get(pThreadEdge));

    // ensure there are two single successors that are both statement edges
    CFANodeForThread firstSuccessor = pThreadEdge.getSuccessor();
    pCoveredNodes.add(firstSuccessor);
    // const CPAchecker_TMP declarations can have only 1 successor edge
    CFAEdgeForThread firstSuccessorEdge = Iterables.getOnlyElement(firstSuccessor.leavingEdges());
    assert firstSuccessorEdge.cfaEdge instanceof CStatementEdge
        : "successor edge of const CPAchecker_TMP declaration must be CStatementEdge";
    CFANodeForThread secondSuccessor = firstSuccessorEdge.getSuccessor();
    // second successor of const CPAchecker_TMP declarations can have only 1 successor edge
    CFAEdgeForThread secondSuccessorEdge = Iterables.getOnlyElement(secondSuccessor.leavingEdges());

    CStatementEdge secondSuccessorStatement = (CStatementEdge) secondSuccessorEdge.cfaEdge;
    // there are programs where a const CPAchecker_TMP statement has only two parts.
    // in the tested programs, this only happened when the statement was followed by a function call
    if (secondSuccessorStatement.getStatement() instanceof CFunctionCallStatement) {
      return buildTwoPartConstCpaCheckerTmpStatement(constCpaCheckerTmpEdge, firstSuccessorEdge);
    } else {
      // cover second successor only when it is a three part const CPAchecker_TMP statement
      pCoveredNodes.add(secondSuccessor);
      return buildThreePartConstCpaCheckerTmpStatement(
          constCpaCheckerTmpEdge, firstSuccessorEdge, secondSuccessorEdge);
    }
  }

  private SeqThreadStatement buildTwoPartConstCpaCheckerTmpStatement(
      SubstituteEdge pConstCpaCheckerTmpEdge, CFAEdgeForThread pSuccessorEdge)
      throws UnsupportedCodeException {

    // treat const CPAchecker_TMP var as atomic (3 statements in 1 case)
    SubstituteEdge substituteEdgeA = Objects.requireNonNull(substituteEdges.get(pSuccessorEdge));
    int newTargetPc = pSuccessorEdge.getSuccessor().pc;

    return buildConstCpaCheckerTmpStatement(
        pConstCpaCheckerTmpEdge,
        substituteEdgeA,
        Optional.empty(),
        ImmutableSet.of(pConstCpaCheckerTmpEdge, substituteEdgeA),
        newTargetPc);
  }

  private SeqThreadStatement buildThreePartConstCpaCheckerTmpStatement(
      SubstituteEdge pConstCpaCheckerTmpEdge,
      CFAEdgeForThread pFirstSuccessorEdge,
      CFAEdgeForThread pSecondSuccessorEdge)
      throws UnsupportedCodeException {

    // treat const CPAchecker_TMP var as atomic (3 statements in 1 case)
    SubstituteEdge firstSuccessorEdge =
        Objects.requireNonNull(substituteEdges.get(pFirstSuccessorEdge));
    SubstituteEdge secondSuccessorEdge =
        Objects.requireNonNull(substituteEdges.get(pSecondSuccessorEdge));
    int newTargetPc = pSecondSuccessorEdge.getSuccessor().pc;

    return buildConstCpaCheckerTmpStatement(
        pConstCpaCheckerTmpEdge,
        firstSuccessorEdge,
        Optional.of(secondSuccessorEdge),
        ImmutableSet.of(pConstCpaCheckerTmpEdge, firstSuccessorEdge, secondSuccessorEdge),
        newTargetPc);
  }

  private SeqThreadStatement buildConstCpaCheckerTmpStatement(
      SubstituteEdge pConstCpaCheckerTmpEdge,
      SubstituteEdge pFirstSuccessorEdge,
      Optional<SubstituteEdge> pSecondSuccessorEdge,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pNewTargetPc)
      throws UnsupportedCodeException {

    SeqThreadStatementData data =
        new SeqThreadStatementData(
            SeqThreadStatementType.CONST_CPACHECKER_TMP,
            pSubstituteEdges,
            thread.id(),
            pcLeftHandSide);

    // ensure that the declaration is a CVariableDeclaration and cast accordingly
    CDeclarationEdge declarationEdge = (CDeclarationEdge) pConstCpaCheckerTmpEdge.cfaEdge;
    CVariableDeclaration variableDeclaration =
        (CVariableDeclaration) declarationEdge.getDeclaration();

    checkConstCpaCheckerTmpArguments(
        variableDeclaration, pFirstSuccessorEdge, pSecondSuccessorEdge);

    ImmutableList.Builder<CCompoundStatementElement> exportStatements = ImmutableList.builder();
    exportStatements.add(new CVariableDeclarationWrapper(variableDeclaration));
    exportStatements.add(
        new CStatementWrapper(((CStatementEdge) pFirstSuccessorEdge.cfaEdge).getStatement()));

    if (pSecondSuccessorEdge.isPresent()) {
      exportStatements.add(
          new CStatementWrapper(
              ((CStatementEdge) pSecondSuccessorEdge.orElseThrow().cfaEdge).getStatement()));
    }
    return SeqThreadStatement.of(data, pNewTargetPc, exportStatements.build());
  }

  private void checkConstCpaCheckerTmpArguments(
      CVariableDeclaration pVariableDeclaration,
      SubstituteEdge pFirstSuccessorEdge,
      Optional<SubstituteEdge> pSecondSuccessorEdge)
      throws UnsupportedCodeException {

    checkArgument(
        MPORUtil.isConstCpaCheckerTmp(pVariableDeclaration),
        "pDeclaration must declare a const __CPAchecker_TMP variable");
    checkArgument(
        pFirstSuccessorEdge.cfaEdge instanceof CStatementEdge,
        "pFirstSuccessorEdge.cfaEdge must be CStatementEdge");
    if (pSecondSuccessorEdge.isPresent()) {
      checkArgument(
          pSecondSuccessorEdge.orElseThrow().cfaEdge instanceof CStatementEdge,
          "pSecondSuccessorEdge.cfaEdge must be CStatementEdge");

      CStatement secondStatement =
          ((CStatementEdge) pSecondSuccessorEdge.orElseThrow().cfaEdge).getStatement();
      if (secondStatement instanceof CExpressionStatement secondExpressionStatement) {
        CIdExpression secondIdExpression =
            getIdExpressionFromSecondSuccessor(secondExpressionStatement.getExpression());
        CSimpleDeclaration secondDeclaration = secondIdExpression.getDeclaration();
        checkArgument(
            pVariableDeclaration.equals(secondDeclaration),
            "pDeclaration and pSecondSuccessorEdge must use the same __CPAchecker_TMP variable when"
                + " pSecondSuccessorEdge is a CExpressionStatement");

      } else if (secondStatement instanceof CExpressionAssignmentStatement secondAssignment) {
        CStatement firstStatement = ((CStatementEdge) pFirstSuccessorEdge.cfaEdge).getStatement();
        checkArgument(
            firstStatement instanceof CExpressionAssignmentStatement,
            "pFirstSuccessorEdge must be CExpressionAssignmentStatement when pSecondSuccessorEdge"
                + " is a CExpressionAssignmentStatement");
        CExpressionAssignmentStatement firstAssignment =
            (CExpressionAssignmentStatement) firstStatement;
        if (pVariableDeclaration.getInitializer()
            instanceof CInitializerExpression initializerExpression) {
          if (initializerExpression.getExpression().equals(firstAssignment.getLeftHandSide())) {
            if (secondAssignment.getRightHandSide() instanceof CIdExpression secondIdExpression) {
              // this happens e.g. in weaver/parallel-ticket-6.wvr.c
              // _Atomic int CPA_TMP_0 = t; t = t + 1; m1 = CPA_TMP_0;
              // we want to ensure that the declaration is equal to the RHS in the last statement
              checkArgument(
                  pVariableDeclaration.equals(secondIdExpression.getDeclaration()),
                  "pVariableDeclaration must equal pSecondSuccessorEdge RHS");
              return;
            }
          }
        }
        // this happens e.g. in ldv-races/race-2_2-container_of:
        // CPA_TMP_0 = {  }; CPA_TMP_1 = (struct my_data *)(((char *)mptr) - 40); data = CPA_TMP_1;
        // check if the middle statement LHS matches the last statements RHS (CPA_TMP_1)
        checkArgument(
            firstAssignment.getLeftHandSide().equals(secondAssignment.getRightHandSide()),
            "pFirstSuccessorEdge LHS must equal pSecondSuccessorEdge RHS when pSecondSuccessorEdge"
                + " is a CExpressionAssignmentStatement");
      }
    }
  }

  private CIdExpression getIdExpressionFromSecondSuccessor(CExpression pExpression)
      throws UnsupportedCodeException {

    if (pExpression instanceof CIdExpression idExpression) {
      return idExpression;
    } else if (pExpression instanceof CPointerExpression pointerExpression) {
      if (pointerExpression.getOperand() instanceof CIdExpression idExpression) {
        return idExpression;
      }
    }
    throw new UnsupportedCodeException(
        String.format(
            "pExpression must be either CIdExpression or CPointerExpression %s",
            pExpression.toASTString()),
        null);
  }

  // CPAchecker_TMP without initializer ============================================================

  private SeqThreadStatement buildCpaCheckerTmpWithoutInitializerStatement(
      CFAEdgeForThread pThreadEdge) {

    SubstituteEdge cpaCheckerTmpEdge = Objects.requireNonNull(substituteEdges.get(pThreadEdge));
    CDeclarationEdge declarationEdge = (CDeclarationEdge) cpaCheckerTmpEdge.cfaEdge;
    CVariableDeclaration variableDeclaration =
        (CVariableDeclaration) declarationEdge.getDeclaration();
    CIdExpression idExpression = new CIdExpression(FileLocation.DUMMY, variableDeclaration);
    CExpressionStatementWrapper exportStatement =
        new CExpressionStatementWrapper(new CExpressionWrapper(idExpression));

    SeqThreadStatementData data =
        new SeqThreadStatementData(
            SeqThreadStatementType.CPACHECKER_TMP_WITHOUT_INITIALIZER,
            ImmutableSet.of(cpaCheckerTmpEdge),
            thread.id(),
            pcLeftHandSide);

    return SeqThreadStatement.of(
        data, pThreadEdge.getSuccessor().pc, ImmutableList.of(exportStatement));
  }

  // Statement build methods =======================================================================

  private SeqThreadStatement buildStatementFromThreadEdge(
      CFAEdgeForThread pThreadEdge, SubstituteEdge pSubstituteEdge)
      throws UnrecognizedCodeException {

    CFAEdge cfaEdge = pThreadEdge.cfaEdge;
    int targetPc = pThreadEdge.getSuccessor().pc;
    CFANode successor = pThreadEdge.getSuccessor().getCfaNode();

    if (resultsInBlankStatement(pSubstituteEdge, successor)) {
      return buildGhostOnlyStatement(
          thread, ImmutableSet.of(pSubstituteEdge), pcLeftHandSide, targetPc);
    }

    return switch (pSubstituteEdge.cfaEdge) {
      case BlankEdge blankEdge ->
          buildWhileTrueLoopHeadStatement(blankEdge, pSubstituteEdge, targetPc);

      case CAssumeEdge assumeEdge -> buildAssumeStatement(assumeEdge, pSubstituteEdge, targetPc);

      case CDeclarationEdge declarationEdge ->
          // "leftover" declarations should be local variables with an initializer
          buildLocalVariableInitializationStatement(
              (CVariableDeclaration) declarationEdge.getDeclaration(), pSubstituteEdge, targetPc);

      case CFunctionCallEdge functionCallEdge ->
          buildFunctionCallStatement(pThreadEdge, functionCallEdge, pSubstituteEdge, targetPc);

      case CReturnStatementEdge ignore ->
          buildReturnValueAssignmentStatement(pThreadEdge, pSubstituteEdge, targetPc);

      case CFAEdge edge when PthreadUtil.isExplicitlyHandledPthreadFunction(edge) ->
          buildStatementFromPthreadFunction(pThreadEdge, pSubstituteEdge, targetPc);

      case CStatementEdge statementEdge ->
          buildDefaultStatement(statementEdge, pSubstituteEdge, targetPc);

      default ->
          throw new AssertionError("Unhandled CFAEdge type: " + cfaEdge.getClass().getSimpleName());
    };
  }

  private SeqThreadStatement buildWhileTrueLoopHeadStatement(
      BlankEdge pBlankEdge, SubstituteEdge pSubstituteEdge, int pTargetPc) {

    checkArgument(
        pBlankEdge.getPredecessor().isLoopStart(),
        "The predecessor of a left over BlankEdge must be a loop head.");

    SeqThreadStatementData data =
        new SeqThreadStatementData(
            SeqThreadStatementType.WHILE_TRUE_LOOP_HEAD,
            ImmutableSet.of(pSubstituteEdge),
            thread.id(),
            pcLeftHandSide);
    // just add a comment with "while (1)" for better overview in the output program
    CComment commentStatement = new CComment("while (1)");
    return SeqThreadStatement.of(data, pTargetPc, ImmutableList.of(commentStatement));
  }

  private SeqThreadStatement buildAssumeStatement(
      CAssumeEdge pAssumeEdge, SubstituteEdge pSubstituteEdge, int pTargetPc) {

    SeqThreadStatementDataWithIfExpression data =
        new SeqThreadStatementDataWithIfExpression(
            SeqThreadStatementType.ASSUME,
            ImmutableSet.of(pSubstituteEdge),
            thread.id(),
            pcLeftHandSide,
            pAssumeEdge.getExpression());

    // just return with empty statements, the block handles the if-else branch
    return SeqThreadStatement.of(data, pTargetPc, ImmutableList.of());
  }

  private SeqThreadStatement buildDefaultStatement(
      CStatementEdge pStatementEdge, SubstituteEdge pSubstituteEdge, int pTargetPc) {

    SeqThreadStatementData data =
        SeqThreadStatementData.of(
            SeqThreadStatementType.DEFAULT, pSubstituteEdge, thread.id(), pcLeftHandSide);
    return SeqThreadStatement.of(
        data, pTargetPc, ImmutableList.of(new CStatementWrapper(pStatementEdge.getStatement())));
  }

  static SeqThreadStatement buildGhostOnlyStatement(
      MPORThread pThread,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    SeqThreadStatementData data =
        SeqThreadStatementData.of(
            SeqThreadStatementType.GHOST_ONLY, pSubstituteEdges, pThread.id(), pPcLeftHandSide);
    return SeqThreadStatement.of(data, pTargetPc, ImmutableList.of());
  }

  private SeqThreadStatement buildLocalVariableInitializationStatement(
      CVariableDeclaration pVariableDeclaration, SubstituteEdge pSubstituteEdge, int pTargetPc)
      throws UnsupportedCodeException {

    checkArgument(!pVariableDeclaration.isGlobal(), "pVariableDeclaration must be local");
    checkArgument(
        pVariableDeclaration.getInitializer() != null,
        "pVariableDeclaration must have an initializer");

    SeqThreadStatementData data =
        SeqThreadStatementData.of(
            SeqThreadStatementType.LOCAL_VARIABLE_INITIALIZATION,
            pSubstituteEdge,
            thread.id(),
            pcLeftHandSide);
    CStatementWrapper assignmentStatement =
        buildExpressionAssignmentStatementFromVariableDeclaration(pVariableDeclaration);
    return SeqThreadStatement.of(data, pTargetPc, ImmutableList.of(assignmentStatement));
  }

  private SeqThreadStatement buildFunctionCallStatement(
      CFAEdgeForThread pThreadEdge,
      CFunctionCallEdge pFunctionCallEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc) {

    String functionName =
        pFunctionCallEdge.getFunctionCallExpression().getDeclaration().getOrigName();
    SeqThreadStatementData parameterAssignmentData =
        SeqThreadStatementData.of(
            SeqThreadStatementType.PARAMETER_ASSIGNMENT,
            pSubstituteEdge,
            thread.id(),
            pcLeftHandSide);

    // handle (some arbitrary) function with parameters
    if (functionStatements.parameterAssignments().containsKey(pThreadEdge)) {
      ImmutableList<FunctionParameterAssignment> assignments =
          functionStatements.parameterAssignments().get(pThreadEdge);
      return buildParameterAssignmentStatement(
          functionName, assignments, parameterAssignmentData, pTargetPc);
    }

    // handle function without parameters that is a call to "reach_error"
    if (functionName.equals(REACH_ERROR_FUNCTION_NAME)) {
      return buildParameterAssignmentStatement(
          functionName, ImmutableList.of(), parameterAssignmentData, pTargetPc);
    }

    // handle function without parameters that is not "reach_error" -> blank statement
    CFunctionDeclaration functionDeclaration =
        pFunctionCallEdge.getFunctionCallExpression().getDeclaration();
    checkState(
        functionDeclaration.getParameters().isEmpty(),
        "function has parameters, but they are not present in pFunctionStatements");

    return buildGhostOnlyStatement(
        thread, ImmutableSet.of(pSubstituteEdge), pcLeftHandSide, pTargetPc);
  }

  private SeqThreadStatement buildParameterAssignmentStatement(
      String pFunctionName,
      ImmutableList<FunctionParameterAssignment> pFunctionParameterAssignments,
      SeqThreadStatementData pData,
      int pTargetPc) {

    checkArgument(
        !pFunctionParameterAssignments.isEmpty() || pFunctionName.equals(REACH_ERROR_FUNCTION_NAME),
        "If pAssignments is empty, then the function name must be reach_error.");

    ImmutableList.Builder<CCompoundStatementElement> functionStatementBuilder =
        ImmutableList.builder();
    // if the function name is "reach_error", add a "reach_error()" call for reachability
    if (pFunctionName.equals(REACH_ERROR_FUNCTION_NAME)) {
      functionStatementBuilder.add(new CStatementWrapper(REACH_ERROR_FUNCTION_CALL_STATEMENT));
    }
    for (FunctionParameterAssignment assignment : pFunctionParameterAssignments) {
      functionStatementBuilder.add(
          new CStatementWrapper(assignment.toExpressionAssignmentStatement()));
    }
    return SeqThreadStatement.of(pData, pTargetPc, functionStatementBuilder.build());
  }

  private SeqThreadStatement buildReturnValueAssignmentStatement(
      CFAEdgeForThread pThreadEdge, SubstituteEdge pSubstituteEdge, int pTargetPc) {

    // returning from non-start-routine function: assign return value to return vars
    if (functionStatements.returnValueAssignments().containsKey(pThreadEdge)) {
      FunctionReturnValueAssignment assignment =
          Objects.requireNonNull(functionStatements.returnValueAssignments().get(pThreadEdge));
      SeqThreadStatementData data =
          SeqThreadStatementData.of(
              SeqThreadStatementType.DEFAULT, pSubstituteEdge, thread.id(), pcLeftHandSide);
      return SeqThreadStatement.of(
          data, pTargetPc, ImmutableList.of(new CStatementWrapper(assignment.statement())));
    }

    // -> function does not return anything, i.e. return;
    return buildGhostOnlyStatement(
        thread, ImmutableSet.of(pSubstituteEdge), pcLeftHandSide, pTargetPc);
  }

  private SeqThreadStatement buildStatementFromPthreadFunction(
      CFAEdgeForThread pThreadEdge, SubstituteEdge pSubstituteEdge, int pTargetPc)
      throws UnrecognizedCodeException {

    CFAEdge cfaEdge = pSubstituteEdge.cfaEdge;
    CFunctionCall functionCall = PthreadUtil.tryGetFunctionCallFromCfaEdge(cfaEdge).orElseThrow();
    PthreadFunctionType functionType = PthreadUtil.getPthreadFunctionType(functionCall);

    return switch (functionType) {
      case PTHREAD_COND_SIGNAL ->
          buildCondStatement(
              SeqThreadStatementType.COND_SIGNAL, functionType, pSubstituteEdge, pTargetPc);
      case PTHREAD_COND_WAIT ->
          throw new AssertionError(
              "pthread_cond_wait is handled separately, it requires two clauses");
      case PTHREAD_CREATE ->
          buildThreadCreationStatement(functionCall, pThreadEdge, pSubstituteEdge, pTargetPc);
      case PTHREAD_EXIT -> buildThreadExitStatement(pThreadEdge, pSubstituteEdge, pTargetPc);
      case PTHREAD_JOIN -> buildThreadJoinStatement(functionCall, pSubstituteEdge, pTargetPc);
      case PTHREAD_MUTEX_LOCK ->
          buildMutexStatement(
              SeqThreadStatementType.MUTEX_LOCK, functionType, pSubstituteEdge, pTargetPc);
      case PTHREAD_MUTEX_UNLOCK ->
          buildMutexStatement(
              SeqThreadStatementType.MUTEX_UNLOCK, functionType, pSubstituteEdge, pTargetPc);
      case PTHREAD_RWLOCK_RDLOCK ->
          buildRwLockStatement(
              SeqThreadStatementType.RW_LOCK_RD_LOCK, functionType, pSubstituteEdge, pTargetPc);
      case PTHREAD_RWLOCK_UNLOCK ->
          buildRwLockStatement(
              SeqThreadStatementType.RW_LOCK_UNLOCK, functionType, pSubstituteEdge, pTargetPc);
      case PTHREAD_RWLOCK_WRLOCK ->
          buildRwLockStatement(
              SeqThreadStatementType.RW_LOCK_WR_LOCK, functionType, pSubstituteEdge, pTargetPc);
      case VERIFIER_ATOMIC_BEGIN ->
          buildAtomicStatement(
              PthreadFunctionType.VERIFIER_ATOMIC_BEGIN,
              SeqThreadStatementType.ATOMIC_BEGIN,
              pSubstituteEdge,
              pTargetPc);
      case VERIFIER_ATOMIC_END ->
          buildAtomicStatement(
              PthreadFunctionType.VERIFIER_ATOMIC_END,
              SeqThreadStatementType.ATOMIC_END,
              pSubstituteEdge,
              pTargetPc);
      default ->
          throw new AssertionError("unhandled relevant pthread method: " + functionType.name);
    };
  }

  public SeqThreadStatement buildAtomicStatement(
      PthreadFunctionType pFunctionType,
      SeqThreadStatementType pStatementType,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc) {

    checkArgument(
        pFunctionType.equals(PthreadFunctionType.VERIFIER_ATOMIC_BEGIN)
            || pFunctionType.equals(PthreadFunctionType.VERIFIER_ATOMIC_END),
        "pFunctionType must be VERIFIER_ATOMIC_BEGIN or VERIFIER_ATOMIC_END.");

    SeqThreadStatementData data =
        SeqThreadStatementData.of(pStatementType, pSubstituteEdge, thread.id(), pcLeftHandSide);

    // just add a comment with the function name for better overview in the output program
    CComment commentStatement = new CComment(pFunctionType.name + ";");
    return SeqThreadStatement.of(data, pTargetPc, ImmutableList.of(commentStatement));
  }

  public SeqThreadStatement buildCondStatement(
      SeqThreadStatementType pStatementType,
      PthreadFunctionType pFunctionType,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc)
      throws UnrecognizedCodeException {

    SeqThreadStatementData data =
        SeqThreadStatementData.of(pStatementType, pSubstituteEdge, thread.id(), pcLeftHandSide);
    return SeqThreadStatement.of(
        data,
        pTargetPc,
        buildThreadSyncStatements(
            pStatementType, pFunctionType, PthreadObjectType.PTHREAD_COND_T, pSubstituteEdge));
  }

  private SeqThreadStatement buildThreadCreationStatement(
      CFunctionCall pFunctionCall,
      CFAEdgeForThread pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc)
      throws UnsupportedCodeException {

    CFAEdge cfaEdge = pSubstituteEdge.cfaEdge;
    checkArgument(
        cfaEdge instanceof CFunctionCallEdge || cfaEdge instanceof CStatementEdge,
        "cfaEdge must be CFunctionCallEdge or CStatementEdge");

    SeqThreadStatementData data =
        SeqThreadStatementData.of(
            SeqThreadStatementType.THREAD_CREATION, pSubstituteEdge, thread.id(), pcLeftHandSide);

    CExpression pthreadTObject =
        PthreadUtil.extractPthreadObject(pFunctionCall, PthreadObjectType.PTHREAD_T);
    MPORThread createdThread =
        MPORThreadUtil.getThreadByObject(allThreads, Optional.of(pthreadTObject));
    Optional<FunctionParameterAssignment> startRoutineArgAssignment =
        functionStatements.tryGetStartRoutineArgAssignmentByThreadEdge(pThreadEdge);

    ImmutableList.Builder<CCompoundStatementElement> exportStatements = ImmutableList.builder();
    if (startRoutineArgAssignment.isPresent()) {
      exportStatements.add(
          new CStatementWrapper(
              startRoutineArgAssignment.orElseThrow().toExpressionAssignmentStatement()));
    }
    exportStatements.add(
        new CStatementWrapper(
            ProgramCounterVariables.buildPcAssignmentStatement(
                pcVariables().getPcLeftHandSide(createdThread.id()),
                ProgramCounterVariables.INIT_PC)));

    return SeqThreadStatement.of(data, pTargetPc, exportStatements.build());
  }

  private SeqThreadStatement buildThreadExitStatement(
      CFAEdgeForThread pThreadEdge, SubstituteEdge pSubstituteEdge, int pTargetPc) {

    if (!functionStatements.startRoutineExitAssignments().containsKey(pThreadEdge)) {
      return buildGhostOnlyStatement(
          thread, ImmutableSet.of(pSubstituteEdge), pcLeftHandSide, pTargetPc);
    }
    SeqThreadStatementData data =
        SeqThreadStatementData.of(
            SeqThreadStatementType.THREAD_EXIT, pSubstituteEdge, thread.id(), pcLeftHandSide);
    FunctionReturnValueAssignment returnValueAssignment =
        Objects.requireNonNull(functionStatements.startRoutineExitAssignments().get(pThreadEdge));
    return SeqThreadStatement.of(
        data,
        pTargetPc,
        ImmutableList.of(new CStatementWrapper(returnValueAssignment.statement())));
  }

  private SeqThreadStatement buildThreadJoinStatement(
      CFunctionCall pFunctionCall, SubstituteEdge pSubstituteEdge, int pTargetPc)
      throws UnsupportedCodeException {

    SeqThreadStatementData data =
        SeqThreadStatementData.of(
            SeqThreadStatementType.THREAD_JOIN, pSubstituteEdge, thread.id(), pcLeftHandSide);

    MPORThread targetThread = MPORThreadUtil.getThreadByCFunctionCall(allThreads, pFunctionCall);
    CStatementWrapper assumeCall =
        new CStatementWrapper(
            SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
                pcVariables.getThreadInactiveExpression(targetThread.id())));

    CExpression parameter =
        PthreadUtil.getParameterExpressionAtPthreadObjectTypeIndex(
            pFunctionCall, PthreadObjectType.RETURN_VALUE);
    if (!MPORUtil.isVoidPointer(parameter)) {
      if (targetThread.startRoutineExitVariable().isPresent()) {
        CStatementWrapper returnValueRead =
            new CStatementWrapper(
                buildReturnValueRead(
                    targetThread.startRoutineExitVariable().orElseThrow(), pSubstituteEdge));
        return SeqThreadStatement.of(
            data, pTargetPc, ImmutableList.of(assumeCall, returnValueRead));
      }
    }
    return SeqThreadStatement.of(data, pTargetPc, ImmutableList.of(assumeCall));
  }

  private static CStatement buildReturnValueRead(
      CIdExpression pJoinedThreadExitVariable, SubstituteEdge pSubstituteEdge)
      throws UnsupportedCodeException {

    int returnValueIndex =
        PthreadFunctionType.PTHREAD_JOIN.getParameterIndex(PthreadObjectType.RETURN_VALUE);
    CFunctionCall functionCall =
        PthreadUtil.tryGetFunctionCallFromCfaEdge(pSubstituteEdge.cfaEdge).orElseThrow();
    CExpression returnValueParameter =
        functionCall.getFunctionCallExpression().getParameterExpressions().get(returnValueIndex);
    if (returnValueParameter instanceof CUnaryExpression unaryExpression) {
      // extract retval from unary expression &retval
      if (unaryExpression.getOperator().equals(UnaryOperator.AMPER)) {
        if (unaryExpression.getOperand() instanceof CIdExpression idExpression) {
          return SeqStatementBuilder.buildExpressionAssignmentStatement(
              idExpression, pJoinedThreadExitVariable);
        }
      }
    }
    throw new UnsupportedCodeException(
        "pthread_join retval could not be extracted from the following expression: "
            + returnValueParameter,
        pSubstituteEdge.cfaEdge);
  }

  public SeqThreadStatement buildMutexStatement(
      SeqThreadStatementType pStatementType,
      PthreadFunctionType pFunctionType,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc)
      throws UnrecognizedCodeException {

    checkArgument(
        pStatementType.equals(SeqThreadStatementType.MUTEX_LOCK)
            || pStatementType.equals(SeqThreadStatementType.MUTEX_UNLOCK),
        "pStatementType must be MUTEX_LOCK or MUTEX_UNLOCK");

    SeqThreadStatementData data =
        SeqThreadStatementData.of(pStatementType, pSubstituteEdge, thread.id(), pcLeftHandSide);
    return SeqThreadStatement.of(
        data,
        pTargetPc,
        buildThreadSyncStatements(
            pStatementType, pFunctionType, PthreadObjectType.PTHREAD_MUTEX_T, pSubstituteEdge));
  }

  private SeqThreadStatement buildRwLockStatement(
      SeqThreadStatementType pStatementType,
      PthreadFunctionType pFunctionType,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc)
      throws UnrecognizedCodeException {

    SeqThreadStatementData data =
        SeqThreadStatementData.of(pStatementType, pSubstituteEdge, thread.id(), pcLeftHandSide);
    return SeqThreadStatement.of(
        data,
        pTargetPc,
        buildThreadSyncStatements(
            pStatementType, pFunctionType, PthreadObjectType.PTHREAD_RWLOCK_T, pSubstituteEdge));
  }

  // Thread Sync Statements

  private ImmutableList<CCompoundStatementElement> buildThreadSyncStatements(
      SeqThreadStatementType pStatementType,
      PthreadFunctionType pFunctionType,
      PthreadObjectType pObjectType,
      SubstituteEdge pSubstituteEdge)
      throws UnrecognizedCodeException {

    // all memory locations (potentially) accessed in pSubstituteEdge including aliased pointers
    ImmutableSet<SeqMemoryLocation> accessedMemoryLocations =
        pSubstituteEdge.getMemoryLocationsByAccessType(MemoryAccessType.ACCESS);

    // First check the non-pointer memory locations which is the usual case for pthread objects.
    // Example: pthread_mutex_lock(&m);
    ImmutableSet<SeqMemoryLocation> nonPointerMemoryLocations =
        PthreadUtil.getNonPointerMemoryLocationsByPthreadObject(
            accessedMemoryLocations, pObjectType);
    if (!nonPointerMemoryLocations.isEmpty()) {
      checkState(
          nonPointerMemoryLocations.size() == 1,
          "nonPointerMemoryLocations must have exactly one element.");
      return buildThreadSyncStatementsByObjectType(
          pStatementType,
          pFunctionType,
          pObjectType,
          pSubstituteEdge,
          Iterables.getOnlyElement(nonPointerMemoryLocations));
    }

    // Accesses to pthread object pointers are treated as dereferences, even if they are not
    // dereferences in the input program because the pthread function dereferences the pointer.
    //
    // Example: pthread_mutex_t *m_ptr; m_ptr = &m; pthread_mutex_lock(m_ptr);
    //
    // 'm_ptr' in the call to 'pthread_mutex_lock' is not dereferenced, but it should be treated as
    // dereferenced to find the associated memory locations of 'm_ptr'.
    ImmutableSet<SeqMemoryLocation> pointerMemoryLocations =
        PthreadUtil.getPointerMemoryLocationsByPthreadObjectType(
            accessedMemoryLocations, pObjectType);
    // the pointer can target multiple memory locations, but there is only a single pointer
    checkState(
        pointerMemoryLocations.size() == 1,
        "pPointerMemoryLocations must have exactly one element.");

    ImmutableSet<SeqMemoryLocation> memoryLocations =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereferences(
            pointerMemoryLocations, memoryModel);
    checkState(!memoryLocations.isEmpty(), "pMemoryLocations is empty");

    InputRejection.checkPthreadObjectPointerAliasing(pointerMemoryLocations, memoryLocations);

    return buildThreadSyncStatementsByObjectType(
        pStatementType,
        pFunctionType,
        pObjectType,
        pSubstituteEdge,
        Iterables.getOnlyElement(memoryLocations));
  }

  private ImmutableList<CCompoundStatementElement> buildThreadSyncStatementsByObjectType(
      SeqThreadStatementType pStatementType,
      PthreadFunctionType pFunctionType,
      PthreadObjectType pObjectType,
      SubstituteEdge pSubstituteEdge,
      SeqMemoryLocation pMemoryLocation)
      throws UnrecognizedCodeException {

    return switch (pObjectType) {
      case PthreadObjectType.PTHREAD_COND_T -> {
        CondSignaledFlag condSignaledFlag = threadSyncFlags.getCondSignaledFlag(pMemoryLocation);
        yield buildCondStatements(pStatementType, pSubstituteEdge, condSignaledFlag);
      }
      case PthreadObjectType.PTHREAD_MUTEX_T -> {
        MutexLockedFlag mutexLockedFlag = threadSyncFlags.getMutexLockedFlag(pMemoryLocation);
        yield buildMutexStatements(pStatementType, mutexLockedFlag);
      }
      case PthreadObjectType.PTHREAD_RWLOCK_T -> {
        RwLockNumReadersWritersFlag rwLockFlag = threadSyncFlags.getRwLockFlag(pMemoryLocation);
        yield buildRwLockStatements(pFunctionType, rwLockFlag);
      }
      default -> throw new IllegalArgumentException("Invalid pthread object: " + pObjectType.name);
    };
  }

  private ImmutableList<CCompoundStatementElement> buildCondStatements(
      SeqThreadStatementType pStatementType,
      SubstituteEdge pSubstituteEdge,
      CondSignaledFlag pCondSignaledFlag)
      throws UnrecognizedCodeException {

    return switch (pStatementType) {
      case COND_SIGNAL -> {
        CExpressionAssignmentStatement setCondSignaledTrue =
            SeqStatementBuilder.buildExpressionAssignmentStatement(
                pCondSignaledFlag.idExpression(), CIntegerLiteralExpression.ONE);
        yield ImmutableList.of(new CStatementWrapper(setCondSignaledTrue));
      }
      case COND_WAIT -> {
        // for a breakdown on this behavior, cf. https://linux.die.net/man/3/pthread_cond_wait
        // step 1: the calling thread blocks on the condition variable -> assume(signaled == 1)
        CFunctionCallStatement assumeSignaled =
            SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
                pCondSignaledFlag.isSignaledExpression());
        CExpressionAssignmentStatement setSignaledFalse =
            SeqStatementBuilder.buildExpressionAssignmentStatement(
                pCondSignaledFlag.idExpression(), CIntegerLiteralExpression.ZERO);

        // step 2: on return, the mutex is locked and owned by the calling thread
        ImmutableList<CCompoundStatementElement> mutexStatements =
            buildThreadSyncStatements(
                SeqThreadStatementType.MUTEX_LOCK,
                PthreadFunctionType.PTHREAD_MUTEX_LOCK,
                PthreadObjectType.PTHREAD_MUTEX_T,
                pSubstituteEdge);
        yield elementsAndList(
            new CStatementWrapper(assumeSignaled),
            new CStatementWrapper(setSignaledFalse),
            mutexStatements);
      }
      default ->
          throw new IllegalArgumentException(
              String.format(
                  "pStatementType is not a pthread_cond_t statement: %s", pStatementType));
    };
  }

  private ImmutableList<CCompoundStatementElement> buildMutexStatements(
      SeqThreadStatementType pStatementType, MutexLockedFlag pMutexLockedFlag) {

    return switch (pStatementType) {
      case COND_WAIT -> {
        CExpressionAssignmentStatement setMutexLockedTrue =
            SeqStatementBuilder.buildExpressionAssignmentStatement(
                pMutexLockedFlag.idExpression(), CIntegerLiteralExpression.ONE);
        yield ImmutableList.of(new CStatementWrapper(setMutexLockedTrue));
      }
      case MUTEX_LOCK -> {
        CFunctionCallStatement assumeCall =
            SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
                pMutexLockedFlag.notLockedExpression());
        CExpressionAssignmentStatement setMutexLockedTrue =
            SeqStatementBuilder.buildExpressionAssignmentStatement(
                pMutexLockedFlag.idExpression(), CIntegerLiteralExpression.ONE);
        yield ImmutableList.of(
            new CStatementWrapper(assumeCall), new CStatementWrapper(setMutexLockedTrue));
      }
      case MUTEX_UNLOCK -> {
        CExpressionAssignmentStatement lockedFalseAssignment =
            SeqStatementBuilder.buildExpressionAssignmentStatement(
                pMutexLockedFlag.idExpression(), CIntegerLiteralExpression.ZERO);
        yield ImmutableList.of(new CStatementWrapper(lockedFalseAssignment));
      }
      default ->
          throw new IllegalArgumentException(
              String.format(
                  "pStatementType is not a pthread_mutex_t statement: %s", pStatementType));
    };
  }

  private ImmutableList<CCompoundStatementElement> buildRwLockStatements(
      PthreadFunctionType pFunctionType, RwLockNumReadersWritersFlag pRwLockFlag) {

    return switch (pFunctionType) {
      case PTHREAD_RWLOCK_RDLOCK -> {
        CStatementWrapper assumption =
            new CStatementWrapper(
                SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
                    pRwLockFlag.writerEqualsZero()));
        CStatementWrapper rwLockReadersIncrement =
            new CStatementWrapper(pRwLockFlag.readersIncrement());
        yield ImmutableList.of(assumption, rwLockReadersIncrement);
      }
      case PTHREAD_RWLOCK_UNLOCK -> {
        CExpressionAssignmentStatement setNumWritersToZero =
            SeqStatementBuilder.buildExpressionAssignmentStatement(
                pRwLockFlag.writersIdExpression(), CIntegerLiteralExpression.ZERO);
        CIfStatement ifStatement =
            new CIfStatement(
                new CExpressionWrapper(pRwLockFlag.writerEqualsZero()),
                new CCompoundStatement(new CStatementWrapper(pRwLockFlag.readersDecrement())),
                new CCompoundStatement(new CStatementWrapper(setNumWritersToZero)));
        yield ImmutableList.of(new CStatementWrapper(setNumWritersToZero), ifStatement);
      }
      case PTHREAD_RWLOCK_WRLOCK -> {
        CExpressionAssignmentStatement setWritersToOne =
            SeqStatementBuilder.buildExpressionAssignmentStatement(
                pRwLockFlag.writersIdExpression(), CIntegerLiteralExpression.ONE);
        CFunctionCallStatement assumptionWriters =
            SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
                pRwLockFlag.writerEqualsZero());
        CFunctionCallStatement assumptionReaders =
            SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
                pRwLockFlag.readersEqualsZero());
        yield ImmutableList.of(
            new CStatementWrapper(assumptionWriters),
            new CStatementWrapper(assumptionReaders),
            new CStatementWrapper(setWritersToOne));
      }
      default ->
          throw new IllegalArgumentException(
              String.format("pFunctionType is not a pthread_rwlock_t function: %s", pFunctionType));
    };
  }

  // Helpers

  /**
   * Returns {@code true} if the resulting statement has only {@code pc} adjustments, i.e. no code
   * changing the input program state, {@code false} otherwise.
   *
   * <p>This is the case when:
   *
   * <ul>
   *   <li>{@code pSuccessor} marks the termination of a thread
   *   <li>{@code pSubstituteEdge} itself is a {@link BlankEdge} that is not a {@code while (1)}
   *       loop head
   *   <li>{@code pSubstituteEdge} is a {@code PTHREAD_MUTEX_INITIALIZER} assignment
   *   <li>{@code pSubstituteEdge} is a {@link CDeclarationEdge}, except for local variable
   *       declarations with an initializer
   *   <li>{@code pSubstituteEdge} is a call to a {@code pthread} function that is not explicitly
   *       handled.
   * </ul>
   */
  private boolean resultsInBlankStatement(SubstituteEdge pSubstituteEdge, CFANode pSuccessor)
      throws UnsupportedCodeException {

    // exiting start_routine of thread -> blank, just set pc = EXIT_PC;
    if (pSuccessor instanceof FunctionExitNode
        && pSuccessor.getFunction().equals(thread.startRoutine())) {
      return true;

    } else if (pSubstituteEdge.cfaEdge instanceof BlankEdge
        && !pSubstituteEdge.cfaEdge.getPredecessor().isLoopStart()) {
      // blank edges have no code
      assert pSubstituteEdge.cfaEdge.getCode().isEmpty();
      return true;

    } else if (PthreadUtil.isPthreadMutexInitializerAssignment(pSubstituteEdge.cfaEdge)) {
      // PTHREAD_MUTEX_INITIALIZER are similar to pthread_mutex_init, we exclude it
      return true;

    } else if (pSubstituteEdge.cfaEdge instanceof CDeclarationEdge declarationEdge) {
      // IMPORTANT: this step (checking for declaration edges) must come after checking for
      // PTHREAD_MUTEX_INITIALIZER (which may be inside a CDeclarationEdge too!)
      CDeclaration declaration = declarationEdge.getDeclaration();
      if (declaration instanceof CVariableDeclaration variableDeclaration) {
        // all variables, functions, structs... are declared outside the main function,
        // EXCEPT local variables that have an initializer:
        return !(!variableDeclaration.isGlobal() && variableDeclaration.getInitializer() != null);
      }
      return true;

    } else {
      Optional<CFunctionCall> functionCall =
          PthreadUtil.tryGetFunctionCallFromCfaEdge(pSubstituteEdge.cfaEdge);
      if (functionCall.isPresent()) {
        if (PthreadUtil.isCallToAnyPthreadFunction(functionCall.orElseThrow())) {
          // not explicitly handled PthreadFunc -> empty case code
          return !PthreadUtil.isExplicitlyHandledPthreadFunction(pSubstituteEdge.cfaEdge);
        }
      }
    }
    return false;
  }

  private static CStatementWrapper buildExpressionAssignmentStatementFromVariableDeclaration(
      CVariableDeclaration pVariableDeclaration) throws UnsupportedCodeException {

    if (!(pVariableDeclaration.getInitializer() instanceof CInitializerExpression)) {
      throw new UnsupportedCodeException(
          "The sequentialization does not support CInitializer other than CInitializerExpression"
              + " for local variables.",
          null);
    }
    // the local variable is declared outside main() without an initializer e.g. 'int x;', and here
    // it is assigned the initializer e.g. 'x = 7;'
    CIdExpression idExpression =
        new CIdExpression(pVariableDeclaration.getFileLocation(), pVariableDeclaration);
    CExpressionAssignmentStatement assignmentStatement =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY,
            idExpression,
            ((CInitializerExpression) pVariableDeclaration.getInitializer()).getExpression());
    return new CStatementWrapper(assignmentStatement);
  }

  private static boolean isExcludedSummaryEdge(CFAEdge pCfaEdge) {
    return pCfaEdge instanceof CFunctionSummaryEdge
        || pCfaEdge instanceof CFunctionSummaryStatementEdge;
  }
}
