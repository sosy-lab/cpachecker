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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;


public class ExternModelLoader {

  private final CtoFormulaTypeHandler typeHandler;
  private final BooleanFormulaManagerView bfmgr;
  private final FormulaManagerView fmgr;

  public ExternModelLoader(CtoFormulaTypeHandler pTypeHandler, BooleanFormulaManagerView pBfmgr,
      FormulaManagerView pFmgr) {
    typeHandler = pTypeHandler;
    bfmgr = pBfmgr;
    fmgr = pFmgr;
  }

  public BooleanFormula handleExternModelFunction(CFunctionCallExpression fexp, List<CExpression> parameters, SSAMapBuilder ssa) {
    assert (parameters.size()>0): "No external model given!";
    // the parameter comes in C syntax (with ")
    String filename = parameters.get(0).toASTString().replaceAll("\"", "");
    Path modelFile = Paths.get(filename);
    return loadExternalFormula(modelFile, ssa);
  }

  /**
   * Loads a formula from an external dimacs file and returns it as BooleanFormula object.
   * Each variable in the dimacs file will be associated with a program variable if a corresponding (name equality) variable is known.
   * Otherwise we use internal SMT variable to represent the dimacs variable and do not introduce a program variable.
   * Might lead to problems when the program variable is introduced afterwards.
   * @param pModelFile File with the dimacs model.
   * @return BooleanFormula
   */
  private BooleanFormula loadExternalFormula(Path pModelFile, SSAMapBuilder ssa) {
    if (! pModelFile.getName().endsWith(".dimacs")) {
      throw new UnsupportedOperationException("Sorry, we can only load dimacs models.");
    }
    try (BufferedReader br =  pModelFile.asCharSource(StandardCharsets.UTF_8).openBufferedStream()) {
       ArrayList<String> predicates = new ArrayList<>(10000);
       //var ids in dimacs files start with 1, so we want the first var at position 1
       predicates.add("RheinDummyVar");
       BooleanFormula externalModel = bfmgr.makeBoolean(true);
       Formula zero = fmgr.makeNumber(FormulaType.BitvectorType.getBitvectorType(32), 0);

       String line = "";
       while ((line = br.readLine()) != null) {
         if (line.startsWith("c ")) {
           // comment line, here the vars are declared
           // c 8 LOGO_SGI_CLUT224_m
           // c 80255$ _X31351_m
           // starting with id1
           String[] parts = line.split(" ");
           int varID = Integer.parseInt(parts[1].replace("$", ""));
           assert predicates.size() == varID : "messed up the dimacs parsing!";
           predicates.add(parts[2]);
         } else if (line.startsWith("p ")) {
           //p cnf 80258 388816
           // 80258 vars
           // 388816 cnf constraints
           String[] parts = line.split(" ");
           // +1 because of the dummy var
           assert predicates.size()==Integer.parseInt(parts[2])+1: "did not get all dimcas variables?";
         } else if (line.trim().length()>0) {
           //-17552 -11882 1489 48905 0
           // constraints
           BooleanFormula constraint = bfmgr.makeBoolean(false);
           String[] parts = line.split(" ");
           for (String elementStr : parts) {
             if (!elementStr.equals("0") && !elementStr.isEmpty()) {
               int elem = Integer.parseInt(elementStr);
               String predName = "";
               if (elem > 0) {
                predName = predicates.get(elem);
              } else {
                predName = predicates.get(-elem);
              }
               int ssaIndex = ssa.getIndex(predName);
               BooleanFormula constraintPart = null;
               if (ssaIndex != -1) {
                 // this variable was already declared in the program
                 Formula formulaVar = fmgr.makeVariable(typeHandler.getFormulaTypeFromCType(ssa.getType(predName)), predName, ssaIndex);
                 if (elem > 0) {
                  constraintPart = fmgr.makeNot(fmgr.makeEqual(formulaVar, zero)); // C semantics (x) <=> (x!=0)
                } else {
                  constraintPart = fmgr.makeEqual(formulaVar, zero);
                }
               } else {
                 // var was not declared in the program
                 // get a new SMT-var for it (i have to pass a ssa index, choosing 1)
                 BooleanFormula formulaVar = fmgr.makeVariable(FormulaType.BooleanType, predName, 1);
                 if (elem > 0) {
                  constraintPart = formulaVar;
                } else {
                  constraintPart = bfmgr.not(formulaVar);
                }
               }
               if (constraint == null) {
                constraint = constraintPart;
              } else {
                constraint = bfmgr.or(constraint, constraintPart);
              }
             }
           }
           if (externalModel == null) {
            externalModel = constraint;
          } else {
            externalModel = bfmgr.and(externalModel, constraint);
          }
         }
       }// end of while
      return externalModel;
    } catch (IOException e) {
      throw new RuntimeException(e); //TODO: find the proper exception
    }
  }
}
