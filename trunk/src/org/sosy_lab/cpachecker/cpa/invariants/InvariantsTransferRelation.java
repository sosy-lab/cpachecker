// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundIntervalFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Variable;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerTransferRelation;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.CFAEdgeUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

class InvariantsTransferRelation extends SingleEdgeTransferRelation {

  // Set of functions that may not appear in the source code
  // the value of the map entry is the explanation for the user
  private static final ImmutableMap<String, String> UNSUPPORTED_FUNCTIONS =
      ImmutableMap.of("memcpy", "memcpy", "memmove", "memmove", "memset", "memset");

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR =
      new CollectVarsVisitor<>();

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  private final CompoundIntervalFormulaManager compoundIntervalFormulaManager;

  private final MachineModel machineModel;

  private final EdgeAnalyzer edgeAnalyzer;

  private final boolean allowOverapproximationOfUnsupportedFeatures;

  private final boolean usePointerAliasStrengthening;

  private final Optional<VariableClassification> variableClassification;

  public InvariantsTransferRelation(
      CompoundIntervalManagerFactory pCompoundIntervalManagerFactory,
      MachineModel pMachineModel,
      boolean pAllowOverapproximationOfUnsupportedFeatures,
      boolean pUsePointerAliasStrengthening,
      Optional<VariableClassification> pVariableClassification) {
    compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
    machineModel = pMachineModel;
    edgeAnalyzer = new EdgeAnalyzer(compoundIntervalManagerFactory, machineModel);
    compoundIntervalFormulaManager =
        new CompoundIntervalFormulaManager(compoundIntervalManagerFactory);
    allowOverapproximationOfUnsupportedFeatures = pAllowOverapproximationOfUnsupportedFeatures;
    usePointerAliasStrengthening = pUsePointerAliasStrengthening;
    variableClassification = Objects.requireNonNull(pVariableClassification);
  }

  private CompoundIntervalManager getCompoundIntervalManager(TypeInfo pTypeInfo) {
    return compoundIntervalManagerFactory.createCompoundIntervalManager(pTypeInfo);
  }

  private NumeralFormula<CompoundInterval> allPossibleValues(Type pType) {
    TypeInfo typeInfo = BitVectorInfo.from(machineModel, pType);
    return InvariantsFormulaManager.INSTANCE.asConstant(
        typeInfo, getCompoundIntervalManager(typeInfo).allPossibleValues());
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pEdge) throws CPATransferException {

    InvariantsState state = (InvariantsState) pState;
    InvariantsPrecision precision = (InvariantsPrecision) pPrecision;

    final AtomicBoolean overflowDetected = new AtomicBoolean(false);
    OverflowEventHandler overflowEventHandler = () -> overflowDetected.set(true);

    if (compoundIntervalManagerFactory instanceof CompoundBitVectorIntervalManagerFactory) {
      CompoundBitVectorIntervalManagerFactory compoundBitVectorIntervalManagerFactory =
          (CompoundBitVectorIntervalManagerFactory) compoundIntervalManagerFactory;
      compoundBitVectorIntervalManagerFactory.addOverflowEventHandler(overflowEventHandler);
    }

    state = getSuccessor(pEdge, precision, state);

    if (compoundIntervalManagerFactory instanceof CompoundBitVectorIntervalManagerFactory) {
      CompoundBitVectorIntervalManagerFactory compoundBitVectorIntervalManagerFactory =
          (CompoundBitVectorIntervalManagerFactory) compoundIntervalManagerFactory;
      compoundBitVectorIntervalManagerFactory.removeOverflowEventHandler(overflowEventHandler);
    }

    if (state == null) {
      return ImmutableSet.of();
    }

    state = state.updateAbstractionState(precision, pEdge);

    if (overflowDetected.get()) {
      state = state.overflowDetected();
    }

    if (!allowOverapproximationOfUnsupportedFeatures
        && state.overapproximatesUnsupportedFeature()) {
      throw new UnsupportedCodeException(
          "Over-approximation of unsupported features is switched off", pEdge);
    }

    return Collections.singleton(state);
  }

  private InvariantsState getSuccessor(
      CFAEdge pEdge, InvariantsPrecision pPrecision, InvariantsState pState)
      throws UnrecognizedCFAEdgeException, UnrecognizedCodeException {
    InvariantsState state = pState.setTypes(edgeAnalyzer.getInvolvedVariableTypes(pEdge));
    switch (pEdge.getEdgeType()) {
      case BlankEdge:
        break;
      case FunctionReturnEdge:
        state = handleFunctionReturn(state, (CFunctionReturnEdge) pEdge, pPrecision);
        break;
      case ReturnStatementEdge:
        state = handleReturnStatement(state, (CReturnStatementEdge) pEdge);
        break;
      case AssumeEdge:
        state = handleAssume(state, (CAssumeEdge) pEdge);
        break;
      case DeclarationEdge:
        state = handleDeclaration(state, (CDeclarationEdge) pEdge);
        break;
      case FunctionCallEdge:
        state = handleFunctionCall(state, (CFunctionCallEdge) pEdge);
        break;
      case StatementEdge:
        state = handleStatement(state, (CStatementEdge) pEdge, pPrecision);
        break;
      default:
        throw new UnrecognizedCFAEdgeException(pEdge);
    }
    if (state != null && pPrecision != null && !pPrecision.isRelevant(pEdge)) {
      state = state.clear();
    }
    return state;
  }

  private InvariantsState handleAssume(InvariantsState pState, AssumeEdge pEdge)
      throws UnrecognizedCodeException {
    return handleAssume(pState, pEdge, getExpressionToFormulaVisitor(pEdge, pState));
  }

  private InvariantsState handleAssume(
      InvariantsState pState,
      AssumeEdge pEdge,
      ExpressionToFormulaVisitor pExpressionToFormulaVisitor)
      throws UnrecognizedCodeException {
    AExpression expression = pEdge.getExpression();

    // Create a formula representing the edge expression

    BooleanFormula<CompoundInterval> assumption = null;
    if (expression instanceof CExpression) {
      NumeralFormula<CompoundInterval> expressionFormula =
          ((CExpression) expression).accept(pExpressionToFormulaVisitor);
      assumption = compoundIntervalFormulaManager.fromNumeral(expressionFormula);
    } else if (expression instanceof JExpression) {
      NumeralFormula<CompoundInterval> expressionFormula =
          ((JExpression) expression).accept(pExpressionToFormulaVisitor);
      assumption = compoundIntervalFormulaManager.fromNumeral(expressionFormula);
    } else {
      return pState;
    }

    if (!pEdge.getTruthAssumption()) {
      assumption = compoundIntervalFormulaManager.logicalNot(assumption);
    }

    return handleAssumption(pState, assumption);
  }

  private InvariantsState handleAssumption(
      InvariantsState pState, BooleanFormula<CompoundInterval> pAssumption) {
    /*
     * Assume the state of the expression:
     */
    InvariantsState result = pState.assume(pAssumption);
    return result;
  }

  private InvariantsState handleDeclaration(InvariantsState pElement, CDeclarationEdge pEdge)
      throws UnrecognizedCodeException {
    if (!(pEdge.getDeclaration() instanceof CVariableDeclaration)) {
      return pElement;
    }

    CVariableDeclaration decl = (CVariableDeclaration) pEdge.getDeclaration();
    if (decl.getType().isIncomplete()) {
      // Variables of such types cannot store values, only their address can be taken.
      // We can ignore them.
      return pElement;
    }

    // Ignore unsupported types
    if (!BitVectorInfo.isSupported(decl.getType())) {
      return pElement;
    }

    MemoryLocation varName = MemoryLocation.parseExtendedQualifiedName(decl.getName());
    if (!decl.isGlobal()) {
      varName =
          MemoryLocationExtractor.scope(decl.getName(), pEdge.getSuccessor().getFunctionName());
    }

    NumeralFormula<CompoundInterval> value;
    if (decl.getInitializer() instanceof CInitializerExpression) {
      CExpression init = ((CInitializerExpression) decl.getInitializer()).getExpression();
      value = init.accept(getExpressionToFormulaVisitor(pEdge, pElement));
      if (containsArrayWildcard(value)) {
        value = toConstant(value, pElement.getEnvironment());
      }
    } else {
      value = allPossibleValues(decl.getType());
    }

    value = handlePotentialOverflow(pElement, value, decl.getType());
    return pElement.assign(varName, value);
  }

  private InvariantsState handleFunctionCall(
      final InvariantsState pElement, final CFunctionCallEdge pEdge)
      throws UnrecognizedCodeException {

    InvariantsState newElement = pElement;
    List<String> formalParams = pEdge.getSuccessor().getFunctionParameterNames();
    List<CParameterDeclaration> declarations = pEdge.getSuccessor().getFunctionParameters();
    List<CExpression> actualParams = pEdge.getArguments();
    int limit = Math.min(formalParams.size(), actualParams.size());

    ExpressionToFormulaVisitor actualParamExpressionToFormulaVisitor =
        getExpressionToFormulaVisitor(
            new MemoryLocationExtractor(
                compoundIntervalManagerFactory,
                machineModel,
                pEdge,
                true,
                pElement.getEnvironment()),
            pElement);

    if (limit == 1 && "__VERIFIER_assume".equals(pEdge.getSuccessor().getFunctionName())) {
      NumeralFormula<CompoundInterval> assumptionExpression =
          actualParams.get(0).accept(actualParamExpressionToFormulaVisitor);
      BooleanFormula<CompoundInterval> assumption =
          compoundIntervalFormulaManager.fromNumeral(assumptionExpression);
      return handleAssumption(pElement, assumption);
    }

    formalParams = FluentIterable.from(formalParams).limit(limit).toList();
    actualParams = FluentIterable.from(actualParams).limit(limit).toList();

    Iterator<CParameterDeclaration> declarationIterator = declarations.iterator();
    for (Pair<String, CExpression> param : Pair.zipList(formalParams, actualParams)) {
      CExpression actualParam = param.getSecond();
      CParameterDeclaration declaration = declarationIterator.next();

      // Ignore unsupported types
      if (!BitVectorInfo.isSupported(declaration.getType())) {
        continue;
      }

      NumeralFormula<CompoundInterval> value =
          actualParam.accept(actualParamExpressionToFormulaVisitor);
      if (containsArrayWildcard(value)) {
        value = toConstant(value, pElement.getEnvironment());
      }
      MemoryLocation formalParam =
          MemoryLocationExtractor.scope(param.getFirst(), pEdge.getSuccessor().getFunctionName());

      value = handlePotentialOverflow(pElement, value, declaration.getType());
      newElement = newElement.assign(formalParam, value);
    }

    return newElement;
  }

  private CompoundInterval evaluate(
      NumeralFormula<CompoundInterval> pFormula,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return pFormula.accept(
        new FormulaCompoundStateEvaluationVisitor(compoundIntervalManagerFactory), pEnvironment);
  }

  private NumeralFormula<CompoundInterval> toConstant(
      NumeralFormula<CompoundInterval> pFormula,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return InvariantsFormulaManager.INSTANCE.asConstant(
        pFormula.getTypeInfo(), evaluate(pFormula, pEnvironment));
  }

  private InvariantsState handleStatement(
      InvariantsState pElement, CStatementEdge pEdge, InvariantsPrecision pPrecision)
      throws UnrecognizedCodeException {

    if (pEdge.getStatement() instanceof CFunctionCall) {
      CExpression fn =
          ((CFunctionCall) pEdge.getStatement())
              .getFunctionCallExpression()
              .getFunctionNameExpression();
      if (fn instanceof CIdExpression) {
        String func = ((CIdExpression) fn).getName();
        if (UNSUPPORTED_FUNCTIONS.containsKey(func)) {
          throw new UnsupportedCodeException(UNSUPPORTED_FUNCTIONS.get(func), pEdge, fn);
        }
      }
    }

    if (pEdge.getStatement() instanceof CAssignment) {
      CAssignment assignment = (CAssignment) pEdge.getStatement();
      ExpressionToFormulaVisitor etfv = getExpressionToFormulaVisitor(pEdge, pElement);
      CExpression leftHandSide = assignment.getLeftHandSide();
      CRightHandSide rightHandSide = assignment.getRightHandSide();
      NumeralFormula<CompoundInterval> value =
          ExpressionToFormulaVisitor.makeCastFromArrayToPointerIfNecessary(
                  assignment.getRightHandSide(), leftHandSide.getExpressionType())
              .accept(etfv);
      if (compoundIntervalFormulaManager.containsAllPossibleValues(value)
          && rightHandSide instanceof CFunctionCallExpression) {
        CFunctionCallExpression cFunctionCallExpression = (CFunctionCallExpression) rightHandSide;
        CExpression functionNameExpression = cFunctionCallExpression.getFunctionNameExpression();
        if (functionNameExpression instanceof CIdExpression) {
          CIdExpression idExpression = (CIdExpression) functionNameExpression;
          if (idExpression.getName().equals("__VERIFIER_nondet_uint")) {
            TypeInfo typeInfo = BitVectorInfo.from(machineModel, leftHandSide.getExpressionType());
            value =
                InvariantsFormulaManager.INSTANCE.asConstant(
                    typeInfo, getCompoundIntervalManager(typeInfo).singleton(0).extendToMaxValue());
          }
        }
      }
      value = handlePotentialOverflow(pElement, value, leftHandSide.getExpressionType());
      return handleAssignment(pElement, pEdge, leftHandSide, value, pPrecision);
    }

    return pElement;
  }

  private InvariantsState handleAssignment(
      InvariantsState pElement,
      CFAEdge pEdge,
      CExpression pLeftHandSide,
      NumeralFormula<CompoundInterval> pValue,
      InvariantsPrecision pPrecision)
      throws UnrecognizedCodeException {

    NumeralFormula<CompoundInterval> value = pValue;
    if (pPrecision.getMaximumFormulaDepth() == 0) {
      CompoundInterval v = evaluate(pValue, pElement.getEnvironment());
      if (v.isSingleton()) {
        value = InvariantsFormulaManager.INSTANCE.asConstant(value.getTypeInfo(), v);
      } else {
        value = allPossibleValues(pLeftHandSide.getExpressionType());
      }
    }

    ExpressionToFormulaVisitor etfv = getExpressionToFormulaVisitor(pEdge, pElement);
    MemoryLocationExtractor variableNameExtractor =
        new MemoryLocationExtractor(
            compoundIntervalManagerFactory, machineModel, pEdge, pElement.getEnvironment());
    if (pLeftHandSide instanceof CArraySubscriptExpression) {
      CArraySubscriptExpression arraySubscriptExpression =
          (CArraySubscriptExpression) pLeftHandSide;
      MemoryLocation array =
          variableNameExtractor.getMemoryLocation(arraySubscriptExpression.getArrayExpression());
      NumeralFormula<CompoundInterval> subscript =
          arraySubscriptExpression.getSubscriptExpression().accept(etfv);
      return pElement.assignArray(array, subscript, value);
    } else {
      MemoryLocation varName = variableNameExtractor.getMemoryLocation(pLeftHandSide);
      return pElement.assign(varName, value);
    }
  }

  private NumeralFormula<CompoundInterval> handlePotentialOverflow(
      InvariantsState pState, NumeralFormula<CompoundInterval> pFormula, CType pType) {
    return ExpressionToFormulaVisitor.handlePotentialOverflow(
        compoundIntervalManagerFactory, pFormula, machineModel, pType, pState.getEnvironment());
  }

  private InvariantsState handleReturnStatement(
      InvariantsState pElement, CReturnStatementEdge pEdge) throws UnrecognizedCodeException {
    // If the return edge has no statement, no return value is passed: "return;"
    if (!pEdge.getExpression().isPresent()) {
      return pElement;
    }
    ExpressionToFormulaVisitor etfv = getExpressionToFormulaVisitor(pEdge, pElement);
    Optional<CAssignment> assignment = pEdge.asAssignment();
    if (assignment.isPresent()) {
      CAssignment cAssignment = assignment.orElseThrow();
      NumeralFormula<CompoundInterval> returnedState = cAssignment.getRightHandSide().accept(etfv);
      MemoryLocationExtractor variableNameExtractor =
          new MemoryLocationExtractor(
              compoundIntervalManagerFactory, machineModel, pEdge, pElement.getEnvironment());
      CLeftHandSide leftHandSide = cAssignment.getLeftHandSide();
      if (leftHandSide instanceof CArraySubscriptExpression) {
        CArraySubscriptExpression arraySubscriptExpression =
            (CArraySubscriptExpression) leftHandSide;
        MemoryLocation array =
            variableNameExtractor.getMemoryLocation(arraySubscriptExpression.getArrayExpression());
        NumeralFormula<CompoundInterval> subscript =
            arraySubscriptExpression.getSubscriptExpression().accept(etfv);
        return pElement.assignArray(array, subscript, returnedState);
      }
      MemoryLocation varName = variableNameExtractor.getMemoryLocation(leftHandSide);
      return pElement.assign(varName, returnedState);
    }
    NumeralFormula<CompoundInterval> returnedState =
        pEdge.getExpression().orElseThrow().accept(etfv);
    MemoryLocation returnValueName =
        MemoryLocation.forDeclaration(
            pEdge.getSuccessor().getEntryNode().getReturnVariable().get());
    return pElement.assign(returnValueName, returnedState);
  }

  private InvariantsState handleFunctionReturn(
      InvariantsState pElement,
      CFunctionReturnEdge pFunctionReturnEdge,
      InvariantsPrecision pPrecision)
      throws UnrecognizedCodeException {
    CFunctionSummaryEdge summaryEdge = pFunctionReturnEdge.getSummaryEdge();

    CFunctionCall expression = summaryEdge.getExpression();

    final String calledFunctionName = pFunctionReturnEdge.getPredecessor().getFunctionName();

    Optional<CVariableDeclaration> var = pFunctionReturnEdge.getFunctionEntry().getReturnVariable();
    InvariantsState result = pElement;

    // expression is an assignment operation, e.g. a = g(b);
    if (expression instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement funcExp = (CFunctionCallAssignmentStatement) expression;

      if (var.isPresent()) {
        ExpressionToFormulaVisitor expressionToFormulaVisitor =
            getExpressionToFormulaVisitor(
                new MemoryLocationExtractor(
                    compoundIntervalManagerFactory,
                    machineModel,
                    summaryEdge.getFunctionEntry().getFunctionName(),
                    pElement.getEnvironment()),
                pElement);
        CExpression idExpression =
            ExpressionToFormulaVisitor.makeCastFromArrayToPointerIfNecessary(
                new CIdExpression(pFunctionReturnEdge.getFileLocation(), var.orElseThrow()),
                funcExp.getLeftHandSide().getExpressionType());
        NumeralFormula<CompoundInterval> value = idExpression.accept(expressionToFormulaVisitor);
        result =
            handleAssignment(
                pElement, pFunctionReturnEdge, funcExp.getLeftHandSide(), value, pPrecision);
      }
    } else {
      Iterator<CExpression> actualParamIterator =
          summaryEdge
              .getExpression()
              .getFunctionCallExpression()
              .getParameterExpressions()
              .iterator();
      for (String formalParamName :
          pFunctionReturnEdge.getPredecessor().getEntryNode().getFunctionParameterNames()) {
        if (!actualParamIterator.hasNext()) {
          break;
        }
        CExpression actualParam = actualParamIterator.next();
        NumeralFormula<CompoundInterval> actualParamFormula =
            actualParam.accept(getExpressionToFormulaVisitor(summaryEdge, pElement));
        if (actualParamFormula instanceof Variable) {
          MemoryLocation actualParamName = ((Variable<?>) actualParamFormula).getMemoryLocation();
          String formalParamPrefixDeref = calledFunctionName + "::" + formalParamName + "->";
          String formalParamPrefixAccess = calledFunctionName + "::" + formalParamName + ".";
          for (Entry<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> entry :
              pElement.getEnvironment().entrySet()) {
            String varName = entry.getKey().getExtendedQualifiedName();
            if (varName.startsWith(formalParamPrefixDeref)) {
              String formalParamSuffix = varName.substring(formalParamPrefixDeref.length());
              result =
                  result.assign(
                      MemoryLocation.parseExtendedQualifiedName(
                          actualParamName + "->" + formalParamSuffix),
                      entry.getValue());
            } else if (varName.startsWith(formalParamPrefixAccess)) {
              String formalParamSuffix = varName.substring(formalParamPrefixAccess.length());
              result =
                  result.assign(
                      MemoryLocation.parseExtendedQualifiedName(
                          actualParamName + "." + formalParamSuffix),
                      entry.getValue());
            }
          }
        }
      }
    }

    // Remove all variables that are in the scope of the returning function
    result =
        result.clearAll(
            pMemoryLocation ->
                pMemoryLocation.isOnFunctionStack()
                    && pMemoryLocation.getFunctionName().equals(calledFunctionName));

    return result;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement,
      Iterable<AbstractState> pOtherElements,
      CFAEdge pCfaEdge,
      Precision pPrecision)
      throws UnrecognizedCodeException {

    InvariantsState state = (InvariantsState) pElement;

    for (AbstractStateWithAssumptions assumptionState :
        FluentIterable.from(pOtherElements).filter(AbstractStateWithAssumptions.class)) {
      AFunctionDeclaration function = pCfaEdge.getSuccessor().getFunction();
      for (AExpression assumption : assumptionState.getAssumptions()) {
        AssumeEdge fakeEdge;
        if (assumption instanceof CExpression) {
          fakeEdge =
              new CAssumeEdge(
                  assumption.toASTString(),
                  FileLocation.DUMMY,
                  new CFANode(function),
                  new CFANode(function),
                  (CExpression) assumption,
                  true);
        } else if (assumption instanceof JExpression) {
          fakeEdge =
              new JAssumeEdge(
                  assumption.toASTString(),
                  FileLocation.DUMMY,
                  new CFANode(function),
                  new CFANode(function),
                  (JExpression) assumption,
                  true);
        } else {
          throw new AssertionError("unexpected expression type " + assumption);
        }
        state = handleAssume(state, fakeEdge, getExpressionToFormulaVisitor(pCfaEdge, state));
        if (state == null) {
          return ImmutableSet.of();
        }
      }
    }

    if (usePointerAliasStrengthening) {
      return pointerAliasStrengthening(pElement, pOtherElements, pCfaEdge, state);
    }
    return Collections.singleton(pElement);
  }

  private Collection<? extends AbstractState> pointerAliasStrengthening(
      AbstractState pElement,
      Iterable<AbstractState> pOtherElements,
      CFAEdge pCfaEdge,
      InvariantsState state)
      throws UnrecognizedCodeException {
    CFAEdge edge = pCfaEdge;
    ALeftHandSide leftHandSide = CFAEdgeUtils.getLeftHandSide(edge);
    if (leftHandSide instanceof CPointerExpression
        || (leftHandSide instanceof CFieldReference
            && ((CFieldReference) leftHandSide).isPointerDereference())) {
      FluentIterable<PointerState> pointerStates =
          FluentIterable.from(pOtherElements).filter(PointerState.class);
      if (pointerStates.isEmpty()) {
        return Collections.singleton(clearAddressedVariables(state));
      }

      InvariantsState result = state;
      for (PointerState pointerState : pointerStates) {
        ExpressionToFormulaVisitor etfv = getExpressionToFormulaVisitor(edge, result);
        NumeralFormula<CompoundInterval> rhs = CFAEdgeUtils.getRightHandSide(edge).accept(etfv);

        LocationSet locationSet =
            PointerTransferRelation.asLocations((CExpression) leftHandSide, pointerState);
        if (locationSet.isTop()) {
          return Collections.singleton(clearAddressedVariables(state));
        }
        Iterable<MemoryLocation> locations =
            PointerTransferRelation.toNormalSet(pointerState, locationSet);
        boolean moreThanOneLocation = hasMoreThanNElements(locations, 1);
        for (MemoryLocation location : locations) {
          int lastIndexOfDot = location.getExtendedQualifiedName().lastIndexOf('.');
          int lastIndexOfArrow = location.getExtendedQualifiedName().lastIndexOf("->");
          final boolean hasDot = lastIndexOfDot >= 0;
          final boolean hasArrow = lastIndexOfArrow >= 0;
          if (hasArrow || hasDot) {
            if (hasArrow) {
              ++lastIndexOfArrow;
            }
            int lastIndexOfSep = Math.max(lastIndexOfDot, lastIndexOfArrow);
            final String end = location.getExtendedQualifiedName().substring(lastIndexOfSep + 1);
            Iterable<? extends MemoryLocation> targets =
                FluentIterable.from(result.getEnvironment().keySet())
                    .filter(
                        pVar ->
                            pVar.getIdentifier().endsWith("." + end)
                                || pVar.getIdentifier().endsWith("->" + end));
            if (moreThanOneLocation || hasMoreThanNElements(targets, 1)) {
              for (MemoryLocation variableName : targets) {
                Type type = result.getType(variableName);
                if (type != null) {
                  result = result.assign(variableName, allPossibleValues(type));
                }
              }
            } else {
              for (MemoryLocation variableName : targets) {
                result = result.assign(variableName, rhs);
              }
            }
          } else if (moreThanOneLocation) {
            Type type = result.getType(location);
            if (type != null) {
              result = result.assign(location, allPossibleValues(type));
            }
          } else {
            result = result.assign(location, rhs);
          }
        }
      }
      return Collections.singleton(result);
    }
    return Collections.singleton(pElement);
  }

  private InvariantsState clearAddressedVariables(InvariantsState pState) {
    if (!variableClassification.isPresent()) {
      return pState.clear();
    }
    VariableClassification varClassification = variableClassification.orElseThrow();
    InvariantsState result = pState;
    for (String variable : varClassification.getAddressedVariables()) {
      MemoryLocation location = MemoryLocation.fromQualifiedName(variable);
      Type type = result.getType(location);
      if (type != null) {
        result = result.assign(location, allPossibleValues(type));
      }
    }
    return result;
  }

  private ExpressionToFormulaVisitor getExpressionToFormulaVisitor(
      final CFAEdge pEdge, final InvariantsState pState) {
    return getExpressionToFormulaVisitor(
        new MemoryLocationExtractor(
            compoundIntervalManagerFactory, machineModel, pEdge, pState.getEnvironment()),
        pState);
  }

  private ExpressionToFormulaVisitor getExpressionToFormulaVisitor(
      final MemoryLocationExtractor pVariableNameExtractor, final InvariantsState pState) {
    return new ExpressionToFormulaVisitor(
        compoundIntervalManagerFactory,
        machineModel,
        pVariableNameExtractor,
        pState.getEnvironment());
  }

  private static boolean containsArrayWildcard(NumeralFormula<CompoundInterval> pFormula) {
    for (MemoryLocation memoryLocation : pFormula.accept(COLLECT_VARS_VISITOR)) {
      if (memoryLocation.getIdentifier().contains("[*]")) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasMoreThanNElements(Iterable<?> pIterable, int pN) {
    return !Iterables.isEmpty(Iterables.skip(pIterable, pN));
  }
}
