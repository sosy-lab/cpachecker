// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure;

/** Enum representing different communication entities in Distributed Summaries Synthesis. */
public enum DssCommunicationEntity {
  /**
   * Block entities are responsible for sharing information about code blocks (preconditions,
   * postconditions).
   */
  BLOCK,
  /** Observers decide whether to terminate the analysis based on received information. */
  OBSERVER,
  /** ALL represents all communication entities. */
  ALL
}
