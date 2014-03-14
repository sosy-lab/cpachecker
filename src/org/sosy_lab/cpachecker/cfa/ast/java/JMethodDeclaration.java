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

import static com.google.common.base.Preconditions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JMethodType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

import com.google.common.base.Strings;

/**
 *
 * This class represents the method declaration AST node type.
 *
 *  MethodDeclaration:
 *   [ Javadoc ] { ExtendedModifier }
 *                 [ < TypeParameter { , TypeParameter } > ]
 *       ( Type | void ) Identifier (
 *       [ FormalParameter
 *                    { , FormalParameter } ] ) {[ ] }
 *       [ throws TypeName { , TypeName } ] ( Block | ; )
 *
 *
 *
 */
public class JMethodDeclaration extends AFunctionDeclaration implements JDeclaration {

 // TODO Type Variables, Exceptions, Annotations

  private final boolean isFinal;
  private final boolean isAbstract;
  private final boolean isStatic;
  private final boolean isNative;
  private final boolean isSynchronized;
  private final boolean isStrictfp;
  private final VisibilityModifier visibility;
  private final JClassOrInterfaceType declaringClass;
  private final String simpleName;

  private static final JMethodDeclaration UNRESOLVED_METHOD =
      new JMethodDeclaration(FileLocation.DUMMY,
          JMethodType.createUnresolvableType(), "__Unresolved__",
          "__Unresolved__",
          new ArrayList<JParameterDeclaration>(),
          VisibilityModifier.NONE, false, false, false, false,
          false, false, JClassType.createUnresolvableType());


  public JMethodDeclaration(FileLocation pFileLocation, JMethodType pType, String pName,
      String pSimpleName, List<JParameterDeclaration> pParameterDeclarations,
      VisibilityModifier pVisibility, final boolean pIsFinal,
      final boolean pIsAbstract, final boolean pIsStatic,
      final boolean pIsNative, final boolean pIsSynchronized,
      final boolean pIsStrictfp, JClassOrInterfaceType pDeclaringClass) {
    super(pFileLocation, pType, pName, pParameterDeclarations);

    visibility = pVisibility;
    isFinal = pIsFinal;
    isAbstract = pIsAbstract;
    isStatic = pIsStatic;
    isNative = pIsNative;
    isSynchronized = pIsSynchronized;
    isStrictfp = pIsStrictfp;
    declaringClass = pDeclaringClass;
    simpleName = pSimpleName;

    checkNotNull(pSimpleName);
    checkNotNull(pVisibility);
    checkArgument((isAbstract() && !isStatic() && !isNative()
        && !isFinal() && !isSynchronized() && !isStrictfp())
        || (!isAbstract())
        , "Abstract Method may only have one Modifier , either public or protected");
  }

  @Override
  public JMethodType getType() {
    return (JMethodType) super.getType();
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<JParameterDeclaration> getParameters() {
    return (List<JParameterDeclaration>) super.getParameters();
  }

  @Override
  public String toASTString() {
    String name = Strings.nullToEmpty(getName());
    StringBuilder modifier = new StringBuilder() ;

    modifier.append(getVisibility().getModifierString() + " ");

    if (isAbstract()) {
      modifier.append("abstract ");
    }
    if (isStatic()) {
      modifier.append("static ");
    }
    if (isFinal()) {
      modifier.append("final ");
    }
    if (isSynchronized()) {
      modifier.append("synchronized ");
    }
    if (isNative()) {
      modifier.append("native ");
    }
    if (isStrictfp()) {
      modifier.append("strictfp ");
    }

    return modifier + getType().toASTString(name) + ";";
  }


  public boolean isFinal() {
    return isFinal;
  }


  public boolean isAbstract() {
    return isAbstract;
  }


  public boolean isStatic() {
    return isStatic;
  }


  public boolean isNative() {
    return isNative;
  }


  public boolean isSynchronized() {
    return isSynchronized;
  }


  public boolean isStrictfp() {
    return isStrictfp;
  }


  public VisibilityModifier getVisibility() {
    return visibility;
  }

  public JClassOrInterfaceType getDeclaringClass() {
    return declaringClass;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(declaringClass);
    result = prime * result + (isAbstract ? 1231 : 1237);
    result = prime * result + (isFinal ? 1231 : 1237);
    result = prime * result + (isNative ? 1231 : 1237);
    result = prime * result + (isStatic ? 1231 : 1237);
    result = prime * result + (isStrictfp ? 1231 : 1237);
    result = prime * result + (isSynchronized ? 1231 : 1237);
    result = prime * result + Objects.hashCode(visibility);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JMethodDeclaration)
        || !super.equals(obj)) {
      return false;
    }

    JMethodDeclaration other = (JMethodDeclaration) obj;

    return Objects.equals(other.declaringClass, declaringClass)
            && other.isAbstract == isAbstract
            && other.isFinal == isFinal
            && other.isNative == isNative
            && other.isStatic == isStatic
            && other.isStrictfp == isStrictfp
            && other.isSynchronized == isSynchronized
            && Objects.equals(other.visibility, visibility);
  }

  public static JMethodDeclaration createUnresolvedMethodDeclaration() {
    return UNRESOLVED_METHOD;
  }

  public static JMethodDeclaration createExternMethodDeclaration(
      JMethodType pMethodType, String pName, String pSimpleName,
      VisibilityModifier pPublic, boolean pFinal,
      boolean pAbstract, boolean pStatic, boolean pNative,
      boolean pSynchronized, boolean pStrictFp,
      JClassOrInterfaceType pDeclaringClassType) {

    List<JType> parameterTypes = pMethodType.getParameters();
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

    return new JMethodDeclaration(externFileLoc, pMethodType,
        pName, pSimpleName, parameters, pPublic, pFinal, pAbstract, pStatic,
        pNative, pSynchronized, pStrictFp, pDeclaringClassType);
  }

  public String getSimpleName() {
    return simpleName;
  }
}