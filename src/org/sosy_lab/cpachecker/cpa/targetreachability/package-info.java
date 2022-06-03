// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * CPA which only explores the edges which can syntactically lead to a property violation.
 *
 * <p>Needs to be used in conjunction with {@link
 * org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA}.
 */
package org.sosy_lab.cpachecker.cpa.targetreachability;
