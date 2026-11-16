// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl.antlr;

import static com.google.common.truth.Truth.assertThat;
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
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate.AcslBinaryTermPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCanAccessPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionCallTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicFunctionDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicPredicateDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateApplicationPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslQuantifiedPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslQuantifiedPredicate.AcslQuantifier;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslSeparateMemoryConjunctionPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTypeVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate.AcslUnaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLogicDefinitionAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.AcslParser.AcslParseException;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslCType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslFunctionType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslPointerType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslPolymorphicType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslPredicateType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;

public class AcslParserLogicalDefinitionsTest {

  private AcslScope getAcslScope() {
    return AcslScope.empty();
  }

  private void testLogicalFunctionParsing(String input, AcslAstNode output)
      throws AcslParseException {

    AcslScope acslScope = getAcslScope();

    AcslLogicDefinition parsed =
        AcslParser.parseLogicalDefinition(input, acslScope, FileLocation.DUMMY);
    assertThat(parsed).isEqualTo(output);

    String printed = new AcslLogicDefinitionAnnotation(FileLocation.DUMMY, parsed).toAstString();
    assertThat(AcslParser.parseLogicalDefinition(printed, acslScope, FileLocation.DUMMY))
        .isEqualTo(parsed);
  }

  @Test
  public void parseMaxArrayLogicalFunctionDeclaration() throws AcslParseException {

    AcslPolymorphicType polymorphicType = new AcslPolymorphicType("T");
    AcslPointerType pointerType = new AcslPointerType(polymorphicType);
    AcslParameterDeclaration inputArray =
        new AcslParameterDeclaration(FileLocation.DUMMY, pointerType, "a", "MaxArray");
    AcslParameterDeclaration inputIndex =
        new AcslParameterDeclaration(
            FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i", "MaxArray");
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
                    AcslBinaryTermPredicateOperator.EQUALS),
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
                            maxArrayDeclaration.getType().getReturnType(),
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
                        AcslBinaryTermPredicateOperator.GREATER_THAN),
                    // True branch
                    new AcslArraySubscriptTerm(
                        FileLocation.DUMMY,
                        polymorphicType,
                        new AcslIdTerm(FileLocation.DUMMY, inputArray),
                        new AcslIdTerm(FileLocation.DUMMY, inputIndex)),
                    // False branch
                    new AcslFunctionCallTerm(
                        FileLocation.DUMMY,
                        maxArrayDeclaration.getType().getReturnType(),
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
            + " MaxArray(a, i - 1));";

    testLogicalFunctionParsing(input, output);
  }

  @Test
  public void parseMinArrayLogicalFunctionDeclaration() throws AcslParseException {

    AcslPolymorphicType polymorphicType = new AcslPolymorphicType("T");
    AcslPointerType pointerType = new AcslPointerType(polymorphicType);
    AcslParameterDeclaration inputArray =
        new AcslParameterDeclaration(FileLocation.DUMMY, pointerType, "a", "MinArray");
    AcslParameterDeclaration inputIndex =
        new AcslParameterDeclaration(
            FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i", "MinArray");
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
                    AcslBinaryTermPredicateOperator.EQUALS),
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
                            minArrayDeclaration.getType().getReturnType(),
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
                        AcslBinaryTermPredicateOperator.LESS_THAN),
                    // True branch
                    new AcslArraySubscriptTerm(
                        FileLocation.DUMMY,
                        polymorphicType,
                        new AcslIdTerm(FileLocation.DUMMY, inputArray),
                        new AcslIdTerm(FileLocation.DUMMY, inputIndex)),
                    // False branch
                    new AcslFunctionCallTerm(
                        FileLocation.DUMMY,
                        minArrayDeclaration.getType().getReturnType(),
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
            + " MinArray(a, i - 1));";

    testLogicalFunctionParsing(input, output);
  }

  @Test
  public void parseSumArrayLogicalFunctionDeclaration() throws AcslParseException {

    AcslPolymorphicType polymorphicType = new AcslPolymorphicType("T");
    AcslPointerType pointerType = new AcslPointerType(polymorphicType);
    AcslParameterDeclaration inputArray =
        new AcslParameterDeclaration(FileLocation.DUMMY, pointerType, "a", "SumArray");
    AcslParameterDeclaration inputIndex =
        new AcslParameterDeclaration(
            FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i", "SumArray");
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
                    AcslBinaryTermPredicateOperator.EQUALS),
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
                        sumArrayDeclaration.getType().getReturnType(),
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
    String input = "T SumArray<T>(T* a,integer i) = (i == 0) ? a[0] : a[i] + SumArray(a, i - 1);";

    testLogicalFunctionParsing(input, output);
  }

  @Test
  public void parseFibLogicalFunctionDeclaration() throws AcslParseException {
    AcslParameterDeclaration inputValue =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i", "Fib");
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
                        AcslBinaryTermPredicateOperator.EQUALS),
                    new AcslBinaryTermPredicate(
                        FileLocation.DUMMY,
                        new AcslIdTerm(FileLocation.DUMMY, inputValue),
                        AcslIntegerLiteralTerm.ONE,
                        AcslBinaryTermPredicateOperator.EQUALS),
                    AcslBinaryPredicateOperator.OR),
                // True branch
                new AcslIdTerm(FileLocation.DUMMY, inputValue),
                // False branch
                new AcslBinaryTerm(
                    FileLocation.DUMMY,
                    fibDeclaration.getType().getReturnType(),
                    new AcslFunctionCallTerm(
                        FileLocation.DUMMY,
                        fibDeclaration.getType().getReturnType(),
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
                        fibDeclaration.getType().getReturnType(),
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
    String input = "integer Fib(integer i) = (i == 0) || (i == 1) ? i : Fib(i - 1) + Fib(i - 2);";

    testLogicalFunctionParsing(input, output);
  }

  @Test
  public void parseSortedLogicalPredicateDeclaration() throws AcslParseException {
    AcslPolymorphicType polymorphicType = new AcslPolymorphicType("T");
    AcslPointerType pointerType = new AcslPointerType(polymorphicType);
    AcslParameterDeclaration inputArray =
        new AcslParameterDeclaration(FileLocation.DUMMY, pointerType, "a", "Sorted");
    AcslParameterDeclaration lowerLimit =
        new AcslParameterDeclaration(
            FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i", "Sorted");
    AcslParameterDeclaration upperLimit =
        new AcslParameterDeclaration(
            FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "j", "Sorted");
    AcslParameterDeclaration firstIterator =
        new AcslParameterDeclaration(
            FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "k", "Sorted");
    AcslParameterDeclaration secondIterator =
        new AcslParameterDeclaration(
            FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "l", "Sorted");

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
            new AcslQuantifiedPredicate(
                FileLocation.DUMMY,
                AcslQuantifier.FORALL,
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
                            AcslBinaryTermPredicateOperator.LESS_EQUAL),
                        new AcslBinaryPredicate(
                            FileLocation.DUMMY,
                            new AcslBinaryTermPredicate(
                                FileLocation.DUMMY,
                                new AcslIdTerm(FileLocation.DUMMY, firstIterator),
                                new AcslIdTerm(FileLocation.DUMMY, secondIterator),
                                AcslBinaryTermPredicateOperator.LESS_THAN),
                            new AcslBinaryTermPredicate(
                                FileLocation.DUMMY,
                                new AcslIdTerm(FileLocation.DUMMY, lowerLimit),
                                new AcslIdTerm(FileLocation.DUMMY, firstIterator),
                                AcslBinaryTermPredicateOperator.LESS_EQUAL),
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
                        AcslBinaryTermPredicateOperator.LESS_THAN),
                    AcslBinaryPredicateOperator.IMPLICATION)));
    String input =
        "Sorted<T>(T* a, integer i, integer j) = \\forall integer k, l ; i <= k < l <= j ==> a[k] <"
            + " a[l];";

    testLogicalFunctionParsing(input, output);
  }

  @Test
  public void parseIsPositiveLogicalPredicateDeclaration() throws AcslParseException {
    /*
    This example was taken from ANSI/ISO C Specification Language Version 1.23 §2.61 Example 2.40.
     */
    AcslParameterDeclaration i =
        new AcslParameterDeclaration(
            FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i", "is_positive");
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
            AcslBinaryTermPredicateOperator.GREATER_EQUAL);
    AcslLogicPredicateDefinition output =
        new AcslLogicPredicateDefinition(FileLocation.DUMMY, declaration, body);
    String input = "predicate is_positive(integer i) = i >= 0;";
    testLogicalFunctionParsing(input, output);
  }

  @Test
  public void parseIsFalseLogicPredicate() throws AcslParseException {
    AcslScope acslScope = getAcslScope();
    // The grammar does not allow a negated predicate as term
    assertThrows(
        AcslParseException.class,
        () ->
            AcslParser.parseLogicalDefinition(
                "predicate is_false(boolean p) = (!p) == \\true;", acslScope, FileLocation.DUMMY));
    // A boolean term as predicate holds if it is true
    assertThat(
            AcslParser.parseLogicalDefinition(
                "predicate is_false(boolean p) = !p;", acslScope, FileLocation.DUMMY))
        .isEqualTo(
            AcslParser.parseLogicalDefinition(
                "predicate is_false(boolean p) = !(p == \\true);", acslScope, FileLocation.DUMMY));
  }

  @Test
  public void parseIsFalseTermPredicate() throws AcslParseException {
    String definition = "predicate is_false(boolean p) = !(p == \\true);";
    AcslParameterDeclaration p =
        new AcslParameterDeclaration(
            FileLocation.DUMMY, AcslBuiltinLogicType.BOOLEAN, "p", "is_false");
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
                AcslBinaryTermPredicateOperator.EQUALS),
            AcslUnaryPredicateOperator.NEGATION);
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
        new AcslParameterDeclaration(
            FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "i", "is_positive");
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
                AcslBinaryTermPredicateOperator.GREATER_EQUAL),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ONE),
            new AcslIntegerLiteralTerm(
                FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO));
    AcslLogicFunctionDefinition output =
        new AcslLogicFunctionDefinition(FileLocation.DUMMY, declaration, body);
    testLogicalFunctionParsing(input, output);
  }

  // TODO: implement this test and remove the suppression
  // Parse predicate definition 'pred_sll'
  // from witnesses 'sll_blank-32-valid-witness-v2--1.yml'
  // and 'sll_blank-64-valid-witness-v2--1.yml'
  // for program 'sll_blank.c' in ILP32 and LP64
  @SuppressWarnings("all")
  @Test
  @Ignore
  public void memSafetySllBlank1PredicateTest() throws AcslParseException {
    AcslAstNode expectedOutput = getPredSllForSllBlank();

    String input = "TODO";

    testLogicalFunctionParsing(input, expectedOutput);
  }

  // TODO: implement this test and remove the suppression
  // Parse predicate definitions 'pred_sll' and 'pred_sll2'
  // from witnesses 'sll_blank-32-valid-witness-v2--2.yml'
  // and 'sll_blank-64-valid-witness-v2--2.yml'
  // for program 'sll_blank.c' in ILP32 and LP64
  @SuppressWarnings("all")
  @Test
  @Ignore
  public void memSafetySllBlank2PredicateTest() throws AcslParseException {
    AcslAstNode expectedOutput1 = getPredSllForSllBlank();
    AcslAstNode expectedOutput2 = getPredSll2ForSllBlank();

    String input = "TODO";

    testLogicalFunctionParsing(input, null);
  }

  // Generates the predicate definition 'pred_sll(start, end)' for the witnesses for the C program
  // 'sll_blank.c'
  private static AcslAstNode getPredSllForSllBlank() {
    CCompositeType sllCType =
        new CCompositeType(CTypeQualifiers.NONE, ComplexTypeKind.STRUCT, "sll", "sll");
    CPointerType sllCPointerType = new CPointerType(CTypeQualifiers.NONE, sllCType);
    sllCType.setMembers(
        ImmutableList.of(new CCompositeTypeMemberDeclaration(sllCPointerType, "next")));

    AcslCType sllPointerAcslType = new AcslCType(sllCPointerType);

    // The 'start' and 'end' parameters can be seen as new variables in C, like in regular fun calls
    AcslParameterDeclaration startAcslParamDecl =
        new AcslParameterDeclaration(FileLocation.DUMMY, sllPointerAcslType, "start", "pred_sll");
    AcslParameterDeclaration endAcslParamDecl =
        new AcslParameterDeclaration(FileLocation.DUMMY, sllPointerAcslType, "end", "pred_sll");
    AcslTerm startTerm = new AcslIdTerm(FileLocation.DUMMY, startAcslParamDecl);
    AcslTerm endTerm = new AcslIdTerm(FileLocation.DUMMY, endAcslParamDecl);
    // The parameters as C expressions, e.g. for start->next
    CIdExpression startCIdExpr =
        new CIdExpression(
            FileLocation.DUMMY, startAcslParamDecl.asCParameterDeclaration().orElseThrow());
    CIdExpression endCIdExpr =
        new CIdExpression(
            FileLocation.DUMMY, endAcslParamDecl.asCParameterDeclaration().orElseThrow());

    // start->next
    CFieldReference startCNextFieldDeref =
        new CFieldReference(FileLocation.DUMMY, sllCType, "next", startCIdExpr, true);
    AcslTerm startCNextFieldDerefTerm =
        new AcslCExpression(FileLocation.DUMMY, startCNextFieldDeref);
    AcslTerm zeroTerm =
        new AcslIntegerLiteralTerm(
            FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO);

    // start->next == 0
    AcslPredicate startNextEqualsZero =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            startCNextFieldDerefTerm,
            zeroTerm,
            AcslBinaryTermPredicateOperator.EQUALS);
    // start->next != 0
    AcslPredicate startNextNotEqualsZero =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            startCNextFieldDerefTerm,
            zeroTerm,
            AcslBinaryTermPredicateOperator.NOT_EQUALS);
    // start->next != start
    AcslPredicate startNextNotEqualsStart =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            startCNextFieldDerefTerm,
            startTerm,
            AcslBinaryTermPredicateOperator.NOT_EQUALS);
    // start == end
    AcslPredicate startEqualsEnd =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY, startTerm, endTerm, AcslBinaryTermPredicateOperator.EQUALS);
    // start != end
    AcslPredicate startNotEqualsEnd =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY, startTerm, endTerm, AcslBinaryTermPredicateOperator.NOT_EQUALS);
    // start != 0
    AcslPredicate startNotEqualsZero =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY, startTerm, zeroTerm, AcslBinaryTermPredicateOperator.NOT_EQUALS);
    // end != 0
    AcslPredicate endNotEqualsZero =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY, endTerm, zeroTerm, AcslBinaryTermPredicateOperator.NOT_EQUALS);

    AcslPredicateDeclaration sllPredicateDeclaration =
        new AcslPredicateDeclaration(
            FileLocation.DUMMY,
            // Function type:
            new AcslPredicateType(
                ImmutableList.of(startAcslParamDecl.getType(), endAcslParamDecl.getType()), false),
            "pred_sll",
            "pred_sll",
            // We don't want polymorphic types for MemSafety
            ImmutableList.of(),
            // Parameters:
            ImmutableList.of(startAcslParamDecl, endAcslParamDecl));

    AcslSeparateMemoryConjunctionPredicate startSepEndPredicate =
        new AcslSeparateMemoryConjunctionPredicate(
            FileLocation.DUMMY, ImmutableList.of(startCIdExpr, endCIdExpr));
    AcslSeparateMemoryConjunctionPredicate startNextSepStartPredicate =
        new AcslSeparateMemoryConjunctionPredicate(
            FileLocation.DUMMY, ImmutableList.of(startCNextFieldDeref, startCIdExpr));
    CExpression sizeOfSllCExpr =
        new CUnaryExpression(
            FileLocation.DUMMY, CNumericTypes.SIZE_T, startCIdExpr, UnaryOperator.SIZEOF);
    AcslCanAccessPredicate canAccessStart =
        new AcslCanAccessPredicate(FileLocation.DUMMY, startCIdExpr, sizeOfSllCExpr);
    AcslCanAccessPredicate canAccessStartNext =
        new AcslCanAccessPredicate(FileLocation.DUMMY, startCNextFieldDeref, sizeOfSllCExpr);
    AcslCanAccessPredicate canAccessEnd =
        new AcslCanAccessPredicate(FileLocation.DUMMY, endCIdExpr, sizeOfSllCExpr);

    AcslPredicate canAccessPredicatesStartAndEnd =
        new AcslBinaryPredicate(
            FileLocation.DUMMY, canAccessStart, canAccessEnd, AcslBinaryPredicateOperator.AND);

    return new AcslLogicPredicateDefinition(
        FileLocation.DUMMY,
        // Function Declaration
        sllPredicateDeclaration,
        // Function body (a disjunction with 2 big predicates). The first is:
        // start != 0 && (end != 0 && (start->next == 0 && (start == end &&
        //     (\\canAccess(start) && \\canAccess(end)))))
        new AcslBinaryPredicate(
            FileLocation.DUMMY,
            new AcslBinaryPredicate(
                FileLocation.DUMMY,
                startNotEqualsZero,
                new AcslBinaryPredicate(
                    FileLocation.DUMMY,
                    endNotEqualsZero,
                    new AcslBinaryPredicate(
                        FileLocation.DUMMY,
                        startNextEqualsZero,
                        new AcslBinaryPredicate(
                            FileLocation.DUMMY,
                            startEqualsEnd,
                            canAccessPredicatesStartAndEnd,
                            AcslBinaryPredicateOperator.AND),
                        AcslBinaryPredicateOperator.AND),
                    AcslBinaryPredicateOperator.AND),
                AcslBinaryPredicateOperator.AND),
            // second argument of || :
            // start != 0 && (start != end && (start->next != start && (start->next != 0
            // && (\\canAccess(start) && (\\canAccess(start->next) && (\\canAccess(end)
            // && ((start)@(start->next) && ((start)@(end) && pred_sll(start->next, end)))))))))
            new AcslBinaryPredicate(
                FileLocation.DUMMY,
                startNotEqualsZero,
                new AcslBinaryPredicate(
                    FileLocation.DUMMY,
                    startNotEqualsEnd,
                    new AcslBinaryPredicate(
                        FileLocation.DUMMY,
                        startNextNotEqualsStart,
                        new AcslBinaryPredicate(
                            FileLocation.DUMMY,
                            startNextNotEqualsZero,
                            new AcslBinaryPredicate(
                                FileLocation.DUMMY,
                                canAccessStart,
                                new AcslBinaryPredicate(
                                    FileLocation.DUMMY,
                                    canAccessStartNext,
                                    new AcslBinaryPredicate(
                                        FileLocation.DUMMY,
                                        canAccessEnd,
                                        new AcslBinaryPredicate(
                                            FileLocation.DUMMY,
                                            startNextSepStartPredicate,
                                            new AcslBinaryPredicate(
                                                FileLocation.DUMMY,
                                                startSepEndPredicate,
                                                /* pred_sll(start->next, end) */
                                                new AcslPredicateApplicationPredicate(
                                                    FileLocation.DUMMY,
                                                    sllPredicateDeclaration,
                                                    ImmutableList.of(
                                                        /*start->next*/ startCNextFieldDerefTerm,
                                                        /* end */ endTerm)),
                                                AcslBinaryPredicateOperator.AND),
                                            AcslBinaryPredicateOperator.AND),
                                        AcslBinaryPredicateOperator.AND),
                                    AcslBinaryPredicateOperator.AND),
                                AcslBinaryPredicateOperator.AND),
                            AcslBinaryPredicateOperator.AND),
                        AcslBinaryPredicateOperator.AND),
                    AcslBinaryPredicateOperator.AND),
                AcslBinaryPredicateOperator.AND),
            AcslBinaryPredicateOperator.OR));
  }

  // Generates the predicate definition 'pred_sll2(start)' for the witnesses for the C program
  // 'sll_blank.c'
  private static AcslAstNode getPredSll2ForSllBlank() {
    CCompositeType sllCType =
        new CCompositeType(CTypeQualifiers.NONE, ComplexTypeKind.STRUCT, "sll", "sll");
    CPointerType sllCPointerType = new CPointerType(CTypeQualifiers.NONE, sllCType);
    sllCType.setMembers(
        ImmutableList.of(new CCompositeTypeMemberDeclaration(sllCPointerType, "next")));

    AcslCType sllPointerAcslType = new AcslCType(sllCPointerType);

    // The 'start' parameter can be seen as new variable in C, like in regular fun calls
    AcslParameterDeclaration startAcslParamDecl =
        new AcslParameterDeclaration(FileLocation.DUMMY, sllPointerAcslType, "start", "pred_sll2");
    AcslTerm startTerm = new AcslIdTerm(FileLocation.DUMMY, startAcslParamDecl);
    // The parameter as C expression, e.g. for start->next
    CIdExpression startCIdExpr =
        new CIdExpression(
            FileLocation.DUMMY, startAcslParamDecl.asCParameterDeclaration().orElseThrow());

    // start->next
    CFieldReference startCNextFieldDeref =
        new CFieldReference(FileLocation.DUMMY, sllCType, "next", startCIdExpr, true);
    AcslTerm startCNextFieldDerefTerm =
        new AcslCExpression(FileLocation.DUMMY, startCNextFieldDeref);
    AcslTerm zeroTerm =
        new AcslIntegerLiteralTerm(
            FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO);

    // start->next == 0
    AcslPredicate startNextEqualsZero =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            startCNextFieldDerefTerm,
            zeroTerm,
            AcslBinaryTermPredicateOperator.EQUALS);
    // start->next != 0
    AcslPredicate startNextNotEqualsZero =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            startCNextFieldDerefTerm,
            zeroTerm,
            AcslBinaryTermPredicateOperator.NOT_EQUALS);
    // start->next != start
    AcslPredicate startNextNotEqualsStart =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY,
            startCNextFieldDerefTerm,
            startTerm,
            AcslBinaryTermPredicateOperator.NOT_EQUALS);
    // start != 0
    AcslPredicate startNotEqualsZero =
        new AcslBinaryTermPredicate(
            FileLocation.DUMMY, startTerm, zeroTerm, AcslBinaryTermPredicateOperator.NOT_EQUALS);

    AcslPredicateDeclaration sll2PredicateDeclaration =
        new AcslPredicateDeclaration(
            FileLocation.DUMMY,
            // Function type:
            new AcslPredicateType(ImmutableList.of(startAcslParamDecl.getType()), false),
            "pred_sll2",
            "pred_sll2",
            // We don't want polymorphic types for MemSafety
            ImmutableList.of(),
            // Parameters:
            ImmutableList.of(startAcslParamDecl));

    AcslSeparateMemoryConjunctionPredicate startNextSepStartPredicate =
        new AcslSeparateMemoryConjunctionPredicate(
            FileLocation.DUMMY, ImmutableList.of(startCNextFieldDeref, startCIdExpr));
    CExpression sizeOfSllCExpr =
        new CUnaryExpression(
            FileLocation.DUMMY, CNumericTypes.SIZE_T, startCIdExpr, UnaryOperator.SIZEOF);
    AcslCanAccessPredicate canAccessStart =
        new AcslCanAccessPredicate(FileLocation.DUMMY, startCIdExpr, sizeOfSllCExpr);
    AcslCanAccessPredicate canAccessStartNext =
        new AcslCanAccessPredicate(FileLocation.DUMMY, startCNextFieldDeref, sizeOfSllCExpr);

    return new AcslLogicPredicateDefinition(
        FileLocation.DUMMY,
        // Function Declaration
        sll2PredicateDeclaration,
        // Function body (a disjunction with 2 big predicates):
        // start != 0 && (start->next != 0 && (start->next != start && ((start->next) @ (start)
        //   && (\\canAccess(start) && (\\canAccess(start->next) && pred_sll2(start->next))))))
        // || start != 0 && (start->next == 0 && \\canAccess(start))"
        new AcslBinaryPredicate(
            FileLocation.DUMMY,
            new AcslBinaryPredicate(
                FileLocation.DUMMY,
                startNotEqualsZero,
                new AcslBinaryPredicate(
                    FileLocation.DUMMY,
                    startNextNotEqualsZero,
                    new AcslBinaryPredicate(
                        FileLocation.DUMMY,
                        startNextNotEqualsStart,
                        new AcslBinaryPredicate(
                            FileLocation.DUMMY,
                            startNextSepStartPredicate,
                            new AcslBinaryPredicate(
                                FileLocation.DUMMY,
                                canAccessStart,
                                new AcslBinaryPredicate(
                                    FileLocation.DUMMY,
                                    canAccessStartNext,
                                    /* pred_sll2(start->next) */
                                    new AcslPredicateApplicationPredicate(
                                        FileLocation.DUMMY,
                                        sll2PredicateDeclaration,
                                        ImmutableList.of(/*start->next*/ startCNextFieldDerefTerm)),
                                    AcslBinaryPredicateOperator.AND),
                                AcslBinaryPredicateOperator.AND),
                            AcslBinaryPredicateOperator.AND),
                        AcslBinaryPredicateOperator.AND),
                    AcslBinaryPredicateOperator.AND),
                AcslBinaryPredicateOperator.AND),
            new AcslBinaryPredicate(
                FileLocation.DUMMY,
                startNotEqualsZero,
                new AcslBinaryPredicate(
                    FileLocation.DUMMY,
                    startNextEqualsZero,
                    canAccessStart,
                    AcslBinaryPredicateOperator.AND),
                AcslBinaryPredicateOperator.AND),
            AcslBinaryPredicateOperator.OR));
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
