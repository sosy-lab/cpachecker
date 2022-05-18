// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Package for reporting what proportion of code was analyzed by the verifier depending on time. So
 * it is possible to build later a graph (Time-Dependent Coverage Graph, short: TDCG). The classes
 * in this package depend on instances of CoverageCPA which delivers the data.
 */
package org.sosy_lab.cpachecker.util.coverage.tdcg;
