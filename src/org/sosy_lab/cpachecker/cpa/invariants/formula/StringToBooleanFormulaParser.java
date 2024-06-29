// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import com.google.common.base.Splitter;

public class StringToBooleanFormulaParser {

    public static BooleanFormula<CompoundInterval> parseBooleanFormula(String formulaString) {
        ArrayDeque<Character> brackets = new ArrayDeque<>();

        return parseBooleanFormula(formulaString, brackets);

    }

    private static BooleanFormula<CompoundInterval>
            parseBooleanFormula(String formulaString, ArrayDeque<Character> brackets) {
        formulaString = formulaString.trim();
        if (formulaString.startsWith("(")
                && formulaString.endsWith(")")
                && findMatchingParenthesis(formulaString, 0) == formulaString.length() - 1) {
            formulaString = formulaString.substring(1, formulaString.length() - 1).trim();
        }

        int length = formulaString.length();
        for (int i = 0; i < length; i++) {
            char c = formulaString.charAt(i);
            if (c == '(') {
                brackets.push(c);
            } else if (c == ')') {
                if (brackets.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Unbalanced brackets in formula: " + formulaString);
                }
                brackets.pop();
            } else if (brackets.isEmpty()) {
                if (formulaString.substring(i).startsWith("&&")) {
                    return LogicalAnd.of(
                            parseBooleanFormula(formulaString.substring(0, i).trim(), brackets),
                            parseBooleanFormula(formulaString.substring(i + 2).trim(), brackets));
                } else if (formulaString.substring(i).startsWith("!")) {
                    return LogicalNot
                            .of(parseBooleanFormula(formulaString.substring(1).trim(), brackets));
                } else if (formulaString.substring(i).startsWith(":")) {
                    return Equal.of(
                            parseNumeralFormula(formulaString.substring(0, i).trim()),
                            parseNumeralFormula(formulaString.substring(i + 1).trim()));
                } else if (formulaString.substring(i).startsWith("<")) {
                    return LessThan.of(
                            parseNumeralFormula(formulaString.substring(0, i).trim()),
                            parseNumeralFormula(formulaString.substring(i + 1).trim()));
                }

            }
        }

        if (formulaString.equals("true")) {
            return BooleanConstant.getTrue();
        } else if (formulaString.equals("false")) {
            return BooleanConstant.getFalse();
        }
        return BooleanConstant.getTrue();

    }

    private static NumeralFormula<CompoundInterval> parseNumeralFormula(String formulaString) {
        formulaString = formulaString.trim();
        if (formulaString.startsWith("(")
                && formulaString.endsWith(")")
                && findMatchingParenthesis(formulaString, 0) == formulaString.length() - 1) {
            formulaString = formulaString.substring(1, formulaString.length() - 1).trim();
        }

        int length = formulaString.length();
        ArrayDeque<Character> brackets = new ArrayDeque<>();

        for (int i = 0; i < length; i++) {
            char c = formulaString.charAt(i);
            if (c == '(') {
                brackets.push(c);
            } else if (c == ')') {
                brackets.pop();
            } else if (brackets.isEmpty()) {
                if (formulaString.startsWith("+", i)) {
                    return Add.of(
                            parseNumeralFormula(formulaString.substring(0, i)),
                            parseNumeralFormula(formulaString.substring(i + 1)));
                } else if (formulaString.startsWith("*", i)) {
                    return Multiply.of(
                            parseNumeralFormula(formulaString.substring(0, i)),
                            parseNumeralFormula(formulaString.substring(i + 1)));
                } else if (formulaString.startsWith("/", i)) {
                    return Divide.of(
                            parseNumeralFormula(formulaString.substring(0, i)),
                            parseNumeralFormula(formulaString.substring(i + 1)));
                } else if (formulaString.startsWith("%", i)) {
                    return Modulo.of(
                            parseNumeralFormula(formulaString.substring(0, i)),
                            parseNumeralFormula(formulaString.substring(i + 1)));
                } else if (formulaString.startsWith("&", i)) {
                    return BinaryAnd.of(
                            parseNumeralFormula(formulaString.substring(0, i)),
                            parseNumeralFormula(formulaString.substring(i + 1)));
                } else if (formulaString.startsWith("|", i)) {
                    return BinaryOr.of(
                            parseNumeralFormula(formulaString.substring(0, i)),
                            parseNumeralFormula(formulaString.substring(i + 1)));
                } else if (formulaString.startsWith("^", i)) {
                    return BinaryXor.of(
                            parseNumeralFormula(formulaString.substring(0, i)),
                            parseNumeralFormula(formulaString.substring(i + 1)));
                } else if (formulaString.startsWith("<<", i)) {
                    return ShiftLeft.of(
                            parseNumeralFormula(formulaString.substring(0, i)),
                            parseNumeralFormula(formulaString.substring(i + 2)));
                } else if (formulaString.startsWith(">>", i)) {
                    return ShiftRight.of(
                            parseNumeralFormula(formulaString.substring(0, i)),
                            parseNumeralFormula(formulaString.substring(i + 2)));
                }
            }
        }
        if (formulaString.matches("(\\[\\d+,\\d+\\],?)+")) {
            return parseConstant(formulaString);
        }

        return Variable
                .of(BitVectorInfo.from(32, true), MemoryLocation.forIdentifier(formulaString));

    }

    private static int findMatchingParenthesis(String input, int openIndex) {
        int depth = 1;
        for (int i = openIndex + 1; i < input.length(); i++) {
            if (input.charAt(i) == '(') {
                depth++;
            } else if (input.charAt(i) == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static NumeralFormula<CompoundInterval> parseConstant(String input) {
        List<BitVectorInterval> bitVectorIntervals = new ArrayList<>();
        BitVectorInfo bitVectorInfo = BitVectorInfo.from(32, true);
        Iterable<String> intervalStrings = Splitter.onPattern("],\\[").split(input);

        for (String intervalString : intervalStrings) {
            intervalString = intervalString.replace("[", "").replace("]", "");
            List<String> bounds = Splitter.on(',').splitToList(intervalString);
            bitVectorIntervals.add(
                    BitVectorInterval.of(
                            bitVectorInfo,
                            new BigInteger(bounds.get(0)),
                            new BigInteger(bounds.get(1))));
        }
        CompoundBitVectorInterval compoundBitVectorInterval =
                CompoundBitVectorInterval.of(bitVectorIntervals.get(0));
        for (int i = 1; i < bitVectorIntervals.size(); i++) {
            compoundBitVectorInterval =
                    CompoundBitVectorInterval.logicalOr(
                            compoundBitVectorInterval,
                            CompoundBitVectorInterval.of(bitVectorIntervals.get(i)));
        }

        return Constant.of(bitVectorInfo, compoundBitVectorInterval);
    }

}
