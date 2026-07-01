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
 * Predicates that describe that the two (C) memory regions inside are disjoint (i.e. different
 * memory regions). Contrary to SL (which uses *) we use the symbol @ (so that we don't accidentally
 * confuse the symbol with dereference in C). Example:
 *
 * <pre>
 * <code>struct sll {
 *    struct sll *next;
 *  };</code>
 * </pre>
 *
 * <pre>
 * <code>struct sll* create(void) {
 *   struct sll *sll = malloc(sizeof(sll));
 *   if (sll == 0) return 0;
 *   sll->next = malloc(sizeof(sll));
 *   if (sll->next == 0) return 0;
 *   // Here we can guaratee that: (sll) @ (sll->next)
 *   ...
 * }</code>
 * </pre>
 */
public final class AcslSeparateMemoryConjunctionPredicate extends AbstractExpression
    implements AcslPredicate {

  @Serial private static final long serialVersionUID = -2029723119416804891L;

  // TODO: decide whether we want input validation here, or in the visitor. Theoretically you may
  //  put every C statement here, but we know its invalid for e.g. integer types.
  // Both expressions are expected to target some memory. May also be an array for example.
  private final CExpression leftPointer;
  private final CExpression rightPointer;

  // TODO: the usual structure of CExpression in ACSL-Term in ACSL-Predicate is annoying here
  public AcslSeparateMemoryConjunctionPredicate(
      FileLocation pFileLocation, CExpression pLeftPointer, CExpression pRightPointer) {
    super(pFileLocation, AcslBuiltinLogicType.BOOLEAN);
    leftPointer = checkNotNull(pLeftPointer);
    rightPointer = checkNotNull(pRightPointer);
  }

  /**
   * @return the {@link CExpression} left statement that is supposed to target some memory that is
   *     separate from the right memory. If it does not, this is an invalid witness!
   */
  public CExpression getLeftCExpressionTargetingMemory() {
    return leftPointer;
  }

  /**
   * @return the {@link CExpression} right statement that is supposed to target some memory that is
   *     separate from the left memory. If it does not, this is an invalid witness!
   */
  public CExpression getRightCExpressionTargetingMemory() {
    return rightPointer;
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
    return "\\("
        + leftPointer.toASTString(pAAstNodeRepresentation)
        + " @ "
        + rightPointer.toASTString(pAAstNodeRepresentation)
        + ")";
  }
}
