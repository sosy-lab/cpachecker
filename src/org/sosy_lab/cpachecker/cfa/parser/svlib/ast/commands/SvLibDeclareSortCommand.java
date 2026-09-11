// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSortDeclaration;

public final class SvLibDeclareSortCommand implements SvLibCommand, SmtLibCommand {

  @Serial private static final long serialVersionUID = -7401678598804609690L;
  private final SvLibSortDeclaration typeDelaration;
  private final FileLocation fileLocation;

  public SvLibDeclareSortCommand(SvLibSortDeclaration pDeclaration, FileLocation pFileLocation) {
    typeDelaration = pDeclaration;
    fileLocation = pFileLocation;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return "(declare-sort "
        + typeDelaration.getType().getType()
        + " "
        + typeDelaration.getType().getArity()
        + ")";
  }

  public SvLibSortDeclaration getTypeDelaration() {
    return typeDelaration;
  }

  @Override
  public int hashCode() {
    return typeDelaration.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibDeclareSortCommand other
        && typeDelaration.equals(other.typeDelaration);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibCommandVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
