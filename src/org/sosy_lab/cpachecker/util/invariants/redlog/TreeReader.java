/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.invariants.redlog;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.util.invariants.Rational;

public class TreeReader {

  //private static boolean isVerbose = false;

  /**
  public static void verbose(boolean v) {
    isVerbose = v;
  }

  public static String read(IASTNode N) {
    return padread(N,"");
  }

  private static String padread(IASTNode N, String pad) {
    String s = "";
    //
    if (isVerbose) {
      s += pad + N.getClass().getName() + "\n";
    }
    //
    s += pad + N.getRawSignature() + "\n";

    String cn = N.getClass().getName();
    if (cn.endsWith("BinaryExpression")) {
      IASTBinaryExpression BE = (IASTBinaryExpression) N;
      s += pad + "    " + BE.getOperator().getOperator() + "\n";
    }

    IASTNode[] children = N.getChildren();
    IASTNode C;
    for (int i = 0; i < children.length; i++) {
      C = children[i];
      s += TreeReader.padread(C, pad+"    ");
    }
    return s;
  }

  public static Vector<IASTNode> findDenominators(IASTNode N) {
    // We do depth-first search, so that if ever we return both a
    // term t and a subterm t' thereof, the subterm t' will come
    // before t in the list. This way, when it is time to try
    // evaluating these terms, we can stop as soon as one of them
    // is zero, and in this way we will never get caught trying to
    // evaluate a term in which division by zero is taking place.

    Vector<IASTNode> denoms = new Vector<IASTNode>();

    // Recursion first, for depth-first search.
    IASTNode[] children = N.getChildren();
    IASTNode C;
    Vector<IASTNode> recurDenoms;
    for (int i = 0; i < children.length; i++) {
      C = children[i];
      recurDenoms = TreeReader.findDenominators(C);
      denoms.addAll(recurDenoms);
    }

    // Now N itself.
    String cn = N.getClass().getName();
    if (cn.endsWith("BinaryExpression")) {
      IASTBinaryExpression BE = (IASTBinaryExpression) N;
      // Now check whether the operation is '/', and if so, then
      // add the second operand to the denoms Vector.
      String op = BE.getOperator().getOperator();
      if (op.equals("/")) {
        IASTNode denom = BE.getOperand2();
        denoms.add(denom);
      }
    }

    return denoms;
  }

  public static HashSet<String> getAllVars(IASTNode N) {
    // We return a set of the names of all variables appearing
    // in the expression N, which we assume involves only binary
    // and unary expressions, and integers and variables.

    HashSet<String> vars = new HashSet<String>();

    // Recursion first.
    IASTNode[] children = N.getChildren();
    IASTNode C;
    HashSet<String> recurVars;
    for (int i = 0; i < children.length; i++) {
      C = children[i];
      recurVars = TreeReader.getAllVars(C);
      vars.addAll(recurVars);
    }

    // Now N itself.
    String cn = N.getClass().getName();
    if (cn.endsWith("IdExpression")) {
      IASTIdExpression I = (IASTIdExpression) N;
      String v = I.getName().toString();
      vars.add(v);
    }

    return vars;
  }
  */

  public static Rational evaluate(IASTNode N, Substitution S) {
    Rational r = null;
    String cn = N.getClass().getName();
    if (cn.endsWith("BinaryExpression")) {
      IASTBinaryExpression BE = (IASTBinaryExpression) N;
      String op = BE.getOperator().getOperator();
      Rational a = TreeReader.evaluate(BE.getOperand1(), S);
      Rational b = TreeReader.evaluate(BE.getOperand2(), S);
      try {
        r = a.operate(op,b);
      } catch (Exception e) {}
    } else if (cn.endsWith("UnaryExpression")) {
      // We assume the operator is "MINUS".
      IASTUnaryExpression U = (IASTUnaryExpression) N;
      Rational a = TreeReader.evaluate(U.getOperand(), S);
      try {
        r = a.times(new Rational(-1,1));
      } catch (Exception e) {}
    } else if (cn.endsWith("IntegerLiteralExpression")) {
      IASTIntegerLiteralExpression I = (IASTIntegerLiteralExpression) N;
      BigInteger n = I.getValue();
      int m = n.intValue();
      try {
        r = new Rational(m,1);
      } catch (Exception e) {}
    } else if (cn.endsWith("IdExpression")) {
      IASTIdExpression ID = (IASTIdExpression) N;
      String v = ID.getName().toString();
      r = S.get(v);
    // TODO: proper error checking
    } else {
      System.err.println("Unrecognized node type.");
    }
    return r;
  }

}
