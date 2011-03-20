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

public final class IASTArrayDeclarator extends IASTDeclarator {

  private final IASTDeclarator          nestedDeclarator;
  private final List<IASTArrayModifier> modifiers;

  public IASTArrayDeclarator(final String pRawSignature,
      final IASTFileLocation pFileLocation,
      final List<IASTPointer> pPointerOperators,
      final IASTDeclarator pNestedDeclarator,
      final List<IASTArrayModifier> pModifiers) {
    super(pRawSignature, pFileLocation, pPointerOperators);
    nestedDeclarator = pNestedDeclarator;
    modifiers = pModifiers;
  }

  public IASTDeclarator getNestedDeclarator() {
    return nestedDeclarator;
  }
  
  public IASTArrayModifier[] getArrayModifiers() {
    return modifiers.toArray(new IASTArrayModifier[modifiers.size()]);
  }

  @Override
  public IASTNode[] getChildren() {
    final IASTNode[] children1 = super.getChildren();
    final IASTNode[] children2 = getArrayModifiers();
    IASTNode[] allChildren=new IASTNode[children1.length + children2.length + 1];
    System.arraycopy(children1, 0, allChildren, 0, children1.length);
    System.arraycopy(children2, 0, allChildren, children1.length, children2.length);
    
    allChildren[allChildren.length - 1] = nestedDeclarator;
    return allChildren;
  }
}
