// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.formulaslicing;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public abstract class SlicingState implements AbstractState {

  /** Cast to subclass. Syntax sugar. */
  public SlicingIntermediateState asIntermediate() {
    return (SlicingIntermediateState) this;
  }

  public SlicingAbstractedState asAbstracted() {
    return (SlicingAbstractedState) this;
  }

  public abstract boolean isAbstracted();
}
