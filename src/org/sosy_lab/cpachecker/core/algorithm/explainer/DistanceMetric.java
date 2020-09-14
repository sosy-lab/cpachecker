// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.explainer;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

public interface DistanceMetric {

  /**
   * Starts the distance Metric
   *
   * @param safePaths the list with the successful executions
   * @param counterexample a feasible counterexample
   * @return a list of CFAEdge that represent to the closest execution to the counterexample
   */
  List<CFAEdge> startDistanceMetric(List<ARGPath> safePaths, ARGPath counterexample);
}
