/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.observeranalysis;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Symbol;
import java_cup.runtime.SymbolFactory;

import org.eclipse.cdt.core.dom.ast.IASTNode;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.observeranalysis.ObserverBoolExpr.MaybeBoolean;

/**
 * This class contains Tests for the ObserverAnalysis
 */
class ObserverInternalTests {

  /**
   * Runs some tests for the observerAnalysis
   * @param args
   */
  public static void main(String[] args) {



    ObserverBoolExpr ex = new ObserverBoolExpr.True();
    System.out.println(ex.eval(null));
    try {
      File f = new File("test/programs/observerAutomata/LockingAutomatonAstComp.txt");

      /*
      SymbolFactory sf1 = new ComplexSymbolFactory();
      Scanner s = new Scanner(new FileInputStream(f), sf1);
      Symbol symb = s.next_token();
      while (symb.sym != sym.EOF) {
        System.out.println(symb);
        symb = s.next_token();
      }
      System.out.println(s.next_token());
      */

      SymbolFactory sf = new ComplexSymbolFactory();
      //change back if you have problems:
      //SymbolFactory sf = new DefaultSymbolFactory();
     Symbol symbol = new ObserverParser(new ObserverScanner(new java.io.FileInputStream(f), sf),sf).parse();
     ObserverAutomaton a = (ObserverAutomaton) symbol.value;
     a.writeDotFile(System.out);


    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    testExpressionEvaluator();
    testASTcomparison();

    testAndOr();
  }
  private static void testAndOr() {
    // will always return MaybeBoolean.MAYBE
    ObserverBoolExpr maybe = new ObserverBoolExpr.CPAQuery("none", "none");
    Map<String, ObserverVariable> vars = Collections.emptyMap();
    List<AbstractElement> elements = Collections.emptyList();
    ObserverExpressionArguments args = new ObserverExpressionArguments(vars, elements, null, null);
    ObserverBoolExpr ex;
    boolean ok = true;
    ObserverBoolExpr myTrue= new ObserverBoolExpr.True();
    ObserverBoolExpr myFalse= new ObserverBoolExpr.False();

    ex = new ObserverBoolExpr.And(myTrue, myTrue); if (ex.eval(args) != MaybeBoolean.TRUE) ok = false;
    ex = new ObserverBoolExpr.And(myTrue, myFalse); if (ex.eval(args) != MaybeBoolean.FALSE) ok = false;
    ex = new ObserverBoolExpr.And(myTrue, maybe); if (ex.eval(args) != MaybeBoolean.MAYBE) ok = false;
    ex = new ObserverBoolExpr.And(myFalse, myTrue); if (ex.eval(args) != MaybeBoolean.FALSE) ok = false;
    ex = new ObserverBoolExpr.And(myFalse, myFalse); if (ex.eval(args) != MaybeBoolean.FALSE) ok = false;
    ex = new ObserverBoolExpr.And(myFalse, maybe); if (ex.eval(args) != MaybeBoolean.FALSE) ok = false;
    ex = new ObserverBoolExpr.And(maybe, myTrue); if (ex.eval(args) != MaybeBoolean.MAYBE) ok = false;
    ex = new ObserverBoolExpr.And(maybe, myFalse); if (ex.eval(args) != MaybeBoolean.FALSE) ok = false;
    ex = new ObserverBoolExpr.And(maybe, maybe); if (ex.eval(args) != MaybeBoolean.MAYBE) ok = false;

    ex = new ObserverBoolExpr.Or(myTrue, myTrue); if (ex.eval(args) != MaybeBoolean.TRUE) ok = false;
    ex = new ObserverBoolExpr.Or(myTrue, myFalse); if (ex.eval(args) != MaybeBoolean.TRUE) ok = false;
    ex = new ObserverBoolExpr.Or(myTrue, maybe); if (ex.eval(args) != MaybeBoolean.TRUE) ok = false;
    ex = new ObserverBoolExpr.Or(myFalse, myTrue); if (ex.eval(args) != MaybeBoolean.TRUE) ok = false;
    ex = new ObserverBoolExpr.Or(myFalse, myFalse); if (ex.eval(args) != MaybeBoolean.FALSE) ok = false;
    ex = new ObserverBoolExpr.Or(myFalse, maybe); if (ex.eval(args) != MaybeBoolean.MAYBE) ok = false;
    ex = new ObserverBoolExpr.Or(maybe, myTrue); if (ex.eval(args) != MaybeBoolean.TRUE) ok = false;
    ex = new ObserverBoolExpr.Or(maybe, myFalse); if (ex.eval(args) != MaybeBoolean.MAYBE) ok = false;
    ex = new ObserverBoolExpr.Or(maybe, maybe); if (ex.eval(args) != MaybeBoolean.MAYBE) ok = false;

    if (!ok) {
      System.out.println("AndOr test has failed!");
    } else {
      System.out.println("AndOr Test was OK");
    }
  }
  private static void testExpressionEvaluator() {
    /*
    Map<String, ObserverVariable> map = new HashMap<String, ObserverVariable>();
    ObserverIntExpr AccessA = new ObserverIntExpr.VarAccess("a");
    ObserverIntExpr AccessB = new ObserverIntExpr.VarAccess("b");

    ObserverActionExpr storeA = new ObserverActionExpr.Assignment("a",
        new ObserverIntExpr.Constant(5));

    ObserverActionExpr storeB = new ObserverActionExpr.Assignment("b",
        new ObserverIntExpr.Plus(AccessA, new ObserverIntExpr.Constant(2)));

    ObserverBoolExpr bool = new ObserverBoolExpr.EqTest(
        new ObserverIntExpr.Plus(new ObserverIntExpr.Constant(2), AccessA)
        , AccessB
        );

    storeA.execute(map);
    storeB.execute(map);

    System.out.println("Expression Evaluation result: " + bool.eval(map));
    */
  }
  private static void testASTcomparison() {

   testAST("x=5;", "x= $?;");
   testAST("x=5;", "x= 10;");
   //ObserverASTComparator.printAST("x=10;");
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
   * @param pattern string in the observer automaton definition (may contain $?)
   */
  static void testAST(String src, String pattern) {
    System.out.print("AST Test of ");
    System.out.print(src);
    System.out.print(" and ");
    System.out.print(pattern);
    System.out.print(" returns ");
    ObserverExpressionArguments args = new ObserverExpressionArguments(null, null, null, null);
    try {
      IASTNode sourceAST  = ObserverASTComparator.generateSourceAST(src);
      IASTNode patternAST = ObserverASTComparator.generatePatternAST(pattern);
      System.out.print(ObserverASTComparator.compareASTs(sourceAST, patternAST, args));
    } catch (InvalidAutomatonException e) {
      System.out.println(e.getMessage());
    }
    System.out.println();
  }



}
