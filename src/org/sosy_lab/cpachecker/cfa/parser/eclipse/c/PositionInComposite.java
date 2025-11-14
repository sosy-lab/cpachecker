// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.eclipse.cdt.core.dom.ast.c.ICASTDesignatedInitializer;
import org.eclipse.cdt.core.dom.ast.c.ICASTDesignator;
import org.eclipse.cdt.core.dom.ast.c.ICASTFieldDesignator;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * A position within a composite type. The position can be any given member (named or unnamed)
 * within a hierarchical structure of (potentially nested) composite types.
 *
 * <p>Example:
 *
 * <pre>
 *   struct {
 *     union { <--- potential position
 *       int a; <--- potential position
 *       struct { <- potential position
 *         int b; <- potential position
 *       }
 *     };
 *     int c; <----- potential position
 *     struct { <--- potential position
 *       int d; <--- potential position
 *     }
 *   }
 * </pre>
 */
public class PositionInComposite {
  private final CCompositeType rootType;

  /**
   * Path encoding to the current position within the root type. The path is represented as a list
   * of member indices, where each index represents the position within a composite type at that
   * nesting level.
   *
   * <p>Path semantics:
   *
   * <ul>
   *   <li>[i] - positioned at member i of the root type
   *   <li>[i,j] - positioned at member j of member i
   *   <li>[i,j,k] - positioned at member k of member j of member i
   * </ul>
   *
   * <p>Examples:
   *
   * <pre>
   *   struct {
   *     struct { <--- position [0]
   *       int a; <--- position [0,0]
   *       struct { <- position [0,1]
   *         int b; <- position [0,1,0]
   *       }
   *     };
   *     int c; <----- position [1]
   *     struct { <--- position [2]
   *       int d; <--- position [2,0]
   *     }
   *   }
   * </pre>
   */
  private Deque<Integer> path;

  /**
   * Creates a new position at the first direct member of the given type.
   *
   * @param pRootType the root type being initialized (canonical type must be a CCompositeType)
   */
  public PositionInComposite(CType pRootType) {
    if (!(pRootType.getCanonicalType() instanceof CCompositeType compositeType)) {
      throw new IllegalArgumentException(
          "Type must resolve to a composite type, but does not: "
              + pRootType
              + "(resolves to "
              + pRootType.getCanonicalType()
              + ")");
    }
    rootType = compositeType;
    path = new ArrayDeque<>();
    path.addLast(0);
  }

  /** Get the type of the (potentially nested) member at the current position. */
  public CType getCurrentType() {
    return getTypeAt(path);
  }

  private CType getTypeAt(Iterable<Integer> pathToTraverse) {
    CType currentType = rootType;

    for (int memberIndex : pathToTraverse) {
      if (currentType instanceof CCompositeType compositeType) {
        List<CCompositeTypeMemberDeclaration> members = compositeType.getMembers();

        if (memberIndex < 0 || memberIndex >= members.size()) {
          throw new IndexOutOfBoundsException(
              "Position out of bounds: index "
                  + memberIndex
                  + ", but type "
                  + compositeType
                  + " has only "
                  + members.size()
                  + " members");
        }
        currentType = members.get(memberIndex).getType().getCanonicalType();

      } else if (currentType instanceof CArrayType arrayType) {
        currentType = arrayType.getType().getCanonicalType();

      } else {
        throw new CFAGenerationRuntimeException(
            "Cannot traverse into into non-composite, non-array type at index "
                + memberIndex
                + ": "
                + currentType);
      }
    }

    return currentType;
  }

  /**
   * Advance the position to the next element, following the semantics of C11 positional
   * initializers.
   *
   * <p>For structs, the position moves to the next sibling member at the current nesting level. If
   * no more siblings exist, it pops to the structural parent level and advances there.
   *
   * <p>For unions, the position always pops to the structural parent level and advances there.
   *
   * <p>Example:
   *
   * <pre>
   * struct {
   *   int a;
   *   int b;
   *   struct {
   *     int x;
   *     int y;
   *   } n;
   *   union {
   *     int z;
   *     int w;
   *   } u;
   *   int c;
   * }
   * </pre>
   *
   * <ul>
   *   <li>At 'a' advance to 'b'
   *   <li>At 'b' advance to 'n'
   *   <li>At 'n.x' advance to 'n.y'
   *   <li>At 'n.y' advance to 'u'. 'n.y' is the last sub-member, so pop to the parent struct and
   *       advance to 'u')
   *   <li>At 'u.z' advance to 'c'. 'u.z' is the only member initialized in the union, so pop to the
   *       parent struct and advance to 'c')
   * </ul>
   *
   * @see #advanceToScalar()
   */
  public void advanceToNextElement() {
    int currentPosition = path.getLast();
    int nextPositionOnSameLevel = currentPosition + 1;

    Deque<Integer> pathToParent = removeLastPosition(path);
    CType parentType = getTypeAt(pathToParent);

    int numberOfElementsInParents =
        switch (parentType) {
          case CCompositeType compositeType -> {
            if (compositeType.getKind() == ComplexTypeKind.UNION) {
              // For unions the next position is not the next member within the union,
              // but the next sibling of that union.
              yield 1;
            } else {
              yield compositeType.getMembers().size();
            }
          }
          case CArrayType arrayType -> arrayType.getLengthAsInt().orElse(0);
          default -> 0;
        };

    if (nextPositionOnSameLevel < numberOfElementsInParents) {
      path = setLastPosition(path, nextPositionOnSameLevel);
    } else {
      // 'pop out' and advance to the next element (to the sibling of the parent of the old
      // position)
      path.removeLast();
      if (!path.isEmpty()) {
        advanceToNextElement();
      }
    }
  }

  private Deque<Integer> removeLastPosition(Deque<Integer> sequence) {
    Deque<Integer> result = new ArrayDeque<>(sequence);
    result.removeLast();
    return result;
  }

  private Deque<Integer> setLastPosition(Deque<Integer> sequence, int newElement) {
    Deque<Integer> result = removeLastPosition(sequence);
    result.addLast(newElement);
    return result;
  }

  /**
   * Advances to the next scalar, if the current position is at an array or composite type. If the
   * position already is at a scalar, nothing happens.
   *
   * <p>Example:
   *
   * <pre>
   * struct {
   *   int a;
   *   struct {
   *     struct {
   *      int x;
   *     } m;
   *     int y;
   *   } n;
   * }
   * </pre>
   *
   * <ul>
   *   <li>At 'a' stays at 'a'.
   *   <li>At 'n' advances to 'n.m.x'
   * </ul>
   *
   * @see #advanceToNextElement()
   */
  public void advanceToScalar() {
    CType currentType = getCurrentType();
    while (currentType.getCanonicalType() instanceof CCompositeType
        || currentType.getCanonicalType() instanceof CArrayType) {
      // Descend to the first member/element
      path.add(0);
      currentType = getCurrentType();
    }
  }

  /**
   * Jump to the next position after the given designated initializer.
   *
   * @param designatedInit the designated initializer
   * @param parentType the composite type that contains the designator
   */
  public void jumpToPositionAfterDesignator(
      ICASTDesignatedInitializer designatedInit, CType parentType) {

    List<ICASTFieldDesignator> relevantDesignators = getPrefixOfFieldDesignators(designatedInit);
    jumpToDesignator(relevantDesignators, parentType);
    advanceToNextElement();
  }

  private void jumpToDesignator(List<ICASTFieldDesignator> designatorSequence, CType parentType) {
    List<Pair<String, CType>> wayToLastField = new ArrayList<>();
    CType currentType = parentType.getCanonicalType();
    for (ICASTFieldDesignator fieldDesignator : designatorSequence) {
      if (!(currentType instanceof CCompositeType compositeType)) {
        throw new CFAGenerationRuntimeException("No composite type: " + parentType);
      }

      String targetFieldName = fieldDesignator.getName().toString();
      List<Pair<String, CType>> wayToCurrentField =
          getWayToInnerField(compositeType, targetFieldName, new ArrayList<>());
      Preconditions.checkState(!wayToCurrentField.isEmpty());
      wayToLastField.addAll(wayToCurrentField);

      currentType = wayToLastField.getLast().getSecond().getCanonicalType();
    }

    path = getMemberIndicesToField((CCompositeType) parentType, wayToLastField);
  }

  /**
   * Returns the prefix of uninterrupted field designators in a sequence of designators. For
   * example, for {@code .a.b[0].c}, returns [a, b].
   */
  private List<ICASTFieldDesignator> getPrefixOfFieldDesignators(
      ICASTDesignatedInitializer designatedInit) {

    ICASTDesignator[] designators = designatedInit.getDesignators();
    if (designators.length == 0) {
      throw new IllegalArgumentException("Designators must not be empty: " + designatedInit);
    }

    List<ICASTFieldDesignator> result = new ArrayList<>();
    for (ICASTDesignator designator : designators) {
      if (designator instanceof ICASTFieldDesignator fieldDesignator) {
        result.add(fieldDesignator);
      } else {
        break;
      }
    }
    return result;
  }

  /**
   * Returns the sequence of member indices that represent the named way to a field.
   *
   * <p>Example: Given the below struct as composite type and the way '.a.z.t', the method returns
   * '[0, 2, 1]'
   *
   * <pre>
   *   struct {
   *     struct {
   *       int x;
   *       int y;
   *       struct {
   *         int s;
   *         int t;
   *       } z;
   *     } a;
   *   }
   * </pre>
   */
  private @NonNull Deque<Integer> getMemberIndicesToField(
      CCompositeType compositeType, List<Pair<String, CType>> wayToField) {

    Deque<Integer> indices = new ArrayDeque<>();
    CType currentType = compositeType;

    for (Pair<String, CType> nextField : wayToField) {
      String fieldName = nextField.getFirst();
      if (!(currentType instanceof CCompositeType currentCompositeType)) {
        throw new CFAGenerationRuntimeException(
            "Cannot navigate through non-composite type "
                + currentType
                + " to reach field "
                + fieldName);
      }

      List<CCompositeTypeMemberDeclaration> members = currentCompositeType.getMembers();
      int memberIndex = getMemberIndex(members, fieldName, currentCompositeType);

      indices.addLast(memberIndex);
      currentType = nextField.getSecond().getCanonicalType();
    }
    return indices;
  }

  private int getMemberIndex(
      List<CCompositeTypeMemberDeclaration> members,
      String fieldName,
      CCompositeType currentComposite) {

    for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
      CCompositeTypeMemberDeclaration member = members.get(memberIndex);
      if (member.getName().equals(fieldName)) {
        return memberIndex;
      }
    }
    throw new CFAGenerationRuntimeException(
        String.format("Field %s not found in composite type %s", fieldName, currentComposite));
  }

  /**
   * Resolves the path to a field within a composite type, traversing anonymous structs/unions.
   *
   * @param owner the composite type to search in
   * @param fieldName the name of the field to find
   * @param allReferences accumulator list tracking the path to the field
   * @return immutable list of (field name, field type) pairs representing the path, or empty if
   *     field not found
   */
  public static List<Pair<String, CType>> getWayToInnerField(
      CCompositeType owner, String fieldName, List<Pair<String, CType>> allReferences) {
    for (CCompositeTypeMemberDeclaration member : owner.getMembers()) {
      if (member.getName().equals(fieldName)) {
        allReferences.add(Pair.of(member.getName(), member.getType()));
        return ImmutableList.copyOf(allReferences);
      }
    }

    // no field found in current struct, so proceed to the structs/unions which are
    // fields inside the current struct
    for (CCompositeTypeMemberDeclaration member : owner.getMembers()) {
      CType memberType = member.getType().getCanonicalType();
      if (memberType instanceof CCompositeType cCompositeType
          && member.getName().contains("__anon_type_member_")) {
        List<Pair<String, CType>> tmp = new ArrayList<>(allReferences);
        tmp.add(Pair.of(member.getName(), member.getType()));
        tmp = getWayToInnerField(cCompositeType, fieldName, tmp);
        if (!tmp.isEmpty()) {
          return ImmutableList.copyOf(tmp);
        }
      }
    }

    return ImmutableList.of();
  }
}
