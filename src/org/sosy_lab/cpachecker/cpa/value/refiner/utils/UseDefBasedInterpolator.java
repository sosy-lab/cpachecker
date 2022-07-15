// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
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
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.refinement.UseDefRelation;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** This class allows to obtain interpolants statically from a given ARGPath. */
public class UseDefBasedInterpolator {

  /** the use-def relation of the final, failing (assume) edge */
  private final UseDefRelation useDefRelation;

  /** the sliced infeasible prefix for which to compute the interpolants */
  private final ARGPath slicedPrefix;

  /** the machine model in use */
  private final MachineModel machineModel;

  /**
   * This class allows the creation of (fake) interpolants by using the use-def-relation. This
   * interpolation approach only works if the given path is a sliced prefix, obtained via {@link
   * PrefixSelector#selectSlicedPrefix(List, List)}.
   */
  public UseDefBasedInterpolator(
      final ARGPath pSlicedPrefix,
      final UseDefRelation pUseDefRelation,
      final MachineModel pMachineModel) {
    slicedPrefix = pSlicedPrefix;
    useDefRelation = pUseDefRelation;
    machineModel = pMachineModel;
  }

  /**
   * This method obtains the interpolation sequence as pairs of {@link ARGState}s and their
   * respective {@link ValueAnalysisInterpolant}s.
   *
   * @return the (ordered) list of {@link ARGState}s and their respective {@link
   *     ValueAnalysisInterpolant}s
   */
  public List<Pair<ARGState, ValueAnalysisInterpolant>> obtainInterpolants() {
    Map<ARGState, Collection<ASimpleDeclaration>> useDefSequence =
        useDefRelation.getExpandedUses(slicedPrefix);
    ValueAnalysisInterpolant trivialItp = ValueAnalysisInterpolant.FALSE;

    // reverse order!
    List<Pair<ARGState, ValueAnalysisInterpolant>> interpolants = new ArrayList<>();
    PathIterator iterator = slicedPrefix.reversePathIterator();
    while (iterator.hasNext()) {
      iterator.advance();
      ARGState state = iterator.getAbstractState();

      Collection<ASimpleDeclaration> uses = useDefSequence.get(state);

      ValueAnalysisInterpolant interpolant = uses.isEmpty() ? trivialItp : createInterpolant(uses);

      interpolants.add(Pair.of(state, interpolant));

      // as the traversal goes backwards, once the interpolant was non-trivial once,
      // the next time it is trivial, it has to be TRUE, and no longer FALSE
      if (interpolant != trivialItp) {
        trivialItp = ValueAnalysisInterpolant.TRUE;
      }
    }

    return Lists.reverse(interpolants);
  }

  /**
   * This method obtains the interpolation sequence as mapping from {@link ARGState}s to their
   * respective {@link ValueAnalysisInterpolant}s.
   *
   * @return the (ordered) mapping from {@link ARGState}s to their respective {@link
   *     ValueAnalysisInterpolant}s
   */
  public Map<ARGState, ValueAnalysisInterpolant> obtainInterpolantsAsMap() {

    Map<ARGState, ValueAnalysisInterpolant> interpolants = new LinkedHashMap<>();
    for (Pair<ARGState, ValueAnalysisInterpolant> itp : obtainInterpolants()) {
      interpolants.put(itp.getFirst(), itp.getSecond());
    }

    return interpolants;
  }

  /**
   * This method creates an interpolant for the given variable declaration.
   *
   * <p>As this interpolation strategy is static, memory locations for the whole type are created,
   * i.e, even if only a single offset in an array would suffice, still the whole array is add here,
   * because interesting offsets are not known statically. The same applies for complex types, where
   * also the whole type ends up in the interpolant and not only partially.
   *
   * @param uses the variable declaration for which to create the interpolant
   * @return the interpolant for the given variable declaration
   */
  private ValueAnalysisInterpolant createInterpolant(Collection<ASimpleDeclaration> uses) {
    PersistentMap<MemoryLocation, ValueAndType> useDefInterpolant =
        PathCopyingPersistentTreeMap.of();

    for (ASimpleDeclaration use : uses) {

      for (MemoryLocation memoryLocation : obtainMemoryLocationsForType(use)) {
        useDefInterpolant =
            useDefInterpolant.putAndCopy(
                memoryLocation, new ValueAndType(UnknownValue.getInstance(), null));
      }
    }

    return new ValueAnalysisInterpolant(useDefInterpolant);
  }

  /**
   * This method returns a list of all memory locations needed to represent the type of the given
   * variable declaration.
   */
  private ImmutableList<MemoryLocation> obtainMemoryLocationsForType(ASimpleDeclaration use) {

    return ((CType) use.getType())
        .accept(new MemoryLocationCreator(use.getQualifiedName(), machineModel));
  }

  /**
   * This class creates the needed memory locations for a given type.
   *
   * <p>This class has one mutable field {@link MemoryLocationCreator#currentOffset}, so throw away
   * after use.
   */
  private static class MemoryLocationCreator
      implements CTypeVisitor<ImmutableList<MemoryLocation>, IllegalArgumentException> {

    /** the qualified name of the actual variable identifier for which to create memory location */
    private final String qualifiedName;

    /** the machine model to use */
    private final MachineModel model;

    /** the current offset for which to create the next memory location */
    private long currentOffset = 0L;

    /** marker to know if traversal went through a complex type */
    private boolean withinComplexType = false;

    private MemoryLocationCreator(final String pQualifiedName, final MachineModel pModel) {
      model = pModel;
      qualifiedName = pQualifiedName;
    }

    @Override
    public ImmutableList<MemoryLocation> visit(final CArrayType pArrayType) {
      withinComplexType = true;

      CExpression arrayLength = pArrayType.getLength();

      if (arrayLength instanceof CIntegerLiteralExpression) {
        int length = ((CIntegerLiteralExpression) arrayLength).getValue().intValue();

        return createMemoryLocationsForArray(length, pArrayType.getType());
      }

      // treat arrays with variable length as pointer
      return createSingleMemoryLocation(model.getSizeofPtr());
    }

    @Override
    public ImmutableList<MemoryLocation> visit(final CCompositeType pCompositeType) {
      withinComplexType = true;

      switch (pCompositeType.getKind()) {
        case STRUCT:
          return createMemoryLocationsForStructure(pCompositeType);
        case UNION:
          return createMemoryLocationsForUnion(pCompositeType);
        case ENUM: // there is no such kind of CompositeType
        default:
          throw new AssertionError();
      }
    }

    @Override
    public ImmutableList<MemoryLocation> visit(final CElaboratedType pElaboratedType) {
      withinComplexType = true;

      CType definition = pElaboratedType.getRealType();
      if (definition != null) {
        return definition.accept(this);
      }

      switch (pElaboratedType.getKind()) {
        case ENUM:
        case STRUCT: // TODO: UNDEFINED
        case UNION: // TODO: UNDEFINED
        default:
          return createSingleMemoryLocation(model.getSizeofInt());
      }
    }

    @Override
    public ImmutableList<MemoryLocation> visit(final CEnumType pEnumType) {
      return createSingleMemoryLocation(model.getSizeofInt());
    }

    @Override
    public ImmutableList<MemoryLocation> visit(final CFunctionType pFunctionType) {
      // a function does not really have a size, but references to functions can be used as pointers
      return createSingleMemoryLocation(model.getSizeofPtr());
    }

    @Override
    public ImmutableList<MemoryLocation> visit(final CPointerType pPointerType) {
      return createSingleMemoryLocation(model.getSizeofPtr());
    }

    @Override
    public ImmutableList<MemoryLocation> visit(final CProblemType pProblemType) {
      throw new IllegalArgumentException("Unknown C-Type: " + pProblemType.getClass());
    }

    @Override
    public ImmutableList<MemoryLocation> visit(final CSimpleType pSimpleType) {
      return createSingleMemoryLocation(model.getSizeof(pSimpleType));
    }

    @Override
    public ImmutableList<MemoryLocation> visit(final CTypedefType pTypedefType) {
      return pTypedefType.getRealType().accept(this);
    }

    @Override
    public ImmutableList<MemoryLocation> visit(final CVoidType pVoidType) {
      return createSingleMemoryLocation(model.getSizeofVoid());
    }

    private ImmutableList<MemoryLocation> createSingleMemoryLocation(final long pSize) {
      if (withinComplexType) {
        ImmutableList<MemoryLocation> memory =
            ImmutableList.of(MemoryLocation.fromQualifiedName(qualifiedName, currentOffset));

        currentOffset = currentOffset + pSize;

        return memory;
      }

      return ImmutableList.of(MemoryLocation.fromQualifiedName(qualifiedName));
    }

    private ImmutableList<MemoryLocation> createMemoryLocationsForArray(
        final int pLength, final CType pType) {
      long sizeOfType = model.getSizeof(pType).longValueExact();

      ImmutableList.Builder<MemoryLocation> memoryLocationsForArray =
          ImmutableList.builderWithExpectedSize(pLength);
      for (int i = 0; i < pLength; i++) {
        memoryLocationsForArray.addAll(createSingleMemoryLocation(sizeOfType));
      }

      return memoryLocationsForArray.build();
    }

    private ImmutableList<MemoryLocation> createMemoryLocationsForStructure(
        final CCompositeType pCompositeType) {
      ImmutableList.Builder<MemoryLocation> memoryLocationsForStructure = ImmutableList.builder();
      for (CCompositeTypeMemberDeclaration member : pCompositeType.getMembers()) {
        memoryLocationsForStructure.addAll(member.getType().accept(this));
      }

      return memoryLocationsForStructure.build();
    }

    private ImmutableList<MemoryLocation> createMemoryLocationsForUnion(
        final CCompositeType pCompositeType) {
      return createSingleMemoryLocation(model.getSizeof(pCompositeType).longValueExact());
    }

    @Override
    public ImmutableList<MemoryLocation> visit(CBitFieldType pCBitFieldType) {
      return createSingleMemoryLocation(model.getSizeof(pCBitFieldType).longValueExact());
    }
  }
}
