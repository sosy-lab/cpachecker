// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslAtTerm implements AcslTerm {

  @Serial private static final long serialVersionUID = 814408976675011353L;

  private final AcslTerm term;
  private final AcslLabel label;
  private final FileLocation fileLocation;

  public AcslAtTerm(FileLocation pLocation, AcslTerm pTerm, AcslLabel pLabel) {
    fileLocation = pLocation;
    term = pTerm;
    label = pLabel;
  }

  public AcslLabel getLabel() {
    return label;
  }

  public AcslTerm getTerm() {
    return term;
  }

  @Override
  public <R, X extends Exception> R accept(AcslTermVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public AcslType getExpressionType() {
    return term.getExpressionType();
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "\\at("
        + term.toASTString(pAAstNodeRepresentation)
        + ", "
        + label.toASTString(pAAstNodeRepresentation)
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(" + toASTString(pAAstNodeRepresentation) + ")";
  }

  @Override
  public int hashCode() {
    final int prime = 37;
    int result = 2;
    result = prime * result + Objects.hashCode(fileLocation);
    result = prime * result + Objects.hashCode(term);
    result = prime * result + Objects.hashCode(label);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslAtTerm other
        && Objects.equals(fileLocation, other.fileLocation)
        && Objects.equals(other.term, term)
        && Objects.equals(other.label, label);
  }
}
