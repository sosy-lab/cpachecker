// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceIndexVariable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceResolved;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentHandler.ArraySliceSpanLhs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentHandler.ArraySliceSpanResolved;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentHandler.ArraySliceSpanRhs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentHandler.AssignmentOptions;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

class AssignmentQuantifierHandler {
  private int nextQuantifierVariableNumber = 0;

  private final FormulaEncodingWithPointerAliasingOptions options;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;

  private final CToFormulaConverterWithPointerAliasing conv;
  private final TypeHandlerWithPointerAliasing typeHandler;
  private final CFAEdge edge;
  private final String function;
  private final SSAMapBuilder ssa;
  private final PointerTargetSetBuilder pts;
  private final Constraints constraints;
  private final ErrorConditions errorConditions;
  private final MemoryRegionManager regionMgr;

  private final AssignmentOptions assignmentOptions;
  private final Map<CRightHandSide, ArraySliceResolved> resolvedLhsBases;
  private final Map<CRightHandSide, ArraySliceResolved> resolvedRhsBases;

  private final CSimpleType sizeType;

  /**
   * Creates a new AssignmentQuantifierHandler.
   *
   * @param pConv The C to SMT formula converter.
   * @param pEdge The current edge of the CFA (for logging purposes).
   * @param pFunction The name of the current function.
   * @param pSsa The SSA map.
   * @param pPts The underlying set of pointer targets.
   * @param pConstraints Additional constraints.
   * @param pErrorConditions Additional error conditions.
   */
  AssignmentQuantifierHandler(
      CToFormulaConverterWithPointerAliasing pConv,
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts,
      Constraints pConstraints,
      ErrorConditions pErrorConditions,
      MemoryRegionManager pRegionMgr,
      AssignmentOptions pAssignmentOptions,
      Map<CRightHandSide, ArraySliceResolved> pResolvedLhsBases,
      Map<CRightHandSide, ArraySliceResolved> pResolvedRhsBases) {
    conv = pConv;

    typeHandler = pConv.typeHandler;
    options = conv.options;
    fmgr = conv.fmgr;
    bfmgr = conv.bfmgr;

    edge = pEdge;
    function = pFunction;
    ssa = pSsa;
    pts = pPts;
    constraints = pConstraints;
    errorConditions = pErrorConditions;
    regionMgr = pRegionMgr;

    assignmentOptions = pAssignmentOptions;
    resolvedLhsBases = pResolvedLhsBases;
    resolvedRhsBases = pResolvedRhsBases;

    sizeType = conv.machineModel.getPointerEquivalentSimpleType();
  }

  BooleanFormula handleSimpleSliceAssignments(
      final Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> assignmentMultimap)
      throws UnrecognizedCodeException, InterruptedException {

    LinkedHashSet<ArraySliceIndexVariable> variablesToQuantify = new LinkedHashSet<>();

    for (Entry<ArraySliceSpanLhs, Collection<ArraySliceSpanRhs>> entry :
        assignmentMultimap.asMap().entrySet()) {
      variablesToQuantify.addAll(entry.getKey().actual().getUnresolvedIndexVariables());
      for (ArraySliceSpanRhs rhs : entry.getValue()) {
        if (rhs.actual().isPresent()) {
          variablesToQuantify.addAll(rhs.actual().get().getUnresolvedIndexVariables());
        }
      }
    }

    return quantifySliceAssignment(
        assignmentMultimap,
        variablesToQuantify,
        bfmgr.makeTrue());
  }

  private boolean shouldUnroll() {
    return !options.useQuantifiersOnArrays() && !assignmentOptions.forceQuantifiers();
  }

  private BooleanFormula quantifySliceAssignment(
      Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> assignmentMultimap,
      LinkedHashSet<ArraySliceIndexVariable> variablesToQuantify,
      BooleanFormula condition)
      throws UnrecognizedCodeException, InterruptedException {

    // recursive quantification
    if (variablesToQuantify.isEmpty()) {
      // perform quantified assignment
      return performQuantifiedAssignment(assignmentMultimap, condition);
    }

    // get the variable to quantify
    ArraySliceIndexVariable variableToQuantify = variablesToQuantify.iterator().next();

    LinkedHashSet<ArraySliceIndexVariable> nextVariablesToQuantify =
        new LinkedHashSet<>(variablesToQuantify);
    nextVariablesToQuantify.remove(variableToQuantify);

    CExpression sliceSize = variableToQuantify.getSize();

    // we will perform the unrolled assignments conditionally, only if the index is smaller than the
    // actual size
    CExpression sliceSizeCastToSizeType =
        new CCastExpression(FileLocation.DUMMY, sizeType, sliceSize);

    final CExpressionVisitorWithPointerAliasing indexSizeVisitor =
        new CExpressionVisitorWithPointerAliasing(
            conv, edge, function, ssa, constraints, errorConditions, pts, regionMgr);
    Expression sliceSizeExpression = sliceSizeCastToSizeType.accept(indexSizeVisitor);
    // TODO: add fields to UF from visitor

    Formula sliceSizeFormula = indexSizeVisitor.asValueFormula(sliceSizeExpression, sizeType);

    // decide whether to encode or unroll the quantifier
    if (shouldUnroll()) {
      return unrollQuantifier(
          assignmentMultimap,
          nextVariablesToQuantify,
          condition,
          variableToQuantify,
          sliceSizeFormula);
    } else {
      return encodeQuantifier(
          assignmentMultimap,
          nextVariablesToQuantify,
          condition,
          variableToQuantify,
          sliceSizeFormula);
    }
  }

  private Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> resolveQuantifierInAssignments(
      Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> assignmentMultimap,
      Function<ArraySliceExpression, ArraySliceExpression> resolutionFunction) {

    Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> result = LinkedHashMultimap.create();

    for (Entry<ArraySliceSpanLhs, Collection<ArraySliceSpanRhs>> assignment :
        assignmentMultimap.asMap().entrySet()) {
      ArraySliceExpression resolvedLhsSlice = resolutionFunction.apply(assignment.getKey().actual());
      ArraySliceSpanLhs resolvedLhs =
          new ArraySliceSpanLhs(resolvedLhsSlice, assignment.getKey().targetType());

      for (ArraySliceSpanRhs rhs : assignment.getValue()) {
        Optional<ArraySliceExpression> resolvedRhsSlice =
            rhs.actual().map(rhsSlice -> resolutionFunction.apply(rhsSlice));
        ArraySliceSpanRhs resolvedRhs = new ArraySliceSpanRhs(rhs.span(), resolvedRhsSlice);
        result.put(resolvedLhs, resolvedRhs);
      }
    }
    return result;
  }

  private BooleanFormula unrollQuantifier(
      Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> assignmentMultimap,
      LinkedHashSet<ArraySliceIndexVariable> nextVariablesToQuantify,
      BooleanFormula condition,
      ArraySliceIndexVariable variableToUnroll,
      Formula sliceSizeFormula)
      throws UnrecognizedCodeException, InterruptedException {

    CExpression sliceSize = variableToUnroll.getSize();

    FormulaType<?> sizeFormulaType = conv.getFormulaTypeFromCType(sizeType);
    Formula zeroFormula = conv.fmgr.makeNumber(sizeFormulaType, 0);
    boolean sizeTypeSigned = sizeType.getCanonicalType().isSigned();

    // overapproximate for long arrays
    long consideredArraySize = options.defaultArrayLength();

    if (sliceSize instanceof CIntegerLiteralExpression literalSliceSize) {
      consideredArraySize = ((CIntegerLiteralExpression) sliceSize).getValue().longValueExact();
      if (options.maxArrayLength() >= 0 && consideredArraySize > options.maxArrayLength()) {
        consideredArraySize = options.maxArrayLength();
      }
    }

    BooleanFormula result = bfmgr.makeTrue();

    for (long i = 0; i < consideredArraySize; ++i) {
      final Formula indexFormula = conv.fmgr.makeNumber(sizeFormulaType, i);

      // the variable condition holds when 0 <= index < size
      BooleanFormula nextCondition =
          bfmgr.and(
              condition,
              fmgr.makeLessOrEqual(zeroFormula, indexFormula, sizeTypeSigned),
              fmgr.makeLessThan(indexFormula, sliceSizeFormula, sizeTypeSigned));

      // resolve quantifier in multimap
      Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> nextAssignmentMultimap =
          resolveQuantifierInAssignments(
              assignmentMultimap, slice -> slice.resolveVariable(variableToUnroll, indexFormula));

      // quantify recursively
      BooleanFormula recursionResult =
          quantifySliceAssignment(nextAssignmentMultimap, nextVariablesToQuantify, nextCondition);
      result = bfmgr.and(result, recursionResult);
    }

    return result;
  }

  private BooleanFormula performQuantifiedAssignment(
      Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> assignmentMultimap,
      BooleanFormula condition)
      throws UnrecognizedCodeException, InterruptedException {

    AssignmentFormulaHandler assignmentFormulaHandler =
        new AssignmentFormulaHandler(
            conv, edge, function, ssa, pts, constraints, errorConditions, regionMgr);

    BooleanFormula result = bfmgr.makeTrue();

    for (Entry<ArraySliceSpanLhs, Collection<ArraySliceSpanRhs>> assignment :
        assignmentMultimap.asMap().entrySet()) {

      // resolve whole lhs

      ArraySliceExpression lhs = assignment.getKey().actual();
      CType targetType = assignment.getKey().targetType();

      ArraySliceResolved lhsResolvedBase = resolvedLhsBases.get(lhs.getBase());
      ArraySliceResolved lhsResolved =
          lhs.resolveModifiers(lhsResolvedBase, conv, ssa, errorConditions, regionMgr);

      if (lhsResolved.expression().isNondetValue()) {
        // should only happen when we cannot assign to aliased bitfields
        // TODO: implement aliased bitfields
        continue;
      }

      List<ArraySliceSpanResolved> rhsResolvedList = new ArrayList<>();

      for (ArraySliceSpanRhs rhs : assignment.getValue()) {
        // resolve whole rhs
        if (rhs.actual().isEmpty()) {
          // nondet rhs means nondet resolved
          rhsResolvedList.add(new ArraySliceSpanResolved(rhs.span(), Optional.empty()));
          continue;
        }
        ArraySliceExpression rhsSlice = rhs.actual().get();
        ArraySliceResolved rhsResolvedBase = resolvedRhsBases.get(rhsSlice.getBase());
        ArraySliceResolved rhsResolved =
            rhsSlice.resolveModifiers(rhsResolvedBase, conv, ssa, errorConditions, regionMgr);
        rhsResolvedList.add(new ArraySliceSpanResolved(rhs.span(), Optional.of(rhsResolved)));
      }

      // compute pointer-target set pattern if necessary for UFs finishing
      // UFs must be finished only if all three of the following conditions are met:
      // 1. UF heap is used
      // 2. lhs is in aliased location (unaliased location is assigned as a whole)
      // 3. using old SSA indices is not selected
      final PointerTargetPattern pattern =
          !options.useArraysForHeap()
                  && lhsResolved.expression().isAliasedLocation()
                  && !assignmentOptions.useOldSSAIndicesIfAliased()
              ? PointerTargetPattern.forLeftHandSide(
                  (CLeftHandSide) lhs.getDummyResolvedExpression(sizeType), typeHandler, edge, pts)
              : null;

      // make the actual assignment

      result =
          bfmgr.and(
              result,
              assignmentFormulaHandler.makeSliceAssignment(
                  lhsResolved,
                  targetType,
                  rhsResolvedList,
                  assignmentOptions,
                  condition,
                  false,
                  pattern));
    }


    return result;
  }

  private BooleanFormula encodeQuantifier(
      Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> assignmentMultimap,
      LinkedHashSet<ArraySliceIndexVariable> nextVariablesToQuantify,
      BooleanFormula condition,
      ArraySliceIndexVariable variableToEncode,
      Formula sliceSizeFormula)
      throws UnrecognizedCodeException, InterruptedException {

    // the quantified variables should be of the size type
    FormulaType<?> sizeFormulaType = conv.getFormulaTypeFromCType(sizeType);
    Formula zeroFormula = conv.fmgr.makeNumber(sizeFormulaType, 0);
    boolean sizeTypeIsSigned = sizeType.getCanonicalType().isSigned();

    // create encoded quantified variable
    final Formula encodedVariable =
        fmgr.makeVariableWithoutSSAIndex(
            sizeFormulaType, "__quantifier_" + nextQuantifierVariableNumber++);

    // resolve in assignments
    Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> nextAssignmentMultimap =
        resolveQuantifierInAssignments(
            assignmentMultimap,
            slice -> slice.resolveVariable(variableToEncode, encodedVariable));

    // create the condition for quantifier
    // the quantified variable condition holds when 0 <= index < size
    BooleanFormula nextCondition =
        bfmgr.and(
            condition,
            fmgr.makeLessOrEqual(zeroFormula, encodedVariable, sizeTypeIsSigned),
            fmgr.makeLessThan(encodedVariable, sliceSizeFormula, sizeTypeIsSigned));

    // recurse
    BooleanFormula recursionResult =
        quantifySliceAssignment(nextAssignmentMultimap, nextVariablesToQuantify, nextCondition);

    // add quantifier around the recursion result
    return fmgr.getQuantifiedFormulaManager().forall(encodedVariable, recursionResult);
  }

}
