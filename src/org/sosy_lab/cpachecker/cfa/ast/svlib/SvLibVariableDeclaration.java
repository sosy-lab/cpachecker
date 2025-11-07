// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class SvLibVariableDeclaration extends AVariableDeclaration
    implements SvLibDeclaration {
  @Serial private static final long serialVersionUID = 3038552857008234831L;
  private final boolean isDummyVariable;
  private final boolean isConstant;

  public SvLibVariableDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      boolean pIsConstant,
      SvLibType pType,
      String pName,
      String pOrigName,
      String pQualifiedName) {
    super(
        pFileLocation,
        pIsGlobal,
        pType,
        pName,
        pOrigName,
        pQualifiedName,
        null /* There are no initializers in SV-LIB */);
    isConstant = pIsConstant;
    isDummyVariable = false;
  }

  private SvLibVariableDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      boolean pIsConstant,
      SvLibType pType,
      String pName,
      String pOrigName,
      String pQualifiedName,
      boolean pIsDummyVariable) {
    super(
        pFileLocation,
        pIsGlobal,
        pType,
        pName,
        pOrigName,
        pQualifiedName,
        null /* There are no initializers in SV-LIB */);
    isConstant = pIsConstant;
    isDummyVariable = pIsDummyVariable;
  }

  public static SvLibVariableDeclaration dummyVariableForName(String pName) {
    return new SvLibVariableDeclaration(
        FileLocation.DUMMY,
        false,
        false,
        SvLibCustomType.InternalAnyType,
        pName,
        pName,
        pName,
        true);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public boolean isDummyVariable() {
    return isDummyVariable;
  }

  @Override
  public SvLibType getType() {
    return (SvLibType) super.getType();
  }

  public boolean isConstant() {
    return isConstant;
  }
}
