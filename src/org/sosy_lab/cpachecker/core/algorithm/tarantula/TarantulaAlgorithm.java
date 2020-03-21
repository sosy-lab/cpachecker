/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tarantula;

import java.io.PrintStream;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TarantulaAlgorithm implements Algorithm {
  private final Algorithm analysis;
  private final LogManager logger;
  private final int passedPath = 0;
  private final int failedPath = 1;

  public TarantulaAlgorithm(Algorithm analysisAlgorithm, final LogManager pLogger) {
    analysis = analysisAlgorithm;
    this.logger = pLogger;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus result = analysis.run(reachedSet);

    logger.log(Level.INFO, "Run tarantula algorithm");
    if (TarantulaUtils.checkForErrorPath(reachedSet)) {
      if (TarantulaUtils.checkSafePath(reachedSet)) {
        printResult(System.out, reachedSet);
      } else {
        logger.log(Level.WARNING, "There is no Safe path, the algorithm can´t be started");
      }
    } else {
      logger.log(Level.WARNING, "There is no CounterExample, therefore the program is safe");
    }
    logger.log(Level.INFO, "Tarantula algorithm Finished");
    return result;
  }

  public int totalFailed(List<List<Integer>> coveredLines) {
    int oneCounter = 0;
    for (int i = 0; i < coveredLines.size(); i++) {
      if (coveredLines.get(i).get(0) == 1) {
        oneCounter++;
      }
    }

    return oneCounter;
  }

  public int totalPassed(List<List<Integer>> coveredLines) {
    int zeroCounter = 0;
    for (int i = 0; i < coveredLines.size(); i++) {
      if (coveredLines.get(i).get(0) == 0) {
        zeroCounter++;
      }
    }

    return zeroCounter;
  }

  public int failedCase(List<List<Integer>> coveredLines, int lineNumber) {
    int failedCounter = 0;
    for (int i = 1; i < coveredLines.size(); i++) {
      if (coveredLines.get(i).get(0) == 1
          && coveredLines.get(i).get(getIndexOfLineNumber(coveredLines, lineNumber) + 1) == 1) {
        failedCounter++;
      }
    }
    return failedCounter;
  }

  public int passedCase(List<List<Integer>> coveredLines, int lineNumber) {
    int passedCounter = 0;
    for (int i = 1; i < coveredLines.size(); i++) {
      if (coveredLines.get(i).get(0) == 0
          && coveredLines.get(i).get(getIndexOfLineNumber(coveredLines, lineNumber) + 1) == 1) {
        passedCounter++;
      }
    }

    return passedCounter;
  }

  public int getIndexOfLineNumber(List<List<Integer>> coveredLines, int lineNumber) {
    int foundIndex = 0;
    for (int i = 1; i < coveredLines.get(0).size(); i++) {
      if (lineNumber == coveredLines.get(0).get(i)) {
        foundIndex = i;
      }
    }
    return foundIndex;
  }

  public int findLineNumberByIndex(List<List<Integer>> coveredLines, int index) {
    int foundLineNumber = 0;
    for (int i = 0; i < coveredLines.get(0).size(); i++) {
      if (index == i) {
        foundLineNumber = coveredLines.get(0).get(i);
      }
    }
    return foundLineNumber;
  }

  public double makeRanking(List<List<Integer>> coveredLines, int lineNumber) {
    return suspiciousness(
        failedCase(coveredLines, lineNumber),
        totalFailed(coveredLines),
        passedCase(coveredLines, lineNumber),
        totalPassed(coveredLines));
  }

  public double suspiciousness(int failed, int totalFailed, int passed, int totalPassed) {
    int numerator = failed / totalFailed;
    int denominator = (passed / totalPassed) + (failed / totalFailed);
    if (denominator == 0) {
      return 0.0;
    }
    return Math.round((numerator / denominator) * 100.0) / 100.0;
  }

  public void printResult(PrintStream out, ReachedSet reachedSet) {
    List<Integer> passedLines = TarantulaUtils.linesFromSafePath(passedPath, reachedSet);
    List<Integer> failedLines = TarantulaUtils.linesFromErrorPath(failedPath, reachedSet);
    List<List<Integer>> table = TarantulaUtils.getTable(reachedSet, passedLines, failedLines);
    for (int i = 0; i < TarantulaUtils.getProgramLines(reachedSet).size(); i++) {
      out.println(
          findLineNumberByIndex(table, i)
              + "--> "
              + makeRanking(table, findLineNumberByIndex(table, i)));
    }
  }
}
