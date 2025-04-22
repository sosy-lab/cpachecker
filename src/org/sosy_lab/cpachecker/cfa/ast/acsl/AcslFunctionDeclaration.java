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

public final class AcslFunctionDeclaration extends AFunctionDeclaration implements AcslDeclaration {

  @Serial
  private static final long serialVersionUID = -8145502438011234276L;

  private AcslFunctionDeclaration(
      FileLocation pFileLocation,
      AcslFunctionType pType,
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
