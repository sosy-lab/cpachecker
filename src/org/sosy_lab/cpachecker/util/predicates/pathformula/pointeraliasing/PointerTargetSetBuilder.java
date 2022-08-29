// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static java.util.stream.Collectors.toCollection;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;
import static org.sosy_lab.common.collect.PersistentLinkedList.toPersistentLinkedList;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.checkIsSimplified;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.java_smt.api.Formula;

public interface PointerTargetSetBuilder {

  void prepareBase(String name, CType type, @Nullable Formula size, Constraints constraints);

  void shareBase(String name, CType type);

  /**
   * Adds the newly allocated base of the given type for tracking along with all its tracked
   * (sub)fields (if its a structure/union) or all its elements (if its an array).
   */
  void addBase(String name, CType type, @Nullable Formula size, Constraints constraints);

  boolean tracksField(CompositeField field);

  boolean addField(CompositeField field);

  void addEssentialFields(final List<CompositeField> fields);

  void addTemporaryDeferredAllocation(
      boolean isZeroed, Optional<CIntegerLiteralExpression> size, Formula sizeExp, String base);

  void addDeferredAllocationPointer(String newPointer, String originalPointer);

  ImmutableSet<DeferredAllocation> removeDeferredAllocationPointer(String pointer);

  boolean canRemoveDeferredAllocationPointer(String pointer);

  ImmutableSet<DeferredAllocation> removeDeferredAllocations(String pointer);

  ImmutableSet<String> getDeferredAllocationPointers();

  boolean isTemporaryDeferredAllocationPointer(String pointer);

  boolean isDeferredAllocationPointer(String pointer);

  boolean isActualBase(String name);

  boolean isPreparedBase(String name);

  boolean isBase(String name, CType type);

  NavigableSet<String> getAllBases();

  PersistentList<PointerTarget> getAllTargets(MemoryRegion region);

  Iterable<PointerTarget> getMatchingTargets(MemoryRegion region, Predicate<PointerTarget> pattern);

  Iterable<PointerTarget> getNonMatchingTargets(
      MemoryRegion region, Predicate<PointerTarget> pattern);

  int getFreshAllocationId();

  /** Returns an immutable PointerTargetSet with all the changes made to the builder. */
  PointerTargetSet build();

  /**
   * Actual builder implementation for PointerTargetSet.
   *
   * <p>Its state starts with an existing set, but may be changed later. It supports read access,
   * but it is not recommended to use instances of this class except for the short period of time
   * while creating a new set.
   *
   * <p>This class is not thread-safe.
   */
  final class RealPointerTargetSetBuilder implements PointerTargetSetBuilder {

    private final TypeHandlerWithPointerAliasing typeHandler;
    private final PointerTargetSetManager ptsMgr;
    private final FormulaEncodingWithPointerAliasingOptions options;
    private final MemoryRegionManager regionMgr;

    // These fields all exist in PointerTargetSet and are documented there.
    private PersistentSortedMap<String, CType> bases;
    private PersistentSortedMap<CompositeField, Boolean> fields;
    private PersistentList<Pair<String, DeferredAllocation>> deferredAllocations;
    private PersistentSortedMap<String, PersistentList<PointerTarget>> targets;
    private PersistentList<Formula> highestAllocatedAddresses;
    private int allocationCount;

    /**
     * Creates a new RealPointerTargetSetBuilder.
     *
     * @param pointerTargetSet The underlying PointerTargetSet
     * @param pPtsMgr The PointerTargetSetManager
     * @param pOptions Additional configuration options.
     */
    public RealPointerTargetSetBuilder(
        final PointerTargetSet pointerTargetSet,
        final TypeHandlerWithPointerAliasing pTypeHandler,
        final PointerTargetSetManager pPtsMgr,
        final FormulaEncodingWithPointerAliasingOptions pOptions,
        final MemoryRegionManager pRegionMgr) {
      bases = pointerTargetSet.getBases();
      fields = pointerTargetSet.getFields();
      deferredAllocations = pointerTargetSet.getDeferredAllocations();
      targets = pointerTargetSet.getTargets();
      highestAllocatedAddresses = pointerTargetSet.getHighestAllocatedAddresses();
      allocationCount = pointerTargetSet.getAllocationCount();
      typeHandler = pTypeHandler;
      ptsMgr = pPtsMgr;
      options = pOptions;
      regionMgr = pRegionMgr;
    }

    /**
     * Recursively adds pointer targets for every used (tracked) (sub)field of the newly allocated
     * base.
     *
     * <p>Note: The recursion doesn't proceed on unused (untracked) (sub)fields.
     *
     * @param name The name of the newly allocated base variable
     * @param type The type of the allocated base or the next added pointer target
     */
    private void addTargets(final String name, CType type) {
      targets = ptsMgr.addToTargets(name, null, type, null, 0, 0, targets, fields);
    }

    /**
     * Returns a boolean formula for a prepared base of a pointer.
     *
     * @param name The name of the variable.
     * @param type The type of the variable.
     * @param sizeExp An expression for the size in bytes of the new base.
     */
    @Override
    public void prepareBase(
        final String name, CType type, final @Nullable Formula sizeExp, Constraints constraints) {
      checkIsSimplified(type);
      if (bases.containsKey(name)) {
        // The base has already been added
        return;
      }

      // Add base to prevent adding spurious targets when merging.
      // If type is incomplete, we can use a dummy size here because it is only used for the fake
      // base.
      int size = type.isIncomplete() ? 0 : typeHandler.getSizeof(type);
      bases = bases.putAndCopy(name, PointerTargetSetManager.getFakeBaseType(size));

      makeNextBaseAddressInequality(name, type, sizeExp, constraints);
    }

    /**
     * Shares a base of a pointer.
     *
     * @param name The variable name.
     * @param type The type of the variable.
     */
    @Override
    public void shareBase(final String name, CType type) {
      checkIsSimplified(type);
      // checkArgument(bases.containsKey(name),
      //     "The base should be prepared before with prepareBase()");

      if (type instanceof CElaboratedType) {
        assert ((CElaboratedType) type).getRealType() == null
            : "Elaborated type " + type + " that was not simplified but could have been.";
        // This is the declaration of a variable of an incomplete struct type.
        // We can't access the contents of this variable anyway,
        // so we don't add targets.

      } else {
        addTargets(name, type);
      }

      bases = bases.putAndCopy(name, type);
    }

    /**
     * Adds the newly allocated base of the given type for tracking along with all its tracked
     * (sub)fields (if it is a structure/union) or all its elements (if it is an array).
     *
     * @param name The name of the base
     * @param type The type of the base
     * @param size An expression for the size in bytes of the new base.
     */
    @Override
    public void addBase(
        final String name, CType type, final @Nullable Formula size, Constraints constraints) {
      checkIsSimplified(type);
      if (bases.containsKey(name)) {
        // The base has already been added
        return;
      }

      addTargets(name, type);
      bases = bases.putAndCopy(name, type);

      makeNextBaseAddressInequality(name, type, size, constraints);
    }

    /**
     * Create the constraints for inequality between existing bases and a new base (to prevent
     * overlapping), and store the new base as highest allocated address for when the next base is
     * created.
     *
     * @param newBase The name of the next base.
     * @param type The type of the next base.
     * @param allocationSize An expression for the size in bytes of the new base.
     * @param constraints Where the constraints about addresses will be added to.
     */
    private void makeNextBaseAddressInequality(
        final String newBase,
        final CType type,
        final @Nullable Formula allocationSize,
        final Constraints constraints) {

      highestAllocatedAddresses =
          ptsMgr.makeBaseAddressConstraints(
              newBase, type, allocationSize, highestAllocatedAddresses, constraints);
    }

    /**
     * Returns, whether a field of a composite type is tracked or not.
     *
     * @param field The field.
     * @return True, if the field is already tracked, false otherwise.
     */
    @Override
    public boolean tracksField(CompositeField field) {
      return fields.containsKey(field);
    }

    /**
     * Recursively adds pointer targets for the given base variable when the newly used field is
     * added for tracking.
     *
     * @param base The base variable
     * @param cType The type of the base variable or of the next subfield
     * @param properOffset Either {@code 0} or the offset of the next subfield in its innermost
     *     container
     * @param containerOffset Either {@code 0} or the offset of the innermost container relative to
     *     the base address
     * @param field The newly used field.
     */
    private void addTargets(
        final String base,
        final CType cType,
        final long properOffset,
        final long containerOffset,
        final CompositeField field) {
      checkIsSimplified(cType);
      if (cType instanceof CElaboratedType) {
        // unresolved struct type won't have any targets, do nothing

      } else if (cType instanceof CArrayType) {
        final CArrayType arrayType = (CArrayType) cType;
        final int length = CTypeUtils.getArrayLength(arrayType, options);
        int offset = 0;
        for (int i = 0; i < length; ++i) {
          addTargets(base, arrayType.getType(), offset, containerOffset + properOffset, field);
          offset += typeHandler.getSizeof(arrayType.getType());
        }
      } else if (cType instanceof CCompositeType) {
        final CCompositeType compositeType = (CCompositeType) cType;
        assert compositeType.getKind() != ComplexTypeKind.ENUM
            : "Enums are not composite: " + compositeType;
        final boolean isTargetComposite = compositeType.equals(field.getOwnerType());
        for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
          final OptionalLong offset = typeHandler.getOffset(compositeType, memberDeclaration);
          if (!offset.isPresent()) {
            continue; // TODO this looses values of bit fields
          }
          if (tracksField(CompositeField.of(compositeType, memberDeclaration))) {
            addTargets(
                base,
                memberDeclaration.getType(),
                offset.orElseThrow(),
                containerOffset + properOffset,
                field);
          }
          if (isTargetComposite && memberDeclaration.equals(field.getFieldDeclaration())) {
            MemoryRegion newRegion = regionMgr.makeMemoryRegion(compositeType, memberDeclaration);
            targets =
                ptsMgr.addToTargets(
                    base,
                    newRegion,
                    memberDeclaration.getType(),
                    compositeType,
                    offset.orElseThrow(),
                    containerOffset + properOffset,
                    targets,
                    fields);
          }
        }
      }
    }

    /**
     * Adds a field of a composite type to the tracking.
     *
     * @param field The field.
     * @return True, if the addition of the target was successful, false otherwise
     */
    @Override
    public boolean addField(final CompositeField field) {
      if (tracksField(field)) {
        return true; // The field has already been added
      }

      final PersistentSortedMap<String, PersistentList<PointerTarget>> oldTargets = targets;
      for (final Map.Entry<String, CType> baseEntry : bases.entrySet()) {
        addTargets(baseEntry.getKey(), baseEntry.getValue(), 0, 0, field);
      }
      fields = fields.putAndCopy(field, true);

      return oldTargets != targets;
    }

    /**
     * Should be used to remove the newly added field if it didn't turn out to correspond to any
     * actual pointer target.
     *
     * <p>This can happen if we try to track a field of a composite that has no corresponding
     * allocated bases.
     *
     * @param field The field that should be removed.
     */
    private void shallowRemoveField(final CompositeField field) {
      fields = fields.removeAndCopy(field);
    }

    /**
     * Used to start tracking for fields that were used in some expression or an assignment LHS.
     *
     * <p>Each field is added for tracking only if it's present in some currently allocated object;
     * an inner structure/union field is added only if the field corresponding to the inner
     * composite itself is already tracked; also, a field corresponding to an inner composite is
     * added only if any fields of that composite are already tracked. The latter two optimizations
     * cause problems when adding an inner composite field along with the corresponding containing
     * field e.g.:
     *
     * <p>{@code pouter->inner.f = /*...* /;}
     *
     * <p>Here {@code inner.f} is not added because inner is not yet tracked and {@code outer.inner}
     * is not added because no fields in structure <tt>inner</tt> are tracked. The issue is solved
     * by grouping the requested fields into chunks by their nesting and avoid optimizations when
     * adding fields of the same chunk.
     *
     * @param pFields The fields that should be tracked.
     */
    @Override
    public void addEssentialFields(final List<CompositeField> pFields) {
      final Predicate<CompositeField> isNewField = (compositeField) -> !tracksField(compositeField);

      final Comparator<CompositeField> simpleTypedFieldsFirst =
          (field1, field2) -> {
            final int isField1Simple =
                typeHandler.getSimplifiedType(field1.getFieldDeclaration())
                        instanceof CCompositeType
                    ? 1
                    : 0;
            final int isField2Simple =
                typeHandler.getSimplifiedType(field2.getFieldDeclaration())
                        instanceof CCompositeType
                    ? 1
                    : 0;
            return isField1Simple - isField2Simple;
          };

      final List<CompositeField> typedFields =
          FluentIterable.from(pFields).filter(isNewField).toSortedList(simpleTypedFieldsFirst);
      if (typedFields.isEmpty()) {
        return;
      }
      final Set<CompositeField> done = new HashSet<>();
      final List<CompositeField> currentChain = new ArrayList<>();
      for (final CompositeField field : typedFields) {
        if (!done.contains(field)) {
          currentChain.clear();

          CompositeField current = field;
          do {
            currentChain.add(current);
            for (final CompositeField parentField : typedFields) {
              if (!done.contains(parentField)
                  && typeHandler
                      .getSimplifiedType(parentField.getFieldDeclaration())
                      .equals(current.getOwnerType())) {
                current = parentField;
                break;
              }
            }
          } while (!Objects.equals(current, currentChain.get(currentChain.size() - 1)));

          boolean useful = false;
          for (int i = currentChain.size() - 1; i >= 0; i--) {
            final CompositeField f = currentChain.get(i);
            done.add(f);
            useful |= addField(f);
          }

          if (!useful) {
            for (final CompositeField f : currentChain) {
              shallowRemoveField(f);
            }
          }
        }
      }
    }

    /**
     * Adds a new pointer(variable/field)-object mapping to the set of tracked pending objects with
     * yet unknown type to be allocated. This version is specifically for temporary variables used
     * in between allocation in the RHS and (possibly) revealing the type from the LHS.
     *
     * @param isZeroed A flag indicating if the allocated object is zeroed (e.g. allocated with
     *     kzalloc).
     * @param size The size of the allocated memory (usually specified as allocation function
     *     argument).
     * @param sizeExp A formula representing the size of the allocation.
     * @param base The name of the corresponding base.
     */
    @Override
    public void addTemporaryDeferredAllocation(
        final boolean isZeroed,
        final Optional<CIntegerLiteralExpression> size,
        final Formula sizeExp,
        final String base) {
      final Pair<String, DeferredAllocation> p =
          Pair.of(base, new DeferredAllocation(base, size, sizeExp, isZeroed));
      if (!deferredAllocations.contains(p)) {
        deferredAllocations = deferredAllocations.with(p);
      }
    }

    /**
     * Makes {@code newPointer} alias of all the objects (possibly) addressed by the {@code
     * originalPointer}. This is intended to be used for assignments (after an appropriate call to
     * {@link PointerTargetSetBuilder#removeDeferredAllocationPointer(String)}} if the LHS is a
     * variable).
     *
     * @param newPointer The new alias pointer variable or field.
     * @param originalPointer The original pointer variable or field.
     */
    @Override
    public void addDeferredAllocationPointer(
        final String newPointer, final String originalPointer) {
      final Set<Pair<String, DeferredAllocation>> cache = new HashSet<>(deferredAllocations);
      deferredAllocations.stream()
          .filter((p) -> p.getFirst().equals(originalPointer))
          .forEachOrdered(
              (p) -> {
                final Pair<String, DeferredAllocation> pp = Pair.of(newPointer, p.getSecond());
                if (!cache.contains(pp)) {
                  deferredAllocations = deferredAllocations.with(pp);
                }
              });
    }

    /**
     * Returns {@code false} if there are some yet unallocated objects that are pointed
     * <b>exclusively</b> by the given pointer. Otherwise, returns {@code true}.
     */
    @Override
    public boolean canRemoveDeferredAllocationPointer(final String pointer) {
      final Set<DeferredAllocation> result =
          deferredAllocations.stream()
              .filter((p) -> p.getFirst().equals(pointer))
              .map(Pair::getSecond)
              .collect(toCollection(HashSet::new));
      if (result.isEmpty()) {
        return true;
      }
      deferredAllocations.forEach(
          (p) -> {
            if (!p.getFirst().equals(pointer)) {
              result.remove(p.getSecond());
            }
          });
      return result.isEmpty();
    }

    /**
     * Removes all pointer-object mappings mentioning the specified pointer (variable or field) from
     * the set of tracked pending objects to be allocated. Returns the set of all objects orphaned
     * by this operation (not pointed by any pointer other than the removed one). This is intended
     * to be used when a pointer variable is assigned a new value or is deallocated from stack on
     * function exit.
     *
     * @param pointer The variable or field to be removed.
     * @return The set of all objects orphaned by this operation (not pointed by any pointer other
     *     than the removed one)
     */
    @Override
    public ImmutableSet<DeferredAllocation> removeDeferredAllocationPointer(final String pointer) {
      final Set<DeferredAllocation> result =
          deferredAllocations.stream()
              .filter((p) -> p.getFirst().equals(pointer))
              .map(Pair::getSecond)
              .collect(toCollection(HashSet::new));
      deferredAllocations =
          deferredAllocations.stream()
              .filter((p) -> !p.getFirst().equals(pointer))
              .collect(toPersistentLinkedList());
      deferredAllocations.forEach((p) -> result.remove(p.getSecond()));
      return ImmutableSet.copyOf(result);
    }

    /**
     * Removes all pointer-object mappings concerning any object (possibly) pointed by the specified
     * pointer. Returns the set of removed objects. This is intended to be used when the actual
     * (precise) type of some {@code void *} pointer is revealed (all the objects in the returned
     * set are to be allocated).
     *
     * @param pointer The name of the pointer.
     * @return The resulting set of removed objects.
     */
    @Override
    public ImmutableSet<DeferredAllocation> removeDeferredAllocations(final String pointer) {
      final ImmutableSet<DeferredAllocation> result =
          from(deferredAllocations)
              .filter((p) -> p.getFirst().equals(pointer))
              .transform(Pair::getSecond)
              .toSet();
      deferredAllocations =
          deferredAllocations.stream()
              .filter((p) -> !result.contains(p.getSecond()))
              .collect(toPersistentLinkedList());
      return result;
    }

    /**
     * Returns a set of all pointers to deferred allocations.
     *
     * @return The set of pointers.
     */
    @Override
    public ImmutableSet<String> getDeferredAllocationPointers() {
      return transformedImmutableSetCopy(deferredAllocations, Pair::getFirst);
    }

    /**
     * Checks, if a variable/field is a temporary deferred allocation pointer.
     *
     * @param pointer The variable name.
     * @return True, if the variable/field is a temporary deferred allocation pointer, false
     *     otherwise.
     */
    @Override
    public boolean isTemporaryDeferredAllocationPointer(final String pointer) {
      return deferredAllocations.stream()
          .anyMatch((p) -> p.getFirst().equals(pointer) && p.getSecond().getBase().equals(pointer));
    }

    /**
     * Checks, if a variable/field is a deferred allocation pointer.
     *
     * @param pointer The variable/field.
     * @return True, if the supplied variable/field is a deferred allocation pointer, false
     *     otherwise.
     */
    @Override
    public boolean isDeferredAllocationPointer(final String pointer) {
      return deferredAllocations.stream().anyMatch((p) -> p.getFirst().equals(pointer));
    }

    /**
     * Returns, if a variable is the actual base of a pointer.
     *
     * @param name The name of the variable.
     * @return True, if the variable is an actual base, false otherwise.
     */
    @Override
    public boolean isActualBase(final String name) {
      return bases.containsKey(name) && !PointerTargetSetManager.isFakeBaseType(bases.get(name));
    }

    /**
     * Returns, if a variable name is a prepared base.
     *
     * @param name The name of the variable.
     * @return True, if the variable is a prepared base, false otherwise.
     */
    @Override
    public boolean isPreparedBase(final String name) {
      return bases.containsKey(name);
    }

    /**
     * Checks, if a variable is a base of a pointer.
     *
     * @param name The name of the variable.
     * @param type The type of the variable.
     * @return True, if the variable is a base, false otherwise.
     */
    @Override
    public boolean isBase(final String name, CType type) {
      checkIsSimplified(type);
      final CType baseType = bases.get(name);
      return baseType != null && baseType.equals(type);
    }

    /**
     * Returns a set of all pointer bases.
     *
     * @return A set of all pointer bases.
     */
    @Override
    public NavigableSet<String> getAllBases() {
      return bases.keySet();
    }

    /**
     * Gets a list of all targets of a pointer type.
     *
     * @param region The region of the pointer variable.
     * @return A list of all targets of a pointer type.
     */
    @Override
    public PersistentList<PointerTarget> getAllTargets(final MemoryRegion region) {
      return targets.getOrDefault(
          regionMgr.getPointerAccessName(region), PersistentLinkedList.of());
    }

    /**
     * Gets all matching targets of a pointer target pattern.
     *
     * @param region The region of the pointer variable.
     * @param pattern The pointer target pattern.
     * @return A list of matching pointer targets.
     */
    @Override
    public Iterable<PointerTarget> getMatchingTargets(
        final MemoryRegion region, final Predicate<PointerTarget> pattern) {
      return from(getAllTargets(region)).filter(pattern);
    }

    /**
     * Gets all spurious targets of a pointer target pattern.
     *
     * @param region The region of the pointer variable.
     * @param pattern The pointer target pattern.
     * @return A list of spurious pointer targets.
     */
    @Override
    public Iterable<PointerTarget> getNonMatchingTargets(
        final MemoryRegion region, final Predicate<PointerTarget> pattern) {
      return from(getAllTargets(region)).filter(not(pattern));
    }

    /**
     * Returns an immutable PointerTargetSet with all the changes made to the builder.
     *
     * @return A PointerTargetSet with all changes made to the builder.
     */
    @Override
    public PointerTargetSet build() {
      PointerTargetSet result =
          new PointerTargetSet(
              bases,
              fields,
              deferredAllocations,
              targets,
              highestAllocatedAddresses,
              allocationCount);
      if (result.isEmpty()) {
        return PointerTargetSet.emptyPointerTargetSet();
      } else {
        return result;
      }
    }

    /** Returns a fresh ID that can be used as identifier for a heap allocation. */
    @Override
    public int getFreshAllocationId() {
      return allocationCount = Math.incrementExact(allocationCount);
    }
  }

  /**
   * Dummy implementation of {@link PointerTargetSetBuilder} that throws an exception on all methods
   * except for {@link #build()}, where it returns an empty {@link PointerTargetSet}.
   */
  enum DummyPointerTargetSetBuilder implements PointerTargetSetBuilder {
    INSTANCE;

    @Override
    public void prepareBase(
        String pName, CType pType, @Nullable Formula pSize, Constraints constraints) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void shareBase(String pName, CType pType) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addBase(
        String pName, CType pType, @Nullable Formula pSize, Constraints constraints) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean tracksField(CompositeField pField) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addField(CompositeField pField) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addEssentialFields(List<CompositeField> pFields) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addTemporaryDeferredAllocation(
        boolean pIsZeroed,
        Optional<CIntegerLiteralExpression> pSize,
        Formula pSizeExp,
        String pBase) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addDeferredAllocationPointer(String pNewPointer, String pOriginalPointer) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableSet<DeferredAllocation> removeDeferredAllocationPointer(String pPointer) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableSet<DeferredAllocation> removeDeferredAllocations(String pPointer) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableSet<String> getDeferredAllocationPointers() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTemporaryDeferredAllocationPointer(String pPointerVariable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDeferredAllocationPointer(String pPointerVariable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isActualBase(String pName) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPreparedBase(String pName) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isBase(String pName, CType pType) {
      throw new UnsupportedOperationException();
    }

    @Override
    public NavigableSet<String> getAllBases() {
      throw new UnsupportedOperationException();
    }

    @Override
    public PersistentList<PointerTarget> getAllTargets(MemoryRegion region) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<PointerTarget> getMatchingTargets(
        MemoryRegion region, Predicate<PointerTarget> pPattern) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<PointerTarget> getNonMatchingTargets(
        MemoryRegion region, Predicate<PointerTarget> pPattern) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getFreshAllocationId() {
      throw new UnsupportedOperationException();
    }

    @Override
    public PointerTargetSet build() {
      return PointerTargetSet.emptyPointerTargetSet();
    }

    @Override
    public boolean canRemoveDeferredAllocationPointer(String pPointer) {
      throw new UnsupportedOperationException();
    }
  }
}
