// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Offers {@link org.sosy_lab.cpachecker.core.interfaces.MergeOperator} for the {@link
 * org.sosy_lab.cpachecker.cpa.numeric.NumericCPA}.
 *
 * <p>Use {@link
 * org.sosy_lab.cpachecker.cpa.numeric.merge_operator.NumericMergeOperator#getMergeOperator)}. The
 * simplest merge operator is the {@link
 * org.sosy_lab.cpachecker.cpa.numeric.merge_operator.NumericMergeSepOperator}. Two less precise
 * operators are implemented by the {@link
 * org.sosy_lab.cpachecker.cpa.numeric.merge_operator.NumericMergeJoinOperator} and the {@link
 * org.sosy_lab.cpachecker.cpa.numeric.merge_operator.NumericMergeWideningOperator}.
 *
 * <p>The {@link org.sosy_lab.cpachecker.cpa.numeric.merge_operator.NumericSepWideningOperator}
 * could be interesting for the verification of loop programs.
 */
package org.sosy_lab.cpachecker.cpa.numeric.merge_operator;
