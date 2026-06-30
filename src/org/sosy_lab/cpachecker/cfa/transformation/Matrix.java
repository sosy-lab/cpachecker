// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

/**
 * Representation of an integer matrix.
 */
public class Matrix {
  private final int rowNum;
  private final int columnNum;
  private final int[][] matrix;

  @SuppressWarnings("unused")
  public static Matrix createMatrix(int[][] pMatrix) {
    // checks
    return new Matrix(pMatrix);
  }

  public static Matrix createMatrix(String matrixString) {
    return new Matrix(new int[0][0]);
  }

  /**
   * Private Constructor without checks.
   * @param pMatrix the matrix array
   */
  private Matrix(int[][] pMatrix) {
    matrix = pMatrix;
    rowNum = matrix.length;
    columnNum = matrix[0].length;
  }

  public int getRowNum() {
    return rowNum;
  }

  public int getColumnNum() {
    return columnNum;
  }

  public int[][] getMatrix() {
    return matrix;
  }

  /**
   * Get the element a_ij from this matrix.
   * @param pi the row index
   * @param pj the column index
   * @return the element a_ij
   */
  public Integer getElement(int pi, int pj) {
    assert(pi >= 0 && pi < rowNum);
    assert(pj >= 0 && pj < columnNum);
    return matrix[pi][pj];
  }

  public static Matrix matrixAddition(Matrix p1, Matrix p2) {
    assert (p1.getRowNum() == p2.getRowNum());
    assert (p1.getColumnNum() == p2.getColumnNum());
    int[][] newMatrix = new int[p1.getRowNum()][p2.getColumnNum()];
    for (int i = 0; i < p1.getRowNum(); i++) {
      int[] newRow = newMatrix[i];
      for (int j = 0; j < p1.getColumnNum(); j++) {
        newRow[j] = (p1.getElement(i,j) + p2.getElement(i,j));
      }
    }
    return new Matrix(newMatrix);
  }

  public static Matrix matrixMultiplication(Matrix p1, Matrix p2) {
    assert (p1.getColumnNum() == p2.getRowNum());
    int[][] newMatrix = new int[p1.getRowNum()][p2.getColumnNum()];

    for (int p2_column = 0; p2_column < p2.getColumnNum(); p2_column++) {
      for (int p1_row = 0; p1_row < p1.getRowNum(); p1_row++) {
        // sum all things up and add to newMatrix
        for (int sum = 0; sum < p1.getColumnNum(); sum++) {
          newMatrix[p1_row][p2_column] += (p1.getElement(p1_row, sum) * p2.getElement(sum, p2_column));
        }
      }
    }
    return new Matrix(newMatrix);
  }

  public static Matrix transposeMatrix(Matrix p1) {
    int[][] newMatrix = new int[p1.getColumnNum()][p1.getRowNum()];
    for (int i = 0; i < p1.getRowNum(); i++) {
      for (int j = 0; j < p1.getColumnNum(); j++) {
        newMatrix[j][i] = p1.getElement(i, j);
      }
    }
    return new Matrix(newMatrix);
  }

  public static Matrix invertMatrix(Matrix p1) {
    assert(p1.getRowNum() == p1.getColumnNum());
    return p1;
  }

  @Override
  public String toString() {
    StringBuilder returnString = new StringBuilder("{");
    for (int i = 0; i < rowNum; i++) {
      returnString.append("{");
      for (int j = 0; j < columnNum; j++) {
        if (j == columnNum - 1) {
          returnString.append(matrix[i][j]);
        } else {
          returnString.append(matrix[i][j]).append(",");
        }
      }
      returnString.append("}");
      if (i != rowNum - 1) {
        returnString.append(",");
      }
    }
    return returnString + "}";
  }

}
