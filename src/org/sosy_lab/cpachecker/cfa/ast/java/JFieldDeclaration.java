// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AbstractInitializer;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class represents the field declaration node type.
 *
 * <pre>
 * FieldDeclaration:
 *   [Javadoc] { ExtendedModifier } Type Identifier { [] } [ = Expression ]
 * </pre>
 *
 * The simple name contains the Identifier and can be ambiguous in the CFA . The name also contains
 * the type as qualifier and is unique in the CFA.
 *
 * <p>{@link JInitializerExpression} contains the initializer expression.
 */
public final class JFieldDeclaration extends JVariableDeclaration {

  // TODO Annotation,

  @Serial private static final long serialVersionUID = -4482849212846810730L;
  private static final boolean IS_FIELD = true;
  private static final JDeclaration UNRESOLVED_DECLARATION =
      new JFieldDeclaration(
          FileLocation.DUMMY,
          JSimpleType.UNSPECIFIED,
          "_unresolved_",
          "_unresolved_",
          false,
          false,
          false,
          false,
          VisibilityModifier.NONE);

  private final VisibilityModifier visibility;
  private final boolean isStatic;
  private final boolean isTransient;
  private final boolean isVolatile;
  private final String simpleName;

  public JFieldDeclaration(
      FileLocation pFileLocation,
      JType pType,
      String pName,
      String pSimpleName,
      boolean pIsFinal,
      boolean pIsStatic,
      boolean pIsTransient,
      boolean pIsVolatile,
      VisibilityModifier pVisibility) {
    super(pFileLocation, IS_FIELD, pType, pName, pName, pName, null, pIsFinal);

    isTransient = pIsTransient;
    isVolatile = pIsVolatile;
    isStatic = pIsStatic;
    visibility = pVisibility;
    simpleName = pSimpleName;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    StringBuilder lASTString = new StringBuilder();

    if (visibility != null) {
      lASTString.append(visibility.getModifierString() + " ");
    }

    if (isFinal()) {
      lASTString.append("final ");
    }

    if (isStatic) {
      lASTString.append("static ");
    }

    lASTString.append(
        switch (pAAstNodeRepresentation) {
          case DEFAULT -> getType().toASTString(getName());
          case QUALIFIED -> getType().toASTString(getQualifiedName().replace("::", "__"));
          case ORIGINAL_NAMES -> getType().toASTString(getOrigName());
        });

    if (getInitializer() != null) {
      lASTString.append(" = ");
      lASTString.append(getInitializer().toASTString(pAAstNodeRepresentation));
    }

    lASTString.append(";");
    return lASTString.toString();
  }

  public boolean isStatic() {
    return isStatic;
  }

  public boolean isTransient() {
    return isTransient;
  }

  public boolean isVolatile() {
    return isVolatile;
  }

  public VisibilityModifier getVisibility() {
    return visibility;
  }

  /** Method for setting an initializer. DO ONLY CALL IT WHILE CREATING THE CFA. */
  public void updateInitializer(AbstractInitializer initializer) {
    addInitializer(initializer);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + (isStatic ? 1231 : 1237);
    result = prime * result + (isTransient ? 1231 : 1237);
    result = prime * result + (isVolatile ? 1231 : 1237);
    result = prime * result + Objects.hashCode(visibility);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof JFieldDeclaration other
        && super.equals(obj)
        && other.isStatic == isStatic
        && other.isTransient == isTransient
        && other.isVolatile == isVolatile
        && Objects.equals(other.visibility, visibility);
  }

  public static JDeclaration createUnresolvedFieldDeclaration() {
    return UNRESOLVED_DECLARATION;
  }

  public static JFieldDeclaration createExternFieldDeclaration(
      JType pType,
      String pName,
      String pSimpleName,
      boolean pIsFinal,
      boolean pIsStatic,
      boolean pIsTransient,
      boolean pIsVolatile,
      VisibilityModifier pVisibility) {

    return new JFieldDeclaration(
        FileLocation.DUMMY,
        pType,
        pName,
        pSimpleName,
        pIsFinal,
        pIsStatic,
        pIsTransient,
        pIsVolatile,
        pVisibility);
  }

  public String getSimpleName() {
    return simpleName;
  }
}
