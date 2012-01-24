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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Symbol;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonASTComparator.ASTMatcher;

/**
 * This class contains Tests for the AutomatonAnalysis
 */
class AutomatonInternalTests {

  /**
   * Runs some tests for the automatonAnalysis
   * @param args
   */
  public static void main(String[] args) {
    /*
    AutomatonBoolExpr ex = AutomatonBoolExpr.TRUE;
    System.out.println(ex.eval(null));
    */
    try {
      File f = new File("test/config/automata/defaultSpecification.spc");
      //File f = new File("test/config/automata/TestAutomaton.txt");

      Configuration emptyConfig = Configuration.defaultConfiguration();
      LogManager logger = new LogManager(emptyConfig);
      ComplexSymbolFactory sf1 = new ComplexSymbolFactory();
      AutomatonScanner s = new AutomatonScanner(new FileInputStream(f), f, emptyConfig, logger, sf1);
      Symbol symb = s.next_token();
      while (symb.sym != AutomatonSym.EOF) {
        System.out.println(symb);
        symb = s.next_token();
      }
      System.out.println(s.next_token());

/*
      SymbolFactory sf = new ComplexSymbolFactory();
      //change back if you have problems:
      //SymbolFactory sf = new DefaultSymbolFactory();
     Symbol symbol = new AutomatonParser(new AutomatonScanner(new java.io.FileInputStream(f), sf),sf).parse();
     Automaton a = (Automaton) symbol.value;
     a.writeDotFile(System.out);
*/

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    //testExpressionEvaluator();

    //testASTcomparison();

    testAndOr();
  }
  private static void testAndOr() {
    // will always return MaybeBoolean.MAYBE
    AutomatonBoolExpr cannot = new AutomatonBoolExpr.CPAQuery("none", "none");
    Map<String, AutomatonVariable> vars = Collections.emptyMap();
    List<AbstractElement> elements = Collections.emptyList();
    AutomatonExpressionArguments args = new AutomatonExpressionArguments(vars, elements, null, null);
    AutomatonBoolExpr ex;
    boolean ok = true;
    AutomatonBoolExpr myTrue= AutomatonBoolExpr.TRUE;
    AutomatonBoolExpr myFalse= AutomatonBoolExpr.FALSE;

    ex = new AutomatonBoolExpr.And(myTrue, myTrue); if (!ex.eval(args).getValue().equals(Boolean.TRUE)) ok = false;
    ex = new AutomatonBoolExpr.And(myTrue, myFalse); if (!ex.eval(args).getValue().equals(Boolean.FALSE)) ok = false;
    ex = new AutomatonBoolExpr.And(myTrue, cannot); if (!ex.eval(args).canNotEvaluate()) ok = false;
    ex = new AutomatonBoolExpr.And(myFalse, myTrue); if (!ex.eval(args).getValue().equals(Boolean.FALSE)) ok = false;
    ex = new AutomatonBoolExpr.And(myFalse, myFalse); if (!ex.eval(args).getValue().equals(Boolean.FALSE)) ok = false;
    ex = new AutomatonBoolExpr.And(myFalse, cannot); if (!ex.eval(args).getValue().equals(Boolean.FALSE)) ok = false;
    ex = new AutomatonBoolExpr.And(cannot, myTrue); if (!ex.eval(args).canNotEvaluate()) ok = false;
    ex = new AutomatonBoolExpr.And(cannot, myFalse); if (!ex.eval(args).getValue().equals(Boolean.FALSE)) ok = false;
    ex = new AutomatonBoolExpr.And(cannot, cannot); if (!ex.eval(args).canNotEvaluate()) ok = false;

    ex = new AutomatonBoolExpr.Or(myTrue, myTrue); if (!ex.eval(args).getValue().equals(Boolean.TRUE)) ok = false;
    ex = new AutomatonBoolExpr.Or(myTrue, myFalse); if (!ex.eval(args).getValue().equals(Boolean.TRUE)) ok = false;
    ex = new AutomatonBoolExpr.Or(myTrue, cannot); if (!ex.eval(args).getValue().equals(Boolean.TRUE)) ok = false;
    ex = new AutomatonBoolExpr.Or(myFalse, myTrue); if (!ex.eval(args).getValue().equals(Boolean.TRUE)) ok = false;
    ex = new AutomatonBoolExpr.Or(myFalse, myFalse); if (!ex.eval(args).getValue().equals(Boolean.FALSE)) ok = false;
    ex = new AutomatonBoolExpr.Or(myFalse, cannot); if (!ex.eval(args).canNotEvaluate()) ok = false;
    ex = new AutomatonBoolExpr.Or(cannot, myTrue); if (!ex.eval(args).getValue().equals(Boolean.TRUE)) ok = false;
    ex = new AutomatonBoolExpr.Or(cannot, myFalse); if (!ex.eval(args).canNotEvaluate()) ok = false;
    ex = new AutomatonBoolExpr.Or(cannot, cannot); if (!ex.eval(args).canNotEvaluate()) ok = false;

    if (!ok) {
      System.out.println("AndOr test has failed!");
    } else {
      System.out.println("AndOr Test was OK");
    }
  }
  @SuppressWarnings("unused")
  private static void testExpressionEvaluator() {
    /*
    Map<String, AutomatonVariable> map = new HashMap<String, AutomatonVariable>();
    AutomatonIntExpr AccessA = new AutomatonIntExpr.VarAccess("a");
    AutomatonIntExpr AccessB = new AutomatonIntExpr.VarAccess("b");

    AutomatonActionExpr storeA = new AutomatonActionExpr.Assignment("a",
        new AutomatonIntExpr.Constant(5));

    AutomatonActionExpr storeB = new AutomatonActionExpr.Assignment("b",
        new AutomatonIntExpr.Plus(AccessA, new AutomatonIntExpr.Constant(2)));

    AutomatonBoolExpr bool = new AutomatonBoolExpr.EqTest(
        new AutomatonIntExpr.Plus(new AutomatonIntExpr.Constant(2), AccessA)
        , AccessB
        );

    storeA.execute(map);
    storeB.execute(map);

    System.out.println("Expression Evaluation result: " + bool.eval(map));
    */
  }
  @SuppressWarnings("unused")
  private static void testASTcomparison() {

   testAST("x=5;", "x= $?;");
   testAST("x=5;", "x= 10;");
   //AutomatonASTComparator.printAST("x=10;");
   testAST("x=5;", "$? =10;");
   testAST("x  = 5;", "$?=$?;");

   testAST("a = 5;", "b    = 5;");

   testAST("init(a);", "init($?);");
   testAST("init();", "init($?);");

   testAST("init(a, b);", "init($?, b);");
   testAST("init(a, b);", "init($?, c);");


   testAST("init();", "init();;"); // two ';' lead to not-equal
   testAST("x = 5;", "x=$?");
   testAST("x = 5", "x=$?;");
   testAST("x = 5;;", "x=$?");


   testAST("f();", "f($?);");
   testAST("f(x);", "f($?);");
   testAST("f(x, y);", "f($?);");

   testAST("f(x);", "f(x, $?);");
   testAST("f(x, y);", "f(x, $?);");
   testAST("f(x, y, z);", "f(x, $?);");

  }
  /**
   * Tests the equality of two strings as used the ASTComparison transition.
   * @param src sourcecode string
   * @param pattern string in the automaton definition (may contain $?)
   */
  static void testAST(String src, String pattern) {
    System.out.print("AST Test of ");
    System.out.print(src);
    System.out.print(" and ");
    System.out.print(pattern);
    System.out.print(" returns ");
    AutomatonExpressionArguments args = new AutomatonExpressionArguments(null, null, null, null);
    try {
      IASTNode sourceAST  = AutomatonASTComparator.generateSourceAST(src);
      ASTMatcher patternAST = AutomatonASTComparator.generatePatternAST(pattern);
      System.out.print(patternAST.matches(sourceAST, args));
    } catch (InvalidAutomatonException e) {
      System.out.println(e.getMessage());
    }
    System.out.println();
  }



}
