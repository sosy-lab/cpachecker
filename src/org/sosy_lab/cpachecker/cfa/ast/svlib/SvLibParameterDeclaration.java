// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public final class SvLibParameterDeclaration extends AParameterDeclaration
    implements SvLibSimpleDeclaration {
  @Serial private static final long serialVersionUID = -720428046149807846L;
  private final String procedureName;

  public SvLibParameterDeclaration(
      FileLocation pFileLocation, SvLibType pType, String pName, String pProcedureName) {
    super(pFileLocation, pType, pName);
    procedureName = pProcedureName;
  }

  @Override
  public String getProcedureName() {
    return procedureName;
  }

  @Override
  public SvLibType getType() {
    return (SvLibType) super.getType();
  }

  @Override
  public String getQualifiedName() {
    return procedureName + "::" + getName();
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof SvLibParameterDeclaration other && super.equals(other);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
