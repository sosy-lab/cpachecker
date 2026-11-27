// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults.precision;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
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
   * Returns the C expressions denoting the given memory locations of the given variables, by the
   * names of the variables. A memory location of another variable is left out, as well as one whose
   * element or member cannot be determined.
   */
  protected static ImmutableSet<String> asCExpressions(
      Iterable<MemoryLocation> pLocations,
      Map<String, AbstractSimpleDeclaration> pVariables,
      MachineModel pMachineModel) {
    ImmutableMap<String, Entry<String, AbstractSimpleDeclaration>> variablesByQualifiedName =
        Maps.uniqueIndex(pVariables.entrySet(), variable -> variable.getValue().getQualifiedName());
    ImmutableSet.Builder<String> expressions = ImmutableSet.builder();
    for (MemoryLocation location : pLocations) {
      Entry<String, AbstractSimpleDeclaration> variable =
          variablesByQualifiedName.get(location.getQualifiedName());
      if (variable != null && variable.getValue().getType() instanceof CType type) {
        location.asCExpression(variable.getKey(), type, pMachineModel).ifPresent(expressions::add);
      }
    }
    return expressions.build();
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
  public boolean isTracking(MemoryLocation pVariable, Type pType, @Nullable CFANode pLocation) {
    checkNotNull(pVariable);
    checkNotNull(pType);
    checkState(pLocation != null || baseline instanceof NonlocalizedVariableTrackingPrecision);
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
