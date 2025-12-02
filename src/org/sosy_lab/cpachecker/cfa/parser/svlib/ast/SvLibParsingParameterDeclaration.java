// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public final class SvLibParsingParameterDeclaration implements SvLibSimpleParsingDeclaration {
  @Serial private static final long serialVersionUID = -720428046149807846L;
  private final FileLocation fileLocation;
  private final SvLibType type;
  private final String name;
  private final String procedureName;

  public SvLibParsingParameterDeclaration(
      FileLocation pFileLocation, SvLibType pType, String pName, String pProcedureName) {
    fileLocation = pFileLocation;
    type = pType;
    name = pName;
    procedureName = pProcedureName;
  }

  @Override
  public SvLibParameterDeclaration toSimpleDeclaration() {
    SvLibParameterDeclaration svLibParameterDeclaration =
        new SvLibParameterDeclaration(fileLocation, type, name);
    svLibParameterDeclaration.setQualifiedName(getQualifiedName());
    return svLibParameterDeclaration;
  }

  public SvLibVariableDeclaration toVariableDeclaration() {
    return new SvLibVariableDeclaration(fileLocation, false, type, name, name, getQualifiedName());
  }

  @Override
  public SvLibType getType() {
    return type;
  }

  @Override
  public String getProcedureName() {
    return procedureName;
  }

  public String getQualifiedName() {
    return procedureName + "::" + name;
  }

  public String getName() {
    return name;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return name;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof SvLibParsingParameterDeclaration other
        && type.equals(other.type)
        && name.equals(other.name)
        && procedureName.equals(other.procedureName);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + type.hashCode();
    result = prime * result + name.hashCode();
    result = prime * result + procedureName.hashCode();
    return result;
  }
}
