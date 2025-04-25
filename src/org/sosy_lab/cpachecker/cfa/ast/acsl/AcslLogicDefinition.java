// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract sealed class AcslLogicDefinition implements AcslAstNode
    permits AcslLogicFunctionDefinition {

  @Serial private static final long serialVersionUID = 19841239875456789L;

  private final FileLocation fileLocation;
  private final AcslLogicDeclaration declaration;
  private final AcslAstNode body;

  protected AcslLogicDefinition(
      FileLocation pFileLocation, AcslLogicDeclaration pDeclaration, AcslAstNode pBody) {
    fileLocation = pFileLocation;
    declaration = pDeclaration;
    body = pBody;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  public AcslLogicDeclaration getDeclaration() {
    return declaration;
  }

  public AcslAstNode getBody() {
    return body;
  }

  public abstract <R, X extends Exception> R accept(AcslLogicDefinitionVisitor<R, X> v) throws X;

  @Override
  public int hashCode() {
    final int prime = 37;
    int result = 8;
    result = prime * result + Objects.hashCode(body);
    result = prime * result + Objects.hashCode(declaration);
    result = prime * result + Objects.hashCode(fileLocation);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslLogicDefinition other
        && Objects.equals(other.declaration, declaration)
        && Objects.equals(other.body, body)
        && Objects.equals(other.fileLocation, fileLocation);
  }
}
