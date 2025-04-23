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

public final class AcslProgramLabel implements AcslLabel {
  @Serial private static final long serialVersionUID = 70111261956900L;

  private final String label;
  private final FileLocation location;

  public AcslProgramLabel(String pLabel, FileLocation pLocation) {
    label = pLabel;
    location = pLocation;
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return location;
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
    result = prime * result + Objects.hashCode(location);
    result = prime * result + Objects.hashCode(label);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslProgramLabel other
        && Objects.equals(other.label, label)
        && Objects.equals(other.location, location);
  }
}
