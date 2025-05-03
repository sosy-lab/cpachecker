// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqLoopHeadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
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

  private final Optional<SeqLoopHeadLabelStatement> loopHeadLabel;

  private final CVariableDeclaration constCpaCheckerTmpDeclaration;

  private final SubstituteEdge statementA;

  private final SubstituteEdge statementB;

  private final CLeftHandSide pcLeftHandSide;

  private final ImmutableSet<SubstituteEdge> substituteEdges;

  private final Optional<Integer> targetPc;

  private final Optional<String> targetGoto;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  private final ImmutableList<SeqThreadStatement> concatenatedStatements;

  private void checkArguments(
      CVariableDeclaration pVariableDeclaration,
      SubstituteEdge pStatementA,
      SubstituteEdge pStatementB) {

    checkArgument(
        MPORUtil.isConstCpaCheckerTmp(pVariableDeclaration),
        "pDeclaration must declare a const __CPAchecker_TMP variable");
    checkArgument(
        pStatementA.cfaEdge instanceof CStatementEdge,
        "pStatementA.cfaEdge must be CStatementEdge");
    checkArgument(
        pStatementB.cfaEdge instanceof CStatementEdge,
        "pStatementB.cfaEdge must be CStatementEdge");

    CExpressionStatement statement =
        (CExpressionStatement) ((CStatementEdge) pStatementB.cfaEdge).getStatement();
    CIdExpression idExpressionB = (CIdExpression) statement.getExpression();
    CSimpleDeclaration declarationB = idExpressionB.getDeclaration();

    checkArgument(
        pVariableDeclaration.equals(declarationB),
        "pDeclaration and pStatementB must use the same __CPAchecker_TMP variable");
  }

  SeqConstCpaCheckerTmpStatement(
      CVariableDeclaration pDeclaration,
      SubstituteEdge pStatementA,
      SubstituteEdge pStatementB,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    checkArguments(pDeclaration, pStatementA, pStatementB);
    loopHeadLabel = Optional.empty();
    statementA = pStatementA;
    statementB = pStatementB;
    constCpaCheckerTmpDeclaration = pDeclaration;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = Optional.of(pTargetPc);
    targetGoto = Optional.empty();
    injectedStatements = ImmutableList.of();
    concatenatedStatements = ImmutableList.of();
  }

  private SeqConstCpaCheckerTmpStatement(
      Optional<SeqLoopHeadLabelStatement> pLoopHeadLabel,
      CVariableDeclaration pConstCpaCheckerTmpDeclaration,
      SubstituteEdge pStatementA,
      SubstituteEdge pStatementB,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<String> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements,
      ImmutableList<SeqThreadStatement> pConcatenatedStatements) {

    checkArguments(pConstCpaCheckerTmpDeclaration, pStatementA, pStatementB);
    loopHeadLabel = pLoopHeadLabel;
    statementA = pStatementA;
    statementB = pStatementB;
    constCpaCheckerTmpDeclaration = pConstCpaCheckerTmpDeclaration;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    injectedStatements = pInjectedStatements;
    concatenatedStatements = pConcatenatedStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, targetGoto, injectedStatements, concatenatedStatements);
    // we only want name and initializer here, the declaration is done beforehand
    return SeqStringUtil.buildLoopHeadLabel(loopHeadLabel)
        + constCpaCheckerTmpDeclaration.toASTStringWithOnlyNameAndInitializer()
        + SeqSyntax.SPACE
        + statementA.cfaEdge.getCode()
        + SeqSyntax.SPACE
        + statementB.cfaEdge.getCode()
        + SeqSyntax.SPACE
        + targetStatements;
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
  public Optional<SeqLoopHeadLabelStatement> getLoopHeadLabel() {
    return loopHeadLabel;
  }

  @Override
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public ImmutableList<SeqThreadStatement> getConcatenatedStatements() {
    return concatenatedStatements;
  }

  @Override
  public SeqConstCpaCheckerTmpStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqConstCpaCheckerTmpStatement(
        loopHeadLabel,
        constCpaCheckerTmpDeclaration,
        statementA,
        statementB,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        SeqThreadStatementClauseUtil.replaceTargetGotoLabel(injectedStatements, pTargetPc),
        concatenatedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithTargetGoto(String pLabel) {
    return new SeqConstCpaCheckerTmpStatement(
        loopHeadLabel,
        constCpaCheckerTmpDeclaration,
        statementA,
        statementB,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {
    return new SeqConstCpaCheckerTmpStatement(
        loopHeadLabel,
        constCpaCheckerTmpDeclaration,
        statementA,
        statementB,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        pInjectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithLoopHeadLabel(SeqLoopHeadLabelStatement pLoopHeadLabel) {
    return new SeqConstCpaCheckerTmpStatement(
        Optional.of(pLoopHeadLabel),
        constCpaCheckerTmpDeclaration,
        statementA,
        statementB,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqThreadStatement> pConcatenatedStatements) {

    return new SeqConstCpaCheckerTmpStatement(
        loopHeadLabel,
        constCpaCheckerTmpDeclaration,
        statementA,
        statementB,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.empty(),
        injectedStatements,
        pConcatenatedStatements);
  }

  @Override
  public boolean isConcatenable() {
    return true;
  }

  @Override
  public boolean isCriticalSectionStart() {
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
