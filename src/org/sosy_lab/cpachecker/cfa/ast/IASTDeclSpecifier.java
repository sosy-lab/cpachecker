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

public abstract class IASTDeclSpecifier extends IASTNode {

  private final int storageClass;
  private boolean   isConst;
  private boolean   isInline;
  private boolean   isVolatile;

  public IASTDeclSpecifier(final String pRawSignature,
      final IASTFileLocation pFileLocation, final int pStorageClass,
      final boolean pConst, final boolean pInline, final boolean pVolatile) {
    super(pRawSignature, pFileLocation);
    storageClass = pStorageClass;
    isConst = pConst;
    isInline = pInline;
    isVolatile = pVolatile;
  }

  public int getStorageClass() {
    return storageClass;
  }

  public boolean isConst() {
    return isConst;
  }

  public boolean isInline() {
    return isInline;
  }

  public boolean isVolatile() {
    return isVolatile;
  }

  @Override
  public IASTNode[] getChildren(){
    // there are no children of this class
    return new IASTNode[0];
  }

  public static final int sc_typedef = 1;
  public static final int sc_extern  = 2;
}
