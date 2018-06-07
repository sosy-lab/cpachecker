/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast.js;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;
import org.sosy_lab.cpachecker.cfa.types.js.JSType;

/**
 * This class represents variable declarations. Example code:
 *
 * <p>int x = 0; struct s { ... } st;
 */
public final class JSVariableDeclaration extends AVariableDeclaration implements JSDeclaration {

  private static final long serialVersionUID = 8864367789579668073L;

  public JSVariableDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      String pName,
      String pOrigName,
      String pQualifiedName,
      JSInitializer pInitializer) {

    super(
        pFileLocation,
        pIsGlobal,
        JSAnyType.ANY,
        checkNotNull(pName),
        pOrigName,
        pQualifiedName,
        pInitializer);
  }

  /**
   * Creates a local variable declaration with dummy file location, no initializer and same names.
   *
   * @param pName Used as name, original name and qualified name.
   * @return Local variable declaration with dummy file location, no initializer and same names.
   */
  public static JSVariableDeclaration local(final @Nonnull String pName) {
    return new JSVariableDeclaration(FileLocation.DUMMY, false, pName, pName, pName, null);
  }

  @Override
  public JSType getType() {
    return (JSType) super.getType();
  }

  @Override
  public JSInitializer getInitializer() {
    return (JSInitializer) super.getInitializer();
  }

  /**
   * Add an initializer. This is only possible if there is no initializer already. DO NOT CALL this
   * method after CFA construction!
   *
   * @param pJSInitializer the new initializer
   */
  public void addInitializer(JSInitializer pJSInitializer) {
    super.addInitializer(pJSInitializer);
  }

  @Override
  public String toASTString() {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append("var ");
    lASTString.append(getName());

    if (getInitializer() != null) {
      lASTString.append(" = ");
      lASTString.append(getInitializer().toASTString());
    }

    lASTString.append(";");

    return lASTString.toString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
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

    if (!(obj instanceof JSVariableDeclaration) || !super.equals(obj)) {
      return false;
    }

    return true; // TODO check
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
  public <R, X extends Exception> R accept(JSSimpleDeclarationVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(JSAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }
}
