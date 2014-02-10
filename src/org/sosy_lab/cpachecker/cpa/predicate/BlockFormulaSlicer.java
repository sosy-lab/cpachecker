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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Predicates.in;
import static com.google.common.collect.FluentIterable.from;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.IARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;


public class BlockFormulaSlicer {

  /** if important or not, this does not matter, because it will be ignored later,
   * so it can be used for optimization. */
  private static final boolean IS_BLANK_EDGE_IMPORTANT = false;

  private static final String FUNCTION_RETURN_VARIABLE = "__CPAchecker_return_var";

  final private PathFormulaManager pfmgr;
  final private boolean sliceBlockFormulas;

  //  TODO future work:
  //  We could not store the important edges, because they are much more.
  //  It is more efficient, to store the complement.

  /** This set contains all edges, that are important.
   * We store the parent- and the child-ARGState, because they are unique,
   * the edge itself can be used several times (for example in a loop). */
  final private Multimap<ARGState, ARGState> importantEdges = ArrayListMultimap.create();

  @SuppressWarnings("unused")
  private static final Function<PredicateAbstractState, BooleanFormula> GET_BLOCK_FORMULA =
      new Function<PredicateAbstractState, BooleanFormula>() {

        @Override
        public BooleanFormula apply(PredicateAbstractState e) {
          assert e.isAbstractionState();
          return e.getAbstractionFormula().getBlockFormula().getFormula();
        }
      };

  private static final Function<PathFormula, BooleanFormula> GET_BOOLEAN_FORMULA =
      new Function<PathFormula, BooleanFormula>() {

        @Override
        public BooleanFormula apply(PathFormula pf) {
          return pf.getFormula();
        }
      };

  public BlockFormulaSlicer(PathFormulaManager pPfmgr) {
    this.pfmgr = pPfmgr;
    this.sliceBlockFormulas = true;
  }

  public List<BooleanFormula> sliceFormulasForPath(List<ARGState> path, ARGState initialState)
      throws CPATransferException {

    // first find all ARGStates for each block,
    // a block is a set of states with one start- and one end-state,
    // each path, that ends at the end-state, starts in the start-state,
    // but not the other way. there can be several pathes from start to end.

    final List<Set<ARGState>> blocks = new ArrayList<>(path.size());
    for (int i = 0; i < path.size(); i++) {
      final ARGState start = i > 0 ? path.get(i - 1) : initialState;
      final ARGState end = path.get(i);

      blocks.add(getARGStatesOfBlock(start, end));
    }

    assert path.size() == blocks.size();

    // slice each block, we do this backwards
    Collection<String> importantVars = new LinkedHashSet<>();
    for (int i = path.size() - 1; i >= 0; i--) {
      final ARGState start = i > 0 ? path.get(i - 1) : initialState;
      final ARGState end = path.get(i);
      final Set<ARGState> block = blocks.get(i);

      importantVars = sliceBlock(start, end, block, importantVars);
    }

    // build new pathformulas, forwards
    PathFormula pf = pfmgr.makeEmptyPathFormula();
    final List<PathFormula> pfs = new ArrayList<>(path.size());
    for (int i = 0; i < path.size(); i++) {
      final ARGState start = i > 0 ? path.get(i - 1) : initialState;
      final ARGState end = path.get(i);

      // we do not need the block later, so we can remove it.
      // the list gets shorter each iteration, so this is equal to "blocks.get(i)"
      final Set<ARGState> block = blocks.remove(0);

      final PathFormula oldPf = pfmgr.makeEmptyPathFormula(pf);
      pf = buildFormula(start, end, block, oldPf);
      pfs.add(pf);
    }

    final ImmutableList<BooleanFormula> list = from(pfs)
        .transform(GET_BOOLEAN_FORMULA)
        .toList();

    //    System.out.println("\n\nFORMULA::");
    //    for (BooleanFormula formula : list) {
    //      System.out.println(formula);
    //      //System.out.println(printFormula(formula.toString()));
    //    }
    //
    //    ImmutableList<BooleanFormula> origlist = from(path)
    //        .transform(toState(PredicateAbstractState.class))
    //        .transform(GET_BLOCK_FORMULA)
    //        .toList();
    //
    //    System.out.println("\n\nORIG FORMULA::");
    //    for (BooleanFormula formula : origlist) {
    //      System.out.println(formula);
    //      //System.out.println(printFormula(formula.toString()));
    //    }

    return list;
  }

  /** This function returns all states, that are contained in a block.
   * The block is the union of all paths, that end in the end-state.
   * We assume, that all paths begin in the start-state (that may be null).
   * The returned collection includes the end-state and the start-state. */
  private Set<ARGState> getARGStatesOfBlock(ARGState start, ARGState end) {
    final Set<ARGState> states = new HashSet<>();
    states.add(start); // start is the last state to be reachable backwards
    states.add(end); // end is the first state to be reachable backwards

    // backwards-bfs for parents, visit each state once
    final List<ARGState> waitlist = new LinkedList<>();
    waitlist.add(end);
    while (!waitlist.isEmpty()) {
      final ARGState current = waitlist.remove(0);
      for (final ARGState parent : current.getParents()) {
        if (states.add(parent)) { // state was not seen before
          waitlist.add(parent);
        }
      }
    }

    return states;
  }


  private Collection<String> sliceBlock(ARGState start, ARGState end,
      Set<ARGState> block, Collection<String> importantVars) {

    // this map contains all done states with their vars (if not removed through cleanup)
    final Map<ARGState, Collection<String>> s2v = new HashMap<>(block.size());

    // this map contains all done states with their last important state
    // a state is important, if any outgoing edge is important
    final Multimap<ARGState, ARGState> s2s = HashMultimap.create(block.size(), 1);

    // bfs for parents, visit each state once.
    // we use a list for the next states,
    // but we also remove states from waitlist, when they are done,
    // so we need fast access to the states
    final Set<ARGState> waitlist = new LinkedHashSet<>();

    // special handling of first state
    s2v.put(end, importantVars);
    s2s.put(end, end);
    for (final ARGState parent : end.getParents()) {
      if (block.contains(parent)) {
        waitlist.add(parent);
      }
    }

    while (!waitlist.isEmpty()) {
      final ARGState current = Iterables.getFirst(waitlist, null);
      waitlist.remove(current);

      // already handled
      assert !s2v.keySet().contains(current);

      // we have to wait for all children completed,
      // because we want to join the branches
      if (!isAllChildrenDone(current, s2v.keySet(), block)) {
        waitlist.add(current); // re-add current state, at last position
        continue;
      }

      // collect new states, ignore unreachable states
      for (final ARGState parent : current.getParents()) {
        if (block.contains(parent)) {
          waitlist.add(parent);
        }
      }

      // handle state
      final Collection<String> vars = handleEdgesForState(current, s2v, s2s, block);
      s2v.put(current, vars);

      // cleanup, remove states, that will not be used in future
      for (final ARGState child : current.getChildren()) {
        if (block.contains(child) && isAllParentsDone(child, s2v.keySet(), block)) {
          s2v.remove(child);
          s2s.removeAll(child);
        }
      }
    }

    // logging
    //    System.out.println("START::  " + (start == null ? null : start.getStateId()));
    //    System.out.println("END::    " + end.getStateId());
    //    System.out.print("BLOCK::  ");
    //    for (ARGState current : block) {
    //      System.out.print(current.getStateId() + ", ");
    //    }
    //    System.out.println();
    //    System.out.print("VISITED::  ");
    //    for (ARGState current : s2v.keySet()) {
    //      System.out.print(current.getStateId() + ", ");
    //    }
    //    System.out.println("\n\n");

    return s2v.get(start);
  }

  /** This function handles all outgoing edges of the current state.
   * Their important vars are joined and returned. */
  private Collection<String> handleEdgesForState(ARGState current,
      Map<ARGState, Collection<String>> s2v,
      Multimap<ARGState, ARGState> s2s,
      Set<ARGState> block) {

    final List<ARGState> usedChildren = from(current.getChildren()).filter(in(block)).toList();
    assert usedChildren.size() > 0 : "no child for " + current.getStateId();

    // if we have an assumption, and the branches are completely unimportant,
    // the assumption itself is unimportant, so we can ignore it
    if (isAssumptionWithSameImpChild(usedChildren, current, s2s)) {

      final ARGState child1 = usedChildren.get(0);
      s2s.putAll(current, s2s.get(child1));

      final Collection<String> iVars = new LinkedHashSet<>();
      // vars from latest important child,
      // we have to copy them, there could be another parent somewhere else
      iVars.addAll(s2v.get(child1));
      return iVars;

    } else {
      // there can be several children --> collect their vars and join them
      // normally there is 1 or 2 children.
      Collection<String> iVars = null;

      for (ARGState child : usedChildren) {

        final Collection<String> oldVars = s2v.get(child);
        final Collection<String> newVars;

        // if there is only one parent for the child, we re-use oldVars.
        // TODO better solution: if (allParentsExceptThisDone(child)) {
        if (child.getParents().size() == 1) {
          newVars = oldVars;
        } else {
          // copy oldVars, they will be used later for a second parent
          newVars = new LinkedHashSet<>();
          newVars.addAll(oldVars);
        }

        // do the hard work
        final CFAEdge edge = current.getEdgeToChild(child);
        final boolean isImportant = handleEdge(edge, newVars);

        assert !importantEdges.containsEntry(current, child);

        if (isImportant) {
          importantEdges.put(current, child);
          s2s.put(current, current);
        } else {
          s2s.putAll(current, s2s.get(child));
        }

        if (iVars == null) {
          iVars = newVars;
        } else {
          iVars.addAll(newVars);
        }
      }

      Preconditions.checkNotNull(iVars);
      return iVars;
    }
  }

  /** checks, if an assumption has no important edge until the branches join. */
  private boolean isAssumptionWithSameImpChild(final List<ARGState> usedChildren,
      final ARGState current, final Multimap<ARGState, ARGState> s2s) {
    if (usedChildren.size() == 2) {
      final ARGState child1 = usedChildren.get(0);
      final ARGState child2 = usedChildren.get(1);
      final CFAEdge edge1 = current.getEdgeToChild(child1);
      final CFAEdge edge2 = current.getEdgeToChild(child2);

      if (edge1.getEdgeType() == CFAEdgeType.AssumeEdge
          && edge2.getEdgeType() == CFAEdgeType.AssumeEdge) {
        final CAssumeEdge assume1 = (CAssumeEdge) edge1;
        final CAssumeEdge assume2 = (CAssumeEdge) edge2;

        return (assume1.getExpression() == assume2.getExpression()
            && assume1.getTruthAssumption() != assume2.getTruthAssumption()
            && s2s.get(child1).equals(s2s.get(child2)));
      }
    }
    return false;
  }

  /** This function only forwards to the correct type of edge. */
  private boolean handleEdge(CFAEdge edge, Collection<String> importantVars) {

    final boolean result;
    // check the type of the edge
    switch (edge.getEdgeType()) {

    // int a;
    case DeclarationEdge:
      result = handleDeclaration((CDeclarationEdge) edge, importantVars);
      break;

    // if (a == b) {...}
    case AssumeEdge:
      result = handleAssumption((CAssumeEdge) edge, importantVars);
      break;

    // a = b + c;
    case StatementEdge:
      result = handleStatement((CStatementEdge) edge, importantVars);
      break;

    // return (x);
    case ReturnStatementEdge:
      result = handleReturnStatement((CReturnStatementEdge) edge, importantVars);
      break;

    // assignment from y = f(x);
    case FunctionReturnEdge:
      result = handleFunctionReturn((CFunctionReturnEdge) edge, importantVars);
      break;

    // call from y = f(x);
    case FunctionCallEdge:
      result = handleFunctionCall((CFunctionCallEdge) edge, importantVars);
      break;

    case BlankEdge:
      result = IS_BLANK_EDGE_IMPORTANT;
      break;

    case MultiEdge:
      // TODO is support possible?
      throw new AssertionError("multiEdge not supported: " + edge.getRawStatement());

    default:
      throw new AssertionError("unhandled edge: " + edge.getRawStatement());
    }

    return result;
  }

  private boolean handleDeclaration(CDeclarationEdge edge,
      Collection<String> importantVars) {
    final CDeclaration decl = edge.getDeclaration();

    if (decl instanceof CVariableDeclaration) {
      final CVariableDeclaration vdecl = (CVariableDeclaration) decl;
      final String functionName = edge.getPredecessor().getFunctionName();

      if (importantVars.remove(buildVarName(vdecl.isGlobal() ? null : functionName, vdecl.getName()))) {
        final CInitializer initializer = vdecl.getInitializer();
        if (initializer != null && initializer instanceof CInitializerExpression) {
          final CExpression init = ((CInitializerExpression) initializer).getExpression();
          addAllVarsFromExpr(init, functionName, importantVars);
        }
        return true;

      } else {
        return false;
      }

    } else {
      return true;
    }
  }

  private boolean handleAssumption(CAssumeEdge edge,
      Collection<String> importantVars) {

    final CExpression expression = edge.getExpression();
    final String functionName = edge.getPredecessor().getFunctionName();
    addAllVarsFromExpr(expression, functionName, importantVars);

    return true;
  }

  /** This function handles statements like "a = 0;" and calls of external functions.
   * @param pImportantVars */
  private boolean handleStatement(CStatementEdge edge,
      Collection<String> importantVars) {
    final IAStatement statement = edge.getStatement();

    // expression is an assignment operation, e.g. a = b;
    if (statement instanceof CAssignment) {
      return handleAssignment((CAssignment) statement, edge, importantVars);
    }

    // call of external function, "scanf(...)" without assignment
    // internal functioncalls are handled as FunctionCallEdges
    else if (statement instanceof CFunctionCallStatement) {
      return handleExternalFunctionCall(edge,
          ((CFunctionCallStatement) statement).
              getFunctionCallExpression().getParameterExpressions());

      // "exp;" -> nothing to do?
    } else if (statement instanceof CExpressionStatement) {
      return false;

    } else {
      throw new AssertionError("unhandled statement: " + edge.getRawStatement());
    }
  }


  private boolean handleAssignment(CAssignment statement, CStatementEdge edge,
      Collection<String> importantVars) {
    final CExpression lhs = statement.getLeftHandSide();

    // a = ?
    if (lhs instanceof CIdExpression) {
      final String functionName = edge.getPredecessor().getFunctionName();
      final String scopedFunctionName = isGlobal(lhs) ? null : functionName;
      final String varName = ((CIdExpression) lhs).getName();

      if (importantVars.remove(buildVarName(scopedFunctionName, varName))) {
        final IARightHandSide rhs = statement.getRightHandSide();

        // a = b + c
        if (rhs instanceof CExpression) {
          addAllVarsFromExpr((CExpression) rhs, functionName, importantVars);
          return true;

          // a = f(x)
        } else if (rhs instanceof CFunctionCallExpression) {
          // TODO handled somewhere else?
          return true;
        } else {
          throw new AssertionError("unknown class");
        }

      } else {
        return false;
      }

    } else {
      // pointer assignment or something else --> important
      return true;
    }
  }

  private boolean handleExternalFunctionCall(CStatementEdge pEdge,
      List<CExpression> parameterExpressions) {
    return true;
  }

  /** This function handles functionStatements like "return (x)".
   * The FUNCTION_RETURN_VARIABLE is equal to the right side ("x"). */
  private boolean handleReturnStatement(CReturnStatementEdge edge,
      Collection<String> importantVars) {
    CExpression rhs = edge.getExpression();

    if (rhs == null) {
      return false;

    } else {

      String functionName = edge.getPredecessor().getFunctionName();
      if (importantVars.remove(buildVarName(functionName, FUNCTION_RETURN_VARIABLE))) {
        addAllVarsFromExpr(rhs, functionName, importantVars);
        return true;
      } else {
        return false;
      }
    }
  }


  /** This function handles functionReturns like "y=f(x)".
   * The equality of the FUNCTION_RETURN_VARIABLE and the
   * left side ("y") is build. */
  private boolean handleFunctionReturn(CFunctionReturnEdge edge,
      Collection<String> importantVars) {

    // set result of function equal to variable on left side
    CFunctionSummaryEdge fnkCall = edge.getSummaryEdge();
    CFunctionCall call = fnkCall.getExpression();

    // handle assignments like "y = f(x);"
    if (call instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement cAssignment = (CFunctionCallAssignmentStatement) call;
      CExpression lhs = cAssignment.getLeftHandSide();

      final String innerFunctionName = edge.getPredecessor().getFunctionName();
      final String outerFunctionName = isGlobal(lhs) ? null : edge.getSuccessor().getFunctionName();

      if (importantVars.remove(buildVarName(outerFunctionName, lhs.toASTString()))) {
        importantVars.add(buildVarName(innerFunctionName, FUNCTION_RETURN_VARIABLE));
        return true;

      } else {
        return false;
      }

      // f(x); --> function could change global vars, the 'return' is unimportant
    } else if (call instanceof CFunctionCallStatement) {
      // do nothing
      return false;

    } else {
      throw new AssertionError("unhandled functionreturn " + call);
    }
  }

  /** This function handles functioncalls like "f(x)", that calls "f(int a)".
   * Therefore each arg ("x") assigned to a param ("int a") of the function. */
  private boolean handleFunctionCall(CFunctionCallEdge edge,
      Collection<String> importantVars) {

    // overtake arguments from last functioncall into function,
    // get args from functioncall and make them equal with params from functionstart
    final List<CExpression> args = edge.getArguments();
    final List<CParameterDeclaration> params = edge.getSuccessor().getFunctionParameters();

    // var_args cannot be handled: func(int x, ...) --> we only handle the first n parameters
    assert args.size() >= params.size();

    final String innerFunctionName = edge.getSuccessor().getFunctionName();
    final String outerFunctionName = edge.getPredecessor().getFunctionName();

    for (int i = 0; i < params.size(); i++) {
      if (importantVars.remove(buildVarName(innerFunctionName, params.get(i).getName()))) {
        addAllVarsFromExpr(args.get(i), outerFunctionName, importantVars);
      }
    }

    // TODO how can we (not) handle untracked params in CtoFormulaCOnverter??
    return true;
  }

  private String buildVarName(String function, String var) {
    if (function == null) {
      return var;
    } else {
      return function + "::" + var;
    }
  }

  private void addAllVarsFromExpr(
      CExpression exp, String functionName, Collection<String> importantVars) {
    exp.accept(new VarCollector(functionName, importantVars));
  }

  /** This Visitor collects all var-names in the expression. */
  private class VarCollector implements CExpressionVisitor<Void, RuntimeException> {

    final Collection<String> vars;
    final private String functionName;

    VarCollector(String functionName, Collection<String> vars) {
      this.functionName = functionName;
      this.vars = vars;
    }

    @Override
    public Void visit(CArraySubscriptExpression exp) {
      return null;
    }

    @Override
    public Void visit(CBinaryExpression exp) {
      exp.getOperand1().accept(this);
      exp.getOperand2().accept(this);
      return null;
    }

    @Override
    public Void visit(CCastExpression exp) {
      exp.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CComplexCastExpression exp) {
      exp.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CFieldReference exp) {
      return null;
    }

    @Override
    public Void visit(CIdExpression exp) {
      String var = exp.getName();
      String function = isGlobal(exp) ? null : functionName;
      vars.add(buildVarName(function, var));
      return null;
    }

    @Override
    public Void visit(CCharLiteralExpression exp) {
      return null;
    }

    @Override
    public Void visit(CFloatLiteralExpression exp) {
      return null;
    }

    @Override
    public Void visit(CIntegerLiteralExpression exp) {
      return null;
    }

    @Override
    public Void visit(CStringLiteralExpression exp) {
      return null;
    }

    @Override
    public Void visit(CImaginaryLiteralExpression exp) {
      return null;
    }

    @Override
    public Void visit(CTypeIdExpression exp) {
      return null;
    }

    @Override
    public Void visit(CTypeIdInitializerExpression exp) {
      return null;
    }

    @Override
    public Void visit(CUnaryExpression exp) {
      exp.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CPointerExpression exp) {
      exp.getOperand().accept(this);
      return null;
    }
  }


  /** This function returns a PathFormula for the whole block from start to end.
   * The SSA-indices of the new formula are based on the old formula. */
  private PathFormula buildFormula(ARGState start, ARGState end,
      Collection<ARGState> block, PathFormula oldPf) throws CPATransferException {

    // this map contains all done states with their formulas
    final Map<ARGState, PathFormula> s2f = new HashMap<>(block.size());

    // bfs for parents, visit each state once
    // we use a list for the next states,
    // but we also remove states from waitlist, when they are done,
    // so we need fast access to the states
    final Set<ARGState> waitlist = new LinkedHashSet<>();

    // special handling of first state
    s2f.put(start, oldPf);
    for (ARGState child : start.getChildren()) {
      if (block.contains(child)) {
        waitlist.add(child);
      }
    }

    while (!waitlist.isEmpty()) {
      final ARGState current = Iterables.getFirst(waitlist, null);
      waitlist.remove(current);

      // already handled
      assert !s2f.keySet().contains(current);

      // we have to wait for all parents completed,
      // because we want to join the branches
      if (!isAllParentsDone(current, s2f.keySet(), block)) {
        waitlist.add(current); // re-add current state, at last position
        continue;
      }

      // collect new states, ignore unreachable states
      for (final ARGState child : current.getChildren()) {
        if (block.contains(child)) {
          waitlist.add(child);
        }
      }

      // handle state
      final PathFormula pf = makeFormulaForState(current, s2f);
      s2f.put(current, pf);

      // cleanup, remove states, that will not be used in future
      for (final ARGState parent : current.getParents()) {
        if (block.contains(parent) && isAllChildrenDone(parent, s2f.keySet(), block)) {
          s2f.remove(parent);
        }
      }
    }
    return s2f.get(end);
  }


  private PathFormula makeFormulaForState(ARGState current, Map<ARGState, PathFormula> s2f)
      throws CPATransferException {

    assert current.getParents().size() > 0 : "no parent for " + current.getStateId();

    // join all formulas from parents
    final List<PathFormula> pfs = new ArrayList<>(current.getParents().size());
    for (ARGState parent : current.getParents()) {
      final PathFormula oldPf = s2f.get(parent);
      pfs.add(buildFormulaForEdge(parent, current, oldPf));
    }

    PathFormula joined = pfs.get(0);
    for (int i = 1; i < pfs.size(); i++) {
      joined = pfmgr.makeOr(joined, pfs.get(i));
    }

    return joined;
  }

  private PathFormula buildFormulaForEdge(ARGState parent, ARGState child, PathFormula oldFormula)
      throws CPATransferException {
    if (sliceBlockFormulas && !importantEdges.containsEntry(parent, child)) {
      return oldFormula;
    } else {
      return pfmgr.makeAnd(oldFormula, parent.getEdgeToChild(child));
    }
  }

  private static boolean isGlobal(CExpression exp) {
    if (exp instanceof CIdExpression) {
      CSimpleDeclaration decl = ((CIdExpression) exp).getDeclaration();
      if (decl instanceof CDeclaration) { return ((CDeclaration) decl).isGlobal(); }
    }
    return false;
  }

  /** This function returns, if all parents of a state,
   * that are in the subset, are done. */
  private boolean isAllChildrenDone(ARGState s,
      Collection<ARGState> done, Collection<ARGState> subset) {
    for (ARGState child : s.getChildren()) {
      if (subset.contains(child) && !done.contains(child)) { return false; }
    }
    return true;
  }

  /** This function returns, if all parents of a state,
   * that are in the subset, are done. */
  private boolean isAllParentsDone(ARGState s,
      Collection<ARGState> done, Collection<ARGState> subset) {
    for (ARGState parent : s.getParents()) {
      if (subset.contains(parent) && !done.contains(parent)) { return false; }
    }
    return true;
  }

  @SuppressWarnings("unused")
  private static String printFormula(String formula) {
    StringBuilder str = new StringBuilder();

    final String IND = "    ";
    int indent = 0;
    int pos = 0;
    boolean closed = false;

    while (pos != -1) {
      int open = formula.indexOf('(', pos + 1);
      int close = formula.indexOf(')', pos + 1);

      //  System.out.println(open + "   " + close + "    " + pos + "\n" + str.toString());

      if (open != -1 && open < close) { // new child

        str.append('\n');
        for (int i = 0; i < indent; i++) {
          str.append(IND);
        }
        indent++;

        if (closed) {
          str.append(IND);
        }
        closed = false;

        str.append(formula.substring(pos, open));
        pos = open;

      } else if (close != -1) { // close child

        str.append('\n');
        for (int i = 0; i < indent; i++) {
          str.append(IND);
        }
        indent--;

        if (closed) {
          str.append(IND);
        }
        closed = true;

        str.append(formula.substring(pos, close));
        pos = close;

      } else {
        break;
      }
    }

    return str.toString();
  }
}
