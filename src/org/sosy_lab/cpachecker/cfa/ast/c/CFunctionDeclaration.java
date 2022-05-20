// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;

/**
 * This class represents forward declarations of functions.
 * Example code:
 *
 * int foo(int x);
 */
public final class CFunctionDeclaration extends AFunctionDeclaration implements CDeclaration {

  public static final CFunctionDeclaration DUMMY =
      new CFunctionDeclaration(
          FileLocation.DUMMY, CFunctionType.NO_ARGS_VOID_FUNCTION, "dummy", ImmutableList.of());

  private static final long serialVersionUID = 5485363555708455537L;

  public CFunctionDeclaration(FileLocation pFileLocation,
      CFunctionType pType, String pName,
      List<CParameterDeclaration> parameters) {
    super(pFileLocation, pType, checkNotNull(pName), pName, parameters);
  }

  public CFunctionDeclaration(
      FileLocation pFileLocation,
      CFunctionType pType,
      String pName,
      String pOrigName,
      List<CParameterDeclaration> parameters) {
    super(pFileLocation, pType, checkNotNull(pName), checkNotNull(pOrigName), parameters);
  }

  @Override
  public CFunctionType getType() {
    return (CFunctionType) super.getType();
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<CParameterDeclaration> getParameters() {
    return (List<CParameterDeclaration>)super.getParameters();
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }

    if (!(obj instanceof CFunctionDeclaration)) {
      return false;
    }

    return super.equals(obj);
  }

  @Override
  public <R, X extends Exception> R accept(CSimpleDeclarationVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

}
