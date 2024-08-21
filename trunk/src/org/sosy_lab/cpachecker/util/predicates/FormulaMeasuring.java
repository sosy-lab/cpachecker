// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import com.google.common.collect.ImmutableSortedSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.visitors.DefaultBooleanFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.TraversalProcess;

public class FormulaMeasuring {

  public static class FormulaMeasures {
    private int conjunctions = 0;
    private int disjunctions = 0;
    private int negations = 0;
    private int atoms = 0;
    private final Set<String> variables = new HashSet<>();

    public int getAtoms() {
      return atoms;
    }

    public int getConjunctions() {
      return conjunctions;
    }

    public int getDisjunctions() {
      return disjunctions;
    }

    public int getNegations() {
      return negations;
    }

    public ImmutableSortedSet<String> getVariables() {
      return ImmutableSortedSet.copyOf(variables);
    }
  }

  private final FormulaManagerView managerView;

  public FormulaMeasuring(FormulaManagerView pManagerView) {
    managerView = pManagerView;
  }

  public FormulaMeasures measure(BooleanFormula formula) {
    FormulaMeasures result = new FormulaMeasures();
    managerView
        .getBooleanFormulaManager()
        .visitRecursively(formula, new FormulaMeasuringVisitor(managerView, result));
    return result;
  }

  private static class FormulaMeasuringVisitor
      extends DefaultBooleanFormulaVisitor<TraversalProcess> {

    private final FormulaMeasures measures;
    private final FormulaManagerView fmgr;

    FormulaMeasuringVisitor(FormulaManagerView pFmgr, FormulaMeasures pMeasures) {
      measures = pMeasures;
      fmgr = pFmgr;
    }

    @Override
    protected TraversalProcess visitDefault() {
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitAtom(
        BooleanFormula pAtom, FunctionDeclaration<BooleanFormula> decl) {
      measures.atoms++;

      BooleanFormula atom = fmgr.uninstantiate(pAtom);
      measures.variables.addAll(fmgr.extractVariableNames(atom));
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitNot(BooleanFormula pOperand) {
      measures.negations++;
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitAnd(List<BooleanFormula> pOperands) {
      measures.conjunctions++;
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitOr(List<BooleanFormula> pOperands) {
      measures.disjunctions++;
      return TraversalProcess.CONTINUE;
    }
  }
}
