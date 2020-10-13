// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * CPA that computes the dominators of CFA nodes. A CFA node `d` is a dominator of a CFA node `l` in
 * a CFA if it is part of all paths from the initial location to `l`.
 *
 * <p>This CPA can also be used for post-dominator computation, i.e., to compute all nodes that are
 * part of all paths from a given node to the program exit. To do so, run the CPA with {@link
 * org.sosy_lab.cpachecker.cpa.location.LocationCPABackwards LocationCPABackwards}.
 *
 * <p>Note: If run with {@link org.sosy_lab.cpachecker.cpa.location.LocationCPABackwards
 * LocationCPABackwards}, each node will be post-dominated by itself. This is not a problem and not
 * wrong, just don't be confused.
 */
package org.sosy_lab.cpachecker.cpa.dominator;
