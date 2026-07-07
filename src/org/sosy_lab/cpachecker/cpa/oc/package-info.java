// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Exploration CPA for the bounded ordering-consistency analysis of concurrent programs: each thread
 * instance is explored separately over all its paths (no feasibility checks, bounded loop
 * unrolling), collecting guarded global-memory events for a subsequent SMT-based consistency check.
 */
package org.sosy_lab.cpachecker.cpa.oc;
