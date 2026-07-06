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
 * 'canAccess(start->next)'
 */
public final class AcslCanAccessPredicate extends AbstractExpression implements AcslPredicate {

  @Serial private static final long serialVersionUID = 3569337811428113170L;

  // This is expected to be something targeting memory. May also be an array.
  // TODO: decide whether we want input validation here, or in the visitor. Theoretically you may
  //  put every C statement into canAccess(), but we know its invalid for e.g. integer types.
  private final CExpression pointer;

  // TODO: the usual structure of CExpression in ACSL-Term in ACSL-Predicate is annoying here
  public AcslCanAccessPredicate(FileLocation pFileLocation, CExpression pPointer) {
    super(pFileLocation, AcslBuiltinLogicType.BOOLEAN);
    pointer = checkNotNull(pPointer);
  }

  /**
   * Returns the {@link CExpression} in the canAccess() statement that is supposed to target some
   * memory whose access is supposed to be valid.
   *
   * @return the {@link CExpression} in the canAccess() statement that is supposed to target some
   *     memory.
   */
  public CExpression getCExpressionTargetingMemory() {
    return pointer;
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
    return "\\canAccess(" + pointer.toASTString(pAAstNodeRepresentation) + ")";
  }
}
