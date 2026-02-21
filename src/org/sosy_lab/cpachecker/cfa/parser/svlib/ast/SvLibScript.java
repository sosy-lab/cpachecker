// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;

public class SvLibScript implements SvLibParsingAstNode {
  @Serial private static final long serialVersionUID = 6016969061868381148L;
  private final ImmutableList<SvLibCommand> commands;
  private final FileLocation fileLocation;

  public SvLibScript(List<SvLibCommand> pCommands, FileLocation pFileLocation) {
    commands = ImmutableList.copyOf(pCommands);
    fileLocation = pFileLocation;
  }

  public ImmutableList<SvLibCommand> getCommands() {
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

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return Joiner.on("\n").join(commands.stream().map(SvLibCommand::toASTString).toList());
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }
}
