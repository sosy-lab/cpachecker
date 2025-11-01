// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3GetWitnessCommand implements K3Command {
  @Serial private static final long serialVersionUID = 2891210418359322531L;
  private final FileLocation fileLocation;

  public K3GetWitnessCommand(FileLocation pFileLocation) {
    fileLocation = pFileLocation;
  }

  @Override
  public int hashCode() {
    return 1527;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof K3GetWitnessCommand;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(get-witness)";
  }

  @Override
  public <R, X extends Exception> R accept(K3CommandVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
