// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/** Standard merge-join operator */
public class MergeJoinOperator implements MergeOperator {

  private final AbstractDomain domain;

  /** Creates a merge-join operator, based on the given join operator */
  public MergeJoinOperator(AbstractDomain d) {
    domain = d;
  }

  @Override
  public AbstractState merge(AbstractState el1, AbstractState el2, Precision p)
      throws CPAException, InterruptedException {
    return domain.join(el1, el2);
  }
}
