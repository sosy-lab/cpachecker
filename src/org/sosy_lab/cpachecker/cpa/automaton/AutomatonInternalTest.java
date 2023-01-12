// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;
import com.google.common.io.MoreFiles;
import com.google.common.truth.Fact;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Subject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Symbol;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CParser.ParserOptions;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonASTComparator.ASTMatcher;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariable.AutomatonIntVariable;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariable.AutomatonSetVariable;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

/** This class contains Tests for the AutomatonAnalysis */
public class AutomatonInternalTest {

  private final LogManager logger;
  private final CParser parser;

  private static final Path defaultSpecPath =
      Path.of("test/config/automata/defaultSpecification.spc");
  private static final CharSource defaultSpec =
      MoreFiles.asCharSource(defaultSpecPath, StandardCharsets.UTF_8);

  public AutomatonInternalTest() {
    logger = LogManager.createTestLogManager();

    ParserOptions options = CParser.Factory.getDefaultOptions();
    parser =
        CParser.Factory.getParser(
            logger, options, MachineModel.LINUX32, ShutdownNotifier.createDummy());
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
    Map<String, AutomatonVariable> vars = ImmutableMap.of();
    List<AbstractState> elements = ImmutableList.of();
    AutomatonExpressionArguments args =
        new AutomatonExpressionArguments(null, vars, elements, null, null);
    AutomatonBoolExpr ex;
    AutomatonBoolExpr myTrue = AutomatonBoolExpr.TRUE;
    AutomatonBoolExpr myFalse = AutomatonBoolExpr.FALSE;

    ex = new AutomatonBoolExpr.And(myTrue, myTrue);
    assertThat(ex.eval(args).getValue()).isTrue();

    ex = new AutomatonBoolExpr.And(myTrue, myFalse);
    assertThat(ex.eval(args).getValue()).isFalse();

    ex = new AutomatonBoolExpr.And(myTrue, cannot);
    assertThat(ex.eval(args).canNotEvaluate()).isTrue();

    ex = new AutomatonBoolExpr.And(myFalse, myTrue);
    assertThat(ex.eval(args).getValue()).isFalse();

    ex = new AutomatonBoolExpr.And(myFalse, myFalse);
    assertThat(ex.eval(args).getValue()).isFalse();

    ex = new AutomatonBoolExpr.And(myFalse, cannot);
    assertThat(ex.eval(args).getValue()).isFalse();

    ex = new AutomatonBoolExpr.And(cannot, myTrue);
    assertThat(ex.eval(args).canNotEvaluate()).isTrue();

    ex = new AutomatonBoolExpr.And(cannot, myFalse);
    assertThat(ex.eval(args).getValue()).isFalse();

    ex = new AutomatonBoolExpr.And(cannot, cannot);
    assertThat(ex.eval(args).canNotEvaluate()).isTrue();

    ex = new AutomatonBoolExpr.Or(myTrue, myTrue);
    assertThat(ex.eval(args).getValue()).isTrue();

    ex = new AutomatonBoolExpr.Or(myTrue, myFalse);
    assertThat(ex.eval(args).getValue()).isTrue();

    ex = new AutomatonBoolExpr.Or(myTrue, cannot);
    assertThat(ex.eval(args).getValue()).isTrue();

    ex = new AutomatonBoolExpr.Or(myFalse, myTrue);
    assertThat(ex.eval(args).getValue()).isTrue();

    ex = new AutomatonBoolExpr.Or(myFalse, myFalse);
    assertThat(ex.eval(args).getValue()).isFalse();

    ex = new AutomatonBoolExpr.Or(myFalse, cannot);
    assertThat(ex.eval(args).canNotEvaluate()).isTrue();

    ex = new AutomatonBoolExpr.Or(cannot, myTrue);
    assertThat(ex.eval(args).getValue()).isTrue();

    ex = new AutomatonBoolExpr.Or(cannot, myFalse);
    assertThat(ex.eval(args).canNotEvaluate()).isTrue();

    ex = new AutomatonBoolExpr.Or(cannot, cannot);
    assertThat(ex.eval(args).canNotEvaluate()).isTrue();
  }

  @Test
  public void testJokerReplacementInPattern() {
    // tests the replacement of Joker expressions in the AST comparison
    String result = AutomatonASTComparator.replaceJokersInPattern("$20 = $?");
    assertThat(result)
        .contains(
            "CPAchecker_AutomatonAnalysis_JokerExpression_Num20  = "
                + " CPAchecker_AutomatonAnalysis_JokerExpression");
    result = AutomatonASTComparator.replaceJokersInPattern("$1 = $?");
    assertThat(result)
        .contains(
            "CPAchecker_AutomatonAnalysis_JokerExpression_Num1  = "
                + " CPAchecker_AutomatonAnalysis_JokerExpression");
    result = AutomatonASTComparator.replaceJokersInPattern("$? = $?");
    assertThat(result)
        .contains(
            "CPAchecker_AutomatonAnalysis_JokerExpression_Wildcard0  = "
                + " CPAchecker_AutomatonAnalysis_JokerExpression_Wildcard1");
    result = AutomatonASTComparator.replaceJokersInPattern("$1 = $5");
    assertThat(result)
        .contains(
            "CPAchecker_AutomatonAnalysis_JokerExpression_Num1  = "
                + " CPAchecker_AutomatonAnalysis_JokerExpression_Num5 ");
  }

  @Test
  public void testJokerReplacementInAST() throws InterruptedException {
    // tests the replacement of Joker expressions in the AST comparison
    final String pattern = "$20 = $5($1, $?);";
    final String source = "var1 = function(var2, egal);";

    assert_()
        .about(ASTMatcherSubject::new)
        .that(pattern)
        .matches(source)
        .andVariable(20)
        .isEqualTo("var1");
    assert_()
        .about(ASTMatcherSubject::new)
        .that(pattern)
        .matches(source)
        .andVariable(1)
        .isEqualTo("var2");
    assert_()
        .about(ASTMatcherSubject::new)
        .that(pattern)
        .matches(source)
        .andVariable(5)
        .isEqualTo("function");
  }

  @Test
  public void transitionVariableReplacement() {
    LogManager mockLogger = mock(LogManager.class);
    AutomatonExpressionArguments args =
        new AutomatonExpressionArguments(null, null, null, null, mockLogger);
    args.putTransitionVariable(1, TestDataTools.makeVariable("hi", CNumericTypes.INT));
    args.putTransitionVariable(2, TestDataTools.makeVariable("hello", CNumericTypes.INT));
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
  public void automataVariableReplacement() {
    LogManager mockLogger = mock(LogManager.class);
    Map<String, AutomatonVariable> automatonVariables = new HashMap<>();
    AutomatonVariable intVar1 = AutomatonVariable.createAutomatonVariable("int", "intVar1");
    AutomatonVariable intVar2 = AutomatonVariable.createAutomatonVariable("Integer", "intVar2");
    ((AutomatonIntVariable) intVar2).setValue(10);
    AutomatonVariable setVar1 = AutomatonVariable.createAutomatonVariable("Set", "setVar1", "int");
    AutomatonVariable setVar2 =
        AutomatonVariable.createAutomatonVariable("SET", "setVar2", "string", "elem1, elem2");

    automatonVariables.putAll(
        ImmutableMap.of(
            intVar1.getName(),
            intVar1,
            intVar2.getName(),
            intVar2,
            setVar1.getName(),
            setVar1,
            setVar2.getName(),
            setVar2));

    AutomatonExpressionArguments args =
        new AutomatonExpressionArguments(null, automatonVariables, null, null, mockLogger);
    args.putTransitionVariable(1, TestDataTools.makeVariable("programVar", CNumericTypes.INT));

    // actual test
    String result = args.replaceVariables("$1 == $$intVar1");
    assertThat(result).isEqualTo("programVar == 0");

    result = args.replaceVariables("$$intVar2 == 0");
    assertThat(result).isEqualTo("10 == 0");

    ((AutomatonIntVariable) intVar1).setValue(5);
    result = args.replaceVariables("$1 + $$intVar1");
    assertThat(result).isEqualTo("programVar + 5");

    result = args.replaceVariables("$$setVar1");
    assertThat(result).isEqualTo("0");

    result = args.replaceVariables("$$setVar2");
    assertThat(result).isEqualTo("1");

    ((AutomatonSetVariable<?>) setVar1).add(1);
    result = args.replaceVariables("$$setVar1");
    assertThat(result).isEqualTo("1");

    ((AutomatonSetVariable<?>) setVar2).remove("elem1");
    ((AutomatonSetVariable<?>) setVar2).remove("elem2");
    result = args.replaceVariables("$$setVar2");
    assertThat(result).isEqualTo("0");

    result = args.replaceVariables("$1 == $$intVar3");
    assertThat(result).isNull(); // automaton variable intVar3 does not exist
    // this test should issue a log message!
    verify(mockLogger).log(eq(Level.WARNING), (Object[]) any());
  }

  @Test
  public void testASTcomparison() throws InterruptedException {
    assert_().about(ASTMatcherSubject::new).that("x= $?;").matches("x=5;");
    assert_().about(ASTMatcherSubject::new).that("x= 10;").doesNotMatch("x=5;");
    assert_().about(ASTMatcherSubject::new).that("$? =10;").doesNotMatch("x=5;");
    assert_().about(ASTMatcherSubject::new).that("$?=$?;").matches("x  = 5;");

    assert_().about(ASTMatcherSubject::new).that("b    = 5;").doesNotMatch("a = 5;");

    assert_().about(ASTMatcherSubject::new).that("init($?);").matches("init(a);");
    assert_().about(ASTMatcherSubject::new).that("init($?);").matches("init();");
    assert_().about(ASTMatcherSubject::new).that("init($1);").doesNotMatch("init();");

    assert_().about(ASTMatcherSubject::new).that("init($?, b);").matches("init(a, b);");
    assert_().about(ASTMatcherSubject::new).that("init($?, c);").doesNotMatch("init(a, b);");

    assert_().about(ASTMatcherSubject::new).that("x=$?").matches("x = 5;");
    assert_().about(ASTMatcherSubject::new).that("x=$?;").matches("x = 5");

    assert_().about(ASTMatcherSubject::new).that("f($?);").matches("f();");
    assert_().about(ASTMatcherSubject::new).that("f($?);").matches("f(x);");
    assert_().about(ASTMatcherSubject::new).that("f($?);").matches("f(x, y);");

    // Too-large number in a joker makes it be ignored.
    assert_().about(ASTMatcherSubject::new).that("$12345678901;").doesNotMatch("x");
  }

  @Test
  public void testAstMatcherParameters() throws InterruptedException {
    assert_().about(ASTMatcherSubject::new).that("f();").matches("f();");
    assert_().about(ASTMatcherSubject::new).that("f();").doesNotMatch("f(x);");
    assert_().about(ASTMatcherSubject::new).that("f();").doesNotMatch("f(x, y);");

    assert_().about(ASTMatcherSubject::new).that("f($1);").doesNotMatch("f();");
    assert_()
        .about(ASTMatcherSubject::new)
        .that("f($1);")
        .matches("f(x);")
        .andVariable(1)
        .isEqualTo("x");
    assert_().about(ASTMatcherSubject::new).that("f($1);").doesNotMatch("f(x, y);");

    assert_().about(ASTMatcherSubject::new).that("f($?);").matches("f();");
    assert_().about(ASTMatcherSubject::new).that("f($?);").matches("f(x);");
    assert_().about(ASTMatcherSubject::new).that("f($?);").matches("f(x, y);");

    assert_().about(ASTMatcherSubject::new).that("f(x, $?);").doesNotMatch("f(x);");
    assert_().about(ASTMatcherSubject::new).that("f(x, $?);").matches("f(x, y);");
    assert_().about(ASTMatcherSubject::new).that("f(x, $?);").doesNotMatch("f(x, y, z);");
  }

  @Test
  public void testAstMatcherFunctionCall() throws InterruptedException {
    assert_().about(ASTMatcherSubject::new).that("$?();").matches("f();");
    assert_().about(ASTMatcherSubject::new).that("$?();").doesNotMatch("x = f();");
    assert_()
        .about(ASTMatcherSubject::new)
        .that("$1();")
        .matches("f();")
        .andVariable(1)
        .isEqualTo("f");

    assert_().about(ASTMatcherSubject::new).that("x = $?();").doesNotMatch("f();");
    assert_().about(ASTMatcherSubject::new).that("x = $?();").matches("x = f();");
    assert_()
        .about(ASTMatcherSubject::new)
        .that("x = $1();")
        .matches("x = f();")
        .andVariable(1)
        .isEqualTo("f");

    assert_().about(ASTMatcherSubject::new).that("$?($?);").matches("f();");
    assert_().about(ASTMatcherSubject::new).that("$?($?);").matches("f(y);");
    assert_().about(ASTMatcherSubject::new).that("$?($?);").matches("f(y, z);");
    assert_().about(ASTMatcherSubject::new).that("$?($?);").doesNotMatch("x = f();");

    assert_().about(ASTMatcherSubject::new).that("$? = $1($?);").matches("x = f();");
    assert_().about(ASTMatcherSubject::new).that("$? = $1($?);").matches("x = f(y);");
    assert_().about(ASTMatcherSubject::new).that("$? = $1($?);").matches("x = f(y, z);");
    assert_().about(ASTMatcherSubject::new).that("$? = $1($?);").doesNotMatch("f();");
  }

  /**
   * {@link Subject} subclass for testing ASTMatchers with Truth (allows to use
   * assert_().about(astMatcher).that("ast pattern").matches(...)).
   */
  @SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
  private class ASTMatcherSubject extends Subject {

    private final String pattern;
    private final AutomatonExpressionArguments args =
        new AutomatonExpressionArguments(null, null, null, null, null);

    public ASTMatcherSubject(FailureMetadata pMetadata, String pPattern) {
      super(pMetadata, pPattern);
      pattern = pPattern;
    }

    private boolean matches0(String src) throws InvalidAutomatonException, InterruptedException {
      CAstNode sourceAST;
      ASTMatcher matcher;
      try {
        sourceAST = CParserUtils.parseSingleStatement(src, parser, CProgramScope.empty());
      } catch (InvalidAutomatonException e) {
        throw new RuntimeException("Cannot parse source code for test", e);
      }
      matcher = AutomatonASTComparator.generatePatternAST(pattern, parser, CProgramScope.empty());

      return matcher.matches(sourceAST, args);
    }

    public Matches matches(final String src) throws InterruptedException {
      boolean matches;
      try {
        matches = matches0(src);
      } catch (InvalidAutomatonException e) {
        failWithoutActual(
            Fact.simpleFact("expected to be a valid pattern"),
            Fact.fact("but was", pattern),
            Fact.fact("which cannot be parsed", e));
        return new Matches() {
          @Override
          public StringSubject andVariable(int pVar) {
            // Cannot test value of variable with failed parsing.
            return ASTMatcherSubject.this.ignoreCheck().that("");
          }
        };
      }

      if (!matches) {
        failWithActual(Fact.fact("expected to match", src));
        return new Matches() {
          @Override
          public StringSubject andVariable(int pVar) {
            // Cannot test value of variable if pattern does not match.
            return ASTMatcherSubject.this.ignoreCheck().that("");
          }
        };
      }
      return new Matches() {
        @Override
        public StringSubject andVariable(int pVar) {
          check("getTransitionVariables()").that(args.getTransitionVariables()).containsKey(pVar);
          return ASTMatcherSubject.this
              .check("transition variable $%s", pVar)
              .that(args.getTransitionVariable(pVar).toASTString());
        }
      };
    }

    public void doesNotMatch(String src) throws InterruptedException {
      try {
        if (matches0(src)) {
          if (args.getTransitionVariables().isEmpty()) {
            failWithActual(Fact.fact("expected to not match", src));
          } else {
            failWithoutActual(
                Fact.fact("expected to not match", src),
                Fact.fact("but was", pattern),
                Fact.fact("with transition variables", args.getTransitionVariables()));
          }
        }
      } catch (InvalidAutomatonException e) {
        failWithoutActual(
            Fact.simpleFact("expected to be a valid pattern"),
            Fact.fact("but was", pattern),
            Fact.fact("which cannot be parsed", e));
      }
    }
  }

  private interface Matches {
    StringSubject andVariable(int var);
  }
}
