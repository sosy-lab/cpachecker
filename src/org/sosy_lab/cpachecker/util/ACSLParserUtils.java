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

/**
 * This class provides the necessary methods to parse lemmas from a file.
 *
 * <p>All methods within this class are only intended as temporary workarounds until a proper
 * ACSL-parser has been implemented. At this point these methods should not be used anywhere outside
 * the parsing of lemmas from a YAML witness for predicate abstraction.
 */
public class ACSLParserUtils {

  /**
   * This method creates an ACSLFunctionCall from a String.
   *
   * <p>This method is only intended as a temporary workaround until a proper ACSL-parser has been
   * implemented. It should not be used anywhere outside the parsing of lemmas from a YAML * witness
   * for predicate abstraction.
   *
   * @param functionCall A string representation of an ACSL function call statement.
   * @param parser A CParser
   * @param scope The program scope of the C prorgam of the verification task.
   */
  private static ACSLFunctionCall extractFunctionCall(
      String functionCall, CParser parser, Scope scope) throws InterruptedException {
    CStatement s;
    try {
      s = CParserUtils.parseSingleStatement(functionCall, parser, scope);
    } catch (InvalidAutomatonException e) {
      throw new RuntimeException("Not a valid statement: " + functionCall);
    }

    CFunctionCallExpression fExp = ((CFunctionCallStatement) s).getFunctionCallExpression();
    return new ACSLFunctionCall(fExp);
  }

  /**
   * This method takes a C statement string that might contain a function call and returns the
   * corresponding CExpression.
   *
   * <p>This method is only intended as a temporary workaround until a proper ACSL-parser has been
   * implemented. It should not be used anywhere outside the parsing of lemmas from a YAML * witness
   * for predicate abstraction.
   *
   * <p>Function calls are identified via regex pattern matching and then replaced with the
   * substring "lemma_tmp_i". The ACSLFunctionCall and the corresponding key "lemma_tmp_i" are
   * stored in a HashMap. The String is parsed as a regular CBinaryExpression. Afterwards the
   * ACSLFunctionCalls are back inserted at the appropriate places using the ACSLVisitor.
   *
   * @param lAssumeCode A string representation of an ACSL statement.
   * @param parser A CParser *
   * @param scope The program scope of the C prorgam of the verification task.
   */
  public static CExpression parseACSLExpression(
      String lAssumeCode, CParser parser, CProgramScope scope)
      throws InterruptedException, InvalidAutomatonException {

    String lString = lAssumeCode;
    CProgramScope tmpScope = new CProgramScope(scope);

    Map<String, ACSLFunctionCall> replacements = new HashMap<>();
    Pattern lp = Pattern.compile("ACSL\\((?<function>\\w+\\([^()]*\\))\\)");
    Matcher lm = lp.matcher(lString);
    String tmp = "lemma_tmp_";

    while (lm.find()) {
      String functionCall = lm.group("function");
      ACSLFunctionCall lFuncCall = extractFunctionCall(functionCall, parser, scope);
      String key = tmp + replacements.size();
      replacements.put(key, lFuncCall);
      lString = lm.replaceFirst(key);

      CVariableDeclaration tmpDeclaration =
          new CVariableDeclaration(
              lFuncCall.getFileLocation(),
              false,
              CStorageClass.AUTO,
              lFuncCall.getExpressionType(),
              key,
              key,
              key,
              null);
      try {
        tmpScope.addDeclarationToScope(tmpDeclaration);
      } catch (InvalidYAMLWitnessException e) {
        throw new RuntimeException("This should not happen");
      }
      lm = lp.matcher(lString);
    }

    CStatement statement;
    try {
      statement = CParserUtils.parseSingleStatement(lString, parser, tmpScope);
    } catch (InvalidAutomatonException e) {
      throw new RuntimeException("Not a valid statement: " + lString);
    }
    if (!(statement instanceof CExpressionStatement)) {
      throw new InvalidAutomatonException(
          "Cannot interpret String as CExpressionStatement" + lString);
    }
    CExpression exp = ((CExpressionStatement) statement).getExpression();
    exp = exp.accept(new ACSLVisitor(replacements));
    return exp;
  }

  /**
   * This method creates a List of CDeclarations from a List of Strings.
   *
   * <p>This method is only intended as a temporary workaround until a proper ACSL-parser has been
   * implemented. It should not be used anywhere outside the parsing of lemmas from a YAML * witness
   * for predicate abstraction.
   *
   * @param declarations A List of strings containing C function and variable declarations.
   */
  public static List<CDeclaration> parseDeclarations(List<String> declarations)
      throws InvalidYAMLWitnessException {
    ImmutableList.Builder<CDeclaration> cDeclarations = new ImmutableList.Builder<>();

    for (String declaration : declarations) {
      cDeclarations.add(parseSingleDeclaration(declaration));
    }
    return cDeclarations.build();
  }

  /**
   * This method takes a String that is either a function declaration or a variable declaration and
   * returns either a CFunctionDeclaration or a CVariableDeclaration.
   *
   * <p>This method is only intended as a temporary workaround until a * proper ACSL-parser has been
   * implemented. It should not be used anywhere outside the parsing of * lemmas from a YAML *
   * witness for predicate abstraction.
   *
   * @param assumeDeclaration A stirng representation of either a C variable or function
   *     declaration.
   */
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

  /**
   * This method takes a function declaration as a String and creates a corresponding object of type
   * CFunctionDeclaration.
   *
   * <p>This method is only intended as a temporary workaround until a * proper ACSL-parser has been
   * implemented. It should not be used anywhere outside the parsing of * lemmas from a YAML *
   * witness for predicate abstraction.
   *
   * @param assumeDeclaration A string representation of a C function declaration.
   * @param pPattern A regex that represents a C declaration of type and name.
   */
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

  /**
   * This method takes a variable declaration as a String and creates a corresponding object of type
   * CVariableDeclaration.
   *
   * <p>This method is only intended as a temporary workaround until a * proper ACSL-parser has been
   * implemented. It should not be used anywhere outside the parsing of * lemmas from a YAML *
   * witness for predicate abstraction.
   *
   * @param assumeDeclaration A string representation of a C variable declaration.
   * @param pPattern A regex that represents a C declaration of type and name.
   */
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

  /**
   * This method converts a string representing a c-type into the corresponding CType.
   *
   * <p>This method is only intended as a temporary workaround until a proper ACSL-parser has been
   * implemented. It should not be used anywhere outside the parsing of lemmas from a YAML * witness
   * for predicate abstraction.
   *
   * @param typeName A string that represents a C type.
   */
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
