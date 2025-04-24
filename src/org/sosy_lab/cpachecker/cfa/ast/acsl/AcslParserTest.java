// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Objects;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicateExpression.AcslBinaryPredicateExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm.AcslBinaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermExpression.AcslBinaryTermExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParser.AcslParseException;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryExpression.AcslUnaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm.AcslUnaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;

public class AcslParserTest {

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

  private void testParsing(String input, AcslAstNode output) throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslScope acslScope = getAcslScope();

    AcslAstNode parsed = AcslParser.parsePredicate(input, cProgramScope, acslScope);
    assert parsed.equals(output) : "Parsed object does not match expected object";
  }

  @Test
  public void parseConstantTruePredicate() throws AcslParseException {
    AcslExpression output = new AcslBooleanLiteralExpression(FileLocation.DUMMY, true);
    String input = "\\true";

    testParsing(input, output);
  }

  @Test
  public void parseConstantFalsePredicate() throws AcslParseException {
    AcslExpression output = new AcslBooleanLiteralExpression(FileLocation.DUMMY, false);
    String input = "\\false";

    testParsing(input, output);
  }

  @Test
  public void parseSimplePredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryTermExpression(
            FileLocation.DUMMY,
            AcslBuiltinLogicType.BOOLEAN,
            new AcslIdTerm(
                FileLocation.DUMMY,
                new AcslCVariableDeclaration(
                    (CVariableDeclaration)
                        Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.TEN),
            AcslBinaryTermExpressionOperator.EQUALS);
    String input = "x == 10";

    testParsing(input, output);
  }

  @Test
  public void parseSimpleNegatedPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslUnaryExpression(
            FileLocation.DUMMY,
            AcslBuiltinLogicType.BOOLEAN,
            new AcslBinaryTermExpression(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.BOOLEAN,
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

    testParsing(input, output);
  }

  @Test
  public void parseSimplePredicateWithOperations() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryTermExpression(
            FileLocation.DUMMY,
            AcslBuiltinLogicType.BOOLEAN,
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

    testParsing(input, output);
  }

  @Test
  public void parseSimplePredicateWithUnaryTerm() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryTermExpression(
            FileLocation.DUMMY,
            AcslBuiltinLogicType.BOOLEAN,
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

    testParsing(input, output);
  }

  @Test
  public void parseSimpleBinaryPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryPredicateExpression(
            FileLocation.DUMMY,
            AcslBuiltinLogicType.BOOLEAN,
            // first operator
            new AcslBinaryTermExpression(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.BOOLEAN,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.TEN),
                AcslBinaryTermExpressionOperator.LESS_THAN),
            // second operator
            new AcslBinaryTermExpression(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.BOOLEAN,
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

    testParsing(input, output);
  }

  @Test
  public void parseSimplePredicateWithOld() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryTermExpression(
            FileLocation.DUMMY,
            AcslBuiltinLogicType.BOOLEAN,
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

    testParsing(input, output);
  }

  @Test
  public void parseSimpleOldPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslOldExpression(
            FileLocation.DUMMY,
            new AcslBinaryTermExpression(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.BOOLEAN,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.valueOf(-1)),
                AcslBinaryTermExpressionOperator.LESS_EQUAL));
    String input = "\\old(x <= -1)";

    testParsing(input, output);
  }

  @Test
  public void parseSimpleResultPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryTermExpression(
            FileLocation.DUMMY,
            AcslBuiltinLogicType.BOOLEAN,
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

    testParsing(input, output);
  }

  @Test
  public void parseSimpleAtPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryTermExpression(
            FileLocation.DUMMY,
            AcslBuiltinLogicType.BOOLEAN,
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

    testParsing(input, output);
  }

  @Test
  public void parseSimpleImplicitTermToPredicateConversion() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryTermExpression(
            FileLocation.DUMMY,
            AcslBuiltinLogicType.BOOLEAN,
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

    testParsing(input, output);
  }

  @Test
  public void parseSimpleAtPredicateWithArbitraryLabel() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryTermExpression(
            FileLocation.DUMMY,
            AcslBuiltinLogicType.BOOLEAN,
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

    testParsing(input, output);
  }

  @Test
  public void parseSimpleTernaryOperatorWithPredicates() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslTernaryPredicateExpression(
            FileLocation.DUMMY,
            // Condition
            new AcslBinaryTermExpression(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.BOOLEAN,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
                AcslBinaryTermExpressionOperator.EQUALS),
            // If true operator
            new AcslBinaryTermExpression(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.BOOLEAN,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("y")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
                AcslBinaryTermExpressionOperator.GREATER_THAN),
            // If false operator
            new AcslBinaryTermExpression(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.BOOLEAN,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("z")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
                AcslBinaryTermExpressionOperator.LESS_THAN));
    String input = "x == 0 ? y > 0 : z < 0";

    testParsing(input, output);
  }

  @Test
  public void parseSimpleTermTernaryOperatorWithTerms() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryTermExpression(
            FileLocation.DUMMY,
            AcslBuiltinLogicType.BOOLEAN,
            // First operand
            new AcslTernaryTermExpression(
                FileLocation.DUMMY,
                // Condition
                new AcslBinaryTermExpression(
                    FileLocation.DUMMY,
                    AcslBuiltinLogicType.BOOLEAN,
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

    testParsing(input, output);
  }

  @Test
  public void parseSimpleValidPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslValidExpression(
            FileLocation.DUMMY,
            AcslBuiltinLogicType.BOOLEAN,
            new AcslMemoryLocationSetTerm(
                FileLocation.DUMMY,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x"))))));
    String input = "\\valid(x)";

    testParsing(input, output);
  }

  @Test
  public void parseLogicalFunctionDeclaration() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryTermExpression(
            FileLocation.DUMMY,
            AcslBuiltinLogicType.BOOLEAN,
            // First operand
            new AcslTernaryTermExpression(
                FileLocation.DUMMY,
                // Condition
                new AcslBinaryTermExpression(
                    FileLocation.DUMMY,
                    AcslBuiltinLogicType.BOOLEAN,
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
    String input =
        "integer MaxArray<T>(T* a,integer i) = (i == 0) ? a[0] : (a[i] < MaxArray(a, i - 1) ? a[i] : MaxArray(a, i - 1))";

    testParsing(input, output);
  }
}
