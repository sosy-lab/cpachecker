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
import java.util.ArrayList;
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
import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
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
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
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

  /** stack to track variables,
   * so we can cleanup the BDD after returning from a function */
  private List<Set<String>> stack;
  private Set<String> globalVars;
  private Set<String> topLevel;

  /** name for return-variables, it is used for function-returns. */
  private static final String FUNCTION_RETURN_VARIABLE = "__cpachecker_return_var";

  @Option(description = "initialize all variables to 0 when they are declared")
  private boolean initAllVars = false;

  public BDDTransferRelation(NamedRegionManager manager, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this);
    this.rmgr = manager;

    // initialize stack and globalVars to track variables
    globalVars = new LinkedHashSet<String>();
    topLevel = globalVars;
    stack = new ArrayList<Set<String>>();
    stack.add(globalVars); // floor of stack
  }

  @Override
  public Collection<BDDElement> getAbstractSuccessors(
      AbstractElement element, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException {
    BDDElement elem = (BDDElement) element;

    if (elem.getRegion().isFalse()) { return Collections.emptyList(); }

    BDDElement successor = null;

    switch (cfaEdge.getEdgeType()) {

    case AssumeEdge: {
      AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
      successor = handleAssumption(elem, assumeEdge.getExpression(), cfaEdge,
          assumeEdge.getTruthAssumption());
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
          successor = elem;
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
      String functionName = cfaEdge.getPredecessor().getFunctionName();
      Region var = rmgr.createPredicate(buildVarName(varName, functionName));
      Region newRegion = rmgr.makeExists(element.getRegion(), var);

      IASTRightHandSide rhs = assignment.getRightHandSide();
      if (rhs instanceof IASTExpression) {

        // make region for RIGHT SIDE and build equality of var and region
        Region regRHS = propagateBooleanExpression(
            (IASTExpression) rhs, functionName, cfaEdge, false);
        newRegion = addEquality(var, regRHS, newRegion);

      } else {
        throw new UnrecognizedCCodeException(cfaEdge, rhs);
      }

      result = new BDDElement(newRegion, rmgr);
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
      String varName = vdecl.getName();
      assert !topLevel.contains(varName);
      topLevel.add(varName);
      String functionName = cfaEdge.getPredecessor().getFunctionName();
      Region var = rmgr.createPredicate(buildVarName(varName, functionName));

      // initializer on RIGHT SIDE available, make region for it
      if (init != null) {
        Region regRHS = propagateBooleanExpression(init, functionName, cfaEdge, false);
        Region newRegion = addEquality(var, regRHS, element.getRegion());
        return new BDDElement(newRegion, rmgr);
      }
    }

    return element; // if we know nothing, we return the old element
  }

  private BDDElement handleFunctionCallEdge(BDDElement element, FunctionCallEdge cfaEdge)
      throws UnrecognizedCCodeException {

    // create Set for storing current variables, we have to delete them later
    topLevel = new LinkedHashSet<String>();
    stack.add(topLevel);

    Region newRegion = element.getRegion();

    // overtake arguments from last functioncall into function?
    List<IASTExpression> args = cfaEdge.getArguments();
    List<IASTParameterDeclaration> params = cfaEdge.getSuccessor().getFunctionParameters();
    assert args.size() == params.size();

    for (int i = 0; i < args.size(); i++) {

      // make variable (predicate) for param
      String varName = params.get(i).getName();
      assert !topLevel.contains(varName);
      topLevel.add(varName);
      String innerFunctionName = cfaEdge.getSuccessor().getFunctionName();
      Region var = rmgr.createPredicate(buildVarName(varName, innerFunctionName));

      // make region for arg and build equality of var and arg
      String outerFunctionName = cfaEdge.getPredecessor().getFunctionName();
      Region arg = propagateBooleanExpression(args.get(i), outerFunctionName, cfaEdge, false);
      newRegion = addEquality(var, arg, newRegion);
    }

    return new BDDElement(newRegion, rmgr);
  }

  private BDDElement handleFunctionReturnEdge(BDDElement element, FunctionReturnEdge cfaEdge)
      throws UnrecognizedCCodeException {
    Region newRegion = element.getRegion();

    // delete variables from returning function, this results in a smaller BDD
    String innerFunctionName = cfaEdge.getPredecessor().getFunctionName();
    for (String varName : topLevel) {
      newRegion = rmgr.makeExists(newRegion,
          rmgr.createPredicate(buildVarName(varName, innerFunctionName)));
    }

    // cleanup the stack
    Set<String> oldVars = stack.remove(stack.size() - 1);
    assert oldVars == topLevel;
    topLevel = stack.get(stack.size() - 1);

    // set result of function equal to variable on left side
    CallToReturnEdge fnkCall = cfaEdge.getSummaryEdge();
    IASTStatement call = fnkCall.getExpression().asStatement();

    if (call instanceof IASTFunctionCallAssignmentStatement) {
      IASTFunctionCallAssignmentStatement cAssignment = (IASTFunctionCallAssignmentStatement) call;
      IASTExpression lhs = cAssignment.getLeftHandSide();

      // make variable (predicate) for LEFT SIDE of assignment,
      // delete variable, if it was used before, this is done with an existential operator
      String varName = lhs.toASTString();
      String outerFunctionName = cfaEdge.getSuccessor().getFunctionName();
      Region var = rmgr.createPredicate(buildVarName(varName, outerFunctionName));
      newRegion = rmgr.makeExists(newRegion, var);

      // make region (predicate) for RIGHT SIDE
      Region retVar = rmgr.createPredicate(buildVarName(FUNCTION_RETURN_VARIABLE, innerFunctionName));

      newRegion = addEquality(var, retVar, newRegion);

      // LAST ACTION: delete varname of right side
      newRegion = rmgr.makeExists(newRegion, retVar);
    }

    return new BDDElement(newRegion, rmgr);
  }

  private BDDElement handleReturnStatementEdge(BDDElement element, ReturnStatementEdge cfaEdge)
      throws UnrecognizedCCodeException {

    // make variable (predicate) for returnStatement,
    // delete variable, if it was used before, this is done with an existential operator
    String innerFunctionName = cfaEdge.getPredecessor().getFunctionName();
    Region retvar = rmgr.createPredicate(buildVarName(FUNCTION_RETURN_VARIABLE, innerFunctionName));
    Region newRegion = rmgr.makeExists(element.getRegion(), retvar);
    assert newRegion.equals(element.getRegion()) : FUNCTION_RETURN_VARIABLE + " was used twice in one trace??";

    // make region for RIGHT SIDE, this is the 'x' from 'return (x);
    IASTRightHandSide rhs = cfaEdge.getExpression();
    if (rhs instanceof IASTExpression) {
      Region regRHS = propagateBooleanExpression(
          (IASTExpression) rhs, innerFunctionName, cfaEdge, false);
      newRegion = addEquality(retvar, regRHS, newRegion);
    }
    return new BDDElement(newRegion, rmgr);
  }

  private BDDElement handleAssumption(BDDElement element,
      IASTExpression expression, CFAEdge cfaEdge, boolean truthValue)
      throws UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    Region operand = propagateBooleanExpression(expression, functionName, cfaEdge, false);

    if (operand == null) { // assumption cannot be evaluated
      return element;

    } else {
      if (!truthValue) {
        operand = rmgr.makeNot(operand);
      }
      Region newRegion = rmgr.makeAnd(element.getRegion(), operand);
      if (newRegion.isFalse()) { // assumption is not fulfilled / not possible
        return null;
      } else {
        return new BDDElement(newRegion, rmgr);
      }
    }
  }

  /** Chooses function to propagate, depending on class of exp:
   * IASTIdExpression (&Co), IASTUnaryExpression, IASTBinaryExpression, IASTIntegerLiteralExpression.
   * @param ignoreLiterals ignore all numbers except Zero
   * @throws UnrecognizedCCodeException
   * @returns region containing all vars from the expression */
  private Region propagateBooleanExpression(IASTExpression exp, String functionName,
      CFAEdge edge, boolean ignoreLiterals)
      throws UnrecognizedCCodeException {
    Region region = null;

    if (exp instanceof IASTIdExpression || exp instanceof IASTFieldReference
        || exp instanceof IASTArraySubscriptExpression) {
      String varName = exp.toASTString();
      region = rmgr.createPredicate(buildVarName(varName, functionName));

    } else if (exp instanceof IASTUnaryExpression) {
      region = propagateUnaryBooleanExpression((IASTUnaryExpression) exp, functionName, edge);

    } else if (exp instanceof IASTBinaryExpression) {
      region = propagateBinaryBooleanExpression(((IASTBinaryExpression) exp), functionName, edge);

    } else if (exp instanceof IASTIntegerLiteralExpression) {
      IASTIntegerLiteralExpression number = (IASTIntegerLiteralExpression) exp;
      if (number.getValue().equals(BigInteger.ZERO)) {
        region = rmgr.makeFalse();
      } else if (!ignoreLiterals) {
        region = rmgr.makeTrue();
      }
    } else {
      throw new UnrecognizedCCodeException(edge, exp);
    }

    return region;
  }

  private Region propagateUnaryBooleanExpression(IASTUnaryExpression unExp,
      String functionName, CFAEdge edge)
      throws UnrecognizedCCodeException {

    Region operand = propagateBooleanExpression(unExp.getOperand(), functionName, edge, false);

    if (operand == null) { return null; }

    Region returnValue = null;
    switch (unExp.getOperator()) {
    case NOT:
      returnValue = rmgr.makeNot(operand);
      break;
    case MINUS: // (-X == 0) <==> (X == 0)
      returnValue = operand;
    default:
      // *exp --> don't know anything
    }
    return returnValue;
  }

  private Region propagateBinaryBooleanExpression(IASTBinaryExpression binExp,
      String functionName, CFAEdge edge)
      throws UnrecognizedCCodeException {

    Region operand1 = propagateBooleanExpression(binExp.getOperand1(), functionName, edge, true);
    Region operand2 = propagateBooleanExpression(binExp.getOperand2(), functionName, edge, true);

    if (operand1 == null || operand2 == null) { return null; }

    Region returnValue = null;
    // binary expression
    switch (binExp.getOperator()) {
    case LOGICAL_AND:
      returnValue = rmgr.makeAnd(operand1, operand2);
      break;
    case LOGICAL_OR:
      returnValue = rmgr.makeOr(operand1, operand2);
      break;
    case EQUALS:
      returnValue = rmgr.makeEqual(operand1, operand2);
      break;
    case NOT_EQUALS:
    case LESS_THAN:
    case GREATER_THAN:
      returnValue = rmgr.makeUnequal(operand1, operand2);
      break;
    default:
      // a+b, a-b, etc --> don't know anything
    }
    return returnValue;
  }

  public String buildVarName(String variableName, String functionName) {
    if (topLevel.contains(variableName) || FUNCTION_RETURN_VARIABLE.equals(variableName)) {
      return functionName + "::" + variableName;
    } else {
      assert globalVars.contains(variableName) : "variable not on stack and not global: " + variableName;
      return variableName;
    }
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement element, List<AbstractElement> elements, CFAEdge cfaEdge,
      Precision precision) throws UnrecognizedCCodeException {
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
}
