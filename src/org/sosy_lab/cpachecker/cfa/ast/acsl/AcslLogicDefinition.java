// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AbstractAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract sealed class AcslLogicDefinition extends AbstractAstNode implements AcslAstNode
    permits AcslLogicFunctionDefinition, AcslLogicPredicateDefinition {

  @Serial private static final long serialVersionUID = 19841239875456789L;

  private final AcslLogicDeclaration declaration;
  private final AcslAstNode body;

  protected AcslLogicDefinition(
      FileLocation pFileLocation, AcslLogicDeclaration pDeclaration, AcslAstNode pBody) {
    super(pFileLocation);
    declaration = checkNotNull(pDeclaration);
    body = checkNotNull(pBody);
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
    result = prime * result + body.hashCode();
    result = prime * result + declaration.hashCode();
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslLogicDefinition other
        && super.equals(other)
        && other.declaration.equals(declaration)
        && other.body.equals(body);
  }
}
