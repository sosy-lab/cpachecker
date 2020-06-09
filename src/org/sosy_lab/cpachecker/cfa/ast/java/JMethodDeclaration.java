// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JMethodType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

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

  // TODO refactor to be either abstract or final

  // TODO Type Variables, Exceptions, Annotations

  private static final long serialVersionUID = 2250464052511901845L;
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
      new JMethodDeclaration(
          FileLocation.DUMMY,
          JMethodType.createUnresolvableType(),
          "__Unresolved__",
          "__Unresolved__",
          new ArrayList<>(),
          VisibilityModifier.NONE,
          false,
          false,
          false,
          false,
          false,
          false,
          JClassType.createUnresolvableType());


  public JMethodDeclaration(FileLocation pFileLocation, JMethodType pType, String pName,
      String pSimpleName, List<JParameterDeclaration> pParameterDeclarations,
      VisibilityModifier pVisibility, final boolean pIsFinal,
      final boolean pIsAbstract, final boolean pIsStatic,
      final boolean pIsNative, final boolean pIsSynchronized,
      final boolean pIsStrictfp, JClassOrInterfaceType pDeclaringClass) {
    super(pFileLocation, pType, pName, pName, pParameterDeclarations);

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
        || !isAbstract()
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
  public String toASTString(boolean pQualified) {
    return toASTString();
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

  @Override
  public <R, X extends Exception> R accept(JAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

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
      final String parameterName = "parameter" + i;
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