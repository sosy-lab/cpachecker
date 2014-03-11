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
package org.sosy_lab.cpachecker.core.algorithm.testgen.predicates.formula;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;


public class FormulaEditFunctionTest {

  private FormulaEditFunction editFunction;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    //    editFunction;
  }

  @Before
  public void setUp() throws Exception {
    editFunction = new FormulaEditImpl();

  }

  @Test
  public void testNegateAtomOfFormula() throws Exception {

    StartupConfig configSet = new TestHelper().createPredicateConfig();
    FormulaManagerFactory factory =
        new FormulaManagerFactory(configSet.getConfig(), configSet.getLog(), configSet.getNotifier());
    FormulaManagerView manager = new FormulaManagerView(factory.getFormulaManager());
    BooleanFormula cnf = null;
    List<BooleanFormula> formulas = new LinkedList<>();
    int negateAt = 2;
    for (int i = 0; i < 5; i++) {
      //      formulas.add(manager.getBooleanFormulaManager().makeVariable("testVar", i));
      if (cnf == null) {
        //        cnf = manager.getBooleanFormulaManager().makeVariable("testVar", i);
        cnf = manager.createPredicateVariable("testVar" + i);
        if (i == negateAt) {
          cnf = manager.makeNot(cnf);
        }
      } else
      {
        //        BooleanFormula tmp = manager.getBooleanFormulaManager().makeVariable("testVar", i);
        BooleanFormula tmp = manager.createPredicateVariable("testVar" + i);
        if (i == negateAt) {
          tmp = manager.makeNot(tmp);
        }
        cnf = manager.makeAnd(tmp, cnf);
      }

    }
    //    cnf = manager.getBooleanFormulaManager().and(formulas);
    System.out.println(cnf.toString());
    //    splitCNF(cnf, manager);
    //    List<BooleanFormula> splitCNF = new FormulaSplitter().splitCNF2(cnf, manager);
    //    for (BooleanFormula booleanFormula : splitCNF) {
    //      System.out.println(booleanFormula);

    BooleanFormula changedFormula = editFunction.negateAtomOfFormula(cnf, 1, manager);
    System.out.println(changedFormula.toString());
    //    throw new RuntimeException("not yet implemented");
  }



}
