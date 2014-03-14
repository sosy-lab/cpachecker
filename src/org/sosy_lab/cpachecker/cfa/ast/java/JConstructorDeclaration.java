/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.ast.java;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JConstructorType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 *
 * This class represents the Constructor declaration AST node type.
 *
 * ConstructorDeclaration:
 *   [ Javadoc ] { ExtendedModifier }
 *                 [ < TypeParameter { , TypeParameter } > ]
 *       Identifier (
 *                 [ FormalParameter
 *                        { , FormalParameter } ] )
 *       [throws TypeName { , TypeName } ] Block
 *
 *  The constructor declaration is a method declaration represented
 *  in {@link JMethodDeclaration}, who's return type is denoted as
 *  the class type it was declared in. Additionally, not all valid
 *  method modifiers are valid for a constructor, e.g. abstract
 *  static, native, synchronized, final.
 *
 */
public class JConstructorDeclaration extends JMethodDeclaration {

  private static final JConstructorDeclaration UNRESOLVED_CONSTRUCTOR =
      new JConstructorDeclaration(FileLocation.DUMMY,
          JConstructorType.createUnresolvableConstructorType(), "__UNRESOLVABLE__",
          "__UNRESOLVABLE__", new ArrayList<JParameterDeclaration>(), VisibilityModifier.NONE,
          false, JClassType.createUnresolvableType());

  public JConstructorDeclaration(FileLocation pFileLocation,
      JConstructorType pType, String pName, String simpleName,
      List<JParameterDeclaration> pParameterDeclarations,
      VisibilityModifier pVisibility, boolean pIsStrictfp, JClassType declaringClass) {
    super(pFileLocation, pType, pName, simpleName, pParameterDeclarations, pVisibility,
        false, false, false, false, false,
        pIsStrictfp, declaringClass);
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
    if (this == obj) { return true; }

    if (!(obj instanceof JConstructorDeclaration)) { return false; }

    return super.equals(obj);
  }

  public static JConstructorDeclaration createUnresolvedConstructorDeclaration() {
    return UNRESOLVED_CONSTRUCTOR;
  }

  public static JConstructorDeclaration createExternConstructorDeclaration(
      JConstructorType pConstructorType,
      String pName, String simpleName,
      VisibilityModifier pVisibility,
      boolean pStrictFp, JClassType pDeclaringClassType) {

    List<JType> parameterTypes = pConstructorType.getParameters();
    List<JParameterDeclaration> parameters = new ArrayList<>(parameterTypes.size());

    FileLocation externFileLoc = FileLocation.DUMMY;


    int i = 0;

    for (JType parameterType : parameterTypes) {
      final String parameterName = "parameter" + String.valueOf(i);
      parameters.add(
          new JParameterDeclaration(externFileLoc, parameterType, parameterName,
              pName + "::" + parameterName, false));
      i++;
    }

    return new JConstructorDeclaration(externFileLoc, pConstructorType,
        pName, simpleName, parameters, pVisibility, pStrictFp, pDeclaringClassType);
  }
}