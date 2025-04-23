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
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryExpression.AcslBinaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermComparisonExpression.AcslBinaryTermComparisonExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParser.AcslParseException;
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
    scope.registerDeclaration(
        new CVariableDeclaration(
            FileLocation.DUMMY,
            true,
            CStorageClass.AUTO,
            basicInt(),
            "x",
            "x",
            "x",
            null /* No initializer, we only want it for testing */));
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
        new AcslBinaryTermComparisonExpression(
            FileLocation.DUMMY,
            AcslBuiltinLogicType.BOOLEAN,
            new AcslIdTerm(
                FileLocation.DUMMY,
                new AcslCVariableDeclaration(
                    (CVariableDeclaration)
                        Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.TEN),
            AcslBinaryTermComparisonExpressionOperator.EQUALS);
    String input = "x == 10";

    testParsing(input, output);
  }

  @Test
  public void parseSimplePredicateWithUnaryTerm() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryTermComparisonExpression(
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
            AcslBinaryTermComparisonExpressionOperator.EQUALS);
    String input = "-x == 5";

    testParsing(input, output);
  }

  @Test
  public void parseSimpleBinaryPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryExpression(
            FileLocation.DUMMY,
            AcslBuiltinLogicType.BOOLEAN,
            // first operator
            new AcslBinaryTermComparisonExpression(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.BOOLEAN,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.TEN),
                AcslBinaryTermComparisonExpressionOperator.LESS_THAN),
            // second operator
            new AcslBinaryTermComparisonExpression(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.BOOLEAN,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
                AcslBinaryTermComparisonExpressionOperator.GREATER_THAN),
            AcslBinaryExpressionOperator.AND);
    String input = "x < 10 && x > 0";

    testParsing(input, output);
  }

  @Test
  public void parseSimplePredicateWithOld() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryTermComparisonExpression(
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
            AcslBinaryTermComparisonExpressionOperator.LESS_THAN);
    String input = "\\old(x) < 0";

    testParsing(input, output);
  }

  @Test
  public void parseSimpleOldPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslOldExpression(
            FileLocation.DUMMY,
            new AcslBinaryTermComparisonExpression(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.BOOLEAN,
                new AcslIdTerm(
                    FileLocation.DUMMY,
                    new AcslCVariableDeclaration(
                        (CVariableDeclaration)
                            Objects.requireNonNull(cProgramScope.lookupVariable("x")))),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.valueOf(-1)),
                AcslBinaryTermComparisonExpressionOperator.LESS_EQUAL));
    String input = "\\old(x <= -1)";

    testParsing(input, output);
  }

  @Test
  public void parseSimpleResultPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryTermComparisonExpression(
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
            AcslBinaryTermComparisonExpressionOperator.GREATER_THAN);
    String input = "\\result > 1";

    testParsing(input, output);
  }

  @Test
  public void parseSimpleAtPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryTermComparisonExpression(
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
            AcslBinaryTermComparisonExpressionOperator.GREATER_THAN);
    String input = "\\at(x, Pre) > 1";

    testParsing(input, output);
  }

  @Test
  public void parseSimpleAtPredicateWithArbitraryLabel() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslExpression output =
        new AcslBinaryTermComparisonExpression(
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
            AcslBinaryTermComparisonExpressionOperator.GREATER_THAN);
    String input = "\\at(x, a) > 1";

    testParsing(input, output);
  }
}
