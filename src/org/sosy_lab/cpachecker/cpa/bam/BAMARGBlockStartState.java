// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class BAMARGBlockStartState extends ARGState {

  private static final long serialVersionUID = -5143941913753150639L;

  private ARGState analyzedBlock = null;

  public BAMARGBlockStartState(AbstractState pWrappedState, ARGState pParentElement) {
    super(pWrappedState, pParentElement);
  }

  public void setAnalyzedBlock(ARGState pRootOfBlock) {
    analyzedBlock = pRootOfBlock;
  }

  public ARGState getAnalyzedBlock() {
    return analyzedBlock;
  }

  @Override
  public String toString() {
    return "BAMARGBlockStartState " + super.toString();
  }
}
