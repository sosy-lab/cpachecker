// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast;

import java.io.Serial;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibCustomType;

public final class SvLibSortDeclaration implements SvLibParsingDeclaration {

  @Serial private static final long serialVersionUID = -4339353958320549395L;
  private final FileLocation fileLocation;
  private final SvLibCustomType type;
  private final String name;
  private final String origName;

  public SvLibSortDeclaration(
      FileLocation pFileLocation, SvLibCustomType pType, String pName, String pOrigName) {
    fileLocation = pFileLocation;
    type = pType;
    name = pName;
    origName = pOrigName;
  }

  public String getName() {
    return name;
  }

  @Override
  public SvLibCustomType getType() {
    return type;
  }

  @Override
  public @Nullable String getProcedureName() {
    return null;
  }

  @Override
  public SvLibSimpleDeclaration toSimpleDeclaration() {
    throw new UnsupportedOperationException(
        "Cannot convert sort declaration to simple declaration");
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
    return v.accept(this);
  }

  public String getOrigName() {
    return origName;
  }

  @Override
  public int hashCode() {
    return type.hashCode() * 31 + name.hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibSortDeclaration other
        && type.equals(other.type)
        && name.equals(other.name);
  }
}
