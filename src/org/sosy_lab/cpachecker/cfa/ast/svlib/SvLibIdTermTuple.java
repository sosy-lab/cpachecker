// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibProductType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public class SvLibIdTermTuple implements SvLibLeftHandSide, SvLibExpression {
  @Serial private static final long serialVersionUID = -559740395473649753L;
  private final FileLocation fileLocation;
  private final ImmutableList<SvLibIdTerm> idTerms;

  public SvLibIdTermTuple(FileLocation pFileLocation, List<SvLibIdTerm> pIdTerms) {
    fileLocation = pFileLocation;
    idTerms = ImmutableList.copyOf(pIdTerms);
  }

  public ImmutableList<SvLibIdTerm> getIdTerms() {
    return idTerms;
  }

  @Override
  public SvLibType getExpressionType() {
    return new SvLibProductType(
        transformedImmutableListCopy(idTerms, SvLibIdTerm::getExpressionType));
  }

  @Override
  public <R, X extends Exception> R accept(SvLibExpressionVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "("
        + String.join(
            ", ",
            FluentIterable.from(idTerms).transform(t -> t.toASTString(pAAstNodeRepresentation)))
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public int hashCode() {
    return idTerms.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibIdTermTuple other && idTerms.equals(other.idTerms);
  }
}
