// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl.antlr;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAstNode;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate.AcslBinaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm.AcslBinaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate.AcslBinaryTermPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLabel;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslMemoryLocationSet;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslProgramLabel;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslQuantifiedPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslQuantifiedPredicate.AcslQuantifier;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslResultTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate.AcslUnaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm.AcslUnaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslValidPredicate;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.AcslParser.AcslParseException;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslCType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class AcslParserPredicateTest {

  private CSimpleType basicInt() {
    return new CSimpleType(
        CTypeQualifiers.NONE, CBasicType.INT, false, false, true, false, false, false, false);
  }

  private CProgramScope getCProgramScope() {
    String currentFunctionName = "f";
    ImmutableList.Builder<CSimpleDeclaration> declarations = ImmutableList.builder();
    for (String var : ImmutableList.of("x", "y", "z")) {
      declarations.add(
          new CVariableDeclaration(
              FileLocation.DUMMY,
              true,
              CStorageClass.AUTO,
              basicInt(),
              var,
              var,
              var,
              null /* No initializer, we only want it for testing */,
              /* pHasThreadLocalStorage= */ false));
    }
    declarations.add(
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            new CFunctionType(basicInt(), ImmutableList.of(), false),
            currentFunctionName,
            ImmutableList.of(),
            ImmutableSet.of()));

    return CProgramScope.ofDeclarations(declarations.build())
        .withFunctionScope(currentFunctionName);
  }

  private AcslScope getAcslScope() {
    AcslScope scope = AcslScope.empty();

    return scope;
  }

  private void testPredicateParsing(String input, AcslAstNode output) throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslScope acslScope = getAcslScope();

    AcslAstNode parsed =
        AcslParser.parsePredicate(input, cProgramScope, acslScope, FileLocation.DUMMY);
    assert parsed.equals(output) : "Parsed object does not match expected object";
  }

  private Object parseConstant(String pConstant) throws AcslParseException {
    AcslPredicate parsed =
        AcslParser.parsePredicate(
            "x == " + pConstant, getCProgramScope(), getAcslScope(), FileLocation.DUMMY);
    assertThat(parsed).isInstanceOf(AcslBinaryTermPredicate.class);
    AcslTerm constant = ((AcslBinaryTermPredicate) parsed).getOperand2();
    assertThat(constant).isInstanceOf(AcslLiteralTerm.class);
    return ((AcslLiteralTerm) constant).getValue();
  }

  @Test
  public void parseIntegerConstants() throws AcslParseException {
    assertThat(parseConstant("42")).isEqualTo(BigInteger.valueOf(42));
    assertThat(parseConstant("42UL")).isEqualTo(BigInteger.valueOf(42));
    assertThat(parseConstant("0x2a")).isEqualTo(BigInteger.valueOf(42));
    assertThat(parseConstant("0X2Allu")).isEqualTo(BigInteger.valueOf(42));
    assertThat(parseConstant("052")).isEqualTo(BigInteger.valueOf(42));
    assertThat(parseConstant("0")).isEqualTo(BigInteger.ZERO);
  }

  @Test
  public void parseFloatingConstants() throws AcslParseException {
    assertThat(parseConstant("1.5")).isEqualTo(new BigDecimal("1.5"));
    assertThat(parseConstant("1.5e3f")).isEqualTo(new BigDecimal("1.5e3"));
    assertThat(parseConstant("2.")).isEqualTo(new BigDecimal("2."));
    assertThat(parseConstant("0x1.8p1")).isEqualTo(new BigDecimal("3"));
  }

  @Test
  public void parseCharacterConstants() throws AcslParseException {
    assertThat(parseConstant("'a'")).isEqualTo('a');
    assertThat(parseConstant("'\\n'")).isEqualTo('\n');
    assertThat(parseConstant("'\\''")).isEqualTo('\'');
    assertThat(parseConstant("'\\101'")).isEqualTo('A');
    assertThat(parseConstant("L'\\x41'")).isEqualTo('A');
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
            AcslBinaryTermPredicateOperator.EQUALS);
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
                AcslBinaryTermPredicateOperator.EQUALS),
            AcslUnaryPredicateOperator.NEGATION);
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
            AcslBinaryTermPredicateOperator.EQUALS);
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
            AcslBinaryTermPredicateOperator.EQUALS);
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
                AcslBinaryTermPredicateOperator.LESS_THAN),
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
                AcslBinaryTermPredicateOperator.GREATER_THAN),
            AcslBinaryPredicateOperator.AND);
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
            AcslBinaryTermPredicateOperator.LESS_THAN);
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
                AcslBinaryTermPredicateOperator.LESS_EQUAL));
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
            AcslBinaryTermPredicateOperator.GREATER_THAN);
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
            AcslBinaryTermPredicateOperator.GREATER_THAN);
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
            AcslBinaryTermPredicateOperator.EQUALS);
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
            AcslBinaryTermPredicateOperator.GREATER_THAN);
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
                AcslBinaryTermPredicateOperator.EQUALS),
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
                AcslBinaryTermPredicateOperator.GREATER_THAN),
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
                AcslBinaryTermPredicateOperator.LESS_THAN));
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
                    AcslBinaryTermPredicateOperator.EQUALS),
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
            AcslBinaryTermPredicateOperator.NOT_EQUALS);
    String input = "(x == 0 ? y + 1 : z - 2) != -1";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimpleValidPredicate() throws AcslParseException {
    CProgramScope cProgramScope = getCProgramScope();
    AcslPredicate output =
        new AcslValidPredicate(
            FileLocation.DUMMY,
            new AcslMemoryLocationSet(
                FileLocation.DUMMY,
                ImmutableList.of(
                    new AcslIdTerm(
                        FileLocation.DUMMY,
                        new AcslCVariableDeclaration(
                            (CVariableDeclaration)
                                Objects.requireNonNull(cProgramScope.lookupVariable("x")))))));
    String input = "\\valid(x)";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimpleEmptyValidPredicate() throws AcslParseException {
    AcslPredicate output =
        new AcslValidPredicate(
            FileLocation.DUMMY, new AcslMemoryLocationSet(FileLocation.DUMMY, ImmutableList.of()));
    String input = "\\valid(\\empty)";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimpleForallPredicate() throws AcslParseException {
    AcslParameterDeclaration x =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "ax");
    AcslParameterDeclaration y =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.REAL, "ay");
    AcslPredicate output =
        new AcslQuantifiedPredicate(
            FileLocation.DUMMY,
            AcslQuantifier.FORALL,
            ImmutableList.of(x, y),
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                new AcslIdTerm(FileLocation.DUMMY, x),
                new AcslIdTerm(FileLocation.DUMMY, y),
                AcslBinaryTermPredicateOperator.EQUALS));
    String input = "\\forall integer ax, real ay ; ax == ay";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimpleForallPredicate2() throws AcslParseException {
    AcslParameterDeclaration x =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "ax");
    AcslParameterDeclaration y =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "ay");
    AcslPredicate output =
        new AcslQuantifiedPredicate(
            FileLocation.DUMMY,
            AcslQuantifier.FORALL,
            ImmutableList.of(x, y),
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                new AcslIdTerm(FileLocation.DUMMY, x),
                new AcslIdTerm(FileLocation.DUMMY, y),
                AcslBinaryTermPredicateOperator.EQUALS));
    String input = "\\forall integer ax, ay ; ax == ay";

    testPredicateParsing(input, output);
  }

  @Test
  public void parseSimpleExistsPredicate() throws AcslParseException {
    AcslParameterDeclaration x =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "ax");
    AcslParameterDeclaration y =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.REAL, "ay");
    AcslPredicate output =
        new AcslQuantifiedPredicate(
            FileLocation.DUMMY,
            AcslQuantifier.EXISTS,
            ImmutableList.of(x, y),
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                new AcslIdTerm(FileLocation.DUMMY, x),
                new AcslIdTerm(FileLocation.DUMMY, y),
                AcslBinaryTermPredicateOperator.EQUALS));
    String input = "\\exists integer ax, real ay ; ax == ay";

    testPredicateParsing(input, output);
  }

  @Test
  public void printQuantifiedPredicates() throws AcslParseException {
    for (String input :
        ImmutableList.of(
            "\\exists integer ax, real ay; ax == ay", "\\forall integer ax; ax <= (ax + 1)")) {
      AcslPredicate parsed =
          AcslParser.parsePredicate(input, getCProgramScope(), getAcslScope(), FileLocation.DUMMY);
      assertThat(parsed.toASTString()).isEqualTo(input);
      assertThat(
              AcslParser.parsePredicate(
                  parsed.toASTString(), getCProgramScope(), getAcslScope(), FileLocation.DUMMY))
          .isEqualTo(parsed);
    }
  }

  @Test
  public void traverseChildrenOfPredicates() throws AcslParseException {
    AcslPredicate parsed =
        AcslParser.parsePredicate(
            "\\forall integer ax; ax <= x ? \\old(y) : z",
            getCProgramScope(),
            getAcslScope(),
            FileLocation.DUMMY);
    assertThat(
            CFAUtils.traverseRecursively(parsed)
                .filter(AcslIdTerm.class)
                .transform(term -> term.getDeclaration().getName()))
        .containsExactly("ax", "x", "y", "z");
  }
}
