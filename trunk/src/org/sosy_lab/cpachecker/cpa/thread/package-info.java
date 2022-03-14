// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This CPA allows to analyze concurrent programs. Each abstract state contains the location and
 * callstack of a one thread. Compatibility of states is checked with special algorithm of labels
 */
package org.sosy_lab.cpachecker.cpa.thread;
