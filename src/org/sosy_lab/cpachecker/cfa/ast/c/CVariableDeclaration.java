// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

/**
 * This class represents variable declarations. Example code:
 *
 * <p>int x = 0; struct s { ... } st;
 */
public final class CVariableDeclaration extends AVariableDeclaration implements CDeclaration {

  @Serial private static final long serialVersionUID = 8303959164064236061L;
  private final CStorageClass cStorageClass;

  public CVariableDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      CStorageClass pCStorageClass,
      CType pType,
      String pName,
      String pOrigName,
      String pQualifiedName,
      CInitializer pInitializer) {

    super(
        pFileLocation,
        pIsGlobal,
        pType,
        checkNotNull(pName),
        pOrigName,
        pQualifiedName,
        pInitializer);
    cStorageClass = pCStorageClass;

    checkArgument(
        !(cStorageClass == CStorageClass.EXTERN && getInitializer() != null),
        "Extern declarations cannot have an initializer");
    checkArgument(
        cStorageClass == CStorageClass.EXTERN || cStorageClass == CStorageClass.AUTO,
        "CStorageClass is %s",
        cStorageClass);
    checkArgument(pIsGlobal || cStorageClass == CStorageClass.AUTO);
    checkArgument(
        cStorageClass == CStorageClass.EXTERN || !(pType.getCanonicalType() instanceof CVoidType),
        "Cannot declare variable of type void: %s",
        this);
  }

  @Override
  public CType getType() {
    return (CType) super.getType();
  }

  /** The storage class of this variable (either extern or auto). */
  public CStorageClass getCStorageClass() {
    return cStorageClass;
  }

  @Override
  public CInitializer getInitializer() {
    return (CInitializer) super.getInitializer();
  }

  /**
   * Add an initializer. This is only possible if there is no initializer already. DO NOT CALL this
   * method after CFA construction!
   *
   * @param pCInitializer the new initializer
   */
  public void addInitializer(CInitializer pCInitializer) {
    super.addInitializer(pCInitializer);
  }

  /**
   * Only call this method when there is a {@link CInitializer}.
   *
   * <p>If {@link CVariableDeclaration#toASTString()} yields {@code int x = 42;} then this method
   * yields {@code int x;}.
   */
  public String toASTStringWithoutInitializer() {
    checkArgument(getInitializer() != null, "this instance does not have an initializer");
    return cStorageClass.toASTString() + getType().toASTString(getName()) + ";";
  }

  /**
   * Only call this method when there is a {@link CInitializer}.
   *
   * <p>If {@link CVariableDeclaration#toASTString()} yields {@code int x = 42;} then this method
   * yields {@code x = 42;}.
   */
  public String toASTStringWithoutStorageClass() {
    // it only makes sense to call this method to extract the assignment without the storage class
    checkArgument(getInitializer() != null, "this instance does not have an initializer");
    return getType().toASTString(getName()) + " = " + getInitializer().toASTString() + ";";
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append(cStorageClass.toASTString());
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(cStorageClass);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof CVariableDeclaration other
        && super.equals(obj)
        && Objects.equals(other.cStorageClass, cStorageClass);
  }

  public int hashCodeWithOutStorageClass() {
    final int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  public boolean equalsWithoutStorageClass(Object obj) {
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
