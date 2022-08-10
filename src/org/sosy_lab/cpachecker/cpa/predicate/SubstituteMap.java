// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.sosy_lab.common.log.LogManager;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.visitors.BooleanFormulaTransformationVisitor;


/**
 * This class provides a SubstituteMap used for Predicate CPA based symbolic execution
 */
public class SubstituteMap {

  private final FormulaManagerView fmgr;
  private HashMap<Formula, Formula> map;
  private BooleanFormula f;
  private final SSAMap ssa;
  private final PathFormula pathFormula;
  private final BooleanFormulaManagerView bfmgr;
  private final LogManager logger;



  public SubstituteMap(BooleanFormula pF, SSAMap pSSAMap, PathFormula pPathFormula, FormulaManagerView pFmgr, LogManager pLogger) {
    f = pF;
    ssa = pSSAMap;
    pathFormula = pPathFormula;
    fmgr = pFmgr;
    logger = pLogger;

    bfmgr = fmgr.getBooleanFormulaManager();
    map = new HashMap<Formula, Formula>();
  }

  /**
   * Execute the syntactic substitution on the boolean formula f of the SubstituteMap
   *
   * A substitution map is constructed from f, containing the most up-to-date variable assignments
   * Variable occurences in f are then replaced.
   *
   * @param removeOutdatedSSA experimental! remove all subformulas from f which do have an SSA index
   *                          but the SSA index is not in the SSAMap
   * @return boolean formula where syntactic substitution has taken place
   */
  public BooleanFormula syntacticSubstitution(boolean removeOutdatedSSA){
    map = buildMap();
    if (!map.isEmpty()) {
      SubstituteAssumptionTransformationVisitor
          stAssume = new SubstituteAssumptionTransformationVisitor(fmgr.manager, map);
      f = bfmgr.transformRecursively(f, stAssume);
      logger.log(Level.ALL, "After Assumption substituion", f);

      SubstituteAssignmentTransformationVisitor stAssign;
      stAssign =
          new SubstituteAssignmentTransformationVisitor(fmgr.manager, map);
      f = bfmgr.transformRecursively(f, stAssign);
      logger.log(Level.ALL, "After Assignment substituion", f, "\n");

      // filter out formulas, which contain as SSA index, but the SSA index is not the most recent one, as indicated in the SSAMap ssa
       if (removeOutdatedSSA) {
        f = removeOutdatedSSA();
        logger.log(Level.ALL, "After removing formulas with outdated SSA index ", f, "\n");
      }
      BooleanFormula substitionFormula = bfmgr.makeBoolean(true);
      for (Formula key : map.keySet()) {
        if (formulaInSsaMap(key)) {
          substitionFormula = fmgr.makeAnd(substitionFormula, fmgr.assignment(key, map.get(key)));
        }
      }
      f = fmgr.makeAnd(f, substitionFormula);
      logger.log(Level.ALL, "After adding formulas from substitution map ", f, "\n");
    }
    return f;
  }

  /**
   * Experimential! Remove outdated subformulas.
   *
   * Remove all subformulas from f which do have an SSA index but the SSA index is not in the SSAMap, i.e. it is not the newest.
   * May have interesting effects on programs with call-by-value.
   * @return boolean formula where
   */
  private BooleanFormula removeOutdatedSSA(){
    return bfmgr.transformRecursively(f, new BooleanFormulaTransformationVisitor(fmgr.manager) {

      // very inefficient, optimise this: probably visitAtom instead
      // right now, every subformula is handled by itself and additionally as part of a conjunction
      // watch out how this plays out when disjunctions are present in the formula, e.g. introduced by modula operator
      @Override
      public BooleanFormula visitAnd(List<BooleanFormula> processedOperands) {
//          logger.log(Level.FINE, "Filtering before ", processedOperands, "\n");
        List<BooleanFormula> res = new ArrayList<>();
        for (BooleanFormula formula : processedOperands) {
          boolean skip_f = true;
          for (String varAtIndex : fmgr.extractVariableNames(formula)) {
            Pair<String, OptionalInt> stringOptionalIntPair =
                FormulaManagerView.parseName(varAtIndex);
            assert stringOptionalIntPair.getFirst() != null;

            if (!stringOptionalIntPair.getFirst().isEmpty()
                && !stringOptionalIntPair.getSecond().isEmpty()) { // we have a variable with an SSA Index
              if (ssa.containsVariable(stringOptionalIntPair.getFirst()) && // the variable is in the ssamap
                  (ssa.getIndex(stringOptionalIntPair.getFirst()) == stringOptionalIntPair.getSecond().getAsInt())) { // ssa index from ssamap and variable are the same
                skip_f = false;
                break;
              }
            }
          }
          if (skip_f){
            res.add(bfmgr.makeTrue());
          } else {
            res.add(formula);
          }
        }
        return super.visitAnd(res);
      }
    });
  }

  private HashMap<Formula, Formula> buildMap(){
    SubstituteVisitor stvisitorF = new SubstituteVisitor(fmgr.manager);
    bfmgr.visitRecursively(f, stvisitorF);
    HashMap<Formula, Formula> substituteMap = stvisitorF.fmap;
    logger.log(Level.ALL, "Initial substituion map", substituteMap);
    logger.log(Level.FINEST, "Updating substitution map with pathFormula");
    // for example
    // let the last abstaction state be x = 0;
    // let the new pathFormula be x=5;
    // we want to make sure x=5 is the entry for x in the map
    SubstituteVisitor stvisitorPF = new SubstituteVisitor(fmgr.manager);
    bfmgr.visitRecursively(pathFormula.getFormula(), stvisitorPF);
    HashMap<Formula, Formula> substituteMapPF = stvisitorPF.fmap;
    substituteMap.putAll(substituteMapPF);
    logger.log(Level.ALL, "Updated substituion map with pathFormula: ", substituteMap);
    logger.log(Level.FINEST, "Simplifying substitution map by internal replacements");
    substituteMap = simplifySubstituteMap(substituteMap);
    logger.log(Level.ALL, "Updated substituion map with simplifications", substituteMap);
    return substituteMap;
  }

  /**
   * Simplify substitution map by replacing its values with entries from the substitution map
   * wherever possible
   *
   * @param pMap Substitution Map in form of a HashMap
   * @return Simplified substitution Map in form of a HashMap
   */
  private HashMap<Formula, Formula> simplifySubstituteMap(HashMap<Formula, Formula> pMap){
    boolean changed = true;
    HashMap<Formula, Formula> m = new HashMap<>(pMap);
    while(changed){
      logger.log(Level.ALL, "Simplifying Map: ", m, "\n");
      HashMap<Formula, Formula> mapOld = new HashMap<>(m);
      HashMap<Formula, Formula> mapNew = new HashMap<>();
      for (Formula key : m.keySet()) {
        HashMap<Formula, Formula> localMap = new HashMap<>(m);
        localMap.remove(key);
        if (!localMap.isEmpty()){
          mapNew.put(key, fmgr.manager.substitute(m.get(key), localMap));
        } else {
          mapNew.put(key, m.get(key));
        }
      }
      changed = !mapNew.equals(mapOld); // last changed here
      m = mapNew;
    }
    return m;
  }

  /**
   * Checks if a variable along with its index is in the SSAMap
   *
   * @param pF Formula consisting of only one variable in the form `name@index`
   * @return true if the variable along with its SSA index is in the SSAMap
   *         false otherwise
   */
  private boolean formulaInSsaMap(Formula pF){
    Map<String, Formula> vars = fmgr.extractVariables(pF);
    if (vars.size()!=1) {
      return false;
//      throw new IllegalArgumentException("Error checking if variable index in SSAMAP: " + f.toString() +
//          "\nNot exactly one variable in f");
    }
    Pair<String, OptionalInt> stringOptionalIntPair = FormulaManagerView.parseName(vars.keySet().iterator().next());
    assert stringOptionalIntPair.getFirst() != null;
    if (stringOptionalIntPair.getFirst().isEmpty() || stringOptionalIntPair.getSecond().isEmpty()) {
      return false;
//      throw new IllegalArgumentException("Error checking if variable index in SSAMAP: " + f.toString());
    }
    return !ssa.containsVariable(stringOptionalIntPair.getFirst()) || (
        ssa.getIndex(stringOptionalIntPair.getFirst()) == stringOptionalIntPair.getSecond()
            .getAsInt());
  }

}
