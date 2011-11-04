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

import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFactory;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;

public class FormulaHandler {

  private Configuration      config;
  private LogManager         logger;
  private PathFormulaManager pfm;
  private FormulaManager     fm;

  public FormulaHandler(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    config = pConfig;
    logger = pLogger;
    MathsatFormulaManager mathsatFormulaManager =
        MathsatFactory.createFormulaManager(config, logger);
    fm = new ExtendedFormulaManager(mathsatFormulaManager, config, logger);
    pfm = new PathFormulaManagerImpl(fm, config, logger);
  }

  public boolean isFalse(String pFormula) {
    if (pFormula == null) { return false; }
    try {
      Formula f = fm.parse(pFormula);
      return f.isFalse();
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public Formula buildEdgeInvariant(String pLeft, String pOperation,
      String pRight) {
    if (pLeft == null || pLeft.length() == 0 || pOperation == null
        || pRight == null || pRight.length() == 0) { return null; }
    try {
      Formula fR, fOp, fL;
      fL = fm.parse(pLeft);
      fR = fm.parse(pRight);
      fR = fm.makeNot(fR);
      if (pOperation.length() != 0) {
        fOp = fm.parse(pOperation);
        fL = fm.makeAnd(fL, fOp);
      }
      return fm.makeAnd(fL, fR);
    } catch (IllegalArgumentException e) {
      return null;
    }
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
    }
    // check if same object due to blank edge (no abstraction element)
    if (oldFormula == formula || formula.getLength() == 0) {
      return "";
    } else {
      return formula.toString();
    }
  }

  @SuppressWarnings({ "deprecation", "finally" })
  public boolean isSameFormulaWithoutSSAIndicies(String pFormula1,
      String pFormula2) {
    if (pFormula1.equals("") && pFormula2.equals("")) { return true; }
    try {
      Formula formula1 = fm.parse(pFormula1);
      Formula formula2 = fm.parse(pFormula2);
      try {
        formula1 = fm.uninstantiate(formula1);
      } finally {
        try {
          formula2 = fm.uninstantiate(formula2);
        } finally {
          return formula1.equals(formula2);
        }
      }
    } catch (IllegalArgumentException e) {
      return false;
    }
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
       pos = var.indexOf("@");
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
         oldVar = var+"@"+knownIndex;
         newVar = var+"@"+usedIndexes;
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
