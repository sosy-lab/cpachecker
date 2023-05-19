// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import com.google.common.base.Splitter;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class ExternModelLoader {

  private final CtoFormulaConverter conv;
  private final BooleanFormulaManagerView bfmgr;
  private final FormulaManagerView fmgr;

  public ExternModelLoader(
      CtoFormulaConverter pConv, BooleanFormulaManagerView pBfmgr, FormulaManagerView pFmgr) {
    conv = pConv;
    bfmgr = pBfmgr;
    fmgr = pFmgr;
  }

  public BooleanFormula handleExternModelFunction(List<CExpression> parameters, SSAMapBuilder ssa) {
    assert !parameters.isEmpty() : "No external model given!";
    // the parameter comes in C syntax (with ")
    String filename = parameters.get(0).toASTString().replace("\"", "");
    Path modelFile = Path.of(filename);
    return loadExternalFormula(modelFile, ssa);
  }

  /**
   * Loads a formula from an external dimacs file and returns it as BooleanFormula object. Each
   * variable in the dimacs file will be associated with a program variable if a corresponding (name
   * equality) variable is known. Otherwise we use internal SMT variable to represent the dimacs
   * variable and do not introduce a program variable. Might lead to problems when the program
   * variable is introduced afterwards.
   *
   * @param pModelFile File with the dimacs model.
   * @return BooleanFormula
   */
  private BooleanFormula loadExternalFormula(Path pModelFile, SSAMapBuilder ssa) {
    Path fileName = pModelFile.getFileName();
    if (fileName == null || !fileName.toString().endsWith(".dimacs")) {
      throw new UnsupportedOperationException("Sorry, we can only load dimacs models.");
    }
    try (BufferedReader br = Files.newBufferedReader(pModelFile, StandardCharsets.UTF_8)) {
      List<String> predicates = new ArrayList<>(10000);
      // var ids in dimacs files start with 1, so we want the first var at position 1
      predicates.add("RheinDummyVar");
      BooleanFormula externalModel = bfmgr.makeTrue();
      Formula zero = fmgr.makeNumber(FormulaType.getBitvectorTypeWithSize(32), 0);

      String line = "";
      while ((line = br.readLine()) != null) {
        if (line.startsWith("c ")) {
          // comment line, here the vars are declared
          // c 8 LOGO_SGI_CLUT224_m
          // c 80255$ _X31351_m
          // starting with id1
          List<String> parts = Splitter.on(' ').splitToList(line);
          int varID = Integer.parseInt(parts.get(1).replace("$", ""));
          assert predicates.size() == varID : "messed up the dimacs parsing!";
          predicates.add(parts.get(2));
        } else if (line.startsWith("p ")) {
          // p cnf 80258 388816
          // 80258 vars
          // 388816 cnf constraints
          List<String> parts = Splitter.on(' ').splitToList(line);
          // +1 because of the dummy var
          assert predicates.size() == Integer.parseInt(parts.get(2)) + 1
              : "did not get all dimcas variables?";
        } else if (!line.trim().isEmpty()) {
          // -17552 -11882 1489 48905 0
          // constraints
          BooleanFormula constraint = bfmgr.makeFalse();
          for (String elementStr : Splitter.on(' ').split(line)) {
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
                Formula formulaVar =
                    fmgr.makeVariable(
                        conv.getFormulaTypeFromCType(ssa.getType(predName)), predName, ssaIndex);
                if (elem > 0) {
                  constraintPart =
                      fmgr.makeNot(fmgr.makeEqual(formulaVar, zero)); // C semantics (x) <=> (x!=0)
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
              constraint = bfmgr.or(constraint, constraintPart);
            }
          }
          externalModel = bfmgr.and(externalModel, constraint);
        }
      } // end of while
      return externalModel;
    } catch (IOException e) {
      throw new RuntimeException(e); // TODO: find the proper exception
    }
  }
}
