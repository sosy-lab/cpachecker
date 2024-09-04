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

        if (remaining.startsWith(Operation.LOGICAL_AND.getRepresentation())) {
          return LogicalAnd.of(
              parseBooleanFormula(formulaString.substring(0, i).trim()),
              parseBooleanFormula(
                  remaining.substring(Operation.LOGICAL_AND.getRepresentation().length()).trim()));
        } else if (remaining.startsWith(Operation.LOGICAL_NOT.getRepresentation())) {
          return LogicalNot.of(
              parseBooleanFormula(
                  remaining.substring(Operation.LOGICAL_NOT.getRepresentation().length()).trim()));
        } else if (remaining.startsWith(Operation.EQUAL.getRepresentation())) {
          return Equal.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(
                  remaining.substring(Operation.EQUAL.getRepresentation().length()).trim()));
        } else if (remaining.startsWith(Operation.LESS_THAN.getRepresentation())) {
          return LessThan.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(
                  remaining.substring(Operation.LESS_THAN.getRepresentation().length()).trim()));
        }
      }
    }
    throw new IllegalArgumentException("Unknown boolean formula: " + formulaString);
  }

  private static NumeralFormula<CompoundInterval> parseNumeralFormula(String formulaString) {
    formulaString = removeEnclosingParentheses(formulaString);

    if (formulaString.startsWith(Operation.CAST.getRepresentation())) {
      return parseCast(formulaString);
    }
    if (formulaString.contains(Operation.IF.getRepresentation())) {
      return parseIfThenElse(formulaString);
    }
    return parseNumeralOperators(formulaString);
  }

  private static NumeralFormula<CompoundInterval> parseCast(String formulaString) {
    int typeInfoStartIndex = formulaString.indexOf(".ti") + 3;
    int typeInfoEndIndex = formulaString.indexOf('(', typeInfoStartIndex);
    String typeInfoString = formulaString.substring(typeInfoStartIndex, typeInfoEndIndex).trim();
    TypeInfo typeInfo = parseTypeInfo(typeInfoString);
    int castEndIndex = findMatchingParenthesis(formulaString, typeInfoEndIndex);
    String castedFormula = formulaString.substring(typeInfoEndIndex + 1, castEndIndex).trim();

    return Cast.of(typeInfo, parseNumeralFormula(castedFormula));
  }

  private static NumeralFormula<CompoundInterval> parseIfThenElse(String formulaString) {
    int ifIndex = formulaString.indexOf(Operation.IF.getRepresentation());
    int elseIndex = formulaString.indexOf(Operation.ELSE.getRepresentation(), ifIndex);
    String condition = formulaString.substring(0, ifIndex).trim();
    String ifCase =
        formulaString
            .substring(ifIndex + Operation.IF.getRepresentation().length(), elseIndex)
            .trim();
    String elseCase =
        formulaString.substring(elseIndex + Operation.ELSE.getRepresentation().length()).trim();

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
        if (remaining.startsWith(Operation.ADD.getRepresentation())) {
          return Add.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(
                  remaining.substring(Operation.ADD.getRepresentation().length()).trim()));
        } else if (remaining.startsWith(Operation.MULTIPLY.getRepresentation())) {
          return Multiply.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(
                  remaining.substring(Operation.MULTIPLY.getRepresentation().length()).trim()));
        } else if (remaining.startsWith(Operation.DIVIDE.getRepresentation())) {
          return Divide.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(
                  remaining.substring(Operation.DIVIDE.getRepresentation().length()).trim()));
        } else if (remaining.startsWith(Operation.MODULO.getRepresentation())) {
          return Modulo.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(
                  remaining.substring(Operation.MODULO.getRepresentation().length()).trim()));
        } else if (remaining.startsWith(Operation.BINARY_AND.getRepresentation())) {
          return BinaryAnd.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(
                  remaining.substring(Operation.BINARY_AND.getRepresentation().length()).trim()));
        } else if (remaining.startsWith(Operation.BINARY_OR.getRepresentation())) {
          return BinaryOr.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(
                  remaining.substring(Operation.BINARY_OR.getRepresentation().length()).trim()));
        } else if (remaining.startsWith(Operation.BINARY_XOR.getRepresentation())) {
          return BinaryXor.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(
                  remaining.substring(Operation.BINARY_XOR.getRepresentation().length()).trim()));
        } else if (remaining.startsWith(Operation.SHIFT_LEFT.getRepresentation())) {
          return ShiftLeft.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(
                  remaining.substring(Operation.SHIFT_LEFT.getRepresentation().length()).trim()));
        } else if (remaining.startsWith(Operation.SHIFT_RIGHT.getRepresentation())) {
          return ShiftRight.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(
                  remaining.substring(Operation.SHIFT_RIGHT.getRepresentation().length()).trim()));
        } else if (remaining.startsWith(Operation.EXCLUSION.getRepresentation())) {
          return Exclusion.of(
              parseNumeralFormula(
                  remaining.substring(Operation.EXCLUSION.getRepresentation().length()).trim()));

        } else if (remaining.startsWith(Operation.UNION.getRepresentation())) {
          return Union.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(
                  remaining.substring(Operation.UNION.getRepresentation().length()).trim()));
        } else if (remaining.startsWith(Operation.BINARY_NOT.getRepresentation())) {
          return BinaryNot.of(
              parseNumeralFormula(
                  remaining.substring(Operation.BINARY_NOT.getRepresentation().length()).trim()));
        }
      }
    }
    return parseNumeralConstantOrVariable(formulaString);
  }

  private static NumeralFormula<CompoundInterval> parseNumeralConstantOrVariable(
      String formulaString) {
    if (formulaString.matches("(\\[(-?\\d+|-inf),(\\d+|inf)\\],?)+.ti\\d+,\\w+")) {
      return parseConstant(formulaString);
    }
    formulaString = removeEnclosingParentheses(formulaString);
    int separatorIndex = formulaString.lastIndexOf(".ti");
    if (separatorIndex == -1) {
      throw new IllegalArgumentException("Invalid type info in formula: " + formulaString);
    }

    String variableString = formulaString.substring(0, separatorIndex);
    String typeInfoString = formulaString.substring(separatorIndex + ".ti".length()).trim();
    TypeInfo typeInfo = parseTypeInfo(typeInfoString);

    return Variable.of(typeInfo, MemoryLocation.fromQualifiedName(variableString));
  }

  private static NumeralFormula<CompoundInterval> parseConstant(String input) {
    List<String> parts = Splitter.on(".ti").splitToList(input);
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
    if (typeInfo.contains(",")) {
      List<String> parts = Splitter.on(',').splitToList(typeInfo);
      int size = Integer.parseInt(parts.get(0).trim());
      boolean signed = Boolean.parseBoolean(parts.get(1).trim());
      return BitVectorInfo.from(size, signed);
    } else {
      switch (typeInfo) {
        case "float":
          return FloatingPointTypeInfo.FLOAT;
        case "double":
          return FloatingPointTypeInfo.DOUBLE;
        default:
          throw new IllegalArgumentException("Unknown TypeInfo abbreviation: " + typeInfo);
      }
    }
  }

  private static String removeEnclosingParentheses(String formulaString) {
    while (formulaString.startsWith("(")
        && formulaString.endsWith(")")
        && findMatchingParenthesis(formulaString, 0) == formulaString.length() - 1) {
      formulaString = formulaString.substring(1, formulaString.length() - 1).trim();
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
