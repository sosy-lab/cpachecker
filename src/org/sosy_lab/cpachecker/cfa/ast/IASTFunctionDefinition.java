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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This is a function definition. It contains a storage class, a type and a name.
 * It is only used for functions that are actually defined in the code, although
 * it stores no information about the function body.
 */
public final class IASTFunctionDefinition extends IASTNode {

  private final StorageClass           storageClass;
  private final IASTSimpleDeclaration  declaration;

  public IASTFunctionDefinition(final String pRawSignature,
      final IASTFileLocation pFileLocation,
      final StorageClass pStorageClass,
      final IASTSimpleDeclaration pDeclaration) {
    super(pRawSignature, pFileLocation);
    storageClass = checkNotNull(pStorageClass);
    declaration = checkNotNull(pDeclaration);
    checkArgument(declaration.getDeclSpecifier() instanceof IASTFunctionTypeSpecifier);
  }
  
  public StorageClass getStorageClass() {
    return storageClass;
  }

  public IASTFunctionTypeSpecifier getDeclSpecifier() {
    return (IASTFunctionTypeSpecifier) declaration.getDeclSpecifier();
  }
  
  public IASTName getName() {
    return declaration.getName();
  }
  
  public IASTSimpleDeclaration getDeclaration() {
    return declaration;
  }
  
  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {getName()};
  }
}
