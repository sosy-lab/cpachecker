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
package org.sosy_lab.cpachecker.cpa.loopinvariants;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.PolynomExpression;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.visitors.CollectVariablesVisitor;

public class CalculationHelper {

  /**
   * Calculates for a state
   */
  public static List<Polynom> calculateGroebnerBasis(List<PolynomExpression> polynomials,
      Map<String, Double> valueMap, LogManager log) {

    try (SympyProcess sympy = SympyProcess.newProcess()) {
      return calculateGroebnerBasis(polynomials, valueMap, log, sympy);
    } catch (IOException e) {
      Error er = new Error("Communication with the sympy subprocess failed.");
      er.initCause(e);
      throw er;
    }
  }

  private static List<Polynom> calculateGroebnerBasis(
      List<PolynomExpression> pPolynomials,
      Map<String, Double> pValueMap,
      LogManager pLog,
      SympyProcess pSympy)
      throws IOException {

    CollectVariablesVisitor collector = new CollectVariablesVisitor();
    Set<String> variables = new HashSet<>();

    for (PolynomExpression polynom : pPolynomials) {
      variables.addAll(polynom.accept(collector));
    }

    String polynomsStr = preprocessingOfPolynomials(pPolynomials, pValueMap, pLog).toString();

    pSympy.sendLine("n = Symbol('n')");
    for (String var : variables) {
      pSympy.sendLine(var + " = Symbol('" + var + "')");
    }

    StringBuilder sb = new StringBuilder();
    sb.append("z = groebner(");

    sb.append(polynomsStr + " ");

    for (String var : variables) {
      sb.append(", " + var);
    }
    sb.append(")");

    pSympy.sendLine(sb.toString());

    pSympy.sendLine("print str(z) + \"\\n\"");

    pSympy.commit();

    List<String> groebnerBasis = getMatchesFromStreamWithGroebner(pSympy);

    List<Polynom> polynoms = new LinkedList<>();

    for (String invariant : groebnerBasis) {
      Polynom poly = new Polynom();
      poly.fromString(invariant);
      polynoms.add(poly);
    }

    return polynoms;
  }

  private static String readAllStderr(SympyProcess pSympy) throws IOException {
    try {
      return pSympy.readErrorLines().collect(Collectors.joining(System.lineSeparator()));
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  /**
   * Attempts to compute Groebner Base as polynoms represented by Strings
   *
   * @param pSympy the sympy process.
   * @return the Groebner Base as polynoms represented by Strings.
   * @throws IOException if an IOException occurred during the communication with the sympy process.
   */
  private static List<String> getMatchesFromStreamWithGroebner(SympyProcess pSympy)
      throws IOException {
    String line;
    while ((line = pSympy.readLine()) != null) {
      Pattern p = Pattern.compile("GroebnerBasis\\(\\[([^\\[\\]]+)\\].*");
      Matcher matcher = p.matcher(line);
      if (matcher.matches()) {
        String basisStr = matcher.group(1);
        String[] basisPolyStrs = basisStr.split(", ");
        List<String> res = new ArrayList<>(basisPolyStrs.length);
        for (String str : basisPolyStrs) {
          res.add(str.replace("**", "^"));
        }
        return res;
      } else {
        throw new RuntimeException("Groebner Basis can not be calculated.");
      }
    }
    return Collections.emptyList();
  }

  private static List<String> getMatchesFromSympy(SympyProcess pSympy, String pVar)
      throws IOException {
    final List<String> result;
    try {
      result = pSympy.readLines().map(l -> l.replace("C0", pVar)).collect(Collectors.toList());
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
    return result;
  }

  private static List<String> preprocessingOfPolynomials(List<PolynomExpression> polynomials,
      Map<String, Double> valueMap,
      LogManager log) {

    try {
      return preprocessingOfPolynomialsHelper(polynomials, valueMap, log);
    } catch (IOException e) {
      Error er = new Error("Communication with the sympy subprocess failed.");
      er.initCause(e);
      throw er;
    }
  }

  private static List<String> preprocessingOfPolynomialsHelper(List<PolynomExpression> polynomials,
      Map<String, Double> valueMap, LogManager log)
      throws IOException {

    CollectVariablesVisitor collector = new CollectVariablesVisitor();
    Set<String> variables;
    List<String> closedFormPolynomials = new LinkedList<>();

    for (PolynomExpression polynom : polynomials) {

      variables = polynom.accept(collector);
      String polyStr = polynom.toString();
      StringBuilder sb = new StringBuilder();
      sb.append("z = rsolve(");
      sb.append(polyStr + ", ");
      String var = variables.iterator().next();
      final List<String> closedFormPoly;

      int openBracket = var.indexOf("(");
      String startVar = var.substring(0, openBracket);

      try (SympyProcess sympy = SympyProcess.newProcess()) {

        sb.append(startVar + "(n)");

        //Startwert
        if (valueMap.containsKey(startVar)) {
          sb.append(", {" + startVar + "(0):" + valueMap.get(startVar) + "}");
        }

        sb.append(")");
        sympy.sendLine(sb.toString());

        sympy.sendLine("print str(z) + \"\\n\"");
        sympy.commit();
        closedFormPoly = getMatchesFromSympy(sympy, startVar);
        if (log.wouldBeLogged(Level.INFO)) {
          String errorOut = readAllStderr(sympy).trim();
          if (!errorOut.isEmpty()) {
            log.log(Level.INFO, errorOut);
          }
        }
      }
      if (!closedFormPoly.isEmpty()) {
        closedFormPolynomials.add(closedFormPoly.get(0) + " - " + startVar + "(n)");
        log.log(Level.FINE, "Closed form of " + polyStr + " : " + closedFormPoly.get(0).toString());
      } else {
        throw new RuntimeException(
            "The closed form of the polynomial '" + polyStr + "' can not be computed.");
      }
    }
    return closedFormPolynomials;
  }
}
