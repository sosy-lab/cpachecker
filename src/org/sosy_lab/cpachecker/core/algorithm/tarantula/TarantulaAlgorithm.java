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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TarantulaAlgorithm implements Algorithm {
  private final Algorithm analysis;
  private final LogManager logger;

  public TarantulaAlgorithm(Algorithm analysisAlgorithm, final LogManager pLogger) {
    analysis = analysisAlgorithm;
    this.logger = pLogger;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus result = analysis.run(reachedSet);
    if (TarantulaUtils.checkForErrorPath(reachedSet)) {
      if (TarantulaUtils.checkSafePath(reachedSet)) {
        printResult(System.out, reachedSet);
      } else {
        logger.log(Level.WARNING, "There was no safe path, please check your input program");
      }
    } else {
      logger.log(Level.WARNING, "There is no CounterExample, therefore the program is safe");
    }

    logger.log(Level.INFO, "Tarantula algorithm Finished");
    return result;
  }

  public int totalFailed(List<List<Integer>> coveredLines) {
    int oneCounter = 0;
    for (List<Integer> pCoveredLine : coveredLines) {
      if (pCoveredLine.get(0) == 1) {
        oneCounter++;
      }
    }
    return oneCounter;
  }

  public int totalPassed(List<List<Integer>> coveredLines) {
    int zeroCounter = 0;
    for (List<Integer> pCoveredLine : coveredLines) {
      if (pCoveredLine.get(0) == 0) {
        zeroCounter++;
      }
    }
    return zeroCounter;
  }

  public int failedCase(List<List<Integer>> coveredLines, int lineNumber) {
    int failedCounter = 0;
    for (List<Integer> pCoveredLine : coveredLines) {
      if (pCoveredLine.get(0) == 1 && pCoveredLine.get(lineNumber) == 1) {
        failedCounter++;
      }
    }
    return failedCounter;
  }

  public int passedCase(List<List<Integer>> coveredLines, int lineNumber) {
    int passedCounter = 0;

    for (List<Integer> pCoveredLine : coveredLines) {
      if (pCoveredLine.get(0) == 0 && pCoveredLine.get(lineNumber) == 1) {
        passedCounter++;
      }
    }
    return passedCounter;
  }

  public int getIndexOfEdge(List<CFAEdge> coveredEdges, CFAEdge edge) {
    int foundIndex = 0;
    for (int i = 0; i < coveredEdges.size(); i++) {
      if (coveredEdges.get(i).equals(edge)) {
        foundIndex = i;
      }
    }
    return foundIndex;
  }

  public CFAEdge findCFAEdgeByIndex(List<CFAEdge> coveredEdges, int index) {
    CFAEdge foundLineNumber = null;
    for (int i = 0; i < coveredEdges.size(); i++) {
      if (index == i) {
        foundLineNumber = coveredEdges.get(i);
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
    double numerator = (double) failed / (double) totalFailed;

    double denominator =
        ((double) passed / (double) totalPassed) + ((double) failed / (double) totalFailed);
    if (denominator == 0.0) {
      return 0.0;
    }
    return (numerator / denominator);
  }

  public void printResult(PrintStream out, ReachedSet reachedSet) {

    List<List<Integer>> table = TarantulaUtils.getTable(reachedSet);
    List<CFAEdge> programEdges = TarantulaUtils.getProgramEdges(reachedSet);
    for (CFAEdge e : programEdges) {
      out.println(
          findCFAEdgeByIndex(programEdges, getIndexOfEdge(programEdges, e))
              + "--->"
              + makeRanking(table, getIndexOfEdge(programEdges, e)));
    }
  }
}
