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
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;

public final class SvLibTraceEntryProcedure extends SvLibTraceComponent {
  @Serial private static final long serialVersionUID = 5543731065650175240L;
  private final SvLibProcedureDeclaration declaration;

  public SvLibTraceEntryProcedure(
      SvLibProcedureDeclaration pDeclaration, FileLocation pFileLocation) {
    super(pFileLocation);
    declaration = pDeclaration;
  }

  public SvLibProcedureDeclaration getDeclaration() {
    return declaration;
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
    return "(entry-proc " + declaration.getName() + ")";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + declaration.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibTraceEntryProcedure other && declaration.equals(other.declaration);
  }
}
