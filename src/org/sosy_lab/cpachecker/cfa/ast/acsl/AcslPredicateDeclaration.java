// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

// Formally a predicate is not a function, but it can be expressed as a function which returns
// booleans
public final class AcslPredicateDeclaration extends AFunctionDeclaration
    implements AcslDeclaration {

  @Serial
  private static final long serialVersionUID = -814553465151276L;

  private AcslPredicateDeclaration(
      FileLocation pFileLocation,
      AcslPredicateType pType,
      String pName,
      String pOrigName,
      List<? extends AParameterDeclaration> pParameters) {
    super(pFileLocation, pType, pName, pOrigName, pParameters);
  }

  @Override
  public AcslFunctionType getType() {
    return (AcslFunctionType) super.getType();
  }

  @Override
  public <R, X extends Exception> R accept(AcslSimpleDeclarationVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
