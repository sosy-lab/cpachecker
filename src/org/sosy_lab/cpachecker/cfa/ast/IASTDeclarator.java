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

import java.util.List;
import com.google.common.collect.ImmutableList;

public abstract class IASTDeclarator extends IASTNode {

  private final IASTInitializer           initializer;
  private final IASTName                  name;
  private final IASTDeclarator            nestedDeclarator;
  private final List<IASTPointer> pointerOperators;

  public IASTDeclarator(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTInitializer pInitializer,
      final IASTName pName, final IASTDeclarator pNestedDeclarator,
      final List<IASTPointer> pPointerOperators) {
    super(pRawSignature, pFileLocation);
    initializer = pInitializer;
    name = pName;
    nestedDeclarator = pNestedDeclarator;
    pointerOperators = ImmutableList.copyOf(pPointerOperators);
  }

  public IASTInitializer getInitializer() {
    return initializer;
  }

  public IASTName getName() {
    return name;
  }

  public IASTDeclarator getNestedDeclarator() {
    return nestedDeclarator;
  }

  public IASTPointer[] getPointerOperators() {
    return pointerOperators.toArray(new IASTPointer[pointerOperators
        .size()]);
  }

  @Override
  public IASTNode[] getChildren(){
    final IASTNode[] children = pointerOperators.toArray(
        new IASTPointer[pointerOperators.size() + 3]);
    children[pointerOperators.size()] = initializer;
    children[pointerOperators.size() + 1] = name;
    children[pointerOperators.size() + 2] = nestedDeclarator;
    return children;
  }
}
