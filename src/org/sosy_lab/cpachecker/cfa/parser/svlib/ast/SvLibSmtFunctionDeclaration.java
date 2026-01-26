// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibFunctionType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

/**
 * Corresponds to a mathematical function declaration i.e. as given in SMT-LIB. It has neither
 * parameters, nor a return variable, since this is handled by assumptions in SMT-LIB
 */
public final class SvLibSmtFunctionDeclaration implements SvLibParsingDeclaration {

  @Serial private static final long serialVersionUID = 5745608767872283746L;
  private final FileLocation fileLocation;
  private final String name;
  private final ImmutableList<SvLibType> inputTypes;
  private final SvLibType returnType;

  public SvLibSmtFunctionDeclaration(
      FileLocation pFileLocation,
      String pName,
      List<SvLibType> pInputTypes,
      SvLibType pReturnType) {
    fileLocation = pFileLocation;
    name = pName;
    inputTypes = ImmutableList.copyOf(pInputTypes);
    returnType = pReturnType;
  }

  public String getName() {
    return name;
  }

  @Override
  public @Nullable String getProcedureName() {
    return name;
  }

  @Override
  public SvLibFunctionType getType() {
    return new SvLibFunctionType(inputTypes, returnType);
  }

  @Override
  public SvLibFunctionDeclaration toSimpleDeclaration() {
    return new SvLibFunctionDeclaration(
        fileLocation,
        new SvLibFunctionType(inputTypes, returnType),
        name,
        name,
        // No parameters, since it is a mathematical function
        // TODO: Fix this design decision later
        null);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return getType().toASTString(getName());
  }

  @Override
  public int hashCode() {
    return name.hashCode() * 31 + getType().hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibSmtFunctionDeclaration other
        && name.equals(other.name)
        && getType().equals(other.getType());
  }
}
