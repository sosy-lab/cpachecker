// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.refinement;

import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class RefinementPairStub
    implements ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> {

  @Override
  public void update(
      Class<? extends RefinementInterface> pCallerClass,
      Class<? extends RefinementInterface> pDstClass,
      Object pData) {}

  @Override
  public void start(Class<? extends RefinementInterface> pCallerClass) {}

  @Override
  public void finish(Class<? extends RefinementInterface> pCallerClass)
      throws CPAException, InterruptedException {}

  @Override
  public void printStatistics(StatisticsWriter pOut) {}

  @Override
  public RefinementResult performBlockRefinement(Pair<ExtendedARGPath, ExtendedARGPath> pInput)
      throws CPAException, InterruptedException {

    return RefinementResult.createTrue(pInput.getFirst(), pInput.getSecond());
  }
}
