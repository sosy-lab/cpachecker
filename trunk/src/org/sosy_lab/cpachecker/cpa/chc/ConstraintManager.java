// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.chc;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import jpl.Compound;
import jpl.JPL;
import jpl.Query;
import jpl.Term;
import jpl.Util;
import jpl.Variable;
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
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
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.Pair;

public class ConstraintManager {

  private static LogManager logger;

  public static boolean init(
      String firingRelation, String generalizationOperator, LogManager logM) {

    String[] initstr = {
      "swipl",
      "-x",
      NativeLibraries.getNativeLibraryPath().resolve("chc_lib").toString(),
      "-g",
      "true",
      "-nosignals"
    };

    boolean init = JPL.init(initstr);

    initFiringRelation(firingRelation);
    initGeneralizationOperator(generalizationOperator);
    logger = logM;

    return init;
  }

  public static Constraint simplify(List<Term> cn, Map<String, Term> vars) {

    // Constraint to be solved
    Term constraint = Util.termArrayToList(cn.toArray(new Term[0]));
    // Create a list of variable to solve the constraint
    Term varList = Util.termArrayToList(vars.values().toArray(new Term[0]));
    // Solve constraint w.r.t. variables occurring in varList
    Term[] args = {constraint, varList, new Variable("S")};
    Query q = new Query("solve", args);

    logger.log(Level.FINEST, "\n * solve (w.r.t. " + varList + ")");

    @SuppressWarnings("unchecked")
    Map<String, Term> sol = q.oneSolution();

    return ConstraintManager.normalize("S", sol);
  }

  public static boolean subsumes(Constraint cn1, Constraint cn2) {

    // Constraint 1
    Term constraint1 = Util.termArrayToList(cn1.getConstraint().toArray(new Term[0]));
    // Constraint 2
    Term constraint2 = Util.termArrayToList(cn2.getConstraint().toArray(new Term[0]));

    Term[] args = {constraint1, constraint2};

    Query q = new Query("entails", args);

    logger.log(Level.FINEST, "\n * " + cn1 + "\n * entails" + "\n * " + cn2 + ")");

    boolean res = q.hasSolution();

    logger.log(Level.FINEST, "\n * result: " + res);

    return res;
  }

  @SuppressWarnings("unchecked")
  public static Constraint generalize(Constraint cn1, Constraint cn2) {

    // Constraint 1
    Term constraint1 = Util.termArrayToList(cn1.getConstraint().toArray(new Term[0]));
    // Constraint 2
    Term constraint2 = Util.termArrayToList(cn2.getConstraint().toArray(new Term[0]));

    Term[] args = {constraint1, constraint2, new Variable("G")};

    Query q = new Query("generalize", args);

    logger.log(Level.FINEST, "\n * definition: " + cn1 + "\n * ancestor :  " + cn1);

    return normalize("G", q.oneSolution());
  }

  public static Constraint and(Constraint cn1, Constraint cn2) {

    List<Term> andCn = new ArrayList<>(cn1.getConstraint());
    andCn.addAll(cn2.getConstraint());

    logger.log(Level.FINEST, "\n * " + cn1 + "\n * and \n * " + cn2);

    /*
     * create a list of variable to solve the constraint
     * Remove all non primed variables which occur in
     * the set of primed variables
     */
    Map<String, Term> newVars = ConstraintManager.selectVariables(cn1.getVars(), cn2.getVars());

    Constraint andConstraint = ConstraintManager.simplify(andCn, newVars);

    logger.log(Level.FINEST, "\n * " + andConstraint);

    return andConstraint;
  }

  private static Map<String, Term> selectVariables(
      Map<String, Term> vars, Map<String, Term> pVars) {

    Map<String, Term> newVars = new HashMap<>(pVars);

    for (Map.Entry<String, Term> me : vars.entrySet()) {
      if (!pVars.containsKey(me.getKey())) {
        newVars.put(me.getKey(), me.getValue());
      }
    }

    return newVars;
  }

  private static Constraint normalize(String sol, Map<String, Term> varMap) {

    // fetches the solution
    Term cn = varMap.get(sol);

    String newConstraint = cn.toString();

    Constraint nres = new Constraint();

    if (Constraint.isFalse(newConstraint)) {
      return nres.setFalse();
    }

    Map<String, Term> varSolMap = new HashMap<>(varMap);

    varSolMap.remove(sol);

    /*
     * replace all occurrences of primed variables
     * by their corresponding unprimed version
     */
    for (Map.Entry<String, Term> me : varSolMap.entrySet()) {
      newConstraint =
          newConstraint.replace(
              me.getValue().toString(), ConstraintManager.primedVarToVar(me.getKey()));
      nres.addVar(
          var2CVar(ConstraintManager.primedVarToVar(me.getKey())),
          new Variable(primedVarToVar(me.getKey())));
    }

    // TODO: to be improved
    nres.setConstraint(
        new ArrayList<>(Arrays.asList(Util.listToTermArray(Util.textToTerm(newConstraint)))));

    logger.log(Level.FINEST, "\n * result: " + nres);

    return nres;
  }

  public static List<Constraint> getConstraint(AssumeEdge ae) {
    CBinaryExpression c = (CBinaryExpression) ae.getExpression();
    Collection<Pair<Term, List<Term>>> acList;
    List<Constraint> cns = new ArrayList<>(2);

    if (ae.getTruthAssumption()) {
      // atomic constraint
      acList = expressionToCLP(c);
      // for all term in the list create a constraint
      for (Pair<Term, List<Term>> p : acList) {
        cns.add(new Constraint(p.getFirst(), p.getSecond()));
      }
    } else {
      // negated atomic constraint
      CBinaryExpression negbe = null;
      if (c.getOperator().isLogicalOperator()) {
        negbe =
            new CBinaryExpression(
                c.getFileLocation(),
                c.getExpressionType(),
                c.getCalculationType(),
                c.getOperand1(),
                c.getOperand2(),
                c.getOperator().getOppositLogicalOperator());
      }
      acList = expressionToCLP(negbe);
      for (Pair<Term, List<Term>> p : acList) {
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
    c.addVar(lhs.toString(), ConstraintManager.CVar2PrologPrimedVar(lhs.toString()));
    if (lhs instanceof AIdExpression) {
      for (Pair<Term, List<Term>> t : expressionToCLP(exp)) {
        Term[] operands = {c.getVars().get(lhs.toString()), t.getFirst()};
        List<Term> list = new ArrayList<>();
        list.add(new Compound("=:=", operands));
        c.setConstraint(list);
      }
    } else {
      throw new AssertionError("unhandled assignment " + ca);
    }
    return c;
  }

  public static Constraint getConstraint(CExpression exp) {
    List<Term> tlist = new ArrayList<>();
    List<Term> vlist = new ArrayList<>();
    for (Pair<Term, List<Term>> t : expressionToCLP(exp)) {
      tlist.add(t.getFirst());
      vlist.addAll(t.getSecond());
    }
    return new Constraint(tlist, vlist);
  }

  public static List<Constraint> getConstraint(List<CExpression> exp) {
    List<Constraint> clist = new ArrayList<>();
    for (CExpression c : exp) {
      clist.add(getConstraint(c));
    }
    return clist;
  }

  public static Constraint getConstraint(ADeclarationEdge ae) {
    CDeclaration decl = (CDeclaration) ae.getDeclaration();
    Constraint ac = new Constraint();
    if (decl instanceof CVariableDeclaration) {
      CVariableDeclaration vdecl = (CVariableDeclaration) decl;
      AInitializer initializer = vdecl.getInitializer();
      String varName = vdecl.getName();
      Term lhs = CVar2PrologPrimedVar(varName);
      if (initializer != null) {
        if (initializer instanceof CInitializerExpression) {
          CExpression expression = ((CInitializerExpression) initializer).getExpression();
          Collection<Pair<Term, List<Term>>> at = expressionToCLP(expression);
          for (Pair<Term, List<Term>> t : at) {
            Term rhs = t.getFirst();
            List<Term> acList = new ArrayList<>();
            acList.add(new Compound("=:=", new Term[] {lhs, rhs}));
            ac.setConstraint(acList);
          }
        }
      }
      ac.addVar(varName, lhs);
    }
    return ac;
  }

  public static Constraint getConstraint(AReturnStatementEdge aRetEdge) {

    AExpression expression =
        aRetEdge.getExpression().isPresent()
            ? aRetEdge.getExpression().get()
            : CIntegerLiteralExpression.ZERO; // this is the default in C

    String varName = "FRET_" + aRetEdge.getSuccessor().getFunctionName();

    Term lhs = CVar2PrologVar(varName);

    Constraint ac = new Constraint();

    Collection<Pair<Term, List<Term>>> at = expressionToCLP(expression);
    for (Pair<Term, List<Term>> t : at) {
      Term rhs = t.getFirst();
      List<Term> acList = new ArrayList<>();
      acList.add(new Compound("=:=", new Term[] {lhs, rhs}));
      ac.setConstraint(acList);
    }

    ac.addVar(varName, lhs);

    return ac;
  }

  public static Constraint getConstraint(FunctionReturnEdge fretEdge)
      throws UnrecognizedCodeException {

    FunctionSummaryEdge summaryEdge = fretEdge.getSummaryEdge();
    AFunctionCall exprOnSummary = summaryEdge.getExpression();

    // expression is an assignment operation, e.g. a = g(b);
    if (exprOnSummary instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement assignExp =
          ((AFunctionCallAssignmentStatement) exprOnSummary);
      AExpression op1 = assignExp.getLeftHandSide();

      // we expect left hand side of the expression to be a variable
      if ((op1 instanceof AIdExpression) || (op1 instanceof CFieldReference)) {

        String varName = "FRET_" + fretEdge.getPredecessor().getFunctionName();
        Term lhs = CVar2PrologPrimedVar(op1.toString());
        Term rhs = CVar2PrologVar(varName);
        Constraint ac =
            new Constraint(Lists.newArrayList(new Compound("=:=", new Term[] {lhs, rhs})));

        ac.addVar(op1.toString(), lhs);

        return ac;
      }
      // TODO: a[x] = b();
      else if (op1 instanceof CArraySubscriptExpression) {
        return new Constraint();
      } else {
        throw new UnrecognizedCodeException("on function return", summaryEdge, null);
      }
    }
    return new Constraint();
  }

  public static Constraint getConstraint(CExpression lhs, CFunctionCallExpression rhs) {
    Constraint c = new Constraint();
    if (rhs != null) {
      c.addVar(lhs.toString(), ConstraintManager.CVar2PrologPrimedVar(lhs.toString()));
      if (lhs instanceof AIdExpression) {
        Term[] operands = {
          c.getVars().get(lhs.toString()),
          CVar2PrologVar(rhs.getFunctionNameExpression().toString())
        };
        List<Term> list = new ArrayList<>();
        list.add(new Compound("=:=", operands));
        c.setConstraint(list);
      }
    }
    return c;
  }

  public static Collection<Constraint> getConstraint(
      List<String> names, List<? extends AExpression> expressions) {

    List<Constraint> cnList = new ArrayList<>();

    for (int i = 0; i < names.size(); i++) {

      String name = names.get(i);
      AExpression expression = expressions.get(i);

      for (Pair<Term, List<Term>> p : paramExpressionToCLP(name, expression)) {
        cnList.add(
            new Constraint(
                new ArrayList<>(Arrays.asList(Util.listToTermArray(p.getFirst()))), p.getSecond()));
      }
    }

    return cnList;
  }

  /**
   * input: ppv is of the form "_$CVAR", where $CVAR stands for a C program variable output: "$CVAR"
   */
  public static String var2CVar(String pv) {
    return pv.substring(4);
  }

  /**
   * input: ppv is of the form "_p_$CVAR", where $CVAR stands for a C program variable output:
   * "$CVAR"
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
      return pvar.replace("Primed_", "");
    }
    return pvar;
  }

  private static Collection<Pair<Term, List<Term>>> getNegatedConstraintList(
      Pair<Term, List<Term>> cn) {

    Compound atomCnT = (Compound) cn.getFirst();
    Compound negAtomCnT = null;
    switch (atomCnT.name()) {
      case "<":
        negAtomCnT = new Compound(">=", 2);
        negAtomCnT.setArg(1, atomCnT.arg(1));
        negAtomCnT.setArg(2, atomCnT.arg(2));
        return ImmutableSet.of(Pair.of(negAtomCnT, cn.getSecond()));
      case "=<":
        negAtomCnT = new Compound(">", 2);
        negAtomCnT.setArg(1, atomCnT.arg(1));
        negAtomCnT.setArg(2, atomCnT.arg(2));
        return ImmutableSet.of(Pair.of(negAtomCnT, cn.getSecond()));
      case ">":
        negAtomCnT = new Compound("=<", 2);
        negAtomCnT.setArg(1, atomCnT.arg(1));
        negAtomCnT.setArg(2, atomCnT.arg(2));
        return ImmutableSet.of(Pair.of(negAtomCnT, cn.getSecond()));
      case ">=":
        negAtomCnT = new Compound("<", 2);
        negAtomCnT.setArg(1, atomCnT.arg(1));
        negAtomCnT.setArg(2, atomCnT.arg(2));
        return ImmutableSet.of(Pair.of(negAtomCnT, cn.getSecond()));
      case "=:=":
        return Arrays.asList(
            Pair.of(new Compound("<", new Term[] {atomCnT.arg(1), atomCnT.arg(2)}), cn.getSecond()),
            Pair.of(
                new Compound(">", new Term[] {atomCnT.arg(1), atomCnT.arg(2)}), cn.getSecond()));
      default:
        return null;
    }
  }

  private static Collection<Pair<Term, List<Term>>> expressionToCLP(AExpression ce) {

    List<Term> vars = new ArrayList<>();

    if (ce instanceof CIdExpression) {
      vars.add(CVar2PrologVar(ce.toString()));
      return ImmutableSet.of(Pair.of(CVar2PrologVar(ce.toString()), vars));
    } else if (ce instanceof CIntegerLiteralExpression) {
      return ImmutableSet.of(Pair.of(Util.textToTerm("rdiv(" + ce + ",1)"), vars));
    } else if (ce instanceof CBinaryExpression) {
      CBinaryExpression bexp = (CBinaryExpression) ce;
      Collection<Pair<Term, List<Term>>> operand1 = expressionToCLP(bexp.getOperand1());
      Collection<Pair<Term, List<Term>>> operand2 = expressionToCLP(bexp.getOperand2());
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
          Collection<Pair<Term, List<Term>>> opUnion =
              new ArrayList<>(addOperands(">", operand1, operand2));
          opUnion.addAll(addOperands("<", operand1, operand2));
          return ImmutableList.copyOf(opUnion);
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

  private static Collection<Pair<Term, List<Term>>> paramExpressionToCLP(
      String paramName, AExpression ce) {

    List<Term> vars = new ArrayList<>();

    Term paramVariable = CVar2PrologVar(paramName);
    vars.add(paramVariable);
    Term expTerm = null;

    if (ce instanceof CIdExpression) {
      vars.add(CVar2PrologVar(ce.toString()));
      expTerm = CVar2PrologVar(ce.toString());
      Term paramAexpTerm = new Compound("=:=", new Term[] {paramVariable, expTerm});
      return ImmutableSet.of(Pair.of(Util.termArrayToList(new Term[] {paramAexpTerm}), vars));
    } else if (ce instanceof CIntegerLiteralExpression) {
      expTerm = Util.textToTerm("rdiv(" + ce + ",1)");
      Term paramAexpTerm = new Compound("=:=", new Term[] {paramVariable, expTerm});
      return ImmutableSet.of(Pair.of(Util.termArrayToList(new Term[] {paramAexpTerm}), vars));
    } else if (ce instanceof CBinaryExpression) {
      CBinaryExpression bexp = (CBinaryExpression) ce;
      Collection<Pair<Term, List<Term>>> aexpTerms = expressionToCLP(ce);
      ImmutableCollection.Builder<Pair<Term, List<Term>>> paramAexpTerms =
          ImmutableList.builderWithExpectedSize(aexpTerms.size());
      switch (bexp.getOperator()) {
        case PLUS:
        case MINUS:
        case MULTIPLY:
        case DIVIDE:
          for (Pair<Term, List<Term>> aexpTerm : aexpTerms) {
            List<Term> aexpTermVars = new ArrayList<>(aexpTerm.getSecond());
            aexpTermVars.add(paramVariable);
            Term paramAexpTerm =
                new Compound("=:=", new Term[] {paramVariable, aexpTerm.getFirst()});
            paramAexpTerms.add(
                Pair.of(Util.termArrayToList(new Term[] {paramAexpTerm}), aexpTermVars));
          }
          return paramAexpTerms.build();
          // add an extra atomic constraint
        case EQUALS:
        case LESS_THAN:
        case LESS_EQUAL:
        case GREATER_THAN:
        case GREATER_EQUAL:
        case NOT_EQUALS:
          for (Pair<Term, List<Term>> aexpTerm : aexpTerms) {
            List<Term> aexpTermVars = new ArrayList<>(aexpTerm.getSecond());
            aexpTermVars.add(paramVariable);
            Term paramAexpTerm =
                new Compound("=:=", new Term[] {paramVariable, Util.textToTerm("rdiv(1,1)")});
            paramAexpTerms.add(
                Pair.of(
                    Util.termArrayToList(new Term[] {paramAexpTerm, aexpTerm.getFirst()}),
                    aexpTermVars));
            paramAexpTerm =
                new Compound("=:=", new Term[] {paramVariable, Util.textToTerm("rdiv(0,1)")});
            for (Pair<Term, List<Term>> negAexpTerm : getNegatedConstraintList(aexpTerm)) {
              paramAexpTerms.add(
                  Pair.of(
                      Util.termArrayToList(new Term[] {paramAexpTerm, negAexpTerm.getFirst()}),
                      aexpTermVars));
            }
          }
          return paramAexpTerms.build();
        default:
          return null;
      }
    } else {
      return null;
    }
  }

  private static Collection<Pair<Term, List<Term>>> addOperands(
      String operator,
      Collection<Pair<Term, List<Term>>> operand1,
      Collection<Pair<Term, List<Term>>> operand2) {

    ImmutableCollection.Builder<Pair<Term, List<Term>>> termList = ImmutableList.builder();
    List<Term> vars = new ArrayList<>();

    for (Pair<Term, List<Term>> subop1 : operand1) {
      for (Pair<Term, List<Term>> subop2 : operand2) {
        vars.addAll(subop1.getSecond());
        vars.addAll(subop2.getSecond());
        termList.add(
            Pair.of(
                new Compound(operator, new Term[] {subop1.getFirst(), subop2.getFirst()}), vars));
      }
    }

    return termList.build();
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

  /**
   * Compute over-approximation of convex hull of two constraints. TODO: Currently the
   * over-approximation is always very imprecise (it is just the top element)
   *
   * @param cn1 the first constraint
   * @param cn2s the second constraint
   */
  public static Constraint convexHull(Constraint cn1, Constraint cn2s) {
    return new Constraint();
  }

  private ConstraintManager() {}
}
