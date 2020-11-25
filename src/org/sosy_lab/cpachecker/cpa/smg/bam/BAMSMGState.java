// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.bam;

import com.google.common.collect.BiMap;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentStack;

public class BAMSMGState implements AbstractState {
  private final SMGState wrappedState;

  public BAMSMGState(SMGState pWrappedState) {
    wrappedState = pWrappedState;
  }

  SMGState getWrappedState(){
    return wrappedState;
  }

  static SMGState prepareForBAM(SMGState pState) throws SMGInconsistentException {
    return pState.prepareForBam();
  }

  static SMGState expandBAM(SMGState reduced, SMGState expanded){
    /*therefore reducing makes a copy we can here commit changes straight to reduced*/
    try{
      return expanded.copyWith(reduced.getHeap().copyOf(),  (BiMap<SMGKnownSymbolicValue, SMGKnownExpValue>) reduced.getExplicitValues());

    } catch (Exception e){
      return expanded;
    }
  }

  @Override
  public boolean equals(@Nullable Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof BAMSMGState)) {
      return false;
    }
    BAMSMGState bamSMGState = (BAMSMGState) pO;
    return Objects.equals(wrappedState, bamSMGState.wrappedState);
  }

  @Override
  public int hashCode() {
    return Objects.hash(wrappedState);
  }

}
