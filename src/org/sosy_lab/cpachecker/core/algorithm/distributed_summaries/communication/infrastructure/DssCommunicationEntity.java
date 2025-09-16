// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure;

/**
 * Entities for communication in distributed summaries.
 */
public enum DssCommunicationEntity {
  /* Block worker */
  BLOCK,
  /* Observer worker (thread monitor and result check) */
  OBSERVER,
  /* any entity */
  ALL
}
