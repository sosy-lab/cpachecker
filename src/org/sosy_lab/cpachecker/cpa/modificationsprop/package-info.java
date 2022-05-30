// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * CPA that tracks the flows of two {@link org.sosy_lab.cpachecker.cfa.CFA CFAs} simultaneously and
 * detects similarities to reduce the spaces that could lead to new errors.
 */
package org.sosy_lab.cpachecker.cpa.modificationsprop;
