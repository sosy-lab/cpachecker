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
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibProductType;

public class SvLibVariableDeclarationTuple extends AVariableDeclaration implements SvLibAstNode {
  @Serial private static final long serialVersionUID = -559740395473649753L;
  private final ImmutableList<SvLibVariableDeclaration> declarations;

  public SvLibVariableDeclarationTuple(
      FileLocation pFileLocation, List<SvLibVariableDeclaration> pDeclarations) {
    super(
        pFileLocation,
        false,
        new SvLibProductType(
            transformedImmutableListCopy(pDeclarations, SvLibVariableDeclaration::getType)),
        "tuple of variables",
        "tuple of variables",
        "tuple of variables",
        null);
    declarations = ImmutableList.copyOf(pDeclarations);
  }

  public ImmutableList<SvLibVariableDeclaration> getDeclarations() {
    return declarations;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "("
        + String.join(
            ", ",
            FluentIterable.from(declarations)
                .transform(t -> t.toASTString(pAAstNodeRepresentation)))
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public int hashCode() {
    return declarations.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibVariableDeclarationTuple other
        && declarations.equals(other.declarations);
  }
}
