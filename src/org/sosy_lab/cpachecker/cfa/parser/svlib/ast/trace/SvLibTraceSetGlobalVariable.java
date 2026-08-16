// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;

public final class SvLibTraceSetGlobalVariable extends SvLibTraceComponent {
  @Serial private static final long serialVersionUID = 5543731065650175240L;
  private final SvLibIdTerm declaration;
  private final SvLibConstantTerm constantTerm;

  public SvLibTraceSetGlobalVariable(
      SvLibIdTerm pDeclaration, SvLibConstantTerm pConstantTerm, FileLocation pFileLocation) {
    super(pFileLocation);
    declaration = pDeclaration;
    constantTerm = pConstantTerm;
  }

  public SvLibIdTerm getSymbol() {
    return declaration;
  }

  public SvLibConstantTerm getConstantTerm() {
    return constantTerm;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  <R, X extends Exception> R accept(SvLibTraceComponentVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public String toASTString() {
    return "(" + declaration.getName() + " " + constantTerm.toASTString() + ")";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + declaration.hashCode();
    result = prime * result + constantTerm.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibTraceSetGlobalVariable other
        && declaration.equals(other.declaration)
        && constantTerm.equals(other.constantTerm);
  }
}
