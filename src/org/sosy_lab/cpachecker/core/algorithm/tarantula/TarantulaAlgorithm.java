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
import java.text.DecimalFormat;
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
   * Calculates suspiciousness of tarantula algorithm.
   *
   * @param failed Is the number of failed cases in each edge.
   * @param passed Is the number of passed cases in each edge.
   * @param pReachedSet Input.
   * @return Calculated suspicious.
   */
  public double suspiciousness(double failed, double passed, ReachedSet pReachedSet) {
    DecimalFormat df = new DecimalFormat("0.00");
    double numerator = failed / totalFailed(pReachedSet);
    double denominator = (passed / totalPassed(pReachedSet)) + (failed / totalFailed(pReachedSet));
    if (denominator == 0.0) {
      return 0.0;
    }
    double result = numerator / denominator;
    return Double.parseDouble(df.format(result));
  }
  /**
   * Calculates how many total failed cases are in ARG.
   *
   * @param pReachedSet Input.
   * @return how many failed cases are found.
   */
  public int totalFailed(ReachedSet pReachedSet) {
    List<List<CFAEdge>> allPaths = TarantulaUtils.getAllPossiblePaths(pReachedSet);

    int counterResult = 0;
    for (int i = 0; i < allPaths.size(); i++) {
      if (TarantulaUtils.isFailedPath(allPaths.get(i), pReachedSet)) {
        counterResult++;
      }
    }
    return counterResult;
  }
  /**
   * Calculates how many total passed cases are in ARG.
   *
   * @param pReachedSet Input.
   * @return how many passed cases are found.
   */
  public int totalPassed(ReachedSet pReachedSet) {
    List<List<CFAEdge>> allPaths = TarantulaUtils.getAllPossiblePaths(pReachedSet);

    return allPaths.size() - totalFailed(pReachedSet);
  }

  /**
   * Just prints result after calculating suspicious and make the ranking for all edges and then
   * store the result into <code>Map</code>.
   */
  public void printResult(PrintStream out, ReachedSet reachedSet) {
    Map<CFAEdge, int[]> table = TarantulaUtils.getTable(reachedSet);
    Map<CFAEdge, Double> resultMap = new LinkedHashMap<>();

    table.forEach(
        (key, value) -> resultMap.put(key, suspiciousness(value[0], value[1], reachedSet)));

    // Sort the result by its value and ignore the suspicious with 0.0 ration.
    final Map<CFAEdge, Double> sortedByCount =
        resultMap.entrySet().stream()
            .filter(e -> e.getValue() != 0)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    sortByValue(sortedByCount).forEach((k, v) -> out.println(k + "--->" + v));
  }

  private static Map<CFAEdge, Double> sortByValue(final Map<CFAEdge, Double> wordCounts) {

    return wordCounts.entrySet().stream()
        .sorted(Map.Entry.<CFAEdge, Double>comparingByValue().reversed())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }
}
