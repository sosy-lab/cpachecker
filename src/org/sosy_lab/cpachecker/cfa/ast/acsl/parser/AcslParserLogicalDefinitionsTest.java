// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslArraySubscriptTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAstNode;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate.AcslBinaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm.AcslBinaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCExpressionTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCLeftHandSideTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCanAccessPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslForallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionCallTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicFunctionDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicPredicateDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPointerType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPolymorphicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateApplicationPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslSeparateMemoryConjunctionPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTypeVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate.AcslUnaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslParser.AcslParseException;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;

public class AcslParserLogicalDefinitionsTest {

  private AcslScope getAcslScope() {
    AcslScope scope = AcslScope.mutableCopy(AcslScope.empty());

    return scope;
  }

  private void testLogicalFunctionParsing(String input, AcslAstNode output)
      throws AcslParseException {

    AcslScope acslScope = getAcslScope();

    AcslAstNode parsed = AcslParser.parseLogicalDefinition(input, acslScope, FileLocation.DUMMY);
    assert parsed.equals(output) : "Parsed object does not match expected object";
  }

  @Test
  public void parseMaxArrayLogicalFunctionDeclaration() throws AcslParseException {

    AcslPolymorphicType polymorphicType = new AcslPolymorphicType("T");
    AcslPointerType pointerType = new AcslPointerType(polymorphicType);
    AcslParameterDeclaration inputArray =
        new AcslParameterDeclaration(FileLocation.DUMMY, pointerType, "a");
    AcslParameterDeclaration inputIndex =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i");
    AcslFunctionDeclaration maxArrayDeclaration =
        new AcslFunctionDeclaration(
            FileLocation.DUMMY,
            // Function type
            new AcslFunctionType(
                polymorphicType,
                ImmutableList.of(pointerType, AcslBuiltinLogicType.INTEGER),
                false),
            "MaxArray",
            "MaxArray",
            // Polymorphic types
            ImmutableList.of(
                new AcslTypeVariableDeclaration(
                    FileLocation.DUMMY,
                    false,
                    polymorphicType,
                    polymorphicType.toString(),
                    polymorphicType.toString())),
            // Parameters
            ImmutableList.of(inputArray, inputIndex));

    AcslAstNode output =
        new AcslLogicFunctionDefinition(
            FileLocation.DUMMY,
            // Function Declaration
            maxArrayDeclaration,
            // Function body
            new AcslTernaryTerm(
                FileLocation.DUMMY,
                // Condition
                new AcslBinaryTermPredicate(
                    FileLocation.DUMMY,
                    new AcslIdTerm(FileLocation.DUMMY, inputIndex),
                    new AcslIntegerLiteralTerm(
                        FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
                    AcslBinaryTermExpressionOperator.EQUALS),
                // True branch
                new AcslArraySubscriptTerm(
                    FileLocation.DUMMY,
                    polymorphicType,
                    new AcslIdTerm(FileLocation.DUMMY, inputArray),
                    new AcslIntegerLiteralTerm(
                        FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO)),
                // False branch
                new AcslTernaryTerm(
                    FileLocation.DUMMY,
                    // condition
                    new AcslBinaryTermPredicate(
                        FileLocation.DUMMY,
                        new AcslArraySubscriptTerm(
                            FileLocation.DUMMY,
                            polymorphicType,
                            new AcslIdTerm(FileLocation.DUMMY, inputArray),
                            new AcslIdTerm(FileLocation.DUMMY, inputIndex)),
                        new AcslFunctionCallTerm(
                            FileLocation.DUMMY,
                            (AcslType) maxArrayDeclaration.getType().getReturnType(),
                            new AcslIdTerm(FileLocation.DUMMY, maxArrayDeclaration),
                            ImmutableList.of(
                                new AcslIdTerm(FileLocation.DUMMY, inputArray),
                                new AcslBinaryTerm(
                                    FileLocation.DUMMY,
                                    AcslBuiltinLogicType.INTEGER,
                                    new AcslIdTerm(FileLocation.DUMMY, inputIndex),
                                    new AcslIntegerLiteralTerm(
                                        FileLocation.DUMMY,
                                        AcslBuiltinLogicType.INTEGER,
                                        BigInteger.valueOf(1)),
                                    AcslBinaryTermOperator.MINUS)),
                            maxArrayDeclaration),
                        AcslBinaryTermExpressionOperator.GREATER_THAN),
                    // True branch
                    new AcslArraySubscriptTerm(
                        FileLocation.DUMMY,
                        polymorphicType,
                        new AcslIdTerm(FileLocation.DUMMY, inputArray),
                        new AcslIdTerm(FileLocation.DUMMY, inputIndex)),
                    // False branch
                    new AcslFunctionCallTerm(
                        FileLocation.DUMMY,
                        (AcslType) maxArrayDeclaration.getType().getReturnType(),
                        new AcslIdTerm(FileLocation.DUMMY, maxArrayDeclaration),
                        ImmutableList.of(
                            new AcslIdTerm(FileLocation.DUMMY, inputArray),
                            new AcslBinaryTerm(
                                FileLocation.DUMMY,
                                AcslBuiltinLogicType.INTEGER,
                                new AcslIdTerm(FileLocation.DUMMY, inputIndex),
                                new AcslIntegerLiteralTerm(
                                    FileLocation.DUMMY,
                                    AcslBuiltinLogicType.INTEGER,
                                    BigInteger.valueOf(1)),
                                AcslBinaryTermOperator.MINUS)),
                        maxArrayDeclaration))));
    String input =
        "T MaxArray<T>(T* a,integer i) = (i == 0) ? a[0] : (a[i] > MaxArray(a, i - 1) ? a[i] :"
            + " MaxArray(a, i - 1))";

    testLogicalFunctionParsing(input, output);
  }

  @Test
  public void parseMinArrayLogicalFunctionDeclaration() throws AcslParseException {

    AcslPolymorphicType polymorphicType = new AcslPolymorphicType("T");
    AcslPointerType pointerType = new AcslPointerType(polymorphicType);
    AcslParameterDeclaration inputArray =
        new AcslParameterDeclaration(FileLocation.DUMMY, pointerType, "a");
    AcslParameterDeclaration inputIndex =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i");
    AcslFunctionDeclaration minArrayDeclaration =
        new AcslFunctionDeclaration(
            FileLocation.DUMMY,
            // Function type
            new AcslFunctionType(
                polymorphicType,
                ImmutableList.of(pointerType, AcslBuiltinLogicType.INTEGER),
                false),
            "MinArray",
            "MinArray",
            // Polymorphic types
            ImmutableList.of(
                new AcslTypeVariableDeclaration(
                    FileLocation.DUMMY,
                    false,
                    polymorphicType,
                    polymorphicType.toString(),
                    polymorphicType.toString())),
            // Parameters
            ImmutableList.of(inputArray, inputIndex));

    AcslAstNode output =
        new AcslLogicFunctionDefinition(
            FileLocation.DUMMY,
            // Function Declaration
            minArrayDeclaration,
            // Function body
            new AcslTernaryTerm(
                FileLocation.DUMMY,
                // Condition
                new AcslBinaryTermPredicate(
                    FileLocation.DUMMY,
                    new AcslIdTerm(FileLocation.DUMMY, inputIndex),
                    new AcslIntegerLiteralTerm(
                        FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
                    AcslBinaryTermExpressionOperator.EQUALS),
                // True branch
                new AcslArraySubscriptTerm(
                    FileLocation.DUMMY,
                    polymorphicType,
                    new AcslIdTerm(FileLocation.DUMMY, inputArray),
                    new AcslIntegerLiteralTerm(
                        FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO)),
                // False branch
                new AcslTernaryTerm(
                    FileLocation.DUMMY,
                    // condition
                    new AcslBinaryTermPredicate(
                        FileLocation.DUMMY,
                        new AcslArraySubscriptTerm(
                            FileLocation.DUMMY,
                            polymorphicType,
                            new AcslIdTerm(FileLocation.DUMMY, inputArray),
                            new AcslIdTerm(FileLocation.DUMMY, inputIndex)),
                        new AcslFunctionCallTerm(
                            FileLocation.DUMMY,
                            (AcslType) minArrayDeclaration.getType().getReturnType(),
                            new AcslIdTerm(FileLocation.DUMMY, minArrayDeclaration),
                            ImmutableList.of(
                                new AcslIdTerm(FileLocation.DUMMY, inputArray),
                                new AcslBinaryTerm(
                                    FileLocation.DUMMY,
                                    AcslBuiltinLogicType.INTEGER,
                                    new AcslIdTerm(FileLocation.DUMMY, inputIndex),
                                    new AcslIntegerLiteralTerm(
                                        FileLocation.DUMMY,
                                        AcslBuiltinLogicType.INTEGER,
                                        BigInteger.valueOf(1)),
                                    AcslBinaryTermOperator.MINUS)),
                            minArrayDeclaration),
                        AcslBinaryTermExpressionOperator.LESS_THAN),
                    // True branch
                    new AcslArraySubscriptTerm(
                        FileLocation.DUMMY,
                        polymorphicType,
                        new AcslIdTerm(FileLocation.DUMMY, inputArray),
                        new AcslIdTerm(FileLocation.DUMMY, inputIndex)),
                    // False branch
                    new AcslFunctionCallTerm(
                        FileLocation.DUMMY,
                        (AcslType) minArrayDeclaration.getType().getReturnType(),
                        new AcslIdTerm(FileLocation.DUMMY, minArrayDeclaration),
                        ImmutableList.of(
                            new AcslIdTerm(FileLocation.DUMMY, inputArray),
                            new AcslBinaryTerm(
                                FileLocation.DUMMY,
                                AcslBuiltinLogicType.INTEGER,
                                new AcslIdTerm(FileLocation.DUMMY, inputIndex),
                                new AcslIntegerLiteralTerm(
                                    FileLocation.DUMMY,
                                    AcslBuiltinLogicType.INTEGER,
                                    BigInteger.valueOf(1)),
                                AcslBinaryTermOperator.MINUS)),
                        minArrayDeclaration))));
    String input =
        "T MinArray<T>(T* a,integer i) = (i == 0) ? a[0] : (a[i] < MinArray(a, i - 1) ? a[i] :"
            + " MinArray(a, i - 1))";

    testLogicalFunctionParsing(input, output);
  }

  @Test
  public void parseSumArrayLogicalFunctionDeclaration() throws AcslParseException {

    AcslPolymorphicType polymorphicType = new AcslPolymorphicType("T");
    AcslPointerType pointerType = new AcslPointerType(polymorphicType);
    AcslParameterDeclaration inputArray =
        new AcslParameterDeclaration(FileLocation.DUMMY, pointerType, "a");
    AcslParameterDeclaration inputIndex =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i");
    AcslFunctionDeclaration sumArrayDeclaration =
        new AcslFunctionDeclaration(
            FileLocation.DUMMY,
            // Function type
            new AcslFunctionType(
                polymorphicType,
                ImmutableList.of(pointerType, AcslBuiltinLogicType.INTEGER),
                false),
            "SumArray",
            "SumArray",
            // Polymorphic types
            ImmutableList.of(
                new AcslTypeVariableDeclaration(
                    FileLocation.DUMMY,
                    false,
                    polymorphicType,
                    polymorphicType.toString(),
                    polymorphicType.toString())),
            // Parameters
            ImmutableList.of(inputArray, inputIndex));

    AcslAstNode output =
        new AcslLogicFunctionDefinition(
            FileLocation.DUMMY,
            // Function Declaration
            sumArrayDeclaration,
            // Function body
            new AcslTernaryTerm(
                FileLocation.DUMMY,
                // Condition
                new AcslBinaryTermPredicate(
                    FileLocation.DUMMY,
                    new AcslIdTerm(FileLocation.DUMMY, inputIndex),
                    new AcslIntegerLiteralTerm(
                        FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
                    AcslBinaryTermExpressionOperator.EQUALS),
                // True branch
                new AcslArraySubscriptTerm(
                    FileLocation.DUMMY,
                    polymorphicType,
                    new AcslIdTerm(FileLocation.DUMMY, inputArray),
                    new AcslIntegerLiteralTerm(
                        FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO)),
                // False branch
                new AcslBinaryTerm(
                    FileLocation.DUMMY,
                    polymorphicType,
                    new AcslArraySubscriptTerm(
                        FileLocation.DUMMY,
                        polymorphicType,
                        new AcslIdTerm(FileLocation.DUMMY, inputArray),
                        new AcslIdTerm(FileLocation.DUMMY, inputIndex)),
                    new AcslFunctionCallTerm(
                        FileLocation.DUMMY,
                        (AcslType) sumArrayDeclaration.getType().getReturnType(),
                        new AcslIdTerm(FileLocation.DUMMY, sumArrayDeclaration),
                        ImmutableList.of(
                            new AcslIdTerm(FileLocation.DUMMY, inputArray),
                            new AcslBinaryTerm(
                                FileLocation.DUMMY,
                                AcslBuiltinLogicType.INTEGER,
                                new AcslIdTerm(FileLocation.DUMMY, inputIndex),
                                new AcslIntegerLiteralTerm(
                                    FileLocation.DUMMY,
                                    AcslBuiltinLogicType.INTEGER,
                                    BigInteger.valueOf(1)),
                                AcslBinaryTermOperator.MINUS)),
                        sumArrayDeclaration),
                    AcslBinaryTermOperator.PLUS)));
    String input = "T SumArray<T>(T* a,integer i) = (i == 0) ? a[0] : a[i] + SumArray(a, i - 1)";

    testLogicalFunctionParsing(input, output);
  }

  @Test
  public void parseFibLogicalFunctionDeclaration() throws AcslParseException {
    AcslParameterDeclaration inputValue =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i");
    AcslFunctionDeclaration fibDeclaration =
        new AcslFunctionDeclaration(
            FileLocation.DUMMY,
            // Function type
            new AcslFunctionType(
                AcslBuiltinLogicType.INTEGER, ImmutableList.of(inputValue.getType()), false),
            "Fib",
            "Fib",
            // Polymorphic types
            ImmutableList.of(),
            // Parameters
            ImmutableList.of(inputValue));

    AcslAstNode output =
        new AcslLogicFunctionDefinition(
            FileLocation.DUMMY,
            // Function Declaration
            fibDeclaration,
            // Function body
            new AcslTernaryTerm(
                FileLocation.DUMMY,
                // Condition
                new AcslBinaryPredicate(
                    FileLocation.DUMMY,
                    new AcslBinaryTermPredicate(
                        FileLocation.DUMMY,
                        new AcslIdTerm(FileLocation.DUMMY, inputValue),
                        AcslIntegerLiteralTerm.ZERO,
                        AcslBinaryTermExpressionOperator.EQUALS),
                    new AcslBinaryTermPredicate(
                        FileLocation.DUMMY,
                        new AcslIdTerm(FileLocation.DUMMY, inputValue),
                        AcslIntegerLiteralTerm.ONE,
                        AcslBinaryTermExpressionOperator.EQUALS),
                    AcslBinaryPredicateOperator.OR),
                // True branch
                new AcslIdTerm(FileLocation.DUMMY, inputValue),
                // False branch
                new AcslBinaryTerm(
                    FileLocation.DUMMY,
                    (AcslType) fibDeclaration.getType().getReturnType(),
                    new AcslFunctionCallTerm(
                        FileLocation.DUMMY,
                        (AcslType) fibDeclaration.getType().getReturnType(),
                        new AcslIdTerm(FileLocation.DUMMY, fibDeclaration),
                        ImmutableList.of(
                            new AcslBinaryTerm(
                                FileLocation.DUMMY,
                                AcslBuiltinLogicType.INTEGER,
                                new AcslIdTerm(FileLocation.DUMMY, inputValue),
                                AcslIntegerLiteralTerm.ONE,
                                AcslBinaryTermOperator.MINUS)),
                        fibDeclaration),
                    new AcslFunctionCallTerm(
                        FileLocation.DUMMY,
                        (AcslType) fibDeclaration.getType().getReturnType(),
                        new AcslIdTerm(FileLocation.DUMMY, fibDeclaration),
                        ImmutableList.of(
                            new AcslBinaryTerm(
                                FileLocation.DUMMY,
                                AcslBuiltinLogicType.INTEGER,
                                new AcslIdTerm(FileLocation.DUMMY, inputValue),
                                AcslIntegerLiteralTerm.TWO,
                                AcslBinaryTermOperator.MINUS)),
                        fibDeclaration),
                    AcslBinaryTermOperator.PLUS)));
    String input = "integer Fib(integer i) = (i == 0) || (i == 1) ? i : Fib(i - 1) + Fib(i - 2)";

    testLogicalFunctionParsing(input, output);
  }

  @Test
  public void parseSortedLogicalPredicateDeclaration() throws AcslParseException {
    AcslPolymorphicType polymorphicType = new AcslPolymorphicType("T");
    AcslPointerType pointerType = new AcslPointerType(polymorphicType);
    AcslParameterDeclaration inputArray =
        new AcslParameterDeclaration(FileLocation.DUMMY, pointerType, "a");
    AcslParameterDeclaration lowerLimit =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i");
    AcslParameterDeclaration upperLimit =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "j");
    AcslParameterDeclaration firstIterator =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "k");
    AcslParameterDeclaration secondIterator =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "l");

    AcslPredicateDeclaration sortedDeclaration =
        new AcslPredicateDeclaration(
            FileLocation.DUMMY,
            // Function type
            new AcslPredicateType(
                ImmutableList.of(inputArray.getType(), lowerLimit.getType(), upperLimit.getType()),
                false),
            "Sorted",
            "Sorted",
            // Polymorphic types
            ImmutableList.of(
                new AcslTypeVariableDeclaration(
                    FileLocation.DUMMY,
                    false,
                    polymorphicType,
                    polymorphicType.toString(),
                    polymorphicType.toString())),
            // Parameters
            ImmutableList.of(inputArray, lowerLimit, upperLimit));

    AcslAstNode output =
        new AcslLogicPredicateDefinition(
            FileLocation.DUMMY,
            // Function Declaration
            sortedDeclaration,
            // Function body
            new AcslForallPredicate(
                FileLocation.DUMMY,
                ImmutableList.of(firstIterator, secondIterator),
                new AcslBinaryPredicate(
                    FileLocation.DUMMY,
                    // The pre-condition of the quantifier
                    // this is: i <= k < l <= j
                    new AcslBinaryPredicate(
                        FileLocation.DUMMY,
                        new AcslBinaryTermPredicate(
                            FileLocation.DUMMY,
                            new AcslIdTerm(FileLocation.DUMMY, secondIterator),
                            new AcslIdTerm(FileLocation.DUMMY, upperLimit),
                            AcslBinaryTermExpressionOperator.LESS_EQUAL),
                        new AcslBinaryPredicate(
                            FileLocation.DUMMY,
                            new AcslBinaryTermPredicate(
                                FileLocation.DUMMY,
                                new AcslIdTerm(FileLocation.DUMMY, firstIterator),
                                new AcslIdTerm(FileLocation.DUMMY, secondIterator),
                                AcslBinaryTermExpressionOperator.LESS_THAN),
                            new AcslBinaryTermPredicate(
                                FileLocation.DUMMY,
                                new AcslIdTerm(FileLocation.DUMMY, lowerLimit),
                                new AcslIdTerm(FileLocation.DUMMY, firstIterator),
                                AcslBinaryTermExpressionOperator.LESS_EQUAL),
                            AcslBinaryPredicateOperator.AND),
                        AcslBinaryPredicateOperator.AND),
                    // This is the implied condition
                    // a[k] < a[l]
                    new AcslBinaryTermPredicate(
                        FileLocation.DUMMY,
                        new AcslArraySubscriptTerm(
                            FileLocation.DUMMY,
                            polymorphicType,
                            new AcslIdTerm(FileLocation.DUMMY, inputArray),
                            new AcslIdTerm(FileLocation.DUMMY, firstIterator)),
                        new AcslArraySubscriptTerm(
                            FileLocation.DUMMY,
                            polymorphicType,
                            new AcslIdTerm(FileLocation.DUMMY, inputArray),
                            new AcslIdTerm(FileLocation.DUMMY, secondIterator)),
                        AcslBinaryTermExpressionOperator.LESS_THAN),
                    AcslBinaryPredicateOperator.IMPLICATION)));
    String input =
        "Sorted<T>(T* a, integer i, integer j) = \\forall integer k, l ; i <= k < l <= j ==> a[k] <"
            + " a[l]";

    testLogicalFunctionParsing(input, output);
  }

  @Test
  public void parseIsPositiveLogicalPredicateDeclaration() throws AcslParseException {
    /*
    This example was taken from ANSI/ISO C Specification Language Version 1.23 §2.61 Example 2.40.
     */
    AcslParameterDeclaration i =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i");
    AcslPredicateDeclaration declaration =
        new AcslPredicateDeclaration(
            FileLocation.DUMMY,
            new AcslPredicateType(ImmutableList.of(AcslBuiltinLogicType.INTEGER), false),
            "is_positive",
            "is_positive",
            ImmutableList.of(),
            ImmutableList.of(i));
    AcslBinaryTermPredicate body =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            new AcslIdTerm(FileLocation.DUMMY, i),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
            AcslBinaryTermExpressionOperator.GREATER_EQUAL);
    AcslLogicPredicateDefinition output =
        new AcslLogicPredicateDefinition(FileLocation.DUMMY, declaration, body);
    String input = "predicate is_positive(integer i) = i >= 0;";
    testLogicalFunctionParsing(input, output);
  }

  @Test
  public void parseIsFalseLogicPredicate() {
    // Logic definitions that take boolean parameters are not supported.
    String definition1 = "predicate is_false(boolean p) = (!p) == \\true;";
    String definition2 = "predicate is_false(boolean p) = !p;";
    AcslScope acslScope = getAcslScope();
    assertThrows(
        NullPointerException.class,
        () -> AcslParser.parseLogicalDefinition(definition1, acslScope, FileLocation.DUMMY));
    assertThrows(
        NullPointerException.class,
        () -> AcslParser.parseLogicalDefinition(definition2, acslScope, FileLocation.DUMMY));
  }

  @Test
  public void parseIsFalseTermPredicate() throws AcslParseException {
    String definition = "predicate is_false(boolean p) = !(p == \\true);";
    AcslParameterDeclaration p =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.BOOLEAN, "p");
    AcslPredicateDeclaration declaration =
        new AcslPredicateDeclaration(
            FileLocation.DUMMY,
            new AcslPredicateType(ImmutableList.of(AcslBuiltinLogicType.BOOLEAN), false),
            "is_false",
            "is_false",
            ImmutableList.of(),
            ImmutableList.of(p));
    AcslPredicate body =
        new AcslUnaryPredicate(
            FileLocation.DUMMY,
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                new AcslIdTerm(FileLocation.DUMMY, p),
                new AcslBooleanLiteralTerm(FileLocation.DUMMY, true),
                AcslBinaryTermExpressionOperator.EQUALS),
            AcslUnaryExpressionOperator.NEGATION);
    AcslLogicPredicateDefinition expected =
        new AcslLogicPredicateDefinition(FileLocation.DUMMY, declaration, body);
    testLogicalFunctionParsing(definition, expected);
  }

  @Test
  public void parseIsPositiveLogicFunction() throws AcslParseException {
    /*
    This example was taken from ANSI/ISO C Specification Language Version 1.23 §2.61 Example 2.40.
     */
    String input = "logic integer is_positive (integer i) = i >= 0 ? 1 : 0;";
    AcslParameterDeclaration i =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i");
    AcslFunctionDeclaration declaration =
        new AcslFunctionDeclaration(
            FileLocation.DUMMY,
            new AcslFunctionType(
                AcslBuiltinLogicType.INTEGER,
                ImmutableList.of(AcslBuiltinLogicType.INTEGER),
                false),
            "is_positive",
            "is_positive",
            ImmutableList.of(),
            ImmutableList.of(i));
    AcslTerm body =
        new AcslTernaryTerm(
            FileLocation.DUMMY,
            new AcslBinaryTermPredicate(
                FileLocation.DUMMY,
                new AcslIdTerm(FileLocation.DUMMY, i),
                new AcslIntegerLiteralTerm(
                    FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
                AcslBinaryTermExpressionOperator.GREATER_EQUAL),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ONE),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO));
    AcslLogicFunctionDefinition output =
        new AcslLogicFunctionDefinition(FileLocation.DUMMY, declaration, body);
    testLogicalFunctionParsing(input, output);
  }

  // Predicate for an SLL without any values that is null terminated and starts at the beginning of
  // the list
  @Test
  @Ignore
  public void memSafetySimpleSllPredicateTest() throws AcslParseException {
    /*
     * Abstraction predicate 'pred_sll' for a Singly-Linked-List (SLL) of type 'sll' defined as:
     *
     * struct sll {
     *   struct sll *next;
     * };
     *
     * With the following C code creating the list with the predicate (loop invariant) 'pred_sll':
     *
     * struct sll* create(void) {
     *   struct sll *sll = alloc_and_zero();
     *   struct sll *now = sll;
     *   while(random()) {
     *     now->next = alloc_and_zero();
     *     now = now->next;
     *   }
     *   return sll;
     * }
     *
     * // Case 1 is the end of list case, i.e. end address reached and next pointer is 0
     * pred_sll(sll *start, sll *end, int size)
     *   match size:
     *     case 1: start != 0 && start->next == 0 && start == end && start != 0
     *     case n + 1: start != 0 && start->next != start && start->next != 0 &&
     *                 pred_sll(start->next, end, size - 1)
     *
     * Translates to:
     * pred_sll(sll * start, sll * end, int size):
     *   size == 1 ? start != 0 && start->next == 0 && start == end &&
     *     canAccess(start) && canAccess(start->next) && canAccess(end)
     *   : start != 0 && start->next != start && start->next != 0 &&
     *     canAccess(start) && canAccess(start->next) &&
     *     canAccess(end) && start @ start->next &&
     *     pred_sll(start->next, end, size - 1)
     *
     * Needs to be called in our example above like this:
     * pred_sll(now, sll, ?)
     *
     * Note: canAccess(end) in the recursive case is only needed because of the recursive function call
     */

    CCompositeType sllCType =
        new CCompositeType(CTypeQualifiers.NONE, ComplexTypeKind.STRUCT, "sll", "sll");
    CPointerType sllCPointerType = new CPointerType(CTypeQualifiers.NONE, sllCType);
    sllCType.setMembers(
        ImmutableList.of(new CCompositeTypeMemberDeclaration(sllCPointerType, "next")));

    // The 'start' and 'end' parameters can be seen as new variables in C, like in regular fun calls
    CParameterDeclaration startCParamDecl =
        new CParameterDeclaration(FileLocation.DUMMY, sllCPointerType, "start");
    startCParamDecl.setQualifiedName("pred_sll::start");
    CIdExpression startCIdExpr = new CIdExpression(FileLocation.DUMMY, startCParamDecl);
    CParameterDeclaration endCParamDecl =
        new CParameterDeclaration(FileLocation.DUMMY, sllCPointerType, "end");
    endCParamDecl.setQualifiedName("pred_sll::end");
    CIdExpression endCIdExpr = new CIdExpression(FileLocation.DUMMY, endCParamDecl);

    // start->next
    CFieldReference startCNextFieldDeref =
        new CFieldReference(FileLocation.DUMMY, sllCType, "next", startCIdExpr, true);
    // start->next == 0
    CBinaryExpression sllNextEqualsZero =
        new CBinaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            sllCPointerType,
            startCNextFieldDeref,
            new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, BigInteger.ZERO),
            BinaryOperator.EQUALS);
    // start->next != 0
    CBinaryExpression sllNextNotEqualsZero =
        new CBinaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            sllCPointerType,
            startCNextFieldDeref,
            new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, BigInteger.ZERO),
            BinaryOperator.NOT_EQUALS);
    // start->next != start
    CBinaryExpression sllNextNotEqualsStart =
        new CBinaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            sllCPointerType,
            startCNextFieldDeref,
            startCIdExpr,
            BinaryOperator.NOT_EQUALS);
    // start == end (pointer equality)
    CBinaryExpression startEqualsEnd =
        new CBinaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            sllCPointerType,
            startCIdExpr,
            endCIdExpr,
            BinaryOperator.EQUALS);
    // start != 0 (pointer equality)
    CBinaryExpression startNotEqualsZero =
        new CBinaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            sllCPointerType,
            startCIdExpr,
            new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, BigInteger.ZERO),
            BinaryOperator.NOT_EQUALS);

    AcslCType sllPointerAcslType = new AcslCType(sllCPointerType);

    // TODO: getName() or .getQualifiedName() as name for the AcslCParameterDeclarations?
    AcslCParameterDeclaration startAcslCParamDecl =
        new AcslCParameterDeclaration(
            FileLocation.DUMMY, sllPointerAcslType, startCParamDecl.getName(), startCParamDecl);
    AcslCParameterDeclaration endAcslCParamDecl =
        new AcslCParameterDeclaration(
            FileLocation.DUMMY, sllPointerAcslType, startCParamDecl.getName(), endCParamDecl);
    AcslCIdExpression endAcslCIdExpr =
        new AcslCIdExpression(FileLocation.DUMMY, sllPointerAcslType, endCIdExpr);

    AcslParameterDeclaration size =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "size");

    AcslCLeftHandSideTerm startCNextFieldDerefTerm =
        new AcslCLeftHandSideTerm(
            FileLocation.DUMMY, startAcslCParamDecl.getType(), startCNextFieldDeref);
    AcslCExpressionTerm sllNextEqualsZeroTerm =
        new AcslCExpressionTerm(
            FileLocation.DUMMY, AcslBuiltinLogicType.BOOLEAN, sllNextEqualsZero);
    AcslCExpressionTerm sllNextNotEqualsZeroTerm =
        new AcslCExpressionTerm(
            FileLocation.DUMMY, AcslBuiltinLogicType.BOOLEAN, sllNextNotEqualsZero);
    AcslCExpressionTerm sllNextNotEqualsStartTerm =
        new AcslCExpressionTerm(
            FileLocation.DUMMY, AcslBuiltinLogicType.BOOLEAN, sllNextNotEqualsStart);
    AcslCExpressionTerm startEqualsEndTerm =
        new AcslCExpressionTerm(FileLocation.DUMMY, AcslBuiltinLogicType.BOOLEAN, startEqualsEnd);
    AcslCExpressionTerm startNotEqualsZeroTerm =
        new AcslCExpressionTerm(
            FileLocation.DUMMY, AcslBuiltinLogicType.BOOLEAN, startNotEqualsZero);

    // Note on Acsl expressions vs C expressions here: Acsl is purely mathematical (i.e. unbounded
    // integers), C expressions are bound to the C types. As long as there is no C bound upper limit
    // to the list length, this does not really matter.
    AcslIdTerm sizeIdTerm = new AcslIdTerm(FileLocation.DUMMY, size);
    AcslBinaryTerm sizeMinusOne =
        new AcslBinaryTerm(
            FileLocation.DUMMY,
            sizeIdTerm.getExpressionType(),
            sizeIdTerm,
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, sizeIdTerm.getExpressionType(), BigInteger.ONE),
            AcslBinaryTermOperator.MINUS);

    AcslPredicateDeclaration sllPredicateDeclaration =
        new AcslPredicateDeclaration(
            FileLocation.DUMMY,
            // Function type:
            new AcslPredicateType(
                ImmutableList.of(
                    startAcslCParamDecl.getType(), endAcslCParamDecl.getType(), size.getType()),
                false),
            "pred_sll",
            "pred_sll",
            // We don't want polymorphic types for MemSafety
            ImmutableList.of(),
            // Parameters:
            ImmutableList.of(startAcslCParamDecl, endAcslCParamDecl, size));

    AcslSeparateMemoryConjunctionPredicate startSepEndPredicate =
        new AcslSeparateMemoryConjunctionPredicate(FileLocation.DUMMY, startCIdExpr, endCIdExpr);
    AcslCanAccessPredicate canAccessStart =
        new AcslCanAccessPredicate(FileLocation.DUMMY, startCIdExpr);
    AcslCanAccessPredicate canAccessStartNext =
        new AcslCanAccessPredicate(FileLocation.DUMMY, startCNextFieldDeref);
    AcslCanAccessPredicate canAccessEnd =
        new AcslCanAccessPredicate(FileLocation.DUMMY, endCIdExpr);

    AcslPredicate canAccessPredicates =
        new AcslBinaryPredicate(
            FileLocation.DUMMY,
            new AcslBinaryPredicate(
                FileLocation.DUMMY,
                canAccessStart,
                canAccessStartNext,
                AcslBinaryPredicateOperator.AND),
            canAccessEnd,
            AcslBinaryPredicateOperator.AND);

    AcslPredicate canAccessPredicatesWithSepConj =
        new AcslBinaryPredicate(
            FileLocation.DUMMY,
            canAccessPredicates,
            startSepEndPredicate,
            AcslBinaryPredicateOperator.AND);

    AcslAstNode expectedOutput =
        new AcslLogicPredicateDefinition(
            FileLocation.DUMMY,
            // Function Declaration
            sllPredicateDeclaration,
            // Function body
            new AcslTernaryPredicate(
                FileLocation.DUMMY,
                // ITE condition: size == 1
                new AcslBinaryTermPredicate(
                    FileLocation.DUMMY,
                    new AcslIdTerm(FileLocation.DUMMY, size),
                    new AcslIntegerLiteralTerm(
                        FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ONE),
                    AcslBinaryTermExpressionOperator.EQUALS),
                // If branch: start->next == 0 && start == end && && start != 0 &&  canAccess(start)
                //            && canAccess(end) && canAccess(start->next)
                new AcslBinaryPredicate(
                    FileLocation.DUMMY,
                    new AcslPredicateTerm(sllNextEqualsZeroTerm),
                    new AcslBinaryPredicate(
                        FileLocation.DUMMY,
                        new AcslPredicateTerm(startEqualsEndTerm),
                        canAccessPredicates,
                        AcslBinaryPredicateOperator.AND),
                    AcslBinaryPredicateOperator.AND),
                // Else branch: start != 0 && start->next != start && start->next != 0
                //               && canAccess(start) && canAccess(start->next) && canAccess(end)
                //               && start @ end && start != end && end != null
                //               && pred_sll(start->next, end, size - 1)
                new AcslBinaryPredicate(
                    FileLocation.DUMMY,
                    new AcslPredicateTerm(startNotEqualsZeroTerm),
                    new AcslBinaryPredicate(
                        FileLocation.DUMMY, /* start->next != start */
                        new AcslPredicateTerm(sllNextNotEqualsStartTerm),
                        new AcslBinaryPredicate(
                            FileLocation.DUMMY, /* start->next != 0 */
                            new AcslBinaryPredicate(
                                FileLocation.DUMMY,
                                new AcslPredicateTerm(sllNextNotEqualsZeroTerm),
                                canAccessPredicatesWithSepConj,
                                AcslBinaryPredicateOperator.AND),
                            /* pred_sll(start->next, end, size - 1) */
                            new AcslPredicateApplicationPredicate(
                                FileLocation.DUMMY,
                                sllPredicateDeclaration,
                                ImmutableList.of(
                                    /*start->next*/ startCNextFieldDerefTerm,
                                    /* end */ endAcslCIdExpr,
                                    sizeMinusOne)),
                            AcslBinaryPredicateOperator.AND),
                        AcslBinaryPredicateOperator.AND),
                    AcslBinaryPredicateOperator.AND)));

    String input = "TODO";

    testLogicalFunctionParsing(input, expectedOutput);
  }

  // TODO: Predicate for an DLL without any values that is null terminated and starts at the
  // beginning of the list

  // TODO: Predicate for an DLL without any values that is null terminated and starts at the end of
  // the list

  // TODO: Predicate for an SLL with a single value shared in all segments that is null terminated
  // and starts at the beginning of the list

  // TODO: Predicate for an DLL with a single value shared in all segments that is null terminated
  // and starts at the beginning of the list

  // TODO: Predicate for an DLL with a single value shared in all segments that is null terminated
  // and starts at the end of the list

  // TODO: Predicate for an SLL with a single value that is a new nondet in all segments that is
  // null terminated and starts at the beginning of the list

  // TODO: Predicate for an DLL with a single value that is a new nondet in all segments that is
  // null terminated and starts at the beginning of the list

  // TODO: Predicate for an DLL with a single value that is a new nondet in all segments that is
  // null terminated and starts at the end of the list

  // TODO: more tests with looping lists
  // TODO: more tests with pointer offsets that need to be taken into account (linux style
  // linked-list)
}
