// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector;

public enum BitVectorReachType {
  /** For bit vectors that store access/read/write variable ids for only the next step. */
  DIRECT,
  /** For bit vectors that store access/read/write variable ids for all possible future steps. */
  REACHABLE
}
