// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ast.c.ACSLFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.ACSLVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
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

  public static CExpression parseACSLExpression(String lAssumeCode, CParser parser, Scope scope)
      throws InterruptedException, InvalidAutomatonException {

    String lString = lAssumeCode;
    Map<String, ACSLFunctionCall> replacements = new HashMap<String, ACSLFunctionCall>();
    Pattern lp = Pattern.compile("LEMMA_FUNC\\((?<function>.*)\\)");
    Matcher lm = lp.matcher(lString);
    String tmp = "lemma_tmp_";

    while (lm.find()) {
      String functionCall = lm.group("function");
      ACSLFunctionCall lFuncCall = extractFunctionCall(functionCall, parser, scope);
      String key = tmp + replacements.size();
      replacements.put(key, lFuncCall);
      lString = lm.replaceFirst(key);
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
    return exp;
  }
}
