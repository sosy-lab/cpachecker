// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.util;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class ErrorOrigin {
  private final AbstractState state;

  private final CFANode location;
  
  private final Precision precision;
    
  private ErrorOrigin(final AbstractState pState, final Precision pPrecision) {
    state = pState;
    precision = pPrecision;
    
    location = extractLocation(state);
    assert location != null;
  }

  public static ErrorOrigin create(final AbstractState pState, final Precision pPrecision) {
    return new ErrorOrigin(pState, pPrecision);
  }
  
  public AbstractState getState() {
    return state;
  }
  
  public CFANode getLocation() {
    return location;
  }
  
  public Precision getPrecision() {
    return precision;
  }
}
