// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;

public final class SvLibGetWitnessCommand implements SvLibCommand {
  @Serial private static final long serialVersionUID = 2891210418359322531L;
  private final FileLocation fileLocation;

  public SvLibGetWitnessCommand(FileLocation pFileLocation) {
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

    return pO instanceof SvLibGetWitnessCommand;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return "(get-witness)";
  }

  @Override
  public <R, X extends Exception> R accept(SvLibCommandVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
