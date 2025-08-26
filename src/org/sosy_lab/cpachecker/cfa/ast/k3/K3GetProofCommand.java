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

public final class K3GetProofCommand implements K3Command {
  @Serial private static final long serialVersionUID = 2891210418359322531L;
  private final FileLocation fileLocation;

  public K3GetProofCommand(FileLocation pFileLocation) {
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

    return pO instanceof K3GetProofCommand;
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }
}
