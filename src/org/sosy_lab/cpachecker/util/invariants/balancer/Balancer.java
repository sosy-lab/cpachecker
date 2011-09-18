/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.invariants.balancer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.invariants.Farkas;
import org.sosy_lab.cpachecker.util.invariants.LinearInequality;
import org.sosy_lab.cpachecker.util.invariants.interfaces.VariableManager;
import org.sosy_lab.cpachecker.util.invariants.redlog.EliminationAnswer;
import org.sosy_lab.cpachecker.util.invariants.redlog.EliminationHandler;
import org.sosy_lab.cpachecker.util.invariants.redlog.Rational;
import org.sosy_lab.cpachecker.util.invariants.redlog.RedlogInterface;
import org.sosy_lab.cpachecker.util.invariants.templates.AliasingMap;
import org.sosy_lab.cpachecker.util.invariants.templates.Purification;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormula;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateLinearizer;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateVariableManager;
import org.sosy_lab.cpachecker.util.invariants.templates.VariableWriteMode;

public class Balancer {

  // options:
  private UIFAxiomStrategy strategy = UIFAxiomStrategy.DONOTUSEAXIOMS;
  private boolean lhsParamsNotAllZero = true;

  private final LogManager logger;
  private final Path cePath;
  private final RedlogInterface RLI;

  public Balancer(LogManager pLogger, Path pPath) {
    logger = pLogger;
    cePath = pPath;
    RLI = new RedlogInterface(logger);
  }

  public boolean balance(TemplateNetwork tnet) throws RefinementFailedException {

    //Find a satisfiable formula for each Transition, considered individually.
    Vector<Transition> transitions = tnet.getTransitions();
    for (Transition t : transitions) {
      findSingleTransitionFormula(t, tnet);
    }

    // Now we hope that the individual formulas are simultaneously satisfiable.
    // If not, we'll have to program some recourse. For now, we have none.

    // Build the combined elimination formula,
    // and combine the elimination parameters.
    String Phi = "";
    List<String> params = new Vector<String>();
    for (Transition t : transitions) {
      Phi += " and " + t.getEliminationFormula(lhsParamsNotAllZero);
      params.addAll(t.getEliminationParameters());
    }
    Phi = "rlex(" + Phi.substring(5) + ")";
    logger.log(Level.ALL, "Combined formula for all transitions:\n", Phi);

    // Attempt to eliminate variables, and compute values for all parameters.
    boolean succeed = false;
    HashMap<String,Rational> map = getParameterValuesFromRedlog(Phi, params);

    if (map == null) {
      logger.log(Level.FINEST, "Redlog could not find values for all parameters.");
    } else {
      succeed = tnet.evaluate(map);
      if (!succeed) {
        logger.log(Level.FINEST, "Redlog appears to have completed, although not all parameters received values. Check for 'infinity' values.");
      }
    }

    return succeed;
  }

  /**
   * For a single Transition in a Program, see whether there is
   * a satisfiable formula for that Transition, for any possible ordered
   * set of UIF axioms.
   * @return a satisfiable formula, as a String, if there is one; null if not.
   */
  private void findSingleTransitionFormula(Transition t, TemplateNetwork prog) throws RefinementFailedException {

    // Get the template map.
    TemplateMap tmap = prog.getTemplateMap();

    // Get the templates and the path formula.
    TemplateFormula t1, p, t2;
    t1 = tmap.getTemplate(t.getStart());
    p = t.getConstraint();
    t2 = tmap.getTemplate(t.getEnd());
    if (t2 == t1) {
      t2 = t1.copy();
    }
    logger.log(Level.ALL, "\nInitial template:\n", t1, "\nPath formula:\n", p, "\nFinal template:\n", t2);

    // Index the templates so they match up with the path formula.
    Map<String,Integer> indices = p.getMaxIndices();
    t1.preindex(indices);
    t2.postindex(indices);
    logger.log(Level.ALL, "\nPreindexed initial template:\n", t1, "\nPostindexed final template:\n", t2);

    // Alias all the program variables, and keep the AliasingMap.
    AliasingMap amap = new AliasingMap("v");
    t1.alias(amap);
    p.alias(amap);
    t2.alias(amap);
    logger.log(Level.ALL, "\nAliased formulas:\nInitial template:\n", t1, "\nPath formula:\n", p, "\nFinal template:\n", t2);

    // Purify the formulas, and keep the Purification.
    Purification pur = new Purification("u");
    t1.purify(pur);
    p.purify(pur);
    t2.purify(pur);
    logger.log(Level.ALL, "\nPurified formulas:\nInitial template:\n", t1, "\nPath formula:\n", p, "\nFinal template:\n", t2);

    // Build variable manager.
    int n = amap.size();
    int m = pur.size();
    TemplateVariableManager vmgr = new TemplateVariableManager(n,m);
    logger.log(Level.ALL, "Variable manager:\n", vmgr);

    // Find a formula, using the desired strategy.
    switch (strategy) {
    case DONOTUSEAXIOMS:
      findSingleTransitionFormulaWithoutUIFAxioms(t,t1,p,t2,vmgr); break;
    case USEAXIOMS:
      findSingleTransitionFormulaWithUIFAxioms(t,t1,p,t2,vmgr,pur); break;
    }

    // Restore the templates, since they may be involved in other transitions.
    t1.unpurify();
    t1.unalias();
    t1.unindex();

    t2.unpurify();
    t2.unalias();
    t2.unindex();

  }

  private void findSingleTransitionFormulaWithoutUIFAxioms(Transition t, TemplateFormula t1, TemplateFormula p, TemplateFormula t2, VariableManager vmgr) {
    String Phi = consec(t1,p,t2,vmgr);
    List<String> params = getParameters(t1,p,t2,VariableWriteMode.REDLOG);
    t.setEliminationFormula(Phi);
    t.setEliminationParameters(params);
  }

  private void findSingleTransitionFormulaWithUIFAxioms(Transition t, TemplateFormula t1, TemplateFormula p, TemplateFormula t2,
      VariableManager vmgr, Purification pur) throws RefinementFailedException {

    // Get set of possible UIF axioms.
    TemplateFormula[] tfs = {t1, p, t2};
    UIFAxiomSet A = new UIFAxiomSet(pur, tfs);

    // Find a formula.
    String Phi = null;
    while (Phi == null && A.hasMore()) {

      //build possibly-satisfiable formula, and get its parameter list
      Vector<UIFAxiom> U = A.getNext();
      Phi = consec(t1,p,t2,U,vmgr);
      List<String> params = getParameters(t1,p,t2,U,VariableWriteMode.REDLOG);

      //check whether it's satisfiable
      HashMap<String,Rational> map = getParameterValuesFromRedlog(Phi, params);
      if (map == null) {
        // Not satisfiable, so get ready to try again.
        Phi = null;
      } else {
        // Satisfiable, so store it in the transition.
        t.setEliminationFormula(Phi);
        t.setEliminationParameters(params);
      }
    }

    if (Phi == null) {
      logger.log(Level.FINEST, "No combination of UIFAxioms worked for transition.");
      logger.log(Level.ALL, "Formula:\n",t);
      throw new RefinementFailedException(Reason.InvariantRefinementFailed, cePath);
    }

  }

  /**
   * Ask Redlog to find parameter values that satisfy a formula.
   * @param phi The formula to be satisfied.
   * @param params The parameters for which to find values.
   * @return A map from parameter names to satisfying Rationals, if any are found; null otherwise.
   */
  private HashMap<String,Rational> getParameterValuesFromRedlog(String phi, List<String> params) {
    HashMap<String,Rational> map = null;
    try {
      EliminationAnswer EA = RLI.rlqea(phi);
      EliminationHandler EH = new EliminationHandler(EA);
      map = EH.getParameterValues(params);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to read a result from Redlog.");
    }
    return map;
  }

  private List<String> getParameters(TemplateFormula P, TemplateFormula R,
      TemplateFormula Q, VariableWriteMode vwm) {
    Vector<UIFAxiom> U = new Vector<UIFAxiom>();
    return getParameters(P,R,Q,U,vwm);
  }

  private List<String> getParameters(TemplateFormula P, TemplateFormula R,
      TemplateFormula Q, Vector<UIFAxiom> U, VariableWriteMode vwm) {
    Vector<String> params = new Vector<String>();

    params.addAll( P.getAllParameters(vwm) );
    params.addAll( R.getAllParameters(vwm) );
    params.addAll( Q.getAllParameters(vwm) );
    for (UIFAxiom A : U) {
      params.addAll( A.getAllParameters(vwm) );
    }

    return params;
  }

  private TemplateVariableManager getVariableManager(TemplateFormula P, TemplateFormula R,
      TemplateFormula Q, Vector<UIFAxiom> U) {
    TemplateVariableManager V = P.getVariableManager();
    V.merge(Q.getVariableManager());
    V.merge(R.getVariableManager());
    for (UIFAxiom A : U) {
      V.merge(A.getVariableManager());
    }
    return V;
  }

  public String consec(TemplateFormula P, TemplateFormula R, TemplateFormula Q, Vector<UIFAxiom> U) {
    TemplateVariableManager vmgr = getVariableManager(P,R,Q,U);
    return consec(P,R,Q,U,vmgr);
  }

  public String consec(TemplateFormula Pt, TemplateFormula Rt, TemplateFormula Qt, VariableManager vmgr) {
    Vector<UIFAxiom> U = new Vector<UIFAxiom>();
    return consec(Pt,Rt,Qt,U,vmgr);
  }

  public String consec(TemplateFormula Pt, TemplateFormula Rt, TemplateFormula Qt,
      Vector<UIFAxiom> U, VariableManager vmgr) {

    // Linearize
    LinearInequality P = TemplateLinearizer.linearize(Pt, vmgr);
    LinearInequality R = TemplateLinearizer.linearize(Rt, vmgr);
    LinearInequality Q = TemplateLinearizer.linearize(Qt, vmgr);

    // Initialize the formula Psi
    String Psi = "";

    // Declare loop variables.
    LinearInequality prem, concl;
    String Phi;

    //loop
    //Example: (A = antecedent, C = consequent)
    //  U = <Ax0, Ax1, Ax2>
    //  P ^ R --> A0
    //  P ^ R ^ C0 --> A1
    //  P ^ R ^ C0 ^ C1 --> A2
    //  P ^ R ^ C0 ^ C1 ^ C2 --> Q
    prem = P.combine(R);
    for (int i = 0; i < U.size(); i++) {
      UIFAxiom A = U.get(i);
      concl = TemplateLinearizer.linearize(A.getAntecedent(), vmgr);
      Phi = Farkas.makeRedlogFormula(prem, concl);
      Psi += " and " + Phi;
      prem.append(TemplateLinearizer.linearize(A.getConsequent(), vmgr));
    }
    concl = Q;
    logger.log(Level.ALL,"Linearized premises and conclusions:\nPremises:\n",prem,"\nConclusions:\n",concl);
    Phi = Farkas.makeRedlogFormula(prem, concl);
    Psi += " and " + Phi;

    //Psi begins with a superfluous " and ".
    assert(Psi.length() >= 5);
    Psi = Psi.substring(5);

    logger.log(Level.ALL, "Consecution formula for Redlog:\n",Psi);

    return Psi;
  }

  public enum UIFAxiomStrategy {
    USEAXIOMS, DONOTUSEAXIOMS;
  }

}

















