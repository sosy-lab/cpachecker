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
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.MultiEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
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
public class BDDTransferRelation implements TransferRelation {

  private final NamedRegionManager rmgr;

  public BDDTransferRelation(NamedRegionManager manager) {
    this.rmgr = manager;
  }

  @Override
  public Collection<BDDElement> getAbstractSuccessors(
      AbstractElement element, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException {
    System.out.println("getSuccessor: " + cfaEdge + "  " + cfaEdge.getEdgeType());
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

    case ReturnStatementEdge:
    case DeclarationEdge: // TODO int a=0;
    case BlankEdge:
    case FunctionCallEdge:
    case FunctionReturnEdge:
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

  private BDDElement handleStatementEdge(BDDElement element, StatementEdge cfaEdge) {
    IASTStatement statement = cfaEdge.getStatement();
    if (!(statement instanceof IASTAssignment)) { return element; }
    IASTAssignment assignment = (IASTAssignment) statement;

    IASTExpression lhs = assignment.getLeftHandSide();
    BDDElement result = element;
    if (lhs instanceof IASTIdExpression || lhs instanceof IASTFieldReference
        || lhs instanceof IASTArraySubscriptExpression) {
      String varName = lhs.toASTString();
      IASTRightHandSide rhs = assignment.getRightHandSide();
      if (rhs instanceof IASTIntegerLiteralExpression) {

        // make variable
        BigInteger value = ((IASTIntegerLiteralExpression) rhs).getValue();
        Region var = rmgr.createPredicate(varName);

        // delete variable, if used before. this is done with an existential operator
        Region newRegion = rmgr.makeExists(element.getRegion(), var);

        // convert new variable to match its value.
        if (BigInteger.ZERO.equals(value)) {
          var = rmgr.makeNot(var);
        }

        // add new variable to region
        newRegion = rmgr.makeAnd(newRegion, var);

        result = new BDDElement(newRegion, rmgr);
      }
    }
    assert !result.getRegion().isFalse();
    return result;
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
      String varName = exp.toASTString(); //this.getvarName(op2.getRawSignature(), functionName);
      region = rmgr.createPredicate(varName);

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
    case STAR: // *exp, don't know anything
      break;
    default:
      throw new UnrecognizedCCodeException("Unhandled case "
          + unExp.toASTString(), edge);
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
      returnValue = rmgr.makeOr(
          rmgr.makeAnd(operand1, operand2),
          rmgr.makeAnd(rmgr.makeNot(operand1), rmgr.makeNot(operand2))
          );
      break;
    case NOT_EQUALS:
      returnValue = rmgr.makeOr(
          rmgr.makeAnd(rmgr.makeNot(operand1), operand2),
          rmgr.makeAnd(operand1, rmgr.makeNot(operand2))
          );
      break;
    default:
      // nothing, we don't know something
      // throw new UnrecognizedCCodeException("Only &&, ||, == and != are implemented, other operators are missing.", edge);
    }
    return returnValue;
  }

  /*
    public String getvarName(String variableName, String functionName) {
      if(globalVars.contains(variableName)){
        return "$global::" + variableName;
      }
      return functionName + "::" + variableName;
    }*/

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement element, List<AbstractElement> elements, CFAEdge cfaEdge,
      Precision precision) throws UnrecognizedCCodeException {
    // do nothing
    return null;
  }
}
