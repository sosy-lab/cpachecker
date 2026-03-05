// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatementElement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CLabelStatement;

/**
 * A block features a {@code goto} label and a list of {@link SeqThreadStatement}. An inner block is
 * only reachable from inside a thread simulation via its {@code goto} label.
 */
public final class SeqThreadStatementBlock implements SeqExportStatement {

  private final int threadId;

  /**
   * The goto label for the block, e.g. {@code T0_42;}. It is mandatory for all blocks, but may not
   * actually be targeted with a {@code goto}.
   */
  private final int labelNumber;

  private final ImmutableList<SeqThreadStatement> statements;

  private final Optional<CLabelStatement> nextThreadLabel;

  public SeqThreadStatementBlock(
      int pThreadId,
      int pLabelNumber,
      ImmutableList<SeqThreadStatement> pStatements,
      Optional<CLabelStatement> pNextThreadLabel) {

    checkArgument(
        pStatements.size() == 1 || pStatements.size() == 2,
        "pStatements must have either 1 or 2 elements");
    threadId = pThreadId;
    labelNumber = pLabelNumber;
    statements = pStatements;
    nextThreadLabel = pNextThreadLabel;
  }

  public CLabelStatement buildLabelStatement() {
    return new CLabelStatement(
        SeqNameUtil.buildThreadStatementBlockLabelName(threadId, labelNumber));
  }

  @Override
  public ImmutableList<CCompoundStatementElement> toCExportAstNodes() {
    ImmutableList.Builder<CCompoundStatementElement> exportStatements = ImmutableList.builder();

    exportStatements.add(buildLabelStatement());

    if (statements.size() == 1) {
      // 1 statement: add its respective export statements
      exportStatements.addAll(statements.getFirst().toCExportAstNodes());

    } else {
      // 2 statements (= assume statements): create if-else statement
      SeqThreadStatement firstAssume = statements.getFirst();
      SeqThreadStatementDataWithIfExpression firstAssumeData =
          (SeqThreadStatementDataWithIfExpression) firstAssume.data();
      SeqThreadStatement secondAssume = statements.getLast();
      CIfStatement ifStatement =
          new CIfStatement(
              new CExpressionWrapper(firstAssumeData.getIfExpression()),
              new CCompoundStatement(firstAssume.toCExportAstNodes()),
              new CCompoundStatement(secondAssume.toCExportAstNodes()));
      exportStatements.add(ifStatement);
    }

    return exportStatements.build();
  }

  public int getLabelNumber() {
    return labelNumber;
  }

  public SeqThreadStatement getFirstStatement() {
    return statements.getFirst();
  }

  public ImmutableList<SeqThreadStatement> getStatements() {
    return statements;
  }

  public Optional<CLabelStatement> getNextThreadLabel() {
    return nextThreadLabel;
  }

  public boolean isLoopHead() {
    return statements.stream().anyMatch(s -> s.data().isLoopHead());
  }

  public SeqThreadStatementBlock withLabelNumber(int pLabelNumber) {
    return new SeqThreadStatementBlock(threadId, pLabelNumber, statements, nextThreadLabel);
  }

  public SeqThreadStatementBlock withStatements(ImmutableList<SeqThreadStatement> pStatements) {
    checkArgument(
        statements.size() == pStatements.size(),
        "pStatements.size() must be equal to the existing statements.size()");
    return new SeqThreadStatementBlock(threadId, labelNumber, pStatements, nextThreadLabel);
  }

  /** Whether this block begins with {@code __VERIFIER_atomic_begin();}. */
  public boolean startsAtomicBlock() {
    return getFirstStatement().data().getType().equals(SeqThreadStatementType.ATOMIC_BEGIN);
  }

  /**
   * Whether this block begins in an atomic block. This is true for all blocks after {@code
   * __VERIFIER_atomic_begin();}, but not {@code __VERIFIER_atomic_begin();} itself.
   */
  public boolean startsInAtomicBlock() {
    return SeqThreadStatementUtil.startsInAtomicBlock(getFirstStatement());
  }
}
