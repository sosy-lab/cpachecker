/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.harness;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;

public class ComparableFunctionDeclaration
    implements Comparable<ComparableFunctionDeclaration> {

  final AFunctionDeclaration declaration;

  public ComparableFunctionDeclaration(AFunctionDeclaration pDeclaration) {
    this.declaration = Objects.requireNonNull(pDeclaration);
  }

  public AFunctionDeclaration getDeclaration() {
    return declaration;
  }

  public String getName() {
    return declaration.getName();
  }

  @Override
  public int compareTo(ComparableFunctionDeclaration pOther) {
    if (declaration.equals(pOther.declaration)) {
      return 0;
    }
    return ComparisonChain.start()
        .compare(declaration.getQualifiedName(), pOther.declaration.getQualifiedName())
        .compare(
            TestVector.upcast(declaration.getParameters(), AParameterDeclaration.class),
            TestVector.upcast(pOther.declaration.getParameters(), AParameterDeclaration.class),
            TestVector.PARAMETER_ORDERING.lexicographical())
        .compare(
            PredefinedTypes.getCanonicalType(declaration.getType().getReturnType()),
            PredefinedTypes.getCanonicalType(pOther.declaration.getType().getReturnType()),
            Ordering.usingToString())
        .compareFalseFirst(declaration.isGlobal(), pOther.declaration.isGlobal())
        .result();
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof ComparableFunctionDeclaration) {
      return declaration.equals(((ComparableFunctionDeclaration) pObj).declaration);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return declaration.hashCode();
  }

  @Override
  public String toString() {
    return declaration.toString();
  }
}