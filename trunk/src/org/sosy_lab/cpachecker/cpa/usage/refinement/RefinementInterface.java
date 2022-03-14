// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.refinement;

import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public interface RefinementInterface {

  void update(
      Class<? extends RefinementInterface> callerClass,
      Class<? extends RefinementInterface> dstClass,
      Object data);

  void start(Class<? extends RefinementInterface> callerClass);

  void finish(Class<? extends RefinementInterface> callerClass)
      throws CPAException, InterruptedException;

  void printStatistics(StatisticsWriter pOut);
}
