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
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.MachineModel.BaseSizeofVisitor;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.refinement.UseDefRelation;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * This class allows to obtain interpolants statically from a given ARGPath.
 */
public class UseDefBasedInterpolator {

  /**
   * the use-def relation of the final, failing (assume) edge
   */
  private final UseDefRelation useDefRelation;

  /**
   * the sliced infeasible prefix for which to compute the interpolants
   */
  private final ARGPath slicedPrefix;

  /**
   * the machine model in use
   */
  private final MachineModel machineModel;

  /**
   * This class allows the creation of (fake) interpolants by using the use-def-relation.
   * This interpolation approach only works if the given path is a sliced prefix,
   * obtained via {@link PrefixSelector#selectSlicedPrefix(List, List)}.
   */
  public UseDefBasedInterpolator(
      final ARGPath pSlicedPrefix,
      final UseDefRelation pUseDefRelation,
      final MachineModel pMachineModel
  ) {
    slicedPrefix   = pSlicedPrefix;
    useDefRelation = pUseDefRelation;
    machineModel   = pMachineModel;
  }

  /**
   * This method obtains the interpolation sequence as pairs of {@link ARGState}s
   * and their respective {@link ValueAnalysisInterpolant}s.
   *
   * @return the (ordered) list of {@link ARGState}s and their respective {@link ValueAnalysisInterpolant}s
   */
  public List<Pair<ARGState, ValueAnalysisInterpolant>> obtainInterpolants() {
    Map<ARGState, Collection<ASimpleDeclaration>> useDefSequence = useDefRelation.getExpandedUses(slicedPrefix);
    ValueAnalysisInterpolant trivialItp = ValueAnalysisInterpolant.FALSE;

    LinkedList<Pair<ARGState, ValueAnalysisInterpolant>> interpolants = new LinkedList<>();
    PathIterator iterator = slicedPrefix.reversePathIterator();
    while (iterator.hasNext()) {
      iterator.advance();
      ARGState state = iterator.getAbstractState();

      Collection<ASimpleDeclaration> uses = useDefSequence.get(state);

      ValueAnalysisInterpolant interpolant = uses.isEmpty()
          ? trivialItp
          : createInterpolant(uses);

      interpolants.addFirst(Pair.of(state, interpolant));

      // as the traversal goes backwards, once the interpolant was non-trivial once,
      // the next time it is trivial, it has to be TRUE, and no longer FALSE
      if (interpolant != trivialItp) {
        trivialItp = ValueAnalysisInterpolant.TRUE;
      }
    }

    return interpolants;
  }

  /**
   * This method obtains the interpolation sequence as mapping from {@link ARGState}s
   * to their respective {@link ValueAnalysisInterpolant}s.
   *
   * @return the (ordered) mapping from {@link ARGState}s to their respective {@link ValueAnalysisInterpolant}s
   */
  public Map<ARGState, ValueAnalysisInterpolant> obtainInterpolantsAsMap() {

    Map<ARGState, ValueAnalysisInterpolant> interpolants = new LinkedHashMap<>();
    for(Pair<ARGState, ValueAnalysisInterpolant> itp : obtainInterpolants()) {
      interpolants.put(itp.getFirst(), itp.getSecond());
    }

    return interpolants;
  }

  /**
   * This method creates an interpolant for the given variable declaration.
   *
   * As this interpolation strategy is static, memory locations for the whole type are created,
   * i.e, even if only a single offset in an array would suffice, still the whole array is add
   * here, because interesting offsets are not known statically. The same applies for complex
   * types, where also the whole type ends up in the interpolant and not only partially.
   *
   * @param uses the variable declaration for which to create the interpolant
   * @return the interpolant for the given variable declaration
   */
  private ValueAnalysisInterpolant createInterpolant(Collection<ASimpleDeclaration> uses) {
    HashMap<MemoryLocation, Value> useDefInterpolant = new HashMap<>();

    for (ASimpleDeclaration use : uses) {

      for (MemoryLocation memoryLocation : obtainMemoryLocationsForType(use)) {
        useDefInterpolant.put(memoryLocation, UnknownValue.getInstance());
      }
    }

    return new ValueAnalysisInterpolant(useDefInterpolant, Collections.<MemoryLocation, Type>emptyMap());
  }

  /**
   * This method returns a list of all memory locations needed to represent the type
   * of the given variable declaration.
   */
  private List<MemoryLocation> obtainMemoryLocationsForType(ASimpleDeclaration use) {

    return ((CType) use.getType()).accept(
        new MemoryLocationCreator(use.getQualifiedName(), machineModel));
  }

  /**
   * This class creates the needed memory locations for a given type.
   *
   * This class has one mutable field {@link MemoryLocationCreator#currentOffset}, so throw away after use.
   */
  private static class MemoryLocationCreator implements CTypeVisitor<List<MemoryLocation>, IllegalArgumentException> {

    /**
     * the qualified name of the actual variable identifier for which to create memory location
     */
    private final String qualifiedName;

    /**
     * the machine model to use
     */
    private final MachineModel model;

    /**
     * the current offset for which to create the next memory location
     */
    private int currentOffset = 0;

    /**
     * marker to know if traversal went through a complex type
     */
    private boolean withinComplexType = false;

    private MemoryLocationCreator(final String pQualifiedName, final MachineModel pModel) {
      model = pModel;
      qualifiedName = pQualifiedName;
    }

    @Override
    public List<MemoryLocation> visit(final CArrayType pArrayType) throws IllegalArgumentException {
      withinComplexType = true;

      CExpression arrayLength = pArrayType.getLength();

      if (arrayLength instanceof CIntegerLiteralExpression) {
        int length = ((CIntegerLiteralExpression)arrayLength).getValue().intValue();

        return createMemoryLocationsForArray(length, pArrayType.getType());
      }

      // treat arrays with variable length as pointer
      return createSingleMemoryLocation(model.getSizeofPtr());
    }

    @Override
    public List<MemoryLocation> visit(final CCompositeType pCompositeType) throws IllegalArgumentException {
      withinComplexType = true;

      switch (pCompositeType.getKind()) {
        case STRUCT: return createMemoryLocationsForStructure(pCompositeType);
        case UNION:  return createMemoryLocationsForUnion(pCompositeType);
        case ENUM:   // there is no such kind of CompositeType
        default: throw new AssertionError();
      }
    }

    @Override
    public List<MemoryLocation> visit(final CElaboratedType pElaboratedType) throws IllegalArgumentException {
      withinComplexType = true;

      CType definition = pElaboratedType.getRealType();
      if (definition != null) {
        return definition.accept(this);
      }

      switch (pElaboratedType.getKind()) {
      case ENUM:
      case STRUCT: // TODO: UNDEFINED
      case UNION:  // TODO: UNDEFINED
      default:
        return createSingleMemoryLocation(model.getSizeofInt());
      }
    }

    @Override
    public List<MemoryLocation> visit(final CEnumType pEnumType) throws IllegalArgumentException {
      return createSingleMemoryLocation(model.getSizeofInt());
    }

    @Override
    public List<MemoryLocation> visit(final CFunctionType pFunctionType) throws IllegalArgumentException {
      // a function does not really have a size, but references to functions can be used as pointers
      return createSingleMemoryLocation(model.getSizeofPtr());
    }

    @Override
    public List<MemoryLocation> visit(final CPointerType pPointerType) throws IllegalArgumentException {
      return createSingleMemoryLocation(model.getSizeofPtr());
    }

    @Override
    public List<MemoryLocation> visit(final CProblemType pProblemType) throws IllegalArgumentException {
      throw new IllegalArgumentException("Unknown C-Type: " + pProblemType.getClass().toString());
    }

    @Override
    public List<MemoryLocation> visit(final CSimpleType pSimpleType) throws IllegalArgumentException {
      return createSingleMemoryLocation(model.getSizeof(pSimpleType));
    }

    @Override
    public List<MemoryLocation> visit(final CTypedefType pTypedefType) throws IllegalArgumentException {
      return pTypedefType.getRealType().accept(this);
    }

    @Override
    public List<MemoryLocation> visit(final CVoidType pVoidType) throws IllegalArgumentException {
      return createSingleMemoryLocation(model.getSizeofVoid());
    }

    private List<MemoryLocation> createSingleMemoryLocation(final int pSize) {
      if (withinComplexType) {
        List<MemoryLocation> memory = Collections.singletonList(MemoryLocation.valueOf(qualifiedName, currentOffset));

        currentOffset = currentOffset + pSize;

        return memory;
      }

      return Collections.singletonList(MemoryLocation.valueOf(qualifiedName));
    }

    private List<MemoryLocation> createMemoryLocationsForArray(final int pLength, final CType pType) {
      int sizeOfType = model.getSizeof(pType);

      List<MemoryLocation> memoryLocationsForArray = new ArrayList<>(pLength);
      for (int i = 0; i < pLength; i++) {
        memoryLocationsForArray.addAll(createSingleMemoryLocation(sizeOfType));
      }

      return memoryLocationsForArray;
    }

    private List<MemoryLocation> createMemoryLocationsForStructure(final CCompositeType pCompositeType) {
      List<MemoryLocation> memoryLocationsForStructure = new ArrayList<>();
      for (CCompositeTypeMemberDeclaration member : pCompositeType.getMembers()) {
        memoryLocationsForStructure.addAll(member.getType().accept(this));
      }

      return memoryLocationsForStructure;
    }

    private List<MemoryLocation> createMemoryLocationsForUnion(final CCompositeType pCompositeType) {
      return createSingleMemoryLocation(new BaseSizeofVisitor(model).visit(pCompositeType));
    }
  }
}
