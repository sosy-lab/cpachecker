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

public final class K3SetLogicCommand implements K3Command, SMTLibCommand {

  @Serial private static final long serialVersionUID = -4993252812017741400L;
  private final String logic;
  private final FileLocation fileLocation;

  public K3SetLogicCommand(String pLogic, FileLocation pFileLocation) {
    logic = pLogic;
    fileLocation = pFileLocation;
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }

  public String getLogic() {
    return logic;
  }

  @Override
  public int hashCode() {
    return logic.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3SetLogicCommand other && logic.equals(other.logic);
  }
}
