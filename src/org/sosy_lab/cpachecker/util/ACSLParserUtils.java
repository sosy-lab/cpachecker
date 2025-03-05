// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.ACSLFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.ACSLTemporaryDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.ACSLVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils.InvalidYAMLWitnessException;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;

public class ACSLParserUtils {

  private static ACSLFunctionCall extractFunctionCall(
      String functionCall, CParser parser, Scope scope) throws InterruptedException {

    // Parse CLemmaFunctionCall
    CStatement s;
    try {
      s = CParserUtils.parseSingleStatement(functionCall, parser, scope);
    } catch (InvalidAutomatonException e) {
      throw new RuntimeException("Not a valid statement: " + functionCall);
    }
    // if s not instance of CFunctionCallStatement throw exception
    CFunctionCallExpression fExp = ((CFunctionCallStatement) s).getFunctionCallExpression();
    return new ACSLFunctionCall(fExp);
  }

  public static CExpression parseACSLExpression(
      String lAssumeCode, CParser parser, CProgramScope scope)
      throws InterruptedException, InvalidAutomatonException {

    List<ACSLTemporaryDeclaration> tmpDeclarations = new ArrayList<>();

    String lString = lAssumeCode;
    Map<String, ACSLFunctionCall> replacements = new HashMap<>();
    Pattern lp = Pattern.compile("ACSL\\((?<function>.*)\\)");
    Matcher lm = lp.matcher(lString);
    String tmp = "lemma_tmp_";

    while (lm.find()) {
      String functionCall = lm.group("function");
      ACSLFunctionCall lFuncCall = extractFunctionCall(functionCall, parser, scope);
      String key = tmp + replacements.size();
      replacements.put(key, lFuncCall);
      lString = lm.replaceFirst(key);
      // We need to add lemma_tmp_i as a variable to the scope
      ACSLTemporaryDeclaration tmpDeclaration =
          new ACSLTemporaryDeclaration(
              lFuncCall.getFileLocation(),
              false,
              CStorageClass.AUTO,
              lFuncCall.getExpressionType(),
              key,
              key,
              key,
              null);
      try {
        scope.addDeclarationToScope(tmpDeclaration);
      } catch (InvalidYAMLWitnessException pE) {
        throw new RuntimeException("This should not happen");
      }
      tmpDeclarations.add(tmpDeclaration);
      lm = lp.matcher(lString);
    }

    CStatement statement;
    try {
      statement = CParserUtils.parseSingleStatement(lString, parser, scope);
    } catch (InvalidAutomatonException e) {
      throw new RuntimeException("Not a valid statement: " + lString);
    }
    if (!(statement instanceof CExpressionStatement)) {
      throw new InvalidAutomatonException(
          "Cannot interpret String as CExpressionStatement" + lString);
    }
    CExpression exp = ((CExpressionStatement) statement).getExpression();
    exp = exp.accept(new ACSLVisitor(replacements));
    for (ACSLTemporaryDeclaration tmpDecl : tmpDeclarations) {
      scope.removeTemporaryDeclaration(tmpDecl);
    }
    return exp;
  }

  public static List<CDeclaration> parseDeclarations(List<String> declarations)
      throws InvalidYAMLWitnessException {
    ImmutableList.Builder<CDeclaration> cDeclarations = new ImmutableList.Builder<>();

    for (String declaration : declarations) {
      cDeclarations.add(parseSingleDeclaration(declaration));
    }
    return cDeclarations.build();
  }

  public static CDeclaration parseSingleDeclaration(String assumeDeclaration)
      throws InvalidYAMLWitnessException {
    Pattern functionPattern = Pattern.compile("\\w+\\*?\\s+\\w+\\s*\\(.*\\)");
    Pattern variablePattern = Pattern.compile("\\w+\\*?\\s+\\w+");
    Pattern declarationPattern =
        Pattern.compile(
            "(?<declaration>(?<type>(void|int|char|long|float|double)(\\*?))\\s+(?<name>\\w+))");

    Matcher isFunction = functionPattern.matcher(assumeDeclaration);
    Matcher isVariable = variablePattern.matcher(assumeDeclaration);

    if (isFunction.matches()) {
      return parseFunctionDeclaration(assumeDeclaration, declarationPattern);
    } else if (isVariable.matches()) {
      return parseVariableDeclaration(assumeDeclaration, declarationPattern);

    } else {
      throw new InvalidYAMLWitnessException(
          "Statement is not a valid declaration: " + assumeDeclaration);
    }
  }

  public static CFunctionDeclaration parseFunctionDeclaration(
      String assumeDeclaration, Pattern pPattern) throws InvalidYAMLWitnessException {
    Matcher functionMatcher = pPattern.matcher(assumeDeclaration);

    if (functionMatcher.find()) {
      ImmutableList.Builder<CParameterDeclaration> parameters = new ImmutableList.Builder<>();
      // The first match is the return type and the name of the function
      CType returnType = toCtype(functionMatcher.group("type"));
      String functionName = functionMatcher.group("name");
      List<CType> parameterTypes = new ArrayList<>();

      // Subsequent matches are the type and name of the function parameters
      while (functionMatcher.find()) {
        CType type = toCtype(functionMatcher.group("type"));
        parameterTypes.add(type);
        String name = functionMatcher.group("name");
        CParameterDeclaration decl = new CParameterDeclaration(FileLocation.DUMMY, type, name);
        parameters.add(decl);
      }
      CFunctionType functionType = new CFunctionType(returnType, parameterTypes, false);
      return new CFunctionDeclaration(
          FileLocation.DUMMY, functionType, functionName, parameters.build(), ImmutableSet.of());
    } else {
      throw new InvalidYAMLWitnessException(
          "Statement is not a valid function declaration: " + assumeDeclaration);
    }
  }

  public static CVariableDeclaration parseVariableDeclaration(
      String assumeDeclaration, Pattern pPattern) throws InvalidYAMLWitnessException {
    Matcher variableMatcher = pPattern.matcher(assumeDeclaration);
    if (variableMatcher.find()) {
      CType type = toCtype(variableMatcher.group("type"));
      String name = variableMatcher.group("name");
      return new CVariableDeclaration(
          FileLocation.DUMMY, false, CStorageClass.AUTO, type, name, name, name, null);
    } else {
      throw new InvalidYAMLWitnessException(
          "Statement is not a valid variable declaration: " + assumeDeclaration);
    }
  }

  public static CType toCtype(String typeName) {
    return switch (typeName) {
      case "void" -> CVoidType.VOID;
      case "int" -> CNumericTypes.INT;
      case "char" -> CNumericTypes.CHAR;
      case "long" -> CNumericTypes.LONG_INT;
      case "float" -> CNumericTypes.FLOAT;
      case "double" -> CNumericTypes.DOUBLE;
      case "int*" -> new CPointerType(false, false, CNumericTypes.INT);
      default -> throw new IllegalArgumentException("Not a valid type declaration: " + typeName);
    };
  }
}
