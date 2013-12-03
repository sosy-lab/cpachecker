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
package org.sosy_lab.cpachecker.cpa.octagon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
/**
 * Handles transfer relation for Octagon abstract domain library.
 * See <a href="http://www.di.ens.fr/~mine/oct/">Octagon abstract domain library</a>
 */
class OctTransferRelation implements TransferRelation {

  // set to set global variables
  private List<String> globalVars;

//  private String missingInformationLeftVariable = null;
//  private String missingInformationRightPointer = null;
//  private String missingInformationLeftPointer  = null;
//  private CExpression missingInformationRightExpression = null;

  /**
   * Class constructor.
   */
  public OctTransferRelation() {
    globalVars = new ArrayList<>();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState element,
                                                         Precision prec, CFAEdge cfaEdge) throws UnrecognizedCCodeException {

    // octElement is the region of the current state
    // this state will be updated using the edge

    OctState octState = null;
    OctState prevElement = (OctState)element;
    octState = ((OctState)element).clone();

    assert (octState != null);

    // check the type of the edge
    switch (cfaEdge.getEdgeType()) {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge:
      octState = handleStatement(octState, ((CStatementEdge)cfaEdge).getStatement(), cfaEdge);
      break;

    // this statement is a function return, e.g. return (a);
    // note that this is different from return edge
    // this is a statement edge which leads the function to the
    // last node of its CFA, where return edge is from that last node
    // to the return site of the caller function
    case ReturnStatementEdge:
      CReturnStatementEdge statementEdge = (CReturnStatementEdge) cfaEdge;
      octState = handleExitFromFunction(octState, statementEdge.getExpression(), statementEdge);
      break;

    // edge is a decleration edge, e.g. int a;
    case DeclarationEdge:
      octState = handleDeclaration(octState, (CDeclarationEdge) cfaEdge);
      break;

    // this is an assumption, e.g. if (a == b)
    case AssumeEdge:
      CAssumeEdge assumeEdge = (CAssumeEdge) cfaEdge;
      octState = handleAssumption(octState, assumeEdge.getExpression(), cfaEdge, assumeEdge.getTruthAssumption());
      break;

    // just ignore blank edges
    case BlankEdge:
      break;

    // func(x);
    case FunctionCallEdge:
      octState = handleFunctionCall(octState, prevElement, (CFunctionCallEdge) cfaEdge);
      break;

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case FunctionReturnEdge:
      octState = handleFunctionReturn(octState, (CFunctionReturnEdge) cfaEdge);
      break;

    // Summary edge, we handle this on function return, do nothing
    case CallToReturnEdge:
      assert (false);
      break;

    // this analysis is currently not able to handle multiedges
    case MultiEdge:
      throw new AssertionError("This cpa is currently not able to handle multiedges");
    }

    if (octState == null || octState.isEmpty()) {
      return Collections.emptySet();
    }

    return Collections.singleton(octState);
  }

  /**
   * Handles return from one function to another function.
   * @param element previous abstract state.
   * @param functionReturnEdge return edge from a function to its call site.
   * @return new abstract state.
   */
  private OctState handleFunctionReturn(OctState element, CFunctionReturnEdge functionReturnEdge) throws UnrecognizedCCodeException {

    CFunctionSummaryEdge summaryEdge = functionReturnEdge.getSummaryEdge();
    CFunctionCall exprOnSummary = summaryEdge.getExpression();

    OctState previousElem = element.getPreviousState();

    String callerFunctionName = functionReturnEdge.getSuccessor().getFunctionName();
    String calledFunctionName = functionReturnEdge.getPredecessor().getFunctionName();

    // expression is an assignment operation, e.g. a = g(b);
    if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement binExp = ((CFunctionCallAssignmentStatement)exprOnSummary);
      CExpression op1 = binExp.getLeftHandSide();

      // we expect left hand side of the expression to be a variable
      if (op1 instanceof CIdExpression || op1 instanceof CFieldReference) {

        String varName = op1.toASTString();
        String returnVarName = calledFunctionName + "::" + "___cpa_temp_result_var_";

        String assignedVarName = getvarName(varName, callerFunctionName);

        element.assignVariable(assignedVarName, returnVarName, 1);

      } else {
        throw new UnrecognizedCCodeException("on function return", summaryEdge, op1);
      }
    }

    // g(b), do nothing
    else if (exprOnSummary instanceof CFunctionCallStatement) {

    } else {
      throw new UnrecognizedCCodeException("on function return", summaryEdge, exprOnSummary);
    }

    // delete local variables
    element.removeLocalVariables(previousElem, globalVars.size());

    return element;
  }

  private OctState handleExitFromFunction(OctState element, CExpression expression,
                                          CReturnStatementEdge returnEdge) throws UnrecognizedCCodeException {

    String tempVarName = getvarName("___cpa_temp_result_var_", returnEdge.getSuccessor().getFunctionName());
    element.declareVariable(tempVarName);
    return handleAssignmentToVariable(element, "___cpa_temp_result_var_", expression, returnEdge);
  }

  private OctState handleFunctionCall(OctState octagonElement, OctState pPrevElement,
                                      CFunctionCallEdge callEdge) throws UnrecognizedCCodeException {

    octagonElement.setPreviousState(pPrevElement);

    CFunctionEntryNode functionEntryNode = callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<CExpression> arguments = callEdge.getArguments();

    assert (paramNames.size() == arguments.size());

    for (int i=0; i<arguments.size(); i++) {
      CExpression arg = arguments.get(i);

      // ignore casts
      if (arg instanceof CCastExpression) {
        arg = ((CCastExpression)arg).getOperand();
      }

      String nameOfParam = paramNames.get(i);
      String formalParamName = getvarName(nameOfParam, calledFunctionName);

      octagonElement.declareVariable(formalParamName);

      if (arg instanceof CIdExpression) {
        CIdExpression idExp = (CIdExpression) arg;
        String nameOfArg = idExp.getName();
        String actualParamName = getvarName(nameOfArg, callerFunctionName);

        octagonElement.assignVariable(formalParamName, actualParamName, 1);
      }

      else if (arg instanceof CLiteralExpression) {
        Long val = parseLiteral((CLiteralExpression)arg, callEdge);

        if (val != null) {
          octagonElement.assignConstant(formalParamName, val);
        }
      }

      // do nothing
      else if (arg instanceof CTypeIdExpression) {
      }

      else if (arg instanceof CUnaryExpression) {
        assert (((CUnaryExpression) arg).getOperator() == UnaryOperator.AMPER);
      }

      // do nothing
      else if (arg instanceof CPointerExpression) {
        //TODO check this, while introducing CPointerExpression this was created out of the assert from the UnaryExpression
      }

      // do nothing
      else if (arg instanceof CFieldReference) {
      }

      // do nothing
      else {
        // TODO forgetting
        // throw new ExplicitTransferException("Unhandled case");
      }
    }

    return octagonElement;
  }

  private OctState handleAssumption(OctState pElement, CExpression expression, CFAEdge cfaEdge,
                                         boolean truthValue) throws UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();

    // Binary operation
    if (expression instanceof CBinaryExpression) {
      CBinaryExpression binExp = ((CBinaryExpression)expression);
      BinaryOperator opType = binExp.getOperator();

      CExpression op1 = binExp.getOperand1();
      CExpression op2 = binExp.getOperand2();
      return propagateBooleanExpression(pElement, opType, op1, op2, functionName, truthValue, cfaEdge);
    }

    // Unary operation
    else if (expression instanceof CUnaryExpression) {
      CUnaryExpression unaryExp = ((CUnaryExpression)expression);

      // ! exp
      if (unaryExp.getOperator() == UnaryOperator.NOT) {
        return handleAssumption(pElement, unaryExp.getOperand(), cfaEdge, !truthValue);

      } else {
        throw new UnrecognizedCCodeException("Unknown unary operator in assumption", cfaEdge, expression);
      }
    }

    else if (expression instanceof CIdExpression || expression instanceof CFieldReference) {
      return propagateBooleanExpression(pElement, null, expression, null, functionName, truthValue, cfaEdge);
    }

    else if (expression instanceof CCastExpression) {
      return handleAssumption(pElement, ((CCastExpression)expression).getOperand(), cfaEdge, truthValue);
    }

    else {
      throw new UnrecognizedCCodeException("Unknown expression type in assumption", cfaEdge, expression);
    }
  }

  private OctState propagateBooleanExpression(OctState pElement, BinaryOperator opType, CExpression op1,
                                                   CExpression op2, String functionName, boolean truthValue, CFAEdge edge)
                                                   throws UnrecognizedCCodeException {

    // a (bop) ?
    if (op1 instanceof CIdExpression ||
        op1 instanceof CFieldReference ||
        op1 instanceof CArraySubscriptExpression) {

      // [literal]
      if (op2 == null && opType == null) {
        String varName = op1.toASTString();
        if (truthValue) {
          String variableName = getvarName(varName, functionName);
          return addIneqConstraint(pElement, variableName, 0);
        }
        // ! [literal]
        else {
          String variableName = getvarName(varName, functionName);
          return addEqConstraint(pElement, variableName, 0);
        }
      }
      // a (bop) 9
      else if (op2 instanceof CLiteralExpression) {
        CLiteralExpression literalExp = (CLiteralExpression)op2;
        String varName = op1.toASTString();
        String variableName = getvarName(varName, functionName);

        if (literalExp instanceof CIntegerLiteralExpression
            || literalExp instanceof CCharLiteralExpression) {
          long valueOfLiteral = parseLiteral(literalExp, edge);
          // a == 9
          if (opType == BinaryOperator.EQUALS) {
            if (truthValue) {
              return addEqConstraint(pElement, variableName, valueOfLiteral);
            }
            // ! a == 9
            else {
              return propagateBooleanExpression(pElement, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue, edge);
            }
          }
          // a != 9
          else if (opType == BinaryOperator.NOT_EQUALS) {
            if (truthValue) {
              return addIneqConstraint(pElement, variableName, valueOfLiteral);
            }
            // ! a != 9
            else {
              return propagateBooleanExpression(pElement, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue, edge);
            }
          }

          // a > 9
          else if (opType == BinaryOperator.GREATER_THAN) {
            if (truthValue) {
              return addGreaterConstraint(pElement, variableName, valueOfLiteral);
            } else {
              return propagateBooleanExpression(pElement, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue, edge);
            }
          }
          // a >= 9
          else if (opType == BinaryOperator.GREATER_EQUAL) {
            if (truthValue) {
              return addGreaterEqConstraint(pElement, variableName, valueOfLiteral);
            } else {
              return propagateBooleanExpression(pElement, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue, edge);
            }
          }
          // a < 9
          else if (opType == BinaryOperator.LESS_THAN) {
            if (truthValue) {
              return addSmallerConstraint(pElement, variableName, valueOfLiteral);
            } else {
              return propagateBooleanExpression(pElement, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue, edge);
            }
          }
          // a <= 9
          else if (opType == BinaryOperator.LESS_EQUAL) {
            if (truthValue) {
              return addSmallerEqConstraint(pElement, variableName, valueOfLiteral);
            } else {
              return propagateBooleanExpression(pElement, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue, edge);
            }
          }
          // [a - 9]
          else if (opType == BinaryOperator.MINUS) {
            if (truthValue) {
              return addIneqConstraint(pElement, variableName, valueOfLiteral);
            }
            // ! a != 9
            else {
              return propagateBooleanExpression(pElement, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue, edge);
            }
          }

          // [a + 9]
          else if (opType == BinaryOperator.PLUS) {
            valueOfLiteral = parseLiteralWithOppositeSign(literalExp, edge);
            if (truthValue) {
              return addIneqConstraint(pElement, variableName, valueOfLiteral);
            } else {
              valueOfLiteral = parseLiteralWithOppositeSign(literalExp, edge);

              return addEqConstraint(pElement, variableName, valueOfLiteral);
            }
          }

          // TODO nothing
          else if (opType == BinaryOperator.BINARY_AND ||
              opType == BinaryOperator.BINARY_OR ||
              opType == BinaryOperator.BINARY_XOR) {
            return pElement;
          }

          else {
            throw new UnrecognizedCCodeException("Unhandled case ", edge);
          }
        } else {
          throw new UnrecognizedCCodeException("Unhandled case ", edge);
        }
      }
      // a (bop) b
      else if (op2 instanceof CIdExpression ||
          (op2 instanceof CUnaryExpression && ((((CUnaryExpression)op2).getOperator() == UnaryOperator.AMPER))) ||
           op2 instanceof CPointerExpression) {
        String leftVarName = op1.toASTString();
        String rightVarName = op2.toASTString();

        String leftVariableName = getvarName(leftVarName, functionName);
        String rightVariableName = getvarName(rightVarName, functionName);

        // a == b
        if (opType == BinaryOperator.EQUALS) {
          if (truthValue) {
            return addEqConstraint(pElement, rightVariableName, leftVariableName);
          } else {
            return propagateBooleanExpression(pElement, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue, edge);
          }
        }
        // a != b
        else if (opType == BinaryOperator.NOT_EQUALS) {
          if (truthValue) {
            return addIneqConstraint(pElement, rightVariableName, leftVariableName);
          } else {
            return propagateBooleanExpression(pElement, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue, edge);
          }
        }
        // a > b
        else if (opType == BinaryOperator.GREATER_THAN) {
          if (truthValue) {
            return addGreaterConstraint(pElement, rightVariableName, leftVariableName);
          } else {
            return  propagateBooleanExpression(pElement, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue, edge);
          }
        }
        // a >= b
        else if (opType == BinaryOperator.GREATER_EQUAL) {
          if (truthValue) {
            return addGreaterEqConstraint(pElement, rightVariableName, leftVariableName);
          } else {
            return propagateBooleanExpression(pElement, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue, edge);
          }
        }
        // a < b
        else if (opType == BinaryOperator.LESS_THAN) {
          if (truthValue) {
            return addSmallerConstraint(pElement, rightVariableName, leftVariableName);
          } else {
            return propagateBooleanExpression(pElement, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue, edge);
          }
        }
        // a <= b
        else if (opType == BinaryOperator.LESS_EQUAL) {
          if (truthValue) {
            return addSmallerEqConstraint(pElement, rightVariableName, leftVariableName);
          } else {
            return propagateBooleanExpression(pElement, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue, edge);
          }
        } else {
          throw new UnrecognizedCCodeException("Unhandled case ", edge);
        }
      } else if (op2 instanceof CUnaryExpression) {
        String varName = op1.toASTString();

        CUnaryExpression unaryExp = (CUnaryExpression)op2;
        CExpression unaryExpOp = unaryExp.getOperand();

        UnaryOperator operatorType = unaryExp.getOperator();
        // a == -8
        if (operatorType == UnaryOperator.MINUS) {

          if (unaryExpOp instanceof CLiteralExpression) {
            CLiteralExpression literalExp = (CLiteralExpression)unaryExpOp;

            if (literalExp instanceof CIntegerLiteralExpression
                || literalExp instanceof CCharLiteralExpression) {
              long valueOfLiteral = parseLiteralWithOppositeSign(literalExp, edge);
              String variableName = getvarName(varName, functionName);

              // a == 9
              if (opType == BinaryOperator.EQUALS) {
                if (truthValue) {
                  return addEqConstraint(pElement, variableName, valueOfLiteral);
                }
                // ! a == 9
                else {
                  return propagateBooleanExpression(pElement, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue, edge);
                }
              }
              // a != 9
              else if (opType == BinaryOperator.NOT_EQUALS) {
                if (truthValue) {
                  return addIneqConstraint(pElement, variableName, valueOfLiteral);
                }
                // ! a != 9
                else {
                  return propagateBooleanExpression(pElement, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue, edge);
                }
              }

              // a > 9
              else if (opType == BinaryOperator.GREATER_THAN) {
                if (truthValue) {
                  return addGreaterConstraint(pElement, variableName, valueOfLiteral);
                } else {
                  return propagateBooleanExpression(pElement, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue, edge);
                }
              }
              // a >= 9
              else if (opType == BinaryOperator.GREATER_EQUAL) {
                if (truthValue) {
                  return addGreaterEqConstraint(pElement, variableName, valueOfLiteral);
                } else {
                  return propagateBooleanExpression(pElement, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue, edge);
                }
              }
              // a < 9
              else if (opType == BinaryOperator.LESS_THAN) {
                if (truthValue) {
                  return addSmallerConstraint(pElement, variableName, valueOfLiteral);
                } else {
                  return propagateBooleanExpression(pElement, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue, edge);
                }
              }
              // a <= 9
              else if (opType == BinaryOperator.LESS_EQUAL) {
                if (truthValue) {
                  return addSmallerEqConstraint(pElement, variableName, valueOfLiteral);
                } else {
                  return propagateBooleanExpression(pElement, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue, edge);
                }
              } else {
                throw new UnrecognizedCCodeException("Unhandled case ", edge);
              }
            } else {
              throw new UnrecognizedCCodeException("Unhandled case ", edge);
            }
          } else {
            throw new UnrecognizedCCodeException("Unhandled case ", edge);
          }
        } else {
          throw new UnrecognizedCCodeException("Unhandled case ", edge);
        }
      } else if (op2 instanceof CBinaryExpression) {
        String varName = op1.toASTString();
        String variableName = getvarName(varName, functionName);
        pElement.forget(variableName);
        return pElement;
      }
      // right hand side is a cast exp
      else if (op2 instanceof CCastExpression) {
        CCastExpression castExp = (CCastExpression)op2;
        CExpression exprInCastOp = castExp.getOperand();
        return propagateBooleanExpression(pElement, opType, op1, exprInCastOp, functionName, truthValue, edge);
      } else {
        String varName = op1.toASTString();
        String variableName = getvarName(varName, functionName);
        pElement.forget(variableName);
        return pElement;
      }
    } else if (op1 instanceof CCastExpression) {
      CCastExpression castExp = (CCastExpression) op1;
      CExpression castOperand = castExp.getOperand();
      return propagateBooleanExpression(pElement, opType, castOperand, op2, functionName, truthValue, edge);
    } else {
      String varName = op1.toASTString();
      String variableName = getvarName(varName, functionName);
      pElement.forget(variableName);
      return pElement;
    }
  }

  private OctState addSmallerEqConstraint(OctState pElement,
      String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = pElement.getVariableIndexFor(pRightVariableName);
    int lVarIdx = pElement.getVariableIndexFor(pLeftVariableName);
    pElement.addConstraint(3, lVarIdx, rVarIdx, 0);
    return pElement;
  }

  // Note that this only works if both variables are integers
  private OctState addSmallerConstraint(OctState pElement,
      String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = pElement.getVariableIndexFor(pRightVariableName);
    int lVarIdx = pElement.getVariableIndexFor(pLeftVariableName);
    pElement.addConstraint(3, lVarIdx, rVarIdx, -1);
    return pElement;
  }


  private OctState addGreaterEqConstraint(OctState pElement,
      String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = pElement.getVariableIndexFor(pRightVariableName);
    int lVarIdx = pElement.getVariableIndexFor(pLeftVariableName);
    pElement.addConstraint(4, lVarIdx, rVarIdx, 0);
    return pElement;
  }

  // Note that this only works if both variables are integers
  private OctState addGreaterConstraint(OctState pElement,
      String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = pElement.getVariableIndexFor(pRightVariableName);
    int lVarIdx = pElement.getVariableIndexFor(pLeftVariableName);
    pElement.addConstraint(4, lVarIdx, rVarIdx, -1);
    return pElement;
  }

  // Note that this only works if both variables are integers
  private OctState addIneqConstraint(OctState pElement,
      String pRightVariableName, String pLeftVariableName) {
    OctState newElem1 = null;
    newElem1 = pElement.clone();
    addEqConstraint(newElem1, pLeftVariableName, pRightVariableName);
    if (! newElem1.isEmpty()) {
      return null;
    } else {
      return pElement;
    }
  }

  private OctState addEqConstraint(OctState pElement,
      String pRightVariableName, String pLeftVariableName) {
//    addSmallerEqConstraint(pElement, pRightVariableName, pLeftVariableName);
//    addGreaterEqConstraint(pElement, pRightVariableName, pLeftVariableName);
//    return pElement;

    OctState newElem1 = null;
    newElem1 = pElement.clone();
    addSmallerEqConstraint(pElement, pRightVariableName, pLeftVariableName);
    addGreaterEqConstraint(pElement, pRightVariableName, pLeftVariableName);
    if (newElem1.isEmpty()) {
      return null;
    } else {
      pElement.assignVariable(pLeftVariableName, pRightVariableName, 1);
      return pElement;
    }
  }

  private OctState addSmallerEqConstraint(OctState pElement,
      String pVariableName, long pValueOfLiteral) {
    int varIdx = pElement.getVariableIndexFor(pVariableName);
    pElement.addConstraint(0, varIdx, 0, (int)pValueOfLiteral);
    return pElement;
  }

  // Note that this only works if both variables are integers
  private OctState addSmallerConstraint(OctState pElement,
      String pVariableName, long pValueOfLiteral) {
    int varIdx = pElement.getVariableIndexFor(pVariableName);
    pElement.addConstraint(0, varIdx, -1, (int)pValueOfLiteral-1);
    return pElement;
  }

  private OctState addGreaterEqConstraint(OctState pElement,
      String pVariableName, long pValueOfLiteral) {
    int varIdx = pElement.getVariableIndexFor(pVariableName);
    pElement.addConstraint(1, varIdx, 0, (0 - (int)pValueOfLiteral));
    return pElement;
  }

  // Note that this only works if both variables are integers
  private OctState addGreaterConstraint(OctState pElement,
      String pVariableName, long pValueOfLiteral) {
    int varIdx = pElement.getVariableIndexFor(pVariableName);
    pElement.addConstraint(1, varIdx, 0, (-1 - (int)pValueOfLiteral));
    return pElement;
  }

  private OctState addEqConstraint(OctState pElement,
      String pVariableName, long pI) {
//    addGreaterEqConstraint(pElement, pVariableName, pI);
//    addSmallerEqConstraint(pElement, pVariableName, pI);
//    return pElement;

    OctState newElem1 = null;
    newElem1 = pElement.clone();
    addSmallerEqConstraint(pElement, pVariableName, pI);
    addGreaterEqConstraint(pElement, pVariableName, pI);
    if (newElem1.isEmpty()) {
      return null;
    } else {
      pElement.assignConstant(pVariableName, pI);
      return pElement;
    }

  }

  // Note that this only works if both variables are integers
  private OctState addIneqConstraint(OctState pElement,
      String pVariableName, long pI) {
    OctState newElem1 = null;
    newElem1 = pElement.clone();
    addEqConstraint(newElem1, pVariableName, pI);
    if (! newElem1.isEmpty()) {
      return pElement;
    } else {
      return pElement;
    }
  }

  private OctState handleDeclaration(OctState pElement, CDeclarationEdge declarationEdge) throws UnrecognizedCCodeException {

    if (declarationEdge.getDeclaration() instanceof CVariableDeclaration) {
      CVariableDeclaration decl = (CVariableDeclaration)declarationEdge.getDeclaration();

      // get the variable name in the declarator
      String varName = decl.getName();

      // TODO check other types of variables later - just handle primitive
      // types for the moment
      // don't add pointer variables to the list since we don't track them
      if (decl.getType() instanceof CPointerType) {
        // also we do not look for an initializer we add the declaration, otherwise
        // we cannot cope with idexpressions which have this variablename later on
        String variableName = getvarName(varName, declarationEdge.getPredecessor().getFunctionName());
        pElement.declareVariable(variableName);
        return pElement;
      }

      // if this is a global variable, add to the list of global variables
      if (decl.isGlobal()) {
        globalVars.add(varName);

        Long v;

        // do the declaration of the variable, even if we have no initializer
        String variableName = getvarName(varName, declarationEdge.getPredecessor().getFunctionName());
        pElement.declareVariable(variableName);

        CInitializer init = decl.getInitializer();
        if (init != null) {
          if (init instanceof CInitializerExpression) {
            CExpression exp = ((CInitializerExpression)init).getExpression();

            v = getExpressionValue(pElement, exp, varName, declarationEdge);

            // if there is an initializerlist, the variable is either an array or a struct/union
            // we cannot handle them, so simply return the previous state
          } else if (init instanceof CInitializerList) {
            return pElement;

          } else {
            // TODO show warning
            v = null;
          }
        } else {
          // global variables without initializer are set to 0 in C
          v = 0L;
        }

        if (v != null) {
          pElement.assignConstant(variableName, v.longValue());
          return pElement;
        }
      } else {
        String variableName = getvarName(varName, declarationEdge.getPredecessor().getFunctionName());
        pElement.declareVariable(variableName);
        return pElement;
      }
    } else if (declarationEdge.getDeclaration() instanceof CTypeDeclaration
               || declarationEdge.getDeclaration() instanceof CFunctionDeclaration) {
      return pElement;
    }

    assert (false) : declarationEdge.getDeclaration() + " (" + declarationEdge.getDeclaration().getClass() + ")";
    return null;
  }


  private OctState handleStatement(OctState pElement, CStatement expression, CFAEdge cfaEdge) throws UnrecognizedCCodeException {
    // expression is a binary operation, e.g. a = b;
    if (expression instanceof CAssignment) {
      return handleAssignment(pElement, (CAssignment)expression, cfaEdge);

      // external function call, do nothing
    } else if (expression instanceof CFunctionCallStatement) {
      return pElement;

      // there is such a case, do nothing
    } else if (expression instanceof CExpressionStatement) {
      return pElement;

    } else {
      throw new UnrecognizedCCodeException("unknown statement", cfaEdge, expression);
    }
  }

  private OctState handleAssignment(OctState pElement,
      CAssignment assignExpression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    CExpression op1 = assignExpression.getLeftHandSide();
    CRightHandSide op2 = assignExpression.getRightHandSide();

    // a = ...
    if (op1 instanceof CIdExpression) {
      return handleAssignmentToVariable(pElement, ((CIdExpression)op1).getName(), op2, cfaEdge);

      // *a = ...
    } else if (op1 instanceof CPointerExpression) {
      op1 = ((CPointerExpression)op1).getOperand();

      // Cil produces code like
      // *((int*)__cil_tmp5) = 1;
      // so remove cast and make a normal assignment for this case
      if (op1 instanceof CCastExpression) {
        op1 = ((CCastExpression) op1).getOperand();
        if (((CCastExpression)op1).getExpressionType() instanceof CPointerType && op1 instanceof CIdExpression) {
          return handleAssignmentToVariable(pElement, ((CIdExpression)op1).getName(), op2, cfaEdge);
        }
      }

      if (op1 instanceof CIdExpression) {
//        missingInformationLeftPointer = op1.getRawSignature();
//        missingInformationRightExpression = op2;

      } else {
        throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
      }

      return pElement;

    } else if (op1 instanceof CFieldReference) {
      // TODO assignment to field
      return pElement;

    } else if (op1 instanceof CArraySubscriptExpression) {
      // TODO assignment to array cell
      return pElement;

    } else {
      throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
    }
  }

  private OctState handleAssignmentToVariable(OctState pElement,
      String lParam, CRightHandSide rightExp, CFAEdge cfaEdge) throws UnrecognizedCCodeException {
    String functionName = cfaEdge.getPredecessor().getFunctionName();

    // a = 8.2 or "return;" (when rightExp == null)
    if (rightExp == null || rightExp instanceof CLiteralExpression) {
      return handleAssignmentOfLiteral(pElement, lParam, (CLiteralExpression)rightExp, functionName, cfaEdge);
    }
    // a = b
    else if (rightExp instanceof CIdExpression) {
      return handleAssignmentOfVariable(pElement, lParam, (CIdExpression)rightExp, functionName, 1);
    }
    // a = (cast) ?
    else if (rightExp instanceof CCastExpression) {
      return handleAssignmentOfCast(pElement, lParam, (CCastExpression)rightExp, cfaEdge);
    }
    // a = -b
    else if (rightExp instanceof CUnaryExpression) {
      return handleAssignmentOfUnaryExp(pElement, lParam, (CUnaryExpression)rightExp, cfaEdge);
    }
    // a = *b
    else if (rightExp instanceof CPointerExpression) {
      CExpression operand = ((CPointerExpression)rightExp).getOperand();

      if (operand instanceof CCastExpression) {
        operand = ((CCastExpression)operand).getOperand();
      }

      if (operand instanceof CIdExpression) {
//        missingInformationLeftVariable = assignedVar;
//        missingInformationRightPointer = unaryOperand.getRawSignature();
      } else {
        throw new UnrecognizedCCodeException("too complex pointer dereference", cfaEdge, operand);
      }
      return null;
    }
    // a = b op c
    else if (rightExp instanceof CBinaryExpression) {
      CBinaryExpression binExp = (CBinaryExpression)rightExp;

      return handleAssignmentOfBinaryExp(pElement, lParam, binExp.getOperand1(),
          binExp.getOperand2(), binExp.getOperator(), cfaEdge);
    }
    // a = extCall();  or  a = b->c;
    else if (rightExp instanceof CFunctionCallExpression
        || rightExp instanceof CFieldReference) {
      //      OctState newElement = element.clone();
      String lvarName = getvarName(lParam, functionName);
      pElement.forget(lvarName);
      return pElement;
    } else {
      throw new UnrecognizedCCodeException("unsupported expression", cfaEdge, rightExp);
    }
  }

  private OctState handleAssignmentOfCast(OctState pElement,
      String lParam, CCastExpression castExp, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {
    CExpression castOperand = castExp.getOperand();
    return handleAssignmentToVariable(pElement, lParam, castOperand, cfaEdge);
  }

  private OctState handleAssignmentOfUnaryExp(OctState pElement,
      String lParam, CUnaryExpression unaryExp, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = -b is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
    //    OctState newElement = element.clone();

    CExpression unaryOperand = unaryExp.getOperand();

    // a = -b or similar
    Long value = getExpressionValue(pElement, unaryExp, functionName, cfaEdge);
    if (value != null) {
      pElement.assignConstant(assignedVar, value);
      return pElement;
    } else {
      String rVarName = unaryOperand.toASTString();
      pElement.assignVariable(assignedVar, rVarName, -1);
      return pElement;
    }

    //TODO ?
  }

  private OctState handleAssignmentOfBinaryExp(OctState pElement,
      String lParam, CExpression lVarInBinaryExp, CExpression rVarInBinaryExp,
      BinaryOperator binaryOperator, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = b + c is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
    //    OctState newElement = element.clone();

    switch (binaryOperator) {
    case DIVIDE:
    case MODULO:
    case LESS_EQUAL:
    case GREATER_EQUAL:
    case BINARY_AND:
    case BINARY_OR:
      // TODO check which cases can be handled (I think all)
      pElement.forget(assignedVar);
      return pElement;

    case PLUS:
    case MINUS:
    case MULTIPLY:

      Long val1;
      Long val2;

      val1 = getExpressionValue(pElement, lVarInBinaryExp, functionName, cfaEdge);
      val2 = getExpressionValue(pElement, rVarInBinaryExp, functionName, cfaEdge);

      if (val1 != null && val2 != null) {
        long value;
        switch (binaryOperator) {

        case PLUS:
          value = val1 + val2;
          break;

        case MINUS:
          value = val1 - val2;
          break;

        case MULTIPLY:
          value = val1 * val2;
          break;

        default:
          throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge);
        }
        pElement.assignConstant(assignedVar, value);
        return pElement;
      }

      int lVarCoef = 0;
      int rVarCoef = 0;
      int constVal = 0;

      String lVarName = null;
      String rVarName = null;

      if (val1 == null && val2 != null) {
        if (lVarInBinaryExp instanceof CIdExpression) {
          lVarName = ((CIdExpression)lVarInBinaryExp).getName();

          switch (binaryOperator) {

          case PLUS:
            constVal = val2.intValue();
            lVarCoef = 1;
            rVarCoef = 0;
            break;

          case MINUS:
            constVal = 0 - val2.intValue();
            lVarCoef = 1;
            rVarCoef = 0;
            break;

          case MULTIPLY:
            lVarCoef = val2.intValue();
            rVarCoef = 0;
            constVal = 0;
            break;

          default:
            throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge);
          }
        } else {
          throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge);
        }
      }

      else if (val1 != null && val2 == null) {
        if (lVarInBinaryExp instanceof CIdExpression) {
          rVarName = ((CIdExpression)rVarInBinaryExp).getName();

          switch (binaryOperator) {

          case PLUS:
            constVal = val1.intValue();
            lVarCoef = 0;
            rVarCoef = 1;
            break;

          case MINUS:
            constVal = val1.intValue();
            lVarCoef = 0;
            rVarCoef = -1;
            break;

          case MULTIPLY:
            rVarCoef = val1.intValue();
            lVarCoef = 0;
            constVal = 0;
            break;

          default:
            throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge);
          }
        } else {
          throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge);
        }
      }

      else if (val1 == null && val2 == null) {
        if (lVarInBinaryExp instanceof CIdExpression) {
          lVarName = ((CIdExpression)lVarInBinaryExp).getName();
          rVarName = ((CIdExpression)rVarInBinaryExp).getName();

          switch (binaryOperator) {

          case PLUS:
            lVarCoef = 1;
            rVarCoef = 1;
            break;

          case MINUS:
            lVarCoef = 1;
            rVarCoef = -1;
            break;

          case MULTIPLY:
            pElement.forget(assignedVar);
            return pElement;

          default:
            throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge);
          }
        } else {
          throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge);
        }
      }

      pElement.assignmentOfBinaryExp(assignedVar, getvarName(lVarName, functionName), lVarCoef, getvarName(rVarName, functionName), rVarCoef, constVal);
      return pElement;

    case EQUALS:
    case NOT_EQUALS:

      Long lVal = getExpressionValue(pElement, lVarInBinaryExp, functionName, cfaEdge);
      Long rVal = getExpressionValue(pElement, rVarInBinaryExp, functionName, cfaEdge);

      // TODO handle more cases later

      if (lVal == null || rVal == null) {
        pElement.forget(assignedVar);
        return pElement;

      } else {
        // assign 1 if expression holds, 0 otherwise
        long result = (lVal.equals(rVal) ? 1 : 0);

        if (binaryOperator == BinaryOperator.NOT_EQUALS) {
          // negate
          result = 1 - result;
        }
        pElement.assignConstant(assignedVar, result);
        return pElement;
      }
      //      break;

    default:
      // TODO warning
      pElement.forget(assignedVar);
      return pElement;
    }
    // TODO ?
    //    return null;
  }

  //  // TODO modify this.
  private Long getExpressionValue(OctState pElement, CRightHandSide expression,
      String functionName, CFAEdge cfaEdge) throws UnrecognizedCCodeException {

    if (expression instanceof CLiteralExpression) {
      return parseLiteral((CLiteralExpression)expression, cfaEdge);

    } else if (expression instanceof CIdExpression) {
      return null;
    } else if (expression instanceof CCastExpression) {
      return getExpressionValue(pElement, ((CCastExpression)expression).getOperand(),
          functionName, cfaEdge);

    } else if (expression instanceof CUnaryExpression) {
      CUnaryExpression unaryExpression = (CUnaryExpression)expression;
      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();

      switch (unaryOperator) {

      case MINUS:
        Long val = getExpressionValue(pElement, unaryOperand, functionName, cfaEdge);
        return (val != null) ? -val : null;

      case AMPER:
        return null; // valid expresion, but it's a pointer value

      default:
        throw new UnrecognizedCCodeException("unknown unary operator", cfaEdge, unaryExpression);
      }
    } else {
      // TODO fields, arrays
      throw new UnrecognizedCCodeException("unsupported expression", cfaEdge, expression);
    }
  }

  private OctState handleAssignmentOfVariable(OctState pElement,
      String lParam, CExpression op2, String functionName, int coef) {
    String rParam = op2.toASTString();

    String leftVarName = getvarName(lParam, functionName);
    String rightVarName = getvarName(rParam, functionName);

    pElement.assignVariable(leftVarName, rightVarName, coef);
    return pElement;
  }

//  private OctState handleAssignmentOfReturnVariable(OctState pElement,
//      String lParam, String tempVarName, String functionName, int coef)
//  {
//    String leftVarName = getvarName(lParam, functionName);
//
//    return assignVariable(pElement, leftVarName, tempVarName, coef);
//  }


  private OctState handleAssignmentOfLiteral(OctState pElement,
      String lParam, CLiteralExpression op2, String functionName, CFAEdge edge)
  throws UnrecognizedCCodeException {
    //    OctState newElement = element.clone();

    // op2 may be null if this is a "return;" statement
    Long val = (op2 == null ? Long.valueOf(0L) : parseLiteral(op2, edge));

    String assignedVar = getvarName(lParam, functionName);
    if (val != null) {
      pElement.assignConstant(assignedVar, val);
      return pElement;
    } else {
      pElement.forget(assignedVar);
      return pElement;
    }
    //TODO
    //    return null;
  }

  private Long parseLiteral(CLiteralExpression expression, CFAEdge edge) throws UnrecognizedCCodeException {
    if (expression instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression)expression).asLong();

    } else if (expression instanceof CFloatLiteralExpression) {
      return null;

    } else if (expression instanceof CCharLiteralExpression) {
      return (long)((CCharLiteralExpression)expression).getCharacter();

    } else if (expression instanceof CStringLiteralExpression) {
      return null;

    } else {
      throw new UnrecognizedCCodeException("unknown literal", edge, expression);
    }
  }

  private Long parseLiteralWithOppositeSign(CLiteralExpression expression, CFAEdge edge) throws UnrecognizedCCodeException {
    Long value = parseLiteral(expression, edge);
    if (value != null) {
      value = -value;
    }
    return value;
  }

  public String getvarName(String variableName, String functionName) {
    if (variableName == null) {
      return null;
    }
    if (globalVars.contains(variableName)) {
      return variableName;
    }
    return functionName + "::" + variableName;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element,
      List<AbstractState> otherElements, CFAEdge cfaEdge,
      Precision precision) {
    return null;
  }
}
