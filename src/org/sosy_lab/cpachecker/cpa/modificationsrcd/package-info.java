// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * CPA that tracks which cfa nodes are successors to edges in which potentially changed variables
 * are used in a changed {@link org.sosy_lab.cpachecker.cfa.CFA CFA} and reports assumptions that go
 * towards these edges.
 */
package org.sosy_lab.cpachecker.cpa.modificationsrcd;
