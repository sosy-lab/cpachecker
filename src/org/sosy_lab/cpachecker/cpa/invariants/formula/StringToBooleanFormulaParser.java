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
        if (remaining.startsWith("&&")) {
          return LogicalAnd.of(
              parseBooleanFormula(formulaString.substring(0, i).trim()),
              parseBooleanFormula(remaining.substring(2).trim()));
        } else if (remaining.startsWith("!")) {
          return LogicalNot.of(parseBooleanFormula(remaining.substring(1).trim()));
        } else if (remaining.startsWith("=")) {
          return Equal.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(1).trim()));
        } else if (remaining.startsWith("<")) {
          return LessThan.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(1).trim()));
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

    if (formulaString.contains("?")) {
      return parseIfThenElse(formulaString);
    }

    return parseNumeralOperators(formulaString);
  }

  private static NumeralFormula<CompoundInterval> parseCast(String formulaString) {
    int typeInfoStartIndex = formulaString.indexOf("-typeInfo>") + 10; // Adjust to the correct
    // length
    int typeInfoEndIndex = formulaString.indexOf('(', typeInfoStartIndex);
    if (typeInfoEndIndex == -1) {
      throw new IllegalArgumentException("Invalid cast format: " + formulaString);
    }
    String typeInfoString = formulaString.substring(typeInfoStartIndex, typeInfoEndIndex).trim();
    TypeInfo typeInfo = parseTypeInfo(typeInfoString);

    int castEndIndex = findMatchingParenthesis(formulaString, typeInfoEndIndex);
    String castedFormula = formulaString.substring(typeInfoEndIndex + 1, castEndIndex).trim();

    return Cast.of(typeInfo, parseNumeralFormula(castedFormula));
  }

  private static NumeralFormula<CompoundInterval> parseIfThenElse(String formulaString) {
    int questionMarkIndex = formulaString.indexOf("?");
    int colonIndex = findMatchingColon(formulaString, questionMarkIndex);
    String condition = formulaString.substring(0, questionMarkIndex).trim();
    String ifCase = formulaString.substring(questionMarkIndex + 1, colonIndex).trim();
    String elseCase = formulaString.substring(colonIndex + 1).trim();

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
        if (remaining.startsWith("+")) {
          return Add.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(1).trim()));
        } else if (remaining.startsWith("*")) {
          return Multiply.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(1).trim()));
        } else if (remaining.startsWith("/")) {
          return Divide.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(1).trim()));
        } else if (remaining.startsWith("%")) {
          return Modulo.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(1).trim()));
        } else if (remaining.startsWith("&")) {
          return BinaryAnd.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(1).trim()));
        } else if (remaining.startsWith("|")) {
          return BinaryOr.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(1).trim()));
        } else if (remaining.startsWith("^")) {
          return BinaryXor.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(1).trim()));
        } else if (remaining.startsWith("<<")) {
          return ShiftLeft.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(2).trim()));
        } else if (remaining.startsWith(">>")) {
          return ShiftRight.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(2).trim()));
        } else if (remaining.startsWith("\\")) {
          return Exclusion.of(parseNumeralFormula(remaining.substring(1).trim()));
        } else if (remaining.startsWith("UNION")) {
          return Union.of(
              parseNumeralFormula(formulaString.substring(0, i).trim()),
              parseNumeralFormula(remaining.substring(1).trim()));
        } else if (remaining.startsWith("~")) {
          return BinaryNot.of(parseNumeralFormula(remaining.substring(1).trim()));
        }
      }
    }
    return parseNumeralConstantOrVariable(formulaString);
  }

  private static NumeralFormula<CompoundInterval> parseNumeralConstantOrVariable(
      String formulaString) {

    // Adjusted regex to handle the new format with -inf and inf, and the "UNION" operator
    if (formulaString.matches(
        "(\\[(-?\\d+|-inf),(\\d+|inf)\\],?)+-typeInfo>Size: \\d+; Signed: (true|false)")) {
      return parseConstant(formulaString);
    }

    formulaString = removeEnclosingParentheses(formulaString);

    // Ensure the separator "-typeInfo>" is correctly identified
    int separatorIndex = formulaString.lastIndexOf("-typeInfo>");
    if (separatorIndex == -1) {
      throw new IllegalArgumentException("Invalid type info in formula: " + formulaString);
    }

    String variableString = formulaString.substring(0, separatorIndex).trim();
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
        "No matching closing parenthesis found for opening parenthesis at index " + openIndex);
  }

  private static int findMatchingColon(String input, int questionMarkIndex) {
    int depth = 0;
    for (int i = questionMarkIndex + 1; i < input.length(); i++) {
      char c = input.charAt(i);
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
      } else if (c == ':' && depth == 0) {
        return i;
      }
    }
    throw new IllegalArgumentException(
        "No matching colon found for question mark at index " + questionMarkIndex);
  }
}
