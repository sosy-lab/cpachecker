// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

/** Represents a single case statement in the sequentialization while loop. */
public class SeqLoopCaseStmt implements SeqElement {

  private final int threadId;

  /** True if this is an if, else-if or else statement (extra curly brackets required). */
  public final boolean isAssume;

  // TODO we also need the ThreadEdge (the original, not the substitute)
  //  the original ThreadEdge also contains the original CFAEdges which are needed for commutativity
  //  checks. the POR algorithm can then be run on the pruned switch-cases (because the reduction
  //  assumes only use the pc in the final sequentialization anyway)

  // TODO replace String with CExpression or CStatement later?
  public final Optional<String> statement;

  /**
   * The next pc of the current thread after this statement is executed. Can be empty if there are
   * multiple statements inside a single case.
   */
  public final Optional<Integer> targetPc;

  public SeqLoopCaseStmt(
      int pThreadId, boolean pIsAssume, Optional<String> pStatement, Optional<Integer> pTargetPc) {
    // if isAssume, then targetPc has to be present
    checkArgument(!pIsAssume || pTargetPc.isPresent());
    threadId = pThreadId;
    isAssume = pIsAssume;
    statement = pStatement;
    targetPc = pTargetPc;
  }

  protected SeqLoopCaseStmt cloneWithTargetPc(int pTargetPc) {
    return new SeqLoopCaseStmt(threadId, isAssume, statement, Optional.of(pTargetPc));
  }

  @Override
  public String toString() {
    Optional<CExpressionAssignmentStatement> pcUpdate =
        targetPc.isPresent()
            ? Optional.of(SeqStatements.buildPcAssign(threadId, targetPc.orElseThrow()))
            : Optional.empty();
    String pcUpdateString =
        isAssume && pcUpdate.isPresent()
            ? SeqUtil.wrapInCurlyInwards(pcUpdate.orElseThrow().toASTString())
            : targetPc.isPresent() ? pcUpdate.orElseThrow().toASTString() : SeqSyntax.EMPTY_STRING;
    if (statement.isEmpty()) {
      return pcUpdateString;
    } else {
      if (pcUpdateString.isEmpty()) {
        return statement.orElseThrow();
      } else {
        return statement.orElseThrow() + SeqSyntax.SPACE + pcUpdateString;
      }
    }
  }
}
