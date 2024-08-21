// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.cpa.invariants.formula;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.FloatingPointTypeInfo;
import org.sosy_lab.cpachecker.cpa.invariants.TypeInfo;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class StringToBooleanFormulaParser {

  public static BooleanFormula<CompoundInterval> parseBooleanFormula(String formulaString) {
    formulaString = formulaString.trim();
    formulaString = removeEnclosingParentheses(formulaString);

    switch (formulaString) {
      case "true":
        return BooleanConstant.getTrue();
      case "false":
        return BooleanConstant.getFalse();
      default:
        return parseComplexBooleanFormula(formulaString);
    }
  }

  private static BooleanFormula<CompoundInterval> parseComplexBooleanFormula(String formulaString) {
    ArrayDeque<Character> brackets = new ArrayDeque<>();
    int length = formulaString.length();

    for (int i = 0; i < length; i++) {
      char c = formulaString.charAt(i);
      if (c == '(') {
        brackets.push(c);
      } else if (c == ')') {
        if (brackets.isEmpty()) {
          throw new IllegalArgumentException("Mismatched parentheses in formula: " + formulaString);
        }
        brackets.pop();
      } else if (brackets.isEmpty()) {
        String remaining = formulaString.substring(i).trim();
        if (remaining.startsWith("&&.logicalAnd")) {
          return LogicalAnd.of(
              parseBooleanFormula(formulaString.substring(0, i).trim()),
              parseBooleanFormula(remaining.substring(13).trim()));
        } else if (remaining.startsWith("!.logicalNot")) {
          return LogicalNot.of(parseBooleanFormula(remaining.substring(12).trim()));
        } else if (remaining.startsWith("=.equal")) {
          return Equal.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(7).trim()));
        } else if (remaining.startsWith("<.lessThan")) {
          return LessThan.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(10).trim()));
        }
      }
    }
    throw new IllegalArgumentException("Unknown boolean formula: " + formulaString);
  }

  private static NumeralFormula<CompoundInterval> parseNumeralFormula(String formulaString) {
    formulaString = removeEnclosingParentheses(formulaString);

    if (formulaString.startsWith("cast -typeInfo>")) {
      return parseCast(formulaString);
    }

    if (formulaString.contains("?.if")) {
      return parseIfThenElse(formulaString);
    }

    return parseNumeralOperators(formulaString);
  }

  private static NumeralFormula<CompoundInterval> parseCast(String formulaString) {
    int typeInfoStartIndex = formulaString.indexOf("-typeInfo>") + 10;
    int typeInfoEndIndex = formulaString.indexOf('(', typeInfoStartIndex);
    String typeInfoString = formulaString.substring(typeInfoStartIndex, typeInfoEndIndex).trim();
    TypeInfo typeInfo = parseTypeInfo(typeInfoString);
    int castEndIndex = findMatchingParenthesis(formulaString, typeInfoEndIndex);
    String castedFormula = formulaString.substring(typeInfoEndIndex + 1, castEndIndex).trim();

    return Cast.of(typeInfo, parseNumeralFormula(castedFormula));
  }

  private static NumeralFormula<CompoundInterval> parseIfThenElse(String formulaString) {
    int ifIndex = formulaString.indexOf("?.if");
    int elseIndex = formulaString.indexOf(":.else", ifIndex);
    String condition = formulaString.substring(0, ifIndex).trim();
    String ifCase = formulaString.substring(ifIndex + 5, elseIndex).trim();
    String elseCase = formulaString.substring(elseIndex + 7).trim();

    return IfThenElse.of(
        parseBooleanFormula(condition), parseNumeralFormula(ifCase), parseNumeralFormula(elseCase));
  }

  private static NumeralFormula<CompoundInterval> parseNumeralOperators(String formulaString) {
    ArrayDeque<Character> brackets = new ArrayDeque<>();
    int length = formulaString.length();

    for (int i = 0; i < length; i++) {
      char c = formulaString.charAt(i);
      if (c == '(') {
        brackets.push(c);
      } else if (c == ')') {
        if (brackets.isEmpty()) {
          throw new IllegalArgumentException("Mismatched parentheses in formula: " + formulaString);
        }
        brackets.pop();
      } else if (brackets.isEmpty()) {
        String remaining = formulaString.substring(i).trim();
        if (remaining.startsWith("+.add")) {
          return Add.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(6).trim()));
        } else if (remaining.startsWith("*.multiply")) {
          return Multiply.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(10).trim()));
        } else if (remaining.startsWith("/.divide")) {
          return Divide.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(9).trim()));
        } else if (remaining.startsWith("%.modulo")) {
          return Modulo.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(9).trim()));
        } else if (remaining.startsWith("&.binaryAnd")) {
          return BinaryAnd.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(11).trim()));
        } else if (remaining.startsWith("|.binaryOr")) {
          return BinaryOr.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(10).trim()));
        } else if (remaining.startsWith("^.binaryXor")) {
          return BinaryXor.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(11).trim()));
        } else if (remaining.startsWith("<<.shiftLeft")) {
          return ShiftLeft.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(12).trim()));
        } else if (remaining.startsWith(">>.shiftRight")) {
          return ShiftRight.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(13).trim()));
        } else if (remaining.startsWith("\\.exclusion")) {
          return Exclusion.of(parseNumeralFormula(remaining.substring(12).trim()));

        } else if (remaining.startsWith("U.union")) {
          return Union.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(7).trim()));
        } else if (remaining.startsWith("~.binaryNot")) {
          return BinaryNot.of(parseNumeralFormula(remaining.substring(11).trim()));
        }
      }
    }
    return parseNumeralConstantOrVariable(formulaString);
  }

  private static NumeralFormula<CompoundInterval> parseNumeralConstantOrVariable(
      String formulaString) {
    if (formulaString.matches(
        "(\\[(-?\\d+|-inf),(\\d+|inf)\\],?)+-typeInfo>Size: \\d+; Signed: (true|false)")) {
      return parseConstant(formulaString);
    }
    formulaString = removeEnclosingParentheses(formulaString);
    int separatorIndex = formulaString.lastIndexOf("-typeInfo>");
    if (separatorIndex == -1) {
      throw new IllegalArgumentException("Invalid type info in formula: " + formulaString);
    }

    String variableString = formulaString.substring(0, separatorIndex);
    String typeInfoString = formulaString.substring(separatorIndex + "-typeInfo>".length()).trim();
    TypeInfo typeInfo = parseTypeInfo(typeInfoString);

    return Variable.of(typeInfo, MemoryLocation.fromQualifiedName(variableString));
  }

  private static NumeralFormula<CompoundInterval> parseConstant(String input) {
    List<String> parts = Splitter.on("-typeInfo>").splitToList(input);
    String intervalsString = parts.get(0).trim();
    TypeInfo typeInfo = parseTypeInfo(parts.get(1).trim());
    List<BitVectorInterval> bitVectorIntervals = new ArrayList<>();
    Iterable<String> intervalStrings = Splitter.onPattern("],\\[").split(intervalsString);

    for (String intervalString : intervalStrings) {
      intervalString = intervalString.replace("[", "").replace("]", "");
      List<String> bounds = Splitter.on(',').splitToList(intervalString);
      BigInteger lowerBound = bounds.get(0).equals("-inf") ? null : new BigInteger(bounds.get(0));
      BigInteger upperBound = bounds.get(1).equals("inf") ? null : new BigInteger(bounds.get(1));

      bitVectorIntervals.add(
          BitVectorInterval.of((BitVectorInfo) typeInfo, lowerBound, upperBound));
    }

    return Constant.of(
        typeInfo,
        CompoundBitVectorInterval.of(
            (BitVectorInfo) typeInfo, ImmutableList.copyOf(bitVectorIntervals)));
  }

  private static TypeInfo parseTypeInfo(String typeInfo) {
    int size = 32;
    boolean signed = true;

    if (typeInfo != null && !typeInfo.isEmpty()) {
      List<String> parts = Splitter.on(';').trimResults().splitToList(typeInfo);
      for (String part : parts) {
        List<String> keyValue = Splitter.on(':').trimResults().splitToList(part);
        if (keyValue.size() != 2) {
          if (keyValue.size() == 1) {
            switch (keyValue.get(0).trim()) {
              case "FLOAT":
                return FloatingPointTypeInfo.FLOAT;
              case "DOUBLE":
                return FloatingPointTypeInfo.DOUBLE;
              default:
                throw new IllegalArgumentException(
                    "Unrecognized or malformed type information: " + typeInfo);
            }
          }
          throw new IllegalArgumentException(
              "Invalid key-value pair: " + part + " TypeInfo: " + typeInfo);
        }

        String key = keyValue.get(0).trim();
        String value = keyValue.get(1).trim();

        switch (key) {
          case "Size":
            size = Integer.parseInt(value);
            break;
          case "Signed":
            signed = Boolean.parseBoolean(value);
            break;
          default:
            throw new IllegalArgumentException("Unexpected key in type information: " + key);
        }
      }
    }

    return BitVectorInfo.from(size, signed);
  }

  private static boolean isEnclosedInParentheses(String formulaString) {
    return formulaString.startsWith("(")
        && formulaString.endsWith(")")
        && findMatchingParenthesis(formulaString, 0) == formulaString.length() - 1;
  }

  private static String stripEnclosingParentheses(String formulaString) {
    return formulaString.substring(1, formulaString.length() - 1).trim();
  }

  private static String removeEnclosingParentheses(String formulaString) {
    while (isEnclosedInParentheses(formulaString)) {
      formulaString = stripEnclosingParentheses(formulaString);
    }
    return formulaString;
  }

  private static int findMatchingParenthesis(String input, int openIndex) {
    int depth = 1;
    for (int i = openIndex + 1; i < input.length(); i++) {
      char c = input.charAt(i);
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    throw new IllegalArgumentException(
        "No matching closing parenthesis found for opening parenthesis at index "
            + openIndex
            + " for input: "
            + input);
  }
}
