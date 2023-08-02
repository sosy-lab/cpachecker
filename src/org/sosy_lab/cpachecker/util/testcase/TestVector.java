// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.testcase;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.common.collect.Collections3.listAndElement;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.harness.PredefinedTypes;

public class TestVector {

  private final PersistentSortedMap<
          ComparableFunctionDeclaration, ImmutableList<ExpressionTestValue>>
      inputFunctionValues;

  private final PersistentSortedMap<ComparableVariableDeclaration, InitializerTestValue>
      inputVariableValues;

  private final ImmutableList<TestValue> inputValues;

  private TestVector() {
    this(PathCopyingPersistentTreeMap.of(), PathCopyingPersistentTreeMap.of(), ImmutableList.of());
  }

  private TestVector(
      PersistentSortedMap<ComparableFunctionDeclaration, ImmutableList<ExpressionTestValue>>
          pInputFunctionValues,
      PersistentSortedMap<ComparableVariableDeclaration, InitializerTestValue> pInputVariableValues,
      ImmutableList<TestValue> pInputsInOrder) {
    inputFunctionValues = pInputFunctionValues;
    inputVariableValues = pInputVariableValues;
    inputValues = pInputsInOrder;
  }

  public TestVector addInputValue(AFunctionDeclaration pFunction, AExpression pValue) {
    return addInputValue(pFunction, ExpressionTestValue.of(pValue));
  }

  private ImmutableList<TestValue> getExtendedValues(final TestValue pValue) {
    return listAndElement(inputValues, pValue);
  }

  public TestVector addInputValue(AFunctionDeclaration pFunction, ExpressionTestValue pValue) {
    ComparableFunctionDeclaration function = new ComparableFunctionDeclaration(pFunction);
    ImmutableList<ExpressionTestValue> currentValues = inputFunctionValues.get(function);
    ImmutableList<ExpressionTestValue> newValues;
    if (currentValues == null) {
      newValues = ImmutableList.of(pValue);
    } else {
      ImmutableList.Builder<ExpressionTestValue> valueListBuilder = ImmutableList.builder();
      valueListBuilder.addAll(currentValues).add(pValue);
      newValues = valueListBuilder.build();
    }
    return new TestVector(
        inputFunctionValues.putAndCopy(function, newValues),
        inputVariableValues,
        getExtendedValues(pValue));
  }

  public List<TestValue> getTestInputsInOrder() {
    checkState(
        inputValues.size()
            == inputVariableValues.size()
                + inputFunctionValues.values().stream()
                    .map(ImmutableList::size)
                    .reduce(0, (x, y) -> x + y));
    return inputValues;
  }

  public TestVector addInputValue(AVariableDeclaration pVariable, AInitializer pValue) {
    return addInputValue(pVariable, InitializerTestValue.of(pValue));
  }

  public TestVector addInputValue(AVariableDeclaration pVariable, InitializerTestValue pValue) {
    ComparableVariableDeclaration variable = new ComparableVariableDeclaration(pVariable);
    InitializerTestValue currentValue = inputVariableValues.get(variable);
    if (currentValue != null) {
      throw new IllegalArgumentException(
          String.format("Variable %s already declared with value %s: ", pVariable, pValue));
    }
    return new TestVector(
        inputFunctionValues,
        inputVariableValues.putAndCopy(variable, pValue),
        getExtendedValues(pValue));
  }

  public Iterable<AFunctionDeclaration> getInputFunctions() {
    return FluentIterable.from(inputFunctionValues.keySet()).transform(f -> f.declaration);
  }

  public List<ExpressionTestValue> getInputValues(AFunctionDeclaration pFunction) {
    ComparableFunctionDeclaration function = new ComparableFunctionDeclaration(pFunction);
    ImmutableList<ExpressionTestValue> currentValues = inputFunctionValues.get(function);
    if (currentValues == null) {
      return ImmutableList.of();
    }
    return currentValues;
  }

  public Iterable<AVariableDeclaration> getInputVariables() {
    return FluentIterable.from(inputVariableValues.keySet()).transform(f -> f.declaration);
  }

  public InitializerTestValue getInputValue(AVariableDeclaration pDeclaration) {
    ComparableVariableDeclaration variable = new ComparableVariableDeclaration(pDeclaration);
    InitializerTestValue currentValue = inputVariableValues.get(variable);
    checkArgument(currentValue != null, "Unknown variable: %s", pDeclaration);
    return currentValue;
  }

  public boolean contains(AFunctionDeclaration pFunctionDeclaration) {
    return inputFunctionValues.containsKey(new ComparableFunctionDeclaration(pFunctionDeclaration));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + inputFunctionValues.hashCode();
    result = prime * result + inputVariableValues.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    return pObj instanceof TestVector other
        && inputFunctionValues.equals(other.inputFunctionValues)
        && inputVariableValues.equals(other.inputVariableValues);
  }

  @Override
  public String toString() {
    return inputFunctionValues.toString() + inputVariableValues;
  }

  public static TestVector newTestVector() {
    return new TestVector();
  }

  private static final Ordering<AParameterDeclaration> PARAMETER_ORDERING =
      Ordering.from(
          (pA, pB) ->
              ComparisonChain.start()
                  .compare(pA.getQualifiedName(), pB.getQualifiedName())
                  .compare(pA.getType(), pB.getType(), Ordering.usingToString())
                  .compare(pA.getFileLocation(), pB.getFileLocation())
                  .result());

  private static class ComparableFunctionDeclaration
      implements Comparable<ComparableFunctionDeclaration> {

    private final AFunctionDeclaration declaration;

    public ComparableFunctionDeclaration(AFunctionDeclaration pDeclaration) {
      declaration = Objects.requireNonNull(pDeclaration);
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
      return pObj instanceof ComparableFunctionDeclaration
          && declaration.equals(((ComparableFunctionDeclaration) pObj).declaration);
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

  private static class ComparableVariableDeclaration
      implements Comparable<ComparableVariableDeclaration> {

    private final AVariableDeclaration declaration;

    public ComparableVariableDeclaration(AVariableDeclaration pDeclaration) {
      declaration = Objects.requireNonNull(pDeclaration);
    }

    @Override
    public int compareTo(ComparableVariableDeclaration pOther) {
      if (declaration.equals(pOther.declaration)) {
        return 0;
      }
      return ComparisonChain.start()
          .compare(declaration.getQualifiedName(), pOther.declaration.getQualifiedName())
          .compare(
              PredefinedTypes.getCanonicalType(declaration.getType()),
              PredefinedTypes.getCanonicalType(pOther.declaration.getType()),
              Ordering.usingToString())
          .compareFalseFirst(declaration.isGlobal(), pOther.declaration.isGlobal())
          .result();
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      return pObj instanceof ComparableVariableDeclaration
          && declaration.equals(((ComparableVariableDeclaration) pObj).declaration);
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

  public static class TargetTestVector {

    private final CFAEdge edgeToTarget;

    private final TestVector testVector;

    public TargetTestVector(CFAEdge pEdgeToTarget, TestVector pTestVector) {
      edgeToTarget = Objects.requireNonNull(pEdgeToTarget);
      testVector = Objects.requireNonNull(pTestVector);
    }

    public TestVector getVector() {
      return testVector;
    }

    public CFAEdge getEdgeToTarget() {
      return edgeToTarget;
    }

    @Override
    public String toString() {
      return testVector.toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(edgeToTarget, testVector);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      return pObj instanceof TargetTestVector other
          && edgeToTarget.equals(other.edgeToTarget)
          && testVector.equals(other.testVector);
    }
  }
}
