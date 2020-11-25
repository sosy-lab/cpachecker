// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import java.util.Objects;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;

public class BamState implements AbstractState {
  private final CLangSMG wrappedState;

  public BamState(CLangSMG pWrappedState) {
    wrappedState = pWrappedState;
  }

  CLangSMG getWrappedState(){
    return wrappedState;
  }

  @Override
  public boolean equals(@Nullable Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || !(pO instanceof BamState)) {
      return false;
    }
    BamState bamState = (BamState) pO;
    return Objects.equals(wrappedState, bamState.wrappedState);
  }

  @Override
  public int hashCode() {
    return Objects.hash(wrappedState);
  }

}
