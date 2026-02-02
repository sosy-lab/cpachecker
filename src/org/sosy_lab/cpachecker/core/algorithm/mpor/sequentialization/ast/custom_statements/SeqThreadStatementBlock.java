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
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CLabelStatement;

/**
 * A block features a {@code goto} label and a list of {@link CSeqThreadStatement}. An inner block
 * is only reachable from inside a thread simulation via its {@code goto} label.
 */
public final class SeqThreadStatementBlock implements SeqExportStatement {

  private final MPOROptions options;

  private final Optional<CLabelStatement> nextThreadLabel;

  /**
   * The goto label for the block, e.g. {@code T0_42;}. It is mandatory for all blocks, but may not
   * actually be targeted with a {@code goto}.
   */
  private final SeqBlockLabelStatement label;

  private final ImmutableList<CSeqThreadStatement> statements;

  private final boolean isLoopStart;

  public SeqThreadStatementBlock(
      MPOROptions pOptions,
      Optional<CLabelStatement> pNextThreadLabel,
      SeqBlockLabelStatement pLabel,
      ImmutableList<CSeqThreadStatement> pStatements) {

    checkArgument(
        pStatements.size() == 1 || pStatements.size() == 2,
        "pStatements must have either 1 or 2 elements");
    options = pOptions;
    nextThreadLabel = pNextThreadLabel;
    label = pLabel;
    statements = pStatements;
    isLoopStart = SeqThreadStatementBlockUtil.isLoopStart(statements);
  }

  @Override
  public ImmutableList<CExportStatement> toCExportStatements() throws UnrecognizedCodeException {
    // TODO
    throw new AssertionError();
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner joiner = new StringJoiner(SeqSyntax.NEWLINE);
    joiner.add(label.toASTString(pAAstNodeRepresentation) + SeqSyntax.SPACE);

    if (statements.size() == 1) {
      // 1 statement: just return toASTString
      joiner.add(statements.getFirst().toASTString(pAAstNodeRepresentation));

    } else {
      // 2 statements (= assume statements): create if-else statement
      SeqAssumeStatement firstAssume = (SeqAssumeStatement) statements.getFirst();
      SeqAssumeStatement secondAssume = (SeqAssumeStatement) statements.getLast();
      CIfStatement branchStatement =
          new CIfStatement(
              new CExpressionWrapper(firstAssume.ifExpression.orElseThrow()),
              new CCompoundStatement(firstAssume),
              new CCompoundStatement(secondAssume));
      joiner.add(branchStatement.toASTString(pAAstNodeRepresentation));
    }

    SeqStringUtil.tryBuildBlockSuffix(options, nextThreadLabel, statements, pAAstNodeRepresentation)
        .ifPresent(joiner::add);
    return joiner.toString();
  }

  public SeqBlockLabelStatement getLabel() {
    return label;
  }

  public CSeqThreadStatement getFirstStatement() {
    return statements.getFirst();
  }

  public ImmutableList<CSeqThreadStatement> getStatements() {
    return statements;
  }

  public boolean isLoopStart() {
    return isLoopStart;
  }

  public SeqThreadStatementBlock withLabelNumber(int pLabelNumber) {
    return new SeqThreadStatementBlock(
        options, nextThreadLabel, label.withLabelNumber(pLabelNumber), statements);
  }

  public SeqThreadStatementBlock withStatements(ImmutableList<CSeqThreadStatement> pStatements) {
    return new SeqThreadStatementBlock(options, nextThreadLabel, label, pStatements);
  }

  /** Whether this block begins with {@code __VERIFIER_atomic_begin();}. */
  public boolean startsAtomicBlock() {
    return getFirstStatement() instanceof SeqAtomicBeginStatement;
  }

  /**
   * Whether this block begins in an atomic block. This is true for all blocks after {@code
   * __VERIFIER_atomic_begin();}, but not {@code __VERIFIER_atomic_begin();} itself.
   */
  public boolean startsInAtomicBlock() {
    return SeqThreadStatementUtil.startsInAtomicBlock(getFirstStatement());
  }
}
