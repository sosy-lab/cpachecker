// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.microbenchmarking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.sosy_lab.common.time.Tickers;
import org.sosy_lab.common.time.Tickers.TickerWithUnit;
import org.sosy_lab.cpachecker.core.algorithm.microbenchmarking.MicroBenchmarking.BenchmarkExecutionRun;

public class MBMatrixMultiplication {

  private static class MatrixMultiplicationCell {
    private int[] values;

    public MatrixMultiplicationCell(int size) {
      values = new int[size];
    }

    void consume(int value, int index) {
      values[index] = value;
    }

    int sum() {
      return Arrays.stream(values).reduce(0, (a, b) -> a + b);
    }

  }

  private final int sizeMatrixRowCol;
  private final int numWarmupExecutions;
  private final int numExecutions;

  public MBMatrixMultiplication(
      int sizeMatrixRowCol,
      int numWarmupExecutions,
      int numExecutions) {
    this.sizeMatrixRowCol = sizeMatrixRowCol;
    this.numWarmupExecutions = numWarmupExecutions;
    this.numExecutions = numExecutions;
  }

  List<BenchmarkExecutionRun> runMicrobenchmark() {
    TickerWithUnit ticker = Tickers.getCurrentThreadCputime();

    List<BenchmarkExecutionRun> runTimes = new ArrayList<>();

    for (int exec = 0; exec < (numWarmupExecutions + numExecutions); exec++) {
      int[][] firstMatrix = generateRandomMatrix();
      int[][] secondMatrix = generateRandomMatrix();
      int m = firstMatrix.length;
      int n = firstMatrix[0].length;
      int[][] C = new int[m][n];
      long startTime = ticker.read();

      for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
          MatrixMultiplicationCell cellData = new MatrixMultiplicationCell(secondMatrix.length);
          for (int k = 0; k < secondMatrix.length; k++) {
            int value = firstMatrix[i][k] * secondMatrix[k][j];
            cellData.consume(value, k);
          }
          C[i][j] = cellData.sum();
        }
      }

      long endTime = ticker.read();
      long timeDiff = endTime - startTime;

      if (exec >= numWarmupExecutions) {
        BenchmarkExecutionRun run = new BenchmarkExecutionRun();
        run.duration = timeDiff;
        run.matrixRowSum = sumFirstRow(C);
        runTimes.add(run);
      }

    }

    return runTimes;
  }

  private int[][] generateRandomMatrix() {
    int[][] matrix = new int[sizeMatrixRowCol][sizeMatrixRowCol];
    Random random = new Random();

    for (int i = 0; i < sizeMatrixRowCol; i++) {
      for (int j = 0; j < sizeMatrixRowCol; j++) {
        matrix[i][j] = random.nextInt();
      }
    }

    return matrix;
  }

  private int sumFirstRow(int[][] matrix) {
    if (matrix.length <= 0) {
      return 0;
    }

    return Arrays.stream(matrix[0]).reduce(0, (a, b) -> a + b);
  }

}
