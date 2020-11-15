// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.singleSuccessorCompactor;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

class SSCARGState extends ARGState {

  private static final long serialVersionUID = 1L;

  private final ARGState sscState;

  SSCARGState(ARGState pSscState, AbstractState innerState) {
    this(pSscState, innerState, null);
  }

  SSCARGState(ARGState pSscState, AbstractState innerState, ARGState parent) {
    super(innerState, parent);
    sscState = pSscState;
  }

  public ARGState getSSCState() {
    return sscState;
  }

  @Override
  public String toString() {
    return "SSCARGState {{" + super.toString() + "}}";
  }
}
