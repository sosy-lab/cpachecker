// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Package for management of custom instructions.
 *
 * <p>It is assumed that these custom instructions will execute parts of the program's statements as
 * special purpose instructions i.e. implemented on special HW like FPGAs.
 *
 * <p>Used to support the extraction of requirements for the custom instructions from the software
 * analysis result as explained in approach #3 of paper
 *
 * <p>M.-C. Jakobs, M. Platzner, T. Wiersema, H. Wehrheim: Integrating Softwaren and Hardware
 * Verification Integrated Formal Methods, LNCS, Springer, 2014
 */
package org.sosy_lab.cpachecker.util.ci;
