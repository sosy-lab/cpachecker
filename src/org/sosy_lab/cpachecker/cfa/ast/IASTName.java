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

public final class IASTName extends IASTNode {

  private final IType type;
  
  public IASTName(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType) {
    super(pRawSignature, pFileLocation);
    type = pType;
  }

  /**
   * Return the type of the thing this name references.
   * Not fully implemented.
   * May return null if the parser did not find a binding.
   */
  public IType getType() {
    return type;
  }
  
  public char[] getSimpleID() {
    // TODO: is this really important? 
    // it is equal to toString() and getRawSignatue()
    return getRawSignature().toCharArray();
  }

  @Override
  public String toString() {
    return getRawSignature();
  }

  @Override
  public IASTNode[] getChildren(){
    // there are no children of this class
    return new IASTNode[0];
  }
}
