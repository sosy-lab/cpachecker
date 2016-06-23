/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.tiger.test;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

public class PresenceConditionParser {

  private final static String LEFT_BRACKET = "(";
  private final static String RIGHT_BRACKET = ")";
  private final static String NOT = "!";
  private final static String AND = "&";
  private final static String OR = "|";
  private final static String TRUE = "TRUE";
  private final static String FALSE = "FALSE";
  private final static String SPACE = " ";

  public static BooleanFormula parseFormula(String formula, BooleanFormulaManagerView pBfmgr) {
    formula = formula.trim();

    List<String> tokens = scan(formula);
    return parseExpr(tokens, pBfmgr);
  }

  private static BooleanFormula parseExpr(List<String> pTokens, BooleanFormulaManagerView pBfmgr) {
    BooleanFormula formula = null;
    String token = null;

    while ((token = getToken(pTokens)) != null) {
      switch (token.toUpperCase()) {
        case RIGHT_BRACKET:
          return formula;
        case AND:
          token = getToken(pTokens);
          switch (token.toUpperCase()) {
            case LEFT_BRACKET:
              formula = pBfmgr.and(formula, parseExpr(pTokens, pBfmgr));
              break;
            case TRUE:
              formula = pBfmgr.and(formula, pBfmgr.makeBoolean(true));
              break;
            case FALSE:
              formula = pBfmgr.and(formula, pBfmgr.makeBoolean(false));
              break;
            default:
              formula = makeVariableFormula(pBfmgr, token);
              break;
          }
          break;
        case OR:
          token = getToken(pTokens);
          switch (token.toUpperCase()) {
            case LEFT_BRACKET:
              formula = pBfmgr.or(formula, parseExpr(pTokens, pBfmgr));
              break;
            case TRUE:
              formula = pBfmgr.or(formula, pBfmgr.makeBoolean(true));
              break;
            case FALSE:
              formula = pBfmgr.or(formula, pBfmgr.makeBoolean(false));
              break;
            default:
              formula = makeVariableFormula(pBfmgr, token);
              break;
          }
          break;
        case LEFT_BRACKET:
          formula = parseExpr(pTokens, pBfmgr);
          break;
        case TRUE:
          formula = pBfmgr.makeBoolean(true);
          break;
        case FALSE:
          formula = pBfmgr.makeBoolean(false);
          break;
        default:
          formula = makeVariableFormula(pBfmgr, token);
          break;
      }
    }

    return formula;
  }

  private static BooleanFormula makeVariableFormula(BooleanFormulaManagerView pBfmgr, String token) {
    if (token.startsWith(NOT)) {
      return pBfmgr.not(pBfmgr.makeVariable(parseName(token.substring(1))));
    } else {
      return pBfmgr.makeVariable(parseName(token));
    }
  }

  private static String parseName(String pName) {
    if (pName.endsWith("@0")) {
      return pName.substring(0, pName.length() - 2);
    } else {
      return pName;
    }
  }

  private static List<String> scan(String formula) {
    formula = formula.trim();
    List<String> tokens = new ArrayList<>();
    String token = "";
    for (int i = 0; i < formula.length(); i++) {
      char c = formula.charAt(i);
      String character = String.valueOf(c);

      switch (character.toUpperCase()) {
        case LEFT_BRACKET:
          if (token != "") {
            tokens.add(token);
            token = "";
          }
          tokens.add(LEFT_BRACKET);
          break;
        case RIGHT_BRACKET:
          if (token != "") {
            tokens.add(token);
            token = "";
          }
          tokens.add(RIGHT_BRACKET);
          break;
        case AND:
          tokens.add(AND);
          break;
        case OR:
          tokens.add(OR);
          break;
        //        case NOT:
        //          tokens.add(NOT);
        //          break;
        case TRUE:
          tokens.add(TRUE);
          break;
        case SPACE:
          if (token != "") {
            tokens.add(token);
            token = "";
          } else {
            continue;
          }
          break;
        case FALSE:
          tokens.add(FALSE);
          break;
        default:
          token += character;
          break;
      }
    }
    if (!token.isEmpty()) {
      tokens.add(token);
    }

    return tokens;
  }

  private static String getToken(List<String> pTokens) {
    if (pTokens.isEmpty()) { return null; }

    String token = pTokens.get(0);
    pTokens.remove(0);
    return token;
  }

}
