/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.base.Preconditions.*;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;

/**
 * This class represents variable declarations.
 * Example code:
 *
 * int x = 0;
 * struct s { ... } st;
 */
public final class CVariableDeclaration extends CDeclaration {

  private final CStorageClass    cStorageClass;
  private final CInitializer initializer;

  public CVariableDeclaration(CFileLocation pFileLocation, boolean pIsGlobal,
      CStorageClass pCStorageClass, CType pSpecifier, String pName, String pOrigName,
      CInitializer pInitializer) {

    super(pFileLocation, pIsGlobal, pSpecifier, checkNotNull(pName), pOrigName);
    cStorageClass = pCStorageClass;
    initializer = pInitializer;

    checkArgument(!(cStorageClass == CStorageClass.EXTERN && initializer != null), "Extern declarations cannot have an initializer");
    checkArgument(cStorageClass == CStorageClass.EXTERN || cStorageClass == CStorageClass.AUTO, "CStorageClass is " + cStorageClass);
    checkArgument(pIsGlobal || cStorageClass == CStorageClass.AUTO);
  }

  /**
   * The storage class of this variable (either extern or auto).
   */
  public CStorageClass getCStorageClass() {
    return cStorageClass;
  }

  /**
   * The initial value of the variable
   * (only if present, null otherwise).
   */
  public CInitializer getInitializer() {
    return initializer;
  }

  @Override
  public String toASTString() {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append(cStorageClass.toASTString());
    lASTString.append(getDeclSpecifier().toASTString(getName()));

    if (initializer != null) {
      lASTString.append(" = ");
      lASTString.append(initializer.toASTString());
    }

    lASTString.append(";");

    return lASTString.toString();
  }
}
