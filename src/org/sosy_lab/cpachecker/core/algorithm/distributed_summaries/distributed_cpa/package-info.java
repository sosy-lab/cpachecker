// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Distributed CPAs extends known CPAs by 4 more operators: Serialize, Deserialize, Proceed, and
 * Combine. Add these operators for an arbitrary CPA and register it in {@link
 * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssFactory} to
 * distribute it.
 */
package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;
