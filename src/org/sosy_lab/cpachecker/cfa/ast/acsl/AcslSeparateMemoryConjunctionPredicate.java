// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import java.util.List;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslBuiltinLogicType;

/**
 * Predicates that describe that the memory regions inside each C argument are disjunct (i.e.
 * distinct memory regions). Contrary to SL (which uses *) we use a function {@code \separated(e_1,
 * ..., e_n)} with n >= 1. Example:
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
 *   // Here we can guaratee that: \separated(sll, sll->next);
 *   // Or even stronger, that a recursive function call describing the rest of the list is separate
 *   // in \separated(sll, sll_inv(sll->next));
 *   ...
 * }</code>
 * </pre>
 */
public final class AcslSeparateMemoryConjunctionPredicate extends AbstractExpression
    implements AcslPredicate {

  @Serial private static final long serialVersionUID = -2029723119416804891L;

  // All expressions are expected to target some memory. May also be an array for example.
  private final List<AExpression> disjunctMemory;

  public AcslSeparateMemoryConjunctionPredicate(
      FileLocation pFileLocation, List<AExpression> pDisjunctMemory) {
    super(pFileLocation, AcslBuiltinLogicType.BOOLEAN);
    checkNotNull(pDisjunctMemory);
    checkArgument(!pDisjunctMemory.isEmpty());
    disjunctMemory = pDisjunctMemory;
  }

  /**
   * Returns all n {@link CExpression}s for the n >= 1 arguments describing disjunct memory regions
   * per argument.
   */
  public List<AExpression> getSeparatedArguments() {
    return disjunctMemory;
  }

  @Override
  public <R, X extends Exception> R accept(AcslPredicateVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
  ;

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "\\separated("
        + disjunctMemory.stream()
            .map(e -> e.toASTString(pAAstNodeRepresentation))
            .collect(Collectors.joining(", "))
        + ")";
  }
}
