// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults.precision;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public abstract class RefinablePrecision extends VariableTrackingPrecision {

  /**
   * Whether the given precision tracks a memory location only at the CFA nodes it is mapped to, so
   * that an increment has to name every node a variable is tracked at. Only the precisions of this
   * package differ in this, so they are asked here and not through the interface of every
   * precision.
   */
  public static boolean tracksPerLocation(VariableTrackingPrecision pPrecision) {
    return pPrecision instanceof LocalizedRefinablePrecision;
  }

  /**
   * Returns the C expression denoting the given memory location, or nothing if the declaration of
   * its variable or the element its offset points to cannot be determined.
   */
  protected static Optional<String> asCExpression(
      MemoryLocation pLocation, Supplier<CProgramScope> pScope, MachineModel pMachineModel) {
    if (!pLocation.isReference()) {
      // Without an offset the address of the variable needs no type to be resolved against
      return Optional.of("&" + pLocation.getIdentifier());
    }
    CSimpleDeclaration declaration = pScope.get().lookupVariable(pLocation.getQualifiedName());
    if (declaration == null) {
      return Optional.empty();
    }
    return pLocation.asCExpression(declaration.getType(), pMachineModel);
  }

  /**
   * Returns the scope to look declarations up in, or nothing for a program which is not in C.
   * Building it walks the whole CFA, so it is only built once the first declaration is needed.
   */
  protected static Optional<Supplier<CProgramScope>> scopeOf(CFA pCfa) {
    if (pCfa.getLanguage() != Language.C) {
      return Optional.empty();
    }
    return Optional.of(
        Suppliers.memoize(() -> new CProgramScope(pCfa, LogManager.createNullLogManager())));
  }

  private final VariableTrackingPrecision baseline;

  protected RefinablePrecision(VariableTrackingPrecision pBaseline) {
    baseline = pBaseline;
  }

  @Override
  public final boolean allowsAbstraction() {
    return true;
  }

  @Override
  public boolean isTracking(MemoryLocation pVariable, Type pType, CFANode pLocation) {
    checkNotNull(pVariable);
    checkNotNull(pType);
    checkNotNull(pLocation);
    return baseline.isTracking(pVariable, pType, pLocation);
  }

  protected VariableTrackingPrecision getBaseline() {
    return baseline;
  }

  @Override
  @SuppressWarnings("ForOverride")
  protected final Class<? extends ConfigurableProgramAnalysis> getCPAClass() {
    return baseline.getCPAClass();
  }

  @Override
  public boolean equals(Object pObj) {
    return pObj instanceof RefinablePrecision other && baseline.equals(other.baseline);
  }

  @Override
  public int hashCode() {
    return baseline.hashCode();
  }
}
