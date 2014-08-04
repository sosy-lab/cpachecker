/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.invariants.Rational;
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
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateVariableManager;
import org.sosy_lab.cpachecker.util.invariants.templates.VariableWriteMode;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;


public abstract class AbstractBalancer implements Balancer {

  // Options:
  private boolean ignorePathFormulaDisjunctions = false;
  //

  LogManager logger;
  RedlogInterface RLI;
  FormulaMatriciser formMat = new BasicFormulaMatriciser();

  TemplateNetwork tnet;
  Map<String, Variable> paramVars = null;
  List<Matrix> matrices;

  final Timer redlog = new Timer();
  boolean redlogReturnedTrue = false;

  public boolean redlogSaidTrue() {
    return redlogReturnedTrue;
  }

  @Override
  public abstract boolean balance(TemplateNetwork tn) throws RefinementFailedException;


  /*
   * We put each of the matrices into RREF, up to the point where we would have to choose a
   * pivot with variable numerator. This process cannot generate any assumptions whatsoever,
   * so we do not return any.
   */
  void innocuousRREF() {
    for (Matrix m : matrices) {
      m.setHaltOnVarNumPivot(true);
      m.putInRREF(logger);
      m.setHaltOnVarNumPivot(false);
    }
  }

  /*
   * Diagnostic method, purely for logging.
   */
  void logMatrices() {
    logger.log(Level.ALL, "Matrices:");
    for (Matrix m : matrices) {
      logger.log(Level.ALL, "\n"+m.toString());
    }
  }

  Map<String, Variable> makeParamVars() {
    Set<String> params = tnet.writeAllParameters(VariableWriteMode.REDLOG);
    Map<String, Variable> paramVars = new HashMap<>();
    for (String p : params) {
      Variable v = new Variable(p);
      paramVars.put(p, v);
    }
    return paramVars;
  }

  void fillInZeros(Map<String, Rational> map, Set<String> params) {
    Set<String> dom = map.keySet();
    Rational r = Rational.makeZero();
    for (String p : params) {
      if (!dom.contains(p)) {
        map.put(p, r);
      }
    }
  }

  /*
   * Pass a set of assumptions to Redlog, and ask it to find values for the parameters.
   * If it succeeds, then we return a map, mapping parameter names to rationals.
   * Else we return null.
   */
  public HashMap<String, Rational> tryAssumptionSet(AssumptionSet aset) {
    logger.log(Level.ALL, "Asking Redlog to find values for the parameters, assuming:\n",aset);
    HashMap<String, Rational> map = null;

    // If set is empty, then there are no conditions on the parameters.
    if (aset.size() == 0) {
      map = new HashMap<>();
    } else {
      // Write the QE formula for the assumptions.
      String phi = aset.writeQEformula();
      logger.log(Level.ALL, "QE formula for all RREF assumptions:\n", phi);

      // Attempt quantifier elimination, and determination of values for all parameters.
      redlog.start();
      map = getParameterValuesFromRedlog(phi, paramVars.keySet());
      redlog.stop();
      logger.log(Level.ALL, "Redlog took", redlog.getSumTime().formatAs(TimeUnit.SECONDS));
    }

    if (map == null) {
      logger.log(Level.FINEST, "Redlog could not find values for all parameters.");
    }

    return map;
  }

  public boolean isSatisfiable(AssumptionSet aset) {
    Map<String, Rational> map = tryAssumptionSet(aset);
    return map != null || redlogReturnedTrue;
  }

  /**
   * Ask Redlog to find parameter values that satisfy a formula.
   * @param phi The formula to be satisfied.
   * @param params The parameters for which to find values.
   * @return A map from parameter names to satisfying Rationals, if any are found; null otherwise.
   */
  private HashMap<String, Rational> getParameterValuesFromRedlog(String phi, Set<String> params) {
    HashMap<String, Rational> map = null;
    redlogReturnedTrue = false;
    try {
      EliminationAnswer EA = RLI.rlqea(phi);
      if (EA != null) {
        if (EA.getTruthValue() == false) {
          logger.log(Level.ALL, "Redlog says formula is unsatisfiable.");
        } else {
          if (EA.getTruthValue() == true) {
            redlogReturnedTrue = true;
          }
          EliminationHandler EH = new EliminationHandler(EA);
          map = EH.getParameterValues(params);
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to read a result from Redlog.",e);
    }
    return map;
  }

  List<Matrix> getMatricesForTransition(Transition t) {

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
    /* old method:
    TemplateBoolean antB = TemplateConjunction.conjoin((TemplateBoolean) t1, (TemplateBoolean) p);
    TemplateDisjunction antD = (TemplateDisjunction) antB.makeSDNF();
    */
    TemplateDisjunction antD = computeAntecedent((TemplateBoolean) t1, (TemplateBoolean) p);
    logger.log(Level.ALL, "\nAntecedent:\n", antD);

    // Build variable manager.
    int n = amap.size();
    int m = pur.size();
    //TODO review FormulaType.NumericType
    TemplateVariableManager vmgr = new TemplateVariableManager(FormulaType.RationalType, n, m);
    logger.log(Level.ALL, "Variable manager:\n", vmgr);

    List<Matrix> matrices = consec(antD, t2, vmgr);

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

    return matrices;
  }

  private TemplateDisjunction computeAntecedent(TemplateBoolean t1, TemplateBoolean p) {
    TemplateDisjunction antD = null;
    TemplateBoolean antB = TemplateConjunction.conjoin(t1, p);
    if (ignorePathFormulaDisjunctions) {
      // In this case we ignore any conjuncts in t1 ^ p which are disjunctive. This includes
      // both explicit disjunctions A v B, and inequations x != y (which translate
      // to x > y v x < y).
      // Method: We put antB into strong conjunctive normal form, and then delete any conjunct
      // that is not a constraint.
      logger.log(Level.ALL, "Warning: Ignoring path formula disjunctions.");
      TemplateConjunction c = (TemplateConjunction) antB.makeSCNF();
      c.deleteNonConstraints();
      Vector<TemplateBoolean> onedisjunct = new Vector<>(1);
      onedisjunct.add(c);
      antD = new TemplateDisjunction(onedisjunct);
    } else {
      // In this case, we compute the actual strong disjunctive normal form of t1 ^ p.
      antD = (TemplateDisjunction) antB.makeSDNF();
    }
    return antD;
  }

  private List<Matrix> consec(TemplateDisjunction ant, TemplateFormula t2,
      VariableManager vmgr) {
    Vector<UIFAxiom> U = new Vector<>();
    return consec(ant, t2, U, vmgr);
  }

  private List<Matrix> consec(TemplateDisjunction ant, TemplateFormula t2,
      Vector<UIFAxiom> U, VariableManager vmgr) {

    // Initialize collection of matrices.
    List<Matrix> matrices = new Vector<>();

    // Build matrices.
    // Create Vector containing the linearization of each disjunct in ant:
    Vector<Matrix> matrixAntParts = new Vector<>(ant.getNumDisjuncts());
    Vector<TemplateBoolean> disjuncts = ant.getDisjuncts();
    for (TemplateBoolean d : disjuncts) {
      // According to the formulation of Farkas's lemma in
      // Colon, Sankararanayanan, and Sipma, each matrix gets a column
      // representing "true", in the form "-1 <= 0".
      boolean prependTrue = true;
      Matrix m = (Matrix) formMat.buildMatrix(d, vmgr, paramVars, prependTrue);
      // Add the matrix to the list.
      matrixAntParts.add(m);
    }

    boolean prependTrue = false;
    Matrix Q = (Matrix) formMat.buildMatrix(t2, vmgr, paramVars, prependTrue);

    // Logically speaking, we require that each disjunct in ant taken individually imply each
    // of the constraints in Q.

    // Declare loop variables.
    Matrix concl;
    for (Matrix prem : matrixAntParts) {
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
        concl = (Matrix) formMat.buildMatrix(A.getAntecedent(), vmgr, paramVars, prependTrue);
        logger.log(Level.ALL, "UIFAxiom:\n",A);
        logger.log(Level.ALL, "Linearized premises and conclusions:\nPremises:","\n"+prem.toString(),
            "\nConclusions:","\n"+concl.toString());
        matrices.add(Matrix.augment(prem, concl));
        prem = prem.concat(formMat.buildMatrix(A.getConsequent(), vmgr, paramVars, prependTrue));
      }
      concl = Q;
      //logger.log(Level.ALL,"Linearized premises and conclusions:\nPremises:\n",prem,"\nConclusions:\n",concl);
      logger.log(Level.ALL, "Linearized premises and conclusions:\nPremises:","\n"+prem.toString(),
          "\nConclusions:","\n"+concl.toString());
      matrices.add(Matrix.augment(prem, concl));
    }

    return matrices;
  }

}
