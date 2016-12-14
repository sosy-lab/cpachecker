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

import org.sosy_lab.cpachecker.cfa.ast.AbstractInitializer;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

import java.util.Objects;

/**
 *
 *
 * This class represents the field declaration node type.
 *
 * FieldDeclaration:
 *   [Javadoc] { ExtendedModifier } Type Identifier { [] } [ = Expression ]
 *
 * The simple name contains the Identifier and can be ambiguous in the cfa.
 * The name also contains the type as qualifier and is unique in the cfa.
 *
 *  {@link JInitializerExpression} contains the initializer expression.
 */
public final class JFieldDeclaration extends JVariableDeclaration {

  //TODO Annotation,

  private static final boolean IS_FIELD = true;
  private static final JDeclaration UNRESOLVED_DECLARATION = new JFieldDeclaration(
      FileLocation.DUMMY,
      JSimpleType.getUnspecified(), "_unresolved_", "_unresolved_", false,
      false, false, false, VisibilityModifier.NONE);

  private final VisibilityModifier visibility;
  private final boolean isStatic;
  private final boolean isTransient;
  private final boolean isVolatile;
  private final String simpleName;


  public JFieldDeclaration(FileLocation pFileLocation, JType pType,
      String pName, String pSimpleName,
      boolean pIsFinal, boolean pIsStatic,
      boolean pIsTransient, boolean pIsVolatile,
      VisibilityModifier pVisibility) {
    super(pFileLocation, IS_FIELD, pType, pName, pName, pName, null, pIsFinal);

    isTransient = pIsTransient;
    isVolatile =  pIsVolatile;
    isStatic = pIsStatic;
    visibility = pVisibility;
    simpleName = pSimpleName;
  }

  @Override
  public String toASTString() {
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

    lASTString.append(getType().toASTString(getName()));

    if (getInitializer() != null) {
      lASTString.append(" = ");
      lASTString.append(getInitializer().toASTString());
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

  /**
   * Method for setting a initializer.
   * DO ONLY CALL IT WHILE CREATING THE CFA.
   */
  public void updateInitializer(AbstractInitializer initializer) {
    addInitializer(initializer);
  }



  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
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



  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JFieldDeclaration)
        || !super.equals(obj)) {
      return false;
    }

    JFieldDeclaration other = (JFieldDeclaration) obj;

    return other.isStatic == isStatic
            && other.isTransient == isTransient
            && other.isVolatile == isVolatile
            && Objects.equals(other.visibility, visibility);
  }

  public static JDeclaration createUnresolvedFieldDeclaration() {
    return UNRESOLVED_DECLARATION;
  }

  public static JFieldDeclaration createExternFieldDeclaration(JType pType,
      String pName, String pSimpleName, boolean pIsFinal, boolean pIsStatic,
      boolean pIsTransient, boolean pIsVolatile,
      VisibilityModifier pVisibility) {

    return new JFieldDeclaration(
        FileLocation.DUMMY,
        pType, pName, pSimpleName, pIsFinal, pIsStatic, pIsTransient, pIsVolatile, pVisibility);
  }

  public String getSimpleName() {
    return simpleName;
  }
}
