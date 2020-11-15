// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions;

import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class GenericAssumptionsDomain extends FlatLatticeDomain {

  public GenericAssumptionsDomain(AbstractState top) {
    super(top);
  }

  @Override
  public AbstractState join(AbstractState el1, AbstractState el2) throws CPAException {
    throw new UnsupportedOperationException();
  }
}
