/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

public final class IASTElaboratedTypeSpecifier extends IType {

  private final int      kind;
  private final String   name;

  public IASTElaboratedTypeSpecifier(boolean pConst, final boolean pVolatile,
      final int pKind, final String pName) {
    super(pConst, pVolatile);
    kind = pKind;
    name = pName;
  }

  public int getKind() {
    return kind;
  }

  public String getName() {
    return name;
  }

  public static final int k_enum = 0;
  public static final int k_struct = 1;
  public static final int k_union = 2;

  @Override
  public String toASTString() {
    if (kind == k_enum) {
      return (isConst() ? "const " : "") + "enum " + name;
    } else if (kind == k_struct) {
      return (isConst() ? "const " : "") + "struct " + name;
    } else if (kind == k_union) {
      return (isConst() ? "const " : "") + "union " + name;
    } else {
      return "IASTElaboratedTypeSpecifier: kind unknown;";
    }
  }
}