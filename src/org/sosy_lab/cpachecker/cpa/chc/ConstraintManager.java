/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.chc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import jpl.Compound;
import jpl.JPL;
import jpl.Query;
import jpl.Term;
import jpl.Util;
import jpl.Variable;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

public class ConstraintManager {

  private static LogManager logger;

  public static boolean init(String firingRelation, String generalizationOperator, LogManager logM) {

    String initstr[] = {"swipl", "-x", "./lib/native/x86_64-linux/chc_lib", "-g", "true", "-nosignals"};

    boolean init = JPL.init(initstr);

    initFiringRelation(firingRelation);
    initGeneralizationOperator(generalizationOperator);
    logger = logM;

    return init;
  }

  public static Constraint simplify(ArrayList<Term> cn, HashMap<String,Term> vars) {

    // Constraint to be solved
    Term constraint = Util.termArrayToList(cn.toArray(new Term[0]));
    // Create a list of variable to solve the constraint
    Term varList = Util.termArrayToList(vars.values().toArray(new Term[0]));
    // Solve constraint w.r.t. variables occurring in varList
    Term args[] = {constraint, varList, new Variable("S")};
    Query q = new Query("solve", args);

    logger.log(Level.FINEST, "\n * solve (w.r.t. " + varList.toString() + ")");

    @SuppressWarnings("unchecked")
    Hashtable<String,Term> sol = q.oneSolution();

    return ConstraintManager.normalize("S", sol);
  }

  public static boolean subsumes(Constraint cn1, Constraint cn2) {

    // Constraint 1
    Term constraint1 = Util.termArrayToList(cn1.getConstraint().toArray(new Term[0]));
    // Constraint 2
    Term constraint2 = Util.termArrayToList(cn2.getConstraint().toArray(new Term[0]));

    Term args[] = {constraint1, constraint2};

    Query q = new Query("entails", args);

    logger.log(Level.FINEST, "\n * " + cn1.toString() +
        "\n * entails" + "\n * " + cn2.toString() + ")");

    boolean res = q.hasSolution();

    logger.log(Level.FINEST, "\n * result: " + res);

    return res;
  }

  @SuppressWarnings("unchecked")
  public static Constraint generalize(Constraint cn1, Constraint cn2, Precision p) {

    // Constraint 1
    Term constraint1 = Util.termArrayToList(cn1.getConstraint().toArray(new Term[0]));
    // Constraint 2
    Term constraint2 = Util.termArrayToList(cn2.getConstraint().toArray(new Term[0]));

    Term args[] = {constraint1, constraint2, new Variable("G")};

    Query q = new Query("generalize", args);

    logger.log(Level.FINEST, "\n * definition: " + cn1.toString() +
        "\n * ancestor :  " + cn1.toString());

    return normalize("G",q.oneSolution());
  }

  public static Constraint and(Constraint cn1, Constraint cn2) {

    ArrayList<Term> andCn = new ArrayList<>(cn1.getConstraint());
    andCn.addAll(cn2.getConstraint());

    logger.log(Level.FINEST, "\n * " + cn1.toString() + "\n * and \n * " + cn2.toString());

    /*
     * create a list of variable to solve the constraint
     * Remove all non primed variables which occur in
     * the set of primed variables
     */
    HashMap<String,Term> newVars = ConstraintManager.selectVariables(
        cn1.getVars(), cn2.getVars());

    Constraint andConstraint = ConstraintManager.simplify(andCn, newVars);

    logger.log(Level.FINEST, "\n * " + andConstraint.toString());

    return andConstraint;
  }

  private static HashMap<String,Term> selectVariables(
    HashMap<String,Term> vars,  HashMap<String,Term> pVars) {

    HashMap<String,Term> newVars = new HashMap<>(pVars);

    for (Map.Entry<String, Term> me : vars.entrySet()) {
      if(! pVars.containsKey(me.getKey())) {
        newVars.put(me.getKey(), me.getValue());
      }
    }

    return newVars;
  }

  private static Constraint normalize(String sol, Hashtable<String,Term> varMap) {

    // fetches the solution
    Term cn = varMap.get(sol);

    String newConstraint = cn.toString();

    Constraint nres = new Constraint();

    if (Constraint.isFalse(newConstraint)) {
      return nres.setFalse();
    }


    Hashtable<String,Term> varSolMap = new Hashtable<>(varMap);

    varSolMap.remove(sol);

    /*
     * replace all occurrences of primed variables
     * by their corresponding unprimed version
     */
    for (Map.Entry<String, Term> me : varSolMap.entrySet()) {
      newConstraint = newConstraint.replaceAll(
        me.getValue().toString(),
        ConstraintManager.primedVarToVar(me.getKey()));
      nres.addVar(var2CVar(ConstraintManager.primedVarToVar(me.getKey())),
        new Variable(primedVarToVar(me.getKey())));
    }

    // TODO: to be improved
    nres.setConstraint(new ArrayList<>(
      Arrays.asList(Util.listToTermArray(Util.textToTerm(newConstraint)))));

    logger.log(Level.FINEST, "\n * result: " + nres.toString());

    return nres;
  }


  public static ArrayList<Constraint> getConstraint(AssumeEdge ae) {
    CBinaryExpression c = (CBinaryExpression)(ae.getExpression());
    Collection<Pair<Term,ArrayList<Term>>> acList;
    ArrayList<Constraint> cns = new ArrayList<>(2);

    if (ae.getTruthAssumption()) {
      // atomic constraint
      acList = expressionToCLP(c);
      // for all term in the list create a constraint
      for (Pair<Term,ArrayList<Term>> p : acList) {
        cns.add(new Constraint(p.getFirst(), p.getSecond()));
      }
    } else {
      // negated atomic constraint
      CBinaryExpression negbe = getNegatedRelOperator(c);
      acList = expressionToCLP(negbe);
      for (Pair<Term,ArrayList<Term>> p : acList) {
        cns.add(new Constraint(p.getFirst(), p.getSecond()));
      }
    }

    return cns;
  }


  public static Constraint getConstraint(CAssignment ca) {
    Constraint c = new Constraint();
    CExpression lhs = ca.getLeftHandSide();
    CRightHandSide rhs = ca.getRightHandSide();
    CExpression exp = (CExpression) rhs;
    c.addVar(lhs.toString(),ConstraintManager.CVar2PrologPrimedVar(lhs.toString()));
    if (lhs instanceof AIdExpression) {
      for (Pair<Term,ArrayList<Term>> t: expressionToCLP(exp)) {
        Term[] operands = {
          c.getVars().get(lhs.toString()),
          t.getFirst()
        };
        ArrayList<Term> list = new ArrayList<>();
        list.add(new Compound("=:=",operands));
        c.setConstraint(list);
      }
    } else {
      throw new AssertionError("unhandled assignment " + ca.toString());
    }
    return c;
  }


  public static Constraint getConstraint(CExpression exp) {
    ArrayList<Term> tlist= new ArrayList<>();
    ArrayList<Term> vlist= new ArrayList<>();
    for (Pair<Term,ArrayList<Term>> t: expressionToCLP(exp)) {
        tlist.add(t.getFirst());
        vlist.addAll(t.getSecond());
      }
    return new Constraint(tlist,vlist);
  }


  public static ArrayList<Constraint> getConstraint(List<CExpression> exp) {
    ArrayList<Constraint> clist = new ArrayList<>();
    for (CExpression c: exp) {
      clist.add(getConstraint(c));
    }
    return clist;
  }


  public static Constraint getConstraint(ADeclarationEdge ae) {
    CDeclaration decl = (CDeclaration)ae.getDeclaration();
    Constraint ac = new Constraint();
    if (decl instanceof CVariableDeclaration) {
      CVariableDeclaration vdecl = (CVariableDeclaration) decl;
      IAInitializer initializer = vdecl.getInitializer();
      String varName = vdecl.getName();
      Term lhs = CVar2PrologPrimedVar(varName);
      if (initializer != null) {
        if (initializer instanceof CInitializerExpression) {
          CExpression expression = ((CInitializerExpression)initializer).getExpression();
          Collection<Pair<Term,ArrayList<Term>>> at = expressionToCLP(expression);
          for (Pair<Term,ArrayList<Term>> t: at) {
            Term rhs = t.getFirst();
            ArrayList<Term> acList = new ArrayList<>();
            acList.add(new Compound("=:=", new Term[] {lhs,rhs}));
            ac.setConstraint(acList);
          }
        }
      }
      ac.addVar(varName, lhs);
    }
    return ac;
  }


  public static Constraint getConstraint(AReturnStatementEdge aRetEdge)
    throws UnrecognizedCCodeException {

    IAExpression expression = aRetEdge.getExpression();

    if (expression == null) {
      expression = CNumericTypes.ZERO; // this is the default in C
    }

    String varName = "FRET_" + aRetEdge.getSuccessor().getFunctionName();

    Term lhs = CVar2PrologVar(varName);

    Constraint ac = new Constraint();

    Collection<Pair<Term,ArrayList<Term>>> at = expressionToCLP(expression);
    for (Pair<Term,ArrayList<Term>> t: at) {
      Term rhs = t.getFirst();
      ArrayList<Term> acList = new ArrayList<>();
      acList.add(new Compound("=:=", new Term[] {lhs,rhs}));
      ac.setConstraint(acList);
    }

    ac.addVar(varName, lhs);

    return ac;
  }


  public static Constraint getConstraint(FunctionReturnEdge fretEdge)
    throws UnrecognizedCCodeException {

    FunctionSummaryEdge summaryEdge = fretEdge.getSummaryEdge();
    AFunctionCall exprOnSummary  = summaryEdge.getExpression();

    // expression is an assignment operation, e.g. a = g(b);
    if (exprOnSummary instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement assignExp = ((AFunctionCallAssignmentStatement)exprOnSummary);
      IAExpression op1 = assignExp.getLeftHandSide();

      // we expect left hand side of the expression to be a variable
      if ((op1 instanceof AIdExpression) || (op1 instanceof CFieldReference)) {

        String varName = "FRET_" + fretEdge.getPredecessor().getFunctionName();
        Term lhs = CVar2PrologPrimedVar(op1.toString());
        Term rhs = CVar2PrologVar(varName);
        Constraint ac = new Constraint(new ArrayList<Term>(
            Collections.singletonList(new Compound("=:=", new Term[] {lhs,rhs}))));

        ac.addVar(op1.toString(), lhs);

        return ac;
      }
      // TODO: a[x] = b();
      else if (op1 instanceof CArraySubscriptExpression) {
        return new Constraint();
      } else {
        throw new UnrecognizedCCodeException("on function return", summaryEdge, null);
      }
    }
    return new Constraint();
  }


  public static Constraint getConstraint(CExpression lhs, CFunctionCallExpression rhs) {
    Constraint c = new Constraint();
    if (rhs != null) {
      c.addVar(lhs.toString(),ConstraintManager.CVar2PrologPrimedVar(lhs.toString()));
      if (lhs instanceof AIdExpression) {
        Term[] operands = {
          c.getVars().get(lhs.toString()),
          CVar2PrologVar(rhs.getFunctionNameExpression().toString())
        };
        ArrayList<Term> list = new ArrayList<>();
        list.add(new Compound("=:=",operands));
        c.setConstraint(list);
      }
    }
    return c;
  }


  public static Collection<Constraint> getConstraint(List<String> names,
      List<? extends IAExpression> expressions) {

    ArrayList<Constraint> cnList = new ArrayList<>();

    for (int i = 0; i < names.size(); i++) {

      String name = names.get(i);
      IAExpression expression = expressions.get(i);

      for (Pair<Term,ArrayList<Term>> p: paramExpressionToCLP(name, expression)) {
        cnList.add(new Constraint(
            new ArrayList<>(Arrays.asList(Util.listToTermArray(p.getFirst()))),p.getSecond()));
      }
    }

    return cnList;
  }


  /**
   * input:  ppv is of the form "_$CVAR", where
   *         $CVAR stands for a C program variable
   * output: "$CVAR"
   */
  public static String var2CVar(String pv) {
    return pv.substring(4);
  }


  /**
   * input:  ppv is of the form "_p_$CVAR", where
   *         $CVAR stands for a C program variable
   * output: "$CVAR"
   */
  public static String primedVar2CVar(String ppv) {
    return ppv.substring(11);
  }


  public static Variable CVar2PrologVar(String cv) {
    return new Variable("CPA_" + cv);
  }


  public static Variable CVar2PrologPrimedVar(String cv) {
    return new Variable("Primed_CPA_" + cv);
  }


  private static String primedVarToVar(String pvar) {
    if (pvar.startsWith("Primed_")) {
      return pvar.replace("Primed_","");
    }
    return pvar;
  }


  private static Collection<Pair<Term,ArrayList<Term>>> getNegatedConstraintList(Pair<Term,ArrayList<Term>> cn) {

      Compound atomCnT = (Compound)cn.getFirst();
      Compound negAtomCnT = null;
      switch ( atomCnT.name() ) {
        case "<":
          negAtomCnT = new Compound(">=", 2);
          negAtomCnT.setArg(1, atomCnT.arg(1));
          negAtomCnT.setArg(2, atomCnT.arg(2));
          return Collections.singleton(Pair.of((Term)negAtomCnT, cn.getSecond()));
        case "=<":
          negAtomCnT = new Compound(">", 2);
          negAtomCnT.setArg(1, atomCnT.arg(1));
          negAtomCnT.setArg(2, atomCnT.arg(2));
          return Collections.singleton(Pair.of((Term)negAtomCnT, cn.getSecond()));
        case ">":
          negAtomCnT = new Compound("=<", 2);
          negAtomCnT.setArg(1, atomCnT.arg(1));
          negAtomCnT.setArg(2, atomCnT.arg(2));
          return Collections.singleton(Pair.of((Term)negAtomCnT, cn.getSecond()));
        case ">=":
          negAtomCnT = new Compound("<", 2);
          negAtomCnT.setArg(1, atomCnT.arg(1));
          negAtomCnT.setArg(2, atomCnT.arg(2));
          return Collections.singleton(Pair.of((Term)negAtomCnT, cn.getSecond()));
        case "=:=":
          return Arrays.asList(
              Pair.of((Term)new Compound("<", new Term[] {atomCnT.arg(1), atomCnT.arg(2)}), cn.getSecond()),
              Pair.of((Term)new Compound(">", new Term[] {atomCnT.arg(1), atomCnT.arg(2)}), cn.getSecond()) );
        default:
          return null;
      }
  }

  private static CBinaryExpression getNegatedRelOperator(CBinaryExpression be) {

    switch (be.getOperator()) {
      case EQUALS:
        return new CBinaryExpression(
            be.getFileLocation(),
            be.getExpressionType(),
            be.getCalculationType(),
            be.getOperand1(),
            be.getOperand2(),
            BinaryOperator.NOT_EQUALS );
      case NOT_EQUALS:
        return new CBinaryExpression(
            be.getFileLocation(),
            be.getExpressionType(),
            be.getCalculationType(),
            be.getOperand1(),
            be.getOperand2(),
            BinaryOperator.EQUALS );
      case LESS_THAN:
        return new CBinaryExpression(
            be.getFileLocation(),
            be.getExpressionType(),
            be.getCalculationType(),
            be.getOperand1(),
            be.getOperand2(),
            BinaryOperator.GREATER_EQUAL );
      case LESS_EQUAL:
        return new CBinaryExpression(
            be.getFileLocation(),
            be.getExpressionType(),
            be.getCalculationType(),
            be.getOperand1(),
            be.getOperand2(),
            BinaryOperator.GREATER_THAN );
      case GREATER_THAN:
        return new CBinaryExpression(
            be.getFileLocation(),
            be.getExpressionType(),
            be.getCalculationType(),
            be.getOperand1(),
            be.getOperand2(),
            BinaryOperator.LESS_EQUAL );
      case GREATER_EQUAL:
        return new CBinaryExpression(
            be.getFileLocation(),
            be.getExpressionType(),
            be.getCalculationType(),
            be.getOperand1(),
            be.getOperand2(),
            BinaryOperator.LESS_THAN );
      default: // not a relational operator
        return null;
    }
  }


  private static Collection<Pair<Term,ArrayList<Term>>> expressionToCLP(IAExpression ce) {

    ArrayList<Term> vars = new ArrayList<>();

    if (ce instanceof CIdExpression) {
      vars.add(CVar2PrologVar(ce.toString()));
      return Collections.singleton(Pair.of((Term)CVar2PrologVar(ce.toString()), vars));
    } else if (ce instanceof CIntegerLiteralExpression) {
      return Collections.singleton(Pair.of(Util.textToTerm("rdiv(" + ce.toString() + ",1)"), vars));
    } else if (ce instanceof CBinaryExpression ) {
      CBinaryExpression bexp = (CBinaryExpression)ce;
      Collection<Pair<Term,ArrayList<Term>>> operand1 = expressionToCLP(bexp.getOperand1());
      Collection<Pair<Term,ArrayList<Term>>> operand2 = expressionToCLP(bexp.getOperand2());
      switch (bexp.getOperator()) {
        case PLUS:
          return addOperands("+", operand1, operand2);
        case MINUS:
          return addOperands("-", operand1, operand2);
        case MULTIPLY:
          return addOperands("*", operand1, operand2);
        case DIVIDE:
          return addOperands("/", operand1, operand2);
        case EQUALS:
          return addOperands("=:=", operand1, operand2);
        case NOT_EQUALS:
          Collection<Pair<Term,ArrayList<Term>>> opUnion = new ArrayList<>(
              addOperands(">", operand1, operand2));
          opUnion.addAll(addOperands("<", operand1, operand2));
          return opUnion;
        case LESS_THAN:
          return addOperands("<", operand1, operand2);
        case LESS_EQUAL:
          return addOperands("=<", operand1, operand2);
        case GREATER_THAN:
          return addOperands(">", operand1, operand2);
        case GREATER_EQUAL:
          return addOperands(">=", operand1, operand2);
        default:
          return null;
      }
    } else {
      return null;
    }
  }


  private static Collection<Pair<Term,ArrayList<Term>>> paramExpressionToCLP(
      String paramName, IAExpression ce) {

    ArrayList<Term> vars = new ArrayList<>();

    Term paramVariable = CVar2PrologVar(paramName);
    vars.add(paramVariable);
    Term expTerm = null;

    if (ce instanceof CIdExpression) {
      vars.add(CVar2PrologVar(ce.toString()));
      expTerm = CVar2PrologVar(ce.toString());
      Term paramAexpTerm = new Compound("=:=", new Term[] {paramVariable, expTerm});
      return Collections.singleton(Pair.of(Util.termArrayToList(new Term[] {paramAexpTerm}), vars));
    } else if (ce instanceof CIntegerLiteralExpression) {
      expTerm = Util.textToTerm("rdiv(" + ce.toString() + ",1)");
      Term paramAexpTerm = new Compound("=:=", new Term[] {paramVariable, expTerm});
      return Collections.singleton(Pair.of(Util.termArrayToList(new Term[] {paramAexpTerm}), vars));
    } else if (ce instanceof CBinaryExpression ) {
      CBinaryExpression bexp = (CBinaryExpression)ce;
      Collection<Pair<Term,ArrayList<Term>>> aexpTerms = expressionToCLP(ce);
      Collection<Pair<Term,ArrayList<Term>>> paramAexpTerms = new ArrayList<>(aexpTerms.size());
      switch (bexp.getOperator()) {
        case PLUS:
        case MINUS:
        case MULTIPLY:
        case DIVIDE:
          for (Pair<Term,ArrayList<Term>> aexpTerm : aexpTerms) {
            ArrayList<Term> aexpTermVars = new ArrayList<>(aexpTerm.getSecond());
            aexpTermVars.add(paramVariable);
            Term paramAexpTerm = new Compound("=:=", new Term[] {paramVariable, aexpTerm.getFirst()});
            paramAexpTerms.add(Pair.of(Util.termArrayToList(new Term[] {paramAexpTerm}), aexpTermVars));
          }
          return paramAexpTerms;
        // add an extra atomic constraint
        case EQUALS:
        case LESS_THAN:
        case LESS_EQUAL:
        case GREATER_THAN:
        case GREATER_EQUAL:
        case NOT_EQUALS:
          for (Pair<Term,ArrayList<Term>> aexpTerm : aexpTerms) {
            ArrayList<Term> aexpTermVars = new ArrayList<>(aexpTerm.getSecond());
            aexpTermVars.add(paramVariable);
            Term paramAexpTerm = new Compound("=:=", new Term[] {paramVariable, Util.textToTerm("rdiv(1,1)")});
            paramAexpTerms.add(Pair.of(Util.termArrayToList(new Term[] {paramAexpTerm, aexpTerm.getFirst()}), aexpTermVars));
            paramAexpTerm = new Compound("=:=", new Term[] {paramVariable, Util.textToTerm("rdiv(0,1)")});
            for (Pair<Term,ArrayList<Term>> negAexpTerm :  getNegatedConstraintList(aexpTerm)) {
              paramAexpTerms.add(Pair.of(Util.termArrayToList(new Term[] {paramAexpTerm, negAexpTerm.getFirst()}), aexpTermVars));
            }
          }
          return paramAexpTerms;
        default:
          return null;
      }
    } else {
      return null;
    }
  }


  private static Collection<Pair<Term,ArrayList<Term>>> addOperands(String operator,
      Collection<Pair<Term,ArrayList<Term>>> operand1, Collection<Pair<Term,ArrayList<Term>>> operand2) {

    Collection<Pair<Term,ArrayList<Term>>> termList = new ArrayList<>();
    ArrayList<Term> vars = new ArrayList<>();

    for (Pair<Term,ArrayList<Term>> subop1 : operand1) {
      for (Pair<Term,ArrayList<Term>> subop2 : operand2) {
        vars.addAll(subop1.getSecond());
        vars.addAll(subop2.getSecond());
        termList.add(Pair.of(
            (Term)new Compound(operator, new Term[] {subop1.getFirst(), subop2.getFirst()}),
            vars));
      }
    }

    return termList;
  }


  private static boolean initFiringRelation(String firingRelation) {

    String qStr = "assert((less(C1,C2)";

    switch (firingRelation) {
      case "Always":
        break;
      case "Maxcoeff":
        qStr += ":-less_maxcoeff_cns(C1,C2)";
        break;
      case "Sumcoeff":
        qStr += ":-less_maxsum_cns(C1,C2)";
        break;
      case "Homeocoeff":
        qStr += ":-homeo_embedded_cns(C1,C2)";
        break;
      default:
        throw new AssertionError("Not valid value for the firing relation");
    }

    qStr += "))";

    Query q = new Query(qStr);

    return q.hasSolution();
  }


  private static boolean initGeneralizationOperator(String generalizationOperator) {

    String qStr = "assert((generalize(C1,C2,C3)";

    switch (generalizationOperator) {
      case "Top":
        break;
      case "Widen":
        qStr += ":-plain_cns_widening(C1,C2,C3)";
        break;
      case "WidenMax":
        qStr += ":-e_leq_maxcoeff(C1,C2,C3)";
        break;
      case "WidenSum":
        qStr += ":-e_leq_maxsum(C1,C2,C3)";
        break;
      default:
        throw new AssertionError("invalid value for the firing relation");
    }

    qStr += "))";

    Query q = new Query(qStr);

    return q.hasSolution();
  }


  public static Constraint convexHull(Constraint cn1, Constraint cn2s) {
    return new Constraint();
  }

  private ConstraintManager() {
  }

}