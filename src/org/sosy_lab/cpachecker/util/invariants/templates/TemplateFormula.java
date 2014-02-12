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
package org.sosy_lab.cpachecker.util.invariants.templates;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.invariants.interfaces.Template;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

public class TemplateFormula implements Formula, Template {

  protected String message = "";

  /**
   * Rename all variables in the formula.
   * @param prefix the letter you want to use in all the new names
   * @param vars list of all variables occurring in the formula prior to aliasing,
   *              written in the PLAIN style.
   *              Probably best to get this using the getAllVariables method.
   * Return says whether every variable in the formula was replaced or not
   */
  public void alias(AliasingMap amap) {}

  public void unalias() {}

  /**
   * Evaluate all the parameters occurring in this formula, according
   * to the given HashMap.
   * @param map map from parameter names, in the REDLOG
   * style, to the rational values you want them replaced by.
   * @return true if every parameter in this formula was assigned a
   * value by the passed map; false otherwise.
   */
  public boolean evaluate(Map<String, Rational> map) { return true; }

  public void unevaluate() {}

  /**
   * Change each variable's index to be the one that its name
   * is mapped to in the given map, or give it no index,
   * if its name is not in the domain of the map.
   * Use the getMaxIndices method to get the largest indices
   * occurring in a given formula.
   * @param indices
   */
  public void postindex(Map<String, Integer> indices) {}

  public void postindex(TemplateFormula f) {
    Map<String, Integer> indices = f.getMaxIndices();
    postindex(indices);
  }

  /**
   * Give each variable the index 1 if its name is in the
   * domain of the given map; give it no index otherwise.
   */
  public void preindex(Map<String, Integer> indices) {}

  public void preindex(TemplateFormula f) {
    Map<String, Integer> indices = f.getMaxIndices();
    preindex(indices);
  }

  /**
   * Set all indices to null.
   */
  public void unindex() {}

  /**
   * Introduce fresh variables for the UIFs in the formula. Record
   * definitions of these in pur.
   * @param pur Pass a new Purification object when starting a new
   * purification.
   * @return
   */
  public Purification purify(Purification pur) {
    return pur;
  }

  public void unpurify() {}

  public Set<TermForm> getTopLevelTermForms() { return null; }

  // FIXME: Should use TemplateVariable objects instead. See comments on next method.
  public Set<TemplateVariable> getAllVariables() {
    return new HashSet<>();
  }

//  /**
//   * This method probably shouldn't exist. It's here to return a set of
//   * variables, instead of a mere set of Strings. Probably we should either
//   * redo the getAllVariables method to return a Set of TemplateVariables,
//   * or else we should do this properly, providing a cascade of getAllVariables
//   * methods that take no arguments, and which simply return the Set of
//   * TemplateVariables in the natural way. Here, we use the existing method,
//   * and parse the returned Strings, to create new TemplateVariables.
//   * @return
//   */
//  final public Set<TemplateVariable> getAllVariables() {
//    Iterator<String> I = getAllVariables(VariableWriteMode.PLAIN).iterator();
//    Set<TemplateVariable> V = new HashSet<>();
//    String S;
//    TemplateVariable T;
//    while (I.hasNext()) {
//      S = I.next();
//      T = TemplateVariable.parse(S);
//      V.add(T);
//    }
//    return V;
//  }

  public Set<TemplateVariable> getTopLevelLHSParameters() {
    List<TemplateConstraint> cons = getConstraints();
    Set<TemplateVariable> params = new HashSet<>();
    for (TemplateConstraint c : cons) {
      params.addAll(c.getTopLevelLHSParameters());
    }
    return params;
  }

  public Set<TemplateUIF> getAllTopLevelUIFs() {
    List<TemplateConstraint> cons = getConstraints();
    Set<TemplateUIF> uifs = new HashSet<>();
    for (TemplateConstraint c : cons) {
      uifs.addAll(c.getAllTopLevelUIFs());
    }
    return uifs;
  }

  public Set<TemplateTerm> getTopLevelTerms() {
    List<TemplateConstraint> cons = getConstraints();
    Set<TemplateTerm> terms = new HashSet<>();
    for (TemplateConstraint c : cons) {
      terms.addAll(c.getTopLevelTerms());
    }
    return terms;
  }

  public Set<TemplateVariable> getAllPurificationVariables() {
    List<TemplateConstraint> cons = getConstraints();
    Set<TemplateVariable> pvs = new HashSet<>();
    for (TemplateConstraint c : cons) {
      pvs.addAll(c.getAllPurificationVariables());
    }
    return pvs;
  }

  public Set<TemplateVariable> getAllParameters() {
    return new HashSet<>();
  }

  final public Set<String> getAllParameters(VariableWriteMode vwm) {
    HashSet<String> S = new HashSet<>();
    Set<TemplateVariable> V = getAllParameters();
    for (TemplateVariable T : V) {
      S.add(T.toString(vwm));
    }
    return S;
  }

  /**
   * Get a map from variable names to their highest indices
   * appearing in this formula. Here, the names and indices are those
   * values that would be returned by the getName and getIndex methods
   * of the TemplateVariable class, i.e. precisely its S and I fields.
   */
  public HashMap<String, Integer> getMaxIndices() {
    HashMap<String, Integer> map = new HashMap<>();
    map = getMaxIndices(map);
    return map;
  }

  public HashMap<String, Integer> getMaxIndices(HashMap<String, Integer> map) {
    return map;
  }

  public String getMessage() {
    return message;
  }

  public Vector<TemplateConstraint> getConstraints() {
    return null;
  }

  public Set<TemplateTerm> getRHSTerms() { return null; }

  public TemplateVariableManager getVariableManager() {
    return new TemplateVariableManager();
  }

  /**
   * There are different implementations of the Formula and FormulaManager interfaces,
   * meaning essentially that formulas can be constructed in various "languages". If
   * you want to translate a TemplateFormula into the corresponding formula in another
   * "language", you just call this method, passing a FormulaManager for the language
   * that you want.
   */
  public Formula translate(FormulaManagerView fmgr) {
    return null;
  }

  public FormulaType<?> getFormulaType() {
    return null;
  }

  public TemplateFormula copy() {
    /**
    TemplateFormulaManager fmgr = new TemplateFormulaManager();
    TemplateTerm.setCopying(true);
    TemplateFormula c = (TemplateFormula) translate(fmgr);
    TemplateTerm.setCopying(false);
    return c;
    */
    return null;
  }

  /**
   * See TemplateFormulaManager's extractAtoms method.
   * We implement it recursively.
   */
  public List<TemplateFormula> extractAtoms(boolean sAE, boolean cO) {
    return null;
  }

  public void negate() {}

  public boolean isTrue() { return false; }

  public boolean isFalse() { return false; }

  @Override
  public String toString() {
    return toString(VariableWriteMode.PLAIN);
  }

  public String toString(VariableWriteMode vwm) {
    // For now, we return "true," in order to get usable
    // path formulas.
    return "true";
  }

}
