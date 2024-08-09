// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package contains classes that represent a violation condition in the distributed CPA. A
 * violation condition is a condition that is checked by the distributed CPA to determine if a
 * violation is present. Every forward analysis that finds states satisfying the states of the
 * violation condition of successors reports violation conditions itself.
 */
package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.violation_condition;
