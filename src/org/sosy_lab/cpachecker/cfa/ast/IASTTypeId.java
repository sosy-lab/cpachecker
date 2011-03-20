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

public final class IASTTypeId extends IASTNode {

  // TODO need storage class here?
  private final StorageClass      storageClass;
  private final IASTDeclSpecifier specifier;
  private final IASTName          name;

  public IASTTypeId(final String pRawSignature,
      final IASTFileLocation pFileLocation,
      final StorageClass pStorageClass,
      final IASTDeclSpecifier pSpecifier,
      final IASTName pName) {
    super(pRawSignature, pFileLocation);
    storageClass = pStorageClass;
    specifier = pSpecifier;
    name = pName;
  }

  public StorageClass getStorageClass() {
    return storageClass;
  }

  public IASTDeclSpecifier getDeclSpecifier() {
    return specifier;
  }
  
  public IASTName getName() {
    return name;
  }

  @Override
  public IASTNode[] getChildren(){
    return new IASTNode[] {specifier, name};
  }
}
