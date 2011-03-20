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

public final class IASTEnumerationSpecifier extends IASTDeclSpecifier {

  private final List<IASTEnumerator> enumerators;
  private final IASTName             name;

  public IASTEnumerationSpecifier(final String pRawSignature,
      final IASTFileLocation pFileLocation,
      final boolean pConst, final boolean pVolatile,
      final List<IASTEnumerator> pEnumerators, final IASTName pName) {
    super(pRawSignature, pFileLocation, pConst, pVolatile);
    enumerators = ImmutableList.copyOf(pEnumerators);
    name = pName;
  }

  public IASTEnumerator[] getEnumerators() {
    return enumerators.toArray(new IASTEnumerator[enumerators.size()]);
  }

  @Override
  public IASTNode[] getChildren() {
    final IASTNode[] children1 = super.getChildren();
    final IASTNode[] children2 = getEnumerators();
    IASTNode[] allChildren=new IASTNode[children1.length + children2.length];
    System.arraycopy(children1, 0, allChildren, 0, children1.length);
    System.arraycopy(children2, 0, allChildren, children1.length, children2.length);
    return allChildren;
  }

  public IASTName getName() {
    return name;
  }

  public static final class IASTEnumerator extends IASTNode {

    private final IASTName       name;
    private final IASTExpression value;

    public IASTEnumerator(final String pRawSignature,
        final IASTFileLocation pFileLocation, final IASTName pName,
        final IASTExpression pValue) {
      super(pRawSignature, pFileLocation);
      name = pName;
      value = pValue;
    }

    public IASTName getName() {
      return name;
    }

    public IASTExpression getValue() {
      return value;
    }

    @Override
    public IASTNode[] getChildren(){
      return new IASTNode[] {value};
    }
  }
}
