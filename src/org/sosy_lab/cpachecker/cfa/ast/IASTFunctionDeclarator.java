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

/** This class implements the STANDARD-FunctionDeclarator of eclipse! */
public class IASTFunctionDeclarator extends IASTDeclarator {

  private final IASTDeclarator                 nestedDeclarator;
  private final List<IASTSimpleDeclaration>    parameters;
  private final boolean                        takesVarArgs;

  public IASTFunctionDeclarator(final String pRawSignature,
      final IASTFileLocation pFileLocation,
      final IASTName pName, final IASTDeclarator pNestedDeclarator,
      final List<IASTPointer> pPointerOperators,
      final List<IASTSimpleDeclaration> pParameters,
      final boolean pTakesVarArgs) {
    super(pRawSignature, pFileLocation, pName,
        pPointerOperators);
    nestedDeclarator = pNestedDeclarator;
    parameters = pParameters;
    takesVarArgs = pTakesVarArgs;
    
  }

  public IASTSimpleDeclaration[] getParameters() {
    return parameters.toArray(new IASTSimpleDeclaration[parameters.size()]);
  }

  public IASTDeclarator getNestedDeclarator() {
    return nestedDeclarator;
  }
  
  @Override
  public IASTNode[] getChildren() {
    final IASTNode[] children1 = super.getChildren();
    final IASTNode[] children2 = getParameters();
    IASTNode[] allChildren = new IASTNode[children1.length + children2.length + 1];
    System.arraycopy(children1, 0, allChildren, 0, children1.length);
    System.arraycopy(children2, 0, allChildren, children1.length,
        children2.length);
    
    allChildren[allChildren.length - 1] = nestedDeclarator;
    return allChildren;
  }

  public boolean takesVarArgs() {
    return takesVarArgs;
  }
}
