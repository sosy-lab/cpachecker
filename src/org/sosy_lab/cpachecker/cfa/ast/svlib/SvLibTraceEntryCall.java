// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class SvLibTraceEntryCall extends SvLibSelectTraceComponent {
  @Serial private static final long serialVersionUID = 5543731065650175240L;
  private final SvLibProcedureDeclaration declaration;
  private final ImmutableList<SvLibConstantTerm> constantTerms;

  public SvLibTraceEntryCall(
      SvLibProcedureDeclaration pDeclaration,
      ImmutableList<SvLibConstantTerm> pConstantTerms,
      FileLocation pFileLocation) {
    super(pFileLocation);
    declaration = pDeclaration;
    constantTerms = pConstantTerms;
  }

  public SvLibProcedureDeclaration getDeclaration() {
    return declaration;
  }

  public ImmutableList<SvLibConstantTerm> getConstantTerms() {
    return constantTerms;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  <R, X extends Exception> R accept(SvLibTraceElementVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(entry-call "
        + declaration.getName()
        + " "
        + Joiner.on(" ")
            .join(FluentIterable.from(constantTerms).transform(SvLibConstantTerm::toASTString))
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

    return obj instanceof SvLibTraceEntryCall other
        && declaration.equals(other.declaration)
        && constantTerms.equals(other.constantTerms);
  }
}
