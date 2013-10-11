/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.withUF;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.sosy_lab.common.Triple;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.MachineModel.BaseSizeofVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTarget;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTargetPattern;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class PointerTargetSet implements Serializable {

  private static class CompositeField implements Comparable<CompositeField> {
    private CompositeField(final String compositeType, final String fieldName) {
      this.compositeType = compositeType;
      this.fieldName = fieldName;
    }

    public static CompositeField of(final @Nonnull String compositeType, final @Nonnull String fieldName) {
      return new CompositeField(compositeType, fieldName);
    }

//    public String compositeType() {
//      return compositeType;
//    }

//    public String fieldName() {
//      return fieldName;
//    }

    @Override
    public String toString() {
      return compositeType + "." + fieldName;
    }

    @Override
    public int compareTo(final CompositeField other) {
      final int result = this.compositeType.compareTo(other.compositeType);
      if (result != 0) {
        return result;
      } else {
        return this.fieldName.compareTo(other.fieldName);
      }
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      } else if (!(obj instanceof CompositeField)) {
        return false;
      } else {
        CompositeField other = (CompositeField) obj;
        return compositeType.equals(other.compositeType) && fieldName.equals(other.fieldName);
      }
    }

    @Override
    public int hashCode() {
      return compositeType.hashCode() * 17 + fieldName.hashCode();
    }

    private String compositeType;
    private String fieldName;
  }

  public static class CSizeofVisitor extends BaseSizeofVisitor
                                     implements CTypeVisitor<Integer, IllegalArgumentException> {

    public CSizeofVisitor(final CEvaluatingVisitor evaluatingVisitor) {
      super(evaluatingVisitor.getMachineModel());
      this.evaluatingVisitor = evaluatingVisitor;
    }

    @Override
    public Integer visit(final CArrayType t) throws IllegalArgumentException {

      final CExpression arrayLength = t.getLength();
      assert arrayLength != null : "CFA should be transformed to eliminate unsized arrays";

      Integer length = null;

      if (arrayLength != null) {
        length = arrayLength.accept(evaluatingVisitor);
      }

      if (length == null) {
        length = DEFAULT_ARRAY_LENGTH;
      }

      final int sizeOfType = t.getType().accept(this);
      return length * sizeOfType;
    }

    private final CEvaluatingVisitor evaluatingVisitor;
  }

  public static boolean containsArray(CType type) {
    type = simplifyType(type);
    if (type instanceof CArrayType) {
      return true;
    } else if (type instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) type;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite!";
      for (CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        if (containsArray(memberDeclaration.getType())) {
          return true;
        }
      }
      return false;
    } else {
      return false;
    }
  }

  public static CType getBaseType(CType type) {
    type = simplifyType(type);
    if (!(type instanceof CArrayType)) {
      return new CPointerType(true, false, type);
    } else {
      return new CPointerType(true, false, ((CArrayType) type).getType());
    }
  }

  public static CType simplifyType(final CType type) {
    CType canonicalType = canonicalTypeCache.get(type);
    if (canonicalType != null) {
      return canonicalType;
    } else {
      canonicalType = type.getCanonicalType();
      canonicalTypeCache.put(type, canonicalType);
      return canonicalType;
    }
  }

  public static String typeToString(final CType type) {
    return simplifyType(type).toString();
  }

  public int getSize(CType cType) {
    cType = simplifyType(cType);
    if (cType instanceof CCompositeType) {
      if (sizes.contains(cType)) {
        return sizes.count(cType);
      } else {
        return cType.accept(sizeofVisitor);
      }
    } else {
      return cType.accept(sizeofVisitor);
    }
  }

  public int getOffset(CCompositeType compositeType, final String memberName) {
    compositeType = (CCompositeType) simplifyType(compositeType);
    assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;
    return offsets.get(compositeType).count(memberName);
  }

  private static Iterable<PointerTarget> getTargets(
                                            final CType type,
                                            final PointerTargetPattern pattern,
                                            final boolean matches,
                                            final PersistentSortedMap<String, PersistentList<PointerTarget>> targets) {
    final List<PointerTarget> targetsForType = targets.get(typeToString(type));
    final Iterator<PointerTarget> resultIterator = new Iterator<PointerTarget>() {

      @Override
      public boolean hasNext() {
        if (last != null) {
          return true;
        }
        while (iterator.hasNext()) {
          last = iterator.next();
          if (pattern.matches(last) == matches) {
            return true;
          }
        }
        return false;
      }

      @Override
      public PointerTarget next() {
        PointerTarget result = last;
        if (result != null) {
          last = null;
          return result;
        }
        while (iterator.hasNext()) {
          result = iterator.next();
          if (pattern.matches(result) == matches) {
            return result;
          }
        }
        return null;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      private Iterator<PointerTarget> iterator = targetsForType != null ? targetsForType.iterator() :
                                                                          Collections.<PointerTarget>emptyList()
                                                                                     .iterator();
      private PointerTarget last = null;
     };
     return new Iterable<PointerTarget>() {

      @Override
      public Iterator<PointerTarget> iterator() {
        return resultIterator;
      }
    };
  }

  public Iterable<PointerTarget> getMatchingTargets(final CType type,
                                                    final PointerTargetPattern pattern) {
    return getTargets(type, pattern, true, targets);
  }

  public Iterable<PointerTarget> getSpuriousTargets(final CType type,
                                                    final PointerTargetPattern pattern) {
    return getTargets(type, pattern, false, targets);
  }

  /**
   * Builder for PointerTargetSet. Its state starts with an existing set, but may be
   * changed later. It supports read access, but it is not recommended to use
   * instances of this class except for the short period of time
   * while creating a new set.
   *
   * This class is not thread-safe.
   */
  public static class PointerTargetSetBuilder {

    private PointerTargetSetBuilder(final PointerTargetSet pointerTargetSet) {
      this.pointerTargetSet = pointerTargetSet;

      this.bases = pointerTargetSet.bases;
      this.fields = pointerTargetSet.fields;

      this.targets = pointerTargetSet.targets;
      this.disjointnessFormula = pointerTargetSet.disjointnessFormula;

      final MachineModel machineModel = pointerTargetSet.evaluatingVisitor.getMachineModel();
      final int pointerSize = machineModel.getSizeofPtr();
      final int bitsPerByte = machineModel.getSizeofCharInBits();
      this.pointerType = pointerTargetSet.formulaManager.getBitvectorFormulaManager()
                                                        .getFormulaType(pointerSize * bitsPerByte);
    }

    public void addCompositeType(CCompositeType compositeType) {
      compositeType = (CCompositeType) simplifyType(compositeType);
      if (sizes.contains(compositeType)) {
        assert offsets.containsKey(compositeType) : "Illegal state of PointerTargetSet: no offsets for type:" +
                                                    compositeType;
        return; // The type has already been added
      }

      final Integer size = compositeType.accept(pointerTargetSet.sizeofVisitor);

      assert size != null : "Can't evaluate size of a composite type: " + compositeType;

      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;

      final Multiset<String> members = HashMultiset.create();

      int offset = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        members.setCount(memberDeclaration.getName(), offset);
        final CType memberType = simplifyType(memberDeclaration.getType());
        final CCompositeType memberCompositeType;
        if (memberType instanceof CCompositeType) {
          memberCompositeType = (CCompositeType) memberType;
          if (memberCompositeType.getKind() == ComplexTypeKind.STRUCT ||
              memberCompositeType.getKind() == ComplexTypeKind.UNION) {
            if (!sizes.contains(memberCompositeType)) {
              assert !offsets.containsKey(memberCompositeType) :
                       "Illegal state of PointerTargetSet: offsets for type:" + memberCompositeType;
              addCompositeType(memberCompositeType);
            }
          }
        } else {
          memberCompositeType = null;
        }
        if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
          if (memberCompositeType != null) {
            offset += sizes.count(memberCompositeType);
          } else {
            offset += memberDeclaration.getType().accept(pointerTargetSet.sizeofVisitor);
          }
        }
      }

      assert compositeType.getKind() != ComplexTypeKind.STRUCT || offset == size :
             "Incorrect sizeof or offset of the last member: " + compositeType;

      sizes.setCount(compositeType, size);
      offsets.put(compositeType, members);
    }

    public int getSize(final CType cType) {
      return pointerTargetSet.getSize(cType);
    }

    public int getOffset(final CCompositeType compositeType, final String memberName) {
      return pointerTargetSet.getOffset(compositeType, memberName);
    }

    public Iterable<PointerTarget> getMatchingTargets(final CType type,
                                                      final PointerTargetPattern pattern) {
      return getTargets(type, pattern, true, targets);
    }

    public Iterable<PointerTarget> getSpuriousTargets(final CType type,
                                                      final PointerTargetPattern pattern) {
      return getTargets(type, pattern, false, targets);
    }

    private void addTarget(final String base,
                           final CType targetType,
                           final CType containerType,
                           final int properOffset,
                           final int containerOffset) {
      final String type = typeToString(targetType);
      PersistentList<PointerTarget> targetsForType = targets.get(type);
      if (targetsForType == null) {
        targetsForType = PersistentList.<PointerTarget>empty();
      }
      targets = targets.putAndCopy(type, targetsForType.with(new PointerTarget(base,
                                                                               containerType,
                                                                               properOffset,
                                                                               containerOffset)));
      flag = true;
    }

    private void addTargets(final String base,
                            final CType currentType,
                            final CType containerType,
                            final int properOffset,
                            final int containerOffset) {
      final CType cType = simplifyType(currentType);
      assert !(cType instanceof CElaboratedType) : "Unresolved elaborated type:" + cType;
      if (cType instanceof CArrayType) {
        final CArrayType arrayType = (CArrayType) cType;
        assert arrayType.getLength() != null : "CFA should be transformed to eliminate unsized arrays";
        Integer length = arrayType.getLength().accept(pointerTargetSet.evaluatingVisitor);
        if (length == null) {
          length = DEFAULT_ARRAY_LENGTH;
        }
        int offset = 0;
        for (int i = 0; i < length; ++i) {
          addTargets(base, arrayType.getType(), arrayType, offset, containerOffset + properOffset);
          offset += getSize(arrayType.getType());
        }
      } else if (cType instanceof CCompositeType) {
        final CCompositeType compositeType = (CCompositeType) cType;
        assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;
        final String type = typeToString(compositeType);
        addCompositeType(compositeType);
        int offset = 0;
        for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
          if (fields.containsKey(CompositeField.of(type, memberDeclaration.getName()))) {
            addTargets(base, memberDeclaration.getType(), compositeType, offset, containerOffset + properOffset);
          }
          if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
            offset += getSize(memberDeclaration.getType());
          }
        }
      } else {
        addTarget(base, cType, containerType, properOffset, containerOffset);
      }
    }

    public boolean addBase(final String name, CType type) {
      type = simplifyType(type);
      if (bases.containsKey(name)) {
        return true; // The base has already been added
      }

      final FormulaManagerView fm = pointerTargetSet.formulaManager;
      final Formula base = fm.makeVariable(pointerType, name);
      if (bases.isEmpty()) { // Initial assumption b_0 > 0
        disjointnessFormula = fm.makeGreaterThan(base, fm.makeNumber(pointerType, 0L), true);
      } else { // b_i >= b_{i-1} + sizeof(b_{i-1}) && b_{i-1}  + sizeof(b_{i-1}) > 0
        final String lastBase = bases.lastKey();
        final int lastSize = getSize(bases.get(lastBase));
        final Formula  rhs = fm.makePlus(fm.makeVariable(pointerType, lastBase), fm.makeNumber(pointerType, lastSize));
        disjointnessFormula = fm.makeAnd(disjointnessFormula,
                                         fm.makeAnd(fm.makeGreaterThan(rhs, fm.makeNumber(pointerType, 0L), true),
                                                    fm.makeGreaterOrEqual(base, rhs, true)));
      }

      flag = false;
      addTargets(name, type, null, 0, 0);
      bases = bases.putAndCopy(name, type);
      return flag;
    }

    BooleanFormula addBase(final BooleanFormula disjointnessFormula,
                           final String name,
                           final String lastBase,
                           final int lastSize) {
      final FormulaManagerView fm = pointerTargetSet.formulaManager;
      final Formula base = fm.makeVariable(pointerType, name);
      final Formula rhs = fm.makePlus(fm.makeVariable(pointerType, lastBase), fm.makeNumber(pointerType, lastSize));
      return  fm.makeAnd(disjointnessFormula,
                         fm.makeAnd(fm.makeGreaterThan(rhs, fm.makeNumber(pointerType, 0L), true),
                                    fm.makeGreaterOrEqual(base, rhs, true)));
    }

    public boolean isBase(final String name) {
      return bases.containsKey(name);
    }

    public boolean isBase(final String name, CType type) {
      type = simplifyType(type);
      final CType baseType = bases.get(name);
      return baseType != null && baseType.equals(type);
    }

    public boolean tracksField(final CCompositeType compositeType, final String fieldName) {
      return fields.containsKey(CompositeField.of(typeToString(compositeType), fieldName));
    }

    public FormulaType<?> getPointerType() {
      return pointerType;
    }

    private void addTargets(final String base,
                            final CType currentType,
                            final CType containerType,
                            final int properOffset,
                            final int containerOffset,
                            final String composite,
                            final String memberName) {
      final CType cType = simplifyType(currentType);
      assert !(cType instanceof CElaboratedType) : "Unresolved elaborated type:" + cType;
      if (cType instanceof CArrayType) {
        final CArrayType arrayType = (CArrayType) cType;
        assert arrayType.getLength() != null : "CFA should be transformed to eliminate unsized arrays";
        Integer length = arrayType.getLength().accept(pointerTargetSet.evaluatingVisitor);
        if (length == null) {
          length = DEFAULT_ARRAY_LENGTH;
        }
        int offset = 0;
        for (int i = 0; i < length; ++i) {
          addTargets(base, arrayType.getType(), arrayType, offset, containerOffset + properOffset,
                     composite, memberName);
          offset += getSize(arrayType.getType());
        }
      } else if (cType instanceof CCompositeType) {
        final CCompositeType compositeType = (CCompositeType) cType;
        assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;
        final String type = typeToString(compositeType);
        int offset = 0;
        final boolean isTargetComposite = type.equals(composite);
        for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
          if (fields.containsKey(CompositeField.of(type, memberDeclaration.getName()))) {
            addTargets(base, memberDeclaration.getType(), compositeType, offset, containerOffset + properOffset,
                       composite, memberName);
          }
          if (isTargetComposite && memberDeclaration.getName().equals(memberName)) {
            addTargets(base, memberDeclaration.getType(), compositeType, offset, containerOffset + properOffset);
          }
          if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
            offset += getSize(memberDeclaration.getType());
          }
        }
      }
    }

    public boolean addField(final CCompositeType composite, final String fieldName) {
      final String type = typeToString(composite);
      final CompositeField field = CompositeField.of(type, fieldName);
      if (fields.containsKey(field)) {
        return true; // The field has already been added
      }
      flag = false;
      for (final PersistentSortedMap.Entry<String, CType> baseEntry : bases.entrySet()) {
        addTargets(baseEntry.getKey(), baseEntry.getValue(), null, 0, 0, type, fieldName);
      }
      fields = fields.putAndCopy(field, true);
      return flag;
    }

    public void shallowRemoveField(final CCompositeType composite, final String fieldName) {
      final String type = typeToString(composite);
      final CompositeField field = CompositeField.of(type, fieldName);
      fields = fields.removeAndCopy(field);
    }

    void setFields(PersistentSortedMap<CompositeField, Boolean> fields) {
      this.fields = fields;
    }

    /**
     * Returns an immutable PointerTargetSet with all the changes made to the builder.
     */
    public PointerTargetSet build() {
      return new PointerTargetSet(pointerTargetSet.evaluatingVisitor,
                                  pointerTargetSet.sizeofVisitor,
                                  bases,
                                  fields,
                                  targets,
                                  disjointnessFormula,
                                  pointerTargetSet.formulaManager);
    }

    public CEvaluatingVisitor getEvaluatingVisitor() {
      return pointerTargetSet.evaluatingVisitor;
    }

    private boolean flag; // Used by addBase() addField() to detect essential additions

    private final PointerTargetSet pointerTargetSet;

    private PersistentSortedMap<String, CType> bases;
    private PersistentSortedMap<CompositeField, Boolean> fields;

    private PersistentSortedMap<String, PersistentList<PointerTarget>> targets;
    private BooleanFormula disjointnessFormula;
    private final FormulaType<?> pointerType;
  }

  public BooleanFormula withDisjointnessConstraints(final BooleanFormula formula) {
    return formulaManager.getBooleanFormulaManager().and(formula, disjointnessFormula);
  }

  public boolean isBase(final String name) {
    return bases.containsKey(name);
  }

  public boolean isBase(final String name, CType type) {
    type = simplifyType(type);
    final CType baseType = bases.get(name);
    return baseType != null && baseType.equals(type);
  }

  public static final PointerTargetSet emptyPointerTargetSet(final MachineModel machineModel,
                                                              final BooleanFormula truthFormula,
                                                              final FormulaManagerView formulaManager) {
    final CEvaluatingVisitor evaluatingVisitor = new CEvaluatingVisitor(machineModel);
    return new PointerTargetSet(evaluatingVisitor,
                                new CSizeofVisitor(evaluatingVisitor),
                                PathCopyingPersistentTreeMap.<String, CType>of(),
                                PathCopyingPersistentTreeMap.<CompositeField, Boolean>of(),
                                PathCopyingPersistentTreeMap.<String, PersistentList<PointerTarget>>of(),
                                truthFormula,
                                formulaManager);
  }

  public PointerTargetSet mergeWith(final PointerTargetSet other) {
    final boolean reverseBases = other.bases.size() > bases.size();
    final Triple<PersistentSortedMap<String, CType>,
                 PersistentSortedMap<String, CType>,
                 PersistentSortedMap<String, CType>> mergedBases =
      !reverseBases ? mergeSortedSets(bases, other.bases) : mergeSortedSets(other.bases, bases);

    final boolean reverseFields = other.fields.size() > fields.size();
    final Triple<PersistentSortedMap<CompositeField, Boolean>,
                 PersistentSortedMap<CompositeField, Boolean>,
                 PersistentSortedMap<CompositeField, Boolean>> mergedFields =
      !reverseBases ? mergeSortedSets(fields, other.fields) : mergeSortedSets(other.fields, fields);

    final boolean reverseTargets = other.targets.size() > targets.size();
    final PersistentSortedMap<String, PersistentList<PointerTarget>> mergedTargets =
      !reverseTargets ? mergeSortedMaps(targets, other.targets,PointerTargetSet.<PointerTarget>mergeOnConflict()) :
                        mergeSortedMaps(other.targets, targets, PointerTargetSet.<PointerTarget>mergeOnConflict());

    final PointerTargetSetBuilder builder1 = new PointerTargetSetBuilder(emptyPointerTargetSet(
                                                                               evaluatingVisitor.getMachineModel(),
                                                                               formulaManager.getBooleanFormulaManager()
                                                                                             .makeBoolean(true),
                                                                               formulaManager)),
                                  builder2 = new PointerTargetSetBuilder(emptyPointerTargetSet(
                                                                               evaluatingVisitor.getMachineModel(),
                                                                               formulaManager.getBooleanFormulaManager()
                                                                                             .makeBoolean(true),
                                                                               formulaManager));
    if (reverseBases == reverseFields) {
      builder1.setFields(mergedFields.getFirst());
      builder2.setFields(mergedFields.getSecond());
    } else {
      builder1.setFields(mergedFields.getSecond());
      builder2.setFields(mergedFields.getFirst());
    }

    for (final Map.Entry<String, CType> entry : mergedBases.getSecond().entrySet()) {
      builder1.addBase(entry.getKey(), entry.getValue());
    }

    for (final Map.Entry<String, CType> entry : mergedBases.getFirst().entrySet()) {
      builder2.addBase(entry.getKey(), entry.getValue());
    }

    BooleanFormula disjointessFormula;
    String lastBase;
    int lastSize;
    if (reverseBases) {
      disjointessFormula = this.disjointnessFormula;
      lastBase = bases.lastKey();
      lastSize = getSize(bases.get(lastBase));

    } else {
      disjointessFormula = other.disjointnessFormula;
      lastBase = other.bases.lastKey();
      lastSize = other.getSize(other.bases.get(lastBase));
    }

    for (final Map.Entry<String, CType> entry : mergedBases.getSecond().entrySet()) {
      disjointessFormula = builder1.addBase(disjointessFormula, entry.getKey(), lastBase, lastSize);
      lastBase = entry.getKey();
      lastSize = builder1.getSize(entry.getValue());
    }

    final ConflictHandler<PersistentList<PointerTarget>> conflictHandler =
                                                           PointerTargetSet.<PointerTarget>destructiveMergeOnConflict();

    return new PointerTargetSet(evaluatingVisitor,
                                sizeofVisitor,
                                mergedBases.getThird(),
                                mergedFields.getThird(),
                                mergeSortedMaps(
                                  mergeSortedMaps(mergedTargets,
                                                  builder1.targets,
                                                  conflictHandler),
                                  builder2.targets,
                                  conflictHandler),
                                disjointnessFormula,
                                formulaManager);
  }

  private static <T> PersistentList<T> mergePersistentLists(final PersistentList<T> list1,
                                                            final PersistentList<T> list2) {
    final int size1 = list1.size();
    final ArrayList<T> arrayList1 = new ArrayList<>(size1);
    for (final T element : list1) {
      arrayList1.add(element);
    }
    final int size2 = list2.size();
    final ArrayList<T> arrayList2 = new ArrayList<>(size2);
    for (final T element : list1) {
      arrayList2.add(element);
    }
    int sizeCommon = 0;
    for (int i = 0; i < arrayList1.size() && arrayList1.get(i).equals(arrayList2.get(i)); i++) {
      ++sizeCommon;
    }
    PersistentList<T> result;
    final ArrayList<T> biggerArrayList, smallerArrayList;
    final int biggerCommonStart, smallerCommonStart;
    if (size1 > size2) {
      result = list1;
      biggerArrayList = arrayList1;
      smallerArrayList = arrayList2;
      biggerCommonStart = size1 - sizeCommon;
      smallerCommonStart = size2 - sizeCommon;
    } else {
      result = list2;
      biggerArrayList = arrayList2;
      smallerArrayList = arrayList1;
      biggerCommonStart = size2 - sizeCommon;
      smallerCommonStart = size1 - sizeCommon;
    }
    final Set<T> fromBigger = new HashSet<>(2 * biggerCommonStart, 1.0f);
    for (int i = 0; i < biggerCommonStart; i++) {
      fromBigger.add(biggerArrayList.get(i));
    }
    for (int i = 0; i < smallerCommonStart; i++) {
      final T target = smallerArrayList.get(i);
      if (!fromBigger.contains(target)) {
        result = result.with(target);
      }
    }
    return result;
  }

  private static <K extends Comparable<? super K>, V> Triple<PersistentSortedMap<K, V>,
                                                     PersistentSortedMap<K, V>,
                                                     PersistentSortedMap<K, V>> mergeSortedSets(
    final PersistentSortedMap<K, V> set1,
    final PersistentSortedMap<K, V> set2) {

    PersistentSortedMap<K, V> fromSet1 = PathCopyingPersistentTreeMap.<K, V>of();
    PersistentSortedMap<K, V> fromSet2 = PathCopyingPersistentTreeMap.<K, V>of();
    PersistentSortedMap<K, V> union = set1; // Here we assume that the set1 is bigger

    final Iterator<Map.Entry<K, V>> it1 = set1.entrySet().iterator();
    final Iterator<Map.Entry<K, V>> it2 = set2.entrySet().iterator();

    Map.Entry<K, V> e1 = null;
    Map.Entry<K, V> e2 = null;

    // This loop iterates synchronously through both sets
    // by trying to keep the keys equal.
    // If one iterator fails behind, the other is not forwarded until the first catches up.
    // The advantage of this is it is in O(n log(n))
    // (n iterations, log(n) per update).
    while (it1.hasNext() && it2.hasNext()) {
      if (e1 == null) {
        e1 = it1.next();
      }
      if (e2 == null) {
        e2 = it2.next();
      }

      final int flag = e1.getKey().compareTo(e2.getKey());

      if (flag < 0) {
        // e1 < e2
        fromSet1 = fromSet1.putAndCopy(e1.getKey(), e1.getValue());
        // Forward e1 until it catches up with e2
        e1 = null;
      } else if (flag > 0) {
        // e1 > e2
        fromSet2 = fromSet2.putAndCopy(e2.getKey(), e2.getValue());
        assert !union.containsKey(e2.getKey());
        union = union.putAndCopy(e2.getKey(), e2.getValue());
        // Forward e2 until it catches up with e1
        e2 = null;
      } else {
        // e1 == e2
        assert e1.getValue().equals(e1.getValue()) : "Can't merge maps as sets: values differ";
        // Forward both iterators
        e1 = null;
        e2 = null;
      }
    }

    while (it1.hasNext()) {
      e1 = it1.next();
      fromSet1 = fromSet1.putAndCopy(e1.getKey(), e1.getValue());
    }

    while (it2.hasNext()) {
      e2 = it2.next();
      fromSet2 = fromSet2.putAndCopy(e2.getKey(), e2.getValue());
      assert !union.containsKey(e2.getKey());
      union = union.putAndCopy(e2.getKey(), e2.getValue());
    }

    return Triple.of(fromSet1, fromSet2, union);
  }

  /**
   * Merge two PersistentSortedMaps.
   * The result has all key-value pairs where the key is only in one of the map,
   * those which are identical in both map,
   * and for those keys that have a different value in both maps a handler is called,
   * and the result is put in the resulting map.
   * @param map1 The first map.
   * @param map2 The second map.
   * @param conflictHandler The handler that is called for a key with two different values.
   * @return
   */
  private static <K extends Comparable<? super K>, V> PersistentSortedMap<K, V> mergeSortedMaps(
    final PersistentSortedMap<K, V> map1,
    final PersistentSortedMap<K, V> map2,
    final ConflictHandler<V> conflictHandler) {

    // map1 is the bigger one, so we use it as the base.
    PersistentSortedMap<K, V> result = map1;

    final Iterator<Map.Entry<K, V>> it1 = map1.entrySet().iterator();
    final Iterator<Map.Entry<K, V>> it2 = map2.entrySet().iterator();

    Map.Entry<K, V> e1 = null;
    Map.Entry<K, V> e2 = null;

    // This loop iterates synchronously through both sets
    // by trying to keep the keys equal.
    // If one iterator fails behind, the other is not forwarded until the first catches up.
    // The advantage of this is it is in O(n log(n))
    // (n iterations, log(n) per update).
    while (it1.hasNext() && it2.hasNext()) {
      if (e1 == null) {
        e1 = it1.next();
      }
      if (e2 == null) {
        e2 = it2.next();
      }

      final int flag = e1.getKey().compareTo(e2.getKey());

      if (flag < 0) {
        // e1 < e2
        // forward e1 until it catches up with e2
        e1 = null;
      } else if (flag > 0) {
        // e1 > e2
        // e2 is not in map
        assert !result.containsKey(e2.getKey());
        result = result.putAndCopy(e2.getKey(), e2.getValue());
        // forward e2 until it catches up with e1
        e2 = null;
      } else {
        // e1 == e2
        final K key = e1.getKey();
        final V value1 = e1.getValue();
        final V value2 = e2.getValue();

        if (!value1.equals(value2)) {
          result = result.putAndCopy(key, conflictHandler.resolveConflict(value1, value2));
        }
        // forward both iterators
        e1 = null;
        e2 = null;
      }
    }

    // Now copy the rest of the mappings from s2.
    // For s1 this is not necessary.
    while (it2.hasNext()) {
      e2 = it2.next();
      result = result.putAndCopy(e2.getKey(), e2.getValue());
    }

    assert result.size() >= Math.max(map1.size(), map2.size());
    return result;
  }

  private static interface ConflictHandler<V> {
    public V resolveConflict(V value1, V value2);
  }

  private static <T> ConflictHandler<PersistentList<T>> mergeOnConflict() {
    return new ConflictHandler<PersistentList<T>>() {
      @Override
      public PersistentList<T> resolveConflict(PersistentList<T> list1, PersistentList<T> list2) {
        return mergePersistentLists(list1, list2);
      }
    };
  }

  private static <T> ConflictHandler<PersistentList<T>> destructiveMergeOnConflict() {
    return new ConflictHandler<PersistentList<T>>() {
      @Override
      public PersistentList<T> resolveConflict(PersistentList<T> list1, PersistentList<T> list2) {
        return list2.destructiveBuildOnto(list1);
      }
    };
  }

  @Override
  public String toString() {
    return joiner.join(bases.entrySet()) + " " + joiner.join(fields.entrySet());
  }

  @Override
  public int hashCode() {
    return (31 + bases.keySet().hashCode()) * 31 + fields.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof PointerTargetSet)) {
      return false;
    } else {
      PointerTargetSet other = (PointerTargetSet) obj;
      return bases.equals(other.bases) && fields.equals(other.fields);
    }
  }

  private PointerTargetSet(final CEvaluatingVisitor evaluatingVisitor,
                           final CSizeofVisitor sizeofVisitor,
                           final PersistentSortedMap<String, CType> bases,
                           final PersistentSortedMap<CompositeField, Boolean> fields,
                           final PersistentSortedMap<String, PersistentList<PointerTarget>> targets,
                           final BooleanFormula disjointnessFormula,
                           final FormulaManagerView formulaManager) {
    this.evaluatingVisitor = evaluatingVisitor;
    this.sizeofVisitor = sizeofVisitor;

    this.formulaManager = formulaManager;

    this.bases = bases;
    this.fields = fields;

    this.targets = targets;
    this.disjointnessFormula = disjointnessFormula;
  }

  /**
   * Returns a PointerTargetSetBuilder that is initialized with the current PointerTargetSet.
   */
  public PointerTargetSetBuilder builder() {
    return new PointerTargetSetBuilder(this);
  }

  private static final Joiner joiner = Joiner.on(" ");

  private final CEvaluatingVisitor evaluatingVisitor;
  private final CSizeofVisitor sizeofVisitor;

  private final FormulaManagerView formulaManager;

  /*
   * Use Multiset<String> instead of Map<String, Integer> because it is more
   * efficient. The integer value is stored as the number of instances of any
   * element in the Multiset. So instead of calling map.get(key) we just use
   * Multiset.count(key). This is better because the Multiset internally uses
   * modifiable integers instead of the immutable Integer class.
   */
  private static final Multiset<CCompositeType> sizes = HashMultiset.create();
  private static final Map<CCompositeType, Multiset<String>> offsets = new HashMap<>();

  private final PersistentSortedMap<String, CType> bases;
  private final PersistentSortedMap<CompositeField, Boolean> fields;

  private final PersistentSortedMap<String, PersistentList<PointerTarget>> targets;
  private final BooleanFormula disjointnessFormula;

  public static final int DEFAULT_ARRAY_LENGTH = 100;
  public static final int DEFAULT_ALLOCATION_SIZE = 4;

  public static final CSimpleType CONST_CHAR =
    new CSimpleType(true, false, CBasicType.CHAR, false, false, true, false, false, false, false);
  public static final CType VOID =
    new CSimpleType(false, false, CBasicType.VOID, false, false, false, false, false, false, false);
  public static final CType POINTER_TO_VOID = new CPointerType(true, false, VOID);

  private static final Map<CType, CType> canonicalTypeCache = new HashMap<>();

  static {
    canonicalTypeCache.put(CONST_CHAR, CONST_CHAR);
    canonicalTypeCache.put(VOID, VOID);
    canonicalTypeCache.put(POINTER_TO_VOID, POINTER_TO_VOID);
  }

  private static final long serialVersionUID = 2102505458322248624L;
}

