// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JConstructorType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class represents the Constructor declaration AST node type.
 *
 * <pre>{@code
 * ConstructorDeclaration:
 *   [ Javadoc ] { ExtendedModifier }
 *                 [ < TypeParameter { , TypeParameter } > ]
 *       Identifier (
 *                 [ FormalParameter
 *                        { , FormalParameter } ] )
 *       [throws TypeName { , TypeName } ] Block
 * }</pre>
 *
 * The constructor declaration is a method declaration represented in {@link JMethodDeclaration},
 * who's return type is denoted as the class type it was declared in. Additionally, not all valid
 * method modifiers are valid for a constructor, e.g. abstract static, native, synchronized, final.
 */
public final class JConstructorDeclaration extends JMethodDeclaration {

  @Serial private static final long serialVersionUID = -581061338706783666L;

  public JConstructorDeclaration(
      FileLocation pFileLocation,
      JConstructorType pType,
      String pName,
      String simpleName,
      List<JParameterDeclaration> pParameterDeclarations,
      VisibilityModifier pVisibility,
      boolean pIsStrictfp,
      JClassOrInterfaceType declaringClass) {
    super(
        pFileLocation,
        pType,
        pName,
        simpleName,
        pParameterDeclarations,
        pVisibility,
        false,
        false,
        false,
        false,
        false,
        pIsStrictfp,
        declaringClass);
  }

  @Override
  public JConstructorType getType() {
    return (JConstructorType) super.getType();
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof JConstructorDeclaration && super.equals(obj);
  }

  public static JConstructorDeclaration createExternConstructorDeclaration(
      JConstructorType pConstructorType,
      String pName,
      String simpleName,
      VisibilityModifier pVisibility,
      boolean pStrictFp,
      JClassType pDeclaringClassType) {

    List<JType> parameterTypes = pConstructorType.getParameters();
    List<JParameterDeclaration> parameters = new ArrayList<>(parameterTypes.size());

    FileLocation externFileLoc = FileLocation.DUMMY;

    int i = 0;

    for (JType parameterType : parameterTypes) {
      final String parameterName = "parameter" + i;
      parameters.add(
          new JParameterDeclaration(
              externFileLoc, parameterType, parameterName, pName + "::" + parameterName, false));
      i++;
    }

    return new JConstructorDeclaration(
        externFileLoc,
        pConstructorType,
        pName,
        simpleName,
        parameters,
        pVisibility,
        pStrictFp,
        pDeclaringClassType);
  }
}
