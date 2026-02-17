// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults.precision;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/**
 * A {@link org.sosy_lab.cpachecker.core.interfaces.Precision} that tracks all variables per
 * default.
 */
public class AllVariableTrackingPrecision extends ConfigurablePrecision {

  AllVariableTrackingPrecision(
      Configuration config,
      Optional<VariableClassification> pVc,
      Class<? extends ConfigurableProgramAnalysis> pCpaClass)
      throws InvalidConfigurationException {
    super(config, pVc, pCpaClass);
    checkArgument(pVc.isPresent());
  }

  @Override
  public boolean allowsAbstraction() {
    return false;
  }

  @Override
  public boolean isTracking(MemoryLocation pVariable, Type pType, CFANode location) {
    // Check that we really have a variable that is known
    if (pVariable.isReference()) {
      MemoryLocation owner = pVariable.getReferenceStart();
      checkArgument(vc.orElseThrow().getAllVariables().contains(owner.getExtendedQualifiedName()));
    } else {
      checkArgument(
          vc.orElseThrow().getAllVariables().contains(pVariable.getExtendedQualifiedName()));
    }

    return true;
  }

  @Override
  public boolean isEmpty() {
    return vc.orElseThrow().getAllVariables().isEmpty();
  }

  @Override
  public int getSize() {
    return vc.orElseThrow().getAllVariables().size();
  }

  @Override
  public boolean tracksTheSameVariablesAs(VariableTrackingPrecision pOtherPrecision) {
    if (pOtherPrecision.getClass().equals(getClass())) {
      AllVariableTrackingPrecision precisionCompare =
          (AllVariableTrackingPrecision) pOtherPrecision;
      if (vc.orElseThrow()
              .getAllVariables()
              .equals(precisionCompare.vc.orElseThrow().getAllVariables())
          && cpaClass.equals(precisionCompare.cpaClass)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object pObj) {
    return pObj instanceof AllVariableTrackingPrecision other && tracksTheSameVariablesAs(other);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vc.orElseThrow().getAllVariables());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(ConfigurablePrecision.class)
        .add("CPA", cpaClass.getSimpleName())
        .add("tracking all variables:", vc.orElseThrow().getAllVariables())
        .toString();
  }
}
