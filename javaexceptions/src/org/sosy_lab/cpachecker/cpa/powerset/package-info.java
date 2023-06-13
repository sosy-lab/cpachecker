// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * CPA that transforms the wrapped CPA into a power set version, i.e., the states explored by the
 * wrapped CPA are stored in a set and are joined on merge. This CPA provides a simple way to
 * transform a CPA whose merge operator does not support join into a CPA with merge-join support.
 */
package org.sosy_lab.cpachecker.cpa.powerset;
