// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric.merge_operator;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.numericdomains.DomainFunction;
import org.sosy_lab.numericdomains.Manager;

/** Works exactly like the {@link MergeSepOperator}. */
class NumericMergeSepOperator extends MergeSepOperator implements NumericMergeOperator {
  public NumericMergeSepOperator(Manager pManager) throws InvalidConfigurationException {
    super();
    if (!pManager.implementsFunction(DomainFunction.IS_LEQ)) {
      throw new InvalidConfigurationException("Cannot use mergeSep with chosen domain.");
    }
  }

  @Override
  public boolean usesLoopInformation() {
    return false;
  }
}
