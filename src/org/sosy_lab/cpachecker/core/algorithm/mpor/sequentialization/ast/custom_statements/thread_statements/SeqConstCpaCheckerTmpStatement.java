// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents a special CPAchecker case where a {@code const CPAchecker_TMP} variable is declared
 * and assigned inside a case clause.
 *
 * <p>A {@code const CPAchecker_TMP} is e.g. used for field references:
 *
 * <p>{@code const int __CPAchecker_TMP = q->head; q->head = (q->head) + 1; CPAchecker_TMP;}
 *
 * <p>The original code contained only one statement, but CPAchecker may transform it into 2 or 3.
 *
 * <p>Reasoning: given that we declare all variables outside the main function in the
 * sequentialization, a const declaration will be assigned an undeclared value e.g. {@code q->head}.
 */
public final class SeqConstCpaCheckerTmpStatement extends CSeqThreadStatement {

  private final CVariableDeclaration constCpaCheckerTmpDeclaration;

  private final SubstituteEdge firstSuccessorEdge;

  private final Optional<SubstituteEdge> secondSuccessorEdge;

  private void checkArguments(
      CVariableDeclaration pVariableDeclaration,
      SubstituteEdge pFirstSuccessorEdge,
      Optional<SubstituteEdge> pSecondSuccessorEdge) {

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

  private CIdExpression getIdExpressionFromSecondSuccessor(CExpression pExpression) {
    if (pExpression instanceof CIdExpression idExpression) {
      return idExpression;
    } else if (pExpression instanceof CPointerExpression pointerExpression) {
      if (pointerExpression.getOperand() instanceof CIdExpression idExpression) {
        return idExpression;
      }
    }
    throw new IllegalArgumentException(
        "pExpression must be either CIdExpression or CPointerExpression");
  }

  SeqConstCpaCheckerTmpStatement(
      CVariableDeclaration pDeclaration,
      SubstituteEdge pFirstSuccessorEdge,
      Optional<SubstituteEdge> pSecondSuccessorEdge,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    super(pSubstituteEdges, pPcLeftHandSide, pTargetPc);
    checkArguments(pDeclaration, pFirstSuccessorEdge, pSecondSuccessorEdge);
    firstSuccessorEdge = pFirstSuccessorEdge;
    secondSuccessorEdge = pSecondSuccessorEdge;
    constCpaCheckerTmpDeclaration = pDeclaration;
  }

  private SeqConstCpaCheckerTmpStatement(
      CVariableDeclaration pConstCpaCheckerTmpDeclaration,
      SubstituteEdge pFirstSuccessorEdge,
      Optional<SubstituteEdge> pSecondSuccessorEdge,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(pSubstituteEdges, pPcLeftHandSide, pTargetPc, pTargetGoto, pInjectedStatements);
    checkArguments(pConstCpaCheckerTmpDeclaration, pFirstSuccessorEdge, pSecondSuccessorEdge);
    firstSuccessorEdge = pFirstSuccessorEdge;
    secondSuccessorEdge = pSecondSuccessorEdge;
    constCpaCheckerTmpDeclaration = pConstCpaCheckerTmpDeclaration;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    String substituteEdgeBString =
        secondSuccessorEdge
            .map(substituteEdge -> substituteEdge.cfaEdge.getCode())
            .orElse(SeqSyntax.EMPTY_STRING);

    String targetStatements =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            pcLeftHandSide, targetPc, targetGoto, injectedStatements, pAAstNodeRepresentation);

    return Joiner.on(SeqSyntax.SPACE)
        .join(
            constCpaCheckerTmpDeclaration.toASTString(pAAstNodeRepresentation),
            firstSuccessorEdge.cfaEdge.getCode(),
            substituteEdgeBString,
            targetStatements);
  }

  @Override
  public SeqConstCpaCheckerTmpStatement withTargetPc(int pTargetPc) {
    return new SeqConstCpaCheckerTmpStatement(
        constCpaCheckerTmpDeclaration,
        firstSuccessorEdge,
        secondSuccessorEdge,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqConstCpaCheckerTmpStatement(
        constCpaCheckerTmpDeclaration,
        firstSuccessorEdge,
        secondSuccessorEdge,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqConstCpaCheckerTmpStatement(
        constCpaCheckerTmpDeclaration,
        firstSuccessorEdge,
        secondSuccessorEdge,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        pInjectedStatements);
  }

  @Override
  public boolean isLinkable() {
    return true;
  }

  @Override
  public boolean synchronizesThreads() {
    return false;
  }
}
