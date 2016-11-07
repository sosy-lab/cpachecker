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

import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.APointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
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
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerTransferRelation;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.rtt.NameProvider;
import org.sosy_lab.cpachecker.cpa.rtt.RTTState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.ConstraintsStrengthenOperator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.SymbolicValueAssigner;
import org.sosy_lab.cpachecker.cpa.value.type.ArrayValue;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NullValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.states.MemoryLocationValueHandler;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class ValueAnalysisTransferRelation
    extends ForwardingTransferRelation<ValueAnalysisState, ValueAnalysisState, VariableTrackingPrecision>
    implements StatisticsProvider {
  // set of functions that may not appear in the source code
  // the value of the map entry is the explanation for the user
  private static final Map<String, String> UNSUPPORTED_FUNCTIONS
      = ImmutableMap.of();

  @Options(prefix = "cpa.value")
  static class ValueTransferOptions {

    @Option(
      secure = true,
      description =
          "if there is an assumption like (x!=0), "
              + "this option sets unknown (uninitialized) variables to 1L, "
              + "when the true-branch is handled."
    )
    private boolean initAssumptionVars = false;

    @Option(
      secure = true,
      description =
          "Whether to replace symbolic values with a concrete value"
              + " when only one value is possible for an assumption to be true"
              + " (e.g. for (x == 1) set x to 1, even if x is a symbolic expression)."
    )
    private boolean assignSymbolicAssumptionVars = false;

    @Option(
      secure = true,
      description = "Assume that variables used only in a boolean context are either zero or one."
    )
    private boolean optimizeBooleanVariables = true;

    @Option(
      secure = true,
      description =
          "Track Java array values in explicit value analysis. "
              + "This may be costly if the verified program uses big or lots of arrays. "
              + "Arrays in C programs will always be tracked, even if this value is false."
    )
    private boolean trackJavaArrayValues = true;

    ValueTransferOptions(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }

    boolean isInitAssumptionVars() {
      return initAssumptionVars;
    }

    boolean isAssignSymbolicAssumptionVars() {
      return assignSymbolicAssumptionVars;
    }

    boolean isOptimizeBooleanVariables() {
      return optimizeBooleanVariables;
    }
  }

  private final ValueTransferOptions options;

  private final ConstraintsStrengthenOperator constraintsStrengthenOperator;

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
   * This class assigns symbolic values, if they are enabled.
   * Otherwise it forgets the memory location.
   */
  private MemoryLocationValueHandler unknownValueHandler;

  /**
   * This List is used to communicate the missing
   * Information needed from other cpas.
   * (at the moment specifically SMG)
   */
  private List<MissingInformation> missingInformationList;

  /**
   * Save the old State for strengthen.
   * Do not change or modify this state!
   */
  private ValueAnalysisState oldState;

  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;
  private final Collection<String> addressedVariables;
  private final Collection<String> booleanVariables;

  private int currentNumberOfIterations = 0;

  private StatCounter totalAssumptions = new StatCounter("Number of Assumptions");
  private StatCounter deterministicAssumptions = new StatCounter("Number of deterministic Assumptions");

  private Statistics transferStatistics = new Statistics() {

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      StatisticsWriter writer = writingStatisticsTo(out);

      writer.put(totalAssumptions)
            .put(deterministicAssumptions)
            .put("Level of Determinism", getCurrentLevelOfDeterminism() + "%");
    }

    @Override
    public String getName() {
      return ValueAnalysisTransferRelation.class.getSimpleName();
    }
  };

  public ValueAnalysisTransferRelation(Configuration config, LogManager pLogger, CFA pCfa) throws InvalidConfigurationException {
    options = new ValueTransferOptions(config);
    machineModel = pCfa.getMachineModel();
    logger = new LogManagerWithoutDuplicates(pLogger);

    if (pCfa.getVarClassification().isPresent()) {
      addressedVariables = pCfa.getVarClassification().get().getAddressedVariables();
      booleanVariables   = pCfa.getVarClassification().get().getIntBoolVars();
    } else {
      addressedVariables = ImmutableSet.of();
      booleanVariables   = ImmutableSet.of();
    }

    unknownValueHandler = new SymbolicValueAssigner(config);
    constraintsStrengthenOperator = new ConstraintsStrengthenOperator(config);
  }

  int getCurrentNumberOfIterations() {
    return currentNumberOfIterations;
  }

  int getCurrentLevelOfDeterminism() {
    if (totalAssumptions.getValue() == 0) {
      return 100;
    } else {
      return (int) Math.round((deterministicAssumptions.getValue() * 100) / (double)totalAssumptions.getValue());
    }
  }

  @Override
  protected Collection<ValueAnalysisState> postProcessing(ValueAnalysisState successor, CFAEdge edge) {
    // always return a new state (requirement for strengthening states with interpolants)
    if (successor != null) {
      successor = ValueAnalysisState.copyOf(successor);
    }

    return super.postProcessing(successor, edge);
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
    oldState = (ValueAnalysisState)pAbstractState;
    currentNumberOfIterations++;
  }

  @Override
  protected ValueAnalysisState handleFunctionCallEdge(FunctionCallEdge callEdge,
      List<? extends AExpression> arguments, List<? extends AParameterDeclaration> parameters,
      String calledFunctionName) throws UnrecognizedCCodeException {
    ValueAnalysisState newElement = ValueAnalysisState.copyOf(state);

    assert (parameters.size() == arguments.size())
        || callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs();

    // visitor for getting the values of the actual parameters in caller function context
    final ExpressionValueVisitor visitor = getVisitor();

    // get value of actual parameter in caller function context
    for (int i = 0; i < parameters.size(); i++) {
      Value value;
      AExpression exp = arguments.get(i);

      if (exp instanceof JExpression) {
        value = ((JExpression) exp).accept(visitor);
      } else if (exp instanceof CExpression) {
        value = visitor.evaluate((CExpression) exp, (CType) parameters.get(i).getType());
      } else {
        throw new AssertionError("Unknown expression: " + exp);
      }

      AParameterDeclaration param = parameters.get(i);
      String paramName = param.getName();
      Type paramType = param.getType();

      MemoryLocation formalParamName = MemoryLocation.valueOf(calledFunctionName, paramName);

      if (value.isUnknown()) {
        if (isMissingCExpressionInformation(visitor, exp)) {
          addMissingInformation(formalParamName, exp);
        }

        unknownValueHandler.handle(formalParamName, paramType, newElement, visitor);

      } else {
        newElement.assignConstant(formalParamName, value, paramType);
      }

      visitor.reset();

    }

    return newElement;
  }

  @Override
  protected ValueAnalysisState handleBlankEdge(BlankEdge cfaEdge) {
    if (cfaEdge.getSuccessor() instanceof FunctionExitNode) {
      assert "default return".equals(cfaEdge.getDescription())
              || "skipped unnecessary edges".equals(cfaEdge.getDescription())
              || BlankEdge.REPLACEMENT_LABEL.equals(cfaEdge.getDescription());

      // clone state, because will be changed through removing all variables of current function's scope
      state = ValueAnalysisState.copyOf(state);
      state.dropFrame(functionName);
    }

    return state;
  }

  @Override
  protected ValueAnalysisState handleReturnStatementEdge(AReturnStatementEdge returnEdge)
      throws UnrecognizedCCodeException {

    // visitor must use the initial (previous) state, because there we have all information about variables
    ExpressionValueVisitor evv = new ExpressionValueVisitor(state, functionName, machineModel, logger);

    // clone state, because will be changed through removing all variables of current function's scope.
    // The assignment of the global 'state' is safe, because the 'old state'
    // is available in the visitor and is not used for further computation.
    state = ValueAnalysisState.copyOf(state);
    state.dropFrame(functionName);

    AExpression expression = returnEdge.getExpression().orElse(null);
    if (expression == null && returnEdge instanceof CReturnStatementEdge) {
      expression = CIntegerLiteralExpression.ZERO; // this is the default in C
    }

    final FunctionEntryNode functionEntryNode = returnEdge.getSuccessor().getEntryNode();

    final Optional<? extends AVariableDeclaration> optionalReturnVarDeclaration
        = functionEntryNode.getReturnVariable();
    MemoryLocation functionReturnVar = null;

    if (optionalReturnVarDeclaration.isPresent()) {
      functionReturnVar = MemoryLocation.valueOf(optionalReturnVarDeclaration.get().getQualifiedName());
    }

    if (expression != null && functionReturnVar != null) {
      final Type functionReturnType = functionEntryNode.getFunctionDefinition().getType().getReturnType();

      return handleAssignmentToVariable(functionReturnVar,
          functionReturnType,
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

    ValueAnalysisState newElement  = ValueAnalysisState.copyOf(state);

    Optional<? extends AVariableDeclaration> returnVarName = functionReturnEdge.getFunctionEntry().getReturnVariable();
    MemoryLocation functionReturnVar = null;
    if (returnVarName.isPresent()) {
      functionReturnVar = MemoryLocation.valueOf(returnVarName.get().getQualifiedName());
    }

    // expression is an assignment operation, e.g. a = g(b);
    if (exprOnSummary instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement assignExp = ((AFunctionCallAssignmentStatement)exprOnSummary);
      AExpression op1 = assignExp.getLeftHandSide();

      // we expect left hand side of the expression to be a variable

      ExpressionValueVisitor v =
          new ExpressionValueVisitor(newElement, callerFunctionName, machineModel, logger);

      Value newValue = null;
      boolean valueExists = returnVarName.isPresent() && state.contains(functionReturnVar);
      if (valueExists) {
        newValue = state.getValueFor(functionReturnVar);
      }

      // We have to handle Java arrays in a special way, because they are stored as ArrayValue
      // objects
      if (op1 instanceof JArraySubscriptExpression) {
        JArraySubscriptExpression arraySubscriptExpression = (JArraySubscriptExpression) op1;

        ArrayValue assignedArray = getInnerMostArray(arraySubscriptExpression);
        OptionalInt maybeIndex = getIndex(arraySubscriptExpression);

        if (maybeIndex.isPresent() && assignedArray != null && valueExists) {
          assignedArray.setValue(newValue, maybeIndex.getAsInt());

        } else {
          assignUnknownValueToEnclosingInstanceOfArray(arraySubscriptExpression);
        }

      } else {
        // We can handle all types below "casually", so just get the memory location for the
        // left hand side and assign the function's return value

        Optional<MemoryLocation> memLoc = Optional.empty();

        // get memory location for left hand side
        if (op1 instanceof CLeftHandSide) {
          if (valueExists) {
            memLoc = getMemoryLocation((CLeftHandSide) op1, newValue, v);
          } else {
            memLoc = getMemoryLocation((CLeftHandSide) op1, UnknownValue.getInstance(), v);
          }

        } else if (op1 instanceof AIdExpression) {
          if (op1 instanceof JIdExpression && isDynamicField((JIdExpression)op1)
              && valueExists) {
            missingScopedFieldName = true;
            notScopedField = (JIdExpression)op1;
            notScopedFieldValue = newValue;
          } else {
            String op1QualifiedName = ((AIdExpression)op1).getDeclaration().getQualifiedName();
            memLoc = Optional.of(MemoryLocation.valueOf(op1QualifiedName));
          }
        }

        // a* = b(); TODO: for now, nothing is done here, but cloning the current element
        else if (op1 instanceof APointerExpression) {

        } else {
          throw new UnrecognizedCodeException("on function return", summaryEdge, op1);
        }

        // assign the value if a memory location was successfully computed
        if (memLoc.isPresent()) {
          if (!valueExists) {
            unknownValueHandler.handle(memLoc.get(), op1.getExpressionType(), newElement, v);

          } else {
            newElement.assignConstant(memLoc.get(),
                                      newValue,
                                      state.getTypeForMemoryLocation(functionReturnVar));
          }
        }
      }
    }

    if (returnVarName.isPresent()) {
      newElement.forget(functionReturnVar);
    }

    return newElement;
  }

  private Optional<MemoryLocation> getMemoryLocation(
      final CLeftHandSide pExpression,
      final Value pRightHandSideValue,
      final ExpressionValueVisitor pValueVisitor
  ) throws UnrecognizedCCodeException {

    MemoryLocation assignedVarName = pValueVisitor.evaluateMemoryLocation(pExpression);

    if (assignedVarName == null) {
      if (pValueVisitor.hasMissingPointer()) {
        addMissingInformation(pExpression, pRightHandSideValue);
      }
      return Optional.empty();

    } else {
      return Optional.of(assignedVarName);
    }
  }

  private boolean isDynamicField(JIdExpression pIdentifier) {
    final JSimpleDeclaration declaration = pIdentifier.getDeclaration();

    return (declaration instanceof JFieldDeclaration)
        && !((JFieldDeclaration) declaration).isStatic();
  }

  private OptionalInt getIndex(JArraySubscriptExpression pExpression) {
    final ExpressionValueVisitor evv = getVisitor();
    final Value indexValue = pExpression.getSubscriptExpression().accept(evv);

    if (indexValue.isUnknown()) {
      return OptionalInt.empty();
    } else {
      return OptionalInt.of((int) ((NumericValue) indexValue).longValue());
    }
  }

  @Override
  protected ValueAnalysisState handleFunctionSummaryEdge(CFunctionSummaryEdge cfaEdge) throws CPATransferException {
    ValueAnalysisState newState = ValueAnalysisState.copyOf(state);
    AFunctionCall functionCall  = cfaEdge.getExpression();

    if (functionCall instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement assignment = ((AFunctionCallAssignmentStatement)functionCall);
      AExpression leftHandSide = assignment.getLeftHandSide();

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
  protected ValueAnalysisState handleAssumption(AssumeEdge cfaEdge, AExpression expression, boolean truthValue)
    throws UnrecognizedCCodeException {
    return handleAssumption(expression, truthValue);
  }

  private ValueAnalysisState handleAssumption(AExpression expression, boolean truthValue)
      throws UnrecognizedCCodeException {

    totalAssumptions.inc();

    Pair<AExpression, Boolean> simplifiedExpression = simplifyAssumption(expression, truthValue);
    expression = simplifiedExpression.getFirst();
    truthValue = simplifiedExpression.getSecond();

    final ExpressionValueVisitor evv = getVisitor();
    final Type booleanType = getBooleanType(expression);

    // get the value of the expression (either true[1L], false[0L], or unknown[null])
    Value value = getExpressionValue(expression, booleanType, evv);

    if (value.isExplicitlyKnown()) {
      deterministicAssumptions.inc();
    }

    if (!value.isExplicitlyKnown()) {
      ValueAnalysisState element = ValueAnalysisState.copyOf(state);

      AssigningValueVisitor avv =
          new AssigningValueVisitor(
              element,
              truthValue,
              booleanVariables,
              functionName,
              state,
              machineModel,
              logger,
              options);

      if (expression instanceof JExpression && ! (expression instanceof CExpression)) {

        ((JExpression) expression).accept(avv);

        if (avv.hasMissingFieldAccessInformation()) {
          assert missingInformationRightJExpression != null;
          missingAssumeInformation = true;
        }

      } else {
        ((CExpression) expression).accept(avv);
      }

      if (isMissingCExpressionInformation(evv, expression)) {
        missingInformationList.add(new MissingInformation(truthValue, expression));
      }

      return element;

    } else if (representsBoolean(value, truthValue)) {
      // we do not know more than before, and the assumption is fulfilled, so return a copy of the old state
      // we need to return a copy, otherwise precision adjustment might reset too much information, even on the original state
      return ValueAnalysisState.copyOf(state);

    } else {
      // assumption not fulfilled
      return null;
    }
  }

  private Type getBooleanType(AExpression pExpression) {
    if (pExpression instanceof JExpression) {
      return JSimpleType.getBoolean();
    } else if (pExpression instanceof CExpression) {
      return CNumericTypes.INT;

    } else {
      throw new AssertionError("Unhandled expression type " + pExpression.getClass());
    }
  }

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
  private boolean representsBoolean(Value value, boolean bool) {
    if (value instanceof BooleanValue) {
      return ((BooleanValue) value).isTrue() == bool;

    } else if (value.isNumericValue()) {
      return ((NumericValue) value).equals(new NumericValue(bool ? 1L : 0L));

    } else {
      return false;
    }
  }


  @Override
  protected ValueAnalysisState handleDeclarationEdge(ADeclarationEdge declarationEdge, ADeclaration declaration)
      throws UnrecognizedCCodeException {

    if (!(declaration instanceof AVariableDeclaration) || !isTrackedType(declaration.getType())) {
      // nothing interesting to see here, please move along
      return state;
    }

    ValueAnalysisState newElement = ValueAnalysisState.copyOf(state);
    AVariableDeclaration decl = (AVariableDeclaration) declaration;
    Type declarationType = decl.getType();

    // get the variable name in the declarator
    String varName = decl.getName();

    Value initialValue = getDefaultInitialValue(decl);

    // get initializing statement
    AInitializer init = decl.getInitializer();

    // handle global variables
    if (decl.isGlobal()) {
      if (decl instanceof JFieldDeclaration && !((JFieldDeclaration) decl).isStatic()) {
        missingFieldVariableObject = true;
        javaNonStaticVariables.add(varName);
      }
    }

    MemoryLocation memoryLocation;

    // assign initial value if necessary
    if (decl.isGlobal()) {
      memoryLocation = MemoryLocation.valueOf(varName);
    } else {
      memoryLocation = MemoryLocation.valueOf(functionName, varName);
    }

    if (addressedVariables.contains(decl.getQualifiedName())
        && declarationType instanceof CType
        && ((CType) declarationType).getCanonicalType() instanceof CPointerType) {
      ValueAnalysisState.addToBlacklist(memoryLocation);
    }

    if (init instanceof AInitializerExpression) {
      ExpressionValueVisitor evv = getVisitor();
      AExpression exp = ((AInitializerExpression) init).getExpression();
      initialValue = getExpressionValue(exp, declarationType, evv);

      if (isMissingCExpressionInformation(evv, exp)) {
        addMissingInformation(memoryLocation, exp);
      }
    }

    if (isTrackedType(declarationType)) {
      if (missingFieldVariableObject) {
        fieldNameAndInitialValue = Pair.of(varName, initialValue);

      } else if (missingInformationRightJExpression != null) {
        missingInformationLeftJVariable = memoryLocation.getAsSimpleString();
      }
    } else {
      // If variable not tracked, its Object is irrelevant
      missingFieldVariableObject = false;
    }

    if (initialValue.isUnknown()) {
      unknownValueHandler.handle(memoryLocation, declarationType, newElement, getVisitor());
    } else {
      newElement.assignConstant(memoryLocation, initialValue, declarationType);
    }

    return newElement;
  }

  private Value getDefaultInitialValue(AVariableDeclaration pDeclaration) {
    final boolean defaultBooleanValue = false;
    final long defaultNumericValue = 0;

    if (pDeclaration.isGlobal()) {
      Type declarationType = pDeclaration.getType();

      if (isComplexJavaType(declarationType)) {
        return NullValue.getInstance();

      } else if (declarationType instanceof JSimpleType) {
        JBasicType basicType = ((JSimpleType) declarationType).getType();

        switch (basicType) {
          case BOOLEAN:
            return BooleanValue.valueOf(defaultBooleanValue);
          case BYTE:
          case CHAR:
          case SHORT:
          case INT:
          case LONG:
          case FLOAT:
          case DOUBLE:
            return new NumericValue(defaultNumericValue);
          case UNSPECIFIED:
            return UnknownValue.getInstance();
          default:
            throw new AssertionError("Impossible type for declaration: " + basicType);
        }
      }
    }

    return UnknownValue.getInstance();
  }

  private boolean isComplexJavaType(Type pType) {
    return pType instanceof JClassOrInterfaceType
        || pType instanceof JArrayType;
  }

  private boolean isMissingCExpressionInformation(ExpressionValueVisitor pEvv,
      ARightHandSide pExp) {

    return pExp instanceof CExpression && (pEvv.hasMissingPointer());
  }

  @Override
  protected ValueAnalysisState handleStatementEdge(AStatementEdge cfaEdge, AStatement expression)
    throws UnrecognizedCodeException {

    if (expression instanceof CFunctionCall) {
      CFunctionCall functionCall = (CFunctionCall) expression;
      CFunctionCallExpression functionCallExp = functionCall.getFunctionCallExpression();
      CExpression fn = functionCallExp.getFunctionNameExpression();

      if (fn instanceof CIdExpression) {
        String func = ((CIdExpression)fn).getName();
        if (UNSUPPORTED_FUNCTIONS.containsKey(func)) {
          throw new UnsupportedCCodeException(UNSUPPORTED_FUNCTIONS.get(func), cfaEdge, fn);

        } else if (func.equals("free")) {
          return handleCallToFree(functionCall);

        } else if (expression instanceof CFunctionCallAssignmentStatement) {

          return handleFunctionAssignment((CFunctionCallAssignmentStatement) expression);
        }
      }
    }

    // expression is a binary operation, e.g. a = b;

    if (expression instanceof AAssignment) {
      return handleAssignment((AAssignment)expression, cfaEdge);

    // external function call - do nothing
    } else if (expression instanceof AFunctionCallStatement) {

    // there is such a case
    } else if (expression instanceof AExpressionStatement) {

    } else {
      throw new UnrecognizedCodeException("Unknown statement", cfaEdge, expression);
    }

    return state;
  }

  private ValueAnalysisState handleFunctionAssignment(
      CFunctionCallAssignmentStatement pFunctionCallAssignment)
      throws UnrecognizedCCodeException {

    final CFunctionCallExpression functionCallExp = pFunctionCallAssignment.getFunctionCallExpression();
    final CLeftHandSide leftSide = pFunctionCallAssignment.getLeftHandSide();
    final CType leftSideType = leftSide.getExpressionType();
    final ExpressionValueVisitor evv = getVisitor();

    ValueAnalysisState newElement = ValueAnalysisState.copyOf(state);

    Value newValue = evv.evaluate(functionCallExp, leftSideType);

    final Optional<MemoryLocation> memLoc = getMemoryLocation(leftSide, newValue, evv);

    if (memLoc.isPresent()) {
      if (!newValue.isUnknown()) {
        newElement.assignConstant(memLoc.get(), newValue, leftSideType);

      } else {
        unknownValueHandler.handle(memLoc.get(), leftSideType, newElement, evv);
      }
    }

    return newElement;
  }

  private ValueAnalysisState handleCallToFree(CFunctionCall pExpression) {
    // Needed for erasing values
    missingInformationList.add(new MissingInformation(pExpression.getFunctionCallExpression()));

    return state;
  }

  private ValueAnalysisState handleAssignment(AAssignment assignExpression, CFAEdge cfaEdge)
      throws UnrecognizedCodeException {
    AExpression op1    = assignExpression.getLeftHandSide();
    ARightHandSide op2 = assignExpression.getRightHandSide();

    if (!isTrackedType(op1.getExpressionType())) {
      return state;
    }

    if (op1 instanceof AIdExpression) {
      /*
       * Assignment of the form
       *  a = ...
       */

        if (op1 instanceof JIdExpression && isDynamicField((JIdExpression) op1)) {
          missingScopedFieldName = true;
          notScopedField = (JIdExpression) op1;
        }

        MemoryLocation memloc = getMemoryLocation((AIdExpression) op1);

        return handleAssignmentToVariable(memloc, op1.getExpressionType(), op2, getVisitor());
    } else if (op1 instanceof APointerExpression) {
      // *a = ...

      if (isRelevant(op1, op2)) {
        missingInformationList.add(new MissingInformation(op1, op2));
      }

    } else if (op1 instanceof CFieldReference) {

      ExpressionValueVisitor v = getVisitor();

      MemoryLocation memLoc = v.evaluateMemoryLocation((CFieldReference) op1);

      if (v.hasMissingPointer() && isRelevant(op1, op2)) {
        missingInformationList.add(new MissingInformation(op1, op2));
      }

      if (memLoc != null) {
        return handleAssignmentToVariable(memLoc, op1.getExpressionType(), op2, v);
      }

    } else if (op1 instanceof AArraySubscriptExpression) {
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
      } else if (op1 instanceof JArraySubscriptExpression) {
        JArraySubscriptExpression arrayExpression = (JArraySubscriptExpression) op1;
        ExpressionValueVisitor evv = getVisitor();

        ArrayValue arrayToChange = getInnerMostArray(arrayExpression);
        Value maybeIndex = arrayExpression.getSubscriptExpression().accept(evv);

        if (arrayToChange == null || maybeIndex.isUnknown()) {
          assignUnknownValueToEnclosingInstanceOfArray(arrayExpression);

        } else {
          long concreteIndex = ((NumericValue) maybeIndex).longValue();

          if (concreteIndex < 0 || concreteIndex >= arrayToChange.getArraySize()) {
            throw new UnrecognizedCodeException("Invalid index " + concreteIndex + " for array "
                + arrayToChange, cfaEdge);
          }

          // changes array value in old state
          handleAssignmentToArray(arrayToChange, (int) concreteIndex, op2);
          return ValueAnalysisState.copyOf(state);
        }
      }
    } else {
      throw new UnrecognizedCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
    }

    return state; // the default return-value is the old state
  }

  private boolean isTrackedType(Type pType) {
    return !(pType instanceof JType)
        || options.trackJavaArrayValues
        || !(pType instanceof JArrayType);
  }

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
  private ValueAnalysisState handleAssignmentToVariable(
      MemoryLocation assignedVar, final Type lType, ARightHandSide exp, ExpressionValueVisitor visitor)
          throws UnrecognizedCCodeException {
    // here we clone the state, because we get new information or must forget it.
    ValueAnalysisState newElement = ValueAnalysisState.copyOf(state);
    handleAssignmentToVariable(newElement, assignedVar, lType, exp, visitor);
    return newElement;
  }

  /** This method analyses the expression with the visitor and assigns the value to lParam
   *  to the given value Analysis state.
   */
  private void handleAssignmentToVariable(ValueAnalysisState newElement,
      MemoryLocation assignedVar, final Type lType, ARightHandSide exp, ExpressionValueVisitor visitor)
      throws UnrecognizedCCodeException {

    // c structs have to be handled seperatly, because we do not have a value object representing structs
    if (lType instanceof CType) {
      CType canonicaltype = ((CType) lType).getCanonicalType();
      if (canonicaltype instanceof CCompositeType
          && ((CCompositeType) canonicaltype).getKind() == ComplexTypeKind.STRUCT
          && exp instanceof CLeftHandSide) {
        handleAssignmentToStruct(newElement, assignedVar, (CCompositeType) canonicaltype, (CExpression) exp, visitor);
        return;
      }
    }

    Value value;
    if (exp instanceof JRightHandSide) {
       value = visitor.evaluate((JRightHandSide) exp, (JType) lType);
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

    if (visitor.hasMissingFieldAccessInformation()) {
      // This may happen if an object of class is created which could not be parsed,
      // In  such a case, forget about it
      if (!value.isUnknown()) {
        newElement.forget(assignedVar);
        return;
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

      // if there is no information left to evaluate but the value is unknown, we assign a symbolic
      // identifier to keep track of the variable.
      if (value.isUnknown()) {
        unknownValueHandler.handle(assignedVar, lType, newElement, visitor);

      } else {
        newElement.assignConstant(assignedVar, value, lType);
      }
    }
  }

  /**
   *
   * This method transforms the assignment of the struct into assignments of its respective
   * field references and assigns them to the given value state.
   *
   */
  private void handleAssignmentToStruct(ValueAnalysisState pNewElement,
      MemoryLocation pAssignedVar,
      CCompositeType pLType, CExpression pExp,
      ExpressionValueVisitor pVisitor) throws UnrecognizedCCodeException {

    int offset = 0;
    for (CCompositeType.CCompositeTypeMemberDeclaration memberType : pLType.getMembers()) {
      MemoryLocation assignedField = createFieldMemoryLocation(pAssignedVar, offset);
      CExpression owner = null;

      owner = pExp;

      CExpression fieldReference =
          new CFieldReference(pExp.getFileLocation(), memberType.getType(), memberType.getName(), owner, false);
      handleAssignmentToVariable(pNewElement, assignedField, memberType.getType(), fieldReference, pVisitor);

      offset = offset + machineModel.getSizeof(memberType.getType());
    }
  }

  private MemoryLocation createFieldMemoryLocation(MemoryLocation pStruct, int pOffset) {

    long baseOffset = pStruct.isReference() ? pStruct.getOffset() : 0;

    if (pStruct.isOnFunctionStack()) {
      return MemoryLocation.valueOf(
          pStruct.getFunctionName(), pStruct.getIdentifier(), baseOffset + pOffset);
    } else {
      return MemoryLocation.valueOf(pStruct.getIdentifier(), baseOffset + pOffset);
    }
  }

  private void addMissingInformation(MemoryLocation pMemLoc, ARightHandSide pExp) {
    if (pExp instanceof CExpression) {

      missingInformationList.add(new MissingInformation(pMemLoc,
          (CExpression) pExp));
    }
  }

  private void addMissingInformation(CLeftHandSide pOp1, Value pValue) {
    missingInformationList.add(new MissingInformation(pOp1, pValue));

  }

  /**
   * Returns the {@link ArrayValue} object that represents the innermost array of the given
   * {@link JArraySubscriptExpression}.
   *
   * @param pArraySubscriptExpression the subscript expression to get the inner most array of
   * @return <code>null</code> if the complete array or a part significant for the given array
   *    subscript expression is unknown, the <code>ArrayValue</code> representing the innermost
   *    array, otherwise
   */
  private @Nullable ArrayValue getInnerMostArray(JArraySubscriptExpression pArraySubscriptExpression) {
    JExpression arrayExpression = pArraySubscriptExpression.getArrayExpression();

    if (arrayExpression instanceof JIdExpression) {
      JSimpleDeclaration arrayDeclaration = ((JIdExpression) arrayExpression).getDeclaration();

      if (arrayDeclaration != null) {
        String idName = arrayDeclaration.getQualifiedName();

        if (state.contains(idName)) {
          Value idValue = state.getValueFor(idName);
          if (idValue.isExplicitlyKnown()) {
            return (ArrayValue) idValue;
          }
        }
      }

      return null;
    } else {
      final JArraySubscriptExpression arraySubscriptExpression = (JArraySubscriptExpression) arrayExpression;
      // the array enclosing the array specified in the given array subscript expression
      ArrayValue enclosingArray = getInnerMostArray(arraySubscriptExpression);

      OptionalInt maybeIndex = getIndex(arraySubscriptExpression);
      int index;

      if (maybeIndex.isPresent() && enclosingArray != null) {

        index = maybeIndex.getAsInt();

      } else {
        return null;
      }

      if (index >= enclosingArray.getArraySize() || index < 0) {
        return null;
      }

      return (ArrayValue) enclosingArray.getValueAt(index);
    }
  }

  private void handleAssignmentToArray(ArrayValue pArray, int index, ARightHandSide exp) {
    assert exp instanceof JExpression;

    pArray.setValue(((JExpression) exp).accept(getVisitor()), index);
  }

  private void assignUnknownValueToEnclosingInstanceOfArray(JArraySubscriptExpression pArraySubscriptExpression) {

    JExpression enclosingExpression = pArraySubscriptExpression.getArrayExpression();

    if (enclosingExpression instanceof JIdExpression) {
      JIdExpression idExpression = (JIdExpression) enclosingExpression;
      MemoryLocation memLoc = getMemoryLocation(idExpression);
      Value unknownValue = UnknownValue.getInstance();

      state.assignConstant(memLoc, unknownValue, JSimpleType.getUnspecified());

    } else {
      JArraySubscriptExpression enclosingSubscriptExpression = (JArraySubscriptExpression) enclosingExpression;
      ArrayValue enclosingArray = getInnerMostArray(enclosingSubscriptExpression);
      OptionalInt maybeIndex = getIndex(enclosingSubscriptExpression);

      if (maybeIndex.isPresent() && enclosingArray != null) {
        enclosingArray.setValue(UnknownValue.getInstance(), maybeIndex.getAsInt());

      }
      // if the index of unknown array in the enclosing array is also unknown, we assign unknown at this array's
      // position in the enclosing array
      else {
        assignUnknownValueToEnclosingInstanceOfArray(enclosingSubscriptExpression);
      }
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(transferStatistics);
    statsCollection.add(constraintsStrengthenOperator);
  }

  private class  FieldAccessExpressionValueVisitor extends ExpressionValueVisitor {
    private final RTTState jortState;

    public FieldAccessExpressionValueVisitor(RTTState pJortState, ValueAnalysisState pState) {
      super(pState, functionName, machineModel, logger);
      jortState = pJortState;
    }

    @Override
    public Value visit(JBinaryExpression binaryExpression) {
      return super.visit(binaryExpression);
    }

    private String handleIdExpression(JIdExpression expr) {

      JSimpleDeclaration decl = expr.getDeclaration();

      if (decl == null) {
        return null;
      }

      NameProvider nameProvider = NameProvider.getInstance();
      String objectScope = nameProvider.getObjectScope(jortState, functionName, expr);

      return nameProvider.getScopedVariableName(decl, functionName, objectScope);
    }

    @Override
    public Value visit(JIdExpression idExp) {

      String varName = handleIdExpression(idExp);

      if (readableState.contains(varName)) {
        return readableState.getValueFor(varName);
      } else {
        return Value.UnknownValue.getInstance();
      }
    }
  }

  private Value getExpressionValue(AExpression expression, final Type type, ExpressionValueVisitor evv)
      throws UnrecognizedCCodeException {
    if (!isTrackedType(type)) {
      return UnknownValue.getInstance();
    }

    if (expression instanceof JRightHandSide) {

      final Value value = evv.evaluate((JRightHandSide) expression, (JType) type);

      if (evv.hasMissingFieldAccessInformation()) {
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

    for (AbstractState ae : elements) {
      if (ae instanceof RTTState) {
        result.clear();
        for (ValueAnalysisState state : toStrengthen) {
          super.setInfo(element, precision, cfaEdge);
          Collection<ValueAnalysisState> ret = strengthen((RTTState)ae, cfaEdge);
          if (ret == null) {
            result.add(state);
          } else {
            result.addAll(ret);
          }
        }
        toStrengthen.clear();
        toStrengthen.addAll(result);
      } else if (ae instanceof AutomatonState) {
        result.clear();
        for (ValueAnalysisState state : toStrengthen) {
          super.setInfo(element, precision, cfaEdge);
          AutomatonState autoState = (AutomatonState) ae;
          Collection<ValueAnalysisState> ret = strengthenAutomatonAssume(autoState, state, cfaEdge);
          if (ret == null) {
            result.add(state);
          } else {
            result.addAll(ret);
          }
        }
        toStrengthen.clear();
        toStrengthen.addAll(result);
      } else if (ae instanceof ConstraintsState) {
        result.clear();

        for (ValueAnalysisState state : toStrengthen) {
          super.setInfo(element, precision, cfaEdge);
          Collection<ValueAnalysisState> ret =
              constraintsStrengthenOperator.strengthen((ValueAnalysisState) element, (ConstraintsState) ae, cfaEdge);

          if (ret == null) {
            result.add(state);
          } else {
            result.addAll(ret);
          }
        }
        toStrengthen.clear();
        toStrengthen.addAll(result);
      } else if (ae instanceof PointerState) {

        CFAEdge edge = cfaEdge;

        ARightHandSide rightHandSide = getRightHandSide(edge);
        ALeftHandSide leftHandSide = getLeftHandSide(edge);
        Type leftHandType = getLeftHandType(edge);
        String leftHandVariable = getLeftHandVariable(edge);
        PointerState pointerState = (PointerState) ae;

        result.clear();

        for (ValueAnalysisState state : toStrengthen) {
          super.setInfo(element, precision, cfaEdge);
          ValueAnalysisState newState =
              strengthenWithPointerInformation(state, pointerState, rightHandSide, leftHandType, leftHandSide, leftHandVariable);
          result.add(newState);
        }
        toStrengthen.clear();
        toStrengthen.addAll(result);
      }

    }

    // Do post processing
    final Collection<AbstractState> postProcessedResult = new ArrayList<>(result.size());
    for (ValueAnalysisState rawResult : result) {
      // The original state has already been post-processed
      if (rawResult == element) {
        postProcessedResult.add(element);
      } else {
        postProcessedResult.addAll(postProcessing(rawResult, cfaEdge));
      }
    }

    super.resetInfo();
    oldState = null;

    return postProcessedResult;
  }

  private ValueAnalysisState strengthenWithPointerInformation(
      ValueAnalysisState pValueState,
      PointerState pPointerInfo,
      ARightHandSide pRightHandSide,
      Type pLeftHandType,
      ALeftHandSide pLeftHandSide,
      String pLeftHandVariable) throws UnrecognizedCCodeException {

    ValueAnalysisState newState = pValueState;

    Value value = UnknownValue.getInstance();
    MemoryLocation target = null;
    if (pLeftHandVariable != null) {
      target = MemoryLocation.valueOf(pLeftHandVariable);
    }
    Type type = pLeftHandType;
    boolean shouldAssign = false;

    if (target == null && pLeftHandSide instanceof CPointerExpression) {
      CPointerExpression pointerExpression = (CPointerExpression) pLeftHandSide;
      CExpression addressExpression = pointerExpression.getOperand();

      LocationSet fullSet = PointerTransferRelation.asLocations(addressExpression, pPointerInfo);

      if (fullSet instanceof ExplicitLocationSet) {
        ExplicitLocationSet explicitSet = (ExplicitLocationSet) fullSet;
        if (explicitSet.getSize() == 1) {
          MemoryLocation variable = explicitSet.iterator().next();
          LocationSet pointsToSet = pPointerInfo.getPointsToSet(variable);
          if (pointsToSet instanceof ExplicitLocationSet) {
            ExplicitLocationSet explicitPointsToSet = (ExplicitLocationSet) pointsToSet;
            Iterator<MemoryLocation> pointsToIterator = explicitPointsToSet.iterator();
            MemoryLocation otherVariable = pointsToIterator.next();
            if (!pointsToIterator.hasNext()) {
              target = otherVariable;
              shouldAssign = true;
            }
          }
        }
      }

    }

    if (pRightHandSide instanceof CPointerExpression) {
      if (target == null) {
        return pValueState;
      }

      CPointerExpression rhs = (CPointerExpression) pRightHandSide;
      CExpression addressExpression = rhs.getOperand();

      LocationSet fullSet = PointerTransferRelation.asLocations(addressExpression, pPointerInfo);

      if (fullSet instanceof ExplicitLocationSet) {
        ExplicitLocationSet explicitSet = (ExplicitLocationSet) fullSet;
        if (explicitSet.getSize() == 1) {
          MemoryLocation variable = explicitSet.iterator().next();
          CType variableType = rhs.getExpressionType().getCanonicalType();
          LocationSet pointsToSet = pPointerInfo.getPointsToSet(variable);

          if (pointsToSet instanceof ExplicitLocationSet) {
            ExplicitLocationSet explicitPointsToSet = (ExplicitLocationSet) pointsToSet;
            Iterator<MemoryLocation> pointsToIterator = explicitPointsToSet.iterator();
            MemoryLocation otherVariableLocation = pointsToIterator.next();
            if (!pointsToIterator.hasNext()) {

              Type otherVariableType = pValueState.getTypeForMemoryLocation(otherVariableLocation);
              if (otherVariableType != null) {
                Value otherVariableValue = pValueState.getValueFor(otherVariableLocation);
                if (otherVariableValue != null) {
                  if (variableType.equals(otherVariableType)
                      || (variableType.equals(CNumericTypes.FLOAT)
                          && otherVariableType.equals(CNumericTypes.UNSIGNED_INT)
                          && otherVariableValue.isExplicitlyKnown()
                          && Long.valueOf(0)
                              .equals(otherVariableValue.asLong(CNumericTypes.UNSIGNED_INT)))) {
                    value = otherVariableValue;
                    shouldAssign = true;
                  }
                }
              }
            }
          }
        }
      }
    }

    if (target != null && type != null && shouldAssign) {
      newState = ValueAnalysisState.copyOf(pValueState);
      newState.assignConstant(target, value, type);
    }

    return newState;
  }

  private Type getLeftHandType(CFAEdge pEdge) {
    if (pEdge instanceof ADeclarationEdge) {
      ADeclarationEdge declarationEdge = (ADeclarationEdge) pEdge;
      if (declarationEdge.getDeclaration() instanceof AVariableDeclaration) {
        AVariableDeclaration variableDeclaration = (AVariableDeclaration) declarationEdge.getDeclaration();
        return variableDeclaration.getType();
      }
    } else {
      ALeftHandSide lhs = getLeftHandSide(pEdge);
      if (lhs instanceof AIdExpression) {
        return ((AIdExpression) lhs).getDeclaration().getType();
      }
    }
    return null;
  }

  private String getLeftHandVariable(CFAEdge pEdge) {
    if (pEdge instanceof ADeclarationEdge) {
      ADeclarationEdge declarationEdge = (ADeclarationEdge) pEdge;
      if (declarationEdge.getDeclaration() instanceof AVariableDeclaration) {
        AVariableDeclaration variableDeclaration = (AVariableDeclaration) declarationEdge.getDeclaration();
        return variableDeclaration.getQualifiedName();
      }
    } else {
      ALeftHandSide lhs = getLeftHandSide(pEdge);
      if (lhs instanceof AIdExpression) {
        return ((AIdExpression) lhs).getDeclaration().getQualifiedName();
      }
    }
    return null;
  }

  private static ALeftHandSide getLeftHandSide(CFAEdge pEdge) {
    if (pEdge instanceof AStatementEdge) {
      AStatementEdge statementEdge = (AStatementEdge) pEdge;
      if (statementEdge.getStatement() instanceof AAssignment) {
        AAssignment assignment = (AAssignment)statementEdge.getStatement();
        return assignment.getLeftHandSide();
      }
    } else if (pEdge instanceof FunctionCallEdge) {
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) pEdge;
      AFunctionCall functionCall = functionCallEdge.getSummaryEdge().getExpression();
      if (functionCall instanceof AFunctionCallAssignmentStatement) {
        AFunctionCallAssignmentStatement assignment = (AFunctionCallAssignmentStatement) functionCall;
        return assignment.getLeftHandSide();
      }
    }
    return null;
  }

  private static CRightHandSide getRightHandSide(CFAEdge pEdge) {
    if (pEdge instanceof CDeclarationEdge) {
      CDeclarationEdge declarationEdge = (CDeclarationEdge) pEdge;
      if (declarationEdge.getDeclaration() instanceof CVariableDeclaration) {
        CVariableDeclaration variableDeclaration = (CVariableDeclaration) declarationEdge.getDeclaration();
        CInitializer initializer = variableDeclaration.getInitializer();
        if (initializer instanceof CInitializerExpression) {
          return ((CInitializerExpression) initializer).getExpression();
        }
      }
    } else if (pEdge instanceof CStatementEdge) {
      CStatementEdge statementEdge = (CStatementEdge) pEdge;
      if (statementEdge.getStatement() instanceof CAssignment) {
        CAssignment assignment = (CAssignment)statementEdge.getStatement();
        return assignment.getRightHandSide();
      }
    } else if (pEdge instanceof CFunctionCallEdge) {
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) pEdge;
      CFunctionCall functionCall = functionCallEdge.getSummaryEdge().getExpression();
      if (functionCall instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement assignment = (CFunctionCallAssignmentStatement) functionCall;
        return assignment.getRightHandSide();
      }
    }
    return null;
  }

  private Collection<ValueAnalysisState> strengthenAutomatonAssume(AutomatonState pAutomatonState, ValueAnalysisState pState, CFAEdge pCfaEdge) throws CPATransferException {

    ValueAnalysisState state = pState;

    for (AExpression assumption : pAutomatonState.getAssumptions()) {
      state = handleAssumption(assumption, true);

      if (state == null) {
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

  private Collection<ValueAnalysisState> strengthen(RTTState rttState, CFAEdge edge) {

    ValueAnalysisState newElement = ValueAnalysisState.copyOf(oldState);

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

      boolean truthAssumption = ((AssumeEdge)edge).getTruthAssumption();
      if (value == null || !value.isExplicitlyKnown()) {
        return null;
      } else if (representsBoolean(value, truthAssumption)) {
        return Collections.singleton(newElement);
      } else {
        return new HashSet<>();
      }
    } else if (missingInformationRightJExpression != null) {

      Value value = handleMissingInformationRightJExpression(rttState);

      if (!value.isUnknown()) {
        newElement.assignConstant(missingInformationLeftJVariable, value);
        missingInformationRightJExpression = null;
        missingInformationLeftJVariable = null;
        return Collections.singleton(newElement);
      } else {
        if (missingInformationLeftJVariable != null) {
          newElement.forget(missingInformationLeftJVariable);
        }
        missingInformationRightJExpression = null;
        missingInformationLeftJVariable = null;
        return Collections.singleton(newElement);
      }
    }
    return null;
  }

  private String getRTTScopedVariableName(String fieldName, String uniqueObject) {
    return  uniqueObject + "::"+ fieldName;
  }

  private Value handleMissingInformationRightJExpression(RTTState pJortState) {
    return missingInformationRightJExpression.accept(
        new FieldAccessExpressionValueVisitor(pJortState, oldState));
  }

  private ValueAnalysisState handleNotScopedVariable(RTTState rttState, ValueAnalysisState newElement) {

   String objectScope = NameProvider.getInstance()
                                    .getObjectScope(rttState, functionName, notScopedField);

   if (objectScope != null) {

     String scopedFieldName = getRTTScopedVariableName(notScopedField.getName(), objectScope);

     Value value = notScopedFieldValue;
     if (missingInformationRightJExpression != null) {
       value = handleMissingInformationRightJExpression(rttState);
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

  /** returns an initialized, empty visitor */
  private ExpressionValueVisitor getVisitor() {
    return new ExpressionValueVisitor(state, functionName, machineModel, logger);
  }
}
