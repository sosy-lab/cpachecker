/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

class TestVector {

  private final PersistentSortedMap<ComparableFunctionDeclaration, ImmutableList<TestValue>>
      inputFunctionValues;

  private TestVector() {
    this(
        PathCopyingPersistentTreeMap.<ComparableFunctionDeclaration, ImmutableList<TestValue>>of());
  }

  private TestVector(
      PersistentSortedMap<ComparableFunctionDeclaration, ImmutableList<TestValue>>
          pInputFunctionValues) {
    inputFunctionValues = pInputFunctionValues;
  }

  public TestVector addInputValue(AFunctionDeclaration pFunction, ARightHandSide pValue) {
    return addInputValue(pFunction, TestValue.of(pValue));
  }

  public TestVector addInputValue(AFunctionDeclaration pFunction, TestValue pValue) {
    ComparableFunctionDeclaration function = new ComparableFunctionDeclaration(pFunction);
    ImmutableList<TestValue> currentValues = inputFunctionValues.get(function);
    ImmutableList<TestValue> newValues;
    if (currentValues == null) {
      newValues = ImmutableList.of(pValue);
    } else {
      ImmutableList.Builder<TestValue> valueListBuilder = ImmutableList.builder();
      valueListBuilder.addAll(currentValues).add(pValue);
      newValues = valueListBuilder.build();
    }
    return new TestVector(inputFunctionValues.putAndCopy(function, newValues));
  }

  public Iterable<AFunctionDeclaration> getInputFunctions() {
    return FluentIterable.from(inputFunctionValues.keySet()).transform(f -> f.declaration);
  }

  public List<TestValue> getInputValues(AFunctionDeclaration pFunction) {
    ComparableFunctionDeclaration function = new ComparableFunctionDeclaration(pFunction);
    ImmutableList<TestValue> currentValues = inputFunctionValues.get(function);
    if (currentValues == null) {
      return Collections.emptyList();
    }
    return currentValues;
  }

  @Override
  public int hashCode() {
    return inputFunctionValues.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof TestVector) {
      return inputFunctionValues.equals(((TestVector) pObj).inputFunctionValues);
    }
    return false;
  }

  @Override
  public String toString() {
    return inputFunctionValues.toString();
  }

  public static TestVector newTestVector() {
    return new TestVector();
  }

  private static final Ordering<FileLocation> FILE_LOCATION_ORDERING =
      Ordering.from(
          (pA, pB) -> {
            return ComparisonChain.start()
                .compare(pA.getFileName(), pB.getFileName())
                .compare(pA.getNodeOffset(), pB.getNodeOffset())
                .compare(pA.getNodeLength(), pB.getNodeLength())
                .result();
          });

  private static final Ordering<AParameterDeclaration> PARAMETER_ORDERING =
      Ordering.from(
          (pA, pB) -> {
            return ComparisonChain.start()
                .compare(pA.getQualifiedName(), pB.getQualifiedName())
                .compare(pA.getType(), pB.getType(), Ordering.usingToString())
                .compare(pA.getFileLocation(), pB.getFileLocation(), FILE_LOCATION_ORDERING)
                .result();
          });

  private static class ComparableFunctionDeclaration
      implements Comparable<ComparableFunctionDeclaration> {

    private final AFunctionDeclaration declaration;

    public ComparableFunctionDeclaration(AFunctionDeclaration pDeclaration) {
      this.declaration = Objects.requireNonNull(pDeclaration);
    }

    @Override
    public int compareTo(ComparableFunctionDeclaration pOther) {
      if (declaration.equals(pOther.declaration)) {
        return 0;
      }
      return ComparisonChain.start()
          .compare(declaration.getQualifiedName(), pOther.declaration.getQualifiedName())
          .compare(
              upcast(declaration.getParameters(), AParameterDeclaration.class),
              upcast(pOther.declaration.getParameters(), AParameterDeclaration.class),
              PARAMETER_ORDERING.lexicographical())
          .compare(
              declaration.getType().getReturnType(),
              pOther.declaration.getType().getReturnType(),
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

  private static <T> Iterable<T> upcast(Iterable<? extends T> pIterable, Class<T> pClass) {
    return FluentIterable.from(pIterable).filter(pClass);
  }
}
