// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

public interface SMGNode {

  /**
   * Returns the nesting level of this value node.
   *
   * @return The current nesting level of this value.
   */
  int getNestingLevel();

  /**
   * Changes the nesting level of this node to the given.
   *
   * @param newLevel the new level
   */
  SMGNode withNestingLevelAndCopy(int newLevel);
}
