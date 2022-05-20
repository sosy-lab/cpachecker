// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Instances of implementing classes are CFA edges representing the assignment of values to the
 * program counter variable.
 */
public interface ProgramCounterValueAssignmentEdge extends CFAEdge {

  /**
   * Gets the assigned program counter value.
   *
   * @return the assigned program counter value.
   */
  int getProgramCounterValue();
}
