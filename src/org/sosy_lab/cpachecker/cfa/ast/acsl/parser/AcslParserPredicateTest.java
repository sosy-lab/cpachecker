// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Objects;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAstNode;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate.AcslBinaryPredicateExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm.AcslBinaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLabel;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslMemoryLocationSetEmpty;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslMemoryLocationSetTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslProgramLabel;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslResultTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate.AcslUnaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm.AcslUnaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslValidPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslParser.AcslParseException;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;

public class AcslParserPredicateTest {

  private CSimpleType basicInt() {
    return new CSimpleType(
        false, false, CBasicType.INT, false, false, true, false, false, false, false);
  }

  private CProgramScope getCProgramScope() {
    String currentFunctionName = "f";

    CProgramScope scope =
        CProgramScope.mutableCoy(CProgramScope.empty().withFunctionScope(currentFunctionName));
    for (String var : ImmutableList.of("x", "y", "z")) {
      scope.registerDeclaration(
          new CVariableDeclaration(
              FileLocation.DUMMY,
              true,
              CStorageClass.AUTO,
              basicInt(),
              var,
              var,
              var,
              null /* No initializer, we only want it for testing */));
    }
    scope.registerDeclaration(
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            new CFunctionType(basicInt(), ImmutableList.of(), false),
            currentFunctionName,
            ImmutableList.of(),
            ImmutableSet.of()));

    return scope;
  }

  private AcslScope getAcslScope() {
    AcslScope scope = AcslScope.empty();

    return scope;
  }

  private void testPredicateParsing(String input, AcslAstNode output) throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslScope acslScope = getAcslScope();

    AcslAstNode parsed = AcslParser.parsePredicate(input, cProgramScope, acslScope);
    assert parsed.equals(output) : "Parsed object does not match expected object";
  }

  @Test
  public void parseConstantTruePredicate() throws AcslParseException {
    AcslPredicate output = new AcslBooleanLiteralPredicate(FileLocation.DUMMY, true);
    String input = "\\true";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseConstantFalsePredicate() throws AcslParseException {
    AcslPredicate output = new AcslBooleanLiteralPredicate(FileLocation.DUMMY, false);
    String input = "\\false";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimplePredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslPredicate output =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslIdTerm(
                FileLocation.DUMMY,
                new AcslCVariableDeclaration(
                    (CVariableDeclaration)
                        Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.TEN),
            AcslBinaryTermExpressionOperator.EQUALS);
    String input = "x == 10";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimpleNegatedPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslPredicate output =
        new AcslUnaryPredicate(
            FileLocation.DUMMY,
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.TEN),
                AcslBinaryTermExpressionOperator.EQUALS),
            AcslUnaryExpressionOperator.NEGATION);
    String input = "!(x == 10)";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimplePredicateWithOperations() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslPredicate output =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslBinaryTerm(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.INTEGER,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ONE),
                AcslBinaryTermOperator.PLUS),
            new AcslBinaryTerm(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.INTEGER,
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.valueOf(10)),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.valueOf(2)),
                AcslBinaryTermOperator.MINUS),
            AcslBinaryTermExpressionOperator.EQUALS);
    String input = "x + 1 == 10 - 2";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimplePredicateWithUnaryTerm() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslPredicate output =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslUnaryTerm(
                FileLocation.DUMMY,
                new AcslCType(Objects.requireNonNull(cProgramScope.lookupVariable("x")).getType()),
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                AcslUnaryTermOperator.MINUS),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.valueOf(5)),
            AcslBinaryTermExpressionOperator.EQUALS);
    String input = "-x == 5";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimpleBinaryPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslPredicate output =
        new AcslBinaryPredicate(
            FileLocation.DUMMY,
            // first operator
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.TEN),
                AcslBinaryTermExpressionOperator.LESS_THAN),
            // second operator
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
                AcslBinaryTermExpressionOperator.GREATER_THAN),
            AcslBinaryPredicateExpressionOperator.AND);
    String input = "x < 10 && x > 0";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimplePredicateWithOld() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslPredicate output =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslOldTerm(
                FileLocation.DUMMY,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x"))))),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
            AcslBinaryTermExpressionOperator.LESS_THAN);
    String input = "\\old(x) < 0";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimpleOldPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslPredicate output =
        new AcslOldPredicate(
            FileLocation.DUMMY,
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.valueOf(-1)),
                AcslBinaryTermExpressionOperator.LESS_EQUAL));
    String input = "\\old(x <= -1)";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimpleResultPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslPredicate output =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslResultTerm(
                FileLocation.DUMMY,
                new AcslCType(
                    Objects.requireNonNull(cProgramScope.lookupFunction("f"))
                        .getType()
                        .getReturnType())),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ONE),
            AcslBinaryTermExpressionOperator.GREATER_THAN);
    String input = "\\result > 1";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimpleAtPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslPredicate output =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslAtTerm(
                FileLocation.DUMMY,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                AcslBuiltinLabel.PRE),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ONE),
            AcslBinaryTermExpressionOperator.GREATER_THAN);
    String input = "\\at(x, Pre) > 1";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimpleImplicitTermToPredicateConversion() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslPredicate output =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslBinaryTerm(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.INTEGER,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ONE),
                AcslBinaryTermOperator.PLUS),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
            AcslBinaryTermExpressionOperator.EQUALS);
    String input = "x + 1";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimpleAtPredicateWithArbitraryLabel() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslPredicate output =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslAtTerm(
                FileLocation.DUMMY,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                new AcslProgramLabel("a", FileLocation.DUMMY)),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ONE),
            AcslBinaryTermExpressionOperator.GREATER_THAN);
    String input = "\\at(x, a) > 1";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimpleTernaryOperatorWithPredicates() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslPredicate output =
        new AcslTernaryPredicate(
            FileLocation.DUMMY,
            // Condition
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
                AcslBinaryTermExpressionOperator.EQUALS),
            // If true operator
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("y")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
                AcslBinaryTermExpressionOperator.GREATER_THAN),
            // If false operator
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("z")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
                AcslBinaryTermExpressionOperator.LESS_THAN));
    String input = "x == 0 ? y > 0 : z < 0";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimpleTermTernaryOperatorWithTerms() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslPredicate output =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            // First operand
            new AcslTernaryTerm(
                FileLocation.DUMMY,
                // Condition
                new AcslBinaryTermPredicate(
                    FileLocation.DUMMY,
                    new AcslIdTerm(
                        FileLocation.DUMMY,
                        new AcslCVariableDeclaration(
                            (CVariableDeclaration)
                                Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                    new AcslIntegerLiteralTerm(
                        FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
                    AcslBinaryTermExpressionOperator.EQUALS),
                // If true operator
                new AcslBinaryTerm(
                    FileLocation.DUMMY,
                    AcslBuiltinLogicType.INTEGER,
                    new AcslIdTerm(
                        FileLocation.DUMMY,
                        new AcslCVariableDeclaration(
                            (CVariableDeclaration)
                                Objects.requireNonNull(cProgramScope.lookupVariable("y")))),
                    new AcslIntegerLiteralTerm(
                        FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ONE),
                    AcslBinaryTermOperator.PLUS),
                // If false operator
                new AcslBinaryTerm(
                    FileLocation.DUMMY,
                    AcslBuiltinLogicType.INTEGER,
                    new AcslIdTerm(
                        FileLocation.DUMMY,
                        new AcslCVariableDeclaration(
                            (CVariableDeclaration)
                                Objects.requireNonNull(cProgramScope.lookupVariable("z")))),
                    new AcslIntegerLiteralTerm(
                        FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.TWO),
                    AcslBinaryTermOperator.MINUS)),
            // Second operand
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.valueOf(-1)),
            // Operator
            AcslBinaryTermExpressionOperator.NOT_EQUALS);
    String input = "(x == 0 ? y + 1 : z - 2) != -1";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimpleValidPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslPredicate output =
        new AcslValidPredicate(
            FileLocation.DUMMY,
            new AcslMemoryLocationSetTerm(
                FileLocation.DUMMY,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x"))))));
    String input = "\\valid(x)";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimpleEmptyValidPredicate() throws AcslParseException {
    AcslPredicate output =
        new AcslValidPredicate(
            FileLocation.DUMMY, new AcslMemoryLocationSetEmpty(FileLocation.DUMMY));
    String input = "\\valid(\\empty)";

    testPredicateParsing(input, output);
  }
}
