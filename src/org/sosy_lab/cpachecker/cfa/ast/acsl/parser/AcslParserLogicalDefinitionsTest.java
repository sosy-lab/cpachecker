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
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCExpressionTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCLeftHandSideTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslForallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionCallPredicate;
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
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTypeVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslParser.AcslParseException;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;

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
        "Sorted<T>(T* a, integer i, integer j) = \\forall integer k, l ; i <= k < l <= j ==> a[k] <"
            + " a[l]";

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
     * pred_sll(sll * start, sll * end, int size)
     *   match size:
     *     case 1: start->next == 0 && start == end // End of list,
     *                                             // i.e. end address reached and next pointer is 0
     *     case n + 1: start != 0 && start->next != start && start->next != 0 &&
     *                 pred_sll(start->next, end, size - 1)
     *
     * Translates to:
     * pred_sll(sll * start, sll * end, int size):
     *   size == 1 ? start->next == 0 && start == end
     *   : start != 0 && start->next != start && start->next != 0
     *     && pred_sll(start->next, end, size - 1)
     */

    CCompositeType sllCType =
        new CCompositeType(CTypeQualifiers.NONE, ComplexTypeKind.STRUCT, "sll", "sll");
    CPointerType sllCPointerType = new CPointerType(CTypeQualifiers.NONE, sllCType);
    sllCType.setMembers(
        ImmutableList.of(new CCompositeTypeMemberDeclaration(sllCPointerType, "next")));

    // The C "variable" is the parameter 'start'
    // TODO: is this (and end) a CParameterDeclaration instead?
    CSimpleDeclaration startCVariable =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            sllCPointerType,
            "start",
            "start",
            "start",
            null);
    CIdExpression startCIdExpr = new CIdExpression(FileLocation.DUMMY, startCVariable);
    CSimpleDeclaration endCVariable =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            sllCPointerType,
            "end",
            "end",
            "end",
            null);
    CIdExpression endCIdExpr = new CIdExpression(FileLocation.DUMMY, endCVariable);

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
    AcslParameterDeclaration start =
        new AcslParameterDeclaration(FileLocation.DUMMY, sllPointerAcslType, "start");
    AcslParameterDeclaration end =
        new AcslParameterDeclaration(FileLocation.DUMMY, sllPointerAcslType, "end");
    AcslParameterDeclaration size =
        new AcslParameterDeclaration(FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, "size");

    AcslCLeftHandSideTerm startCNextFieldDerefTerm =
        new AcslCLeftHandSideTerm(FileLocation.DUMMY, start.getType(), startCNextFieldDeref);
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
                ImmutableList.of(start.getType(), end.getType(), size.getType()), false),
            "pred_sll",
            "pred_sll",
            // We don't want polymorphic types for MemSafety
            ImmutableList.of(),
            // Parameters:
            ImmutableList.of(start, end, size));

    // TODO: how to connect 2 boolean terms with logical operators, e.g. AND?
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
                // If branch: start->next == 0 && start == end
                new AcslBinaryPredicate(
                    FileLocation.DUMMY,
                    sllNextEqualsZeroTerm,
                    startEqualsEndTerm,
                    AcslBinaryPredicateOperator.AND),
                // Else branch: start != 0 && start->next != start && start->next != 0
                //               && pred_sll(start->next, end, size - 1)
                new AcslBinaryPredicate(
                    FileLocation.DUMMY,
                    startNotEqualsZeroTerm,
                    new AcslBinaryPredicate(
                        FileLocation.DUMMY, /* start->next != start */
                        sllNextNotEqualsStartTerm,
                        new AcslBinaryPredicate(
                            FileLocation.DUMMY, /* start->next != 0 */
                            sllNextNotEqualsZeroTerm,
                            /* pred_sll(start->next, end, size - 1) */
                            new AcslFunctionCallPredicate(
                                FileLocation.DUMMY,
                                new AcslIdTerm(FileLocation.DUMMY, sllPredicateDeclaration),
                                ImmutableList.of(
                                    /*start->next*/ startCNextFieldDerefTerm,
                                    new AcslIdTerm(FileLocation.DUMMY, end),
                                    sizeMinusOne),
                                sllPredicateDeclaration),
                            AcslBinaryPredicateOperator.AND),
                        AcslBinaryPredicateOperator.AND),
                    AcslBinaryPredicateOperator.AND)));

    // TODO: @Marian: ITE form or pattern matching? Or both? Both would be kinda cool.
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
