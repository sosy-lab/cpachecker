// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.MatchingGenerator;

public class MatchingGeneratorFactory {

  private MatchingGeneratorFactory() {}

  public enum MatchingGenerators {
    RANDOM,
    HEAVY_EDGE
  }

  public static MatchingGenerator createMatchingGenerator(
      final LogManager pLogger, MatchingGenerators generator) {
    return switch (generator) {
      case HEAVY_EDGE -> new HeavyEdgeMatchingGenerator(pLogger);
      default -> new RandomMatchingGenerator(pLogger);
    };
  }
}
