// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This CPA tracks the loop iterations and stops exploration after a given (modifiable) bound is
 * reached, thereby implementing a bound.
 *
 * <p>Applications of this CPA are bounded model checking and k-induction.
 */
package org.sosy_lab.cpachecker.cpa.loopbound;
