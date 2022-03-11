// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.validvars;

import java.io.Serializable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class ValidVarsState
    implements AbstractState, AbstractQueryableState, Graphable, Serializable {

  private static final long serialVersionUID = 9159663474411886276L;
  private final ValidVars validVariables;

  public ValidVarsState(ValidVars pValidVars) {
    validVariables = pValidVars;
  }

  public ValidVars getValidVariables() {
    return validVariables;
  }

  @Override
  public String getCPAName() {
    return "ValidVars";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    return pProperty == null ? false : validVariables.containsVar(pProperty);
  }

  @Override
  public String toDOTLabel() {
    return validVariables.toStringInDOTFormat();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
