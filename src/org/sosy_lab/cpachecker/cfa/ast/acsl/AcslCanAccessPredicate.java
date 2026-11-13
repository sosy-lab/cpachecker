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
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * The witness expression extension keyword {@code \canAccess(x, len)}, e.g. {@code
 * \canAccess(start->next, 1)}, of the witness format version 2.4. It is {@code 1} if dereferencing
 * {@code x + i} does not result in undefined behavior for all {@code 0 <= i < len}, and {@code 0}
 * otherwise. {@code x} must be a pointer {@code T*} to a complete object type {@code T}, and {@code
 * len} of type {@code size_t}, i.e. the memory region consists of {@code len} elements of type
 * {@code T}. Both arguments may be ACSL or C expressions, the validation decides whether they are
 * sensible.
 *
 * @see <a
 *     href="https://gitlab.com/sosy-lab/benchmarking/sv-witnesses/-/blob/main/user-guide/Witness-Format.md#expression-formats">Definition
 *     in the witness format</a>
 */
public final class AcslCanAccessPredicate extends AbstractExpression implements AcslPredicate {

  @Serial private static final long serialVersionUID = 3569337811428113170L;

  // This is expected to be something targeting memory. May also be an array.
  private final AExpression pointer;

  // Second argument describing the size of the access
  private final AExpression len;

  public AcslCanAccessPredicate(
      FileLocation pFileLocation, AExpression pPointer, AExpression pLenArgument) {
    super(pFileLocation, AcslBuiltinLogicType.BOOLEAN);
    pointer = checkNotNull(pPointer);
    len = checkNotNull(pLenArgument);
  }

  /**
   * Returns the argument {@code x} of {@code \canAccess(x, len)}, an ACSL or C expression that is
   * supposed to be a pointer {@code T*} to the start of the accessed memory region.
   */
  public AExpression getExpressionTargetingMemory() {
    return pointer;
  }

  /**
   * Returns the argument {@code len} of {@code \canAccess(x, len)}, an ACSL or C expression that is
   * supposed to be of type {@code size_t} and gives the number of elements of type {@code T} (not
   * bytes) in the accessed memory region.
   */
  public AExpression getLengthExpression() {
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(pointer);
    result = prime * result + Objects.hashCode(len);
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslCanAccessPredicate other
        && super.equals(other)
        && Objects.equals(other.pointer, pointer)
        && Objects.equals(other.len, len);
  }
}
