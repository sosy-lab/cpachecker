/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bdd;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.Defaults;
import org.sosy_lab.cpachecker.cfa.ast.ExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.MultiEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

/** This Transfer Relation tracks variables and handles them as boolean,
 * so only the case ==0 and the case !=0 are tracked. */
@Options(prefix = "cpa.bdd")
public class BDDTransferRelation implements TransferRelation {

  private final NamedRegionManager rmgr;

  /** name for return-variables, it is used for function-returns. */
  private static final String FUNCTION_RETURN_VARIABLE = "__cpachecker_return_var";

  @Option(description = "initialize all variables to 0 when they are declared")
  private boolean initAllVars = false;

  /** for statistics */
  protected int createdPredicates;
  protected int deletedPredicates;

  public BDDTransferRelation(NamedRegionManager manager, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this);
    this.rmgr = manager;
  }

  @Override
  public Collection<BDDElement> getAbstractSuccessors(
      AbstractState element, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException {
    BDDElement elem = (BDDElement) element;

    if (elem.getRegion().isFalse()) { return Collections.emptyList(); }

    BDDElement successor = null;

    switch (cfaEdge.getEdgeType()) {

    case AssumeEdge: {
      successor = handleAssumption(elem, (AssumeEdge) cfaEdge);
      break;
    }

    case StatementEdge: {
      successor = handleStatementEdge(elem, (StatementEdge) cfaEdge);
      break;
    }

    case DeclarationEdge:
      successor = handleDeclarationEdge(elem, (DeclarationEdge) cfaEdge);
      break;

    case MultiEdge: {
      successor = elem;
      Collection<BDDElement> c = null;
      for (CFAEdge innerEdge : (MultiEdge) cfaEdge) {
        c = getAbstractSuccessors(successor, precision, innerEdge);
        if (c.isEmpty()) {
          successor = elem; //TODO really correct??
        } else if (c.size() == 1) {
          successor = c.toArray(new BDDElement[1])[0];
        } else {
          throw new AssertionError("only size 0 or 1 allowed");
        }
      }
    }

    case FunctionCallEdge:
      successor = handleFunctionCallEdge(elem, (FunctionCallEdge) cfaEdge);
      break;

    case FunctionReturnEdge:
      successor = handleFunctionReturnEdge(elem, (FunctionReturnEdge) cfaEdge);
      break;

    case ReturnStatementEdge:
      successor = handleReturnStatementEdge(elem, (ReturnStatementEdge) cfaEdge);
      break;

    case BlankEdge:
    case CallToReturnEdge:
    default:
      successor = elem;
    }

    if (successor == null) {
      return Collections.emptySet();
    } else {
      assert !successor.getRegion().isFalse();
      return Collections.singleton(successor);
    }
  }

  /** handles statements like "a = 0;" and "b = !a;" */
  private BDDElement handleStatementEdge(BDDElement element, StatementEdge cfaEdge)
      throws UnrecognizedCCodeException {
    IASTStatement statement = cfaEdge.getStatement();
    if (!(statement instanceof IASTAssignment)) { return element; }
    IASTAssignment assignment = (IASTAssignment) statement;

    IASTExpression lhs = assignment.getLeftHandSide();
    BDDElement result = element;
    if (lhs instanceof IASTIdExpression || lhs instanceof IASTFieldReference
        || lhs instanceof IASTArraySubscriptExpression) {

      // make variable (predicate) for LEFT SIDE of assignment,
      // delete variable, if it was used before, this is done with an existential operator
      String varName = lhs.toASTString();
      Region var = makePredicate(varName, element.getFunctionName(), isGlobal(lhs));
      Region newRegion = removePredicate(element.getRegion(), var);

      IASTRightHandSide rhs = assignment.getRightHandSide();
      if (rhs instanceof IASTExpression) {

        // make region for RIGHT SIDE and build equality of var and region
        BDDExpressionVisitor ev = new BDDExpressionVisitor(element);
        Region regRHS = ((IASTExpression) rhs).accept(ev);
        newRegion = addEquality(var, regRHS, newRegion);

      } else if (rhs instanceof IASTFunctionCallExpression) {
        // call of external function: we know nothing, so we do nothing

        // TODO can we assume, that malloc returns something !=0?
        // are there some "save functions"?

      } else {
        throw new UnrecognizedCCodeException(cfaEdge, rhs);
      }

      result = new BDDElement(rmgr, element.getFunctionCallElement(), newRegion,
          element.getVars(), cfaEdge.getPredecessor().getFunctionName());
    }

    assert !result.getRegion().isFalse();
    return result;
  }

  /** handles declarations like "int a = 0;" and "int b = !a;" */
  private BDDElement handleDeclarationEdge(BDDElement element, DeclarationEdge cfaEdge)
      throws UnrecognizedCCodeException {

    IASTDeclaration decl = cfaEdge.getDeclaration();

    if (decl instanceof IASTVariableDeclaration) {
      IASTVariableDeclaration vdecl = (IASTVariableDeclaration) decl;
      IASTInitializer initializer = vdecl.getInitializer();

      IASTExpression init = null;
      if (initializer == null && initAllVars) { // auto-initialize variables to zero
        init = Defaults.forType(decl.getDeclSpecifier(), decl.getFileLocation());
      } else if (initializer instanceof IASTInitializerExpression) {
        init = ((IASTInitializerExpression) initializer).getExpression();
      }

      // make variable (predicate) for LEFT SIDE of declaration,
      // delete variable, if it was initialized before i.e. in another block, with an existential operator
      String varName = vdecl.getName();
      Region var = makePredicate(varName, element.getFunctionName(), vdecl.isGlobal());
      Region newRegion = removePredicate(element.getRegion(), var);

      // track vars, so we can delete them after returning from a function,
      // see handleFunctionReturnEdge(...) for detail.
      if (!vdecl.isGlobal()) {
        element.getVars().add(varName);
      }

      // initializer on RIGHT SIDE available, make region for it
      if (init != null) {
        BDDExpressionVisitor ev = new BDDExpressionVisitor(element);
        Region regRHS = init.accept(ev);
        newRegion = addEquality(var, regRHS, newRegion);
        return new BDDElement(rmgr, element.getFunctionCallElement(), newRegion,
            element.getVars(), cfaEdge.getPredecessor().getFunctionName());
      }
    }

    return element; // if we know nothing, we return the old element
  }

  private BDDElement handleFunctionCallEdge(BDDElement element, FunctionCallEdge cfaEdge)
      throws UnrecognizedCCodeException {

    Region newRegion = element.getRegion();
    Set<String> newVars = new LinkedHashSet<String>();

    // overtake arguments from last functioncall into function,
    // get args from functioncall and make them equal with params from functionstart
    List<IASTExpression> args = cfaEdge.getArguments();
    List<IASTParameterDeclaration> params = cfaEdge.getSuccessor().getFunctionParameters();
    String innerFunctionName = cfaEdge.getSuccessor().getFunctionName();
    assert args.size() == params.size();

    for (int i = 0; i < args.size(); i++) {

      // make variable (predicate) for param
      String varName = params.get(i).getName();
      assert !newVars.contains(varName) : "variable used twice as param";
      newVars.add(varName);
      Region var = makePredicate(varName, innerFunctionName, false);

      // make region for arg and build equality of var and arg
      BDDExpressionVisitor ev = new BDDExpressionVisitor(element);
      Region arg = args.get(i).accept(ev);
      newRegion = addEquality(var, arg, newRegion);
    }

    return new BDDElement(rmgr, element, newRegion, newVars, innerFunctionName);
  }

  private BDDElement handleFunctionReturnEdge(BDDElement element, FunctionReturnEdge cfaEdge) {
    Region newRegion = element.getRegion();

    // delete variables from returning function,
    // this results in a smaller BDD and allows to call a function twice.
    for (String varName : element.getVars()) {
      newRegion = removePredicate(newRegion, makePredicate(varName, element.getFunctionName(), false));
    }

    // set result of function equal to variable on left side
    CallToReturnEdge fnkCall = cfaEdge.getSummaryEdge();
    IASTStatement call = fnkCall.getExpression().asStatement();

    // make region (predicate) for RIGHT SIDE
    Region retVar = makePredicate(FUNCTION_RETURN_VARIABLE, element.getFunctionName(), false);

    // handle assignments like "y = f(x);"
    if (call instanceof IASTFunctionCallAssignmentStatement) {
      IASTFunctionCallAssignmentStatement cAssignment = (IASTFunctionCallAssignmentStatement) call;
      IASTExpression lhs = cAssignment.getLeftHandSide();

      // make variable (predicate) for LEFT SIDE of assignment,
      // delete variable, if it was used before, this is done with an existential operator
      String varName = lhs.toASTString();
      BDDElement functionCall = element.getFunctionCallElement();
      Region var = makePredicate(varName, functionCall.getFunctionName(), isGlobal(lhs));
      newRegion = removePredicate(newRegion, var);
      newRegion = addEquality(var, retVar, newRegion);
    }

    // LAST ACTION: delete varname of right side
    newRegion = removePredicate(newRegion, retVar);

    return new BDDElement(rmgr, element.getFunctionCallElement().getFunctionCallElement(), newRegion,
        element.getFunctionCallElement().getVars(),
        cfaEdge.getSuccessor().getFunctionName());
  }

  private BDDElement handleReturnStatementEdge(BDDElement element, ReturnStatementEdge cfaEdge)
      throws UnrecognizedCCodeException {

    // make variable (predicate) for returnStatement,
    // delete variable, if it was used before, this is done with an existential operator
    Region retvar = makePredicate(FUNCTION_RETURN_VARIABLE, element.getFunctionName(), false);

    assert element.getRegion().equals(removePredicate(element.getRegion(), retvar)) : FUNCTION_RETURN_VARIABLE
        + " was used twice in one trace??";

    // make region for RIGHT SIDE, this is the 'x' from 'return (x);
    IASTRightHandSide rhs = cfaEdge.getExpression();
    if (rhs instanceof IASTExpression) {
      BDDExpressionVisitor ev = new BDDExpressionVisitor(element);
      Region regRHS = ((IASTExpression) rhs).accept(ev);
      Region newRegion = addEquality(retvar, regRHS, element.getRegion());
      return new BDDElement(rmgr, element.getFunctionCallElement(), newRegion,
          element.getVars(), cfaEdge.getPredecessor().getFunctionName());
    }
    return element;
  }

  private BDDElement handleAssumption(BDDElement element, AssumeEdge cfaEdge)
      throws UnrecognizedCCodeException {

    IASTExpression expression = cfaEdge.getExpression();
    BDDExpressionVisitor ev = new BDDExpressionVisitor(element);
    Region operand = expression.accept(ev);

    if (operand == null) { // assumption cannot be evaluated
      return element;

    } else {
      if (!cfaEdge.getTruthAssumption()) { // if false-branch
        operand = rmgr.makeNot(operand);
      }
      Region newRegion = rmgr.makeAnd(element.getRegion(), operand);
      if (newRegion.isFalse()) { // assumption is not fulfilled / not possible
        return null;
      } else {
        return new BDDElement(rmgr, element.getFunctionCallElement(), newRegion,
            element.getVars(), cfaEdge.getPredecessor().getFunctionName());
      }
    }
  }

  /** This function returns a region containing a variable.
   * The name of the variable is build from functionName and varName. */
  private Region makePredicate(String varName, String functionName, boolean isGlobal) {
    createdPredicates++;
    return rmgr.createPredicate(buildVarName(varName, isGlobal, functionName));
  }

  /** This function returns a region without a variable. */
  private Region removePredicate(Region region, Region existing) {
    deletedPredicates++;
    return rmgr.makeExists(region, existing);
  }

  private boolean isGlobal(IASTExpression exp) {
    if (exp instanceof IASTIdExpression) {
      IASTSimpleDeclaration decl = ((IASTIdExpression) exp).getDeclaration();
      if (decl instanceof IASTDeclaration) { return ((IASTDeclaration) decl).isGlobal(); }
    }
    return false;
  }

  private String buildVarName(String variableName, boolean isGlobal, String function) {
    if (isGlobal) {
      return variableName;
    } else {
      return function + "::" + variableName;
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState element, List<AbstractState> elements, CFAEdge cfaEdge,
      Precision precision) {
    // do nothing
    return null;
  }

  /** This function builds the equality of left and right side and adds it to the environment.
   * If left or right side is null, the environment is returned unchanged. */
  private Region addEquality(Region leftSide, Region rightSide, Region environment) {
    if (leftSide == null || rightSide == null) {
      return environment;
    } else {
      final Region assignRegion = rmgr.makeEqual(leftSide, rightSide);
      return rmgr.makeAnd(environment, assignRegion);
    }
  }

  /** This Visitor evaluates the visited expression and creates a region for it. */
  private class BDDExpressionVisitor
      implements ExpressionVisitor<Region, UnrecognizedCCodeException> {

    private String functionName;
    private BDDElement element;

    BDDExpressionVisitor(BDDElement element) {
      this.element = element;
      this.functionName = element.getFunctionName();
    }

    @Override
    public Region visit(IASTArraySubscriptExpression exp) throws UnrecognizedCCodeException {
      return makePredicate(exp.toASTString(), functionName, isGlobal(exp));
    }

    @Override
    public Region visit(IASTBinaryExpression exp) throws UnrecognizedCCodeException {
      Region operand1 = exp.getOperand1().accept(this);
      Region operand2 = exp.getOperand2().accept(this);

      if (operand1 == null || operand2 == null) { return null; }

      // does the environment imply the left/right side of binaryExp?
      Region leftSideSat = rmgr.makeAnd(element.getRegion(), operand1);
      Region rightSideSat = rmgr.makeAnd(element.getRegion(), operand2);
      boolean isLeftSideZero = leftSideSat.isFalse();
      boolean isRightSideZero = rightSideSat.isFalse();

      Region returnValue = null;
      // binary expression
      switch (exp.getOperator()) {
      case LOGICAL_AND:
        returnValue = rmgr.makeAnd(operand1, operand2);
        break;
      case LOGICAL_OR:
        returnValue = rmgr.makeOr(operand1, operand2);
        break;
      case EQUALS:
        // bdds cannot handle "2==3", only "==0" is possible
        if (isLeftSideZero || isRightSideZero) {
          returnValue = rmgr.makeEqual(operand1, operand2);
        }
        break;
      case NOT_EQUALS:
      case LESS_THAN:
      case GREATER_THAN:
        // bdds cannot handle "2!=3" or "a=2; b=3; a!=b"
        if (isLeftSideZero || isRightSideZero) {
          returnValue = rmgr.makeUnequal(operand1, operand2);
        }
        break;
      default:
        // a+b, a-b, etc --> don't know anything
      }
      return returnValue;
    }

    @Override
    public Region visit(IASTCastExpression exp) throws UnrecognizedCCodeException {
      // we ignore casts, because Zero is Zero.
      return exp.getOperand().accept(this);
    }

    @Override
    public Region visit(IASTFieldReference exp) throws UnrecognizedCCodeException {
      return makePredicate(exp.toASTString(), functionName, isGlobal(exp));
    }

    @Override
    public Region visit(IASTIdExpression exp) throws UnrecognizedCCodeException {
      return makePredicate(exp.toASTString(), functionName, isGlobal(exp));
    }

    @Override
    public Region visit(IASTCharLiteralExpression exp) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Region visit(IASTFloatLiteralExpression exp) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Region visit(IASTIntegerLiteralExpression exp) throws UnrecognizedCCodeException {
      Region region;
      if (exp.getValue().equals(BigInteger.ZERO)) {
        region = rmgr.makeFalse();
      } else {
        region = rmgr.makeTrue();
      }
      return region;
    }

    @Override
    public Region visit(IASTStringLiteralExpression exp) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Region visit(IASTTypeIdExpression exp) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Region visit(IASTUnaryExpression exp) throws UnrecognizedCCodeException {
      Region operand = exp.getOperand().accept(this);

      if (operand == null) { return null; }

      Region returnValue = null;
      switch (exp.getOperator()) {
      case NOT:
        returnValue = rmgr.makeNot(operand);
        break;
      case PLUS: // (+X == 0) <==> (X == 0)
      case MINUS: // (-X == 0) <==> (X == 0)
        returnValue = operand;
      default:
        // *exp --> don't know anything
      }
      return returnValue;
    }
  }
}
