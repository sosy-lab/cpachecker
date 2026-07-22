// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast;

import java.io.Serial;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibAnyType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public final class SvLibParsingVariableDeclaration implements SvLibParsingDeclaration {
  @Serial private static final long serialVersionUID = 3038552857008234831L;
  private final FileLocation fileLocation;
  private final boolean isGlobal;
  private final boolean isConstant;
  private final SvLibType type;
  private final String name;
  private final String origName;
  private final @Nullable String procedureName;

  public SvLibParsingVariableDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      boolean pIsConstant,
      SvLibType pType,
      String pName,
      String pOrigName,
      @Nullable String pProcedureName) {
    fileLocation = pFileLocation;
    isGlobal = pIsGlobal;
    isConstant = pIsConstant;
    type = pType;
    name = pName;
    origName = pOrigName;
    procedureName = pProcedureName;
  }

  public static SvLibParsingVariableDeclaration dummyVariableForName(String pName) {
    return new SvLibParsingVariableDeclaration(
        FileLocation.DUMMY, true, false, new SvLibAnyType(), pName, pName, pName);
  }

  @Override
  public SvLibVariableDeclaration toSimpleDeclaration() {
    return new SvLibVariableDeclaration(
        fileLocation, isGlobal, type, name, origName, getQualifiedName());
  }

  @Override
  public @Nullable String getProcedureName() {
    return procedureName;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return origName;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public SvLibType getType() {
    return type;
  }

  public boolean isConstant() {
    return isConstant;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + (isGlobal ? 1231 : 1237);
    result = prime * result + (isConstant ? 1231 : 1237);
    result = prime * result + type.hashCode();
    result = prime * result + name.hashCode();
    result = prime * result + origName.hashCode();
    result = prime * result + (procedureName == null ? 0 : procedureName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibParsingVariableDeclaration other
        && isGlobal == other.isGlobal
        && isConstant == other.isConstant
        && type.equals(other.type)
        && name.equals(other.name)
        && origName.equals(other.origName)
        && Objects.equals(procedureName, other.procedureName);
  }

  public boolean isGlobal() {
    return isGlobal;
  }

  public String getName() {
    return name;
  }

  public String getQualifiedName() {
    return procedureName != null ? procedureName + "::" + name : name;
  }
}
