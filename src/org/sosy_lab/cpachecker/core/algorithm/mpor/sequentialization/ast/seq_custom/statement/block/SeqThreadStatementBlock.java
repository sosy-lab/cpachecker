// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SingleControlExpressionEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqAtomicBeginStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqAtomicEndStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class SeqThreadStatementBlock implements SeqStatement {

  private final MPOROptions options;

  private final Optional<MPORThread> nextThread;

  /**
   * The goto label for the block, e.g. {@code T0_42;}. It is mandatory for all blocks, but may not
   * actually be targeted with a {@code goto}.
   */
  private final SeqBlockLabelStatement label;

  private final ImmutableList<SeqThreadStatement> statements;

  public SeqThreadStatementBlock(
      MPOROptions pOptions,
      Optional<MPORThread> pNextThread,
      SeqBlockLabelStatement pLabel,
      ImmutableList<SeqThreadStatement> pStatements) {

    options = pOptions;
    nextThread = pNextThread;
    label = pLabel;
    statements = pStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    ImmutableList.Builder<String> lines = ImmutableList.builder();
    lines.add(label.toASTString() + SeqSyntax.SPACE);
    for (SeqThreadStatement statement : statements) {
      lines.add(statement.toASTString() + SeqSyntax.SPACE);
    }
    Optional<String> suffix =
        SeqStringUtil.tryBuildSuffixByMultiControlStatementEncoding(
            options, nextThread, statements);
    if (suffix.isPresent()) {
      lines.add(suffix.orElseThrow());
    }
    return SeqStringUtil.joinWithNewlines(lines.build());
  }

  public SeqBlockLabelStatement getLabel() {
    return label;
  }

  public SeqThreadStatement getFirstStatement() {
    return statements.getFirst();
  }

  public ImmutableList<SeqThreadStatement> getStatements() {
    return statements;
  }

  public boolean isLoopStart() {
    for (SeqThreadStatement statement : statements) {
      for (SubstituteEdge substituteEdge : statement.getSubstituteEdges()) {
        CFANode predecessorA = substituteEdge.cfaEdge.getPredecessor();
        if (predecessorA.isLoopStart()) {
          // simple for / while loop with predicate expression -> loop is in direct predecessor
          return true;
        } else {
          // infinite while (1) loop -> loop is in predecessor of predecessor
          for (CFAEdge enteringEdgeA : CFAUtils.enteringEdges(predecessorA)) {
            CFANode predecessorB = enteringEdgeA.getPredecessor();
            if (predecessorB.isLoopStart()) {
              for (CFAEdge enteringEdgeB : CFAUtils.enteringEdges(predecessorB)) {
                if (enteringEdgeB instanceof BlankEdge blankEdge) {
                  String description = blankEdge.getDescription();
                  if (description.equals(SingleControlExpressionEncoding.WHILE.keyword)) {
                    return true;
                  }
                }
              }
            }
          }
        }
      }
    }
    return false;
  }

  public SeqThreadStatementBlock cloneWithLabelNumber(int pLabelNumber) {
    return new SeqThreadStatementBlock(
        options, nextThread, label.cloneWithLabelNumber(pLabelNumber), statements);
  }

  public SeqThreadStatementBlock cloneWithStatements(
      ImmutableList<SeqThreadStatement> pStatements) {

    return new SeqThreadStatementBlock(options, nextThread, label, pStatements);
  }

  public boolean startsAtomicBlock() {
    return getFirstStatement() instanceof SeqAtomicBeginStatement;
  }

  public boolean endsAtomicBlock() {
    return getFirstStatement() instanceof SeqAtomicEndStatement;
  }

  public boolean startsInAtomicBlock() {
    return SeqThreadStatementUtil.startsInAtomicBlock(getFirstStatement());
  }
}
