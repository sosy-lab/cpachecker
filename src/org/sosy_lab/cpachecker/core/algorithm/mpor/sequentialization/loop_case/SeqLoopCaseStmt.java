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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.AssignExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExprBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

/** Represents a single case statement in the sequentialization while loop. */
public class SeqLoopCaseStmt implements SeqElement {

  /** True if this is an if, else-if or else statement (extra curly brackets required). */
  public final boolean isAssume;

  public final String statement;

  /**
   * The next pc of the current thread after this statement is executed. Can be empty if there are
   * multiple statements inside a single case.
   */
  public final Optional<Integer> targetPc;

  public SeqLoopCaseStmt(boolean pIsAssume, String pStatement, Optional<Integer> pTargetPc) {
    // if isAssume, then targetPc has to be present
    checkArgument(!pIsAssume || pTargetPc.isPresent());
    isAssume = pIsAssume;
    statement = pStatement;
    targetPc = pTargetPc;
  }

  @Override
  public String createString() {
    AssignExpr pcsUpdate =
        targetPc.isPresent()
            ? SeqExprBuilder.createPcsNextThreadAssign(targetPc.orElseThrow())
            : null;
    String pcsUpdateString =
        isAssume && pcsUpdate != null
            ? SeqUtil.wrapInCurlyBrackets(pcsUpdate)
            : targetPc.isPresent() ? pcsUpdate.createString() : SeqSyntax.EMPTY_STRING;
    return statement + SeqSyntax.SPACE + pcsUpdateString;
  }
}
