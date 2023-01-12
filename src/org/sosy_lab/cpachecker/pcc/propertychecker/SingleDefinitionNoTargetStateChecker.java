// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.propertychecker;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;

public class SingleDefinitionNoTargetStateChecker implements PropertyChecker {

  private SingleDefinitionChecker defChecker;
  private NoTargetStateChecker targetChecker;

  public SingleDefinitionNoTargetStateChecker(final String varWithSingleDef) {
    defChecker = new SingleDefinitionChecker(varWithSingleDef);
    targetChecker = new NoTargetStateChecker();
  }

  @Override
  public boolean satisfiesProperty(final AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean satisfiesProperty(final Collection<AbstractState> pCertificate) {
    return defChecker.satisfiesProperty(pCertificate)
        && targetChecker.satisfiesProperty(pCertificate);
  }
}
