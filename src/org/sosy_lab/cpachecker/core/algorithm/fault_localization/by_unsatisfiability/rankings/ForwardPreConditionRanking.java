// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.rankings;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.formula.TraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.formula.parser.BooleanFormulaParser;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

public class ForwardPreConditionRanking implements FaultRanking {

  private final TraceFormula traceFormula;
  private final FormulaContext context;

  public ForwardPreConditionRanking(TraceFormula pTraceFormula, FormulaContext pContext){
    traceFormula = pTraceFormula;
    context = pContext;
  }

  /**
   * Tell the user which initial variable assignment lead to an error.
   * This is not an actual ranking.
   * @param result The result of any FaultLocalizationWithTraceFormula
   * @return Faults ranked by identity
   */
  @Override
  public List<Fault> rank(Set<Fault> result) {
    // check if alternative precondition was used
    List<Fault> rankedList = new ArrayList<>(result);
    if(!traceFormula.getPrecondition().toString().contains("_VERIFIER_nondet_")){
      return rankedList;
    }

    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    Set<BooleanFormula> preconditions = bmgr.toConjunctionArgs(traceFormula.getPrecondition(), true);

    Map<String, String> mapFormulaToValue = new HashMap<>();
    List<String> assignments = new ArrayList<>();

    for (BooleanFormula precondition : preconditions) {
      String formulaString = precondition.toString();
      formulaString = formulaString.replaceAll("\\(", "").replaceAll("\\)", "");
      List<String> operatorAndOperands = Splitter.on("` ").splitToList(formulaString);
      if(operatorAndOperands.size() != 2) {
        return rankedList;
      }
      String withoutOperator = operatorAndOperands.get(1);
      List<String> operands = Splitter.on(" ").splitToList(withoutOperator);
      if(operands.size() != 2){
        return rankedList;
      }
      if (operands.get(0).contains("__VERIFIER_nondet_") || (operands.get(0).contains("::") && operands.get(0).contains("@"))){
        if((operands.get(0).contains("::") && operands.get(0).contains("@"))){
          assignments.add(Splitter.on("@").splitToList(operands.get(0)).get(0) + " = " + operands.get(1));
        } else {
          mapFormulaToValue.put(operands.get(0), operands.get(1));
        }
      } else {
        if((operands.get(1).contains("::") && operands.get(1).contains("@"))){
          assignments.add(Splitter.on("@").splitToList(operands.get(1)).get(0) + " = " + operands.get(0));
        } else {
          mapFormulaToValue.put(operands.get(1), operands.get(0));
        }
      }
    }

    List<BooleanFormula> atoms = traceFormula.getEntries().toAtomList();
    for(Entry<String, String> entry: mapFormulaToValue.entrySet()){
      for (int i = 0 ; i < atoms.size(); i++) {
        BooleanFormula atom = atoms.get(i);
        if(atom.toString().contains(entry.getKey())){
          atom = context.getSolver().getFormulaManager().uninstantiate(atom);
          String assignment = BooleanFormulaParser
              .parse(atom.toString().replaceAll(entry.getKey(), entry.getValue())).toString();
          assignments.add(assignment);
        }
      }
    }

    String allAssignments = String.join(", ", assignments);
    for (Fault fault : rankedList) {
      fault.addInfo(FaultInfo.hint("The program fails for the initial variable assignment " + allAssignments));
    }

    return rankedList;
  }
}
