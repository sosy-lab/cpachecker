// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taint;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.APointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.BuiltinOverflowFunctions;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class TaintAnalysisTransferRelation
  extends ForwardingTransferRelation <TaintAnalysisState, TaintAnalysisState, VariableTrackingPrecision> {

  private final LogManagerWithoutDuplicates logger;
  private final MachineModel machineModel;

  public TaintAnalysisTransferRelation(
      LogManager pLogger,
      CFA pCfa) {
    // options = pOptions;
    machineModel = pCfa.getMachineModel();
    logger = new LogManagerWithoutDuplicates(pLogger);
    // stats = pStats;

    // if (pCfa.getVarClassification().isPresent()) {
    //   addressedVariables = pCfa.getVarClassification().orElseThrow().getAddressedVariables();
    //   booleanVariables = pCfa.getVarClassification().orElseThrow().getIntBoolVars();
    // } else {
    //   addressedVariables = ImmutableSet.of();
    //   booleanVariables   = ImmutableSet.of();
    // }

    // unknownValueHandler = pUnknownValueHandler;
    // constraintsStrengthenOperator = pConstraintsStrengthenOperator;
  }

  @Override
  protected Collection<TaintAnalysisState> postProcessing(TaintAnalysisState successor, CFAEdge edge) {
    // always return a new state (requirement for strengthening states with interpolants)
    if (successor != null) {
      successor = TaintAnalysisState.copyOf(successor);
    }

    return super.postProcessing(successor, edge);
  }


  @Override
  protected void setInfo(AbstractState pAbstractState,
      Precision pAbstractPrecision, CFAEdge pCfaEdge) {
    super.setInfo(pAbstractState, pAbstractPrecision, pCfaEdge);
  }

  @Override
  protected TaintAnalysisState handleFunctionCallEdge(FunctionCallEdge callEdge,
      List<? extends AExpression> arguments, List<? extends AParameterDeclaration> parameters,
      String calledFunctionName) throws UnrecognizedCodeException {
    TaintAnalysisState newElement = TaintAnalysisState.copyOf(state);

    assert (parameters.size() == arguments.size())
        || callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs();

    // visitor for getting the values of the actual parameters in caller function context
    // final ExpressionValueVisitor visitor = getVisitor();

    // get value of actual parameter in caller function context
    for (int i = 0; i < parameters.size(); i++) {
      AExpression exp = arguments.get(i);
      logger.log(Level.INFO, "YAS"+exp);

      // if (exp instanceof JExpression) {
      //   value = ((JExpression) exp).accept(visitor);
      // } else if (exp instanceof CExpression) {
      //   value = visitor.evaluate((CExpression) exp, (CType) parameters.get(i).getType());
      // } else {
      //   throw new AssertionError("Unknown expression: " + exp);
      // }

      AParameterDeclaration param = parameters.get(i);
      String paramName = param.getName();
      Type paramType = param.getType();

      MemoryLocation formalParamName = MemoryLocation.valueOf(calledFunctionName, paramName);

      // if (value.isUnknown()) {
      //   if (isMissingCExpressionInformation(visitor, exp)) {
      //     addMissingInformation(formalParamName, exp);
      //   }

      //   unknownValueHandler.handle(formalParamName, paramType, newElement, visitor);

      // } else {
      //   newElement.assignConstant(formalParamName, value, paramType);
      // }

      // visitor.reset();

    }

    return newElement;
  }

  @Override
  protected TaintAnalysisState handleBlankEdge(BlankEdge cfaEdge) {
    if (cfaEdge.getSuccessor() instanceof FunctionExitNode) {
      // clone state, because will be changed through removing all variables of current function's scope
      state = TaintAnalysisState.copyOf(state);
      // state.dropFrame(functionName);
    }

    return state;
  }

  @Override
  protected TaintAnalysisState handleReturnStatementEdge(AReturnStatementEdge returnEdge)
      throws UnrecognizedCodeException {

    // visitor must use the initial (previous) state, because there we have all information about variables
    // ExpressionValueVisitor evv = getVisitor();

    // clone state, because will be changed through removing all variables of current function's scope.
    // The assignment of the global 'state' is safe, because the 'old state'
    // is available in the visitor and is not used for further computation.
    state = TaintAnalysisState.copyOf(state);
    // logger.log(Level.INFO, "");
    // state.dropFrame(functionName);

    // AExpression expression = returnEdge.getExpression().orNull();
    // if (expression == null && returnEdge instanceof CReturnStatementEdge) {
    //   expression = CIntegerLiteralExpression.ZERO; // this is the default in C
    // }

    // final FunctionEntryNode functionEntryNode = returnEdge.getSuccessor().getEntryNode();

    // final com.google.common.base.Optional<? extends AVariableDeclaration>
    //     optionalReturnVarDeclaration = functionEntryNode.getReturnVariable();
    // MemoryLocation functionReturnVar = null;

    // if (optionalReturnVarDeclaration.isPresent()) {
    //   functionReturnVar = MemoryLocation.valueOf(optionalReturnVarDeclaration.get().getQualifiedName());
    // }

    // if (expression != null && functionReturnVar != null) {
    //   final Type functionReturnType = functionEntryNode.getFunctionDefinition().getType().getReturnType();

    //   return handleAssignmentToVariable(functionReturnVar,
    //       functionReturnType,
    //       expression,
    //       evv);
    // } else {
    //   return state;
    // }
    return state;
  }

  /**
   * Handles return from one function to another function.
   * @param functionReturnEdge return edge from a function to its call site
   * @return new abstract state
   */
  @Override
  protected TaintAnalysisState handleFunctionReturnEdge(FunctionReturnEdge functionReturnEdge,
      FunctionSummaryEdge summaryEdge, AFunctionCall exprOnSummary, String callerFunctionName)
    throws UnrecognizedCodeException {

    TaintAnalysisState newElement  = TaintAnalysisState.copyOf(state);

    // com.google.common.base.Optional<? extends AVariableDeclaration> returnVarName =
    //     functionReturnEdge.getFunctionEntry().getReturnVariable();
    // MemoryLocation functionReturnVar = null;
    // if (returnVarName.isPresent()) {
    //   functionReturnVar = MemoryLocation.valueOf(returnVarName.get().getQualifiedName());
    // }

    // // expression is an assignment operation, e.g. a = g(b);
    // if (exprOnSummary instanceof AFunctionCallAssignmentStatement) {
    //   AFunctionCallAssignmentStatement assignExp = ((AFunctionCallAssignmentStatement)exprOnSummary);
    //   AExpression op1 = assignExp.getLeftHandSide();

    //   // we expect left hand side of the expression to be a variable

    //   ExpressionValueVisitor v = getVisitor(newElement, callerFunctionName);

    //   Value newValue = null;
    //   boolean valueExists = returnVarName.isPresent() && state.contains(functionReturnVar);
    //   if (valueExists) {
    //     newValue = state.getValueFor(functionReturnVar);
    //   }

    //   // We have to handle Java arrays in a special way, because they are stored as ArrayValue
    //   // objects
    //   if (op1 instanceof JArraySubscriptExpression) {
    //     JArraySubscriptExpression arraySubscriptExpression = (JArraySubscriptExpression) op1;

    //     ArrayValue assignedArray = getInnerMostArray(arraySubscriptExpression);
    //     OptionalInt maybeIndex = getIndex(arraySubscriptExpression);

    //     if (maybeIndex.isPresent() && assignedArray != null && valueExists) {
    //       assignedArray.setValue(newValue, maybeIndex.orElseThrow());

    //     } else {
    //       assignUnknownValueToEnclosingInstanceOfArray(arraySubscriptExpression);
    //     }

    //   } else {
    //     // We can handle all types below "casually", so just get the memory location for the
    //     // left hand side and assign the function's return value

    //     Optional<MemoryLocation> memLoc = Optional.empty();

    //     // get memory location for left hand side
    //     if (op1 instanceof CLeftHandSide) {
    //       if (valueExists) {
    //         memLoc = getMemoryLocation((CLeftHandSide) op1, newValue, v);
    //       } else {
    //         memLoc = getMemoryLocation((CLeftHandSide) op1, UnknownValue.getInstance(), v);
    //       }

    //     } else if (op1 instanceof AIdExpression) {
    //       if (op1 instanceof JIdExpression && isDynamicField((JIdExpression)op1)
    //           && valueExists) {
    //         missingScopedFieldName = true;
    //         notScopedField = (JIdExpression)op1;
    //         notScopedFieldValue = newValue;
    //       } else {
    //         String op1QualifiedName = ((AIdExpression)op1).getDeclaration().getQualifiedName();
    //         memLoc = Optional.of(MemoryLocation.valueOf(op1QualifiedName));
    //       }

    //     } else if (op1 instanceof APointerExpression) {
    //       // a* = b(); TODO: for now, nothing is done here, but cloning the current element

    //     } else {
    //       throw new UnrecognizedCodeException("on function return", summaryEdge, op1);
    //     }

    //     // assign the value if a memory location was successfully computed
    //     if (memLoc.isPresent()) {
    //       if (!valueExists) {
    //         unknownValueHandler.handle(
    //             memLoc.orElseThrow(), op1.getExpressionType(), newElement, v);

    //       } else {
    //         newElement.assignConstant(
    //             memLoc.orElseThrow(), newValue, state.getTypeForMemoryLocation(functionReturnVar));
    //       }
    //     }
    //   }
    // }

    // if (returnVarName.isPresent()) {
    //   newElement.forget(functionReturnVar);
    // }

    return newElement;
  }

  // private Optional<MemoryLocation> getMemoryLocation(
  //     final CLeftHandSide pExpression,
  //     final Value pRightHandSideValue,
  //     final ExpressionValueVisitor pValueVisitor)
  //     throws UnrecognizedCodeException {

  //   MemoryLocation assignedVarName = pValueVisitor.evaluateMemoryLocation(pExpression);

  //   if (assignedVarName == null) {
  //     if (pValueVisitor.hasMissingPointer()) {
  //       addMissingInformation(pExpression, pRightHandSideValue);
  //     }
  //     return Optional.empty();

  //   } else {
  //     return Optional.of(assignedVarName);
  //   }
  // }

  // private boolean isDynamicField(JIdExpression pIdentifier) {
  //   final JSimpleDeclaration declaration = pIdentifier.getDeclaration();

  //   return (declaration instanceof JFieldDeclaration)
  //       && !((JFieldDeclaration) declaration).isStatic();
  // }

  // private OptionalInt getIndex(JArraySubscriptExpression pExpression) {
  //   final ExpressionValueVisitor evv = getVisitor();
  //   final Value indexValue = pExpression.getSubscriptExpression().accept(evv);

  //   if (indexValue.isUnknown()) {
  //     return OptionalInt.empty();
  //   } else {
  //     return OptionalInt.of((int) ((NumericValue) indexValue).longValue());
  //   }
  // }

  @Override
  protected TaintAnalysisState handleFunctionSummaryEdge(CFunctionSummaryEdge cfaEdge) throws CPATransferException {
    TaintAnalysisState newState = TaintAnalysisState.copyOf(state);
    // AFunctionCall functionCall  = cfaEdge.getExpression();

    // if (functionCall instanceof AFunctionCallAssignmentStatement) {
    //   AFunctionCallAssignmentStatement assignment = ((AFunctionCallAssignmentStatement)functionCall);
    //   AExpression leftHandSide = assignment.getLeftHandSide();

    //   if (leftHandSide instanceof CLeftHandSide) {
    //     MemoryLocation assignedMemoryLocation = getVisitor().evaluateMemoryLocation((CLeftHandSide) leftHandSide);

    //     if (newState.contains(assignedMemoryLocation)) {
    //       newState.forget(assignedMemoryLocation);
    //     }
    //   }
    // }

    return newState;
  }

  @Override
  protected TaintAnalysisState handleAssumption(
      AssumeEdge cfaEdge, AExpression expression, boolean truthValue)
      throws UnrecognizedCodeException {
    return handleAssumption(expression, truthValue);
  }

  private TaintAnalysisState handleAssumption(AExpression expression, boolean truthValue)
      throws UnrecognizedCodeException {
    return null;
    // Pair<AExpression, Boolean> simplifiedExpression = simplifyAssumption(expression, truthValue);
    // expression = simplifiedExpression.getFirst();
    // truthValue = simplifiedExpression.getSecond();

    // final ExpressionValueVisitor evv = getVisitor();
    // final Type booleanType = getBooleanType(expression);

    // // get the value of the expression (either true[1L], false[0L], or unknown[null])
    // Value value = getExpressionValue(expression, booleanType, evv);

    // if (value.isExplicitlyKnown() && stats != null) {
    //   stats.incrementDeterministicAssumptions();
    // }

    // if (!value.isExplicitlyKnown()) {
    //   TaintAnalysisState element = TaintAnalysisState.copyOf(state);

    //   AssigningValueVisitor avv =
    //       new AssigningValueVisitor(
    //           element,
    //           truthValue,
    //           booleanVariables,
    //           functionName,
    //           state,
    //           machineModel,
    //           logger,
    //           options);

    //   if (expression instanceof JExpression && ! (expression instanceof CExpression)) {

    //     ((JExpression) expression).accept(avv);

    //     if (avv.hasMissingFieldAccessInformation()) {
    //       assert missingInformationRightJExpression != null;
    //       missingAssumeInformation = true;
    //     }

    //   } else {
    //     ((CExpression) expression).accept(avv);
    //   }

    //   if (isMissingCExpressionInformation(evv, expression)) {
    //     missingInformationList.add(new MissingInformation(truthValue, expression));
    //   }

    //   return element;

    // } else if (representsBoolean(value, truthValue)) {
    //   // we do not know more than before, and the assumption is fulfilled, so return a copy of the old state
    //   // we need to return a copy, otherwise precision adjustment might reset too much information, even on the original state
    //   return TaintAnalysisState.copyOf(state);

    // } else {
    //   // assumption not fulfilled
    //   return null;
    // }
  }

  // private Type getBooleanType(AExpression pExpression) {
  //   if (pExpression instanceof JExpression) {
  //     return JSimpleType.getBoolean();
  //   } else if (pExpression instanceof CExpression) {
  //     return CNumericTypes.INT;

  //   } else {
  //     throw new AssertionError("Unhandled expression type " + pExpression.getClass());
  //   }
  // }

  /*
   *  returns 'true' if the given value represents the specified boolean bool.
   *  A return of 'false' does not necessarily mean that the given value represents !bool,
   *  but only that it does not represent bool.
   *
   *  For example:
   *    * representsTrue(BooleanValue.valueOf(true), true)  = true
   *    * representsTrue(BooleanValue.valueOf(false), true) = false
   *  but:
   *    * representsTrue(NullValue.getInstance(), true)     = false
   *    * representsTrue(NullValue.getInstance(), false)    = false
   *
   */
  // private boolean representsBoolean(Value value, boolean bool) {
  //   if (value instanceof BooleanValue) {
  //     return ((BooleanValue) value).isTrue() == bool;

  //   } else if (value.isNumericValue()) {
  //     return value.equals(new NumericValue(bool ? 1L : 0L));

  //   } else {
  //     return false;
  //   }
  // }

  @Override
  protected TaintAnalysisState handleDeclarationEdge(
      ADeclarationEdge declarationEdge, ADeclaration declaration) throws UnrecognizedCodeException {

    if (!(declaration instanceof AVariableDeclaration)) { // || !isTrackedType(declaration.getType())) {
      // nothing interesting to see here, please move along
      return state;
    }

    TaintAnalysisState newElement = TaintAnalysisState.copyOf(state);
    AVariableDeclaration decl = (AVariableDeclaration) declaration;
    Type declarationType = decl.getType();

    // get the variable name in the declarator
    String varName = decl.getName();
    newElement.assignTaint(varName, false);
    String msg;
    msg = varName+" | tainted: false | Type: "+declarationType;

    // Value initialValue = getDefaultInitialValue(decl);

    // // get initializing statement
    AInitializer init = decl.getInitializer();

    // handle global variables
    if (decl.isGlobal()) {
      if (decl instanceof JFieldDeclaration && !((JFieldDeclaration) decl).isStatic()) {
        msg = msg + " | YAS";
      }
    }

    MemoryLocation memoryLocation;

    // assign initial value if necessary
    if (decl.isGlobal()) {
      msg = msg + " | Global: true";
      memoryLocation = MemoryLocation.valueOf(varName);
    } else {
      msg = msg + " | Global: false";
      memoryLocation = MemoryLocation.valueOf(functionName, varName);
    }

    // if (addressedVariables.contains(decl.getQualifiedName()) && declarationType instanceof CType) {
    //   TaintAnalysisState.addToBlacklist(memoryLocation);
    // }

    if (init instanceof AInitializerExpression) {
      AExpression exp = ((AInitializerExpression) init).getExpression();
      msg = msg + " | EXP: "+exp;
    }

    // if (isTrackedType(declarationType)) {
    //   if (missingFieldVariableObject) {
    //     fieldNameAndInitialValue = Pair.of(varName, initialValue);

    //   } else if (missingInformationRightJExpression != null) {
    //     missingInformationLeftJVariable = memoryLocation.getAsSimpleString();
    //   }
    // } else {
    //   // If variable not tracked, its Object is irrelevant
    //   missingFieldVariableObject = false;
    // }

    // if (initialValue.isUnknown()) {
    //   unknownValueHandler.handle(memoryLocation, declarationType, newElement, getVisitor());
    // } else {
    //   newElement.assignConstant(memoryLocation, initialValue, declarationType);
    // }
    logger.log(Level.INFO, msg);
    return newElement;
  }

  // private Value getDefaultInitialValue(AVariableDeclaration pDeclaration) {
  //   final boolean defaultBooleanValue = false;
  //   final long defaultNumericValue = 0;

  //   if (pDeclaration.isGlobal()) {
  //     Type declarationType = pDeclaration.getType();

  //     if (isComplexJavaType(declarationType)) {
  //       return NullValue.getInstance();

  //     } else if (declarationType instanceof JSimpleType) {
  //       JBasicType basicType = ((JSimpleType) declarationType).getType();

  //       switch (basicType) {
  //         case BOOLEAN:
  //           return BooleanValue.valueOf(defaultBooleanValue);
  //         case BYTE:
  //         case CHAR:
  //         case SHORT:
  //         case INT:
  //         case LONG:
  //         case FLOAT:
  //         case DOUBLE:
  //           return new NumericValue(defaultNumericValue);
  //         case UNSPECIFIED:
  //           return UnknownValue.getInstance();
  //         default:
  //           throw new AssertionError("Impossible type for declaration: " + basicType);
  //       }
  //     }
  //   }

  //   return UnknownValue.getInstance();
  // }

  // private boolean isComplexJavaType(Type pType) {
  //   return pType instanceof JClassOrInterfaceType
  //       || pType instanceof JArrayType;
  // }

  // private boolean isMissingCExpressionInformation(ExpressionValueVisitor pEvv,
  //     ARightHandSide pExp) {

  //   return pExp instanceof CExpression && pEvv.hasMissingPointer();
  // }

  @Override
  protected TaintAnalysisState handleStatementEdge(AStatementEdge cfaEdge, AStatement expression)
    throws UnrecognizedCodeException {

    TaintAnalysisState newElement = TaintAnalysisState.copyOf(state);

    String msg = "";

    if (expression instanceof CFunctionCall) {
      CFunctionCall functionCall = (CFunctionCall) expression;
      CFunctionCallExpression functionCallExp = functionCall.getFunctionCallExpression();
      CExpression fn = functionCallExp.getFunctionNameExpression();

      msg = msg + "functionCall: "+functionCall+" | functionCallExp: "+functionCallExp+" | fn: "+fn;

      if (fn instanceof CIdExpression) {
        String func = ((CIdExpression)fn).getName();
        msg = msg + " | func: "+func;
        if (expression instanceof CFunctionCallAssignmentStatement) {
          msg = msg + " | CFunctionCallAssignmentStatement";
          CFunctionCallAssignmentStatement pFunctionCallAssignment = (CFunctionCallAssignmentStatement)expression;
          final CLeftHandSide leftSide = pFunctionCallAssignment.getLeftHandSide();
          msg = msg + " | leftSide: " + leftSide;
          if(func.equals("getchar")) {
            newElement.change(leftSide.toString(), true);
          }
        } else if (BuiltinOverflowFunctions.isBuiltinOverflowFunction(func)) {
          if (!BuiltinOverflowFunctions.isFunctionWithoutSideEffect(func)) {
            throw new UnsupportedCodeException(func + " is unsupported for this analysis", null);
          }
        } else if (expression instanceof CFunctionCallAssignmentStatement) {
          msg = msg + " | CFunctionCallAssignmentStatement";
        
        } else if (expression instanceof CFunctionCallStatement) {
          msg = msg + " | CFunctionCallStatement";
          if(func.equals("printf")) {
            AFunctionCallStatement stm = (AFunctionCallStatement) expression;
            AFunctionCallExpression exp = stm.getFunctionCallExpression();
            AExpression param = exp.getParameterExpressions().get(0);
            if(state.getStatus(param.toString())) {
              newElement = TaintAnalysisState.copyOf(state, true, "Critical function 'printf' was called with a tainted parameter.");
            }
            msg = msg + " | "+param;
            
          }
        
        } else {
          msg = msg + " | else: "+expression.getClass();
        }
        // if(func.equals("free")) {
        //   state.change(var, tainted);
        //   return state;
        // }
        // if (UNSUPPORTED_FUNCTIONS.containsKey(func)) {
        //   throw new UnsupportedCodeException(UNSUPPORTED_FUNCTIONS.get(func), cfaEdge, fn);

        // } else if (func.equals("free")) {
        //   return handleCallToFree(functionCall);

        // } else if (BuiltinOverflowFunctions.isBuiltinOverflowFunction(func)) {
        //   if (!BuiltinOverflowFunctions.isFunctionWithoutSideEffect(func)) {
        //     throw new UnsupportedCodeException(func + " is unsupported for this analysis", null);
        //   }
        // } else if (expression instanceof CFunctionCallAssignmentStatement) {

        //   return handleFunctionAssignment((CFunctionCallAssignmentStatement) expression);
        // }
      }
    }

    // expression is a binary operation, e.g. a = b;

    else if (expression instanceof AAssignment) {
      newElement = handleAssignment((AAssignment)expression, cfaEdge);
      return newElement;

    } else if (expression instanceof AFunctionCallStatement) {
      msg = msg + " | AFunctionCallStatement";

    } else if (expression instanceof AExpressionStatement) {
      msg = msg + " | AExpressionStatement";

    } else {
      throw new UnrecognizedCodeException("Unknown statement", cfaEdge, expression);
    }
    logger.log(Level.INFO, msg);
    logger.log(Level.INFO, newElement.isTarget() + " | "+newElement.getViolatedProperties());
    return newElement;
  }

  private TaintAnalysisState handleAssignment(AAssignment assignExpression, CFAEdge cfaEdge)
      throws UnrecognizedCodeException {
    TaintAnalysisState newElement = TaintAnalysisState.copyOf(state);
    String msg = "";
    
    AExpression op1    = assignExpression.getLeftHandSide();
    ARightHandSide op2 = assignExpression.getRightHandSide();
    msg = msg + op1 + " = "+ op2 +" | op1: "+op1 + " op1GetType: op2: "+op2 + " "+op2.getExpressionType() + " "+op2.getClass();

    if (op1 instanceof AIdExpression) {
      // a = ...
      msg = msg + "\nAIdExpression";
      if(op2 instanceof ALiteralExpression) {
        msg = msg + " | ALiteralExpression";
        newElement.change(op1.toString(), false);
        return newElement;
      } else {
        msg = msg + " | Status op2: "+state.getStatus(op2.toString());
        newElement.change(op1.toString(), newElement.getStatus(op2.toString()));
        msg = msg + " | Status op1: "+newElement.getStatus(op1.toString());
      } //if(op2 instanceof ALiteralExpression)
      // return handleAssignmentToVariable(memloc, op1.getExpressionType(), op2, getVisitor());
    } else if (op1 instanceof APointerExpression) {
      // *a = ...
      msg = msg + "\nAPointerExpression";
      // if (isRelevant(op1, op2)) {
      //   missingInformationList.add(new MissingInformation(op1, op2));
      // }

    } else if (op1 instanceof CFieldReference) {
      // ???
      msg = msg + "\nCFieldReference";

    } else if (op1 instanceof AArraySubscriptExpression) {
      // array cell
      msg = msg + " | AArraySubscriptExpression";
      if (op1 instanceof CArraySubscriptExpression) {
        msg = msg + " | CArraySubscriptExpression";
      } else if (op1 instanceof JArraySubscriptExpression) {
        msg = msg + " | JArraySubscriptExpression";
      }
    } else {
      throw new UnrecognizedCodeException(
          "left operand of assignment has to be a variable", cfaEdge, op1);
    }
    
    logger.log(Level.INFO, msg);
    return newElement; // the default return-value is the old state
  }

  // private boolean isTrackedType(Type pType) {
  //   return !(pType instanceof JType)
  //       || options.trackJavaArrayValues
  //       || !(pType instanceof JArrayType);
  // }

  private MemoryLocation getMemoryLocation(AIdExpression pIdExpression) {
    String varName = pIdExpression.getName();

    if (isGlobal(pIdExpression)) {
      return MemoryLocation.valueOf(varName);
    } else {
      return MemoryLocation.valueOf(functionName, varName);
    }
  }

  private boolean isRelevant(AExpression pOp1, ARightHandSide pOp2) {
    return pOp1 instanceof CExpression && pOp2 instanceof CExpression;
  }

  /** This method analyses the expression with the visitor and assigns the value to lParam.
   * The method returns a new state, that contains (a copy of) the old state and the new assignment. */
  // private TaintAnalysisState handleAssignmentToVariable(
  //     MemoryLocation assignedVar, final Type lType, ARightHandSide exp, ExpressionValueVisitor visitor)
  //         throws UnrecognizedCodeException {
  //   // here we clone the state, because we get new information or must forget it.
  //   TaintAnalysisState newElement = TaintAnalysisState.copyOf(state);
  //   handleAssignmentToVariable(newElement, assignedVar, lType, exp, visitor);
  //   return newElement;
  // }

  /** This method analyses the expression with the visitor and assigns the value to lParam
   *  to the given value Analysis state.
   */
//   private void handleAssignmentToVariable(TaintAnalysisState newElement,
//       MemoryLocation assignedVar, final Type lType, ARightHandSide exp, ExpressionValueVisitor visitor)
//       throws UnrecognizedCodeException {

//     // c structs have to be handled seperatly, because we do not have a value object representing structs
//     if (lType instanceof CType) {
//       CType canonicaltype = ((CType) lType).getCanonicalType();
//       if (canonicaltype instanceof CCompositeType
//           && ((CCompositeType) canonicaltype).getKind() == ComplexTypeKind.STRUCT
//           && exp instanceof CLeftHandSide) {
//         handleAssignmentToStruct(newElement, assignedVar, (CCompositeType) canonicaltype, (CExpression) exp, visitor);
//         return;
//       }
//     }

//     Value value;
//     if (exp instanceof JRightHandSide) {
//        value = visitor.evaluate((JRightHandSide) exp, (JType) lType);
//     } else if (exp instanceof CRightHandSide) {
//        value = visitor.evaluate((CRightHandSide) exp, (CType) lType);
//     } else {
//       throw new AssertionError("unknown righthandside-expression: " + exp);
//     }

//     if (visitor.hasMissingPointer()) {
//       assert !value.isExplicitlyKnown();
//     }

//     if (isMissingCExpressionInformation(visitor, exp)) {
//       // Evaluation
//       addMissingInformation(assignedVar, exp);
//     }

//     if (visitor.hasMissingFieldAccessInformation()) {
//       // This may happen if an object of class is created which could not be parsed,
//       // In  such a case, forget about it
//       if (!value.isUnknown()) {
//         newElement.forget(assignedVar);
//         return;
//       } else {
//         missingInformationRightJExpression = (JRightHandSide) exp;
//         if (!missingScopedFieldName) {
//           missingInformationLeftJVariable = assignedVar.getAsSimpleString();
//         }
//       }
//     }

//     if (missingScopedFieldName) {
//       notScopedFieldValue = value;
//     } else {
//       // some heuristics to clear wrong information
//       // when a struct or a pointer to one is assigned
//       // TODO not implemented in SMG version of ValueAnalysisCPA
// //      newElement.forgetAllWithPrefix(assignedVar + ".");
// //      newElement.forgetAllWithPrefix(assignedVar + "->");

//       // if there is no information left to evaluate but the value is unknown, we assign a symbolic
//       // identifier to keep track of the variable.
//       if (value.isUnknown()) {
//         unknownValueHandler.handle(assignedVar, lType, newElement, visitor);

//       } else {
//         newElement.assignConstant(assignedVar, value, lType);
//       }
//     }
//   }

  /**
   *
   * This method transforms the assignment of the struct into assignments of its respective
   * field references and assigns them to the given value state.
   *
   */
  // private void handleAssignmentToStruct(TaintAnalysisState pNewElement,
  //     MemoryLocation pAssignedVar,
  //     CCompositeType pLType, CExpression pExp,
  //     ExpressionValueVisitor pVisitor) throws UnrecognizedCodeException {

  //   long offset = 0L;
  //   for (CCompositeType.CCompositeTypeMemberDeclaration memberType : pLType.getMembers()) {
  //     MemoryLocation assignedField = createFieldMemoryLocation(pAssignedVar, offset);

  //     CExpression owner = pExp;

  //     CExpression fieldReference =
  //         new CFieldReference(pExp.getFileLocation(), memberType.getType(), memberType.getName(), owner, false);
  //     handleAssignmentToVariable(pNewElement, assignedField, memberType.getType(), fieldReference, pVisitor);

  //     offset = offset + machineModel.getSizeof(memberType.getType()).longValueExact();
  //   }
  // }

  private MemoryLocation createFieldMemoryLocation(MemoryLocation pStruct, long pOffset) {

    long baseOffset = pStruct.isReference() ? pStruct.getOffset() : 0;

    if (pStruct.isOnFunctionStack()) {
      return MemoryLocation.valueOf(
          pStruct.getFunctionName(), pStruct.getIdentifier(), baseOffset + pOffset);
    } else {
      return MemoryLocation.valueOf(pStruct.getIdentifier(), baseOffset + pOffset);
    }
  }

  // private void addMissingInformation(MemoryLocation pMemLoc, ARightHandSide pExp) {
  //   if (pExp instanceof CExpression) {

  //     missingInformationList.add(new MissingInformation(pMemLoc,
  //         (CExpression) pExp));
  //   }
  // }

  // private void addMissingInformation(CLeftHandSide pOp1, Value pValue) {
  //   missingInformationList.add(new MissingInformation(pOp1, pValue));

  // }

  /**
   * Returns the {@link ArrayValue} object that represents the innermost array of the given
   * {@link JArraySubscriptExpression}.
   *
   * @param pArraySubscriptExpression the subscript expression to get the inner most array of
   * @return <code>null</code> if the complete array or a part significant for the given array
   *    subscript expression is unknown, the <code>ArrayValue</code> representing the innermost
   *    array, otherwise
   */
  // private @Nullable ArrayValue getInnerMostArray(JArraySubscriptExpression pArraySubscriptExpression) {
  //   JExpression arrayExpression = pArraySubscriptExpression.getArrayExpression();

  //   if (arrayExpression instanceof JIdExpression) {
  //     JSimpleDeclaration arrayDeclaration = ((JIdExpression) arrayExpression).getDeclaration();

  //     if (arrayDeclaration != null) {
  //       MemoryLocation idName = MemoryLocation.valueOf(arrayDeclaration.getQualifiedName());

  //       if (state.contains(idName)) {
  //         Value idValue = state.getValueFor(idName);
  //         if (idValue.isExplicitlyKnown()) {
  //           return (ArrayValue) idValue;
  //         }
  //       }
  //     }

  //     return null;
  //   } else {
  //     final JArraySubscriptExpression arraySubscriptExpression = (JArraySubscriptExpression) arrayExpression;
  //     // the array enclosing the array specified in the given array subscript expression
  //     ArrayValue enclosingArray = getInnerMostArray(arraySubscriptExpression);

  //     OptionalInt maybeIndex = getIndex(arraySubscriptExpression);
  //     int index;

  //     if (maybeIndex.isPresent() && enclosingArray != null) {

  //       index = maybeIndex.orElseThrow();

  //     } else {
  //       return null;
  //     }

  //     if (index >= enclosingArray.getArraySize() || index < 0) {
  //       return null;
  //     }

  //     return (ArrayValue) enclosingArray.getValueAt(index);
  //   }
  // }

  // private void handleAssignmentToArray(ArrayValue pArray, int index, ARightHandSide exp) {
  //   assert exp instanceof JExpression;

  //   pArray.setValue(((JExpression) exp).accept(getVisitor()), index);
  // }

  // private void assignUnknownValueToEnclosingInstanceOfArray(JArraySubscriptExpression pArraySubscriptExpression) {

  //   JExpression enclosingExpression = pArraySubscriptExpression.getArrayExpression();

  //   if (enclosingExpression instanceof JIdExpression) {
  //     JIdExpression idExpression = (JIdExpression) enclosingExpression;
  //     MemoryLocation memLoc = getMemoryLocation(idExpression);
  //     Value unknownValue = UnknownValue.getInstance();

  //     state.assignConstant(memLoc, unknownValue, JSimpleType.getUnspecified());

  //   } else {
  //     JArraySubscriptExpression enclosingSubscriptExpression = (JArraySubscriptExpression) enclosingExpression;
  //     ArrayValue enclosingArray = getInnerMostArray(enclosingSubscriptExpression);
  //     OptionalInt maybeIndex = getIndex(enclosingSubscriptExpression);

  //     if (maybeIndex.isPresent() && enclosingArray != null) {
  //       enclosingArray.setValue(UnknownValue.getInstance(), maybeIndex.orElseThrow());

  //     }
  //     // if the index of unknown array in the enclosing array is also unknown, we assign unknown at this array's
  //     // position in the enclosing array
  //     else {
  //       assignUnknownValueToEnclosingInstanceOfArray(enclosingSubscriptExpression);
  //     }
  //   }
  // }

  // private class  FieldAccessExpressionValueVisitor extends ExpressionValueVisitor {
  //   private final RTTState jortState;

  //   public FieldAccessExpressionValueVisitor(RTTState pJortState, TaintAnalysisState pState) {
  //     super(pState, functionName, machineModel, logger);
  //     jortState = pJortState;
  //   }

  //   @Override
  //   public Value visit(JBinaryExpression binaryExpression) {
  //     return super.visit(binaryExpression);
  //   }

  //   private String handleIdExpression(JIdExpression expr) {

  //     JSimpleDeclaration decl = expr.getDeclaration();

  //     if (decl == null) {
  //       return null;
  //     }

  //     NameProvider nameProvider = NameProvider.getInstance();
  //     String objectScope = nameProvider.getObjectScope(jortState, functionName, expr);

  //     return nameProvider.getScopedVariableName(decl, functionName, objectScope);
  //   }

  //   @Override
  //   public Value visit(JIdExpression idExp) {

  //     MemoryLocation varName = MemoryLocation.valueOf(handleIdExpression(idExp));

  //     if (readableState.contains(varName)) {
  //       return readableState.getValueFor(varName);
  //     } else {
  //       return Value.UnknownValue.getInstance();
  //     }
  //   }
  // }

  // private Value getExpressionValue(
  //     AExpression expression, final Type type, ExpressionValueVisitor evv)
  //     throws UnrecognizedCodeException {
  //   if (!isTrackedType(type)) {
  //     return UnknownValue.getInstance();
  //   }

  //   if (expression instanceof JRightHandSide) {

  //     final Value value = evv.evaluate((JRightHandSide) expression, (JType) type);

  //     if (evv.hasMissingFieldAccessInformation()) {
  //       missingInformationRightJExpression = (JRightHandSide) expression;
  //       return Value.UnknownValue.getInstance();
  //     } else {
  //       return value;
  //     }
  //   } else if (expression instanceof CRightHandSide) {
  //     return evv.evaluate((CRightHandSide) expression, (CType) type);
  //   } else {
  //     throw new AssertionError("unhandled righthandside-expression: " + expression);
  //   }
  // }

  // @Override
  // public Collection<? extends AbstractState> strengthen(
  //     AbstractState pElement,
  //     Iterable<AbstractState> pElements,
  //     CFAEdge pCfaEdge,
  //     Precision pPrecision)
  //     throws CPATransferException {
  //   assert pElement instanceof TaintAnalysisState;

  //   List<TaintAnalysisState> toStrengthen = new ArrayList<>();
  //   List<TaintAnalysisState> result = new ArrayList<>();
  //   toStrengthen.add((TaintAnalysisState) pElement);
  //   result.add((TaintAnalysisState) pElement);

  //   for (AbstractState ae : pElements) {
  //     if (ae instanceof RTTState) {
  //       result.clear();
  //       for (TaintAnalysisState stateToStrengthen : toStrengthen) {
  //         super.setInfo(pElement, pPrecision, pCfaEdge);
  //         Collection<TaintAnalysisState> ret = strengthen((RTTState)ae, pCfaEdge);
  //         if (ret == null) {
  //           result.add(stateToStrengthen);
  //         } else {
  //           result.addAll(ret);
  //         }
  //       }
  //       toStrengthen.clear();
  //       toStrengthen.addAll(result);
  //     } else if (ae instanceof AbstractStateWithAssumptions) {
  //       result.clear();
  //       for (TaintAnalysisState stateToStrengthen : toStrengthen) {
  //         super.setInfo(pElement, pPrecision, pCfaEdge);
  //         AbstractStateWithAssumptions stateWithAssumptions = (AbstractStateWithAssumptions) ae;
  //         result.addAll(
  //             strengthenWithAssumptions(stateWithAssumptions, stateToStrengthen, pCfaEdge));
  //       }
  //       toStrengthen.clear();
  //       toStrengthen.addAll(result);
  //     } else if (ae instanceof ThreadingState) {
  //       result.clear();
  //       for (TaintAnalysisState stateToStrengthen : toStrengthen) {
  //         super.setInfo(pElement, pPrecision, pCfaEdge);
  //         result.add(strengthenWithThreads((ThreadingState) ae, stateToStrengthen));
  //       }
  //       toStrengthen.clear();
  //       toStrengthen.addAll(result);
  //     } else if (ae instanceof ConstraintsState) {
  //       result.clear();

  //       for (TaintAnalysisState stateToStrengthen : toStrengthen) {
  //         super.setInfo(pElement, pPrecision, pCfaEdge);
  //         Collection<TaintAnalysisState> ret =
  //             constraintsStrengthenOperator.strengthen((TaintAnalysisState) pElement, (ConstraintsState) ae, pCfaEdge);

  //         if (ret == null) {
  //           result.add(stateToStrengthen);
  //         } else {
  //           result.addAll(ret);
  //         }
  //       }
  //       toStrengthen.clear();
  //       toStrengthen.addAll(result);
  //     } else if (ae instanceof PointerState) {

  //       CFAEdge edge = pCfaEdge;

  //       ARightHandSide rightHandSide = CFAEdgeUtils.getRightHandSide(edge);
  //       ALeftHandSide leftHandSide = CFAEdgeUtils.getLeftHandSide(edge);
  //       Type leftHandType = CFAEdgeUtils.getLeftHandType(edge);
  //       String leftHandVariable = CFAEdgeUtils.getLeftHandVariable(edge);
  //       PointerState pointerState = (PointerState) ae;

  //       result.clear();

  //       for (TaintAnalysisState stateToStrengthen : toStrengthen) {
  //         super.setInfo(pElement, pPrecision, pCfaEdge);
  //         TaintAnalysisState newState =
  //             strengthenWithPointerInformation(stateToStrengthen, pointerState, rightHandSide, leftHandType, leftHandSide, leftHandVariable, UnknownValue.getInstance());

  //         newState = handleModf(rightHandSide, pointerState, newState);

  //         result.add(newState);
  //       }
  //       toStrengthen.clear();
  //       toStrengthen.addAll(result);
  //     }

  //   }

  //   // Do post processing
  //   final Collection<AbstractState> postProcessedResult = new ArrayList<>(result.size());
  //   for (TaintAnalysisState rawResult : result) {
  //     // The original state has already been post-processed
  //     if (rawResult == pElement) {
  //       postProcessedResult.add(pElement);
  //     } else {
  //       postProcessedResult.addAll(postProcessing(rawResult, pCfaEdge));
  //     }
  //   }

  //   super.resetInfo();
  //   oldState = null;

  //   return postProcessedResult;
  // }

  /**
   * Handle a special built-in library function that required pointer-alias handling while computing
   * a floating-point operation.
   *
   * @param pRightHandSide the right-hand side of an assignment edge.
   * @param pPointerState the current pointer-alias information.
   * @param pState the state to strengthen.
   * @return the strengthened state.
   * @throws UnrecognizedCodeException if the C code involved is not recognized.
   */
  // private TaintAnalysisState handleModf(
  //     ARightHandSide pRightHandSide, PointerState pPointerState, TaintAnalysisState pState)
  //     throws UnrecognizedCodeException, AssertionError {
  //   TaintAnalysisState newState = pState;
  //   if (pRightHandSide instanceof AFunctionCallExpression) {
  //     AFunctionCallExpression functionCallExpression = (AFunctionCallExpression) pRightHandSide;
  //     AExpression nameExpressionOfCalledFunc = functionCallExpression.getFunctionNameExpression();
  //     if (nameExpressionOfCalledFunc instanceof AIdExpression) {
  //       String nameOfCalledFunc = ((AIdExpression) nameExpressionOfCalledFunc).getName();
  //       if (BuiltinFloatFunctions.matchesModf(nameOfCalledFunc)) {
  //         List<? extends AExpression> parameters = functionCallExpression.getParameterExpressions();
  //         if (parameters.size() == 2 && parameters.get(1) instanceof CExpression) {
  //           AExpression exp = parameters.get(0);
  //           CExpression targetPointer = (CExpression) parameters.get(1);
  //           CLeftHandSide target =
  //               new CPointerExpression(
  //                   targetPointer.getFileLocation(),
  //                   targetPointer.getExpressionType(),
  //                   targetPointer);
  //           ExpressionValueVisitor evv = getVisitor();
  //           Value value;
  //           if (exp instanceof JRightHandSide) {
  //             value = evv.evaluate((JRightHandSide) exp, (JType) exp.getExpressionType());
  //           } else if (exp instanceof CRightHandSide) {
  //             value = evv.evaluate((CRightHandSide) exp, (CType) exp.getExpressionType());
  //           } else {
  //             throw new AssertionError("unknown righthandside-expression: " + exp);
  //           }
  //           if (value.isExplicitlyKnown()) {
  //             NumericValue numericValue = value.asNumericValue();
  //             CSimpleType paramType =
  //                 BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(nameOfCalledFunc);
  //             if (ImmutableList.of(CBasicType.FLOAT, CBasicType.DOUBLE)
  //                 .contains(paramType.getType())) {
  //               final BigDecimal integralPartValue;
  //               switch (paramType.getType()) {
  //                 case FLOAT:
  //                   integralPartValue = BigDecimal.valueOf((float) ((long) numericValue.floatValue()));
  //                   break;
  //                 case DOUBLE:
  //                   integralPartValue = BigDecimal.valueOf((double) ((long) numericValue.doubleValue()));
  //                   break;
  //                 default:
  //                   throw new AssertionError("Unsupported float type: " + paramType);
  //               }
  //               CFloatLiteralExpression integralPart =
  //                   new CFloatLiteralExpression(
  //                       functionCallExpression.getFileLocation(),
  //                       paramType,
  //                       integralPartValue);
  //               newState =
  //                   strengthenWithPointerInformation(
  //                       newState,
  //                       pPointerState,
  //                       integralPart,
  //                       target.getExpressionType(),
  //                       target,
  //                       null,
  //                       new NumericValue(integralPartValue));
  //             }
  //           }
  //         }
  //       }
  //     }
  //   }
  //   return newState;
  // }

  // private TaintAnalysisState strengthenWithPointerInformation(
  //     TaintAnalysisState pValueState,
  //     PointerState pPointerInfo,
  //     ARightHandSide pRightHandSide,
  //     Type pTargetType,
  //     ALeftHandSide pLeftHandSide,
  //     String pLeftHandVariable,
  //     Value pValue)
  //     throws UnrecognizedCodeException {

  //   TaintAnalysisState newState = pValueState;

  //   Value value = pValue;
  //   MemoryLocation target = null;
  //   if (pLeftHandVariable != null) {
  //     target = MemoryLocation.valueOf(pLeftHandVariable);
  //   }
  //   Type type = pTargetType;
  //   boolean shouldAssign = false;

  //   if (target == null && pLeftHandSide instanceof CPointerExpression) {
  //     CPointerExpression pointerExpression = (CPointerExpression) pLeftHandSide;

  //     LocationSet directLocation =
  //         PointerTransferRelation.asLocations(pointerExpression, pPointerInfo);

  //     if (!(directLocation instanceof ExplicitLocationSet)) {
  //       CExpression addressExpression = pointerExpression.getOperand();
  //       LocationSet indirectLocation =
  //           PointerTransferRelation.asLocations(addressExpression, pPointerInfo);
  //       if (indirectLocation instanceof ExplicitLocationSet) {
  //         ExplicitLocationSet explicitSet = (ExplicitLocationSet) indirectLocation;
  //         if (explicitSet.getSize() == 1) {
  //           MemoryLocation variable = explicitSet.iterator().next();
  //           directLocation = pPointerInfo.getPointsToSet(variable);
  //         }
  //       }
  //     }
  //     if (directLocation instanceof ExplicitLocationSet) {
  //       ExplicitLocationSet explicitDirectLocation = (ExplicitLocationSet) directLocation;
  //       Iterator<MemoryLocation> locationIterator = explicitDirectLocation.iterator();
  //       MemoryLocation otherVariable = locationIterator.next();
  //       if (!locationIterator.hasNext()) {
  //         target = otherVariable;
  //         if (type == null && pValueState.contains(target)) {
  //           type = pValueState.getTypeForMemoryLocation(target);
  //         }
  //         shouldAssign = true;
  //       }
  //     }

  //   }

  //   if (!value.isExplicitlyKnown() && pRightHandSide instanceof CPointerExpression) {
  //     if (target == null) {
  //       return pValueState;
  //     }

  //     CPointerExpression rhs = (CPointerExpression) pRightHandSide;
  //     CExpression addressExpression = rhs.getOperand();

  //     LocationSet fullSet = PointerTransferRelation.asLocations(addressExpression, pPointerInfo);

  //     if (fullSet instanceof ExplicitLocationSet) {
  //       ExplicitLocationSet explicitSet = (ExplicitLocationSet) fullSet;
  //       if (explicitSet.getSize() == 1) {
  //         MemoryLocation variable = explicitSet.iterator().next();
  //         CType variableType = rhs.getExpressionType().getCanonicalType();
  //         LocationSet pointsToSet = pPointerInfo.getPointsToSet(variable);

  //         if (pointsToSet instanceof ExplicitLocationSet) {
  //           ExplicitLocationSet explicitPointsToSet = (ExplicitLocationSet) pointsToSet;
  //           Iterator<MemoryLocation> pointsToIterator = explicitPointsToSet.iterator();
  //           MemoryLocation otherVariableLocation = pointsToIterator.next();
  //           if (!pointsToIterator.hasNext() && pValueState.contains(otherVariableLocation)) {

  //             ValueAndType valueAndType = pValueState.getValueAndTypeFor(otherVariableLocation);
  //             Type otherVariableType = valueAndType.getType();
  //             if (otherVariableType != null) {
  //               Value otherVariableValue = valueAndType.getValue();
  //               if (otherVariableValue != null) {
  //                 if (variableType.equals(otherVariableType)
  //                     || (variableType.equals(CNumericTypes.FLOAT)
  //                         && otherVariableType.equals(CNumericTypes.UNSIGNED_INT)
  //                         && otherVariableValue.isExplicitlyKnown()
  //                         && Long.valueOf(0)
  //                             .equals(otherVariableValue.asLong(CNumericTypes.UNSIGNED_INT)))) {
  //                   value = otherVariableValue;
  //                   shouldAssign = true;
  //                 }
  //               }
  //             }
  //           }
  //         }
  //       }
  //     }
  //   }

  //   if (target != null && type != null && shouldAssign) {
  //     newState = TaintAnalysisState.copyOf(pValueState);
  //     newState.assignConstant(target, value, type);
  //   }

  //   return newState;
  // }

  // private @NonNull Collection<TaintAnalysisState> strengthenWithAssumptions(
  //     AbstractStateWithAssumptions pStateWithAssumptions,
  //     TaintAnalysisState pState,
  //     CFAEdge pCfaEdge)
  //     throws CPATransferException {

  //   TaintAnalysisState newState = pState;

  //   for (AExpression assumption : pStateWithAssumptions.getAssumptions()) {
  //     newState = handleAssumption(assumption, true);

  //     if (newState == null) {
  //       break;
  //     } else {
  //       setInfo(newState, precision, pCfaEdge);
  //     }
  //   }

  //   if (newState == null) {
  //     return ImmutableList.of();
  //   } else {
  //     return Collections.singleton(newState);
  //   }
  // }

  // private @NonNull TaintAnalysisState strengthenWithThreads(
  //     ThreadingState pThreadingState, TaintAnalysisState pState) throws CPATransferException {
  //   final FunctionCallEdge function = pThreadingState.getEntryFunction();
  //   if (function == null) {
  //     return pState;
  //   }
  //   final FunctionEntryNode succ = function.getSuccessor();
  //   final String calledFunctionName = succ.getFunctionName();
  //   return handleFunctionCallEdge(
  //       function, function.getArguments(), succ.getFunctionParameters(), calledFunctionName);
  // }

  // private Collection<TaintAnalysisState> strengthen(RTTState rttState, CFAEdge edge) {

  //   TaintAnalysisState newElement = TaintAnalysisState.copyOf(oldState);

  //   if (missingFieldVariableObject) {
  //     newElement.assignConstant(getRTTScopedVariableName(
  //         fieldNameAndInitialValue.getFirst(),
  //         rttState.getKeywordThisUniqueObject()),
  //         fieldNameAndInitialValue.getSecond());

  //     missingFieldVariableObject = false;
  //     fieldNameAndInitialValue = null;
  //     return Collections.singleton(newElement);

  //   } else if (missingScopedFieldName) {

  //     newElement = handleNotScopedVariable(rttState, newElement);
  //     missingScopedFieldName = false;
  //     notScopedField = null;
  //     notScopedFieldValue = null;
  //     missingInformationRightJExpression = null;

  //     if (newElement != null) {
  //     return Collections.singleton(newElement);
  //     } else {
  //       return null;
  //     }
  //   } else if (missingAssumeInformation && missingInformationRightJExpression != null) {

  //     Value value = handleMissingInformationRightJExpression(rttState);

  //     missingAssumeInformation = false;
  //     missingInformationRightJExpression = null;

  //     boolean truthAssumption = ((AssumeEdge)edge).getTruthAssumption();
  //     if (value == null || !value.isExplicitlyKnown()) {
  //       return null;
  //     } else if (representsBoolean(value, truthAssumption)) {
  //       return Collections.singleton(newElement);
  //     } else {
  //       return new HashSet<>();
  //     }
  //   } else if (missingInformationRightJExpression != null) {

  //     Value value = handleMissingInformationRightJExpression(rttState);

  //     if (!value.isUnknown()) {
  //       newElement.assignConstant(missingInformationLeftJVariable, value);
  //       missingInformationRightJExpression = null;
  //       missingInformationLeftJVariable = null;
  //       return Collections.singleton(newElement);
  //     } else {
  //       if (missingInformationLeftJVariable != null) {
  //         newElement.forget(MemoryLocation.valueOf(missingInformationLeftJVariable));
  //       }
  //       missingInformationRightJExpression = null;
  //       missingInformationLeftJVariable = null;
  //       return Collections.singleton(newElement);
  //     }
  //   }
  //   return null;
  // }

  // private String getRTTScopedVariableName(String fieldName, String uniqueObject) {
  //   return  uniqueObject + "::"+ fieldName;
  // }

  // private Value handleMissingInformationRightJExpression(RTTState pJortState) {
  //   return missingInformationRightJExpression.accept(
  //       new FieldAccessExpressionValueVisitor(pJortState, oldState));
  // }

  // private TaintAnalysisState handleNotScopedVariable(RTTState rttState, TaintAnalysisState newElement) {

  //  String objectScope = NameProvider.getInstance()
  //                                   .getObjectScope(rttState, functionName, notScopedField);

  //  if (objectScope != null) {

  //    String scopedFieldName = getRTTScopedVariableName(notScopedField.getName(), objectScope);

  //    Value value = notScopedFieldValue;
  //    if (missingInformationRightJExpression != null) {
  //      value = handleMissingInformationRightJExpression(rttState);
  //    }

  //    if (!value.isUnknown()) {
  //      newElement.assignConstant(scopedFieldName, value);
  //      return newElement;
  //    } else {
  //      newElement.forget(MemoryLocation.valueOf(scopedFieldName));
  //      return newElement;
  //    }
  //  } else {
  //    return null;
  //  }


  // }

  // /** returns an initialized, empty visitor */
  // private ExpressionValueVisitor getVisitor(TaintAnalysisState pState, String pFunctionName) {
  //   if (options.isIgnoreFunctionValueExceptRandom()
  //       && options.isIgnoreFunctionValue()
  //       && options.getFunctionValuesForRandom() != null) {
  //     return new ExpressionValueVisitorWithPredefinedValues(
  //         pState,
  //         pFunctionName,
  //         options.getFunctionValuesForRandom(),
  //         ValueAnalysisTransferRelation.indexForNextRandomValue,
  //         machineModel,
  //         logger);
  //   } else if (options.isIgnoreFunctionValue()) {
  //     return new ExpressionValueVisitor(pState, pFunctionName, machineModel, logger);
  //   } else {
  //     return new FunctionPointerExpressionValueVisitor(pState, pFunctionName, machineModel, logger);
  //   }
  // }

  // private ExpressionValueVisitor getVisitor() {
  //   return getVisitor(state, functionName);
  // }
}
