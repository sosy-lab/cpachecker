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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
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
      if (!TarantulaUtils.checkForSafePath(reachedSet)) {

        logger.log(
            Level.WARNING, "There is no safe Path, the algorithm is therefore not efficient");
      }
      logger.log(Level.INFO, "Start tarantula algorithm ... ");

      printResult(System.out, reachedSet);

    } else {
      logger.log(Level.WARNING, "There is no CounterExample, the program is therefore safe");
    }

    logger.log(Level.INFO, "Tarantula algorithm Finished ... ");
    return result;
  }
  /**
   * Calculates how many total failed cases are in ARG.
   *
   * @param coveredEdges The binary converted result.
   * @return how many failed cases are found.
   */
  public int totalFailed(List<List<Integer>> coveredEdges) {
    int oneCounter = 0;

    for (List<Integer> pCoveredEdges : coveredEdges) {
      if (pCoveredEdges.get(0) == 1) {
        oneCounter++;
      }
    }

    return oneCounter;
  }
  /**
   * Calculates how many total passed cases are in ARG.
   *
   * @param coveredEdges The binary converted result.
   * @return how many passed cases are found.
   */
  public int totalPassed(List<List<Integer>> coveredEdges) {
    int zeroCounter = 0;

    for (List<Integer> pCoveredEdges : coveredEdges) {
      if (pCoveredEdges.get(0) == 0) {
        zeroCounter++;
      }
    }

    return zeroCounter;
  }
  /**
   * Calculates how many failed cases are in each edge.
   *
   * @param coveredEdges The binary converted result.
   * @param edgeNumber The edge of its failedCase should be calculated.
   * @return how many failed cases are found.
   */
  public int failedCase(List<List<Integer>> coveredEdges, int edgeNumber) {
    int failedCounter = 0;

    for (List<Integer> pCoveredEdges : coveredEdges) {
      if (pCoveredEdges.get(0) == 1 && pCoveredEdges.get(edgeNumber) == 1) {
        failedCounter++;
      }
    }

    return failedCounter;
  }
  /**
   * Calculates how many passed cases are in each edge.
   *
   * @param coveredEdges The binary converted result.
   * @param edgeNumber The edge of its passedCase should be calculated.
   * @return how many passed cases are found.
   */
  public int passedCase(List<List<Integer>> coveredEdges, int edgeNumber) {
    int passedCounter = 0;

    for (List<Integer> pCoveredEdges : coveredEdges) {
      if (pCoveredEdges.get(0) == 0 && pCoveredEdges.get(edgeNumber) == 1) {
        passedCounter++;
      }
    }

    return passedCounter;
  }
  /**
   * Detects which index has a specific edge.
   *
   * @param programEdges The binary converted result.
   * @param edge The edge of its index number should be detected.
   * @return Founded index number.
   */
  public int getIndexOfEdge(List<CFAEdge> programEdges, CFAEdge edge) {
    int foundIndex = 0;

    for (int i = 0; i < programEdges.size(); i++) {
      if (programEdges.get(i).equals(edge)) {
        foundIndex = i;
      }
    }

    return foundIndex;
  }
  /**
   * Detects which edge has a specific index number.
   *
   * @param programEdges The binary converted result.
   * @param index The index of its edge should be detected.
   * @return Founded CFAEdge number.
   */
  public CFAEdge findCFAEdgeByIndex(List<CFAEdge> programEdges, int index) {
    CFAEdge foundEdgeNumber = null;

    for (int i = 0; i < programEdges.size(); i++) {
      if (index == i) {
        foundEdgeNumber = programEdges.get(i);
      }
    }

    return foundEdgeNumber;
  }
  /**
   * Makes ranking of suspicious possible.
   *
   * @param coveredEdges The binary converted result.
   * @param edgeNumber The suspicious should be calculate for each edgeNumber
   * @return suspiciousness for each edgeNumber.
   */
  public double makeRanking(List<List<Integer>> coveredEdges, int edgeNumber) {

    return suspiciousness(
        failedCase(coveredEdges, edgeNumber),
        totalFailed(coveredEdges),
        passedCase(coveredEdges, edgeNumber),
        totalPassed(coveredEdges));
  }
  /**
   * Calculates suspiciousness of tarantula algorithm.
   *
   * @param failed Is the number of failed cases are in each edge.
   * @param totalFailed Is the total numbers of cases that failed.
   * @param passed Is the number of passed cases are in each edge.
   * @param totalPassed Is the total numbers of cases that passed.
   * @return Calculated suspicious.
   */
  public double suspiciousness(
      double failed, double totalFailed, double passed, double totalPassed) {
    double numerator = failed / totalFailed;

    // if there is no safe path therefore the passed and the totalPassed are always 0
    if (passed == 0 && totalPassed == 0) {
      return 0.0;
    }
    double denominator = (passed / totalPassed) + (failed / totalFailed);
    if (denominator == 0.0) {
      return 0.0;
    }

    return (numerator / denominator);
  }
  /**
   * Just prints result after calculating suspicious and make the ranking for all edges and then
   * store the result into <code>Map</code>.
   */
  public void printResult(PrintStream out, ReachedSet reachedSet) {
    Map<CFAEdge, Double> resultMap = new LinkedHashMap<>();
    List<List<Integer>> table = TarantulaUtils.getTable(reachedSet);
    List<CFAEdge> programEdges = TarantulaUtils.getProgramEdges(reachedSet);
    for (CFAEdge e : programEdges) {
      resultMap.put(
          findCFAEdgeByIndex(programEdges, getIndexOfEdge(programEdges, e)),
          makeRanking(table, getIndexOfEdge(programEdges, e)));
    }

    // Sort the result by its value
    final Map<CFAEdge, Double> sortedByCount = sortByValue(resultMap);
    sortedByCount.forEach((k, v) -> out.println(k + "--->" + v));
  }

  private static Map<CFAEdge, Double> sortByValue(final Map<CFAEdge, Double> wordCounts) {

    return wordCounts.entrySet().stream()
        .sorted((Map.Entry.<CFAEdge, Double>comparingByValue().reversed()))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }
}
