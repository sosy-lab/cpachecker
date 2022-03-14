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
 * Checks if an abstract state or a set of abstract states adheres to the property which should be
 * checked by the specific implementation of PerElementPropertyChecker. Property is always checked
 * individually for every element.
 */
public abstract class PerElementPropertyChecker implements PropertyChecker {

  @Override
  public boolean satisfiesProperty(Collection<AbstractState> pCertificate) {
    for (AbstractState elem : pCertificate) {
      if (!satisfiesProperty(elem)) {
        return false;
      }
    }
    return true;
  }
}
