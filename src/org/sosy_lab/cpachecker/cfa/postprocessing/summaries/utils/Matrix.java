// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class Matrix<T> {

  private List<List<T>> matrix;
  private Integer amountRows;
  private Integer amountColumns;
  private BiFunction<T, T, T> multiplicationFunction = null;
  private BiFunction<T, T, T> additionFunction = null;

  public Matrix(int pSize) {
    matrix = new ArrayList<>(pSize);
    for (int t = 0; t < pSize; t++) {
      matrix.add(new ArrayList<>(pSize));
    }
    amountRows = pSize;
    amountColumns = pSize;
  }

  public Matrix(Matrix<T> pMatrix) {
    matrix = new ArrayList<>(pMatrix.amountRows);
    for (int t = 0; t < pMatrix.amountRows; t++) {
      matrix.add(new ArrayList<>(pMatrix.amountColumns));
    }

    for (int i = 0; i < amountRows; i++) {
      for (int j = 0; j < pMatrix.amountColumns; j++) {
        matrix.get(i).set(j, pMatrix.get(i, j));
      }
    }

    amountRows = pMatrix.amountRows;
    amountColumns = pMatrix.amountColumns;
  }

  public T get(Integer row, Integer column) {
    return matrix.get(row).get(column);
  }

  public void put(Integer row, Integer column, T pValue) {
    matrix.get(row).set(column, pValue);
  }

  public void multiplyWith(Matrix<T> pMatrix) {
    assert multiplicationFunction != null : "Multiplication function not set";
    assert additionFunction != null : "Addition function not set";
    assert this.amountColumns.longValue() == pMatrix.amountRows.longValue()
        : "Matrix dimensions do not match";

    List<List<T>> resultMatrix = new ArrayList<>(amountRows);
    for (int t = 0; t < amountRows; t++) {
      resultMatrix.add(new ArrayList<>(amountColumns));
    }

    for (int i = 0; i < amountRows; i++) {
      for (int j = 0; j < pMatrix.amountColumns; j++) {
        T cellResult =
            this.multiplicationFunction.apply(this.matrix.get(i).get(0), pMatrix.matrix.get(0).get(j));
        for (int k = 1; k < this.amountColumns; k++) {
          cellResult =
              this.additionFunction.apply(
                  cellResult,
                  this.multiplicationFunction.apply(
                      this.matrix.get(i).get(k), pMatrix.matrix.get(k).get(j)));
        }
        resultMatrix.get(i).set(j, cellResult);
      }
    }
    this.matrix = resultMatrix;
  }

  public void addMatrix(Matrix<T> pMatrix) {
    for (int i = 0; i < amountRows; i++) {
      for (int j = 0; j < pMatrix.amountColumns; j++) {
        this.put(i, j, this.additionFunction.apply(this.get(i, j), pMatrix.get(i, j)));
      }
    }
  }

  public void multiplyWith(T value) {
    for (int i = 0; i < amountRows; i++) {
      for (int j = 0; j < amountColumns; j++) {
        this.matrix
            .get(i)
            .set(j, this.multiplicationFunction.apply(this.matrix.get(i).get(j), value));
      }
    }
  }

  public void setAdditionOperation(BiFunction<T, T, T> pAdditionFunction) {
    this.additionFunction = pAdditionFunction;
  }

  public void setMultiplicationOperation(BiFunction<T, T, T> pMultiplicationFunction) {
    this.additionFunction = pMultiplicationFunction;
  }
}
