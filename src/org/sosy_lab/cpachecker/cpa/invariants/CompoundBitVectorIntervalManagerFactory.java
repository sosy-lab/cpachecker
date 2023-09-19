// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;

public final class CompoundBitVectorIntervalManagerFactory
    implements CompoundIntervalManagerFactory {

  private final boolean allowSignedWrapAround;
  private final Collection<OverflowEventHandler> overflowEventHandlers =
      new CopyOnWriteArrayList<>();

  private CompoundBitVectorIntervalManagerFactory(boolean pAllowSignedWrapAround) {
    allowSignedWrapAround = pAllowSignedWrapAround;
  }

  static CompoundBitVectorIntervalManagerFactory allowSignedWrapAround() {
    return new CompoundBitVectorIntervalManagerFactory(true);
  }

  static CompoundBitVectorIntervalManagerFactory forbidSignedWrapAround() {
    return new CompoundBitVectorIntervalManagerFactory(false);
  }

  private void handleAllOverflowHandlers() {
    for (OverflowEventHandler component : overflowEventHandlers) {
      component.signedOverflow();
    }
  }

  @Override
  public CompoundIntervalManager createCompoundIntervalManager(
      MachineModel pMachineModel, Type pType) {
    return createCompoundIntervalManager(BitVectorInfo.from(pMachineModel, pType));
  }

  @Override
  public CompoundIntervalManager createCompoundIntervalManager(TypeInfo pInfo) {
    return createCompoundIntervalManager(pInfo, true);
  }

  public CompoundIntervalManager createCompoundIntervalManager(
      TypeInfo pInfo, boolean pWithOverflowHandlers) {
    if (pInfo instanceof BitVectorInfo) {
      return new CompoundBitVectorIntervalManager(
          (BitVectorInfo) pInfo,
          isSignedWrapAroundAllowed(),
          pWithOverflowHandlers ? this::handleAllOverflowHandlers : () -> {});
    }
    if (pInfo instanceof FloatingPointTypeInfo) {
      return new CompoundFloatingPointIntervalManager((FloatingPointTypeInfo) pInfo);
    }
    throw new AssertionError("Unsupported type: " + pInfo);
  }

  public boolean isSignedWrapAroundAllowed() {
    return allowSignedWrapAround;
  }

  public void addOverflowEventHandler(OverflowEventHandler pOverflowEventHandler) {
    overflowEventHandlers.add(pOverflowEventHandler);
  }

  public void removeOverflowEventHandler(OverflowEventHandler pOverflowEventHandler) {
    overflowEventHandlers.remove(pOverflowEventHandler);
  }
}
