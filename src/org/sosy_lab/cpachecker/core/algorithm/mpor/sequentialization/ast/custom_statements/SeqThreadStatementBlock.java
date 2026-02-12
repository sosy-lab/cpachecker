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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CBreakStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CContinueStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CGotoStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CLabelStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CReturnStatementWrapper;

/**
 * A block features a {@code goto} label and a list of {@link SeqThreadStatement}. An inner block is
 * only reachable from inside a thread simulation via its {@code goto} label.
 */
public final class SeqThreadStatementBlock implements SeqExportStatement {

  private final MPOROptions options;

  private final int threadId;

  /**
   * The goto label for the block, e.g. {@code T0_42;}. It is mandatory for all blocks, but may not
   * actually be targeted with a {@code goto}.
   */
  private final int labelNumber;

  private final ImmutableList<SeqThreadStatement> statements;

  private final boolean isLoopStart;

  private final Optional<CLabelStatement> nextThreadLabel;

  public SeqThreadStatementBlock(
      MPOROptions pOptions,
      int pThreadId,
      int pLabelNumber,
      ImmutableList<SeqThreadStatement> pStatements,
      Optional<CLabelStatement> pNextThreadLabel) {

    checkArgument(
        pStatements.size() == 1 || pStatements.size() == 2,
        "pStatements must have either 1 or 2 elements");
    options = pOptions;
    threadId = pThreadId;
    labelNumber = pLabelNumber;
    statements = pStatements;
    isLoopStart = SeqThreadStatementBlockUtil.isLoopStart(statements);
    nextThreadLabel = pNextThreadLabel;
  }

  public CLabelStatement buildLabelStatement() {
    return new CLabelStatement(
        SeqNameUtil.buildThreadStatementBlockLabelName(threadId, labelNumber));
  }

  @Override
  public ImmutableList<CExportStatement> toCExportStatements() {
    ImmutableList.Builder<CExportStatement> exportStatements = ImmutableList.builder();

    exportStatements.add(buildLabelStatement());

    if (statements.size() == 1) {
      // 1 statement: add its respective export statements
      exportStatements.addAll(statements.getFirst().toCExportStatements());

    } else {
      // 2 statements (= assume statements): create if-else statement
      SeqThreadStatement firstAssume = statements.getFirst();
      SeqThreadStatement secondAssume = statements.getLast();
      CIfStatement ifStatement =
          new CIfStatement(
              new CExpressionWrapper(firstAssume.data().ifExpression().orElseThrow()),
              new CCompoundStatement(firstAssume.toCExportStatements()),
              new CCompoundStatement(secondAssume.toCExportStatements()));
      exportStatements.add(ifStatement);
    }

    tryBuildBlockSuffix().ifPresent(s -> exportStatements.add(s));

    return exportStatements.build();
  }

  private Optional<CExportStatement> tryBuildBlockSuffix() {
    // if all statements have a 'goto', then the suffix is never reached
    if (SeqThreadStatementUtil.allHaveTargetGoto(statements)) {
      return Optional.empty();
    }

    // if the bit vector evaluation is empty, 'abort();' is called and the suffix is never reached
    if (SeqThreadStatementUtil.anyContainsEmptyBitVectorEvaluationExpression(statements)) {
      return Optional.empty();
    }

    // use control encoding of the statement since we append the suffix to the statement
    return switch (options.controlEncodingStatement()) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build suffix for control encoding " + options.controlEncodingStatement());
      case BINARY_SEARCH_TREE, IF_ELSE_CHAIN -> {
        if (options.loopUnrolling()) {
          // with loop unrolling (and separate thread functions) enabled, always return to main()
          yield Optional.of(new CReturnStatementWrapper(Optional.empty()));
        }
        // if this is not the last thread, add "goto T{next_thread_ID};"
        if (nextThreadLabel.isPresent()) {
          yield Optional.of(new CGotoStatement(nextThreadLabel.orElseThrow()));
        }
        // otherwise, continue i.e. go to next loop iteration
        yield Optional.of(new CContinueStatement());
      }
      // for switch cases, add additional "break;" after each block, because CSwitchStatement
      // only adds "break;" after an entire compound statement i.e. after the last block
      case SWITCH_CASE -> Optional.of(new CBreakStatement());
    };
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

  public boolean isLoopStart() {
    return isLoopStart;
  }

  public SeqThreadStatementBlock withLabelNumber(int pLabelNumber) {
    return new SeqThreadStatementBlock(
        options, threadId, pLabelNumber, statements, nextThreadLabel);
  }

  public SeqThreadStatementBlock withStatements(ImmutableList<SeqThreadStatement> pStatements) {
    return new SeqThreadStatementBlock(
        options, threadId, labelNumber, pStatements, nextThreadLabel);
  }

  /** Whether this block begins with {@code __VERIFIER_atomic_begin();}. */
  public boolean startsAtomicBlock() {
    return getFirstStatement().data().type().equals(SeqThreadStatementType.ATOMIC_BEGIN);
  }

  /**
   * Whether this block begins in an atomic block. This is true for all blocks after {@code
   * __VERIFIER_atomic_begin();}, but not {@code __VERIFIER_atomic_begin();} itself.
   */
  public boolean startsInAtomicBlock() {
    return SeqThreadStatementUtil.startsInAtomicBlock(getFirstStatement());
  }
}
