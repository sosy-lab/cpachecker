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
package org.sosy_lab.cpachecker.cfa.ast;

import static com.google.common.base.Preconditions.*;

/**
 * This class represents declaration of types and variables. It contains a
 * storage class, a type, a name and an optional initializer.
 *
 * If the storage class is TYPEDEF, then the given name is aliased to the
 * given type (as typedef does in C).
 * Otherwise the name may be null, then it is a struct prototype.
 * In any other case, it is a variable declaration with the given name and type.
 */
public abstract class IASTDeclaration extends IASTSimpleDeclaration {

  private final boolean               isGlobal;
  private final StorageClass          storageClass;

  public IASTDeclaration(IASTFileLocation pFileLocation,
                         boolean pIsGlobal,
                         StorageClass pStorageClass,
                         IType pSpecifier, String pName,
                         String pOrigName) {
    super(pFileLocation, pSpecifier, pName, pOrigName);
    isGlobal = pIsGlobal;
    storageClass = checkNotNull(pStorageClass);

    checkArgument(!(storageClass == StorageClass.TYPEDEF && getName() == null), "Typedefs require a name");
  }

  public boolean isGlobal() {
    return isGlobal;
  }

  public StorageClass getStorageClass() {
    return storageClass;
  }

  @Override
  public String toASTString() {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append(storageClass.toASTString());
    lASTString.append(getDeclSpecifier().toASTString());

    if (getName() != null
        && !(getDeclSpecifier() instanceof IASTFunctionTypeSpecifier)
        && !(getDeclSpecifier() instanceof IASTPointerTypeSpecifier
            && ((IASTPointerTypeSpecifier)getDeclSpecifier()).getType() instanceof IASTFunctionTypeSpecifier)) {
      lASTString.append(getName());
    }

    lASTString.append(";");

    return lASTString.toString();
  }
}
