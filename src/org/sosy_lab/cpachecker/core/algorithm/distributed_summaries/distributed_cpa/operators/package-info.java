// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Defines four operators for distributed CPAs. Proceed decides whether to start an analysis for a
 * block. Combine connects the information of all predecessors/successors (associative). Serialize
 * transforms abstract states to messages and adds metadata. Deserialize reverses the serialize
 * operation.
 */
package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators;
