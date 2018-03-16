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

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.local.LocalState.DataType;
import org.sosy_lab.cpachecker.exceptions.HandleCodeException;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.ConstantIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.IdentifierCreator;
import org.sosy_lab.cpachecker.util.identifiers.LocalVariableIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.ReturnIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.StructureIdentifier;

@Options(prefix = "cpa.local")
public class LocalTransferRelation
    extends ForwardingTransferRelation<LocalState, LocalState, Precision> {

  @Option(
    name = "allocatefunctions",
    description = "functions, which allocate new free memory",
    secure = true
  )
  private Set<String> allocate;

  // Use it carefully: just alloc is not enough, because EMG generates
  // ldv_random_allocationless_scenario_callback_*
  @Option(
    name = "allocateFunctionPattern",
    description = "functions, which allocate new free memory",
    secure = true
  )
  private Set<String> allocatePattern = Sets.newHashSet();

  @Option(
    name = "conservativefunctions",
    description = "functions, which do not change sharedness of parameters",
    secure = true
  )
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
  protected LocalState handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption) {
    // Do not try to remove clone() - leads to problems in BAM's cache
    return state.copy();
  }

  @Override
  protected LocalState handleReturnStatementEdge(CReturnStatementEdge cfaEdge)
      throws HandleCodeException {
    // TODO is it necessary to clone every time?
    // Guess, it is possible to return the same state (we add only in cloned states)
    Optional<CExpression> returnExpression = cfaEdge.getExpression();
    LocalState newState = state.copy();
    if (returnExpression.isPresent()) {

      int potentialDereference = findDereference(returnExpression.get().getExpressionType());
      ReturnIdentifier id = ReturnIdentifier.getInstance(0);
      assign(newState, id, potentialDereference, returnExpression.get());
    }
    return newState;
  }

  @Override
  protected LocalState handleFunctionReturnEdge(
      CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall,
      CFunctionCall summaryExpr,
      String callerFunctionName)
      throws HandleCodeException {
    CFunctionCall exprOnSummary = cfaEdge.getSummaryEdge().getExpression();
    LocalState newElement = state.getClonedPreviousState();

    if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement assignExp =
          ((CFunctionCallAssignmentStatement) exprOnSummary);
      // Need to prepare id as left id is from caller function and the right id is from called
      // function
      int dereference = findDereference(assignExp.getLeftHandSide().getExpressionType());
      AbstractIdentifier leftId =
          createId(assignExp.getLeftHandSide(), 0, new IdentifierCreator(callerFunctionName));
      // return id is handled internally
      assign(newElement, leftId, dereference, assignExp.getRightHandSide());
    }
    // Update the outer parameters:
    CFunctionSummaryEdge sEdge = cfaEdge.getSummaryEdge();
    CFunctionEntryNode entry = sEdge.getFunctionEntry();
    String funcName = entry.getFunctionName();
    if (!isAllocatedFunction(funcName)) {
      List<String> paramNames = entry.getFunctionParameterNames();
      List<CExpression> arguments =
          sEdge.getExpression().getFunctionCallExpression().getParameterExpressions();
      List<CParameterDeclaration> parameterTypes = entry.getFunctionDefinition().getParameters();

      extractIdentifiers(arguments, paramNames, parameterTypes, funcName, getFunctionName())
          .forEach(p -> completeAssign(newElement, p.getFirst(), p.getThird(), p.getSecond()));
    }
    return newElement;
  }

  @Override
  protected LocalState handleFunctionCallEdge(
      CFunctionCallEdge cfaEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> parameterTypes,
      String calledFunctionName) {
    LocalState newState = LocalState.createNextLocalState(state);
    CFunctionEntryNode functionEntryNode = cfaEdge.getSuccessor();
    List<String> paramNames = functionEntryNode.getFunctionParameterNames();

    List<Triple<AbstractIdentifier, LocalVariableIdentifier, Integer>> toProcess =
        extractIdentifiers(
            arguments, paramNames, parameterTypes, getFunctionName(), calledFunctionName);
    // TODO Make it with config
    boolean isThreadCreate =
        cfaEdge.getSummaryEdge().getExpression() instanceof CThreadCreateStatement;

    for (Triple<AbstractIdentifier, LocalVariableIdentifier, Integer> pairId : toProcess) {
      if (isThreadCreate) {
        // Data became shared after thread creation
        completeSet(newState, pairId.getSecond(), pairId.getThird(), DataType.GLOBAL);
      } else {
        completeAssign(newState, pairId.getSecond(), pairId.getThird(), pairId.getFirst());
      }
    }

    return newState;
  }

  @Override
  protected LocalState handleStatementEdge(CStatementEdge cfaEdge, CStatement statement) {
    LocalState newState = state.copy();
    if (statement instanceof CAssignment) {
      // assignment like "a = b" or "a = foo()"
      CAssignment assignment = (CAssignment) statement;
      assign(newState, assignment.getLeftHandSide(), assignment.getRightHandSide());
    }
    return newState;
  }

  @Override
  protected LocalState handleDeclarationEdge(CDeclarationEdge declEdge, CDeclaration decl) {
    LocalState newState = state.copy();
    if (decl instanceof CVariableDeclaration) {
      CInitializer init = ((CVariableDeclaration) decl).getInitializer();

      int deref = findDereference(decl.getType());
      AbstractIdentifier id = IdentifierCreator.createIdentifier(decl, getFunctionName(), 0);
      CType type = decl.getType();
      if (type instanceof CComplexType) {
        if (type instanceof CElaboratedType) {
          type = ((CElaboratedType) type).getRealType();
        }
        if (type instanceof CCompositeType) {
          List<CCompositeTypeMemberDeclaration> members = ((CCompositeType) type).getMembers();
          for (CCompositeTypeMemberDeclaration m : members) {
            int typeDereference = findDereference(m.getType());
            if (typeDereference > 0) {
              // set it as local
              StructureIdentifier sId = new StructureIdentifier(m.getName(), m.getType(), 0, id);
              completeSet(newState, sId, typeDereference, DataType.LOCAL);
            }
          }
        }
      }
      if (init != null && init instanceof CInitializerExpression) {
        assign(newState, id, deref, ((CInitializerExpression) init).getExpression());
      } else if (!decl.isGlobal() && type instanceof CArrayType) {
        // Uninitialized arrays (int a[2]) are pointed to local memory
        completeSet(newState, id, deref, DataType.LOCAL);
      }
    }
    return newState;
  }

  private boolean handleSpecialFunction(
      LocalState pSuccessor,
      AbstractIdentifier leftId,
      int dereference,
      CFunctionCallExpression right) {
    String funcName = right.getFunctionNameExpression().toASTString();
    boolean isConservativeFunction =
        (conservationOfSharedness == null ? false : conservationOfSharedness.contains(funcName));

    if (isAllocatedFunction(funcName)) {
      Integer num = allocateInfo.get(funcName);
      if (num == null) {
        // Means that we use pattern
        num = 0;
      }
      if (num == 0) {
        // local data are returned from function
        pSuccessor.set(leftId, DataType.LOCAL);
      } else if (num > 0) {
        num = allocateInfo.get(funcName);
        if (num > 0) {
          // local data are transmitted, as function parameters. F.e., allocate(&pointer);
          CExpression parameter = right.getParameterExpressions().get(num - 1);
          // TODO How it works?
          AbstractIdentifier rightId = createId(parameter, dereference);
          pSuccessor.set(rightId, DataType.LOCAL);
        }
      }
      return true;

    } else if (isConservativeFunction) {

      List<CExpression> parameters = right.getParameterExpressions();
      // Usually it looks like 'priv = netdev_priv(dev)'
      // Other cases will be handled if they appear
      CExpression targetParam = parameters.get(0);
      // TODO How it works with *a = f(b) ?
      AbstractIdentifier paramId = createId(targetParam, dereference);
      assign(pSuccessor, leftId, paramId);
      return true;
    }
    return false;
  }

  private List<Triple<AbstractIdentifier, LocalVariableIdentifier, Integer>> extractIdentifiers(
      List<CExpression> arguments,
      List<String> paramNames,
      List<CParameterDeclaration> parameterTypes,
      String callerFunction,
      String calledFunction) {

    List<Triple<AbstractIdentifier, LocalVariableIdentifier, Integer>> result = new ArrayList<>();
    CExpression currentArgument;
    int dereference;
    for (int i = 0; i < arguments.size(); i++) {
      if (i >= paramNames.size()) {
        // function with unknown parameter size: printf(char* a, ...)
        // assign what we can
        break;
      }
      currentArgument = arguments.get(i);
      dereference = findDereference(parameterTypes.get(i).getType());

      LocalVariableIdentifier innerId =
          new LocalVariableIdentifier(
              paramNames.get(i), parameterTypes.get(i).getType(), calledFunction, 0);
      AbstractIdentifier outerId =
          createId(currentArgument, 0, new IdentifierCreator(callerFunction));
      result.add(Triple.of(outerId, innerId, dereference));
    }
    return result;
  }

  private AbstractIdentifier createId(
      CExpression expression, int dereference, IdentifierCreator idCreator) {
    return idCreator.createIdentifier(expression, dereference);
  }

  private AbstractIdentifier createId(CExpression expression, int dereference) {
    return createId(expression, dereference, new IdentifierCreator(getFunctionName()));
  }

  private void assign(LocalState pSuccessor, CExpression left, CRightHandSide right) {
    /* If we assign a = b, we should set *a <-> *b and **a <-> **b
     */
    AbstractIdentifier leftId = createId(left, 0);
    if (!(leftId instanceof ConstantIdentifier)) {
      int leftDereference = findDereference(left.getExpressionType());
      if (right instanceof CExpression) {
        assign(pSuccessor, leftId, leftDereference, (CExpression) right);

      } else if (right instanceof CFunctionCallExpression) {
        assign(pSuccessor, leftId, leftDereference, (CFunctionCallExpression) right);
      }
    }
  }

  private void assign(
      LocalState pSuccessor,
      AbstractIdentifier leftId,
      int leftDereference,
      CFunctionCallExpression right) {
    for (int i = 0; i <= leftDereference; i++, leftId = incrementDereference(leftId)) {
      if (!handleSpecialFunction(pSuccessor, leftId, i, right)) {
        AbstractIdentifier returnId = ReturnIdentifier.getInstance(i);
        assign(pSuccessor, leftId, returnId);
      }
    }
  }

  private void assign(
      LocalState pSuccessor, AbstractIdentifier leftId, int leftDereference, CExpression right) {
    /* Difference in leftDereference and right one appears in very specific cases, like
     * 'int* t = 0' and 'int* t[]; void* b; b = malloc(..); t = b;'
     * Therefore, we use left dereference as main one
     */
    AbstractIdentifier rightId = createId(right, 0);
    completeAssign(pSuccessor, leftId, leftDereference, rightId);
  }

  private void completeAssign(
      LocalState pSuccessor,
      AbstractIdentifier leftId,
      int dereference,
      AbstractIdentifier rightId) {
    for (int i = 0;
        i <= dereference;
        i++, leftId = incrementDereference(leftId), rightId = incrementDereference(rightId)) {
      assign(pSuccessor, leftId, rightId);
    }
  }

  private void completeSet(
      LocalState pSuccessor, AbstractIdentifier id, int dereference, DataType type) {
    for (int i = 0; i <= dereference; i++, id = incrementDereference(id)) {
      pSuccessor.set(id, type);
    }
  }

  private void assign(
      LocalState pSuccessor, AbstractIdentifier leftId, AbstractIdentifier rightId) {
    if (leftId.isGlobal()) {
      // Variable is global, not memory location!
      // So, we should set the type of 'right' to global
      pSuccessor.set(rightId, DataType.GLOBAL);
    } else {
      DataType type = getType(rightId);
      pSuccessor.set(leftId, type);
    }
  }

  private DataType getType(AbstractIdentifier id) {
    DataType type = state.getType(id);
    if (type == null && id instanceof ConstantIdentifier) {
      // return (struct myStruct *) 0; - consider the value as local
      return DataType.LOCAL;
    }
    if (type == null && !id.isDereferenced()) {
      // a = &b -> *a = b -> *a = local
      return DataType.LOCAL;
    }
    return type;
  }

  private AbstractIdentifier incrementDereference(AbstractIdentifier id) {
    return id.cloneWithDereference(id.getDereference() + 1);
  }

  public static int findDereference(CType type) {
    if (type instanceof CPointerType) {
      CPointerType pointerType = (CPointerType) type;
      return (findDereference(pointerType.getType()) + 1);
    } else if (type instanceof CArrayType) {
      CArrayType arrayType = (CArrayType) type;
      return (findDereference(arrayType.getType()) + 1);
    } else if (type instanceof CTypedefType) {
      return findDereference(((CTypedefType) type).getRealType());
    } else {
      return 0;
    }
  }

  private boolean isAllocatedFunction(String funcName) {
    if (allocate.contains(funcName)) {
      return true;
    }
    return from(allocatePattern).anyMatch(funcName::contains);
  }
}
