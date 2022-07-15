// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.cpa.value.ExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.states.MemoryLocationValueHandler;

/**
 * This class allows assignment of new {@link SymbolicIdentifier}s to {@link MemoryLocation}s of
 * {@link ValueAnalysisState} object.
 */
@Options(prefix = "cpa.value.symbolic")
public class SymbolicValueAssigner implements MemoryLocationValueHandler {

  @Option(
      description =
          "If this option is set to true, an own symbolic identifier is assigned to"
              + " each struct member when handling non-deterministic structs.")
  private boolean handleStructs = true;

  @Option(
      description =
          "If this option is set to true, an own symbolic identifier is assigned to each array slot"
              + " when handling non-deterministic arrays of fixed length. If the length of the"
              + " array can't be determined, it won't be handled in either cases.")
  private boolean handleArrays = false;

  @Option(description = "Default size of arrays whose length can't be determined.")
  private int defaultArraySize = 20;

  @Option(description = "Whether to handle non-deterministic pointers in symbolic value analysis.")
  private boolean handlePointers = true;

  /**
   * Creates a new <code>SymbolicValueAssigner</code> object with the given configuration.
   *
   * @param pConfig the configuration to use
   * @throws InvalidConfigurationException thrown by {@link Configuration#inject}
   */
  public SymbolicValueAssigner(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  /**
   * Assigns a new symbolic identifier to the variable at the given memory location. If the variable
   * is a struct or array, behaviour is defined by {@link #handleStructs} and {@link #handleArrays}.
   *
   * @param pVarLocation the memory location of the variable to handle
   * @param pVarType the type of th evariable
   * @param pState the {@link ValueAnalysisState} to use. Value assignments will happen directly in
   *     this state
   * @param pValueVisitor a value visitor for possibly needed evaluations or computations
   * @throws UnrecognizedCodeException thrown if a {@link MemoryLocation} can't be evaluated
   */
  @Override
  public void handle(
      MemoryLocation pVarLocation,
      Type pVarType,
      ValueAnalysisState pState,
      ExpressionValueVisitor pValueVisitor)
      throws UnrecognizedCodeException {

    if (isEligibleForSymbolicValue(pVarType)) {
      assignNewSymbolicIdentifier(pState, pVarLocation, pVarType, pValueVisitor);

    } else {
      pState.forget(pVarLocation);
    }
  }

  /**
   * Assigns a new symbolic identifier to the variable at the given memory location.
   *
   * <p>If the variable is a struct, behaviour depends on {@link #handleStructs}. If <code>true
   * </code>, all members of the struct will get a distinct {@link SymbolicIdentifier}. Otherwise,
   * the variable will not be handled.
   *
   * <p>If the variable is an array, behavior depends on {@link #handleArrays}. If <code>true</code>
   * , and if the array size is known, all elements of the array will get a distinct {@link
   * SymbolicIdentifier}. If <code>true</code> and the array size is not known, the first {@link
   * #defaultArraySize} potential elements will be assigned a symbolic identifier. If <code>false
   * </code>, the variable will not be handled.
   *
   * @param pState the state to use for assignments
   * @param pVarLocation the memory location of the variable
   * @param pVarType the type of the variable
   * @param pValueVisitor value visitor for evaluating the memory location of struct members
   * @throws UnrecognizedCodeException thrown if a memory location can't be evaluated
   */
  private void assignNewSymbolicIdentifier(
      ValueAnalysisState pState,
      MemoryLocation pVarLocation,
      Type pVarType,
      ExpressionValueVisitor pValueVisitor)
      throws UnrecognizedCodeException {

    if (pVarType instanceof JType) {
      addSymbolicTracking(pState, pVarLocation, (JType) pVarType);

    } else {
      assert pVarType instanceof CType : "Unhandled type " + pVarType;

      addSymbolicTracking(pState, pVarLocation, (CType) pVarType, pValueVisitor);
    }
  }

  private void addSymbolicTracking(
      ValueAnalysisState pState, MemoryLocation pVarLocation, JType pVarType) {

    if (pVarType instanceof JSimpleType) {
      assignSymbolicIdentifier(pState, pVarLocation, pVarType);

    } else {
      pState.forget(pVarLocation);
    }
  }

  private void addSymbolicTracking(
      ValueAnalysisState pState,
      MemoryLocation pVarLocation,
      CType pVarType,
      ExpressionValueVisitor pValueVisitor)
      throws UnrecognizedCodeException {

    final CType canonicalType = pVarType.getCanonicalType();

    if (canonicalType instanceof CCompositeType) {
      fillStructWithSymbolicIdentifiers(
          pState, pVarLocation, (CCompositeType) canonicalType, pValueVisitor);

    } else if (canonicalType instanceof CArrayType) {
      fillArrayWithSymbolicIdentifiers(
          pState, pVarLocation, (CArrayType) canonicalType, pValueVisitor);

    } else if (canonicalType instanceof CElaboratedType) {
      // undefined enum, struct or union
      pState.forget(pVarLocation);

    } else {
      // use original type for symbolic identifier, not canonical type
      assignSymbolicIdentifier(pState, pVarLocation, pVarType);
    }
  }

  private void assignSymbolicIdentifier(
      ValueAnalysisState pState, MemoryLocation pVarLocation, Type pVarType) {

    SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

    SymbolicIdentifier newIdentifier = factory.newIdentifier(pVarLocation);
    SymbolicValue newIdentifierWithType = factory.asConstant(newIdentifier, pVarType);

    pState.assignConstant(pVarLocation, newIdentifierWithType, pVarType);
  }

  private void fillStructWithSymbolicIdentifiers(
      ValueAnalysisState pState,
      MemoryLocation pStructLocation,
      CCompositeType pStructType,
      ExpressionValueVisitor pValueVisitor)
      throws UnrecognizedCodeException {

    assert handleStructs;
    List<CCompositeType.CCompositeTypeMemberDeclaration> memberDeclarations =
        pStructType.getMembers();

    for (CCompositeType.CCompositeTypeMemberDeclaration d : memberDeclarations) {
      String memberName = d.getName();
      @Nullable MemoryLocation memberLocation =
          pValueVisitor.evaluateRelativeMemLocForStructMember(
              pStructLocation, memberName, pStructType);
      if (memberLocation == null) {
        continue; // TODO this ignores values of bit fields
      }
      CType memberType = d.getType().getCanonicalType();

      if (memberType instanceof CCompositeType) {
        fillStructWithSymbolicIdentifiers(
            pState, memberLocation, (CCompositeType) memberType, pValueVisitor);

      } else {
        assignSymbolicIdentifier(pState, memberLocation, memberType);
      }
    }
  }

  private void fillArrayWithSymbolicIdentifiers(
      final ValueAnalysisState pState,
      final MemoryLocation pArrayLocation,
      final CArrayType pArrayType,
      final ExpressionValueVisitor pValueVisitor)
      throws UnrecognizedCodeException {

    if (!handleArrays) {
      pState.forget(pArrayLocation);
      return;
    }

    CExpression arraySizeExpression = pArrayType.getLength();
    Value arraySizeValue;
    long arraySize;

    if (arraySizeExpression == null) { // array of unknown length
      arraySize = defaultArraySize;
    } else {
      arraySizeValue = arraySizeExpression.accept(pValueVisitor);
      if (!arraySizeValue.isExplicitlyKnown()) {
        arraySize = defaultArraySize;

      } else {
        assert arraySizeValue instanceof NumericValue;

        arraySize = ((NumericValue) arraySizeValue).longValue();
      }
    }

    for (int i = 0; i < arraySize; i++) {
      MemoryLocation arraySlotMemLoc =
          pValueVisitor.evaluateMemLocForArraySlot(pArrayLocation, i, pArrayType);

      handle(arraySlotMemLoc, pArrayType.getType(), pState, pValueVisitor);
    }
  }

  private boolean isEligibleForSymbolicValue(Type pDeclarationType) {
    if (pDeclarationType instanceof CType) {
      CType canonicalType = ((CType) pDeclarationType).getCanonicalType();

      if (canonicalType instanceof CComplexType) {
        return handleStructs;

      } else {
        return (!(canonicalType instanceof CPointerType) || handlePointers)
            && !(canonicalType instanceof CVoidType || canonicalType instanceof CTypedefType);
      }

    } else {
      return true;
    }
  }
}
