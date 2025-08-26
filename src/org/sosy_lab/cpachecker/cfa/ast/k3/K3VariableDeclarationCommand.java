// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3VariableDeclarationCommand implements K3Command {

  @Serial private static final long serialVersionUID = -1300405431265108107L;

  private final K3VariableDeclaration variableDeclaration;

  private final FileLocation fileLocation;

  public K3VariableDeclarationCommand(
      K3VariableDeclaration pVariableDeclaration, FileLocation pFileLocation) {
    variableDeclaration = pVariableDeclaration;
    fileLocation = pFileLocation;
  }

  public K3VariableDeclaration getVariableDeclaration() {
    return variableDeclaration;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3VariableDeclarationCommand other
        && variableDeclaration.equals(other.variableDeclaration);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(variableDeclaration);
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }
}
