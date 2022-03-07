// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.assumptions.storage;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class AssumptionStorageDomain implements AbstractDomain {

  public AssumptionStorageDomain() {}

  @Override
  public AbstractState join(AbstractState pElement1, AbstractState pElement2) {
    return ((AssumptionStorageState) pElement1).join((AssumptionStorageState) pElement2);
  }

  @Override
  public boolean isLessOrEqual(AbstractState pElement1, AbstractState pElement2) {
    throw new UnsupportedOperationException();
  }
}
