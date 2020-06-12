// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Post-processing for the CFA that merges all loops into a large single loop, introducing a
 * separate variable that tracks the program counter.
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop;
