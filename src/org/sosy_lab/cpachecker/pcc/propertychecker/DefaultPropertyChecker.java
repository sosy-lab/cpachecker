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

/**
 * Default implementation of property checker. Accepts every abstract state and every set of
 * abstract states. Does not check any property.
 */
public class DefaultPropertyChecker implements PropertyChecker {

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    return true;
  }

  @Override
  public boolean satisfiesProperty(Collection<AbstractState> pCertificate) {
    return true;
  }
}
