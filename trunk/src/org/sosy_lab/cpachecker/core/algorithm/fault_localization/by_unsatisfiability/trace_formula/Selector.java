// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.error_invariants.AbstractTraceElement;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;

public class Selector extends FaultContribution implements AbstractTraceElement {

  private final String name;
  private BooleanFormula formula;
  private BooleanFormula selectorFormula;
  private final BooleanFormula edgeFormula;
  private final FormulaContext context;
  private final int index;

  public String getName() {
    return name;
  }

  public BooleanFormula getFormula() {
    return formula;
  }

  private Selector(
      int pUniqueIndex,
      BooleanFormula pSelectorFormula,
      BooleanFormula pEdgeFormula,
      CFAEdge pEdge,
      FormulaContext pContext) {
    super(pEdge);
    if (pEdge == null) {
      name = "S" + pUniqueIndex + ": unknown reference";
    } else {
      name = ("S" + pUniqueIndex + ": " + pEdge).replace("\t", " ");
    }
    index = pUniqueIndex;
    formula = pSelectorFormula;
    selectorFormula = pSelectorFormula;
    edgeFormula = pEdgeFormula;
    context = pContext;
  }

  /**
   * The truth value of the corresponding edge is essential. Enabling a selector is equivalent to
   * making the corresponding constraint hard. Note as soon as the selector is part of a formula
   * enable, disable and free will not have any effect.
   */
  public void enable() {
    formula = context.getSolver().getFormulaManager().getBooleanFormulaManager().makeTrue();
  }

  /**
   * The truth value of the corresponding edge is ignored Note as soon as the selector is part of a
   * formula enable, disable and free will not have any effect.
   */
  public void disable() {
    formula = context.getSolver().getFormulaManager().getBooleanFormulaManager().makeFalse();
  }

  /**
   * The solver does not know the truth value of this selector. Note as soon as the selector is part
   * of a formula enable, disable and free will not have any effect.
   */
  public void free() {
    formula = selectorFormula;
  }

  public int getIndex() {
    return index;
  }

  public BooleanFormula getEdgeFormula() {
    return edgeFormula;
  }

  /**
   * Changes the selector formula. Needed for example after loop enrolling. Statements on the exact
   * same location in the program should have the same selector
   *
   * @param selector This selector copies the formula from the given selector
   */
  public void changeSelectorFormula(Selector selector) {
    assert selector
        .correspondingEdge()
        .getDescription()
        .equals(correspondingEdge().getDescription());
    if (selectorFormula.equals(formula)) {
      selectorFormula = selector.selectorFormula;
      formula = selectorFormula;
    } else {
      selectorFormula = selector.selectorFormula;
    }
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object q) {
    if (q instanceof Selector) {
      return name.equals(((Selector) q).getName()) && super.equals(q);
    }
    return false;
  }

  @Override
  public int hashCode() {
    // super class changes hashcode on adding reasons but Selectors shouldn't
    // a selector stays a selector for one edge (reasons do not matter)
    return Objects.hash(Selector.class, name.hashCode());
  }

  public static class Factory {
    private int maxIndex;
    private final Map<String, Selector> selectors;

    public Factory() {
      maxIndex = 0;
      selectors = new HashMap<>();
    }

    public Optional<Selector> selectorOf(BooleanFormula formula) {
      return Optional.ofNullable(selectors.get(formula.toString()));
    }

    /**
     * Creates an unique Selector for every formula that is passed. If a selector for a formula has
     * already been created the existent selector is returned
     *
     * @param pContext the context of the TraceFormula
     * @param pFormula the formula that can be selected with the returned selector
     * @param pEdge the edge which corresponds to the selector
     * @return unique selector for edge pEdge
     */
    public Selector makeSelector(FormulaContext pContext, BooleanFormula pFormula, CFAEdge pEdge) {
      Selector selector = selectors.get(pFormula.toString());
      if (selector != null) {
        return selector;
      }

      selector =
          new Selector(
              maxIndex,
              pContext
                  .getSolver()
                  .getFormulaManager()
                  .makeVariable(FormulaType.BooleanType, "S" + maxIndex),
              pFormula,
              pEdge,
              pContext);
      selectors.put(pFormula.toString(), selector);
      maxIndex++;
      return selector;
    }
  }
}
