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

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.PolynomExpression;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.visitors.CollectVariablesVisitor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalculationHelper {

  /**
   * Calculates for a state
   */

  public static void calculateGroebnerBasis(LoopInvariantsState state, LogManager log) {
    String sympyCommand = "isympy";
    //    String sympyCommand = "/home/ecklbarb/Documents/Uni/SoSe16/Seminar/sympy/bin/isympy";

    ProcessBuilder pb = new ProcessBuilder(sympyCommand);
    Process sympy = null;

    try {
      sympy = pb.start();
    } catch (IOException e) {
      Error er = new Error("Failed to start the sympy subprocess.");
      er.initCause(e);
      throw er;
    }

    BufferedWriter stdin =
        new BufferedWriter(
            new OutputStreamWriter(sympy.getOutputStream(), Charset.defaultCharset()));
    BufferedReader stdout =
        new BufferedReader(new InputStreamReader(sympy.getInputStream(), Charset.defaultCharset()));
    BufferedReader stderr =
        new BufferedReader(new InputStreamReader(sympy.getErrorStream(), Charset.defaultCharset()));

    try {
      calculateGroebnerBasis(state, log, stdin, stdout, stderr);
      log.log(Level.INFO, readAllFromStream(stderr));
    } catch (IOException e) {
      Error er = new Error("Communication with the sympy subprocess failed.");
      er.initCause(e);
      throw er;
    }
  }

  private static void calculateGroebnerBasis(LoopInvariantsState state, LogManager log,
      BufferedWriter stdin, BufferedReader stdout, BufferedReader stderr) throws IOException {

    CollectVariablesVisitor collector = new CollectVariablesVisitor();
    Set<String> variables = new HashSet<>();

    List<String> polynomials = new LinkedList<>();

    for (PolynomExpression polynom : state.getPolynomies()) {
      variables.addAll(polynom.accept(collector));
      polynomials.add(polynom.toString());
    }

    String polynomsStr = preprocessingOfPolynomials(state, log).toString();

    String outPrefix = "sympyout:";

    writeToSympy(stdin, "n = Symbol('n')");
    for (String var : variables) {
      writeToSympy(stdin, var + " = Symbol('" + var + "')");
    }

    StringBuilder sb = new StringBuilder();
    sb.append("z = groebner(");

    sb.append(polynomsStr + " ");

    for (String var : variables) {
      sb.append(", " + var);
    }
    sb.append(")");

    writeToSympy(stdin, sb.toString());

    writeToSympy(stdin, "print \"" + outPrefix + "\" + str(z) + \"\\n\"");

    stdin.close();

    List<String> groebnerBasis = getMatchesFromStreamWithGroebner(stdout, outPrefix);

    List<Polynom> polynoms = new LinkedList<>();

    for (String invariant : groebnerBasis) {
      Polynom poly = new Polynom();
      poly.fromString(invariant);
      polynoms.add(poly);
    }

    state.setInvariant(polynoms);
  }

  private static void writeToSympy(BufferedWriter stdin, String msg) throws IOException {
    stdin.write(msg);
    stdin.newLine();
  }

  private static String readAllFromStream(BufferedReader s) throws IOException {
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = s.readLine()) != null) {
      sb.append(line);
      sb.append('\n');
    }
    return sb.toString();
  }

  /**
   * Gives the computed Groebner Base as Strings
   *
   * @param s   input string
   * @param prefix    start of the output
   * @return polynoms as Strings
   */
  private static List<String> getMatchesFromStreamWithGroebner(BufferedReader s, String prefix)
      throws IOException {
    String line;
    while ((line = s.readLine()) != null) {
      if (line.contains(prefix)) {
        line = line.replaceFirst(".*" + prefix, "");
        Pattern p = Pattern.compile("GroebnerBasis\\(\\[([^\\[\\]]+)\\].*");
        Matcher matsch = p.matcher(line);
        if (matsch.matches()) {
          String basisStr = matsch.group(1);
          String[] basisPolyStrs = basisStr.split(", ");
          List<String> res = new LinkedList<>();
          for (String str : basisPolyStrs) {
            res.add(str.replace("**", "^"));
          }
          return res;
        } else {
          throw new RuntimeException("Groebner Basis can not be calculated.");
        }
      }
    }
    return null;
  }

  private static List<String> getMatchesFromStream(BufferedReader s, String prefix, String var)
      throws IOException {
    String line;
    List<String> res = new LinkedList<>();
    String output;
    while ((line = s.readLine()) != null) {
      if (line.contains(prefix)) {
        output = line.replaceFirst(".*" + prefix, "");
        res.add(output.replace("C0", var));
      }

    }
    return res;
  }

  private static List<String> preprocessingOfPolynomials(LoopInvariantsState state,
      LogManager log) {

    try {
      return preprocessingOfPolynomialsHelper(state, log);
    } catch (IOException e) {
      Error er = new Error("Communication with the sympy subprocess failed.");
      er.initCause(e);
      throw er;
    }
  }

  private static List<String> preprocessingOfPolynomialsHelper(LoopInvariantsState state, LogManager log)
      throws IOException {
    String outPrefix = "sympyout:";

    CollectVariablesVisitor collector = new CollectVariablesVisitor();
    Set<String> variables = new HashSet<>();
    List<String> closedFormPolynomials = new LinkedList<>();

    for (PolynomExpression polynom : state.getPolynomies()) {
      String sympyCommand = "isympy";
      //    String sympyCommand = "/home/ecklbarb/Documents/Uni/SoSe16/Seminar/sympy/bin/isympy";

      ProcessBuilder pb = new ProcessBuilder(sympyCommand);
      Process sympy = null;

      try {
        sympy = pb.start();
      } catch (IOException e) {
        Error er = new Error("Failed to start the sympy subprocess.");
        er.initCause(e);
        throw er;
      }

      BufferedWriter stdin =
          new BufferedWriter(
              new OutputStreamWriter(sympy.getOutputStream(), Charset.defaultCharset()));
      BufferedReader stdout =
          new BufferedReader(
              new InputStreamReader(sympy.getInputStream(), Charset.defaultCharset()));
      BufferedReader stderr =
          new BufferedReader(
              new InputStreamReader(sympy.getErrorStream(), Charset.defaultCharset()));

      variables = polynom.accept(collector);
      String polyStr = polynom.toString();
      StringBuilder sb = new StringBuilder();
      sb.append("z = rsolve(");
      sb.append(polyStr + ", ");
      String var = variables.iterator().next();

      int openBracket = var.indexOf("(");
      String startVar = var.substring(0, openBracket);
      sb.append(startVar + "(n)");

      //Startwert
      if (state.getVariableValueMap().containsKey(startVar)) {
        sb.append(", {" + startVar + "(0):" + state.getVariableValueMap().get(startVar) + "}");
      }

      sb.append(")");
      writeToSympy(stdin, sb.toString());

      writeToSympy(stdin, "print \"" + outPrefix + "\" + str(z) + \"\\n\"");
      stdin.close();
      List<String> closedFormPoly = getMatchesFromStream(stdout, outPrefix, startVar);
      if (!closedFormPoly.isEmpty()) {
        closedFormPolynomials.add(closedFormPoly.get(0) + " - " + startVar + "(n)");
      } else {
        throw new RuntimeException(
            "The closed form of the polynomial '" + polyStr + "' can not be computed.");
      }
      variables = null;
      log.log(Level.INFO, readAllFromStream(stderr));
      stderr.close();
      stdout.close();
    }
    return closedFormPolynomials;
  }
}
