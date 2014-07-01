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
package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.APointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.IAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
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
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.rtt.RTTState;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.value.SymbolicValueFormula.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.Value.UnknownValue;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@Options(prefix="cpa.value")
public class ValueAnalysisTransferRelation extends ForwardingTransferRelation<ValueAnalysisState, ValueAnalysisState, ValueAnalysisPrecision> {
  // set of functions that may not appear in the source code
  // the value of the map entry is the explanation for the user
  private static final Map<String, String> UNSUPPORTED_FUNCTIONS
      = ImmutableMap.of("pthread_create", "threads");

  private boolean symbolicValues = new SymbolicValuesOption().areSymbolicValuesEnabled();

  @Option(description = "if there is an assumption like (x!=0), "
      + "this option sets unknown (uninitialized) variables to 1L, "
      + "when the true-branch is handled.")
  private boolean initAssumptionVars = false;

  @Option(description = "Process the Automaton ASSUMEs as if they were statements, not as if they were"
      + " assumtions.")
  private boolean automatonAssumesAsStatements = false;

  private final Set<String> javaNonStaticVariables = new HashSet<>();

  private JRightHandSide missingInformationRightJExpression = null;
  private String missingInformationLeftJVariable = null;

  private boolean missingFieldVariableObject;
  private Pair<String, Value> fieldNameAndInitialValue;

  private boolean missingScopedFieldName;
  private JIdExpression notScopedField;
  private Value notScopedFieldValue;

  private boolean missingAssumeInformation;

  /**
   * This List is used to communicate the missing
   * Information needed from other cpas.
   * (at the moment specifically SMG)
   */
  private List<MissingInformation> missingInformationList;

  /**
   * Save the old State for strengthen.
   */
  private ValueAnalysisState oldState;

  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;
  private final Collection<String> addressedVariables;
  private final Collection<String> booleanVariables;

  public ValueAnalysisTransferRelation(Configuration config, LogManager pLogger, CFA pCfa) throws InvalidConfigurationException {
    config.inject(this);
    machineModel = pCfa.getMachineModel();
    logger = new LogManagerWithoutDuplicates(pLogger);

    if (pCfa.getVarClassification().isPresent()) {
      addressedVariables = pCfa.getVarClassification().get().getAddressedVariables();
      booleanVariables   = pCfa.getVarClassification().get().getIntBoolVars();
    } else {
      addressedVariables = ImmutableSet.of();
      booleanVariables   = ImmutableSet.of();
    }
  }

  @Override
  protected Collection<ValueAnalysisState> postProcessing(ValueAnalysisState successor) {
    if (successor != null){
      successor.addToDelta(state);
    }
    return super.postProcessing(successor);
  }


  @Override
  protected void setInfo(AbstractState pAbstractState,
      Precision pAbstractPrecision, CFAEdge pCfaEdge) {
    super.setInfo(pAbstractState, pAbstractPrecision, pCfaEdge);
    // More than 5 function parameters is sufficiently seldom.
    // For any other cfaEdge we need only a list of length 1.
    // In principle it is unnecessary to always create a new list
    // but I'm not sure of the behavior of calling strengthen, so
    // it is more secure.
    missingInformationList = new ArrayList<>(5);
    oldState = ((ValueAnalysisState) pAbstractState).clone();
  }

  @Override
  protected ValueAnalysisState handleMultiEdge(final MultiEdge cfaEdge) throws CPATransferException {
    // we need to keep the old state,
    // because the analysis uses a 'delta' for the now state
    final ValueAnalysisState backup = state;
    for (CFAEdge edge : cfaEdge) {
      state = handleSimpleEdge(edge);
    }
    final ValueAnalysisState successor = state;
    state = backup;
    return successor;
  }

  @Override
  protected ValueAnalysisState handleFunctionCallEdge(FunctionCallEdge callEdge,
      List<? extends IAExpression> arguments, List<? extends AParameterDeclaration> parameters,
      String calledFunctionName) throws UnrecognizedCCodeException {
    ValueAnalysisState newElement = state.clone();

    if (!callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert (parameters.size() == arguments.size());
    }

    // visitor for getting the values of the actual parameters in caller function context
    final ExpressionValueVisitor visitor = getVisitor();

    // get value of actual parameter in caller function context
    for (int i = 0; i < parameters.size(); i++) {
      Value value;
      IAExpression exp = arguments.get(i);

      if (exp instanceof JExpression) {
        // value = ((JExpression) exp).accept(visitor); TODO
        value = Value.UnknownValue.getInstance();
      } else if (exp instanceof CExpression) {
        value = visitor.evaluate((CExpression) exp, (CType) parameters.get(i).getType());
      } else {
        throw new AssertionError("unknown expression: " + exp);
      }

      String paramName = parameters.get(i).getName();

      MemoryLocation formalParamName = MemoryLocation.valueOf(calledFunctionName, paramName, 0);

      if (value.isUnknown()) {
        newElement.forget(formalParamName);

        if(isMissingCExpressionInformation(visitor, exp)) {
          addMissingInformation(formalParamName, exp);
        }
      } else {
        newElement.assignConstant(formalParamName, value);
      }

      visitor.reset();

    }

    return newElement;
  }

  @Override
  protected ValueAnalysisState handleBlankEdge(BlankEdge cfaEdge) {
    if (cfaEdge.getSuccessor() instanceof FunctionExitNode) {
      assert "default return".equals(cfaEdge.getDescription())
              || "skipped uneccesary edges".equals(cfaEdge.getDescription());

      // clone state, because will be changed through removing all variables of current function's scope
      state = state.clone();
      state.dropFrame(functionName);
    }

    return state;
  }

  @Override
  protected ValueAnalysisState handleReturnStatementEdge(AReturnStatementEdge returnEdge, IAExpression expression)
          throws UnrecognizedCCodeException {

    // visitor must use the initial (previous) state, because there we have all information about variables
    ExpressionValueVisitor evv = new ExpressionValueVisitor(state, functionName, machineModel, logger, symbolicValues);

    // clone state, because will be changed through removing all variables of current function's scope
    state = state.clone();
    state.dropFrame(functionName);

    if (expression == null && returnEdge instanceof CReturnStatementEdge) {
      expression = CNumericTypes.ZERO; // this is the default in C
    }

    if (expression!= null) {
      MemoryLocation functionReturnVar = MemoryLocation.valueOf(VariableClassification.createFunctionReturnVariable(functionName));

      return handleAssignmentToVariable(functionReturnVar,
          returnEdge.getSuccessor().getEntryNode().getFunctionDefinition().getType().getReturnType(), // TODO easier way to get type?
          expression,
          evv);
    } else {
      return state;
    }
  }

  /**
   * Handles return from one function to another function.
   * @param functionReturnEdge return edge from a function to its call site
   * @return new abstract state
   */
  @Override
  protected ValueAnalysisState handleFunctionReturnEdge(FunctionReturnEdge functionReturnEdge,
      FunctionSummaryEdge summaryEdge, AFunctionCall exprOnSummary, String callerFunctionName)
    throws UnrecognizedCodeException {

    ValueAnalysisState newElement  = state.clone();
    MemoryLocation returnVarName = MemoryLocation.valueOf(VariableClassification.createFunctionReturnVariable(functionName));

    // expression is an assignment operation, e.g. a = g(b);

    if (exprOnSummary instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement assignExp = ((AFunctionCallAssignmentStatement)exprOnSummary);
      IAExpression op1 = assignExp.getLeftHandSide();

      // we expect left hand side of the expression to be a variable

      if (op1 instanceof CLeftHandSide) {
        ExpressionValueVisitor v =
            new ExpressionValueVisitor(state, callerFunctionName,
                machineModel, logger, symbolicValues);
        MemoryLocation assignedVarName = v.evaluateMemoryLocation((CLeftHandSide) op1);

        boolean valueExists = state.contains(returnVarName);

        if (assignedVarName == null) {
          if (v.hasMissingPointer() && valueExists) {
            Value value = state.getValueFor(returnVarName);
            addMissingInformation((CLeftHandSide) op1, value);
          }
        } else if (valueExists) {
          Value value = state.getValueFor(returnVarName);
          newElement.assignConstant(assignedVarName, value);
        } else {
          newElement.forget(assignedVarName);
        }

      } else if ((op1 instanceof AIdExpression)) {
        String assignedVarName = ((AIdExpression) op1).getDeclaration().getQualifiedName();

        if (!state.contains(returnVarName)) {
          newElement.forget(assignedVarName);
        } else if (op1 instanceof JIdExpression && ((JIdExpression) op1).getDeclaration() instanceof JFieldDeclaration && !((JFieldDeclaration) ((JIdExpression) op1).getDeclaration()).isStatic()) {
          missingScopedFieldName = true;
          notScopedField = (JIdExpression) op1;
          // notScopedFieldValue = state.getValueFor(returnVarName); TODO
        } else {
          newElement.assignConstant(assignedVarName, state.getValueFor(returnVarName));
        }
      }

      // a* = b(); TODO: for now, nothing is done here, but cloning the current element
      else if (op1 instanceof APointerExpression) {
      }
      else {
        throw new UnrecognizedCodeException("on function return", summaryEdge, op1);
      }
    }

    newElement.forget(returnVarName);
    return newElement;
  }

  @Override
  protected ValueAnalysisState handleFunctionSummaryEdge(CFunctionSummaryEdge cfaEdge) throws CPATransferException {
    ValueAnalysisState newState = state.clone();
    AFunctionCall functionCall  = cfaEdge.getExpression();

    if (functionCall instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement assignment = ((AFunctionCallAssignmentStatement)functionCall);
      IAExpression leftHandSide = assignment.getLeftHandSide();

      if (leftHandSide instanceof CLeftHandSide) {
        MemoryLocation assignedMemoryLocation = getVisitor().evaluateMemoryLocation((CLeftHandSide) leftHandSide);

        if (newState.contains(assignedMemoryLocation)) {
          newState.forget(assignedMemoryLocation);
        }
      }
    }

    return newState;
  }

  @Override
  protected ValueAnalysisState handleAssumption(AssumeEdge cfaEdge, IAExpression expression, boolean truthValue)
    throws UnrecognizedCCodeException {

    ExpressionValueVisitor evv = getVisitor();

    // get the value of the expression (either true[1L], false[0L], or unknown[null])
    Value value = getExpressionValue(expression, CNumericTypes.INT, evv);

    if (!value.isExplicitlyKnown()) {
      ValueAnalysisState element = state.clone();

      // If it's a symbolic formula, try if we can solve it for any of its symbolic values.
      if(value instanceof SymbolicValueFormula) {
        Pair<SymbolicValue, Value> replacement = null;
        replacement = ((SymbolicValueFormula)value).inferAssignment(truthValue, logger);
        if(replacement != null) {
          for(MemoryLocation memloc : state.getTrackedMemoryLocations()) {
            Value trackedValue = state.getValueFor(memloc);
            if(trackedValue instanceof SymbolicValueFormula) {
              SymbolicValueFormula trackedFormula = (SymbolicValueFormula) trackedValue;
              Value newValue = trackedFormula.replaceSymbolWith(replacement.getFirst(), replacement.getSecond(), logger);
              if(newValue != trackedValue) {
                element.assignConstant(memloc, newValue);
              }
            }
          }
        }
      }

      AssigningValueVisitor avv = new AssigningValueVisitor(element, truthValue, booleanVariables);

      if (expression instanceof JExpression && ! (expression instanceof CExpression)) {

        ((JExpression) expression).accept(avv);

        if (avv.hasMissingFieldAccessInformation() || avv.hasMissingEnumComparisonInformation()) {
          assert missingInformationRightJExpression != null;
          missingAssumeInformation = true;
        }

      } else {
        ((CExpression)expression).accept(avv);
      }

      if (isMissingCExpressionInformation(evv, expression)) {
        missingInformationList.add(new MissingInformation(truthValue, expression));
      }

      return element;

    } else if ((truthValue && value.equals(new NumericValue(1L))) || (!truthValue && value.equals(new NumericValue(0L)))) {
      // we do not know more than before, and the assumption is fulfilled, so return a copy of the old state
      // we need to return a copy, otherwise precision adjustment might reset too much information, even on the original state
      return state.clone();

    } else {
      // assumption not fulfilled
      return null;
    }
  }


  @Override
  protected ValueAnalysisState handleDeclarationEdge(ADeclarationEdge declarationEdge, IADeclaration declaration)
    throws UnrecognizedCCodeException {

    if (!(declaration instanceof AVariableDeclaration)) {
      // nothing interesting to see here, please move along
      return state;
    }

    ValueAnalysisState newElement = state.clone();
    AVariableDeclaration decl = (AVariableDeclaration)declaration;

    // get the variable name in the declarator
    String varName = decl.getName();

    Value initialValue = Value.UnknownValue.getInstance();

    // get initial value
    IAInitializer init = decl.getInitializer();

    // handle global variables
    if (decl.isGlobal()) {
      if (decl instanceof JFieldDeclaration && !((JFieldDeclaration)decl).isStatic()) {
        missingFieldVariableObject = true;
        javaNonStaticVariables.add(varName);
      }

      // global variables without initializer are set to 0 in C
      if (init == null) {
        initialValue = new NumericValue(0L);
      }
    }

    MemoryLocation memoryLocation;

    // assign initial value if necessary
    if(decl.isGlobal()) {
      memoryLocation = MemoryLocation.valueOf(varName,0);
    }else {
      memoryLocation = MemoryLocation.valueOf(functionName, varName, 0);
    }

    if (addressedVariables.contains(decl.getQualifiedName())
        && decl.getType() instanceof CType
        && ((CType)decl.getType()).getCanonicalType() instanceof CPointerType) {
      ValueAnalysisState.addToBlacklist(memoryLocation);
    }

    if (init instanceof AInitializerExpression) {

      ExpressionValueVisitor evv = getVisitor();
      IAExpression exp = ((AInitializerExpression) init).getExpression();
      initialValue = getExpressionValue(exp, decl.getType(), evv);

      if (isMissingCExpressionInformation(evv, exp)) {
        addMissingInformation(memoryLocation, exp);
      }
    }

    boolean complexType = decl.getType() instanceof JClassOrInterfaceType || decl.getType() instanceof JArrayType;


    if (!complexType  && (missingInformationRightJExpression != null || !initialValue.isUnknown())) {
      if (missingFieldVariableObject) {
        fieldNameAndInitialValue = Pair.of(varName, initialValue);
      } else if (missingInformationRightJExpression == null) {
        newElement.assignConstant(memoryLocation, initialValue);
      } else {
        missingInformationLeftJVariable = memoryLocation.getAsSimpleString();
      }
    } else {

      // If variable not tracked, its Object is irrelevant
      missingFieldVariableObject = false;
      newElement.forget(memoryLocation);
    }

    return newElement;
  }

  private boolean isMissingCExpressionInformation(ExpressionValueVisitor pEvv,
      IARightHandSide pExp) {

    return pExp instanceof CExpression && (pEvv.hasMissingPointer());
  }

  @Override
  protected ValueAnalysisState handleStatementEdge(AStatementEdge cfaEdge, IAStatement expression)
    throws UnrecognizedCodeException {

    if (expression instanceof CFunctionCall) {
      CExpression fn = ((CFunctionCall)expression).getFunctionCallExpression().getFunctionNameExpression();
      if (fn instanceof CIdExpression) {
        String func = ((CIdExpression)fn).getName();
        if (UNSUPPORTED_FUNCTIONS.containsKey(func)) {
          throw new UnsupportedCCodeException(UNSUPPORTED_FUNCTIONS.get(func), cfaEdge, fn);
        } else if(func.equals("free")) {
          // Needed for erasing values
          missingInformationList.add(new MissingInformation(((CFunctionCall)expression).getFunctionCallExpression()));
        }
      }
    }

    // expression is a binary operation, e.g. a = b;

    if (expression instanceof IAssignment) {
      return handleAssignment((IAssignment)expression, cfaEdge);

    // external function call - do nothing
    } else if (expression instanceof AFunctionCallStatement) {

    // there is such a case
    } else if (expression instanceof AExpressionStatement) {

    } else {
      throw new UnrecognizedCodeException("Unknown statement", cfaEdge, expression);
    }

    return state;
  }

  private ValueAnalysisState handleAssignment(IAssignment assignExpression, CFAEdge cfaEdge)
    throws UnrecognizedCodeException {
    IAExpression op1    = assignExpression.getLeftHandSide();
    IARightHandSide op2 = assignExpression.getRightHandSide();


    if (op1 instanceof AIdExpression) {
      // a = ...

        if (op1 instanceof JIdExpression && ((JIdExpression) op1).getDeclaration() instanceof JFieldDeclaration && !((JFieldDeclaration) ((JIdExpression) op1).getDeclaration()).isStatic()) {
          missingScopedFieldName = true;
          notScopedField = (JIdExpression) op1;
        }

        String varName = ((AIdExpression) op1).getName();

        MemoryLocation memloc;

        if(isGlobal(op1)) {
          memloc = MemoryLocation.valueOf(varName, 0);
        } else {
          memloc = MemoryLocation.valueOf(functionName, varName, 0);
        }

        return handleAssignmentToVariable(memloc, op1.getExpressionType(), op2, getVisitor());
    } else if (op1 instanceof APointerExpression) {
      // *a = ...

      if (isRelevant(op1, op2)) {
        missingInformationList.add(new MissingInformation(op1, op2));
      }

      op1 = ((APointerExpression)op1).getOperand();

      // Cil produces code like
      // *((int*)__cil_tmp5) = 1;
      // so remove cast
      if (op1 instanceof CCastExpression) {
        op1 = ((CCastExpression)op1).getOperand();
      }
    }

    else if (op1 instanceof CFieldReference) {

      ExpressionValueVisitor v = getVisitor();

      MemoryLocation memLoc = v.evaluateMemoryLocation((CFieldReference) op1);

      if (v.hasMissingPointer() && isRelevant(op1, op2)) {
        missingInformationList.add(new MissingInformation(op1, op2));
      }

      if (memLoc != null) {
        return handleAssignmentToVariable(memLoc, op1.getExpressionType(), op2, v);
      }
    }

    else if (op1 instanceof CArraySubscriptExpression || op1 instanceof AArraySubscriptExpression) {
      // array cell
      if (op1 instanceof CArraySubscriptExpression) {

        ExpressionValueVisitor v = getVisitor();

        MemoryLocation memLoc = v.evaluateMemoryLocation((CLeftHandSide) op1);

        if (v.hasMissingPointer() && isRelevant(op1, op2)) {
          missingInformationList.add(new MissingInformation(op1, op2));
        }

        if (memLoc != null) {
          return handleAssignmentToVariable(memLoc, op1.getExpressionType(), op2, v);
        }
      }
    } else {
      throw new UnrecognizedCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
    }

    return state; // the default return-value is the old state
  }


  private boolean isRelevant(IAExpression pOp1, IARightHandSide pOp2) {
    return pOp1 instanceof CExpression && pOp2 instanceof CExpression;
  }

  /** This method analyses the expression with the visitor and assigns the value to lParam.
   * The method returns a new state, that contains (a copy of) the old state and the new assignment. */
  private ValueAnalysisState handleAssignmentToVariable(
      MemoryLocation assignedVar, final Type lType, IARightHandSide exp, ExpressionValueVisitor visitor)
      throws UnrecognizedCCodeException {

    Value value;
    if (exp instanceof JRightHandSide) {
       value = ((JRightHandSide) exp).accept(visitor); // TODO - find out whether something's wrong with this
      //value = Value.UnknownValue.getInstance();
    } else if (exp instanceof CRightHandSide) {
       value = visitor.evaluate((CRightHandSide) exp, (CType) lType);
    } else {
      throw new AssertionError("unknown righthandside-expression: " + exp);
    }

    if (visitor.hasMissingPointer()) {
      assert !value.isExplicitlyKnown();
    }

    if (isMissingCExpressionInformation(visitor, exp)) {
      // Evaluation
      addMissingInformation(assignedVar, exp);
    }

    // here we clone the state, because we get new information or must forget it.
    ValueAnalysisState newElement = state.clone();

    if (visitor.hasMissingFieldAccessInformation() || visitor.hasMissingEnumComparisonInformation()) {
      // This may happen if an object of class is created which could not be parsed,
      // In  such a case, forget about it
      if (!value.isUnknown()) {
        newElement.forget(assignedVar);
        return newElement;
      } else {
        missingInformationRightJExpression = (JRightHandSide) exp;
        if (!missingScopedFieldName) {
          missingInformationLeftJVariable = assignedVar.getAsSimpleString();
        }
      }
    }

    if (missingScopedFieldName) {
      notScopedFieldValue = value;
    } else {
      // some heuristics to clear wrong information
      // when a struct or a pointer to one is assigned
      // TODO not implemented in SMG version of ValueAnalysisCPA
//      newElement.forgetAllWithPrefix(assignedVar + ".");
//      newElement.forgetAllWithPrefix(assignedVar + "->");

      if (value.isUnknown()) {
        // Don't erase it when there if it has yet to be evaluated
        if (missingInformationRightJExpression == null) {
          // TODO HasToBeErased Later
         newElement.forget(assignedVar);
        }
      } else {
        newElement.assignConstant(assignedVar, value);
      }

    }
    return newElement;
  }

  private void addMissingInformation(MemoryLocation pMemLoc, IARightHandSide pExp) {
    if (pExp instanceof CExpression) {

      missingInformationList.add(new MissingInformation(pMemLoc,
          (CExpression) pExp));
    }
  }

  private void addMissingInformation(CLeftHandSide pOp1, Value pValue) {
    missingInformationList.add(new MissingInformation(pOp1, pValue));

  }

  /**
   * Visitor that derives further information from an assume edge
   */
  private class AssigningValueVisitor extends ExpressionValueVisitor {

    private ValueAnalysisState assignableState;

    private Collection<String> booleans;

    protected boolean truthValue = false;

    public AssigningValueVisitor(ValueAnalysisState assignableState, boolean truthValue, Collection<String> booleanVariables) {
      super(state, functionName, machineModel, logger, symbolicValues);
      this.assignableState  = assignableState;
      this.booleans         = booleanVariables;
      this.truthValue       = truthValue;
    }

    private IAExpression unwrap(IAExpression expression) {
      // is this correct for e.g. [!a != !(void*)(int)(!b)] !?!?!

      if (expression instanceof CCastExpression) {
        CCastExpression exp = (CCastExpression)expression;
        expression = exp.getOperand();

        expression = unwrap(expression);
      }

      return expression;
    }

    @Override
    public Value visit(CBinaryExpression pE) throws UnrecognizedCCodeException {
      BinaryOperator binaryOperator = pE.getOperator();
      CExpression lVarInBinaryExp   = pE.getOperand1();
      CExpression rVarInBinaryExp   = pE.getOperand2();

      lVarInBinaryExp = (CExpression) unwrap(pE.getOperand1());

      Value leftValue   = lVarInBinaryExp.accept(this);
      Value rightValue  = rVarInBinaryExp.accept(this);

      if (isEqualityAssumption(binaryOperator)) {
        if (leftValue.isUnknown() && !rightValue.isUnknown() && isAssignable(lVarInBinaryExp)) {
          assignableState.assignConstant(getMemoryLocation(lVarInBinaryExp), rightValue);
        }

        else if (rightValue.isUnknown() && !leftValue.isUnknown() && isAssignable(rVarInBinaryExp)) {
          assignableState.assignConstant(getMemoryLocation(rVarInBinaryExp), leftValue);
        }
      }

      if (isNonEqualityAssumption(binaryOperator)) {
        if (assumingUnknownToBeZero(leftValue, rightValue) && isAssignable(lVarInBinaryExp)) {
          MemoryLocation leftMemLoc = getMemoryLocation(lVarInBinaryExp);

          if(booleans.contains(leftMemLoc.getAsSimpleString()) || initAssumptionVars) {
            assignableState.assignConstant(leftMemLoc, new NumericValue(1L));
          }
        }

        else if (assumingUnknownToBeZero(rightValue, leftValue) && isAssignable(rVarInBinaryExp)) {
          MemoryLocation rightMemLoc = getMemoryLocation(rVarInBinaryExp);

          if(booleans.contains(rightMemLoc.getAsSimpleString()) || initAssumptionVars) {
            assignableState.assignConstant(rightMemLoc, new NumericValue(1L));
          }
        }
      }

      return super.visit(pE);
    }

    private boolean assumingUnknownToBeZero(Value value1, Value value2) {
      return value1.isUnknown() && value2.equals(new NumericValue(BigInteger.ZERO));
    }

    private boolean isEqualityAssumption(BinaryOperator binaryOperator) {
      return (binaryOperator == BinaryOperator.EQUALS && truthValue)
          || (binaryOperator == BinaryOperator.NOT_EQUALS && !truthValue);
    }

    private boolean isNonEqualityAssumption(BinaryOperator binaryOperator) {
      return (binaryOperator == BinaryOperator.EQUALS && !truthValue)
          || (binaryOperator == BinaryOperator.NOT_EQUALS && truthValue);
    }

    @Override
    public Value visit(JBinaryExpression pE) {
      JBinaryExpression.BinaryOperator binaryOperator   = pE.getOperator();

      JExpression lVarInBinaryExp  = pE.getOperand1();

      lVarInBinaryExp = (JExpression) unwrap(lVarInBinaryExp);

      JExpression rVarInBinaryExp  = pE.getOperand2();

      Value leftValueV  = lVarInBinaryExp.accept(this);
      Value rightValueV = rVarInBinaryExp.accept(this);

      Long leftValue  = leftValueV.isUnknown() || !leftValueV.isNumericValue() ? null : ((NumericValue) leftValueV).longValue();
      Long rightValue = rightValueV.isUnknown() || !leftValueV.isNumericValue() ? null : ((NumericValue) leftValueV).longValue();

      if ((binaryOperator == JBinaryExpression.BinaryOperator.EQUALS && truthValue) || (binaryOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS && !truthValue)) {
        if (leftValue == null &&  rightValue != null && isAssignable(lVarInBinaryExp)) {

          @SuppressWarnings("unused")
          String leftVariableName = ((AIdExpression) lVarInBinaryExp).getDeclaration().getQualifiedName();
          assignableState.assignConstant(leftVariableName, rightValueV);
        } else if (rightValue == null && leftValue != null && isAssignable(rVarInBinaryExp)) {
          @SuppressWarnings("unused")
          String rightVariableName = ((AIdExpression) rVarInBinaryExp).getDeclaration().getQualifiedName();
          assignableState.assignConstant(rightVariableName, leftValueV);

        }
      }

      if (initAssumptionVars) {
        // x is unknown, a binaryOperation (x!=0), true-branch: set x=1L
        // x is unknown, a binaryOperation (x==0), false-branch: set x=1L
        if ((binaryOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS && truthValue)
            || (binaryOperator == JBinaryExpression.BinaryOperator.EQUALS && !truthValue)) {
          if (leftValue == null && rightValue == 0L && isAssignable(lVarInBinaryExp)) {
            String leftVariableName = ((AIdExpression) lVarInBinaryExp).getDeclaration().getQualifiedName();
            assignableState.assignConstant(leftVariableName, new NumericValue(1L));
          }

          else if (rightValue == null && leftValue == 0L && isAssignable(rVarInBinaryExp)) {
            String rightVariableName = ((AIdExpression) rVarInBinaryExp).getDeclaration().getQualifiedName();
            assignableState.assignConstant(rightVariableName, new NumericValue(1L));
          }
        }
      }
      return super.visit(pE);
    }

    protected MemoryLocation getMemoryLocation(CExpression pLValue) throws UnrecognizedCCodeException {
      ExpressionValueVisitor v = getVisitor();
      assert pLValue instanceof CLeftHandSide;
      return checkNotNull(v.evaluateMemoryLocation(pLValue));
    }

    protected boolean isAssignable(JExpression expression) {

      boolean result = false;

      if (expression instanceof JIdExpression) {

        JSimpleDeclaration decl = ((JIdExpression) expression).getDeclaration();

        if (decl == null) {
          result = false;
        } else if (decl instanceof JFieldDeclaration) {
          result = ((JFieldDeclaration) decl).isStatic();
        } else {
          result = true;
        }
      }

      return result;
    }



    protected boolean isAssignable(CExpression expression) throws UnrecognizedCCodeException  {

      if (expression instanceof CIdExpression) {
        return true;
      }

      if (expression instanceof CFieldReference || expression instanceof CArraySubscriptExpression) {
        ExpressionValueVisitor evv = getVisitor();
        return evv.canBeEvaluated(expression);
      }

      return false;
    }
  }


  private class SMGAssigningValueVisitor extends AssigningValueVisitor {

    private final ValueAnalysisSMGCommunicator expressionEvaluator;
    @SuppressWarnings("unused")
    private final SMGState smgState;

    public SMGAssigningValueVisitor(
        ValueAnalysisState pAssignableState,
        boolean pTruthValue,
        Collection<String> booleanVariables,
        SMGState pSmgState) {

      super(pAssignableState, pTruthValue, booleanVariables);
      checkNotNull(pSmgState);
      expressionEvaluator = new ValueAnalysisSMGCommunicator(pAssignableState, functionName,
          pSmgState, machineModel, logger, edge);
      smgState = pSmgState;
    }

    @Override
    protected boolean isAssignable(CExpression pExpression) throws UnrecognizedCCodeException {

      //TODO Ugly, Refactor
      if (pExpression instanceof CLeftHandSide) {
        MemoryLocation memLoc =
            expressionEvaluator.evaluateLeftHandSide(pExpression);

        return memLoc != null;
      }

      return false;
    }

    @Override
    protected MemoryLocation getMemoryLocation(CExpression pLValue) throws UnrecognizedCCodeException {
      return expressionEvaluator.evaluateLeftHandSide(pLValue);
    }
  }

  private class  FieldAccessExpressionValueVisitor extends ExpressionValueVisitor {
    private final RTTState jortState;

    public FieldAccessExpressionValueVisitor(RTTState pJortState) {
      super(state, functionName, machineModel, logger, symbolicValues);
      jortState = pJortState;
    }

    @Override
    public Value visit(JBinaryExpression binaryExpression) {

      if ((binaryExpression.getOperator() == JBinaryExpression.BinaryOperator.EQUALS
          || binaryExpression.getOperator() == JBinaryExpression.BinaryOperator.NOT_EQUALS)
          && (binaryExpression.getOperand1() instanceof JEnumConstantExpression
              ||  binaryExpression.getOperand2() instanceof JEnumConstantExpression)) {
        return handleEnumComparison(
            binaryExpression.getOperand1(),
            binaryExpression.getOperand2(), binaryExpression.getOperator());
      }

      return super.visit(binaryExpression);
    }

    private Value handleEnumComparison(JExpression operand1, JExpression operand2,
        JBinaryExpression.BinaryOperator operator) {

      String value1;
      String value2;

      if (operand1 instanceof JEnumConstantExpression) {
        value1 = ((JEnumConstantExpression) operand1).getConstantName();
      } else if (operand1 instanceof JIdExpression) {
        String scopedVarName = handleIdExpression((JIdExpression) operand1);

        if (jortState.contains(scopedVarName)) {
          String uniqueObject = jortState.getUniqueObjectFor(scopedVarName);

          if (jortState.getConstantsMap().containsValue(uniqueObject)) {
            value1 = jortState.getRunTimeClassOfUniqueObject(uniqueObject);
          } else {
            return UnknownValue.getInstance();
          }
        } else {
          return UnknownValue.getInstance();
        }
      } else {
        return UnknownValue.getInstance();
      }


      if (operand2 instanceof JEnumConstantExpression) {
        value2 = ((JEnumConstantExpression) operand2).getConstantName();
      } else if (operand1 instanceof JIdExpression) {
        String scopedVarName = handleIdExpression((JIdExpression) operand2);

        if (jortState.contains(scopedVarName)) {
          String uniqueObject = jortState.getUniqueObjectFor(scopedVarName);

          if (jortState.getConstantsMap().containsValue(uniqueObject)) {
            value2 = jortState.getRunTimeClassOfUniqueObject(uniqueObject);
          } else {
            return UnknownValue.getInstance();
          }
        } else {
          return UnknownValue.getInstance();
        }
      } else {
        return UnknownValue.getInstance();
      }

      boolean result = value1.equals(value2);

      switch (operator) {
      case EQUALS:   break;
      case NOT_EQUALS: result = !result;
      }

      return  result ? new NumericValue(1L) : new NumericValue(0L);
    }

    private String handleIdExpression(JIdExpression expr) {

      JSimpleDeclaration decl = expr.getDeclaration();

      if (decl == null) {
        return null;
      }

      String objectScope = getObjectScope(jortState, functionName, expr);

      return getRTTScopedVariableName(decl, functionName, objectScope);

    }

    @Override
    public Value visit(JIdExpression idExp) {

      String varName = handleIdExpression(idExp);

      if (state.contains(varName)) {
        return state.getValueFor(varName);
      } else {
        return Value.UnknownValue.getInstance();
      }
    }
  }

  private Value getExpressionValue(IAExpression expression, final Type type, ExpressionValueVisitor evv)
      throws UnrecognizedCCodeException {

    if (expression instanceof JRightHandSide) {

      final Value value = ((JRightHandSide) expression).accept(evv);

      if (evv.hasMissingFieldAccessInformation() || evv.hasMissingEnumComparisonInformation()) {
        missingInformationRightJExpression = (JRightHandSide) expression;
        return Value.UnknownValue.getInstance();
      } else {
        return value;
      }
    } else if (expression instanceof CRightHandSide) {
      return evv.evaluate((CRightHandSide) expression, (CType) type);
    } else {
      throw new AssertionError("unhandled righthandside-expression: " + expression);
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element, List<AbstractState> elements, CFAEdge cfaEdge, Precision precision)
    throws CPATransferException {
    assert element instanceof ValueAnalysisState;

    ArrayList<ValueAnalysisState> toStrengthen = new ArrayList<>();
    ArrayList<ValueAnalysisState> result = new ArrayList<>();
    toStrengthen.add((ValueAnalysisState) element);
    result.add((ValueAnalysisState) element);

    //
    for (AbstractState ae : elements) {
      if (ae instanceof RTTState) {
        result.clear();
        for(ValueAnalysisState state : toStrengthen) {
          super.setInfo(element, precision, cfaEdge);
          Collection<ValueAnalysisState> ret = strengthen((RTTState)ae);
          if(ret == null) {
            result.add(state);
          } else {
            result.addAll(ret);
          }
        }
        toStrengthen.clear();
        toStrengthen.addAll(result);
      } else if(ae instanceof SMGState) {
        result.clear();
        for(ValueAnalysisState state : toStrengthen) {
          super.setInfo(element, precision, cfaEdge);
          Collection<ValueAnalysisState> ret = strengthen((SMGState)ae);
          if(ret == null) {
            result.add(state);
          } else {
            result.addAll(ret);
          }
        }
        toStrengthen.clear();
        toStrengthen.addAll(result);
      } else if(ae instanceof AutomatonState) {
        result.clear();
        for(ValueAnalysisState state : toStrengthen) {
          super.setInfo(element, precision, cfaEdge);
          AutomatonState autoState = (AutomatonState) ae;
          Collection<ValueAnalysisState> ret = automatonAssumesAsStatements ?
              strengthenAutomatonStatement(autoState, cfaEdge) : strengthenAutomatonAssume(autoState, cfaEdge);
          if(ret == null) {
            result.add(state);
          } else {
            result.addAll(ret);
          }
        }
        toStrengthen.clear();
        toStrengthen.addAll(result);
      }
    }

    super.resetInfo();
    oldState = null;

    return result;
  }

  private Collection<ValueAnalysisState> strengthenAutomatonStatement(AutomatonState pAutomatonState, CFAEdge pCfaEdge) throws CPATransferException {

    CIdExpression retVarName = new CIdExpression(FileLocation.DUMMY, new CSimpleType(false, false, CBasicType.INT, false, false, false, false, false, false, false), "___cpa_temp_result_var_", null);

    List<CStatementEdge> statementEdges = pAutomatonState.getAsStatementEdges(retVarName, pCfaEdge.getPredecessor().getFunctionName());

    ValueAnalysisState state = this.state;

    for(CStatementEdge stmtEdge : statementEdges) {
      state = handleStatementEdge((AStatementEdge)stmtEdge, (IAStatement)stmtEdge.getStatement());

      if(state == null) {
        break;
      } else {
        setInfo(state, precision, pCfaEdge);
      }
    }

    if (state == null) {
      return Collections.emptyList();
    } else {
      return Collections.singleton(state);
    }
  }

  private Collection<ValueAnalysisState> strengthenAutomatonAssume(AutomatonState pAutomatonState, CFAEdge pCfaEdge) throws CPATransferException {

    CIdExpression retVarName = new CIdExpression(FileLocation.DUMMY, new CSimpleType(false, false, CBasicType.INT, false, false, false, false, false, false, false), "___cpa_temp_result_var_", null);

    List<AssumeEdge> assumeEdges = pAutomatonState.getAsAssumeEdges(retVarName, pCfaEdge.getPredecessor().getFunctionName());

    ValueAnalysisState state = this.state;


    for(AssumeEdge assumeEdge : assumeEdges) {
      state = this.handleAssumption(assumeEdge, assumeEdge.getExpression(), assumeEdge.getTruthAssumption());

      if(state == null) {
        break;
      } else {
        setInfo(state, precision, pCfaEdge);
      }
    }

    if (state == null) {
      return Collections.emptyList();
    } else {
      return Collections.singleton(state);
    }
  }

  private Collection<ValueAnalysisState> strengthen(SMGState smgState) throws UnrecognizedCCodeException {

    ValueAnalysisState newElement = state.clone();

    //TODO Refactor

    for (MissingInformation missingInformation : missingInformationList) {
      if (missingInformation.isMissingAssumption()) {
        newElement = resolvingAssumption(newElement, smgState, missingInformation);
      } else if (missingInformation.isMissingAssignment()) {
        if (isRelevant(missingInformation)) {
          newElement = resolvingAssignment(newElement, smgState, missingInformation);
        } else {
          // We have to forget Nonrelevant Information to not contradict SMGState.
          newElement = forgetMemLoc(newElement, missingInformation, smgState);
        }
      } else if(missingInformation.isFreeInvocation()) {
        newElement = resolveFree(newElement, smgState, missingInformation);
      }
    }

    //TODO More common handling of missing information (erase missing Information if other cpas solved it).
    missingInformationList.clear();

    if(newElement == null) {
      return new HashSet<>();
    }

    return state.equals(newElement) ? null : Collections.singleton(newElement);
  }

  private ValueAnalysisState resolveFree(ValueAnalysisState pNewElement, SMGState pSmgState,
      MissingInformation pMissingInformation) throws UnrecognizedCCodeException {

    CFunctionCallExpression functionCall = pMissingInformation.getMissingFreeInvocation();

    CExpression pointerExp;

    try {
      pointerExp = functionCall.getParameterExpressions().get(0);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("Bulit in function free has no parameter", edge, functionCall);
    }

    ValueAnalysisSMGCommunicator cc = new ValueAnalysisSMGCommunicator(pNewElement, functionName, pSmgState,
        machineModel, logger, edge);

    SMGAddressValue address;
    try {
      address = cc.evaluateSMGAddressExpression(pointerExp);
    } catch (CPATransferException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("Error while evaluating free pointer exception.", edge, functionCall);
    }

    if (address.isUnknown()) {
      //TODO if sound Option is implemented, here every heap value has to be erased.
      return pNewElement;
    }

    pNewElement.forgetValuesWithIdentifier(address.getObject().getLabel());

    return pNewElement;
  }

  private ValueAnalysisState forgetMemLoc(ValueAnalysisState pNewElement, MissingInformation pMissingInformation,
      SMGState pSmgState) throws UnrecognizedCCodeException {

    MemoryLocation memoryLocation = null;

    if (pMissingInformation.hasKnownMemoryLocation()) {
      memoryLocation = pMissingInformation.getcLeftMemoryLocation();
    } else if (pMissingInformation.hasUnknownMemoryLocation()) {
      memoryLocation = resolveMemoryLocation(pSmgState,
          pMissingInformation.getMissingCLeftMemoryLocation());
    }

    if (memoryLocation == null) {
      // Always return the new Element
      // if you want to interrupt the calculation
      // in case it was changed before
      return pNewElement;
    } else {
      pNewElement.forget(memoryLocation);
      return pNewElement;
    }
  }

  private boolean isRelevant(MissingInformation missingInformation) {

    CRightHandSide value;

    if (missingInformation.hasUnknownMemoryLocation()) {
      value = missingInformation.getMissingCLeftMemoryLocation();
    } else if (missingInformation.hasUnknownValue()) {
      value = missingInformation.getMissingCExpressionInformation();
    } else {
      return false;
    }

    CType type = value.getExpressionType().getCanonicalType();

    return !(type instanceof CPointerType);
  }

  //TODO Better Name, these are not just Assignments, but also calls, etc
  private ValueAnalysisState resolvingAssignment(ValueAnalysisState pNewElement,
      SMGState pSmgState, MissingInformation pMissingInformation) throws UnrecognizedCCodeException {

    MemoryLocation memoryLocation = null;

    if (pMissingInformation.hasKnownMemoryLocation()) {
      memoryLocation = pMissingInformation.getcLeftMemoryLocation();
    } else if (pMissingInformation.hasUnknownMemoryLocation()) {
      memoryLocation = resolveMemoryLocation(pSmgState,
          pMissingInformation.getMissingCLeftMemoryLocation());
    }

    if (memoryLocation == null) {
      // Always return the new Element
      // if you want to interrupt the calculation
      // in case it was changed before
      return pNewElement;
    }

    Value value = Value.UnknownValue.getInstance();

    if (pMissingInformation.hasKnownValue()) {
      value = pMissingInformation.getcExpressionValue();
    } else if (pMissingInformation.hasUnknownValue()) {
      value = resolveValue(pSmgState, pMissingInformation.getMissingCExpressionInformation());
    }

    if (value.isUnknown()) {
      // Always return the new Element
      // if you want to interrupt the calculation
      // in case it was changed before
      if (pNewElement.contains(memoryLocation)) {
        pNewElement.forget(memoryLocation);
      }
      return pNewElement;
    }

    pNewElement.assignConstant(memoryLocation, value);

    return pNewElement;
  }

  private Value resolveValue(SMGState pSmgState, CExpression rValue)
      throws UnrecognizedCCodeException {

    ValueAnalysisSMGCommunicator cc = new ValueAnalysisSMGCommunicator(oldState, functionName,
        pSmgState, machineModel, logger, edge);

    return cc.evaluateExpression(rValue);
  }

  private MemoryLocation resolveMemoryLocation(SMGState pSmgState, CExpression lValue)
      throws UnrecognizedCCodeException {

    ValueAnalysisSMGCommunicator cc =
        new ValueAnalysisSMGCommunicator(oldState, functionName, pSmgState, machineModel, logger, edge);

    return cc.evaluateLeftHandSide(lValue);
  }

  private ValueAnalysisState resolvingAssumption(ValueAnalysisState pNewElement,
      SMGState pSmgState, MissingInformation pMissingInformation) throws UnrecognizedCCodeException {

    Boolean bTruthValue = pMissingInformation.getTruthAssumption();

    long truthValue = bTruthValue ? 1 : 0;

    Value value = resolveValue(pSmgState, pMissingInformation.getMissingCExpressionInformation());

    if(value.isExplicitlyKnown() && !value.equals(new NumericValue(truthValue))) {
      return null;
    } else {

      if(!value.isExplicitlyKnown()) {

        // Try deriving further Information
        ValueAnalysisState element = pNewElement.clone();
        SMGAssigningValueVisitor avv = new SMGAssigningValueVisitor(element, bTruthValue, booleanVariables, pSmgState);
        pMissingInformation.getMissingCExpressionInformation().accept(avv);

        return element;
      }

      return pNewElement;
    }
  }

  private Collection<ValueAnalysisState> strengthen(RTTState rttState)
      throws UnrecognizedCCodeException {

    ValueAnalysisState newElement = state.clone();

    if (missingFieldVariableObject) {
      newElement.assignConstant(getRTTScopedVariableName(
          fieldNameAndInitialValue.getFirst(),
          rttState.getKeywordThisUniqueObject()),
          fieldNameAndInitialValue.getSecond());

      missingFieldVariableObject = false;
      fieldNameAndInitialValue = null;
      return Collections.singleton(newElement);

    } else if (missingScopedFieldName) {

      newElement = handleNotScopedVariable(rttState, newElement);
      missingScopedFieldName = false;
      notScopedField = null;
      notScopedFieldValue = null;
      missingInformationRightJExpression = null;

      if (newElement != null) {
      return Collections.singleton(newElement);
      } else {
        return null;
      }
    } else if (missingAssumeInformation && missingInformationRightJExpression != null) {
      Value value = handleMissingInformationRightJExpression(rttState);

      missingAssumeInformation = false;
      missingInformationRightJExpression = null;

      if (value == null) {
        return null;
      } else if ((((AssumeEdge) edge).getTruthAssumption() && value.equals(new NumericValue(1L)))
          || (!((AssumeEdge) edge).getTruthAssumption() && value.equals(new NumericValue(1L)))) {
        return Collections.singleton(newElement);
      } else {
        return new HashSet<>();
      }
    } else if (missingInformationRightJExpression != null) {

      Value value = handleMissingInformationRightJExpression(rttState);

      if (value.isNumericValue() && !value.isUnknown()) {
        newElement.assignConstant(missingInformationLeftJVariable, value);
        missingInformationRightJExpression = null;
        missingInformationLeftJVariable = null;
        return Collections.singleton(newElement);
      } else {
        missingInformationRightJExpression = null;
        missingInformationLeftJVariable = null;
        if (missingInformationLeftJVariable != null) { // TODO why check this???
          newElement.forget(missingInformationLeftJVariable);
        }
        return Collections.singleton(newElement);
      }
    }
    return null;
  }

  private String getRTTScopedVariableName(String fieldName, String uniqueObject) {
    return  uniqueObject + "::"+ fieldName;
  }

  private Value handleMissingInformationRightJExpression(RTTState pJortState)
      throws UnrecognizedCCodeException {
    return missingInformationRightJExpression.accept(
        new FieldAccessExpressionValueVisitor(pJortState));
  }

  private ValueAnalysisState handleNotScopedVariable(RTTState rttState, ValueAnalysisState newElement) throws UnrecognizedCCodeException {

   String objectScope = getObjectScope(rttState, functionName, notScopedField);

   if (objectScope != null) {

     String scopedFieldName = getRTTScopedVariableName(notScopedField.getName(), objectScope);

     Value value = notScopedFieldValue;
     if (missingInformationRightJExpression != null) {
       value = Value.UnknownValue.getInstance(); // TODO handleMissingInformationRightJExpression(rttState);
     }

     if (!value.isUnknown()) {
       newElement.assignConstant(scopedFieldName, value);
       return newElement;
     } else {
       newElement.forget(scopedFieldName);
       return newElement;
     }
   } else {
     return null;
   }


  }

  private String getObjectScope(RTTState rttState, String methodName,
      JIdExpression notScopedField) {

    // Could not resolve var
    if (notScopedField.getDeclaration() == null) {
      return null;
    }

    if (notScopedField instanceof JFieldAccess) {

      JIdExpression qualifier = ((JFieldAccess) notScopedField).getReferencedVariable();

      String qualifierScope = getObjectScope(rttState, methodName, qualifier);

      String scopedFieldName =
          getRTTScopedVariableName(qualifier.getDeclaration(), methodName, qualifierScope);

      if (rttState.contains(scopedFieldName)) {
        return rttState.getUniqueObjectFor(scopedFieldName);
      } else {
        return null;
      }
    } else {
      if (rttState.contains(RTTState.KEYWORD_THIS)) {
        return rttState.getUniqueObjectFor(RTTState.KEYWORD_THIS);
      } else {
        return null;
      }
    }
  }

  private String getRTTScopedVariableName(
      JSimpleDeclaration decl,
      String methodName, String uniqueObject) {

    if (decl == null) { return ""; }

    if (decl instanceof JFieldDeclaration && ((JFieldDeclaration) decl).isStatic()) {
      return decl.getName();
    } else if (decl instanceof JFieldDeclaration) {
      return uniqueObject + "::" + decl.getName();
    } else {
      return methodName + "::" + decl.getName();
    }
  }

  private static class MissingInformation {

    /**
     * This field stores the Expression of the Memory Location that
     * could not be evaluated.
     */
    private final CExpression missingCLeftMemoryLocation;

    /**
     *  This expression stores the Memory Location
     *  to be assigned.
     */
    private final MemoryLocation cLeftMemoryLocation;

    /**
     * Expression could not be evaluated due to missing information. (e.g.
     * missing pointer alias).
     */
    private final CExpression missingCExpressionInformation;

    /**
     * Expression could not be evaluated due to missing information. (e.g.
     * missing pointer alias).
     */
    private final Value cExpressionValue;

    /**
     * The truth Assumption made in this assume edge.
     */
    private final Boolean truthAssumption;

    private CFunctionCallExpression missingFreeInvocation = null;

    @SuppressWarnings("unused")
    public MissingInformation(CExpression pMissingCLeftMemoryLocation,
        CExpression pMissingCExpressionInformation) {
      missingCExpressionInformation = pMissingCExpressionInformation;
      missingCLeftMemoryLocation = pMissingCLeftMemoryLocation;
      cExpressionValue = null;
      cLeftMemoryLocation = null;
      truthAssumption = null;
    }

    //TODO Better checks...don't be lazy, just because class
    // will likely change.

    public boolean hasUnknownValue() {
      return missingCExpressionInformation != null;
    }

    public boolean hasKnownValue() {
      return cExpressionValue != null;
    }

    public boolean hasUnknownMemoryLocation() {
      return missingCLeftMemoryLocation != null;
    }

    public boolean hasKnownMemoryLocation() {
      return cLeftMemoryLocation != null;
    }

    public boolean isMissingAssignment() {
      // TODO Better Name for this method.
      // Checks if a variable needs to be assigned a value,
      // but to evaluate the MemoryLocation, or the value,
      // we lack information.

      return (missingCExpressionInformation != null
              || missingCLeftMemoryLocation != null)
          && truthAssumption == null;
    }

    public boolean isMissingAssumption() {
      return truthAssumption != null && missingCExpressionInformation != null;
    }

    public MissingInformation(CExpression pMissingCLeftMemoryLocation,
        Value pCExpressionValue) {
      missingCExpressionInformation = null;
      missingCLeftMemoryLocation = pMissingCLeftMemoryLocation;
      cExpressionValue = pCExpressionValue;
      cLeftMemoryLocation = null;
      truthAssumption = null;
    }

    public MissingInformation(MemoryLocation pCLeftMemoryLocation,
        CExpression pMissingCExpressionInformation) {
      missingCExpressionInformation = pMissingCExpressionInformation;
      missingCLeftMemoryLocation = null;
      cExpressionValue = null;
      cLeftMemoryLocation = pCLeftMemoryLocation;
      truthAssumption = null;
    }

    public MissingInformation(IAExpression pMissingCLeftMemoryLocation,
        IARightHandSide pMissingCExpressionInformation) {
      // This constructor casts to CExpression, just to have as few
      // as possible pieces of code for communication cluttering
      // up the transfer relation.
      // Especially, since this class will later be used to
      // communicate missing Information independent of language

      missingCExpressionInformation = (CExpression) pMissingCExpressionInformation;
      missingCLeftMemoryLocation = (CExpression) pMissingCLeftMemoryLocation;
      cExpressionValue = null;
      cLeftMemoryLocation = null;
      truthAssumption = null;
    }

    public MissingInformation(Boolean pTruthAssumption,
        IARightHandSide pMissingCExpressionInformation) {
      // This constructor casts to CExpression, just to have as few
      // as possible pieces of code for communication cluttering
      // up the transfer relation.
      // Especially, since this class will later be used to
      // communicate missing Information independent of language

      missingCExpressionInformation = (CExpression) pMissingCExpressionInformation;
      missingCLeftMemoryLocation = null;
      cExpressionValue = null;
      cLeftMemoryLocation = null;
      truthAssumption = pTruthAssumption;
    }

    public MissingInformation(CFunctionCallExpression pFunctionCallExpression) {
      missingFreeInvocation = pFunctionCallExpression;
      missingCExpressionInformation = null;
      missingCLeftMemoryLocation = null;
      cExpressionValue = null;
      cLeftMemoryLocation = null;
      truthAssumption = null;

    }

    public boolean isFreeInvocation() {
      return missingFreeInvocation != null;
    }

    public Value getcExpressionValue() {
      checkNotNull(cExpressionValue);
      return cExpressionValue;
    }

    public MemoryLocation getcLeftMemoryLocation() {
      checkNotNull(cLeftMemoryLocation);
      return cLeftMemoryLocation;
    }

    @SuppressWarnings("unused")
    public CExpression getMissingCExpressionInformation() {
      checkNotNull(missingCExpressionInformation);
      return missingCExpressionInformation;
    }

    @SuppressWarnings("unused")
    public CExpression getMissingCLeftMemoryLocation() {
      checkNotNull(missingCLeftMemoryLocation);
      return missingCLeftMemoryLocation;
    }

    @SuppressWarnings("unused")
    public Boolean getTruthAssumption() {
      checkNotNull(truthAssumption);
      return truthAssumption;
    }

    public CFunctionCallExpression getMissingFreeInvocation() {
      return missingFreeInvocation;
    }
  }

  /** returns an initialized, empty visitor */
  private ExpressionValueVisitor getVisitor() {
    return new ExpressionValueVisitor(state, functionName, machineModel, logger, symbolicValues);
  }
}
