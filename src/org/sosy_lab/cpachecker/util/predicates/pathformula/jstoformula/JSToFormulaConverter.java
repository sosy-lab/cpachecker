/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.ObjectFormulaManager.OBJECT_FIELDS_VARIABLE_NAME;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.SCOPE_TYPE;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSAssignment;
import org.sosy_lab.cpachecker.cfa.ast.js.JSDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializers;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralField;
import org.sosy_lab.cpachecker.cfa.ast.js.JSParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUndefinedLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.Scope;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.js.JSAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.js.JSFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.js.UnknownFunctionCallerDeclarationBuilder;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder.DummyPointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassificationBuilder;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/** Class containing all the code that converts JS code into a formula. */
@SuppressWarnings({"DeprecatedIsStillUsed", "deprecation"})
public class JSToFormulaConverter {

  //names for special variables needed to deal with functions
  @Deprecated
  private static final String RETURN_VARIABLE_NAME =
      VariableClassificationBuilder.FUNCTION_RETURN_VARIABLE;

  private static final String PARAM_VARIABLE_NAME = "__param__";

  private final FormulaEncodingOptions options;

  protected final FormulaManagerView fmgr;
  protected final BooleanFormulaManagerView bfmgr;
  final FunctionFormulaManagerView ffmgr;
  final ArrayFormulaManagerView afmgr;
  final IntegerFormulaManagerView ifmgr;
  protected final LogManagerWithoutDuplicates logger;

  protected final AnalysisDirection direction;

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Set<JSVariableDeclaration> globalDeclarations = new HashSet<>();

  private final GlobalManagerContext gctx;

  public JSToFormulaConverter(
      FormulaEncodingOptions pOptions,
      final JSFormulaEncodingOptions pJSOptions,
      FormulaManagerView pFmgr,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      AnalysisDirection pDirection) {
    this.fmgr = pFmgr;
    this.options = pOptions;

    this.bfmgr = pFmgr.getBooleanFormulaManager();
    ifmgr = pFmgr.getIntegerFormulaManager();
    ffmgr = pFmgr.getFunctionFormulaManager();
    this.logger = new LogManagerWithoutDuplicates(pLogger);

    this.direction = pDirection;

    afmgr = fmgr.getArrayFormulaManager();

    final TypedValues typedValues = new TypedValues(ffmgr);
    final TypeTags typeTags = new TypeTags(ifmgr);
    final ObjectIdFormulaManager objIdMgr = new ObjectIdFormulaManager(pFmgr);
    gctx =
        new GlobalManagerContext(
            pOptions,
            pJSOptions,
            logger,
            pShutdownNotifier, pDirection,
            typedValues,
            typeTags,
            new TypedValueManager(fmgr, typedValues, typeTags, objIdMgr.getNullObjectId()),
            new Ids<>(),
            new FunctionScopeManager(),
            objIdMgr,
            new StringFormulaManager(fmgr, pJSOptions.maxFieldNameCount),
            fmgr);
  }

  @SuppressWarnings("SameParameterValue")
  private void logfOnce(Level level, CFAEdge edge, String msg, Object... args) {
    if (logger.wouldBeLogged(level)) {
      logger.logfOnce(level, "%s: %s: %s",
          edge.getFileLocation(),
          String.format(msg, args),
          edge.getDescription());
    }
  }

  //  @Override
  public PathFormula makeAnd(PathFormula oldFormula, CFAEdge edge, ErrorConditions errorConditions)
      throws UnrecognizedCFAEdgeException, UnrecognizedCodeException {

    String function = (edge.getPredecessor() != null)
                          ? edge.getPredecessor().getFunctionName() : null;
    assert function != null;

    SSAMapBuilder ssa = oldFormula.getSsa().builder();
    Constraints constraints = new Constraints(bfmgr);
    PointerTargetSetBuilder pts = createPointerTargetSetBuilder(oldFormula.getPointerTargetSet());

    // handle the edge
    BooleanFormula edgeFormula =
        createFormulaForEdge(edge, function, ssa, constraints, errorConditions);

    // result-constraints must be added _after_ handling the edge (some lines above),
    // because this edge could write a global value.
    if (edge.getSuccessor() instanceof FunctionExitNode) {
      final EdgeManagerContext ctx =
          new EdgeManagerContext(gctx, this, edge, function, ssa, constraints, errorConditions);
      addGlobalAssignmentConstraints(ctx, RETURN_VARIABLE_NAME, true);
    }

    edgeFormula = bfmgr.and(edgeFormula, constraints.get());

    SSAMap newSsa = ssa.build();
    PointerTargetSet newPts = pts.build();

    if (bfmgr.isTrue(edgeFormula)
        && (newSsa == oldFormula.getSsa())
        && newPts.equals(oldFormula.getPointerTargetSet())) {
      // formula is just "true" and rest is equal
      // i.e. no writes to SSAMap, no branching and length should stay the same
      return oldFormula;
    }

    BooleanFormula newFormula = bfmgr.and(oldFormula.getFormula(), edgeFormula);
    int newLength = oldFormula.getLength() + 1;
    return new PathFormula(newFormula, newSsa, newPts, newLength);
  }

  /**
   * this function is only executed, if the option useParameterVariablesForGlobals is used,
   * otherwise it does nothing. create and add constraints about a global variable:
   * tmp_1_f==global1, tmp_2_f==global2, ...
   *
   * @param tmpAsLHS if tmpAsLHS: tmp_result1_f := global1 else global1 := tmp_result1_f
   */
  @SuppressWarnings("LoopStatementThatDoesntLoop")
  private void addGlobalAssignmentConstraints(
      final EdgeManagerContext pCtx, final String tmpNamePart, final boolean tmpAsLHS)
      throws UnrecognizedCodeException {

    if (options.useParameterVariablesForGlobals()) {

      // make assignments: tmp_param1_f==global1, tmp_param2_f==global2, ...
      // function-name is important, because otherwise the name is not unique over several
      // function-calls.
      for (final JSVariableDeclaration decl : globalDeclarations) {
        final JSParameterDeclaration tmpParameter =
            new JSParameterDeclaration(
                decl.getFileLocation(), decl.getName() + tmpNamePart + pCtx.function);
        tmpParameter.setQualifiedName(decl.getQualifiedName() + tmpNamePart + pCtx.function);

        final JSIdExpression tmp = new JSIdExpression(decl.getFileLocation(), tmpParameter);
        final JSIdExpression glob = new JSIdExpression(decl.getFileLocation(), decl);

        final BooleanFormula eq;
        if (tmpAsLHS) {
          eq = pCtx.assignmentMgr.makeAssignment(tmp, glob);
        } else {
          eq = pCtx.assignmentMgr.makeAssignment(glob, tmp);
        }
        pCtx.constraints.addConstraint(eq);
        throw new RuntimeException("Not implemented");
      }

    }
  }

  /**
   * This helper method creates a formula for an CFA edge, given the current function, SSA map and
   * constraints.
   *
   * @param edge the edge for which to create the formula
   * @param function the current scope
   * @param ssa the current SSA map
   * @param constraints the current constraints
   * @return the formula for the edge
   */
  private BooleanFormula createFormulaForEdge(
      final CFAEdge edge,
      final String function,
      final SSAMapBuilder ssa,
      final Constraints constraints,
      final ErrorConditions errorConditions)
      throws UnrecognizedCFAEdgeException, UnrecognizedCodeException {
    final EdgeManagerContext ctx =
        new EdgeManagerContext(gctx, this, edge, function, ssa, constraints, errorConditions);
    switch (edge.getEdgeType()) {
    case StatementEdge: {
          return makeStatement(((JSStatementEdge) edge).getStatement(), ctx);
    }

    case ReturnStatementEdge: {
          final JSReturnStatementEdge returnEdge = (JSReturnStatementEdge) edge;
          assert returnEdge.asAssignment().isPresent()
              : "There are no void functions in JavaScript";
          return makeReturn(returnEdge.asAssignment().get(), ctx);
    }

    case DeclarationEdge: {
          return makeDeclaration(((JSDeclarationEdge) edge).getDeclaration(), ctx);
    }

    case AssumeEdge: {
          final JSAssumeEdge assumeEdge = (JSAssumeEdge) edge;
          return makePredicate(assumeEdge.getExpression(), assumeEdge.getTruthAssumption(), ctx);
    }

    case BlankEdge: {
      return bfmgr.makeTrue();
    }

    case FunctionCallEdge: {
          return makeFunctionCall(ctx);
    }

    case FunctionReturnEdge: {
          // get the expression from the summary edge
          final JSFunctionSummaryEdge ce = ((JSFunctionReturnEdge) edge).getSummaryEdge();
          return makeExitFunction(
              new EdgeManagerContext(gctx, this, ce, function, ssa, constraints, errorConditions));
    }

    case CallToReturnEdge:
        final JSFunctionSummaryEdge ce = (JSFunctionSummaryEdge) edge;
        return makeExitFunction(
            new EdgeManagerContext(gctx, this, ce, function, ssa, constraints, errorConditions));

    default:
      throw new UnrecognizedCFAEdgeException(edge);
    }
  }

  private BooleanFormula makeStatement(final JSStatement statement, final EdgeManagerContext ctx)
      throws UnrecognizedCodeException {
    if (statement instanceof JSAssignment) {
      JSAssignment assignment = (JSAssignment) statement;
      return ctx.assignmentMgr.makeAssignment(
          assignment.getLeftHandSide(), assignment.getRightHandSide());
    } else {
      if (!(statement instanceof JSExpressionStatement)) {
        throw new UnrecognizedCodeException("Unknown statement", ctx.edge, statement);
      }
      // side-effect free statement, ignore
      return bfmgr.makeTrue();
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private BooleanFormula makeDeclaration(
      final JSDeclaration pDeclaration, final EdgeManagerContext ctx)
      throws UnrecognizedCodeException {

    if (pDeclaration instanceof JSFunctionDeclaration) {
      // TODO implement without creating CFA expressions
      return ctx.assignmentMgr.makeAssignment(
          new JSIdExpression(FileLocation.DUMMY, pDeclaration),
          new JSObjectLiteralExpression(
              FileLocation.DUMMY,
              ImmutableList.of(
                  new JSObjectLiteralField(
                      "prototype",
                      new JSObjectLiteralExpression(
                          FileLocation.DUMMY, Collections.emptyList())))));
    }
    if (!(pDeclaration instanceof JSVariableDeclaration)) {
      // struct prototype, function declaration, typedef etc.
      logfOnce(Level.FINEST, ctx.edge, "Ignoring declaration");
      return bfmgr.makeTrue();
    }

    JSVariableDeclaration decl = (JSVariableDeclaration) pDeclaration;
    final String varName = decl.getQualifiedName();

    //    if (!isRelevantVariable(decl)) {
    //      logger.logfOnce(Level.FINEST, "%s: Ignoring declaration of unused variable: %s",
    //          decl.getFileLocation(), decl.toASTString());
    //      return bfmgr.makeTrue();
    //    }
    //
    //    checkForLargeArray(edge, decl.getType().getCanonicalType());
    //
    //    if (options.useParameterVariablesForGlobals() && decl.isGlobal()) {
    //      globalDeclarations.add(decl);
    //    }

    // if the var is unsigned, add the constraint that it should
    // be > 0
    //    if (((CSimpleType)spec).isUnsigned()) {
    //    long z = mathsat.api.msat_make_number(msatEnv, "0");
    //    long mvar = buildMsatVariable(var, idx);
    //    long t = mathsat.api.msat_make_gt(msatEnv, mvar, z);
    //    t = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), t);
    //    m1 = new MathsatFormula(t);
    //    }

    // just increment index of variable in SSAMap
    // (a declaration contains an implicit assignment, even without initializer)
    // In case of an existing initializer, we increment the index twice
    // (here and below) so that the index 2 only occurs for uninitialized variables.
    // DO NOT OMIT THIS CALL, even without an initializer!
    // It is only omitted if the variable is a closure variable, since then the variable is shared
    // between closure contexts (see updateIndicesOfOtherScopeVariables).
    // It is assumed that it is a closure variable if the variable is already part of the SSA
    // with a valid index.
    if (direction == AnalysisDirection.FORWARD && !ctx.ssa.allVariables().contains(varName)) {
      ctx.varIdMgr.makeFreshIndex(varName);
    }

    // if there is an initializer associated to this variable,
    // take it into account
    BooleanFormula result = bfmgr.makeTrue();

    //    if (decl.getInitializer() instanceof CInitializerList) {
    //      // If there is an initializer, all fields/elements not mentioned
    //      // in the initializer are set to 0 (C standard § 6.7.9 (21)
    //
    //      int size = machineModel.getSizeof(decl.getType());
    //      if (size > 0) {
    //        Formula var = makeVariable(varName, decl.getType(), ssa);
    //        CType elementCType = decl.getType();
    //        FormulaType<?> elementFormulaType = getFormulaTypeFromCType(elementCType);
    //        Formula zero = fmgr.makeNumber(elementFormulaType, 0L);
    //        result = bfmgr.and(result, fmgr.assignment(var, zero));
    //      }
    //    }

    for (JSAssignment assignment : JSInitializers.convertToAssignments(decl, ctx.edge)) {
      result =
          bfmgr.and(
              result,
              ctx.assignmentMgr.makeAssignment(
                  assignment.getLeftHandSide(), assignment.getRightHandSide()));
    }

    return result;
  }

  private BooleanFormula makeExitFunction(final EdgeManagerContext pCtx)
      throws UnrecognizedCodeException {
    final JSFunctionSummaryEdge ce = (JSFunctionSummaryEdge) pCtx.edge;
    addGlobalAssignmentConstraints(pCtx, RETURN_VARIABLE_NAME, false);

    JSFunctionCall retExp = ce.getExpression();
    if (retExp instanceof JSFunctionCallStatement) {
      // this should be a void return, just do nothing...
      return bfmgr.makeTrue();

    } else if (retExp instanceof JSFunctionCallAssignmentStatement) {
      JSFunctionCallAssignmentStatement exp = (JSFunctionCallAssignmentStatement) retExp;
      JSFunctionCallExpression funcCallExp = exp.getRightHandSide();

      String callerFunction = ce.getSuccessor().getFunctionName();
      final com.google.common.base.Optional<JSVariableDeclaration> returnVariableDeclaration =
          ce.getFunctionEntry().getReturnVariable();
      if (!returnVariableDeclaration.isPresent()) {
        throw new UnrecognizedCodeException("Void function used in assignment", ce, retExp);
      }
      // TODO constructor return value might not be the created object (see 9. of
      // https://www.ecma-international.org/ecma-262/5.1/#sec-13.2.2)
      final JSIdExpression rhs =
          funcCallExp.isConstructorCall()
              ? new JSIdExpression(
                  FileLocation.DUMMY, funcCallExp.getDeclaration().getThisVariableDeclaration())
              : new JSIdExpression(funcCallExp.getFileLocation(), returnVariableDeclaration.get());
      return pCtx.copy(callerFunction)
          .assignmentMgr
          .makeAssignment(exp.getLeftHandSide(), rhs, pCtx);
    } else {
      throw new UnrecognizedCodeException("Unknown function exit expression", ce, retExp);
    }
  }

  @SuppressWarnings("OptionalIsPresent")
  private BooleanFormula makeFunctionCall(final EdgeManagerContext pCallerCtx)
      throws UnrecognizedCodeException {
    final JSFunctionCallEdge edge = (JSFunctionCallEdge) pCallerCtx.edge;
    final List<BooleanFormula> result = new ArrayList<>();
    final JSFunctionCallExpression functionCallExpression =
        edge.getSummaryEdge().getExpression().getFunctionCallExpression();
    final String calledFunctionName = functionCallExpression.getDeclaration().getQualifiedName();
    final EdgeManagerContext calledFunctionCtx = pCallerCtx.copy(calledFunctionName);
    final Optional<JSIdExpression> functionObject = functionCallExpression.getFunctionObject();
    final IntegerFormula callerScopeVariable = pCallerCtx.scopeMgr.getCurrentScope();
    final IntegerFormula currentScopeVariable = calledFunctionCtx.scopeMgr.createCurrentScope();
    result.add(
        fmgr.makeEqual(
            currentScopeVariable,
            fmgr.makeNumber(
                SCOPE_TYPE, gctx.functionScopeManager.createScope(calledFunctionName))));
    // TODO refactor
    final ArrayFormula<IntegerFormula, IntegerFormula> ss;
    final int nestingLevel = functionCallExpression.getDeclaration().getScope().getNestingLevel();
    if (functionCallExpression.getDeclaration().isGlobal()) {
      ss = pCallerCtx.scopeMgr.getGlobalScopeStack();
    } else if (functionObject.isPresent()) {
      ss =
          pCallerCtx.scopeMgr.scopeStack(
              pCallerCtx.scopeMgr.scopeOf(
                  gctx.typedValues.functionValue(
                      pCallerCtx.scopeMgr.scopedVariable(
                          Objects.requireNonNull(functionObject.get().getDeclaration())))));
    } else {
      ss =
          pCallerCtx.scopeMgr.scopeStack(
              afmgr.select(
                  pCallerCtx.scopeMgr.scopeStack(callerScopeVariable),
                  ifmgr.makeNumber(nestingLevel)));
    }
    result.add(
        fmgr.makeEqual(
            calledFunctionCtx.scopeMgr.scopeStack(currentScopeVariable),
            afmgr.store(ss, ifmgr.makeNumber(nestingLevel + 1), currentScopeVariable)));

    // TODO manage global object (no variables are assigned to this dummy)
    final JSIdExpression globalObjectId =
        new JSIdExpression(
            FileLocation.DUMMY,
            new JSVariableDeclaration(
                FileLocation.DUMMY,
                Scope.GLOBAL,
                "globalObject",
                "globalObject",
                "globalObject",
                null));
    final JSExpression thisValue;
    if (functionCallExpression.isConstructorCall()) {
      // TODO implement without creating CFA expressions
      thisValue =
          new JSObjectLiteralExpression(
              FileLocation.DUMMY,
              ImmutableList.of(
                  new JSObjectLiteralField(
                      "__proto__",
                      new JSFieldAccess(
                          FileLocation.DUMMY,
                          new JSIdExpression(
                              FileLocation.DUMMY, functionCallExpression.getDeclaration()),
                          "prototype"))));
    } else {
      thisValue = functionCallExpression.getThisArg().orElse(globalObjectId);
    }
    // this binding, see https://www.ecma-international.org/ecma-262/5.1/#sec-10.4.3
    result.add(
        calledFunctionCtx.assignmentMgr.makeAssignment(
            new JSIdExpression(
                FileLocation.DUMMY,
                functionCallExpression.getDeclaration().getThisVariableDeclaration()),
            // TODO handle null and undefined in non strict code as described in
            // https://www.ecma-international.org/ecma-262/5.1/#sec-10.4.3
            thisValue,
            pCallerCtx));

    final Iterator<JSExpression> actualParams = edge.getArguments().iterator();

    final JSFunctionEntryNode fn = edge.getSuccessor();
    final List<JSParameterDeclaration> formalParams = fn.getFunctionParameters();

    if (UnknownFunctionCallerDeclarationBuilder.maxParameterCount < edge.getArguments().size()) {
      throw new UnrecognizedCodeException(
          "Cannot handle more function arguments than "
              + UnknownFunctionCallerDeclarationBuilder.maxParameterCount
              + " (configured in UnknownFunctionCallerDeclarationBuilder.maxParameterCount)",
          edge);
    }
    for (JSParameterDeclaration formalParam : formalParams) {
      JSExpression paramExpression =
          actualParams.hasNext()
              ? actualParams.next()
              : new JSUndefinedLiteralExpression(FileLocation.DUMMY);
      JSIdExpression lhs = new JSIdExpression(paramExpression.getFileLocation(), formalParam);
      final JSIdExpression paramLHS;
      if (options.useParameterVariables()) {
        // make assignments: tmp_param1==arg1, tmp_param2==arg2, ...
        JSParameterDeclaration tmpParameter =
            new JSParameterDeclaration(
                formalParam.getFileLocation(), formalParam.getName() + PARAM_VARIABLE_NAME);
        tmpParameter.setQualifiedName(formalParam.getQualifiedName() + PARAM_VARIABLE_NAME);
        paramLHS = new JSIdExpression(paramExpression.getFileLocation(), tmpParameter);
      } else {
        paramLHS = lhs;
      }

      result.add(
          calledFunctionCtx.assignmentMgr.makeAssignment(paramLHS, paramExpression, pCallerCtx));
    }

    addGlobalAssignmentConstraints(calledFunctionCtx, PARAM_VARIABLE_NAME, true);

    return bfmgr.and(result);
  }

  private BooleanFormula makeReturn(final JSAssignment assignment, final EdgeManagerContext ctx)
      throws UnrecognizedCodeException {
    return ctx.assignmentMgr.makeAssignment(
        assignment.getLeftHandSide(), assignment.getRightHandSide());
  }

  private BooleanFormula makePredicate(
      final JSExpression exp, boolean isTrue, final EdgeManagerContext ctx)
      throws UnrecognizedCodeException {
    final TypedValue condition = ctx.exprMgr.makeExpression(exp);
    BooleanFormula result = gctx.valConv.toBoolean(condition);
    if (!isTrue) {
      result = bfmgr.not(result);
    }
    return result;
  }

  /**
   * Parameters not used in {@link JSToFormulaConverter}, may be in subclasses they are.
   *
   * @param pts the pointer target set to use initially
   */
  private PointerTargetSetBuilder createPointerTargetSetBuilder(
      @SuppressWarnings("unused") PointerTargetSet pts) {
    return DummyPointerTargetSetBuilder.INSTANCE;
  }

  /**
   * Create the necessary equivalence terms for adjusting the SSA indices of a given symbol (of any
   * type) from oldIndex to newIndex.
   *
   * @param variableName The name of the variable for which the index is adjusted.
   * @param oldIndex The previous SSA index.
   * @param newIndex The new SSA index.
   */
  public BooleanFormula makeSsaUpdateTerm(
      final String variableName, final int oldIndex, final int newIndex) {
    checkArgument(oldIndex > 0 && newIndex > oldIndex);

    final FormulaType<?> variableType =
        variableName.equals(OBJECT_FIELDS_VARIABLE_NAME)
            ? Types.OBJECT_FIELDS_VARIABLE_TYPE
            : Types.VARIABLE_TYPE;
    final Formula oldVariable = fmgr.makeVariable(variableType, variableName, oldIndex);
    final Formula newVariable = fmgr.makeVariable(variableType, variableName, newIndex);

    return fmgr.assignment(newVariable, oldVariable);
  }

}
