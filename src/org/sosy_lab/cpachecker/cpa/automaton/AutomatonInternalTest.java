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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Symbol;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CParser.ParserOptions;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonASTComparator.ASTMatcher;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.io.CharStreams;

/**
 * This class contains Tests for the AutomatonAnalysis
 */
public class AutomatonInternalTest {

  private final Configuration config;
  private final LogManager logger;
  private final CParser parser;

  private static final Path defaultSpec = Paths.get("test/config/automata/defaultSpecification.spc");

  public AutomatonInternalTest() throws InvalidConfigurationException {
    config = Configuration.builder()
        .addConverter(FileOption.class, new FileTypeConverter(Configuration.defaultConfiguration()))
        .build();
    logger = new BasicLogManager(config);

    ParserOptions options = CParser.Factory.getDefaultOptions();
    parser = CParser.Factory.getParser(config, logger, options, MachineModel.LINUX32);
  }

  @Test
  public void testScanner() throws InvalidConfigurationException, IOException {
    ComplexSymbolFactory sf1 = new ComplexSymbolFactory();
    try (InputStream input = defaultSpec.asByteSource().openStream()) {
      AutomatonScanner s = new AutomatonScanner(input, defaultSpec, config, logger, sf1);
      Symbol symb = s.next_token();
      while (symb.sym != AutomatonSym.EOF) {
        symb = s.next_token();
      }
    }
  }

  @Test
  public void testParser() throws Exception {
    ComplexSymbolFactory sf = new ComplexSymbolFactory();
    try (InputStream input = defaultSpec.asByteSource().openStream()) {
      AutomatonScanner scanner = new AutomatonScanner(input, defaultSpec, config, logger, sf);
      Symbol symbol = new AutomatonParser(scanner, sf, logger, parser).parse();
      @SuppressWarnings("unchecked")
      List<Automaton> as = (List<Automaton>) symbol.value;
      for (Automaton a : as) {
        a.writeDotFile(CharStreams.nullWriter());
      }
    }
  }

  @Test
  public void testAndOr() throws CPATransferException {
    // will always return MaybeBoolean.MAYBE
    AutomatonBoolExpr cannot = new AutomatonBoolExpr.CPAQuery("none", "none");
    Map<String, AutomatonVariable> vars = Collections.emptyMap();
    List<AbstractState> elements = Collections.emptyList();
    AutomatonExpressionArguments args = new AutomatonExpressionArguments(null, vars, elements, null, null);
    AutomatonBoolExpr ex;
    AutomatonBoolExpr myTrue= AutomatonBoolExpr.TRUE;
    AutomatonBoolExpr myFalse= AutomatonBoolExpr.FALSE;

    ex = new AutomatonBoolExpr.And(myTrue, myTrue);
    if (!ex.eval(args).getValue().equals(Boolean.TRUE)) {
      Assert.fail();
    }
    ex = new AutomatonBoolExpr.And(myTrue, myFalse);
    if (!ex.eval(args).getValue().equals(Boolean.FALSE)) {
      Assert.fail();
    }
    ex = new AutomatonBoolExpr.And(myTrue, cannot);
    if (!ex.eval(args).canNotEvaluate()) {
      Assert.fail();
    }
    ex = new AutomatonBoolExpr.And(myFalse, myTrue);
    if (!ex.eval(args).getValue().equals(Boolean.FALSE)) {
      Assert.fail();
    }
    ex = new AutomatonBoolExpr.And(myFalse, myFalse);
    if (!ex.eval(args).getValue().equals(Boolean.FALSE)) {
      Assert.fail();
    }
    ex = new AutomatonBoolExpr.And(myFalse, cannot);
    if (!ex.eval(args).getValue().equals(Boolean.FALSE)) {
      Assert.fail();
    }
    ex = new AutomatonBoolExpr.And(cannot, myTrue);
    if (!ex.eval(args).canNotEvaluate()) {
      Assert.fail();
    }
    ex = new AutomatonBoolExpr.And(cannot, myFalse);
    if (!ex.eval(args).getValue().equals(Boolean.FALSE)) {
      Assert.fail();
    }
    ex = new AutomatonBoolExpr.And(cannot, cannot);
    if (!ex.eval(args).canNotEvaluate()) {
      Assert.fail();
    }

    ex = new AutomatonBoolExpr.Or(myTrue, myTrue);
    if (!ex.eval(args).getValue().equals(Boolean.TRUE)) {
      Assert.fail();
    }
    ex = new AutomatonBoolExpr.Or(myTrue, myFalse);
    if (!ex.eval(args).getValue().equals(Boolean.TRUE)) {
      Assert.fail();
    }
    ex = new AutomatonBoolExpr.Or(myTrue, cannot);
    if (!ex.eval(args).getValue().equals(Boolean.TRUE)) {
      Assert.fail();
    }
    ex = new AutomatonBoolExpr.Or(myFalse, myTrue);
    if (!ex.eval(args).getValue().equals(Boolean.TRUE)) {
      Assert.fail();
    }
    ex = new AutomatonBoolExpr.Or(myFalse, myFalse);
    if (!ex.eval(args).getValue().equals(Boolean.FALSE)) {
      Assert.fail();
    }
    ex = new AutomatonBoolExpr.Or(myFalse, cannot);
    if (!ex.eval(args).canNotEvaluate()) {
      Assert.fail();
    }
    ex = new AutomatonBoolExpr.Or(cannot, myTrue);
    if (!ex.eval(args).getValue().equals(Boolean.TRUE)) {
      Assert.fail();
    }
    ex = new AutomatonBoolExpr.Or(cannot, myFalse);
    if (!ex.eval(args).canNotEvaluate()) {
      Assert.fail();
    }
    ex = new AutomatonBoolExpr.Or(cannot, cannot);
    if (!ex.eval(args).canNotEvaluate()) {
      Assert.fail();
    }

  }

  @Test
  public void testJokerReplacementInPattern() {
    // tests the replacement of Joker expressions in the AST comparison
    String result = AutomatonASTComparator.replaceJokersInPattern("$20 = $?");
    Assert.assertTrue(result.contains("CPAchecker_AutomatonAnalysis_JokerExpression_Num20  =  CPAchecker_AutomatonAnalysis_JokerExpression"));
    result = AutomatonASTComparator.replaceJokersInPattern("$1 = $?");
    Assert.assertTrue(result.contains("CPAchecker_AutomatonAnalysis_JokerExpression_Num1  =  CPAchecker_AutomatonAnalysis_JokerExpression"));
    result = AutomatonASTComparator.replaceJokersInPattern("$? = $?");
    Assert.assertTrue(result.contains("CPAchecker_AutomatonAnalysis_JokerExpression  =  CPAchecker_AutomatonAnalysis_JokerExpression"));
    result = AutomatonASTComparator.replaceJokersInPattern("$1 = $5");
    Assert.assertTrue(result.contains("CPAchecker_AutomatonAnalysis_JokerExpression_Num1  =  CPAchecker_AutomatonAnalysis_JokerExpression_Num5 "));
  }

  @Test
  public void testJokerReplacementInAST() throws InvalidAutomatonException, InvalidConfigurationException {
    // tests the replacement of Joker expressions in the AST comparison
    ASTMatcher patternAST = AutomatonASTComparator.generatePatternAST("$20 = $5($1, $?);", parser);
    CAstNode sourceAST  = AutomatonASTComparator.generateSourceAST("var1 = function(var2, egal);", parser);
    AutomatonExpressionArguments args = new AutomatonExpressionArguments(null, null, null, null, null);

    boolean result = patternAST.matches(sourceAST, args);
    Assert.assertTrue(result);
    Assert.assertTrue(args.getTransitionVariable(20).equals("var1"));
    Assert.assertTrue(args.getTransitionVariable(1).equals("var2"));
    Assert.assertTrue(args.getTransitionVariable(5).equals("function"));
  }

  @Test
  public void transitionVariableReplacement() throws Exception {
    AutomatonExpressionArguments args = new AutomatonExpressionArguments(null, null, null, null, logger);
    args.putTransitionVariable(1, "hi");
    args.putTransitionVariable(2, "hello");
    // actual test
    String result = args.replaceVariables("$1 == $2");
    Assert.assertTrue("hi == hello".equals(result));
    result = args.replaceVariables("$1 == $1");
    Assert.assertTrue("hi == hi".equals(result));

    logger.log(Level.WARNING, "Warning expected in the next line (concerning $5)");
    result = args.replaceVariables("$1 == $5");
    Assert.assertTrue(result == null); // $5 has not been found
    // this test should issue a log message!
  }

  @Test
  public void testASTcomparison() throws InvalidAutomatonException, InvalidConfigurationException {

   testAST("x=5;", "x= $?;", true);
   testAST("x=5;", "x= 10;", false);
   testAST("x=5;", "$? =10;", false);
   testAST("x  = 5;", "$?=$?;", true);

   testAST("a = 5;", "b    = 5;", false);

   testAST("init(a);", "init($?);", true);
   testAST("init();", "init($?);", true);
   testAST("init();", "init($1);", false);

   testAST("init(a, b);", "init($?, b);", true);
   testAST("init(a, b);", "init($?, c);", false);

   testAST("x = 5;", "x=$?", true);
   testAST("x = 5", "x=$?;", true);


   testAST("f();", "f($?);", true);
   testAST("f(x);", "f($?);", true);
   testAST("f(x, y);", "f($?);", true);

   testAST("f(x);", "f(x, $?);", false);
   testAST("f(x, y);", "f(x, $?);", true);
   testAST("f(x, y, z);", "f(x, $?);", false);

  }
  /**
   * Tests the equality of two strings as used the ASTComparison transition.
   * @param src sourcecode string
   * @param pattern string in the automaton definition (may contain $?)
   * @throws InvalidConfigurationException
   */
  public void testAST(String src, String pattern, boolean result) throws InvalidAutomatonException, InvalidConfigurationException {
    AutomatonExpressionArguments args = new AutomatonExpressionArguments(null, null, null, null, null);
    CAstNode sourceAST  = AutomatonASTComparator.generateSourceAST(src, parser);
    ASTMatcher patternAST = AutomatonASTComparator.generatePatternAST(pattern, parser);

    Assert.assertEquals(result, patternAST.matches(sourceAST, args));
  }
}
