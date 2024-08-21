// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Package to remove some extracted requirements which are covered by others. Assume that for
 * coverage it is sufficient to look at the part of the requirement which is associated with the
 * variables of the applied custom instructions signature
 */
package org.sosy_lab.cpachecker.util.ci.redundancyremover;
