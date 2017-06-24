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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;
import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Symbol;
import org.junit.Test;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CParser.ParserOptions;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonASTComparator.ASTMatcher;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * This class contains Tests for the AutomatonAnalysis
 */
public class AutomatonInternalTest {

  private final LogManager logger;
  private final CParser parser;

  private static final Path defaultSpecPath = Paths.get("test/config/automata/defaultSpecification.spc");
  private static final CharSource defaultSpec =
      MoreFiles.asCharSource(defaultSpecPath, StandardCharsets.UTF_8);

  public AutomatonInternalTest() {
    logger = LogManager.createTestLogManager();

    ParserOptions options = CParser.Factory.getDefaultOptions();
    parser = CParser.Factory.getParser(logger, options, MachineModel.LINUX32);
  }

  @Test
  public void testScanner() throws IOException {
    ComplexSymbolFactory sf1 = new ComplexSymbolFactory();
    try (Reader input = defaultSpec.openBufferedStream()) {
      AutomatonScanner s = new AutomatonScanner(input, defaultSpecPath, logger, sf1);
      Symbol symb = s.next_token();
      while (symb.sym != AutomatonSym.EOF) {
        symb = s.next_token();
      }
    }
  }

  @Test
  public void testParser() throws Exception {
    ComplexSymbolFactory sf = new ComplexSymbolFactory();
    try (Reader input = defaultSpec.openBufferedStream()) {
      AutomatonScanner scanner = new AutomatonScanner(input, defaultSpecPath, logger, sf);
      Symbol symbol =
          new AutomatonParser(
                  scanner, sf, logger, parser, MachineModel.LINUX32, CProgramScope.empty())
              .parse();
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
    assertThat(ex.eval(args).getValue()).isEqualTo(true);

    ex = new AutomatonBoolExpr.And(myTrue, myFalse);
    assertThat(ex.eval(args).getValue()).isEqualTo(false);

    ex = new AutomatonBoolExpr.And(myTrue, cannot);
    assertThat(ex.eval(args).canNotEvaluate()).isTrue();

    ex = new AutomatonBoolExpr.And(myFalse, myTrue);
    assertThat(ex.eval(args).getValue()).isEqualTo(false);

    ex = new AutomatonBoolExpr.And(myFalse, myFalse);
    assertThat(ex.eval(args).getValue()).isEqualTo(false);

    ex = new AutomatonBoolExpr.And(myFalse, cannot);
    assertThat(ex.eval(args).getValue()).isEqualTo(false);

    ex = new AutomatonBoolExpr.And(cannot, myTrue);
    assertThat(ex.eval(args).canNotEvaluate()).isTrue();

    ex = new AutomatonBoolExpr.And(cannot, myFalse);
    assertThat(ex.eval(args).getValue()).isEqualTo(false);

    ex = new AutomatonBoolExpr.And(cannot, cannot);
    assertThat(ex.eval(args).canNotEvaluate()).isTrue();

    ex = new AutomatonBoolExpr.Or(myTrue, myTrue);
    assertThat(ex.eval(args).getValue()).isEqualTo(true);

    ex = new AutomatonBoolExpr.Or(myTrue, myFalse);
    assertThat(ex.eval(args).getValue()).isEqualTo(true);

    ex = new AutomatonBoolExpr.Or(myTrue, cannot);
    assertThat(ex.eval(args).getValue()).isEqualTo(true);

    ex = new AutomatonBoolExpr.Or(myFalse, myTrue);
    assertThat(ex.eval(args).getValue()).isEqualTo(true);

    ex = new AutomatonBoolExpr.Or(myFalse, myFalse);
    assertThat(ex.eval(args).getValue()).isEqualTo(false);

    ex = new AutomatonBoolExpr.Or(myFalse, cannot);
    assertThat(ex.eval(args).canNotEvaluate()).isTrue();

    ex = new AutomatonBoolExpr.Or(cannot, myTrue);
    assertThat(ex.eval(args).getValue()).isEqualTo(true);

    ex = new AutomatonBoolExpr.Or(cannot, myFalse);
    assertThat(ex.eval(args).canNotEvaluate()).isTrue();

    ex = new AutomatonBoolExpr.Or(cannot, cannot);
    assertThat(ex.eval(args).canNotEvaluate()).isTrue();
  }

  @Test
  public void testJokerReplacementInPattern() {
    // tests the replacement of Joker expressions in the AST comparison
    String result = AutomatonASTComparator.replaceJokersInPattern("$20 = $?");
    assertThat(result).contains("CPAchecker_AutomatonAnalysis_JokerExpression_Num20  =  CPAchecker_AutomatonAnalysis_JokerExpression");
    result = AutomatonASTComparator.replaceJokersInPattern("$1 = $?");
    assertThat(result).contains("CPAchecker_AutomatonAnalysis_JokerExpression_Num1  =  CPAchecker_AutomatonAnalysis_JokerExpression");
    result = AutomatonASTComparator.replaceJokersInPattern("$? = $?");
    assertThat(result).contains("CPAchecker_AutomatonAnalysis_JokerExpression_Wildcard0  =  CPAchecker_AutomatonAnalysis_JokerExpression_Wildcard1");
    result = AutomatonASTComparator.replaceJokersInPattern("$1 = $5");
    assertThat(result).contains("CPAchecker_AutomatonAnalysis_JokerExpression_Num1  =  CPAchecker_AutomatonAnalysis_JokerExpression_Num5 ");
  }

  @Test
  public void testJokerReplacementInAST() {
    // tests the replacement of Joker expressions in the AST comparison
    final String pattern = "$20 = $5($1, $?);";
    final String source = "var1 = function(var2, egal);";

    assert_().about(astMatcher).that(pattern).matches(source).withVariableValue(20, "var1");
    assert_().about(astMatcher).that(pattern).matches(source).withVariableValue(1, "var2");
    assert_().about(astMatcher).that(pattern).matches(source).withVariableValue(5, "function");
  }

  @Test
  public void transitionVariableReplacement() {
    LogManager mockLogger = mock(LogManager.class);
    AutomatonExpressionArguments args = new AutomatonExpressionArguments(null, null, null, null, mockLogger);
    args.putTransitionVariable(1, "hi");
    args.putTransitionVariable(2, "hello");
    // actual test
    String result = args.replaceVariables("$1 == $2");
    assertThat(result).isEqualTo("hi == hello");
    result = args.replaceVariables("$1 == $1");
    assertThat(result).isEqualTo("hi == hi");

    result = args.replaceVariables("$1 == $5");
    assertThat(result).isNull(); // $5 has not been found
    // this test should issue a log message!
    verify(mockLogger).log(eq(Level.WARNING), (Object[]) any());
  }

  @Test
  public void testASTcomparison() {
    assert_().about(astMatcher).that("x= $?;").matches("x=5;");
    assert_().about(astMatcher).that("x= 10;").doesNotMatch("x=5;");
    assert_().about(astMatcher).that("$? =10;").doesNotMatch("x=5;");
    assert_().about(astMatcher).that("$?=$?;").matches("x  = 5;");

    assert_().about(astMatcher).that("b    = 5;").doesNotMatch("a = 5;");

    assert_().about(astMatcher).that("init($?);").matches("init(a);");
    assert_().about(astMatcher).that("init($?);").matches("init();");
    assert_().about(astMatcher).that("init($1);").doesNotMatch("init();");

    assert_().about(astMatcher).that("init($?, b);").matches("init(a, b);");
    assert_().about(astMatcher).that("init($?, c);").doesNotMatch("init(a, b);");

    assert_().about(astMatcher).that("x=$?").matches("x = 5;");
    assert_().about(astMatcher).that("x=$?;").matches("x = 5");

    assert_().about(astMatcher).that("f($?);").matches("f();");
    assert_().about(astMatcher).that("f($?);").matches("f(x);");
    assert_().about(astMatcher).that("f($?);").matches("f(x, y);");

    // Too-large number in a joker makes it be ignored.
    assert_().about(astMatcher).that("$12345678901;").doesNotMatch("x");
  }

  @Test
  public void testAstMatcherFunctionParameters() {
    assert_().about(astMatcher).that("f();").matches("f();");
    assert_().about(astMatcher).that("f();").doesNotMatch("f(x);");
    assert_().about(astMatcher).that("f();").doesNotMatch("f(x, y);");

    assert_().about(astMatcher).that("f($1);").doesNotMatch("f();");
    assert_().about(astMatcher).that("f($1);").matches("f(x);").withVariableValue(1, "x");
    assert_().about(astMatcher).that("f($1);").doesNotMatch("f(x, y);");

    assert_().about(astMatcher).that("f($?);").matches("f();");
    assert_().about(astMatcher).that("f($?);").matches("f(x);");
    assert_().about(astMatcher).that("f($?);").matches("f(x, y);");

    assert_().about(astMatcher).that("f(x, $?);").doesNotMatch("f(x);");
    assert_().about(astMatcher).that("f(x, $?);").matches("f(x, y);");
    assert_().about(astMatcher).that("f(x, $?);").doesNotMatch("f(x, y, z);");
  }

  @Test
  public void testAstMatcherFunctionCall() {
    assert_().about(astMatcher).that("$?();").matches("f();");
    assert_().about(astMatcher).that("$?();").doesNotMatch("x = f();");
    assert_().about(astMatcher).that("$1();").matches("f();").withVariableValue(1, "f");

    assert_().about(astMatcher).that("x = $?();").doesNotMatch("f();");
    assert_().about(astMatcher).that("x = $?();").matches("x = f();");
    assert_().about(astMatcher).that("x = $1();").matches("x = f();").withVariableValue(1, "f");

    assert_().about(astMatcher).that("$?($?);").matches("f();");
    assert_().about(astMatcher).that("$?($?);").matches("f(y);");
    assert_().about(astMatcher).that("$?($?);").matches("f(y, z);");
    assert_().about(astMatcher).that("$?($?);").doesNotMatch("x = f();");

    assert_().about(astMatcher).that("$? = $1($?);").matches("x = f();");
    assert_().about(astMatcher).that("$? = $1($?);").matches("x = f(y);");
    assert_().about(astMatcher).that("$? = $1($?);").matches("x = f(y, z);");
    assert_().about(astMatcher).that("$? = $1($?);").doesNotMatch("f();");
  }

  private final SubjectFactory<ASTMatcherSubject, String> astMatcher =
      new SubjectFactory<ASTMatcherSubject, String>() {
        @Override
        public ASTMatcherSubject getSubject(FailureStrategy pFs, String pThat) {
          return new ASTMatcherSubject(pFs, pThat).named("AST matcher pattern");
        }
      };

  /**
   * {@link Subject} subclass for testing ASTMatchers with Truth
   * (allows to use assert_().about(astMatcher).that("ast pattern").matches(...)).
   */
  @SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
  private class ASTMatcherSubject extends Subject<ASTMatcherSubject, String> {

    private final AutomatonExpressionArguments args = new AutomatonExpressionArguments(null, null, null, null, null);

    public ASTMatcherSubject(FailureStrategy pFailureStrategy, String pPattern) {
      super(pFailureStrategy, pPattern);
    }

    private boolean matches0(String src) throws InvalidAutomatonException {
      CAstNode sourceAST;
      ASTMatcher matcher;
      sourceAST = CParserUtils.parseSingleStatement(src, parser, CProgramScope.empty());
      matcher = AutomatonASTComparator.generatePatternAST(actual(), parser, CProgramScope.empty());

      return matcher.matches(sourceAST, args);
    }

    public Matches matches(final String src) {
      boolean matches;
      try {
        matches = matches0(src);
      } catch (InvalidAutomatonException e) {
        failureStrategy.fail("Cannot parse source or pattern", e);
        return new Matches() {
              @Override
              public void withVariableValue(int pVar, String pValue) {
                ASTMatcherSubject.this.fail("Cannot test value of variable with failed parsing.");
              }
            };
      }

      if (!matches) {
        fail("matches", src);
        return new Matches() {
            @Override
            public void withVariableValue(int pVar, String pValue) {
              ASTMatcherSubject.this.fail("Cannot test value of variable if pattern does not match.");
            }
          };
      }
      return new Matches() {
            @Override
            public void withVariableValue(int pVar, String pExpectedValue) {
              if (!args.getTransitionVariables().containsKey(pVar)) {
                ASTMatcherSubject.this.failWithBadResults(
                    "has variable", pVar, "has variables", args.getTransitionVariables().keySet());
              }
              final String actualValue = args.getTransitionVariable(pVar);
              if (!actualValue.equals(pExpectedValue)) {
                ASTMatcherSubject.this.failWithBadResults(
                    "matches <" + src + "> with value of variable $" + pVar + " being",
                    pExpectedValue, "has value", actualValue);
              }
            }
          };
    }

    public void doesNotMatch(String src) {
      try {
        if (matches0(src)) {
          fail("does not match", src);
        }
      } catch (InvalidAutomatonException e) {
        failureStrategy.fail("Cannot parse source or pattern", e);
      }
    }
  }

  private static interface Matches {
    void withVariableValue(int var, String value);
  }
}
