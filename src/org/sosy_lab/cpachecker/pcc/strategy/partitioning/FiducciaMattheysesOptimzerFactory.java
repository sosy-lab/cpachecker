// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import org.sosy_lab.cpachecker.core.interfaces.pcc.FiducciaMattheysesOptimizer;

public class FiducciaMattheysesOptimzerFactory {

  private FiducciaMattheysesOptimzerFactory() {}

  public enum OptimizationCriteria {
    EDGECUT,
    NODECUT
  }

  public static FiducciaMattheysesOptimizer createFMOptimizer(OptimizationCriteria criterion) {
    switch (criterion) {
      case EDGECUT:
        return new EdgeCutOptimizer();
      default:
        return new NodeCutOptimizer();
    }
  }
}
