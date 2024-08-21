// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.cfa.types.c;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;

public class StringToCTypeParser {

  public static CType parse(String input) {
    if (input.startsWith("ArrayType(")) {
      return parseArrayType(input);
    } else if (input.startsWith("PointerType(")) {
      return parsePointerType(input);
    } else if (input.startsWith("FunctionType(")) {
      return parseFunctionType(input);
    } else if (input.startsWith("SimpleType(")) {
      return parseSimpleType(input);
    } else if (input.startsWith("CompositeType(")) {
      return parseCompositeType(input);
    } else if (input.startsWith("ProblemType(")) {
      return parseProblemType(input);
    } else if (input.startsWith("TypedefType(")) {
      return parseTypedefType(input);
    } else if (input.startsWith("VoidType(")) {
      return parseVoidType(input);
    } else if (input.startsWith("BitFieldType(")) {
      return parseBitFieldType(input);
    } else if (input.startsWith("ElaboratedType(")) {
      return parseElaboratedType(input);
    } else if (input.startsWith("EnumType(")) {
      return parseEnumType(input);
    } else {
      throw new IllegalArgumentException("Unknown CType string: " + input);
    }
  }

  private static CType parsePointerType(String input) {
    String content = extractInnerContent(input, "PointerType(");
    List<String> parts = splitIgnoringNestedCommas(content);

    boolean isConst = Boolean.parseBoolean(parts.get(0));
    boolean isVolatile = Boolean.parseBoolean(parts.get(1));
    CType innerType = parse(parts.get(2));

    return new CPointerType(isConst, isVolatile, innerType);
  }

  private static CType parseArrayType(String input) {
    String content = extractInnerContent(input, "ArrayType(");
    List<String> parts = splitIgnoringNestedCommas(content);

    boolean isConst = Boolean.parseBoolean(parts.get(0));
    boolean isVolatile = Boolean.parseBoolean(parts.get(1));
    CType innerType = parse(parts.get(2));

    return new CArrayType(isConst, isVolatile, innerType);
  }

  private static CType parseFunctionType(String input) {
    String content = extractInnerContent(input, "FunctionType(");
    List<String> parts = splitIgnoringNestedCommas(content);
    CType returnType = parse(parts.get(0));
    List<CType> parameters = parseParameters(parts.get(1));
    boolean takesVarArgs = Boolean.parseBoolean(parts.get(2));

    return new CFunctionType(returnType, parameters, takesVarArgs);
  }

  private static CType parseBitFieldType(String input) {
    String content = extractInnerContent(input, "BitFieldType(");
    List<String> parts = splitIgnoringNestedCommas(content);
    int bitFieldSize = Integer.parseInt(parts.get(0));
    CType innerType = parse(parts.get(1));

    return new CBitFieldType(innerType, bitFieldSize);
  }

  private static CType parseProblemType(String input) {
    String content = extractInnerContent(input, "ProblemType(");
    return new CProblemType(content);
  }

  private static CType parseTypedefType(String input) {
    String content = extractInnerContent(input, "TypedefType(");
    List<String> parts = splitIgnoringNestedCommas(content);
    boolean isConst = Boolean.parseBoolean(parts.get(0));
    boolean isVolatile = Boolean.parseBoolean(parts.get(1));
    String name = parts.get(2);
    CType canonicalType = parse(parts.get(3));

    return new CTypedefType(isConst, isVolatile, name, canonicalType);
  }

  private static CType parseElaboratedType(String input) {
    String content = extractInnerContent(input, "ElaboratedType(");
    List<String> parts = splitIgnoringNestedCommas(content);
    boolean isConst = Boolean.parseBoolean(parts.get(0));
    boolean isVolatile = Boolean.parseBoolean(parts.get(1));
    CComplexType.ComplexTypeKind kind = CComplexType.ComplexTypeKind.valueOf(parts.get(2));
    String name = parts.get(3);
    String origName = parts.get(4);

    return new CElaboratedType(isConst, isVolatile, kind, name, origName, null);
  }

  private static CType parseVoidType(String input) {
    String content = extractInnerContent(input, "VoidType(");
    List<String> parts = splitIgnoringNestedCommas(content);
    boolean isConst = Boolean.parseBoolean(parts.get(0));
    boolean isVolatile = Boolean.parseBoolean(parts.get(1));

    return CVoidType.create(isConst, isVolatile);
  }

  private static CType parseSimpleType(String input) {
    String content = extractInnerContent(input, "SimpleType(");
    List<String> parts = splitIgnoringNestedCommas(content);
    boolean isConst = Boolean.parseBoolean(parts.get(0));
    boolean isVolatile = Boolean.parseBoolean(parts.get(1));
    CBasicType basicType = getBasicTypeFromString(parts.get(2));
    boolean hasLongSpecifier = Boolean.parseBoolean(parts.get(3));
    boolean hasShortSpecifier = Boolean.parseBoolean(parts.get(4));
    boolean hasSignedSpecifier = Boolean.parseBoolean(parts.get(5));
    boolean hasUnsignedSpecifier = Boolean.parseBoolean(parts.get(6));
    boolean hasComplexSpecifier = Boolean.parseBoolean(parts.get(7));
    boolean hasImaginarySpecifier = Boolean.parseBoolean(parts.get(8));
    boolean hasLongLongSpecifier = Boolean.parseBoolean(parts.get(9));

    return new CSimpleType(
        isConst,
        isVolatile,
        basicType,
        hasLongSpecifier,
        hasShortSpecifier,
        hasSignedSpecifier,
        hasUnsignedSpecifier,
        hasComplexSpecifier,
        hasImaginarySpecifier,
        hasLongLongSpecifier);
  }

  private static CType parseCompositeType(String input) {
    String content = extractInnerContent(input, "CompositeType(");
    List<String> parts = splitIgnoringNestedCommas(content);
    boolean isConst = Boolean.parseBoolean(parts.get(0));
    boolean isVolatile = Boolean.parseBoolean(parts.get(1));
    CComplexType.ComplexTypeKind kind = CComplexType.ComplexTypeKind.valueOf(parts.get(2));
    String name = parts.get(3);
    String origName = parts.get(4);

    return new CCompositeType(isConst, isVolatile, kind, name, origName);
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
          new CEnumerator(FileLocation.DUMMY, enumeratorName, name + "::" + enumeratorName, value));
    }

    CSimpleType enumCompatibleType =
        new CSimpleType(
            false, false, CBasicType.INT, false, false, false, false, false, false, false);

    return new CEnumType(isConst, isVolatile, enumCompatibleType, enumerators, name, origName);
  }

  private static List<String> splitIgnoringNestedCommas(String input) {
    List<String> parts = new ArrayList<>();
    StringBuilder currentPart = new StringBuilder();
    int nestedLevel = 0;

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (c == '(') {
        nestedLevel++;
      } else if (c == ')') {
        nestedLevel--;
      }

      if (c == ',' && nestedLevel == 0) {
        parts.add(currentPart.toString().trim());
        currentPart.setLength(0);
      } else {
        currentPart.append(c);
      }
    }

    if (currentPart.length() > 0) {
      parts.add(currentPart.toString().trim());
    }
    return parts;
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

  private static CBasicType getBasicTypeFromString(String typeStr) {
    switch (typeStr) {
      case "_Bool":
        return CBasicType.BOOL;
      case "char":
        return CBasicType.CHAR;
      case "int":
        return CBasicType.INT;
      case "__int128":
        return CBasicType.INT128;
      case "float":
        return CBasicType.FLOAT;
      case "double":
        return CBasicType.DOUBLE;
      case "__float128":
        return CBasicType.FLOAT128;
      default:
        return CBasicType.UNSPECIFIED;
    }
  }

  private static String extractInnerContent(String input, String prefix) {
    return input.substring(prefix.length(), input.length() - 1).trim();
  }
}
