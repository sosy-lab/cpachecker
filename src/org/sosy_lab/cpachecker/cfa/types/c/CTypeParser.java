// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;

/**
 * Utility class for parsing {@link CType} objects from their string representations.
 *
 * <p>The string format expected by this parser matches the output produced by {@link
 * SerializeCTypeVisitor}. Each supported {@link CType} implementation is represented as a string
 * with a specific prefix and a comma-separated list of arguments, for example:
 *
 * <pre>
 *   SimpleType(false, false, int, false, false, false, false, false, false, false)
 *   PointerType(true, false, SimpleType(...))
 *   ArrayType(false, false, SimpleType(...))
 *   FunctionType(SimpleType(...), [SimpleType(...), PointerType(...)], false)
 * </pre>
 *
 * <p>This class provides a single entry point {@link #parse(String)} which reconstructs the
 * corresponding {@link CType} object from such a string.
 */
public class CTypeParser {

  public static CTypeQualifiers getQualifiers(boolean isConst, boolean isVolatile) {
    CTypeQualifiers qualifier = CTypeQualifiers.NONE;
    if (isConst && isVolatile) {
      qualifier = CTypeQualifiers.CONST_VOLATILE;
    } else if (isConst) {
      qualifier = CTypeQualifiers.CONST;
    } else if (isVolatile) {
      qualifier = CTypeQualifiers.VOLATILE;
    }
    return qualifier;
  }

  public static CType parse(String input) {
    if (input.startsWith("ArrayType(")) {
      return parseArrayType(input);
    } else if (input.startsWith("PointerType(")) {
      return parsePointerType(input);
    } else if (input.startsWith("FunctionType(")) {
      return parseFunctionType(input);
    } else if (input.startsWith("BitFieldType(")) {
      return parseBitFieldType(input);
    } else if (input.startsWith("ProblemType(")) {
      return parseProblemType(input);
    } else if (input.startsWith("TypedefType(")) {
      return parseTypedefType(input);
    } else if (input.startsWith("ElaboratedType(")) {
      return parseElaboratedType(input);
    } else if (input.startsWith("VoidType(")) {
      return parseVoidType(input);
    } else if (input.startsWith("SimpleType(")) {
      return parseSimpleType(input);
    } else if (input.startsWith("CompositeType(")) {
      return parseCompositeType(input);
    } else if (input.startsWith("EnumType(")) {
      return parseEnumType(input);
    }
    throw new IllegalArgumentException("Unknown type: " + input);
  }

  private record PointerTypeParts(boolean isConst, boolean isVolatile, CType innerType) {}

  private record ArrayTypeParts(boolean isConst, boolean isVolatile, CType innerType) {}

  private record FunctionTypeParts(
      CType returnType, List<CType> parameters, boolean takesVarArgs) {}

  private record BitFieldTypeParts(int bitFieldSize, CType innerType) {}

  private record TypedefTypeParts(
      boolean isConst, boolean isVolatile, String name, CType canonicalType) {}

  private record ElaboratedTypeParts(
      boolean isConst,
      boolean isVolatile,
      CComplexType.ComplexTypeKind kind,
      String name,
      String origName) {}

  private record SimpleTypeParts(
      boolean isConst,
      boolean isVolatile,
      CBasicType basicType,
      boolean hasLongSpecifier,
      boolean hasShortSpecifier,
      boolean hasSignedSpecifier,
      boolean hasUnsignedSpecifier,
      boolean hasComplexSpecifier,
      boolean hasImaginarySpecifier,
      boolean hasLongLongSpecifier) {}

  private record CompositeTypeParts(
      boolean isConst,
      boolean isVolatile,
      CComplexType.ComplexTypeKind kind,
      String name,
      String origName) {}

  private static CType parsePointerType(String input) {
    String content = extractInnerContent(input, "PointerType(");
    List<String> parts = splitIgnoringNestedCommas(content);
    PointerTypeParts pointerParts =
        new PointerTypeParts(
            Boolean.parseBoolean(parts.get(0)),
            Boolean.parseBoolean(parts.get(1)),
            parse(parts.get(2)));

    return new CPointerType(
        getQualifiers(pointerParts.isConst(), pointerParts.isVolatile()), pointerParts.innerType());
  }

  private static CType parseArrayType(String input) {
    String content = extractInnerContent(input, "ArrayType(");
    List<String> parts = splitIgnoringNestedCommas(content);
    ArrayTypeParts arrayParts =
        new ArrayTypeParts(
            Boolean.parseBoolean(parts.get(0)),
            Boolean.parseBoolean(parts.get(1)),
            parse(parts.get(2)));

    return new CArrayType(
        getQualifiers(arrayParts.isConst(), arrayParts.isVolatile()), arrayParts.innerType());
  }

  private static CType parseFunctionType(String input) {
    String content = extractInnerContent(input, "FunctionType(");
    List<String> parts = splitIgnoringNestedCommas(content);
    FunctionTypeParts functionParts =
        new FunctionTypeParts(
            parse(parts.get(0)), parseParameters(parts.get(1)), Boolean.parseBoolean(parts.get(2)));

    return new CFunctionType(
        functionParts.returnType(), functionParts.parameters(), functionParts.takesVarArgs());
  }

  private static CType parseBitFieldType(String input) {
    String content = extractInnerContent(input, "BitFieldType(");
    List<String> parts = splitIgnoringNestedCommas(content);
    BitFieldTypeParts bitFieldParts =
        new BitFieldTypeParts(Integer.parseInt(parts.get(0)), parse(parts.get(1)));
    return new CBitFieldType(bitFieldParts.innerType(), bitFieldParts.bitFieldSize());
  }

  private static CType parseProblemType(String input) {
    String content = extractInnerContent(input, "ProblemType(");
    return new CProblemType(content);
  }

  private static CType parseTypedefType(String input) {
    String content = extractInnerContent(input, "TypedefType(");
    ImmutableList<String> parts = splitIgnoringNestedCommas(content);
    TypedefTypeParts typeDefParts =
        new TypedefTypeParts(
            Boolean.parseBoolean(parts.get(0)),
            Boolean.parseBoolean(parts.get(1)),
            parts.get(2),
            parse(parts.get(3)));
    return new CTypedefType(
        getQualifiers(typeDefParts.isConst(), typeDefParts.isVolatile()),
        typeDefParts.name(),
        typeDefParts.canonicalType());
  }

  private static CType parseElaboratedType(String input) {
    String content = extractInnerContent(input, "ElaboratedType(");
    ImmutableList<String> parts = splitIgnoringNestedCommas(content);
    ElaboratedTypeParts elaborateParts =
        new ElaboratedTypeParts(
            Boolean.parseBoolean(parts.get(0)),
            Boolean.parseBoolean(parts.get(1)),
            CComplexType.ComplexTypeKind.valueOf(parts.get(2)),
            parts.get(3),
            parts.get(4));
    return new CElaboratedType(
        getQualifiers(elaborateParts.isConst(), elaborateParts.isVolatile()),
        elaborateParts.kind(),
        elaborateParts.name(),
        elaborateParts.origName(),
        null);
  }

  private static CType parseVoidType(String input) {
    String content = extractInnerContent(input, "VoidType(");
    List<String> parts = splitIgnoringNestedCommas(content);
    boolean isConst = Boolean.parseBoolean(parts.get(0));
    boolean isVolatile = Boolean.parseBoolean(parts.get(1));
    if (isConst && isVolatile) {
      return CVoidType.CONST_VOLATILE_VOID;
    } else if (isVolatile) {
      return CVoidType.VOLATILE_VOID;
    } else if (isConst) {
      return CVoidType.CONST_VOID;
    }
    return CVoidType.VOID;
  }

  private static CType parseSimpleType(String input) {
    String content = extractInnerContent(input, "SimpleType(");
    ImmutableList<String> parts = splitIgnoringNestedCommas(content);
    SimpleTypeParts simpParts =
        new SimpleTypeParts(
            Boolean.parseBoolean(parts.get(0)),
            Boolean.parseBoolean(parts.get(1)),
            getBasicTypeFromString(parts.get(2)),
            Boolean.parseBoolean(parts.get(3)),
            Boolean.parseBoolean(parts.get(4)),
            Boolean.parseBoolean(parts.get(5)),
            Boolean.parseBoolean(parts.get(6)),
            Boolean.parseBoolean(parts.get(7)),
            Boolean.parseBoolean(parts.get(8)),
            Boolean.parseBoolean(parts.get(9)));
    return new CSimpleType(
        getQualifiers(simpParts.isConst(), simpParts.isVolatile()),
        simpParts.basicType(),
        simpParts.hasLongSpecifier(),
        simpParts.hasShortSpecifier(),
        simpParts.hasSignedSpecifier(),
        simpParts.hasUnsignedSpecifier(),
        simpParts.hasComplexSpecifier(),
        simpParts.hasImaginarySpecifier(),
        simpParts.hasLongLongSpecifier());
  }

  private static CType parseCompositeType(String input) {
    String content = extractInnerContent(input, "CompositeType(");
    ImmutableList<String> parts = splitIgnoringNestedCommas(content);
    CompositeTypeParts compParts =
        new CompositeTypeParts(
            Boolean.parseBoolean(parts.get(0)),
            Boolean.parseBoolean(parts.get(1)),
            CComplexType.ComplexTypeKind.valueOf(parts.get(2)),
            parts.get(3),
            parts.get(4));
    return new CCompositeType(
        getQualifiers(compParts.isConst(), compParts.isVolatile()),
        compParts.kind(),
        compParts.name(),
        compParts.origName());
  }

  private static CType parseEnumType(String input) {
    String content = extractInnerContent(input, "EnumType(");
    List<String> parts = splitIgnoringNestedCommas(content);
    boolean isConst = Boolean.parseBoolean(parts.get(0));
    boolean isVolatile = Boolean.parseBoolean(parts.get(1));
    String name = parts.get(2);
    String origName = parts.get(3);
    List<CEnumerator> enumerators = new ArrayList<>();
    for (String enumStr :
        splitIgnoringNestedCommas(parts.get(4).substring(1, parts.get(4).length() - 1))) {
      List<String> enumParts = Splitter.on(" = ").splitToList(enumStr);
      String enumeratorName = enumParts.get(0).trim();
      long value = Long.parseLong(enumParts.get(1).trim());
      enumerators.add(
          new CEnumerator(
              FileLocation.DUMMY,
              enumeratorName,
              name + "::" + enumeratorName,
              BigInteger.valueOf(value)));
    }

    CSimpleType enumCompatibleType =
        new CSimpleType(
            getQualifiers(false, false),
            CBasicType.INT,
            false,
            false,
            false,
            false,
            false,
            false,
            false);
    return new CEnumType(
        getQualifiers(isConst, isVolatile), enumCompatibleType, enumerators, name, origName);
  }

  private static String extractInnerContent(String input, String prefix) {
    Preconditions.checkArgument(
        input.startsWith(prefix),
        "Input string '" + input + "' does not start with the given prefix '" + prefix + "'.");
    Preconditions.checkArgument(
        input.endsWith(")"), "Input string '" + input + "' does not end with ')'.");
    return input.substring(prefix.length(), input.length() - 1).trim();
  }

  private static List<CType> parseParameters(String input) {
    List<CType> parameters = new ArrayList<>();
    if (input.length() > 2) {
      String paramContent = input.substring(1, input.length() - 1);
      List<String> paramStrings = splitIgnoringNestedCommas(paramContent);
      for (String paramString : paramStrings) {
        parameters.add(parse(paramString));
      }
    }
    return parameters;
  }

  /**
   * Splits a string into parts separated by commas, ignoring commas inside nested parentheses and
   * brackets.
   *
   * <p>For example: {@code "a, b(c,d), e[f,g], h"} would be split into {@code ["a", "b(c,d)",
   * "e[f,g]", "h"]}.
   *
   * <p>This is necessary because some types may contain nested parameter lists or array
   * declarations which contain commas that should not be treated as top-level separators.
   */
  private static ImmutableList<String> splitIgnoringNestedCommas(String input) {
    ImmutableList.Builder<String> partsBuilder = ImmutableList.builder();
    StringBuilder currentPart = new StringBuilder();
    int nestedParentheses = 0;
    int nestedBrackets = 0;

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (c == ',' && nestedParentheses == 0 && nestedBrackets == 0) {
        partsBuilder.add(currentPart.toString().trim());
        currentPart.setLength(0);
      } else {
        if (c == '(') {
          nestedParentheses++;
        } else if (c == ')') {
          nestedParentheses--;
        } else if (c == '[') {
          nestedBrackets++;
        } else if (c == ']') {
          nestedBrackets--;
        }
        currentPart.append(c);
      }
    }
    if (!currentPart.isEmpty()) {
      partsBuilder.add(currentPart.toString().trim());
    }
    return partsBuilder.build();
  }

  private static CBasicType getBasicTypeFromString(String typeStr) {
    return switch (typeStr) {
      case "_Bool" -> CBasicType.BOOL;
      case "char" -> CBasicType.CHAR;
      case "int" -> CBasicType.INT;
      case "__int128" -> CBasicType.INT128;
      case "float" -> CBasicType.FLOAT;
      case "double" -> CBasicType.DOUBLE;
      case "__float128" -> CBasicType.FLOAT128;
      default -> CBasicType.UNSPECIFIED;
    };
  }
}
