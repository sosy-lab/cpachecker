// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
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
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLogicType;
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
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTypeVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslParser.AcslParseException;

public class AcslParserLogicalDefinitionsTest {

  private AcslScope getAcslScope() {
    AcslScope scope = AcslScope.mutableCopy(AcslScope.empty());

    return scope;
  }

  private void testLogicalFunctionParsing(String input, AcslAstNode output)
      throws AcslParseException {

    AcslScope acslScope = getAcslScope();

    AcslAstNode parsed = AcslParser.parseLogicalDefinition(input, acslScope);
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
        "Sorted<T>(T* a, integer i, integer j) = \\forall integer k, l ; i <= k < l <= j ==> a[k] < a[l]";

    testLogicalFunctionParsing(input, output);
  }
}
