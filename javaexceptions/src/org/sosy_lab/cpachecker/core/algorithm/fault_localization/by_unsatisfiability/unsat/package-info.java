// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/** Contains algorithms that minimize possible error locations with UNSAT cores */

/**
 * reduce the number of possible error prone locations to look at Based on Jose, M., & Majumdar, R.
 * (2011). Cause clue clauses: error localization using maximum satisfiability. ACM SIGPLAN Notices,
 * 46(6), 437-446.
 */
package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.unsat;
