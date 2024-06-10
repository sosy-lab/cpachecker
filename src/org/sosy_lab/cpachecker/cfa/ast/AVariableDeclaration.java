// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.sosy_lab.cpachecker.cfa.types.Type;

public abstract class AVariableDeclaration extends AbstractDeclaration
    implements Comparable<AVariableDeclaration> {

  private static final long serialVersionUID = -8792173769663524307L;
  private final String qualifiedName;
  private AInitializer initializer;

  protected AVariableDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      Type pType,
      String pName,
      String pOrigName,
      String pQualifiedName,
      AInitializer pInitializer) {
    super(pFileLocation, pIsGlobal, pType, pName, pOrigName);
    qualifiedName = checkNotNull(pQualifiedName);
    initializer = pInitializer;
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  /** The initial value of the variable (only if present, null otherwise). */
  public AInitializer getInitializer() {
    return initializer;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append(
        switch (pAAstNodeRepresentation) {
          case DEFAULT -> getType().toASTString(getName());
          case QUALIFIED -> getType().toASTString(getQualifiedName().replace("::", "__"));
          case ORIGINAL_NAMES -> getType().toASTString(getOrigName());
        });

    if (initializer != null) {
      lASTString.append(" = ");
      lASTString.append(initializer.toASTString(pAAstNodeRepresentation));
    }

    lASTString.append(";");

    return lASTString.toString();
  }

  protected void addInitializer(AInitializer pCInitializer) {
    checkState(getInitializer() == null);
    initializer = pCInitializer;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + qualifiedName.hashCode();
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AVariableDeclaration other
        && super.equals(obj)
        && qualifiedName.equals(other.qualifiedName);
  }

  @Override
  public int compareTo(AVariableDeclaration pOther) {
    return getQualifiedName().compareTo(pOther.getQualifiedName());
  }
}
