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

public final class K3SelectTraceCommand implements K3Command {
  @Serial private static final long serialVersionUID = -8963792521896393722L;
  private final K3Trace trace;
  private final FileLocation fileLocation;

  public K3SelectTraceCommand(K3Trace pTrace, FileLocation pFileLocation) {
    trace = pTrace;
    fileLocation = pFileLocation;
  }

  @Override
  public <R, X extends Exception> R accept(K3CommandVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(select-trace " + trace.toASTString(pAAstNodeRepresentation) + ")";
  }

  public K3Trace getTrace() {
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

    return obj instanceof K3SelectTraceCommand pOther && trace.equals(pOther.trace);
  }
}
