// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * Formally this differs from the ACSL syntax, since there a literal can only be contained in a term
 * and a term is not an expression. However, to be consistent with the other {@link AExpression}'s,
 * we consider this too an expression. The semantics of this is the same as in C i.e. 0 is
 * equivalent false and everything else is equivalent to true.
 */
public abstract sealed class AcslLiteralTerm extends AcslTerm
    permits AcslBooleanLiteralTerm,
        AcslCharLiteralTerm,
        AcslIntegerLiteralTerm,
        AcslRealLiteralTerm,
        AcslStringLiteralTerm {

  @Serial
  private static final long serialVersionUID = -81455251151276L;

  protected AcslLiteralTerm(FileLocation pLocation, AcslType pType) {
    super(pLocation, pType);
  }

  public abstract Object getValue();

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return getValue().toString();
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(" + toASTString(pAAstNodeRepresentation) + ")";
  }

  @Override
  public int hashCode() {
    return getValue().hashCode();
  }
}
