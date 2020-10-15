// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric.merge_operator;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.numericdomains.DomainFunction;
import org.sosy_lab.numericdomains.Manager;

public interface NumericMergeOperator extends MergeOperator {
  static MergeOperator getMergeOperator(Manager pManager, NumericMergeOperations operation)
      throws InvalidConfigurationException {
    switch (operation) {
      case SEP:
        if (!pManager.implementsFunction(DomainFunction.IS_LEQ)) {
          throw new InvalidConfigurationException("Cannot use mergeSep with chosen domain.");
        }
        return new NumericMergeSepOperator(pManager);
      case JOIN:
        return new NumericMergeJoinOperator(pManager);
      case WIDENING:
        return new NumericMergeWideningOperator(pManager);
      case SEP_WIDENING:
        return new NumericSepWideningOperator(pManager);
      default:
        throw new AssertionError("MergeOperation not handled: " + operation);
    }
  }

  /** Checks whether the merge operator uses loop information for its computation. */
  boolean usesLoopInformation();

  enum NumericMergeOperations {
    /** This is equal to the normal mergeSep operator. */
    SEP,
    /** Computes the join of the two states. */
    JOIN,
    /** Computes the widening of the two states. */
    WIDENING,
    /**
     * Only computes the widening of two states at loop heads, otherwise it acts like the mergeSep
     * operator.
     */
    SEP_WIDENING;
  }
}
