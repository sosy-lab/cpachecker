// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Collection of strategies/heuristics to reduce number of test goals a priori of test-case
 * generation. Strategies should be sound in the sense that one does not loose coverage if applying
 * a strategy.
 */
package org.sosy_lab.cpachecker.cpa.testtargets.reduction;
