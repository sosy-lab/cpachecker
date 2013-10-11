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
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.MachineModel.BaseSizeofVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeUtils;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTarget;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class PointerTargetSet implements Serializable {

  private static class CompositeField implements Comparable<CompositeField> {
    private CompositeField(final String compositeType, final String fieldName) {
      this.compositeType = compositeType;
      this.fieldName = fieldName;
    }

    public static CompositeField of(final String compositeType, final String fieldName) {
      return new CompositeField(compositeType, fieldName);
    }

    public String compositeType() {
      return compositeType;
    }

    public String fieldName() {
      return fieldName;
    }

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

  public static String cTypeToString(CType type) {
    return CTypeUtils.simplifyType(type).toString();
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
    }

    public void addCompositeType(final CCompositeType compositeType) {
      String type = cTypeToString(compositeType);
      if (sizes.contains(type)) {
        assert offsets.containsKey(type) : "Illegal state of PointerTargetSet: no offsets for type:" + type;
        return; // The type has already been added
      }

      final Integer size = compositeType.accept(pointerTargetSet.sizeofVisitor);

      assert size != null : "Can't evaluate size of a composite type: " + compositeType;

      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;

      final Multiset<String> members = HashMultiset.create();

      int offset = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        members.setCount(memberDeclaration.getName(), offset);
        final CType cType = CTypeUtils.simplifyType(memberDeclaration.getType());
        type = null;
        if (cType instanceof CCompositeType) {
          final CCompositeType compositeMemberType = (CCompositeType) cType;
          if (compositeMemberType.getKind() == ComplexTypeKind.STRUCT ||
              compositeMemberType.getKind() == ComplexTypeKind.UNION) {
            type = cTypeToString(compositeMemberType);
            if (!sizes.contains(type)) {
              assert !offsets.containsKey(type) : "Illegal state of PointerTargetSet: offsets for type:" + type;
              addCompositeType(compositeMemberType);
            }
          }
        }
        if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
          if (type != null) {
            offset += sizes.count(type);
          } else {
            offset += memberDeclaration.getType().accept(pointerTargetSet.sizeofVisitor);
          }
        }
      }

      assert offset == size : "Incorrect sizeof or offset of the last member: " + compositeType;

      sizes.setCount(type, size);
      offsets.put(type, members);
    }

    public int getSize(CType cType) {
      cType = CTypeUtils.simplifyType(cType);
      if (cType instanceof CCompositeType) {
        final String type = cTypeToString(cType);
        if (sizes.contains(type)) {
          return sizes.count(type);
        } else {
          return cType.accept(pointerTargetSet.sizeofVisitor);
        }
      } else {
        return cType.accept(pointerTargetSet.sizeofVisitor);
      }
    }

    public int getOffset(final CCompositeType compositeType, final String memberName) {
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;
      final String type = cTypeToString(compositeType);
      return offsets.get(type).count(memberName);
    }

    private void addTarget(final String base,
                           final CType targetType,
                           final CType containerType,
                           final int properOffset,
                           final int containerOffset) {
      final String type = cTypeToString(targetType);
      PersistentList<PointerTarget> targetsForType = targets.get(type);
      if (targetsForType == null) {
        targetsForType = PersistentList.<PointerTarget>empty();
      }
      targets = targets.putAndCopy(type, targetsForType.with(new PointerTarget(base,
                                                                               containerType,
                                                                               properOffset,
                                                                               containerOffset)));
    }

    private void addTargets(final String base,
                            final CType currentType,
                            final CType containerType,
                            final int properOffset,
                            final int containerOffset) {
      final CType cType = CTypeUtils.simplifyType(currentType);
      if (cType instanceof CArrayType) {
        final CArrayType arrayType = (CArrayType) cType;
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
        final String type = cTypeToString(compositeType);
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

    private void addBase(final String name, CType type) {
      type = CTypeUtils.simplifyType(type);
      if (bases.containsKey(name)) {
        return; // The base has already been added
      }
      addTargets(name, type, null, 0, 0);
      bases = bases.putAndCopy(name, type);
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
                                  disjointnessFormula);
    }

    private final PointerTargetSet pointerTargetSet;

    private PersistentSortedMap<String, CType> bases;
    private PersistentSortedMap<CompositeField, Boolean> fields;

    private PersistentSortedMap<String, PersistentList<PointerTarget>> targets;
    private BooleanFormula disjointnessFormula;
  }

  private static final PointerTargetSet emptyPointerTargetSet(final MachineModel machineModel,
                                                              final BooleanFormula truthFormula) {
    final CEvaluatingVisitor evaluatingVisitor = new CEvaluatingVisitor(machineModel);
    return new PointerTargetSet(evaluatingVisitor,
                                new CSizeofVisitor(evaluatingVisitor),
                                PathCopyingPersistentTreeMap.<String, CType>of(),
                                PathCopyingPersistentTreeMap.<CompositeField, Boolean>of(),
                                PathCopyingPersistentTreeMap.<String, PersistentList<PointerTarget>>of(),
                                truthFormula);
  }

  @Override
  public String toString() {
    return joiner.join(bases.entrySet()) + " " + joiner.join(fields.entrySet());
  }

  @Override
  public int hashCode() {
    return (31 + bases.hashCode()) * 31 + fields.hashCode();
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
                           final BooleanFormula disjointnessFormula) {
    this.evaluatingVisitor = evaluatingVisitor;
    this.sizeofVisitor = sizeofVisitor;

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

  /*
   * Use Multiset<String> instead of Map<String, Integer> because it is more
   * efficient. The integer value is stored as the number of instances of any
   * element in the Multiset. So instead of calling map.get(key) we just use
   * Multiset.count(key). This is better because the Multiset internally uses
   * modifiable integers instead of the immutable Integer class.
   */
  private static final Multiset<String> sizes = HashMultiset.create();
  private static final Map<String, Multiset<String>> offsets = new HashMap<>();

  private final PersistentSortedMap<String, CType> bases;
  private final PersistentSortedMap<CompositeField, Boolean> fields;

  private final PersistentSortedMap<String, PersistentList<PointerTarget>> targets;
  private final BooleanFormula disjointnessFormula;

  private static final int DEFAULT_ARRAY_LENGTH = 100;
}

