// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.visitors.DefaultBooleanFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.TraversalProcess;


public class SubstituteVisitor extends DefaultBooleanFormulaVisitor<TraversalProcess> {

  private final FormulaManager fmgr;
  public java.util.HashMap<Formula, Formula> fmap;


  public SubstituteVisitor(FormulaManager pFmgr) {
    fmgr = pFmgr;
    fmap = new HashMap<Formula, Formula>();
  }

  @Override
  protected TraversalProcess visitDefault() {
    return TraversalProcess.CONTINUE;
  }

  @Override
  public TraversalProcess visitNot(BooleanFormula pOperand) {
    return TraversalProcess.SKIP;
  }

  @Override
  public TraversalProcess visitAtom(BooleanFormula atom, FunctionDeclaration<BooleanFormula> decl) {
    if (decl.getKind() == FunctionDeclarationKind.EQ) {
      // filter assignments
      // we can add a new entry to the hashmap
      ArrayList<Formula> parts = new ArrayList<Formula>();
      AtomicBoolean end = new AtomicBoolean(); // skip the first iteration, then do exactly two iterations. @TODO Very clusmy, beautify this
      AtomicBoolean end2 = new AtomicBoolean();
      fmgr.visitRecursively(atom, new DefaultFormulaVisitor<TraversalProcess>() {
        @Override
        protected TraversalProcess visitDefault(Formula f) {
          if (end.get() && end2.get()) {
            parts.add(f);
            return TraversalProcess.ABORT; // we have collected both branches
          }
          else if (end.get()) {
            end2.set(true);
            parts.add(f);
            return TraversalProcess.SKIP;  // this branch is finished
          }
          else {
            end.set(true);
            return TraversalProcess.CONTINUE; // the first iteration: f == atom,
          }
        }
      });
//      assert parts.size()==2 ;
      if (parts.size()==2){
        // Todo Martin: Think about
        // a) when should a Equals Formula be added to the substitutiontable (this is done here)
        // b) when should a substitution actually take place (cf. SubstituteAssignmentTransformationVisitor)
        Map<String, Formula> allvariables = fmgr.extractVariables(atom);
        if (allvariables.containsKey(parts.get(0).toString())) {
          fmap.put(parts.get(0), parts.get(1));
        } else if (allvariables.containsKey(parts.get(1).toString())){
          fmap.put(parts.get(1), parts.get(0));
        }
      }
    }
    return TraversalProcess.CONTINUE;
  }
}
