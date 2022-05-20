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
 * Edges of this interface are CFA assume edges used in the single loop
 * transformation. They are artificial edges used to encode the control flow
 * through the single loop head into the correct subgraph based on program
 * counter values.
 */
public interface ProgramCounterValueAssumeEdge extends CFAEdge {

  /**
   * Gets the program counter value.
   *
   * @return the program counter value.
   */
  int getProgramCounterValue();

  /**
   * Checks if the assumption is assumed to be true or false on this edge.
   *
   * @return {@code true} is the assumption on this edge is assumed to be
   * true, {@code false} otherwise.
   */
  boolean getTruthAssumption();

}