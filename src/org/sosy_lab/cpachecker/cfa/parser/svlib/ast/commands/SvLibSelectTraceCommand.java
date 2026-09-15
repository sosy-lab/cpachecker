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
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibTrace;

public final class SvLibSelectTraceCommand implements SvLibCommand {
  @Serial private static final long serialVersionUID = -8963792521896393722L;
  private final SvLibTrace trace;
  private final FileLocation fileLocation;

  public SvLibSelectTraceCommand(SvLibTrace pTrace, FileLocation pFileLocation) {
    trace = pTrace;
    fileLocation = pFileLocation;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibCommandVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return "(select-trace "
        + System.lineSeparator()
        + trace.toASTString()
        + System.lineSeparator()
        + ")";
  }

  public SvLibTrace getTrace() {
    return trace;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + trace.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibSelectTraceCommand pOther && trace.equals(pOther.trace);
  }
}
