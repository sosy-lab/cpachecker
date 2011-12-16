/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.pcc.common;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFactory;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.YicesTheoremProver;

public class FormulaHandler {

  private Configuration config;
  private LogManager logger;
  private PathFormulaManager pfm;
  private ExtendedFormulaManager fm;
  private TheoremProver tp;

  public FormulaHandler(Configuration pConfig, LogManager pLogger, String pProverType)
      throws InvalidConfigurationException {
    config = pConfig;
    logger = pLogger;
    MathsatFormulaManager mathsatFormulaManager =
        MathsatFactory.createFormulaManager(config, logger);
    fm = new ExtendedFormulaManager(mathsatFormulaManager, config, logger);
    pfm = new PathFormulaManagerImpl(fm, config, logger);
    if (pProverType.equals("MATHSAT")) {
      tp = new MathsatTheoremProver(mathsatFormulaManager);
    } else if (pProverType.equals("YICES")) {
      tp = new YicesTheoremProver(fm, logger);
    } else {
      throw new InvalidConfigurationException("Update list of allowed solvers!");
    }
  }

  public Formula createFormula(String pString) {
    if (pString == null || pString.length() == 0) { throw new IllegalArgumentException(
        "It is not a valid formula."); }
    // get all variables and try to declare them
    String[] vars = getVariables(pString);
    if (vars == null) { return null; }
    for (int i = 0; i < vars.length; i++) {
      try {
        fm.makeVariable(vars[i]);
      } catch (IllegalArgumentException e) {
        // variable already declared do nothing
      }
    }
    try {
      Formula f = fm.parseInfix(pString);
      return f;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private String[] getVariables(String pFormula) {
    if (pFormula == null) { return null; }
    HashSet<String> foundVars = new HashSet<String>();
    //adapt abstraction such that pattern also matches at beginning and end of string
    pFormula = " " + pFormula + " ";
    //get all variables
    Pattern patVarSSAAbstraction =
        Pattern
            .compile("[\\W&&[^"+Separators.SSAIndexSeparator+"]]([_A-Za-z](\\w)*::)?[_A-Za-z](\\w)*("+Separators.SSAIndexSeparator+"(\\d)+)?[\\W&&[^"+Separators.SSAIndexSeparator+"]]");
    Matcher match = patVarSSAAbstraction.matcher(pFormula);
    String variable;
    while (match.find()) {
      variable = pFormula.substring(match.start() + 1, match.end() - 1);
      if (!foundVars.contains(variable) && !variable.equals("true") && !variable.equals("false")) {
        foundVars.add(variable);
      }
    }
    return foundVars.toArray(new String[foundVars.size()]);
  }

  public boolean isFalse(String pFormula) {
    if (pFormula == null) { return false; }
    try {
      Formula f = createFormula(pFormula);
      return isFalse(f);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public boolean isFalse(Formula pFormula) {
    if (pFormula == null) { return false; }
    if (pFormula.isFalse()) { return true; }
    tp.init();
    boolean result = tp.isUnsat(pFormula);
    tp.reset();
    return result;
  }

  @SuppressWarnings("deprecation")
  // do not call since uninstantiate is buggy
  public Formula removeIndices(Formula pFormula) {
    try {

      return fm.uninstantiate(pFormula);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public String removeIndicesStr(Formula pFormula) {
    if (pFormula == null) { return null; }
    return removeIndicesStr(pFormula.toString());
  }

  public String removeIndicesStr(String pInput) {
    if (pInput == null) { return null; }
    StringBuilder newStr = new StringBuilder();
    Pattern pat = Pattern.compile("[\\W&&[^"+Separators.SSAIndexSeparator+"]]([_A-Za-z](\\w)*::)?([_A-Za-z](\\w)*"+Separators.SSAIndexSeparator+"(\\d)+)[\\W&&[^"+Separators.SSAIndexSeparator+"]]");
    // adapt input such that pattern also matches at beginning and end
    pInput = " " + pInput + " ";
    Matcher match = pat.matcher(pInput);
    int lastIndex = 0;
    while (match.find()) {
      // add content between matches plus first identifier of match which was needed to ensure that only this variable is found
      newStr.append(pInput.substring(lastIndex, pInput.indexOf(Separators.SSAIndexSeparator, match.start())));
      // set lastIndex, also integrate last identifier of match
      lastIndex = match.end() - 1;
    }
    // add rest of string
    if (lastIndex < pInput.length()) {
      newStr.append(pInput.substring(lastIndex, pInput.length()));
    }
    return newStr.substring(1, newStr.length() - 1);
  }

  public Pair<Formula, SSAMap> addIndices(SSAMap pSSA, Formula pFormula) {
    try {
      if (pFormula == null) { return null; }
      if (pSSA == null) {
        //left abstraction -> build SSAMap
        SSAMapBuilder builder = SSAMap.emptySSAMap().builder();
        // get all variable names
        Set<String> variables = fm.extractVariables(pFormula);
        for (String var : variables) {
          if (!var.contains(Separators.SSAIndexSeparator)) {
            builder.setIndex(var, 2);
          }
        }
        // get built SSAMap
        pSSA = builder.build();
      }
      // instantiate formula with indices
      pFormula = fm.instantiate(pFormula, pSSA);
      return new Pair<Formula, SSAMap>(pFormula, pSSA);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public Pair<Formula, SSAMap> addIndices(SSAMap pSSA, String pFormula) {
    try {
      Formula f = createFormula(pFormula);
      if (f == null) { return null; }
      return addIndices(pSSA, f);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public Formula buildEdgeInvariant(String pLeft, String pOperation,
      String pRight) {
    if (pLeft == null || pLeft.length() == 0 || pOperation == null
        || pRight == null || pRight.length() == 0) { return null; }
    Formula fR, fOp, fL;
    fL = createFormula(pLeft);
    fR = createFormula(pRight);
    if (fL == null || fR == null) { return null; }
    if (pOperation.length() != 0) {
      fOp = createFormula(pOperation);
      if (fOp == null) { return null; }
    } else {
      fOp = null;
    }
    return buildEdgeInvariant(fL, fOp, fR);
  }

  public Formula buildEdgeInvariant(Formula pLeft, Formula pOperation,
      Formula pRight) {
    if (pLeft == null || pRight == null) { return null; }
    try {
      pRight = fm.makeNot(pRight);
      if (pOperation != null) {
        pLeft = fm.makeAnd(pLeft, pOperation);
      }
      return fm.makeAnd(pLeft, pRight);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public Formula buildConjunction(Formula[] pList) {
    if (pList == null || pList.length == 0) { return null; }
    Formula result = pList[0];
    try {
      for (int i = 1; i < pList.length; i++) {
        if (result == null) {
          result = pList[i];
        } else {
          if (pList[i] != null) {
            result = fm.makeAnd(result, pList[i]);
          }
        }
      }
      if (result == null) { return fm.makeTrue(); }
      return result;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public Formula buildDisjunction(Formula[] pList) {
    if (pList == null || pList.length == 0) { return null; }
    Formula result = pList[0];
    try {
      for (int i = 1; i < pList.length; i++) {
        if (result == null) {
          result = pList[i];
        } else {
          if (pList[i] != null) {
            result = fm.makeOr(result, pList[i]);
          }
        }
      }
      if (result == null) { return fm.makeTrue(); }
      return result;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public Formula buildImplication(Formula pLeft, Formula pRight) {
    if (pLeft == null || pRight == null) { return null; }
    try {
      pLeft = fm.makeNot(pLeft);
      return fm.makeOr(pLeft, pRight);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public PathFormula getTrueFormula(SSAMap pSSA) {
    if (pSSA == null) {
      return pfm.makeEmptyPathFormula();
    } else {
      return pfm.makeNewPathFormula(pfm.makeEmptyPathFormula(), pSSA);
    }
  }

  public PathFormula extendPath(PathFormula pPath, CFAEdge pEdge) {
    try {
      return pfm.makeAnd(pPath, pEdge);
    } catch (CPATransferException e) {
      logger.log(Level.SEVERE,
          "Cannot create formula representing edge operation.",
          e.getStackTrace());
      return null;
    } catch (IllegalArgumentException e1) {
      return null;
    }
  }

  public PathFormula getEdgeOperationFormula(SSAMap pSSA, CFAEdge[] edges) {
    // TODO
    return null;
  }

  public PathFormula getEdgeOperationFormula(SSAMap pSSA, CFAEdge pEdge) {
    PathFormula oldFormula, formula;
    oldFormula = pfm.makeEmptyPathFormula();
    oldFormula = pfm.makeNewPathFormula(oldFormula, pSSA);
    try {
      formula = pfm.makeAnd(oldFormula, pEdge);
    } catch (CPATransferException e) {
      logger.log(Level.SEVERE,
          "Cannot create formula representing edge operation.",
          e.getStackTrace());
      return null;
    } catch (IllegalArgumentException e) {
      return null;
    }
    return formula;
  }

  public String getEdgeOperationWithSSA(PathFormula pPredecessor, CFAEdge pEdge) {
    PathFormula oldFormula, formula;
    oldFormula = pfm.makeEmptyPathFormula(pPredecessor);
    try {
      formula = pfm.makeAnd(oldFormula, pEdge);
    } catch (CPATransferException e) {
      logger.log(Level.SEVERE,
          "Cannot create formula representing edge operation.",
          e.getStackTrace());
      return null;
    } catch (IllegalArgumentException e) {
      return null;
    }
    // check if same object due to blank edge or no operation on edge ->(no abstraction element)
    if (oldFormula == formula || (formula.getLength() == 0)) {
      if (formula.getFormula().isTrue()) {
        return "";
      } else {
        return null;
      }
    } else {
      return formula.toString();
    }
  }

  public String getEdgeOperation(CFAEdge pEdge) {
    PathFormula oldFormula, formula;
    oldFormula = pfm.makeEmptyPathFormula();
    try {
      formula = pfm.makeAnd(oldFormula, pEdge);
    } catch (CPATransferException e) {
      logger.log(Level.SEVERE,
          "Cannot create formula representing edge operation.",
          e.getStackTrace());
      return null;
    } catch (IllegalArgumentException e) {
      return null;
    }
    // check if same object due to blank edge (no abstraction element)
    if (oldFormula == formula || formula.getLength() == 0) {
      return "";
    } else {
      return formula.toString();
    }
  }

  public boolean isSameFormulaWithoutSSAIndicies(String pFormula1,
      String pFormula2) {
    if (pFormula1.equals("") && pFormula2.equals("")) { return true; }
    pFormula1 = removeIndicesStr(pFormula1);
    pFormula2 = removeIndicesStr(pFormula2);
    return pFormula1.equals(pFormula2);
  }

  public boolean isSameFormulaWithNormalizedIndices(String pFormula1,
      String pFormula2) {
    if (pFormula1.equals(pFormula2)) { return true; }
    Vector<Pair<String, Integer>> first, second;
    first = getIndicesForVariables(pFormula1);
    second = getIndicesForVariables(pFormula2);
    if (first.size() != second.size()) { return false; }
    // normalize
    int firstList;
    for (int i = 0; i < first.size(); i++) {
      if (!first.get(i).getFirst().equals(second.get(i).getFirst())) { return false; }
      firstList =
          first.get(i).getSecond().compareTo(second.get(i).getSecond());
      if (firstList != 0) {
        if (firstList < 0) {
          pFormula2 =
              replaceVariable(second.get(i).getFirst() + Separators.SSAIndexSeparator
                  + second.get(i).getSecond(), pFormula2, first.get(i)
                  .getFirst() + Separators.SSAIndexSeparator + first.get(i).getSecond());
        } else {
          pFormula1 =
              replaceVariable(first.get(i).getFirst() + Separators.SSAIndexSeparator
                  + first.get(i).getSecond(), pFormula1, second.get(i)
                  .getFirst() + Separators.SSAIndexSeparator + second.get(i).getSecond());
        }

      }
    }
    // compare (string not necessarily the same, order by be changed)
    Formula f = buildEdgeInvariant(pFormula1, "", pFormula2);
    Formula f2 = buildEdgeInvariant(pFormula2, "", pFormula1);
    if (f == null || f2 == null) { return false; }
    return isFalse(f) && isFalse(f2);
  }

  private String replaceVariable(String pOldVar, String pInput, String pNewVar) {
    if (pOldVar == null || pInput == null || pNewVar == null) { return null; }
    StringBuilder newStr = new StringBuilder();
    Pattern pat = Pattern.compile("\\W" + pOldVar + "\\W");
    // adapt input such that pattern also matches at beginning and end
    pInput = " " + pInput + " ";
    Matcher match = pat.matcher(pInput);
    int lastIndex = 0;
    while (match.find()) {
      // add content between matches plus first identifier of match which was needed to ensure that only this variable is found
      newStr.append(pInput.substring(lastIndex, match.start() + 1));
      // add replacement
      newStr.append(pNewVar);
      // set lastIndex, also integrate last identifier of match
      lastIndex = match.end() - 1;
    }
    // add rest of string
    if (lastIndex < pInput.length()) {
      newStr.append(pInput.substring(lastIndex, pInput.length()));
    }
    return newStr.substring(1, newStr.length() - 1);

  }

  private Vector<Pair<String, Integer>> getIndicesForVariables(String pFormula) {
    Vector<Pair<String, Integer>> foundVar =
        new Vector<Pair<String, Integer>>();
    HashSet<String> found = new HashSet<String>();
    //adapt abstraction such that pattern also matches at beginning and end of string
    pFormula = " " + pFormula + " ";
    //get all indices for every variable
    Pattern patVarSSAAbstraction =
        Pattern
            .compile("[\\W&&[^"+ Separators.SSAIndexSeparator +"]]([_A-Za-z](\\w)*::)?([_A-Za-z](\\w)*"+ Separators.SSAIndexSeparator +"(\\d)+)[\\W&&[^"+ Separators.SSAIndexSeparator +"]]");
    Matcher match = patVarSSAAbstraction.matcher(pFormula);
    String lastMatch, variable;
    int index;
    while (match.find()) {
      lastMatch = match.group();
      index = lastMatch.indexOf(Separators.SSAIndexSeparator);
      // extract variable name
      variable = lastMatch.substring(1, index);
      // extract SSA index
      try {
        index =
            Integer.parseInt(lastMatch.substring(index + 1,
                lastMatch.length() - 1));
      } catch (NumberFormatException e) {
        continue;
      }
      if (!found.contains(variable + Separators.SSAIndexSeparator + index)) {
        foundVar.add(new Pair<String, Integer>(variable, index));
      }
    }
    // order content
    Collections.sort(foundVar, new PairComparator());
    return foundVar;
  }

  private class PairComparator implements Comparator<Pair<String, Integer>> {

    @Override
    public int compare(Pair<String, Integer> pArg0, Pair<String, Integer> pArg1) {
      int result = pArg0.getFirst().compareTo(pArg1.getFirst());
      if (result != 0) {
        return result;
      } else {
        return pArg0.getSecond().compareTo(pArg1.getSecond());
      }
    }
  }

  private Hashtable<String, Integer> getHighestIndices(String pFormula) {
    Hashtable<String, Integer> highestIndices =
        new Hashtable<String, Integer>();
    //adapt abstraction such that pattern also matches at beginning and end of string
    pFormula = " " + pFormula + " ";
    //get highest index for every SSA variable in pAbstraction
    Pattern patVarSSAAbstraction =
        Pattern
            .compile("[\\W&&[^"+ Separators.SSAIndexSeparator +"]]([_A-Za-z](\\w)*::)?([_A-Za-z](\\w)*"+ Separators.SSAIndexSeparator +"(\\d)+)[\\W&&[^"+ Separators.SSAIndexSeparator +"]]");
    Matcher match = patVarSSAAbstraction.matcher(pFormula);
    String lastMatch, variable;
    Integer highestIndex;
    int index;
    while (match.find()) {
      lastMatch = match.group();
      index = lastMatch.indexOf(Separators.SSAIndexSeparator);
      // extract variable name
      variable = lastMatch.substring(1, index);
      // extract SSA index
      try {
        index =
            Integer.parseInt(lastMatch.substring(index + 1,
                lastMatch.length() - 1));
      } catch (NumberFormatException e) {
        return null;
      }
      // look up if SSA index greater than all indices found for this variable
      highestIndex = highestIndices.get(variable);
      if (highestIndex == null || highestIndex.intValue() < index) {
        highestIndices.put(variable, index);
      }
    }
    return highestIndices;
  }

  public boolean operationFitsToLeftAbstraction(String pAbstraction,
      String pOperation, boolean pAssume) {
    // get highest indices for variables in pAbstraction
    Hashtable<String, Integer> highestIndices = getHighestIndices(pAbstraction);
    if (highestIndices == null) { return false; }
    String intermediate;

    for (String var : highestIndices.keySet()) {
      if (pAssume) {
        // all variables are only allowed to have same indices as highest indices
        intermediate =
            pOperation.replaceAll(var +  Separators.SSAIndexSeparator  + highestIndices.get(var), "");
        if (intermediate.matches("(.)*[\\W]" + var + "[\\W](.)*")) { return false; }
      } else {
        // eliminate all variables of this kind on the left hand of the assignment
        intermediate =
            pOperation
                .replaceAll(var + Separators.SSAIndexSeparator + (highestIndices.get(var) + 1)
                    + "(\\s)*(\\))*(\\s)*=", "");
        // eliminate all remaining variables of this kind
        intermediate =
            intermediate.replaceAll(
                var + Separators.SSAIndexSeparator + highestIndices.get(var)
                    + "(\\s)*[!\\&\\(\\)\\*\\+-/<=>\\[\\]|&&[^=]]", "");
        if (intermediate.matches("(.)*[\\W]" + var + "[\\W](.)*")) { return false; }
      }
    }
    return true;
  }

  public boolean rightAbstractionFitsToOperationAndLeftAbstraction(
      String pLeftAbstraction, String pOperation, String pRightAbstraction) {
    Hashtable<String, Integer> highestVar;
    highestVar = getHighestIndices(pLeftAbstraction + " & " + pOperation);
    // adapt right abstraction such that pattern also matches at start and end
    pRightAbstraction = " " + pRightAbstraction + " ";
    for (String var : highestVar.keySet()) {
      // check if variable is contained
      if (pRightAbstraction.matches("(.)*[\\W]" + var + Separators.SSAIndexSeparator +"(.)*")) {
        // check if variable is contained with highest index
        if (!pRightAbstraction.matches("(.)*[\\W]" + var + Separators.SSAIndexSeparator
            + highestVar.get(var) + "[\\W](.)*")) { return false; }

      }
    }
    return true;
  }
  /**
   *
   * @param pFormula -
   * @param pVariables -either variables with SSA indices or without but no mixture
   * @return
   */
  /* public String normalizeIndicesInFormula(String pFormula, Set<String> pVariables){
     int pos, index;
     VariableWithIndex current;
     String prefix;
     Hashtable<String, VariableWithIndex> result = new Hashtable<String, VariableWithIndex>();
     // collect indices for variables
     for(String var:pVariables){
       pos = var.indexOf(Separators.SSAIndexSeparator);
       if(pos!=-1){
         prefix = var.substring(0,pos);
         current = result.get(prefix);
         if(current == null){
           result.put(prefix, new VariableWithIndex());
           current = result.get(prefix);
         }
         index = Integer.parseInt(var.substring(pos+1,var.length()));
         current.addIndex(index);
       }
     }
     // replace indices
     String oldVar, newVar;
     int usedIndexes;
     ArrayList<Integer> indices;
     for(String var:result.keySet()){
       indices = result.get(var).indices;
       usedIndexes = 0;
       for(Integer knownIndex:indices){
         // build old representation
         oldVar = var+Separators.SSAIndexSeparator+knownIndex;
         newVar = var+Separators.SSAIndexSeparator+usedIndexes;
         usedIndexes++;
         //TODO
       }
     }
     return null;
   }

   private class VariableWithIndex{
     private String variable;
     private ArrayList<Integer> indices;

     public void addIndex(int pIndex){
       int i=0;
       for(;i<indices.size()&& indices.get(i).intValue()<pIndex;i++){
       }
       if(i>indices.size() || indices.get(i).intValue()>pIndex){
         indices.add(i,new Integer(pIndex));
       }
     }
   }*/

}
