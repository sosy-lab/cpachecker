// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/**
 * Represents a statement that simulates calls to {@code pthread_create} of the form:
 *
 * <p>{@code pc[i] = 0; pc[j] = n; } where thread {@code j} creates thread {@code i}.
 */
public class SeqThreadCreationStatement implements SeqCaseBlockStatement {

  private final int createdThreadId;

  private final int threadId;

  private final Optional<Integer> targetPc;

  private final Optional<CExpression> targetPcExpression;

  private final Optional<ImmutableList<SeqCaseBlockStatement>> concatenatedStatements;

  private final PcVariables pcVariables;

  SeqThreadCreationStatement(
      int pCreatedThreadId, int pThreadId, int pTargetPc, PcVariables pPcVariables) {

    createdThreadId = pCreatedThreadId;
    threadId = pThreadId;
    targetPc = Optional.of(pTargetPc);
    targetPcExpression = Optional.empty();
    concatenatedStatements = Optional.empty();
    pcVariables = pPcVariables;
  }

  private SeqThreadCreationStatement(
      int pCreatedThreadId, int pThreadId, CExpression pTargetPc, PcVariables pPcVariables) {

    createdThreadId = pCreatedThreadId;
    threadId = pThreadId;
    targetPc = Optional.empty();
    targetPcExpression = Optional.of(pTargetPc);
    concatenatedStatements = Optional.empty();
    pcVariables = pPcVariables;
  }

  private SeqThreadCreationStatement(
      int pCreatedThreadId,
      int pThreadId,
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements,
      PcVariables pPcVariables) {

    createdThreadId = pCreatedThreadId;
    threadId = pThreadId;
    targetPc = Optional.empty();
    targetPcExpression = Optional.empty();
    concatenatedStatements = Optional.of(pConcatenatedStatements);
    pcVariables = pPcVariables;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement createdPcWrite =
        SeqStatementBuilder.buildPcWrite(
            pcVariables.get(createdThreadId), Sequentialization.INIT_PC);
    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcVariables.get(threadId), targetPc, targetPcExpression, concatenatedStatements);
    return createdPcWrite.toASTString() + SeqSyntax.SPACE + targetStatements;
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
  public SeqThreadCreationStatement cloneWithTargetPc(CExpression pTargetPc) {
    return new SeqThreadCreationStatement(createdThreadId, threadId, pTargetPc, pcVariables);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {
    return new SeqThreadCreationStatement(
        createdThreadId, threadId, pConcatenatedStatements, pcVariables);
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
