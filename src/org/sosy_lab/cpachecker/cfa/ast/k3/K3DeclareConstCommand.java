// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3DeclareConstCommand implements K3Command, SMTLibCommand {

  private final K3VariableDeclaration variable;
  private final FileLocation fileLocation;

  public K3DeclareConstCommand(K3VariableDeclaration pVariable, FileLocation pFileLocation) {
    variable = pVariable;
    fileLocation = pFileLocation;
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }

  public K3VariableDeclaration getVariable() {
    return variable;
  }

  @Override
  public int hashCode() {
    return variable.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3DeclareConstCommand other && variable.equals(other.variable);
  }
}
