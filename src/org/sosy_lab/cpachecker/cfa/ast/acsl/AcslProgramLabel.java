// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AbstractAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslProgramLabel extends AbstractAstNode implements AcslLabel {
  @Serial private static final long serialVersionUID = 7865806609981277317L;

  private final String label;

  public AcslProgramLabel(String pLabel, FileLocation pLocation) {
    super(pLocation);
    label = checkNotNull(pLabel);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return label;
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return label;
  }

  @Override
  public String getLabel() {
    return label;
  }

  @Override
  public int hashCode() {
    final int prime = 37;
    int result = 2;
    result = prime * result + super.hashCode();
    result = prime * result + label.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslProgramLabel other
        && super.equals(other)
        && other.label.equals(label);
  }
}
