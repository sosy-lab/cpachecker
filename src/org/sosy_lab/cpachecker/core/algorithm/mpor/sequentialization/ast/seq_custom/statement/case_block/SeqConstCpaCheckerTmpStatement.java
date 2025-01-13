// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;

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
public class SeqConstCpaCheckerTmpStatement implements SeqCaseBlockStatement {

  private final CDeclarationEdge declaration;

  private final SubstituteEdge statementA;

  private final SubstituteEdge statementB;

  private final int threadId;

  private final int targetPc;

  public SeqConstCpaCheckerTmpStatement(
      CDeclarationEdge pDeclaration,
      SubstituteEdge pStatementA,
      SubstituteEdge pStatementB,
      int pThreadId,
      int pTargetPc) {

    checkArgument(
        pDeclaration.getDeclaration() instanceof CVariableDeclaration,
        "pDeclaration must be CVariableDeclaration");
    checkArgument(
        SeqUtil.isConstCPAcheckerTMP((CVariableDeclaration) pDeclaration.getDeclaration()),
        "pDeclaration must declare a const __CPAchecker_TMP variable");
    checkArgument(
        pStatementA.cfaEdge instanceof CStatementEdge,
        "pStatementA.cfaEdge must be CStatementEdge");
    checkArgument(
        pStatementB.cfaEdge instanceof CStatementEdge,
        "pStatementB.cfaEdge must be CStatementEdge");

    statementA = pStatementA;
    statementB = pStatementB;

    CSimpleDeclaration decA = pDeclaration.getDeclaration();
    CExpressionStatement stmtB =
        (CExpressionStatement) ((CStatementEdge) statementB.cfaEdge).getStatement();
    CIdExpression idB = (CIdExpression) stmtB.getExpression();
    CSimpleDeclaration decB = idB.getDeclaration();

    checkArgument(
        decA.equals(decB),
        "pDeclaration and pStatementB must use the same __CPAchecker_TMP variable");

    declaration = pDeclaration;
    threadId = pThreadId;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcUpdate = SeqStatements.buildPcUpdate(threadId, targetPc);
    return declaration.getCode()
        + SeqSyntax.SPACE
        + statementA.cfaEdge.getCode()
        + SeqSyntax.SPACE
        + statementB.cfaEdge.getCode()
        + SeqSyntax.SPACE
        + pcUpdate.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }

  @NonNull
  @Override
  public SeqConstCpaCheckerTmpStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqConstCpaCheckerTmpStatement(
        declaration, statementA, statementB, threadId, pTargetPc);
  }

  @Override
  public boolean alwaysUpdatesPc() {
    return true;
  }
}
