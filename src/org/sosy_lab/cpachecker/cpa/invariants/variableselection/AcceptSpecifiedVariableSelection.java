// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.variableselection;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class AcceptSpecifiedVariableSelection<ConstantType> implements VariableSelection<ConstantType> {

  private final CollectVarsVisitor<ConstantType> collectVarsVisitor = new CollectVarsVisitor<>();

  private final VariableSelectionJoiner variableSelectionJoiner = new VariableSelectionJoiner();

  private final ImmutableSet<MemoryLocation> specifiedVariables;

  public AcceptSpecifiedVariableSelection(Iterable<? extends MemoryLocation> pIterable) {
    this.specifiedVariables = ImmutableSet.copyOf(pIterable);
  }

  @Override
  public boolean contains(final MemoryLocation pMemoryLocation) {

    return FluentIterable.from(specifiedVariables)
        .anyMatch(
            new Predicate<MemoryLocation>() {

              @Override
              public boolean apply(MemoryLocation pArg0) {
                if (pMemoryLocation.equals(pArg0)) {
                  return true;
                }
                if (pArg0.getIdentifier().endsWith("[*]")) {
                  int arraySubscriptIndex = pMemoryLocation.getIdentifier().indexOf('[');
                  if (arraySubscriptIndex >= 0) {
                    String containedArray =
                        pArg0.getIdentifier().substring(0, pArg0.getIdentifier().indexOf('['));
                    String array = pMemoryLocation.getIdentifier().substring(arraySubscriptIndex);
                    return containedArray.equals(array);
                  }
                }
                return false;
              }
            });
  }

  @Override
  public VariableSelection<ConstantType> acceptAssumption(BooleanFormula<ConstantType> pAssumption) {
    Set<MemoryLocation> involvedVariables = pAssumption.accept(this.collectVarsVisitor);
    for (MemoryLocation involvedVariable : involvedVariables) {
      if (contains(involvedVariable)) {
        /*
         * Extend the set of specified variables transitively.
         * This might be too precise in general, because after they are reassigned,
         * they might no longer be relevant. Perhaps a second, "temporary", set
         * might help... however, this would require the algorithm using this
         * to not only not record assignments to variables not selected, but also
         * actively delete information about previously selected variables.
         */
        return join(involvedVariables);
      }
    }
    return null;
  }

  @Override
  public VariableSelection<ConstantType> acceptAssignment(MemoryLocation pMemoryLocation, NumeralFormula<ConstantType> pAssumption) {
    if (contains(pMemoryLocation)) {
      /*
       * Extend the set of specified variables transitively.
       * See acceptAssumptions for thoughts about this.
       */
      return join(pAssumption.accept(this.collectVarsVisitor));
    }
    return null;
  }

  @Override
  public String toString() {
    return this.specifiedVariables.toString();
  }

  @Override
  public VariableSelection<ConstantType> join(VariableSelection<ConstantType> pOther) {
    return pOther.acceptVisitor(variableSelectionJoiner);
  }

  @Override
  public <T> T acceptVisitor(VariableSelectionVisitor<ConstantType, T> pVisitor) {
    return pVisitor.visit(this);
  }

  private class VariableSelectionJoiner implements VariableSelectionVisitor<ConstantType, VariableSelection<ConstantType>> {

    @Override
    public VariableSelection<ConstantType> visit(AcceptAllVariableSelection<ConstantType> pAcceptAllVariableSelection) {
      return pAcceptAllVariableSelection;
    }

    @Override
    public VariableSelection<ConstantType> visit(
        AcceptSpecifiedVariableSelection<ConstantType> pAcceptSpecifiedVariableSelection) {
      if (AcceptSpecifiedVariableSelection.this == pAcceptSpecifiedVariableSelection
          || AcceptSpecifiedVariableSelection.this.specifiedVariables.containsAll(pAcceptSpecifiedVariableSelection.specifiedVariables)) {
        return AcceptSpecifiedVariableSelection.this;
      }
      return pAcceptSpecifiedVariableSelection.join(specifiedVariables);
    }

  }

  private VariableSelection<ConstantType> join(Set<MemoryLocation> pSpecifiedVariables) {
    if (this.specifiedVariables == pSpecifiedVariables || this.specifiedVariables.containsAll(pSpecifiedVariables)) {
      return this;
    }
    AcceptSpecifiedVariableSelection<ConstantType> result = new AcceptSpecifiedVariableSelection<>(
        Iterables.concat(AcceptSpecifiedVariableSelection.this.specifiedVariables, pSpecifiedVariables));
    return result;
  }

}
