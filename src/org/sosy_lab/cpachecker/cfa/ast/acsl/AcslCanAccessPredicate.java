// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;

/**
 * Predicates that describe that access to the (C) memory region inside is valid. E.g.
 * 'canAccess(start->next, sizeof(struct sll))'.
 */
public final class AcslCanAccessPredicate extends AbstractExpression implements AcslPredicate {

  @Serial private static final long serialVersionUID = 3569337811428113170L;

  // This is expected to be something targeting memory. May also be an array.
  private final CExpression pointer;

  // Second argument describing the size of the access
  private final CExpression len;

  // The usual structure of CExpression in ACSL-Term in ACSL-Predicate is annoying here.
  // This is a shortcut
  public AcslCanAccessPredicate(
      FileLocation pFileLocation, CExpression pPointer, CExpression pLenArgument) {
    super(pFileLocation, AcslBuiltinLogicType.BOOLEAN);
    pointer = checkNotNull(pPointer);
    len = checkNotNull(pLenArgument);
  }

  /**
   * Returns the left hand side argument x, that is a {@link CExpression} in the canAccess(x, len)
   * statement, supposed to target some memory whose access is supposed to be valid up to length len
   * of type x.
   */
  public CExpression getCExpressionTargetingMemory() {
    return pointer;
  }

  /**
   * Returns the right hand side argument len, that is a {@link CExpression} in the canAccess(x,
   * len) statement, supposed to describe the length up to which the access of type x is valid.
   */
  public CExpression getLengthCExpression() {
    return len;
  }

  @Override
  public <R, X extends Exception> R accept(AcslPredicateVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "\\canAccess("
        + pointer.toASTString(pAAstNodeRepresentation)
        + ", "
        + len.toASTString(pAAstNodeRepresentation)
        + ")";
  }
}
