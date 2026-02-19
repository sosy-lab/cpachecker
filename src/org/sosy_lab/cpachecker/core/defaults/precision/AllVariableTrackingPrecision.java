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
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/**
 * A {@link org.sosy_lab.cpachecker.core.interfaces.Precision} that tracks all variables per
 * default.
 */
public class AllVariableTrackingPrecision extends VariableTrackingPrecision {

  private final VariableClassification vc;
  private final Class<? extends ConfigurableProgramAnalysis> cpaClass;

  AllVariableTrackingPrecision(
      VariableClassification pVc, Class<? extends ConfigurableProgramAnalysis> pCpaClass) {
    this.cpaClass = pCpaClass;
    vc = pVc;
  }

  @Override
  public boolean allowsAbstraction() {
    return false;
  }

  @Override
  public boolean isTracking(MemoryLocation pVariable, Type pType, CFANode location) {
    // Check that the variable is known in our variable classification
    // Note: we want to ignore possible offsets when transforming MemoryLocation to a name string,
    // as VariableClassification never returns those
    assert vc.getAllVariables().contains(pVariable.getQualifiedName());

    return true;
  }

  @Override
  public boolean isEmpty() {
    return vc.getAllVariables().isEmpty();
  }

  @Override
  protected Class<? extends ConfigurableProgramAnalysis> getCPAClass() {
    return cpaClass;
  }

  @Override
  public int getSize() {
    return vc.getAllVariables().size();
  }

  @Override
  public void serialize(Writer writer) throws IOException {
    writer.write("# all tracking precision used - nothing to show here");
  }

  @Override
  public VariableTrackingPrecision withIncrement(Multimap<CFANode, MemoryLocation> increment) {
    return this;
  }

  @Override
  public VariableTrackingPrecision join(VariableTrackingPrecision otherPrecision) {
    if (otherPrecision instanceof AllVariableTrackingPrecision pAllVariableTrackingPrecision) {
      checkArgument(pAllVariableTrackingPrecision.cpaClass == cpaClass);
      return this;
    } else if (otherPrecision
        instanceof ConfigurableVariableTrackingPrecision pConfigurableVariableTrackingPrecision) {
      checkArgument(pConfigurableVariableTrackingPrecision.getCPAClass() == cpaClass);
      return pConfigurableVariableTrackingPrecision;
    }
    throw new UnsupportedOperationException(
        "Joining AllVariableTrackingPrecision with "
            + otherPrecision.getClass()
            + " is currently not supported");
  }

  @Override
  public boolean tracksTheSameVariablesAs(VariableTrackingPrecision pOtherPrecision) {
    if (pOtherPrecision.getClass().equals(getClass())) {
      AllVariableTrackingPrecision precisionCompare =
          (AllVariableTrackingPrecision) pOtherPrecision;
      if (vc.getAllVariables().equals(precisionCompare.vc.getAllVariables())
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
    return Objects.hash(vc.getAllVariables());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(ConfigurableVariableTrackingPrecision.class)
        .add("CPA", cpaClass.getSimpleName())
        .add("tracking all variables:", vc.getAllVariables())
        .toString();
  }
}
