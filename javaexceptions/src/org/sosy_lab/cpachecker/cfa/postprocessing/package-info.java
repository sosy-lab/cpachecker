// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Post-processings for the CFA that change the CFA structure, executed (optionally) between parsing
 * and returning the finished CFA.
 *
 * <p>Be careful when you want to add something here. If possible, do not change the CFA, but write
 * you analysis such that it handles the unprocessed CFA. If your analysis depends on a specifically
 * post-processed CFA, it may not be possible to combine it with other CPAs.
 */
package org.sosy_lab.cpachecker.cfa.postprocessing;
