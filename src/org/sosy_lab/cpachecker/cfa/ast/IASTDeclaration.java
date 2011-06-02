/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
 * storage class, a type, a name and an initializer.
 * 
 * If the storage class is TYPEDEF, then the given name is aliased to the
 * given type (as typedef does in C).
 * Otherwise the name may be null, then it is a struct prototype.
 * In any other case, it is a variable declaration with the given name and type.
 */
public final class IASTDeclaration extends IASTSimpleDeclaration {

  private final boolean               isGlobal;
  private final StorageClass          storageClass;
  private final IASTInitializer       initializer;
  
  public IASTDeclaration(String pRawSignature,
      IASTFileLocation pFileLocation,
      boolean pIsGlobal,
      StorageClass pStorageClass,
      IType pSpecifier, String pName,
      IASTInitializer pInitializer) {
    super(pRawSignature, pFileLocation, pSpecifier, pName);
    isGlobal = pIsGlobal;
    storageClass = checkNotNull(pStorageClass);
    initializer = pInitializer;
    
    checkArgument(!(storageClass == StorageClass.TYPEDEF && getName() == null), "Typedefs require a name");
    checkArgument(!(storageClass == StorageClass.EXTERN && initializer != null), "Extern declarations cannot have an initializer");
  }

  public boolean isGlobal() {
    return isGlobal;
  }
  
  public StorageClass getStorageClass() {
    return storageClass;
  }

  public IASTInitializer getInitializer() {
    return initializer;
  }
}
