// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Given an error path, do not explore more states that are a given number of assume statements away
 * from it.
 */
package org.sosy_lab.cpachecker.cpa.distance;
