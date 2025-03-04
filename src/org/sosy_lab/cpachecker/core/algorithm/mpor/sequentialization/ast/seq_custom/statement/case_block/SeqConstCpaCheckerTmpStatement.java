// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
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

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final Optional<CExpression> targetPcExpression;

  private final Optional<ImmutableList<SeqCaseBlockStatement>> concatenatedStatements;

  private void checkArguments(
      CDeclarationEdge pDeclaration, SubstituteEdge pStatementA, SubstituteEdge pStatementB) {

    checkArgument(
        pDeclaration.getDeclaration() instanceof CVariableDeclaration,
        "pDeclaration must be CVariableDeclaration");
    checkArgument(
        MPORUtil.isConstCpaCheckerTmp((CVariableDeclaration) pDeclaration.getDeclaration()),
        "pDeclaration must declare a const __CPAchecker_TMP variable");
    checkArgument(
        pStatementA.cfaEdge instanceof CStatementEdge,
        "pStatementA.cfaEdge must be CStatementEdge");
    checkArgument(
        pStatementB.cfaEdge instanceof CStatementEdge,
        "pStatementB.cfaEdge must be CStatementEdge");

    CSimpleDeclaration declarationA = pDeclaration.getDeclaration();
    CExpressionStatement statement =
        (CExpressionStatement) ((CStatementEdge) pStatementB.cfaEdge).getStatement();
    CIdExpression idExpressionB = (CIdExpression) statement.getExpression();
    CSimpleDeclaration declarationB = idExpressionB.getDeclaration();

    checkArgument(
        declarationA.equals(declarationB),
        "pDeclaration and pStatementB must use the same __CPAchecker_TMP variable");
  }

  SeqConstCpaCheckerTmpStatement(
      CDeclarationEdge pDeclaration,
      SubstituteEdge pStatementA,
      SubstituteEdge pStatementB,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    checkArguments(pDeclaration, pStatementA, pStatementB);
    statementA = pStatementA;
    statementB = pStatementB;
    declaration = pDeclaration;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    targetPcExpression = Optional.empty();
    concatenatedStatements = Optional.empty();
  }

  private SeqConstCpaCheckerTmpStatement(
      CDeclarationEdge pDeclaration,
      SubstituteEdge pStatementA,
      SubstituteEdge pStatementB,
      CLeftHandSide pPcLeftHandSide,
      CExpression pTargetPcExpression) {

    checkArguments(pDeclaration, pStatementA, pStatementB);
    statementA = pStatementA;
    statementB = pStatementB;
    declaration = pDeclaration;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.of(pTargetPcExpression);
    concatenatedStatements = Optional.empty();
  }

  private SeqConstCpaCheckerTmpStatement(
      CDeclarationEdge pDeclaration,
      SubstituteEdge pStatementA,
      SubstituteEdge pStatementB,
      CLeftHandSide pPcLeftHandSide,
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    checkArguments(pDeclaration, pStatementA, pStatementB);
    statementA = pStatementA;
    statementB = pStatementB;
    declaration = pDeclaration;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.empty();
    concatenatedStatements = Optional.of(pConcatenatedStatements);
  }

  @Override
  public String toASTString() {
    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, targetPcExpression, concatenatedStatements);
    return declaration.getCode()
        + SeqSyntax.SPACE
        + statementA.cfaEdge.getCode()
        + SeqSyntax.SPACE
        + statementB.cfaEdge.getCode()
        + SeqSyntax.SPACE
        + targetStatements;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public Optional<CExpression> getTargetPcExpression() {
    return targetPcExpression;
  }

  @Override
  public Optional<ImmutableList<SeqCaseBlockStatement>> getConcatenatedStatements() {
    return concatenatedStatements;
  }

  @Override
  public SeqConstCpaCheckerTmpStatement cloneWithTargetPc(CExpression pTargetPc) {
    return new SeqConstCpaCheckerTmpStatement(
        declaration, statementA, statementB, pcLeftHandSide, pTargetPc);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    return new SeqConstCpaCheckerTmpStatement(
        declaration, statementA, statementB, pcLeftHandSide, pConcatenatedStatements);
  }

  @Override
  public boolean isConcatenable() {
    return true;
  }

  @Override
  public boolean alwaysWritesPc() {
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
