// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AbstractAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/** A set of memory locations, given by its elements. It is empty for {@code \empty}. */
public final class AcslMemoryLocationSet extends AbstractAstNode implements AcslAstNode {

  @Serial private static final long serialVersionUID = 1456718713245456789L;

  private final ImmutableList<AcslTerm> terms;

  public AcslMemoryLocationSet(FileLocation pFileLocation, List<AcslTerm> pTerms) {
    super(pFileLocation);
    terms = ImmutableList.copyOf(pTerms);
  }

  public ImmutableList<AcslTerm> getTerms() {
    return terms;
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return switch (terms.size()) {
      case 0 -> "\\empty";
      case 1 -> terms.getFirst().toASTString(pAAstNodeRepresentation);
      default ->
          "{"
              + Joiner.on(", ")
                  .join(Lists.transform(terms, t -> t.toASTString(pAAstNodeRepresentation)))
              + "}";
    };
  }

  @Override
  public int hashCode() {
    return 37 * super.hashCode() + terms.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslMemoryLocationSet other
        && super.equals(other)
        && other.terms.equals(terms);
  }
}
