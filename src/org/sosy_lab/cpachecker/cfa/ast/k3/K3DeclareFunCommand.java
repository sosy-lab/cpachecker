// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3DeclareFunCommand implements K3Command, SMTLibCommand {

  @Serial private static final long serialVersionUID = -4340667982732887189L;
  private final K3FunctionDeclaration functionDeclaration;
  private final FileLocation fileLocation;

  public K3DeclareFunCommand(
      K3FunctionDeclaration pFunctionDeclaration, FileLocation pFileLocation) {
    functionDeclaration = pFunctionDeclaration;
    fileLocation = pFileLocation;
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(declare-fun "
        + functionDeclaration.getOrigName()
        + " ("
        + Joiner.on(" ")
            .join(
                FluentIterable.from(functionDeclaration.getType().getParameters())
                    .transform(K3Type::toString))
        + ") "
        + functionDeclaration.getType().getReturnType().toString()
        + ")";
  }

  @Override
  public int hashCode() {
    return functionDeclaration.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3DeclareFunCommand other
        && functionDeclaration.equals(other.functionDeclaration);
  }

  public K3FunctionDeclaration getFunctionDeclaration() {
    return functionDeclaration;
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
