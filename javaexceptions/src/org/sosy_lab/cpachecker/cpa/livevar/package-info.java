// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * The LiveVariablesCPA is a backwards program analysis, which is aimed to find out which variable
 * is live (read as used afterwards) at which position. As the information is only complete after
 * this CPA has finished computing live variables was directly added into the preprocessing of
 * CPAchecker and can be toggled with the option <b>cfa.findLiveVariables</b> by default no live
 * variables are generated.
 *
 * <p>Up to now, in case of pointer aliasing this analysis is unsound.
 */
package org.sosy_lab.cpachecker.cpa.livevar;
