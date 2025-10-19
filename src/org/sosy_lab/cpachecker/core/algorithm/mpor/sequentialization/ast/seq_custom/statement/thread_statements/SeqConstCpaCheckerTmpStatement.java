// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
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
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
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
 * <p>The sequentialization treats these 3 statements as one, i.e. atomic. The code generating this
 * class asserts that all usages of {@code const CPAchecker_TMP} vars have the above structure (3
 * statements: 1 {@link CDeclarationEdge} followed by 2 {@link CStatementEdge}s).
 *
 * <p>Reasoning: given that we declare all variables outside the main function in the
 * sequentialization, a const declaration will be assigned an undeclared value e.g. {@code q->head}.
 */
public class SeqConstCpaCheckerTmpStatement implements SeqThreadStatement {

  private final MPOROptions options;

  private final CVariableDeclaration constCpaCheckerTmpDeclaration;

  private final SubstituteEdge substituteEdgeA;

  private final Optional<SubstituteEdge> substituteEdgeB;

  private final CLeftHandSide pcLeftHandSide;

  private final ImmutableSet<SubstituteEdge> substituteEdges;

  private final Optional<Integer> targetPc;

  private final Optional<SeqBlockLabelStatement> targetGoto;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  private void checkArguments(
      CVariableDeclaration pVariableDeclaration,
      SubstituteEdge pSubstituteEdgeA,
      Optional<SubstituteEdge> pSubstituteEdgeB) {

    checkArgument(
        MPORUtil.isConstCpaCheckerTmp(pVariableDeclaration),
        "pDeclaration must declare a const __CPAchecker_TMP variable");
    checkArgument(
        pSubstituteEdgeA.cfaEdge instanceof CStatementEdge,
        "pSubstituteEdgeA.cfaEdge must be CStatementEdge");
    if (pSubstituteEdgeB.isPresent()) {
      checkArgument(
          pSubstituteEdgeB.orElseThrow().cfaEdge instanceof CStatementEdge,
          "pSubstituteEdgeB.cfaEdge must be CStatementEdge");

      CStatement statementB =
          ((CStatementEdge) pSubstituteEdgeB.orElseThrow().cfaEdge).getStatement();
      if (statementB instanceof CExpressionStatement bExpressionStatement) {
        CIdExpression idExpressionB = extractIdExpressionB(bExpressionStatement.getExpression());
        CSimpleDeclaration declarationB = idExpressionB.getDeclaration();
        checkArgument(
            pVariableDeclaration.equals(declarationB),
            "pDeclaration and pSubstituteEdgeB must use the same __CPAchecker_TMP variable when"
                + " pSubstituteEdgeB is a CExpressionStatement");

      } else if (statementB instanceof CExpressionAssignmentStatement bAssignment) {
        CStatement statementA = ((CStatementEdge) pSubstituteEdgeA.cfaEdge).getStatement();
        checkArgument(
            statementA instanceof CExpressionAssignmentStatement,
            "pSubstituteEdgeA must be CExpressionAssignmentStatement when pSubstituteEdgeB is a"
                + " CExpressionAssignmentStatement");
        CExpressionAssignmentStatement aAssignment = (CExpressionAssignmentStatement) statementA;
        if (pVariableDeclaration.getInitializer()
            instanceof CInitializerExpression initializerExpression) {
          if (initializerExpression.getExpression().equals(aAssignment.getLeftHandSide())) {
            if (bAssignment.getRightHandSide() instanceof CIdExpression bIdExpression) {
              // this happens e.g. in weaver/parallel-ticket-6.wvr.c
              // _Atomic int CPA_TMP_0 = t; t = t + 1; m1 = CPA_TMP_0;
              // we want to ensure that the declaration is equal to the RHS in the last statement
              checkArgument(
                  pVariableDeclaration.equals(bIdExpression.getDeclaration()),
                  "pVariableDeclaration must equal pSubstituteEdgeB RHS");
              return;
            }
          }
        }
        // this happens e.g. in ldv-races/race-2_2-container_of:
        // CPA_TMP_0 = {  }; CPA_TMP_1 = (struct my_data *)(((char *)mptr) - 40); data = CPA_TMP_1;
        // check if the middle statement LHS matches the last statements RHS (CPA_TMP_1)
        checkArgument(
            aAssignment.getLeftHandSide().equals(bAssignment.getRightHandSide()),
            "pSubstituteEdgeA LHS must equal pSubstituteEdgeB RHS when pSubstituteEdgeB is a"
                + " CExpressionAssignmentStatement");
      }
    }
  }

  private CIdExpression extractIdExpressionB(CExpression pExpression) {
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
      MPOROptions pOptions,
      CVariableDeclaration pDeclaration,
      SubstituteEdge pSubstituteEdgeA,
      Optional<SubstituteEdge> pSubstituteEdgeB,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    checkArguments(pDeclaration, pSubstituteEdgeA, pSubstituteEdgeB);
    options = pOptions;
    substituteEdgeA = pSubstituteEdgeA;
    substituteEdgeB = pSubstituteEdgeB;
    constCpaCheckerTmpDeclaration = pDeclaration;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = Optional.of(pTargetPc);
    targetGoto = Optional.empty();
    injectedStatements = ImmutableList.of();
  }

  private SeqConstCpaCheckerTmpStatement(
      MPOROptions pOptions,
      CVariableDeclaration pConstCpaCheckerTmpDeclaration,
      SubstituteEdge pSubstituteEdgeA,
      Optional<SubstituteEdge> pSubstituteEdgeB,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    checkArguments(pConstCpaCheckerTmpDeclaration, pSubstituteEdgeA, pSubstituteEdgeB);
    options = pOptions;
    substituteEdgeA = pSubstituteEdgeA;
    substituteEdgeB = pSubstituteEdgeB;
    constCpaCheckerTmpDeclaration = pConstCpaCheckerTmpDeclaration;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    injectedStatements = pInjectedStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    String substituteEdgeBString =
        substituteEdgeB
            .map(substituteEdge -> substituteEdge.cfaEdge.getCode())
            .orElse(SeqSyntax.EMPTY_STRING);

    String targetStatements =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            options, pcLeftHandSide, targetPc, targetGoto, injectedStatements);

    return Joiner.on(SeqSyntax.SPACE)
        .join(
            constCpaCheckerTmpDeclaration.toASTString(),
            substituteEdgeA.cfaEdge.getCode(),
            substituteEdgeBString,
            targetStatements);
  }

  @Override
  public ImmutableSet<SubstituteEdge> getSubstituteEdges() {
    return substituteEdges;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public Optional<SeqBlockLabelStatement> getTargetGoto() {
    return targetGoto;
  }

  @Override
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public SeqConstCpaCheckerTmpStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqConstCpaCheckerTmpStatement(
        options,
        constCpaCheckerTmpDeclaration,
        substituteEdgeA,
        substituteEdgeB,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqConstCpaCheckerTmpStatement(
        options,
        constCpaCheckerTmpDeclaration,
        substituteEdgeA,
        substituteEdgeB,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements);
  }

  @Override
  public SeqThreadStatement cloneReplacingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pReplacingInjectedStatements) {

    return new SeqConstCpaCheckerTmpStatement(
        options,
        constCpaCheckerTmpDeclaration,
        substituteEdgeA,
        substituteEdgeB,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        pReplacingInjectedStatements);
  }

  @Override
  public SeqThreadStatement cloneAppendingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pAppendedInjectedStatements) {

    return new SeqConstCpaCheckerTmpStatement(
        options,
        constCpaCheckerTmpDeclaration,
        substituteEdgeA,
        substituteEdgeB,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        SeqThreadStatementUtil.appendInjectedStatements(this, pAppendedInjectedStatements));
  }

  @Override
  public boolean isLinkable() {
    return true;
  }

  @Override
  public boolean synchronizesThreads() {
    return false;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
