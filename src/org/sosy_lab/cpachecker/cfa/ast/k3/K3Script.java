// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.util.List;

public class K3Script {

  private final List<K3Command> commands;

  public K3Script(List<K3Command> pCommands) {
    commands = pCommands;
  }

  public List<K3Command> getCommands() {
    return commands;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3Script other && commands.equals(other.commands);
  }

  @Override
  public int hashCode() {
    return commands.hashCode();
  }
}
