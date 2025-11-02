// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3TraceEntryCall extends K3SelectTraceComponent {
  @Serial private static final long serialVersionUID = 5543731065650175240L;
  private final K3ProcedureDeclaration declaration;
  private final ImmutableList<K3ConstantTerm> constantTerms;

  public K3TraceEntryCall(
      K3ProcedureDeclaration pDeclaration,
      ImmutableList<K3ConstantTerm> pConstantTerms,
      FileLocation pFileLocation) {
    super(pFileLocation);
    declaration = pDeclaration;
    constantTerms = pConstantTerms;
  }

  public K3ProcedureDeclaration getDeclaration() {
    return declaration;
  }

  public ImmutableList<K3ConstantTerm> getConstantTerms() {
    return constantTerms;
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  <R, X extends Exception> R accept(K3TraceElementVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(entry-call "
        + declaration.getName()
        + " "
        + Joiner.on(" ")
            .join(FluentIterable.from(constantTerms).transform(K3ConstantTerm::toASTString))
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + declaration.hashCode();
    result = prime * result + constantTerms.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3TraceEntryCall other
        && declaration.equals(other.declaration)
        && constantTerms.equals(other.constantTerms);
  }
}
