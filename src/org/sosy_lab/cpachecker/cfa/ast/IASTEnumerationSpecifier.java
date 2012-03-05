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

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public final class IASTEnumerationSpecifier extends IType {

  private final List<IASTEnumerator> enumerators;
  private final String               name;

  public IASTEnumerationSpecifier(final boolean pConst, final boolean pVolatile,
      final List<IASTEnumerator> pEnumerators, final String pName) {
    super(pConst, pVolatile);
    enumerators = ImmutableList.copyOf(pEnumerators);
    name = pName;
  }

  public IASTEnumerator[] getEnumerators() {
    return enumerators.toArray(new IASTEnumerator[enumerators.size()]);
  }

  public String getName() {
    return name;
  }

  @Override
  public String toASTString(String pDeclarator) {
    StringBuilder lASTString = new StringBuilder();

    if (isConst()) {
      lASTString.append("const ");
    }
    if (isVolatile()) {
      lASTString.append("volatile ");
    }

    lASTString.append("enum ");
    lASTString.append(name);

    lASTString.append(" {\n  ");
    lASTString.append(Joiner.on(",\n  ").join(new ASTStringIterable(enumerators)));
    lASTString.append("\n} ");
    lASTString.append(pDeclarator);

    return lASTString.toString();
  }

  public static final class IASTEnumerator extends IASTSimpleDeclaration {

    private static final IType INT_TYPE = new IASTSimpleDeclSpecifier(true, false, BasicType.INT, false, false, true, false, false, false, false);

    private final Long           value;

    public IASTEnumerator(final IASTFileLocation pFileLocation,
                          final String pName,
        final Long pValue) {
      super(pFileLocation, INT_TYPE, pName);

      checkNotNull(pName);
      value = pValue;
    }

    public long getValue() {
      checkState(value != null, "Need to check hasValue() before calling getValue()");
      return value;
    }

    public boolean hasValue() {
      return value != null;
    }

    @Override
    public String toASTString() {
      return getName()
          + (hasValue() ? " = " + String.valueOf(value) : "");
    }
  }
}
