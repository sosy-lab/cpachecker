// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypes.isAggregateType;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypes.withoutConst;
import static org.sosy_lab.cpachecker.cfa.types.c.CTypes.withoutVolatile;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** Utility class for initializer-related tasks. */
public final class CInitializers {

  private CInitializers() {}

  /**
   * Take a variable declaration and create a list of assignment statements that assign the value(s)
   * of the initializer to the declared variable, including cases with complex initializers for
   * structs and arrays.
   *
   * <p>Note that there is currently one unhandled case: In C, for structs and arrays where a
   * brace-delimited initializer list is present, all fields/elements that are not explicitly
   * initialized are initialized with their default value. However, this method does not return any
   * assignments for these default values.
   *
   * <p>Example: <code>
   * struct {
   *   int i;
   *   int a[3];
   * } s = { 1, 2, 3 };
   * </code> will be converted into <code>
   * s.i = 1;
   * s.a[0] = 2;
   * s.a[1] = 3;
   * </code> (The assignment s.a[2] = 0 is missing as explained above.)
   *
   * @param decl The variable declaration.
   * @param edge The current CFA edge.
   * @return A (possibly empty) list of assignment statements.
   */
  public static List<CExpressionAssignmentStatement> convertToAssignments(
      CVariableDeclaration decl, CFAEdge edge) throws UnrecognizedCodeException {

    CInitializer init = decl.getInitializer();
    if (init == null) {
      return ImmutableList.of();
    }

    CLeftHandSide lhs = new CIdExpression(decl.getFileLocation(), decl);

    if (init instanceof CInitializerExpression) {
      CExpression initExp = ((CInitializerExpression) init).getExpression();
      // Create a regular assignment
      CExpressionAssignmentStatement assignment =
          new CExpressionAssignmentStatement(decl.getFileLocation(), lhs, initExp);
      return ImmutableList.of(assignment);

    } else if (init instanceof CInitializerList) {

      return handleInitializerList(lhs, (CInitializerList) init, decl.getFileLocation(), edge);

    } else {
      throw new UnrecognizedCodeException("Unknown initializer type", edge, init);
    }
  }

  /**
   * Handle a brace-delimited initializer list as defined in § 6.7.9 of the C standard.
   *
   * @param currentObject The "current object".
   * @param initializerList The initializer list for the "current object".
   */
  private static List<CExpressionAssignmentStatement> handleInitializerList(
      final CExpression currentObject,
      final CInitializerList initializerList,
      final FileLocation loc,
      final CFAEdge edge)
      throws UnrecognizedCodeException {

    // The term "current object" is defined in the C standard, §6.7.9 (17)
    // The initializer list is the initializer for the "current object"
    // and its subobjects.
    // One call of this method is responsible for initializing the "current object"
    // and its subobjects completely.
    // It calls itself recursively if it encounters nested brace-enclosed
    // initializer lists, because inside such lists, the "current object" is different.

    // We keep two stacks here.
    // - currentSubobjects is the stack of expressions to access
    //   the current subobject and its outer object up to the "current object"
    //   Example: [s.t.f, s.t, s]
    // - nextSubobjects is the stack of iterators giving the next subobjects
    //   to handle after the current subobject was handled
    //   (i.e., the neighboring fields of the current subobjects)
    // Both these stacks are modified by other methods.

    Deque<CExpression> currentSubobjects = new ArrayDeque<>(2);
    Deque<Iterator<CExpression>> nextSubobjects = new ArrayDeque<>(2);

    { // For starting, we go to the first very subobject of the "current object".
      // We cannot go to the first subobject at the deepest nesting level
      // as findFirstSubobjectWithType does in case the first initializer value
      // is a nested brace-delimited initializer list.
      currentSubobjects.push(currentObject);
      CType currentType = currentObject.getExpressionType().getCanonicalType();
      boolean successful;

      if (currentType instanceof CCompositeType
          && ((CCompositeType) currentType).getKind() != ComplexTypeKind.ENUM) {
        successful =
            handleInitializerForCompositeType(
                currentObject,
                Optional.empty(),
                (CCompositeType) currentType,
                currentSubobjects,
                nextSubobjects,
                loc,
                edge,
                null);

      } else if (currentType instanceof CArrayType) {
        successful =
            handleInitializerForArray(
                currentObject,
                0L,
                (CArrayType) currentType,
                currentSubobjects,
                nextSubobjects,
                loc,
                edge,
                null);
      } else if (currentType instanceof CElaboratedType) {
        throw new UnrecognizedCodeException(
            "Unexpected initializer for " + currentType + " that is not fully defined",
            edge,
            initializerList);
      } else {
        throw new UnrecognizedCodeException(
            "Unexpected initializer list for " + currentObject + " with type " + currentType,
            edge,
            initializerList);
      }

      if (!successful) {
        // struct or array was empty, no initializing needed
        if (!initializerList.getInitializers().isEmpty()) {
          throw new UnrecognizedCodeException(
              "Too many values in initializer list", edge, initializerList);
        }
        return ImmutableList.of();
      }
    }

    ImmutableList.Builder<CExpressionAssignmentStatement> result =
        ImmutableList.builderWithExpectedSize(initializerList.getInitializers().size());
    for (CInitializer init : initializerList.getInitializers()) {

      if (init instanceof CDesignatedInitializer) {
        // first, this resets everything except the "current object"
        findDesignatedSubobject(
            ((CDesignatedInitializer) init).getDesignators(),
            currentObject,
            currentSubobjects,
            nextSubobjects,
            loc,
            edge);

        // now analyze the real initializer part
        init = ((CDesignatedInitializer) init).getRightHandSide();
        if (init instanceof CDesignatedInitializer) {
          throw new UnrecognizedCodeException(
              "Too complex struct initializer", edge, initializerList);
        }
      }

      if (currentSubobjects.isEmpty()) {
        throw new UnrecognizedCodeException(
            "Too many values in initializer list", edge, initializerList);
      }

      if (init instanceof CInitializerList) {
        // nested bracketed initializer, handle recursively
        // (the content of the brackets has the current subobject as the "current object"

        final CExpression currentSubobject = currentSubobjects.pop();
        result.addAll(handleInitializerList(currentSubobject, (CInitializerList) init, loc, edge));

      } else if (init instanceof CInitializerExpression) {
        // simple expression,
        CExpression initExp = ((CInitializerExpression) init).getExpression();
        CType initType = initExp.getExpressionType().getCanonicalType();

        // This applies to the first field/element of the current subobject,
        // which might be on a deeper nesting level than we currently are,
        // so we build the stacks if necessary.
        findFirstSubobjectWithType(initType, currentSubobjects, nextSubobjects, loc, edge);

        assert currentSubobjects.peek() instanceof CLeftHandSide
            : "Object hast to be a LeftHandSide";
        final CLeftHandSide currentSubobject = (CLeftHandSide) currentSubobjects.pop();

        // Do a regular assignment
        CExpressionAssignmentStatement assignment =
            new CExpressionAssignmentStatement(loc, currentSubobject, initExp);

        result.add(assignment);

      } else {
        throw new UnrecognizedCodeException("Unknown initializer type", edge, init);
      }

      // Prepare the stacks for the next iteration.
      // Cleanup the iterators stack.
      while (!nextSubobjects.isEmpty() && !nextSubobjects.peek().hasNext()) {
        nextSubobjects.pop();
        currentSubobjects.pop();
      }

      // Fetch the next subobject to handle.
      if (!nextSubobjects.isEmpty()) {
        currentSubobjects.push(nextSubobjects.peek().next());
      }
    }

    return result.build();
  }

  /**
   * Given a designator list of an initializer for an aggregate type, this method builds the two
   * stacks for the subobjects and the iterators such that the designated field/element is the next
   * that will be accessed. Prior to that, both stacks are reset.
   *
   * @param designators A list of designators (e.g. ".f[2][1-4].t")
   * @param currentObject the "current object" with which this whole chain of initializers is
   *     associated
   * @param currentSubobjects as in {@link #handleInitializerList(CExpression, CInitializerList,
   *     FileLocation, CFAEdge)}
   * @param nextSubobjects as in {@link #handleInitializerList(CExpression, CInitializerList,
   *     FileLocation, CFAEdge)}
   */
  private static void findDesignatedSubobject(
      final List<CDesignator> designators,
      final CExpression currentObject,
      final Deque<CExpression> currentSubobjects,
      final Deque<Iterator<CExpression>> nextSubobjects,
      final FileLocation loc,
      final CFAEdge edge)
      throws UnrecognizedCodeException {

    currentSubobjects.clear();
    nextSubobjects.clear();
    currentSubobjects.push(currentObject);

    // We just iterate through the designators and push onto both stacks.
    for (CDesignator designator : designators) {
      final CExpression currentSubobject = currentSubobjects.peek();
      final CType currentType = currentSubobject.getExpressionType().getCanonicalType();
      boolean successful;

      if (designator instanceof CFieldDesignator) {
        String fieldName = ((CFieldDesignator) designator).getFieldName();
        if (!(currentType instanceof CCompositeType)
            || ((CCompositeType) currentType).getKind() == ComplexTypeKind.ENUM) {
          throw new UnrecognizedCodeException(
              "Designated field initializer for non-struct type " + currentType, edge, designator);
        }

        successful =
            handleInitializerForCompositeType(
                currentSubobject,
                Optional.of(fieldName),
                (CCompositeType) currentType,
                currentSubobjects,
                nextSubobjects,
                loc,
                edge,
                designator);

      } else if (designator instanceof CArrayDesignator) {
        if (!(currentType instanceof CArrayType)) {
          throw new UnrecognizedCodeException(
              "Designated array initializer for non-array type " + currentType, edge, designator);
        }

        CArrayType arrayType = (CArrayType) currentType;
        CExpression indexExp = ((CArrayDesignator) designator).getSubscriptExpression();

        if (!(indexExp instanceof CIntegerLiteralExpression)) {
          throw new UnrecognizedCodeException(
              "Cannot evaluate expression as array designator", edge, designator);
        }

        BigInteger index = ((CIntegerLiteralExpression) indexExp).getValue();
        if (!BigInteger.valueOf(index.longValue()).equals(index)) {
          throw new UnrecognizedCodeException(
              "Array designator is too large to initialize explicitly", edge, designator);
        }

        successful =
            handleInitializerForArray(
                currentSubobject,
                index.longValue(),
                arrayType,
                currentSubobjects,
                nextSubobjects,
                loc,
                edge,
                designator);

      } else if (designator instanceof CArrayRangeDesignator) {
        if (!(currentType instanceof CArrayType)) {
          throw new UnrecognizedCodeException(
              "Designated array initializer for non-array type " + currentType, edge, designator);
        }

        CArrayType arrayType = (CArrayType) currentType;
        CExpression floorExp = ((CArrayRangeDesignator) designator).getFloorExpression();
        CExpression ceilExp = ((CArrayRangeDesignator) designator).getCeilExpression();

        if (!(floorExp instanceof CIntegerLiteralExpression)
            || !(ceilExp instanceof CIntegerLiteralExpression)) {
          throw new UnrecognizedCodeException(
              "Cannot evaluate expression as array range designator", edge, designator);
        }

        BigInteger indexBottom = ((CIntegerLiteralExpression) floorExp).getValue();
        BigInteger indexTop = ((CIntegerLiteralExpression) ceilExp).getValue();
        if (!BigInteger.valueOf(indexBottom.longValue()).equals(indexBottom)
            || !BigInteger.valueOf(indexTop.longValue()).equals(indexTop)) {
          throw new UnrecognizedCodeException(
              "Array range designator is too large to initialize explicitly", edge, designator);
        }

        successful = true;
        for (long index = indexBottom.longValue();
            index < indexTop.longValue() && successful;
            index++) {
          successful =
              handleInitializerForArray(
                  currentSubobject,
                  index,
                  arrayType,
                  currentSubobjects,
                  nextSubobjects,
                  loc,
                  edge,
                  designator);
        }

      } else {
        throw new UnrecognizedCodeException(
            "Unrecognized initializer designator", edge, designator);
      }

      if (!successful) {
        throw new UnrecognizedCodeException(
            "Empty struct or array is not supported as field", edge, currentSubobject);
      }
    }
  }

  // safe because Iterator is covariant
  @SuppressWarnings("unchecked")
  private static <T> Iterator<T> safeCast(Iterator<? extends T> it) {
    return (Iterator<T>) it;
  }

  /**
   * Find the first subobject inside the current subobject that may be initialized with a value of a
   * given type. Usually this method just enters nested structs and arrays until it finds the first
   * field/element that is not of an aggegrate type. However, if the given type is for example a
   * struct, and it encounters a field/element of this type, it does not enter this subobject.
   *
   * <p>This method only pushes objects on the two stacks until their position is correct.
   *
   * @param targetType The type to search.
   * @param currentSubobjects as in {@link #handleInitializerList(CExpression, CInitializerList,
   *     FileLocation, CFAEdge)}
   * @param nextSubobjects as in {@link #handleInitializerList(CExpression, CInitializerList,
   *     FileLocation, CFAEdge)}
   * @param loc the location of the currently handled object
   * @param edge the edge that is handled here
   */
  private static void findFirstSubobjectWithType(
      final CType targetType,
      final Deque<CExpression> currentSubobjects,
      final Deque<Iterator<CExpression>> nextSubobjects,
      final FileLocation loc,
      CFAEdge edge)
      throws UnrecognizedCodeException {

    while (true) {
      final CExpression currentSubobject = currentSubobjects.peek();
      final CType currentType = currentSubobject.getExpressionType().getCanonicalType();

      // Ignore modifiers const and volatile for equality checks.
      CType currentTypeWithoutModifier = withoutConst(withoutVolatile(currentType));
      CType targetTypeWithoutModifier = withoutConst(withoutVolatile(targetType));
      if (targetTypeWithoutModifier.equals(currentTypeWithoutModifier)) {
        break;
      }

      // String literals may be used to initialize char arrays.
      // They have a type of (const char)*.
      if (targetType.equals(CPointerType.POINTER_TO_CONST_CHAR)
          && currentType instanceof CArrayType) {
        CType currentElementType = ((CArrayType) currentType).getType();
        if (currentElementType instanceof CSimpleType
            && ((CSimpleType) currentElementType).getType() == CBasicType.CHAR) {
          break;
        }
      }
      boolean successful;

      if (currentType instanceof CCompositeType
          && ((CCompositeType) currentType).getKind() != ComplexTypeKind.ENUM) {
        successful =
            handleInitializerForCompositeType(
                currentSubobject,
                Optional.empty(),
                (CCompositeType) currentType,
                currentSubobjects,
                nextSubobjects,
                loc,
                edge,
                null);

      } else if (currentType instanceof CArrayType) {
        successful =
            handleInitializerForArray(
                currentSubobject,
                0L,
                (CArrayType) currentType,
                currentSubobjects,
                nextSubobjects,
                loc,
                edge,
                null);

      } else {
        // any other type is not an aggregate type
        break;
      }

      if (!successful) {
        throw new UnrecognizedCodeException(
            "Empty struct or array is not supported as field", edge, currentSubobject);
      }
    }
  }

  /**
   * Handle the case when the current subobject that will be initialized next is a composite type
   * (struct or union). This method only prepares the two stacks by pushing one object on both of
   * them (for the next field to be initialized, and the iterator for the remainder of the fields).
   * If a field name is given, this method ignores (i.e., jumps over) all fields that appear
   * _before_ that given field.
   *
   * @param currentSubobject The struct/union to be initialized
   * @param startingFieldName The optional field name to look for
   * @param structType The type of currentSubobject
   * @param currentSubobjects as in {@link #handleInitializerList(CExpression, CInitializerList,
   *     FileLocation, CFAEdge)}
   * @param nextSubobjects as in {@link #handleInitializerList(CExpression, CInitializerList,
   *     FileLocation, CFAEdge)}
   */
  private static boolean handleInitializerForCompositeType(
      final CExpression currentSubobject,
      final Optional<String> startingFieldName,
      final CCompositeType structType,
      final Deque<CExpression> currentSubobjects,
      final Deque<Iterator<CExpression>> nextSubobjects,
      final FileLocation loc,
      final CFAEdge edge,
      final CDesignator designator)
      throws UnrecognizedCodeException {

    Iterator<CFieldReference> fields =
        from(structType.getMembers())
            .filter(
                field ->
                    !(field.getName().contains("__anon_type_member")
                        && (!isAggregateType(field.getType())
                            && (field.getType() instanceof CElaboratedType)
                            && !((CElaboratedType) field.getType())
                                .getKind()
                                .equals(ComplexTypeKind.UNION))))
            .transform(
                field ->
                    new CFieldReference(
                        loc, field.getType(), field.getName(), currentSubobject, false))
            .iterator();

    if (!fields.hasNext()) {
      // empty struct
      return false;
    }

    CFieldReference designatedField = null;

    if (startingFieldName.isPresent()) {
      // find the designated field and advance the iterator up to this point
      while (fields.hasNext()) {
        CFieldReference f = fields.next();
        if (f.getFieldName().equals(startingFieldName.orElseThrow())) {
          designatedField = f;
          break;
        }
      }
      if (designatedField == null) {
        throw new UnrecognizedCodeException(
            "Initializer for field "
                + startingFieldName.orElseThrow()
                + " but no field with this name exists in "
                + structType,
            edge,
            designator);
      }

    } else {
      // first field
      designatedField = fields.next();
    }

    currentSubobjects.push(designatedField);

    switch (structType.getKind()) {
      case STRUCT:
        nextSubobjects.push(CInitializers.safeCast(fields));
        break;
      case UNION:
        // unions only have their first field initialized, ignore the rest
        nextSubobjects.push(Collections.emptyIterator());
        break;
      default:
        throw new AssertionError();
    }

    return true;
  }

  /**
   * Handle the case when the current subobject that will be initialized next is an array. This
   * method only prepares the two stacks by pushing one object on both of them (for the next element
   * to be initialized, and the iterator for the remainder of the elements).
   *
   * @param currentSubobject The struct/union to be initialized
   * @param startIndex The index of the first element to be initialized
   * @param arrayType The type of currentSubobject
   * @param currentSubobjects as in {@link #handleInitializerList(CExpression, CInitializerList,
   *     FileLocation, CFAEdge)}
   * @param nextSubobjects as in {@link #handleInitializerList(CExpression, CInitializerList,
   *     FileLocation, CFAEdge)}
   */
  private static boolean handleInitializerForArray(
      final CExpression currentSubobject,
      final long startIndex,
      final CArrayType arrayType,
      final Deque<CExpression> currentSubobjects,
      final Deque<Iterator<CExpression>> nextSubobjects,
      final FileLocation loc,
      final CFAEdge edge,
      final CDesignator designator)
      throws UnrecognizedCodeException {

    Range<Long> arrayIndices;
    if (arrayType.getLength() instanceof CIntegerLiteralExpression) {
      // fixed-size array
      BigInteger size = ((CIntegerLiteralExpression) arrayType.getLength()).getValue();
      if (!BigInteger.valueOf(size.longValue()).equals(size)) {
        throw new UnrecognizedCodeException(
            "Size of type " + arrayType + " is too large to initialize explicitly",
            edge,
            designator);
      }
      // TODO use DiscreteDomain.bigintegers() when it's available.

      arrayIndices = Range.closedOpen(startIndex, size.longValue());

    } else if (arrayType.getLength() == null) {
      // variable-length array, this array goes until there are no more initializer values

      arrayIndices = Range.atLeast(startIndex);

    } else {
      throw new UnrecognizedCodeException(
          "Cannot initialize arrays with variable modified type like " + arrayType,
          edge,
          designator);
    }

    if (arrayIndices.isEmpty()) {
      return false;
    }

    final CType elementType = arrayType.getType();

    Set<Long> indexSet = ContiguousSet.create(arrayIndices, DiscreteDomain.longs());
    Iterator<CExpression> elements =
        from(indexSet)
            .<CExpression>transform(
                pInput -> {
                  CExpression index =
                      new CIntegerLiteralExpression(
                          loc, CNumericTypes.INT, BigInteger.valueOf(pInput));

                  return new CArraySubscriptExpression(loc, elementType, currentSubobject, index);
                })
            .iterator();

    CExpression firstElement = elements.next();

    currentSubobjects.push(firstElement);
    nextSubobjects.push(elements);

    return true;
  }
}
