/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.local;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadCreateStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.local.LocalState.DataType;
import org.sosy_lab.cpachecker.exceptions.HandleCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.ConstantIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GeneralLocalVariableIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GlobalVariableIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.IdentifierCreator;
import org.sosy_lab.cpachecker.util.identifiers.LocalVariableIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.ReturnIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;

@Options(prefix="cpa.local")
public class LocalTransferRelation extends ForwardingTransferRelation<LocalState, LocalState, Precision> {

  @Option(name="allocatefunctions", description = "functions, which allocate new free memory",
      secure = true)
  private Set<String> allocate;

  //Use it carefully: just alloc is not enough, because EMG generates ldv_random_allocationless_scenario_callback_*
  @Option(name="allocateFunctionPattern", description = "functions, which allocate new free memory",
      secure = true)
  private Set<String> allocatePattern = Sets.newHashSet();

  @Option(name="conservativefunctions", description = "functions, which do not change sharedness of parameters",
      secure = true)
  private Set<String> conservationOfSharedness;

  private Map<String, Integer> allocateInfo;

  public LocalTransferRelation(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    parseAllocatedFunctions(config);
  }

  @SuppressWarnings("deprecation")
  private void parseAllocatedFunctions(Configuration config) {
    String num;
    allocateInfo = new HashMap<>();
    if (allocate != null) {
      for (String funcName : allocate) {
        num = config.getProperty(funcName + ".parameter");
        if (num == null) {
          allocateInfo.put(funcName, 0);
        } else {
          allocateInfo.put(funcName, Integer.parseInt(num));
        }
      }
    }
  }

  @Override
  protected LocalState handleAssumption(CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption) {
    return state.clone();
  }

  @Override
  protected LocalState handleReturnStatementEdge(CReturnStatementEdge cfaEdge) throws HandleCodeException {
    //TODO is it necessary to clone every time?
    //Guess, it is possible to return the same state (we add only in cloned states)
    LocalState newState = state.clone();
    Optional<CExpression> returnExpression = cfaEdge.getExpression();
    if (returnExpression.isPresent()) {
      int dereference = findDereference(returnExpression.get().getExpressionType());
      if (dereference > 0) {
        AbstractIdentifier returnId = createId(returnExpression.get(), dereference);
        DataType type = newState.getType(returnId);
        if (type == null && returnId instanceof ConstantIdentifier) {
          // return (struct myStruct *) 0; - consider the value as local
          type = DataType.LOCAL;
        }
        newState.set(ReturnIdentifier.getInstance(), type);
      }
    }
    return newState;
  }

  @Override
  protected LocalState handleFunctionReturnEdge(CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr, String callerFunctionName) throws HandleCodeException {
    CFunctionCall exprOnSummary     = cfaEdge.getSummaryEdge().getExpression();
    DataType returnType             = state.getType(ReturnIdentifier.getInstance());
    LocalState newElement           = state.getClonedPreviousState();
    if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement assignExp = ((CFunctionCallAssignmentStatement)exprOnSummary);

      CExpression op1 = assignExp.getLeftHandSide();
      CType type = op1.getExpressionType();
      //find type in old state...
      int dereference = findDereference(type);
      AbstractIdentifier returnId = createId(op1, dereference);

      if (!handleSpecialFunction(newElement, returnId, assignExp.getRightHandSide())) {
        newElement.set(returnId, returnType);
      }
    }
    //Update the outer parameters:
    CFunctionSummaryEdge sEdge = cfaEdge.getSummaryEdge();
    CFunctionEntryNode entry = sEdge.getFunctionEntry();
    String funcName = entry.getFunctionName();
    if (!isAllocatedFunction(funcName)) {
      List<String> paramNames = entry.getFunctionParameterNames();
      List<CExpression> arguments = sEdge.getExpression().getFunctionCallExpression().getParameterExpressions();
      List<CParameterDeclaration> parameterTypes = entry.getFunctionDefinition().getParameters();

      List<Pair<AbstractIdentifier, LocalVariableIdentifier>> toProcess =
          extractIdentifiers(arguments, paramNames, parameterTypes);

      for (Pair<AbstractIdentifier, LocalVariableIdentifier> pairId : toProcess) {
        DataType newType = state.getType(pairId.getSecond());
        newElement.set(pairId.getFirst(), newType);
      }
    }
    return newElement;
  }

  @Override
  protected LocalState handleFunctionCallEdge(CFunctionCallEdge cfaEdge,
      List<CExpression> arguments, List<CParameterDeclaration> parameterTypes,
      String calledFunctionName) {
    LocalState newState = LocalState.createNextLocalState(state);
    CFunctionEntryNode functionEntryNode = cfaEdge.getSuccessor();
    List<String> paramNames = functionEntryNode.getFunctionParameterNames();

    List<Pair<AbstractIdentifier, LocalVariableIdentifier>> toProcess =
        extractIdentifiers(arguments, paramNames, parameterTypes);

    //TODO Make it with config
    boolean isThreadCreate = cfaEdge.getSummaryEdge().getExpression() instanceof CThreadCreateStatement;

    for (Pair<AbstractIdentifier, LocalVariableIdentifier> pairId : toProcess) {
      DataType type = state.getType(pairId.getFirst());
      if (isThreadCreate && pairId.getSecond().getDereference() > 0) {
        //Data became shared after thread creation
        type = DataType.GLOBAL;
      }
      newState.set(pairId.getSecond(), type);
    }
    return newState;
  }

  @Override
  protected LocalState handleStatementEdge(CStatementEdge cfaEdge, CStatement statement) {
    LocalState newState = state.clone();
    if (statement instanceof CAssignment) {
      // assignment like "a = b" or "a = foo()"
      CAssignment assignment = (CAssignment)statement;
      CExpression left = assignment.getLeftHandSide();
      assign(newState, left, assignment.getRightHandSide());
    }
    return newState;
  }

  @Override
  protected LocalState handleDeclarationEdge(CDeclarationEdge declEdge, CDeclaration decl) {
    LocalState newState = state.clone();

    if (decl instanceof CVariableDeclaration) {
      CInitializer init = ((CVariableDeclaration)decl).getInitializer();
      if (init != null && init instanceof CInitializerExpression) {
        assign(newState, new CIdExpression(((CVariableDeclaration)decl).getFileLocation(), decl),
            ((CInitializerExpression)init).getExpression());
      } else {
        if (findDereference(decl.getType()) > 0 && !decl.isGlobal()
            && decl.getType() instanceof CArrayType) {
          //we don't save global variables
          newState.set(
              new GeneralLocalVariableIdentifier(decl.getName(), findDereference(decl.getType())),
              DataType.LOCAL);
        }
      }
    }
    return newState;
  }

  private boolean handleSpecialFunction(LocalState pSuccessor, AbstractIdentifier leftId, CFunctionCallExpression right) {
    String funcName = right.getFunctionNameExpression().toASTString();
    boolean isConservativeFunction = (conservationOfSharedness == null ? false : conservationOfSharedness.contains(funcName));

    if (isAllocatedFunction(funcName)) {
      Integer num = allocateInfo.get(funcName);
      if (num == null) {
        //Means that we use pattern
        num = 0;
      }
      if (num == 0) {
        //local data are returned from function
        pSuccessor.set(leftId, DataType.LOCAL);
      } else if (num > 0) {
        num = allocateInfo.get(funcName);
        if (num > 0) {
          //local data are transmitted, as function parameters. F.e., allocate(&pointer);
          CExpression parameter = right.getParameterExpressions().get(num - 1);
          int dereference = findDereference(parameter.getExpressionType());
          AbstractIdentifier rightId = createId(parameter, dereference);
          pSuccessor.set(rightId, DataType.LOCAL);
        }
      }
      return true;

    } else if (isConservativeFunction){

      List<CExpression> parameters = right.getParameterExpressions();
      // Usually it looks like 'priv = netdev_priv(dev)'
      // Other cases will be handled if they appear
      CExpression targetParam = parameters.get(0);
      AbstractIdentifier paramId = createId(targetParam, leftId.getDereference());
      pSuccessor.set(leftId, state.getType(paramId));
      return true;

    }
    return false;
  }

  private List<Pair<AbstractIdentifier, LocalVariableIdentifier>> extractIdentifiers(
      List<CExpression> arguments,
      List<String> paramNames, List<CParameterDeclaration> parameterTypes) {

    List<Pair<AbstractIdentifier, LocalVariableIdentifier>> result = new LinkedList<>();
    CExpression currentArgument;
    int dereference;
    for (int i = 0; i < arguments.size(); i++) {
      if (i >= paramNames.size()) {
        //function with unknown parameter size: printf(char* a, ...)
        //assign what we can
        break;
      }
      currentArgument = arguments.get(i);
      dereference = findDereference(parameterTypes.get(i).getType());
      AbstractIdentifier previousId;

      for (int j = 1; j <= dereference; j++) {
        LocalVariableIdentifier id = new GeneralLocalVariableIdentifier(paramNames.get(i), j);
        previousId = createId(currentArgument, j);
        result.add(Pair.of(previousId, id));
      }
    }
    return result;
  }

  public static int findDereference(CType type) {
    if (type instanceof CPointerType) {
      CPointerType pointerType = (CPointerType) type;
      return (findDereference(pointerType.getType()) + 1);
    } else if (type instanceof CArrayType) {
      CArrayType arrayType = (CArrayType) type;
      return (findDereference(arrayType.getType()) + 1);
    } else if (type instanceof CTypedefType) {
      return findDereference(((CTypedefType)type).getRealType());
    } else {
      return 0;
    }
  }

  private AbstractIdentifier createId(CExpression expression, int dereference) {
    IdentifierCreator idCreator = new IdentifierCreator(getFunctionName());
    AbstractIdentifier id = idCreator.createIdentifier(expression, dereference);
    if (id instanceof GlobalVariableIdentifier || id instanceof LocalVariableIdentifier) {
      id = ((SingleIdentifier)id).getGeneralId();
    }
    return id;
  }

  private void assign(LocalState pSuccessor, CExpression left, CRightHandSide right) {

    int leftDereference = findDereference(left.getExpressionType());

    /* If we assign a = b, we should set *a <-> *b and **a <-> **b
     */
    while (leftDereference > 0) {
      AbstractIdentifier leftId = createId(left, leftDereference);

      if (right instanceof CExpression && !(leftId instanceof ConstantIdentifier)) {
        /* Difference in leftDereference and right one appears in very specific cases, like
         * 'int* t = 0' and 'int* t[]; void* b; b = malloc(..); t = b;'
         * Therefore, we use left dereference as main one
         */
        AbstractIdentifier rightId = createId((CExpression)right, leftDereference);
        if (leftId.isGlobal()) {
          if (!(rightId instanceof ConstantIdentifier)) {
            //Variable is global, not memory location!
            //So, we should set the type of 'right' to global
            pSuccessor.set(rightId, DataType.GLOBAL);
          }
        } else {
          DataType type = pSuccessor.getType(rightId);
          pSuccessor.set(leftId, type);
        }
      } else if (right instanceof CFunctionCallExpression) {

        if (!handleSpecialFunction(pSuccessor, leftId, (CFunctionCallExpression) right)) {
          /* unknown function
           * It is important to reset the value
           */
          pSuccessor.set(leftId, null);
        }
      }
      leftDereference--;
    }
  }

  private boolean isAllocatedFunction(String funcName) {
    if (allocate.contains(funcName)) {
      return true;
    }
    for (String pattern : allocatePattern) {
      if (funcName.contains(pattern)) {
        return true;
      }
    }
    return false;
  }
}
