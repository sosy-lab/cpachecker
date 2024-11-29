// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concolic;

import org.sosy_lab.common.log.LogManager;

public class ConcolicAlgorithmIsInitialized {
  static boolean isInitialized = false;
  static AlgorithmType algorithmType;
  static LogManager logger;

  public enum AlgorithmType {
    GENERATIONAL,
    RANDOM_OR_DFS,
  }

  public static void setIsInitialized(AlgorithmType pAlgorithmType, LogManager pLogger) {
    logger = pLogger;
    algorithmType = pAlgorithmType;
    isInitialized = true;
  }

  public static boolean getIsInitialized() {
    return isInitialized;
  }

  public static AlgorithmType getAlgorithmType() {
    return algorithmType;
  }

  public static LogManager getLogger() {
    return logger;
  }
}
