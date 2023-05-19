// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/** Standard merge-sep operator */
public class MergeSepOperator implements MergeOperator {

  @Override
  public AbstractState merge(AbstractState el1, AbstractState el2, Precision p)
      throws CPAException {
    return el2;
  }

  private static final MergeOperator instance = new MergeSepOperator();

  public static MergeOperator getInstance() {
    return instance;
  }
}
