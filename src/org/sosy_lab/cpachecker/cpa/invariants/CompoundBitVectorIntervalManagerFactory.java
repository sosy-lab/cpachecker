/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.invariants;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;


public enum CompoundBitVectorIntervalManagerFactory implements CompoundIntervalManagerFactory {

  ALLOW_SIGNED_WRAP_AROUND {

    @Override
    public boolean isSignedWrapAroundAllowed() {
      return true;
    }
  },

  FORBID_SIGNED_WRAP_AROUND {

    @Override
    public boolean isSignedWrapAroundAllowed() {
      return false;
    }
  };

  private final Collection<OverflowEventHandler> overflowEventHandlers = new CopyOnWriteArrayList<>();

  private final OverflowEventHandler compositeHandler = new OverflowEventHandler() {

    @Override
    public void signedOverflow() {
      for (OverflowEventHandler component : overflowEventHandlers) {
        component.signedOverflow();
      }
    }
  };

  @Override
  public CompoundIntervalManager createCompoundIntervalManager(MachineModel pMachineModel, Type pType) {
    return createCompoundIntervalManager(BitVectorInfo.from(pMachineModel, pType));
  }

  @Override
  public CompoundIntervalManager createCompoundIntervalManager(TypeInfo pInfo) {
    return createCompoundIntervalManager(pInfo, true);
  }

  public CompoundIntervalManager createCompoundIntervalManager(TypeInfo pInfo, boolean pWithOverflowHandlers) {
    if (pInfo instanceof BitVectorInfo) {
      return new CompoundBitVectorIntervalManager(
          (BitVectorInfo) pInfo, isSignedWrapAroundAllowed(), pWithOverflowHandlers ? compositeHandler : () -> {});
    }
    if (pInfo instanceof FloatingPointTypeInfo) {
      return new CompoundFloatingPointIntervalManager((FloatingPointTypeInfo) pInfo);
    }
    throw new AssertionError("Unsupported type: " + pInfo);
  }

  public abstract boolean isSignedWrapAroundAllowed();

  public void addOverflowEventHandler(OverflowEventHandler pOverflowEventHandler) {
    overflowEventHandlers.add(pOverflowEventHandler);
  }

  public void removeOverflowEventHandler(OverflowEventHandler pOverflowEventHandler) {
    overflowEventHandlers.remove(pOverflowEventHandler);
  }

}
