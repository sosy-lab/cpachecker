// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * contains the error invariants algorithm which tries to summarize parts of the program as
 * interpolants. For more detailed inforamtion see: Ermis Evren, Martin Schäf, and Thomas Wies:
 * "Error invariants." International Symposium on Formal Methods. Springer, Berlin, Heidelberg, 2012
 */
package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.error_invariants;
