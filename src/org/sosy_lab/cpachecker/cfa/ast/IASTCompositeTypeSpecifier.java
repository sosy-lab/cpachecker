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

public final class IASTCompositeTypeSpecifier extends IASTDeclSpecifier {

  private final int                   key;
  private final List<IASTSimpleDeclaration> members;
  private final IASTName              name;

  public IASTCompositeTypeSpecifier(final String pRawSignature,
      final IASTFileLocation pFileLocation, final int pStorageClass,
      final boolean pConst, final boolean pInline, final boolean pVolatile,
      final int pKey, final List<IASTSimpleDeclaration> pMembers, final IASTName pName) {
    super(pRawSignature, pFileLocation, pStorageClass, pConst, pInline,
        pVolatile);
    key = pKey;
    members = ImmutableList.copyOf(pMembers);
    name = pName;
  }

  public int getKey() {
    return key;
  }

  public IASTSimpleDeclaration[] getMembers() {
    return members.toArray(new IASTSimpleDeclaration[members.size()]);
  }

  public IASTName getName() {
    return name;
  }

  @Override
  public IASTNode[] getChildren() {
    final IASTNode[] children =
        members.toArray(new IASTSimpleDeclaration[members.size() + 1]);
    children[members.size()] = name;
    return children;
  }

  public static final int k_struct = 1;
  public static final int k_union  = 2;
}
