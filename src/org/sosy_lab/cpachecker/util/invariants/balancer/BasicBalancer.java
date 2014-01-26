/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.invariants.Farkas;
import org.sosy_lab.cpachecker.util.invariants.LinearInequality;
import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.invariants.balancer.interfaces.MatrixI;
import org.sosy_lab.cpachecker.util.invariants.interfaces.VariableManager;
import org.sosy_lab.cpachecker.util.invariants.redlog.EliminationAnswer;
import org.sosy_lab.cpachecker.util.invariants.redlog.EliminationHandler;
import org.sosy_lab.cpachecker.util.invariants.redlog.RedlogInterface;
import org.sosy_lab.cpachecker.util.invariants.templates.AliasingMap;
import org.sosy_lab.cpachecker.util.invariants.templates.Purification;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateBoolean;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateConjunction;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateDisjunction;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormula;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateLinearizer;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateVariableManager;
import org.sosy_lab.cpachecker.util.invariants.templates.VariableWriteMode;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;


public class BasicBalancer implements Balancer {

  // options:
  private UIFAxiomStrategy strategy = UIFAxiomStrategy.DONOTUSEAXIOMS;

  // Get a matriciser (turns formulas into matrices).
  // Here we decide between "basic" and "IR" matrices.
  private FormulaMatriciser formMat = new BasicFormulaMatriciser();
  //private FormulaMatriciser formMat = new BasicReversedFormulaMatriciser();
  //private FormulaMatriciser formMat = new IRFormulaMatriciser();

  // end options.

  private Map<String, Variable> paramVars = null;

  private LogManager logger;
  private final RedlogInterface RLI;

  final Timer redlog = new Timer();

  public BasicBalancer(Configuration config, LogManager lm) {
    logger = lm;
    RLI = new RedlogInterface(config, logger);
  }

  public void setAxiomStrategy(UIFAxiomStrategy s) {
    strategy = s;
  }

  @Override
  public boolean balance(TemplateNetwork tnet) throws RefinementFailedException {
    //int[] methods = {0,1};
    int[] methods = {1};
    boolean succeed = false;
    for (int i = 0; i < methods.length; i++) {
      int method = methods[i];
      switch (method) {
      case 0:
        succeed = balanceWithRREF(tnet);
        break;
      case 1:
        succeed = balanceWithQEonly(tnet);
        break;
      }
      if (succeed) {
        // If successful, then exit for-loop.
        break;
      }
    }
    return succeed;
  }

  /*
   * Attempt to balance, using our Reduced Row-Echelon Form heuristic.
   */
  public boolean balanceWithRREF(TemplateNetwork tnet) throws RefinementFailedException {

    logger.log(Level.FINEST, "Attempting to balance template network with RREF heuristic.");

    // Get the set of all parameters.
    Set<String> params = tnet.writeAllParameters(VariableWriteMode.REDLOG);

    // Create Variables, and a map from parameter Strings to Variables.
    paramVars = makeParamVars(params);

    // Get the RREF assumptions for each transition.
    AssumptionSet aset = new AssumptionSet();
    for (Transition t : tnet.getTransitions()) {
      AssumptionSet as = getRREFassumptions(t, tnet);
      aset.addAll(as);
      tnet.setAssumptions(aset);
    }

    HashMap<String, Rational> map = null;
    boolean succeed = false;

    // If set is empty, then there are no conditions on the parameters.
    if (aset.size() == 0) {
      map = new HashMap<>();
    } else {
      // Write the QE formula for the assumptions.
      String phi = writeRREFassumptionQEformula(aset);
      logger.log(Level.ALL, "QE formula for all RREF assumptions:\n", phi);

      // Attempt quantifier elimination, and determination of values for all parameters.
      redlog.start();
      map = getParameterValuesFromRedlog(phi, params);
      redlog.stop();
      logger.log(Level.ALL, "Redlog took", redlog.getSumTime().formatAs(TimeUnit.SECONDS));
    }

    if (map == null) {
      logger.log(Level.FINEST, "Redlog could not find values for all parameters.");
    } else {
      // Set parameters to zero for which Redlog specified no value.
      fillInZeros(map, params);
      // Now evaluate the tnet.
      succeed = tnet.evaluate(map);
      if (!succeed) {
        logger.log(Level.FINEST, "Redlog appears to have completed, although not all parameters received values. Check for 'infinity' values.");
        logger.log(Level.ALL, "Templates after attempted evaluation:\n", tnet.dumpTemplates());
      }
    }

    return succeed;

  }

  private Map<String, Variable> makeParamVars(Set<String> params) {
    Map<String, Variable> paramVars = new HashMap<>();
    for (String p : params) {
      Variable v = new Variable(p);
      paramVars.put(p, v);
    }
    return paramVars;
  }

  private AssumptionSet getRREFassumptions(Transition t, TemplateNetwork tnet)
    throws RefinementFailedException {
    processSingleTransition(t, tnet, true);
    AssumptionSet aset = t.getRREFassumptions();
    logger.log(Level.ALL, "Assumptions:\n", aset);
    return aset;
  }

  private String writeRREFassumptionQEformula(AssumptionSet aset) {
    String phi = "";
    for (Assumption a : aset) {
      phi += " and "+a.toString();
    }
    if (phi.length() > 0) {
      phi = phi.substring(5);
    }
    phi = "rlex("+phi+")";
    return phi;
  }

  /*
   * Attempt to balance, using Quantifier Elimination only.
   */
  public boolean balanceWithQEonly(TemplateNetwork tnet) throws RefinementFailedException {

    logger.log(Level.FINEST, "Attempting to balance template network with QE only.");

    //Find a satisfiable formula for each Transition, considered individually.
    Vector<Transition> transitions = tnet.getTransitions();
    for (Transition t : transitions) {
      processSingleTransition(t, tnet, false);
    }

    // Now we hope that the individual formulas are simultaneously satisfiable.
    // If not, we'll have to program some recourse. For now, we have none.

    // Build the combined elimination formula,
    // and combine the elimination parameters.
    String Phi = "";
    Set<String> params = new HashSet<>();
    for (Transition t : transitions) {
      Phi += " and " + t.getEliminationFormula();
      params.addAll(t.writeAllParameters(VariableWriteMode.REDLOG));
    }
    // Add nonzero parameter clause.
    Phi += " and " + writeNonzeroParameterClause(tnet);
    // Quantify variables.
    Phi = "rlex(" + Phi.substring(5) + ")";
    logger.log(Level.ALL, "Combined formula for all transitions:\n", Phi);

    // Attempt quantifier elimination, and determination of values for all parameters.
    boolean succeed = false;
    redlog.start();
    HashMap<String, Rational> map = getParameterValuesFromRedlog(Phi, params);
    redlog.stop();
    logger.log(Level.ALL, "Redlog took", redlog.getSumTime().formatAs(TimeUnit.SECONDS));

    if (map == null) {
      logger.log(Level.FINEST, "Redlog could not find values for all parameters.");
    } else {
      // Set parameters to zero for which Redlog specified no value.
      fillInZeros(map, params);
      // Now evaluate the tnet.
      succeed = tnet.evaluate(map);
      if (!succeed) {
        logger.log(Level.FINEST, "Redlog appears to have completed, although not all parameters received values. Check for 'infinity' values.");
        logger.log(Level.ALL, "Templates after attempted evaluation:\n", tnet.dumpTemplates());
      }
    }

    return succeed;
  }

  /**
   * For a single Transition in a Program, see whether there is
   * a satisfiable formula for that Transition, for any possible ordered
   * set of UIF axioms.
   * Return a satisfiable formula, as a String, if there is one; null if not.
   */
  private void processSingleTransition(Transition t, TemplateNetwork tnet, boolean useRREF)
    throws RefinementFailedException {

    // Get the template map.
    TemplateMap tmap = tnet.getTemplateMap();

    // Get the templates and the path formula.
    TemplateFormula t1, p, t2;
    t1 = tmap.getTemplate(t.getStart()).getTemplateFormula();
    p = t.getConstraint();
    t2 = tmap.getTemplate(t.getEnd()).getTemplateFormula();
    if (t2 == t1) {
      t2 = t1.copy();
    }
    logger.log(Level.ALL, "\nInitial template:\n", t1, "\nPath formula:\n", p, "\nFinal template:\n", t2);

    // Index the templates so they match up with the path formula.
    Map<String, Integer> indices = p.getMaxIndices();
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
    logger.log(Level.ALL, "\nPurification definitions:\n", pur);

    // Compute Strong DNF of antecedent.
    TemplateBoolean antB = TemplateConjunction.conjoin((TemplateBoolean) t1, (TemplateBoolean) p);
    TemplateDisjunction antD = (TemplateDisjunction) antB.makeSDNF();
    logger.log(Level.ALL, "\nSDNF of antecedent:\n", antD);

    // Build variable manager.
    int n = amap.size();
    int m = pur.size();

    // TODO: Review FormulaType.NumericType
    TemplateVariableManager vmgr = new TemplateVariableManager(FormulaType.RationalType, n, m);
    logger.log(Level.ALL, "Variable manager:\n", vmgr);

    // Find a formula, using the desired strategy.
    switch (strategy) {
    case DONOTUSEAXIOMS:
      if (useRREF) {
        findSingleTransitionRREFassumptionsWithoutUIFAxioms(t, antD, t2, vmgr, tnet);
      } else {
        findSingleTransitionFormulaWithoutUIFAxioms(t, antD, t2, vmgr);
      }
      break;
    case USEAXIOMS:
      if (m >= 2) {
        findSingleTransitionFormulaWithUIFAxioms(t, antD, t2, vmgr, pur);
        // TODO: reimplement this:
        /*
        if (useRREF) {
          findSingleTransitionRREFassumptionsWithUIFAxioms(t, antD, t2, vmgr, pur);
        } else {
          findSingleTransitionFormulaWithUIFAxioms(t, antD, t2, vmgr, pur);
        }
        */
      } else {
        // FIXME: Really, we should just quit at this point. But we need to
        // refactor, and reorganize the loop control!
        if (useRREF) {
          findSingleTransitionRREFassumptionsWithoutUIFAxioms(t, antD, t2, vmgr, tnet);
        } else {
          findSingleTransitionFormulaWithoutUIFAxioms(t, antD, t2, vmgr);
        }
      }
      break;
    }

    /*
    findSingleTransitionFormulaWithoutUIFAxioms(t, antD, t2, vmgr);
    if (m > 0 && !t.hasEliminationFormula()) {
      findSingleTransitionFormulaWithUIFAxioms(t, antD, t2, vmgr, pur);
    }
    */

    // Restore templates and path formula.
    // They may be involved in other transitions, or we may restart the entire process
    // with different invariant templates.
    t1.unpurify();
    t1.unalias();
    t1.unindex();

    t2.unpurify();
    t2.unalias();
    t2.unindex();

    p.unalias();
    p.unpurify();
  }

  private void findSingleTransitionRREFassumptionsWithoutUIFAxioms(
      Transition t, TemplateDisjunction ant, TemplateFormula t2,
      VariableManager vmgr, TemplateNetwork tnet) {
    AssumptionSet aset = consecRREF(ant, t2, vmgr, tnet);
    t.setRREFassumptions(aset);
  }

  private void findSingleTransitionFormulaWithoutUIFAxioms(Transition t, TemplateDisjunction ant, TemplateFormula t2, VariableManager vmgr) {
    String Phi = consecQE(ant, t2, vmgr);
    t.setEliminationFormula(Phi);
  }

  /*
  private void findSingleTransitionRREFassumptionsWithUIFAxioms(Transition t, TemplateDisjunction ant, TemplateFormula t2,
      VariableManager vmgr, Purification pur) throws RefinementFailedException {
    // TODO
  }
  */

  private void findSingleTransitionFormulaWithUIFAxioms(Transition t, TemplateDisjunction ant, TemplateFormula t2,
      VariableManager vmgr, Purification pur) throws RefinementFailedException {

    // Get set of possible UIF axioms.
    TemplateFormula[] tfs = {ant, t2};

    UIFAxiomSet A = new UIFAxiomSet(pur, tfs);

    // Find a formula.
    String Phi = null;
    while (Phi == null && A.hasMore()) {

      //build possibly-satisfiable formula, and get its parameter list
      Vector<UIFAxiom> U = A.getNext();
      logger.log(Level.ALL, "UIF Axiom Set:\n", U);

      Phi = consecQE(ant, t2, U, vmgr);

      Set<String> params = t.writeAllParameters(VariableWriteMode.REDLOG);
      params.addAll(getParameters(U, VariableWriteMode.REDLOG));

      // Test.
      // Add nonzero parameter clause.
      String Psi = Phi + " and " + writeNonzeroParameterClause(t);
      // Quantify variables.
      Psi = "rlex(" + Psi + ")";
      logger.log(Level.ALL, "Quantified transition formula:\n", Psi);

      //check whether it's satisfiable
      HashMap<String, Rational> map = getParameterValuesFromRedlog(Psi, params);
      if (map == null) {
        // Not satisfiable, so get ready to try again.
        Phi = null;
      } else {
        // Satisfiable, so store it in the transition.
        t.setEliminationFormula(Phi);
      }
    }

    if (Phi == null) {
      logger.log(Level.FINEST, "No combination of UIFAxioms worked for transition.");
      logger.log(Level.ALL, "Formula:\n",t.getEliminationFormula());
    }

  }

  /**
   * Ask Redlog to find parameter values that satisfy a formula.
   * @param phi The formula to be satisfied.
   * @param params The parameters for which to find values.
   * @return A map from parameter names to satisfying Rationals, if any are found; null otherwise.
   */
  private HashMap<String, Rational> getParameterValuesFromRedlog(String phi, Set<String> params) {
    HashMap<String, Rational> map = null;
    try {
      EliminationAnswer EA = RLI.rlqea(phi);
      if (EA != null) {
        if (EA.getTruthValue() == false) {
          logger.log(Level.ALL, "Redlog says formula is unsatisfiable.");
        } else {
          EliminationHandler EH = new EliminationHandler(EA);
          map = EH.getParameterValues(params);
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to read a result from Redlog.",e);
    }
    return map;
  }

  private void fillInZeros(Map<String, Rational> map, Set<String> params) {
    Set<String> dom = map.keySet();
    Rational r = Rational.makeZero();
    for (String p : params) {
      if (!dom.contains(p)) {
        map.put(p, r);
      }
    }
  }

  private String writeNonzeroParameterClause(TemplateNetwork tnet) {
    String zeta = "";
    Vector<TemplateBoolean> clauses = tnet.getAllNonzeroParameterClauses();
    TemplateConjunction conj = new TemplateConjunction(clauses);
    zeta = conj.toString(VariableWriteMode.REDLOG);
    return zeta;
  }

  private String writeNonzeroParameterClause(Transition t) {
    String zeta = "";
    Vector<TemplateBoolean> clauses = t.getAllNonzeroParameterClauses();
    TemplateConjunction conj = new TemplateConjunction(clauses);
    zeta = conj.toString(VariableWriteMode.REDLOG);
    return zeta;
  }

  private List<String> getParameters(Vector<UIFAxiom> U, VariableWriteMode vwm) {
    TemplateFormula[] F = {};
    return getParameters(F, U, vwm);
  }

  private List<String> getParameters(TemplateFormula[] F, Vector<UIFAxiom> U, VariableWriteMode vwm) {
    Vector<String> params = new Vector<>();

    for (TemplateFormula f : F) {
      params.addAll(f.getAllParameters(vwm));
    }
    for (UIFAxiom A : U) {
      params.addAll(A.getAllParameters(vwm));
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

  @Deprecated
  public String consecQE(TemplateFormula P, TemplateFormula R, TemplateFormula Q, Vector<UIFAxiom> U) {
    TemplateVariableManager vmgr = getVariableManager(P, R, Q, U);
    return consecQE(P, R, Q, U, vmgr);
  }

  @Deprecated
  public String consecQE(TemplateFormula P, TemplateFormula R, TemplateFormula Q, VariableManager vmgr) {
    Vector<UIFAxiom> U = new Vector<>();
    return consecQE(P, R, Q, U, vmgr);
  }

  public AssumptionSet consecRREF(TemplateDisjunction ant, TemplateFormula t2,
      VariableManager vmgr, TemplateNetwork tnet) {
    Vector<UIFAxiom> U = new Vector<>();
    return consecRREF(ant, t2, U, vmgr, tnet);
  }

  public String consecQE(TemplateDisjunction ant, TemplateFormula t2, VariableManager vmgr) {
    Vector<UIFAxiom> U = new Vector<>();
    return consecQE(ant, t2, U, vmgr);
  }

  @Deprecated
  public String consecQE(TemplateFormula Pt, TemplateFormula Rt, TemplateFormula Qt,
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
    logger.log(Level.ALL, "Linearized premises and conclusions:\nPremises:\n",prem,"\nConclusions:\n",concl);
    Phi = Farkas.makeRedlogFormula(prem, concl);
    Psi += " and " + Phi;

    //Psi begins with a superfluous " and ".
    assert (Psi.length() >= 5);
    Psi = Psi.substring(5);

    logger.log(Level.ALL, "Consecution formula for Redlog:\n",Psi);

    return Psi;
  }

  /**
   * In this version of the consec function, we expect ant to be in SDNF, and we expect
   * t2 to be a conjunction of TemplateConstraints.
   */
  public String consecQE(TemplateDisjunction ant, TemplateFormula t2,
      Vector<UIFAxiom> U, VariableManager vmgr) {

    // Linearize
    LinearInequality li;
    // Create Vector containing the linearization of each disjunct in ant:
    Vector<LinearInequality> linearAntParts = new Vector<>(ant.getNumDisjuncts());
    Vector<TemplateBoolean> disjuncts = ant.getDisjuncts();
    for (TemplateBoolean d : disjuncts) {
      li = TemplateLinearizer.linearize(d, vmgr);
      linearAntParts.add(li);
    }

    /*
     * This would be needed only if con could be an arbitrary SCNF formula.
     * Delete this when we are sure we don't want to allow that.
    // Create Vector containing the linearization of each conjunct in con:
    Vector<LinearInequality> linearConParts = new Vector<>(con.getNumConjuncts());
    Vector<TemplateBoolean> conjuncts = con.getConjuncts();
    for (TemplateBoolean c : conjuncts) {
      li = TemplateLinearizer.linearize(c, vmgr);
      linearConParts.add(li);
    }
    */
    LinearInequality Q = TemplateLinearizer.linearize(t2, vmgr);

    // Logically speaking, we require that each disjunct in ant taken individually imply each
    // of the constraints in con.

    // Initialize the formula Psi
    String Psi = "";

    // Declare loop variables.
    LinearInequality concl;
    String Phi;
    for (LinearInequality prem : linearAntParts) {
      //loop
      //Example: (A = antecedent, C = consequent)
      //  U = <Ax0, Ax1, Ax2>
      //  P ^ R --> A0
      //  P ^ R ^ C0 --> A1
      //  P ^ R ^ C0 ^ C1 --> A2
      //  P ^ R ^ C0 ^ C1 ^ C2 --> Q
      for (int i = 0; i < U.size(); i++) {
        UIFAxiom A = U.get(i);
        concl = TemplateLinearizer.linearize(A.getAntecedent(), vmgr);
        logger.log(Level.ALL, "UIFAxiom:\n",A);
        logger.log(Level.ALL, "Linearized premises and conclusions:\nPremises:\n",prem,"\nConclusions:\n",concl);
        Phi = Farkas.makeRedlogFormulaUsingPremiseStrength(prem, concl);
        Psi += " and " + Phi;
        prem.append(TemplateLinearizer.linearize(A.getConsequent(), vmgr));
      }
      concl = Q;
      logger.log(Level.ALL, "Linearized premises and conclusions:\nPremises:\n",prem,"\nConclusions:\n",concl);
      Phi = Farkas.makeRedlogFormulaUsingPremiseStrength(prem, concl);
      Psi += " and " + Phi;
    }

    //Psi begins with a superfluous " and ".
    assert (Psi.length() >= 5);
    Psi = Psi.substring(5);

    logger.log(Level.ALL, "Consecution formula for Redlog:\n",Psi);

    return Psi;
  }

  /**
   * In this version of the consec function, we expect ant to be in SDNF, and we expect
   * t2 to be a conjunction of TemplateConstraints.
   */
  public AssumptionSet consecRREF(TemplateDisjunction ant, TemplateFormula t2,
      Vector<UIFAxiom> U, VariableManager vmgr, TemplateNetwork tnet) {

    // Initialize assumption set.
    AssumptionSet aset = new AssumptionSet();

    // Build matrices.
    // Create Vector containing the linearization of each disjunct in ant:
    Vector<MatrixI> matrixAntParts = new Vector<>(ant.getNumDisjuncts());
    Vector<TemplateBoolean> disjuncts = ant.getDisjuncts();
    for (TemplateBoolean d : disjuncts) {
      // According to the formulation of Farkas's lemma in
      // Colon, Sankararanayanan, and Sipma, each matrix gets a column
      // representing "true", in the form "-1 <= 0".
      boolean prependTrue = true;
      MatrixI m = formMat.buildMatrix(d, vmgr, paramVars, prependTrue);
      // Add the matrix to the list.
      matrixAntParts.add(m);
    }

    boolean prependTrue = false;
    MatrixI Q = formMat.buildMatrix(t2, vmgr, paramVars, prependTrue);

    // Logically speaking, we require that each disjunct in ant taken individually imply each
    // of the constraints in Q.

    // Declare loop variables.
    MatrixI concl;
    for (MatrixI prem : matrixAntParts) {
      //loop
      //Example: (A = antecedent, C = consequent)
      //  U = <Ax0, Ax1, Ax2>
      //  P ^ R --> A0
      //  P ^ R ^ C0 --> A1
      //  P ^ R ^ C0 ^ C1 --> A2
      //  P ^ R ^ C0 ^ C1 ^ C2 --> Q
      for (int i = 0; i < U.size(); i++) {
        prependTrue = false;
        UIFAxiom A = U.get(i);
        concl = formMat.buildMatrix(A.getAntecedent(), vmgr, paramVars, prependTrue);
        logger.log(Level.ALL, "UIFAxiom:\n",A);
        logger.log(Level.ALL, "Linearized premises and conclusions:\nPremises:","\n"+prem.toString(),
            "\nConclusions:","\n"+concl.toString());
        aset.addAll(applyRREFheuristic(prem, concl, tnet));
        prem = prem.concat(formMat.buildMatrix(A.getConsequent(), vmgr, paramVars, prependTrue));
      }
      concl = Q;
      //logger.log(Level.ALL,"Linearized premises and conclusions:\nPremises:\n",prem,"\nConclusions:\n",concl);
      logger.log(Level.ALL, "Linearized premises and conclusions:\nPremises:","\n"+prem.toString(),
          "\nConclusions:","\n"+concl.toString());
      aset.addAll(applyRREFheuristic(prem, concl, tnet));
    }

    return aset;
  }

  private AssumptionSet applyRREFheuristic(MatrixI prem, MatrixI concl, TemplateNetwork tnet) {
    MatrixI aug = prem.augment(concl);
    Matrix au = null;
    try {
      au = (Matrix)aug;
    } catch (ClassCastException e) {
      return applyRREFheuristicOLD(prem, concl);
    }
    logger.log(Level.ALL, "Augmented Matrix:","\n"+au.toString());
    MatrixSolver ms = new MatrixSolver(au, logger);
    Set<AssumptionSet> asetset;
    Timer msTimer = new Timer();
    msTimer.start();
    try {
      asetset = ms.solve(tnet.getAssumptions());
      msTimer.stop();
      logger.log(Level.ALL, "MatrixSolver took", msTimer.getSumTime().formatAs(TimeUnit.SECONDS));
    } catch (MatrixSolvingFailedException e) {
      msTimer.stop();
      logger.log(Level.ALL, e.getReason());
      logger.log(Level.ALL, "MatrixSolver took", msTimer.getSumTime().formatAs(TimeUnit.SECONDS));
      return new AssumptionSet();
    }
    // For the moment we just return the first set.
    // In reality, we need to return a set of sets of conditions, and try different combinations
    // of sets of conditions for the various matrices involved in balancing the network.
    if (asetset.size() == 0) {
      return new AssumptionSet();
    }
    return asetset.iterator().next();
  }

  private AssumptionSet applyRREFheuristicOLD(MatrixI prem, MatrixI concl) {
    MatrixI aug = prem.augment(concl);
    logger.log(Level.ALL, "Augmented Matrix:","\n"+aug.toString());
    // We gather two types of assumptions.
    // First, those made during the process of putting the matrix into RREF:
    // Verbose:
    AssumptionSet aset = aug.putInRREF(logger);
    // Quiet:
    //AssumptionSet aset = aug.putInRREF();
    MatrixI E = aug.getElemMatProd();
    // Write the RREF and the matrix E to the log, for debugging.
    logger.log(Level.ALL, "RREF:","\n"+aug.toString());
    logger.log(Level.ALL, "Matrix representing row operations performed:","\n"+E.toString());
    // Then, the "almost-zero row" assumptions:
    aset.addAll(aug.getAlmostZeroRowAssumptions());
    return aset;
  }

  public enum UIFAxiomStrategy {
    USEAXIOMS          ("use axioms"),
    DONOTUSEAXIOMS     ("do not use axioms");

    private String strat = "";

    private UIFAxiomStrategy(String s) {
      strat = s;
    }

    @Override
    public String toString() {
      return strat;
    }

  }

}

















