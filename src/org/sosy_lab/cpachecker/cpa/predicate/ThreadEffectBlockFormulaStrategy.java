// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class ThreadEffectBlockFormulaStrategy extends BlockFormulaStrategy {

  private final FormulaManagerView fmgr;
  private final PathFormulaManager pathFmgr;

  public ThreadEffectBlockFormulaStrategy(
      FormulaManagerView pFmgr,
      PathFormulaManager pPfgmr) {
    fmgr = pFmgr;
    pathFmgr = pPfgmr;
  }

  private String RenameVariables(String s) {
    String t = s;
    if (!t.contains("::") && !t.endsWith("@")) {
      t = "eff#" + t;
    }
    return t;
  }

  static String RenameVariablesBack(String s) {
    String t = s;
    if (t.startsWith("eff#")) {
      t = t.substring(4, t.length());
    }
    return t;
  }

  @Override
  BlockFormulas getFormulasForPath(ARGState argRoot, List<ARGState> abstractionStates)
      throws CPATransferException, InterruptedException {

    List<ARGState> normalPath = new ArrayList<>();
    SSAMap firstSsa = null, secondSsa;
    List<BooleanFormula> formulasInMain = null;
    BlockFormulas blockFormulas;

    // Right now looks like a hack: divide the list into two paths
    for (ARGState next : abstractionStates) {
      // initialState is filtered from a path, but not from the second part
      if (argRoot.equals(next)) {
        // start of the second part
        assert formulasInMain == null;
        formulasInMain = super.getFormulasForPath(argRoot, normalPath).getFormulas();
        firstSsa = getLastSSAMap(normalPath);
        normalPath.clear();
      }
      normalPath.add(next);
    }

    blockFormulas = super.getFormulasForPath(argRoot, normalPath);

    if (formulasInMain != null) {
      secondSsa = getLastSSAMap(normalPath);

      // making list of formulas in path to effect with renamed variables
      List<BooleanFormula> secondFormulas =
          blockFormulas.getFormulas()
              .stream()
              .map(f -> fmgr.renameFreeVariablesAndUFs(f, this::RenameVariables))
              .collect(Collectors.toList());

      BooleanFormula variableEqualities =
          makeVariableEqualities(formulasInMain, secondFormulas, firstSsa, secondSsa);

      List<BooleanFormula> allFormulas = new ArrayList<>(formulasInMain);
      // Need to add the variable equalities to the first formulas in the second part
      BooleanFormula conjunction = fmgr.makeAnd(secondFormulas.get(0), variableEqualities);
      allFormulas.add(conjunction);
      secondFormulas.stream().skip(1).forEach(allFormulas::add);

      // TODO Branching formulas
      // currently branchingFormulas are not used as checkPath does not support interleavings and it
      // is disabled
      return new BlockFormulas(
          allFormulas,
          pathFmgr.buildBranchingFormula(new HashSet<>(abstractionStates)));

    } else {
      // No devision
      return blockFormulas;
    }
  }

  private BooleanFormula makeVariableEqualities(
      List<BooleanFormula> formulas1,
      List<BooleanFormula> formulas2,
      SSAMap lastSsa1,
      SSAMap lastSsa2) {

    BooleanFormula variableEqualities = fmgr.getBooleanFormulaManager().makeTrue();
    for (String varName : lastSsa1.allVariables()) {
      if (lastSsa2.containsVariable(varName)) {
        FormulaType<Formula> formulaType1, formulaType2;
        formulaType1 = getVariableType(varName, formulas1);
        formulaType2 = getVariableType(RenameVariables(varName), formulas2);

        if (formulaType1 != null && formulaType2 != null && formulaType1 == formulaType2) {
          Formula var1 = fmgr.makeVariable(formulaType1, varName, lastSsa1.getIndex(varName));
          Formula var2 =
              fmgr.makeVariable(formulaType2, RenameVariables(varName), lastSsa2.getIndex(varName));
          variableEqualities = fmgr.makeAnd(variableEqualities, fmgr.makeEqual(var1, var2));
        }
      }
    }
    return variableEqualities;
  }

  private SSAMap getLastSSAMap(List<ARGState> trace) {
    PredicateAbstractState predicateAbstractStateEffect =
        AbstractStates
            .extractStateByType(trace.get(trace.size() - 1), PredicateAbstractState.class);
    return predicateAbstractStateEffect.getPathFormula().getSsa();
  }

  private FormulaType<Formula> getVariableType(String name, List<BooleanFormula> listOfFormulas) {
    // TODO do not hardcode it
    for (BooleanFormula f : listOfFormulas) {
      for (String s : fmgr.extractVariableNames(f)) {
        if (s.startsWith(name + "@")) {
          return fmgr.getFormulaType(fmgr.extractVariables(f).get(s));
        }
      }
    }
    return null;
  }
}
