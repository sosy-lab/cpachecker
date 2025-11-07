// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.util.List;

public class SvLibScript {

  private final List<SvLibCommand> commands;

  public SvLibScript(List<SvLibCommand> pCommands) {
    commands = pCommands;
  }

  public List<SvLibCommand> getCommands() {
    return commands;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibScript other && commands.equals(other.commands);
  }

  @Override
  public int hashCode() {
    return commands.hashCode();
  }
}
