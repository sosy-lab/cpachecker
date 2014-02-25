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
package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.collect.FluentIterable.from;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.StatementToFormulaVisitor;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Range;

/**
 * Utility class for initializer-related tasks.
 */
public final class CInitializers {

  private CInitializers() {}

  /**
   * Take a variable declaration and create a list of assignment statements
   * that assign the value(s) of the initializer to the declared variable,
   * including cases with complex initializers for structs and arrays.
   *
   * Note that there is currently one unhandled case:
   * In C, for structs and arrays where a brace-delimited initializer list is present,
   * all fields/elements that are not explicitly initialized
   * are initialized with their default value.
   * However, this method does not return any assignments for these default values.
   *
   * Example:
   * <code>
   * struct {
   *   int i;
   *   int a[3];
   * } s = { 1, 2, 3 };
   * </code>
   * will be converted into
   * <code>
   * s.i = 1;
   * s.a[0] = 2;
   * s.a[1] = 3;
   * </code>
   * (The assignment s.a[2] = 0 is missing as explained above.)
   *
   * @param decl The variable declaration.
   * @param edge The current CFA edge.
   * @return A (possibly empty) list of assignment statements.
   * @throws UnrecognizedCCodeException
   */
  public static List<CExpressionAssignmentStatement> convertToAssignments(
      CVariableDeclaration decl, CFAEdge edge) throws UnrecognizedCCodeException {

    CInitializer init = decl.getInitializer();
    if (init == null) {
      return ImmutableList.of();
    }

    CLeftHandSide lhs = new CIdExpression(decl.getFileLocation(), decl);

    if (init instanceof CInitializerExpression) {
      CExpression initExp = ((CInitializerExpression)init).getExpression();
      // Create a regular assignment
      CExpressionAssignmentStatement assignment =
          new CExpressionAssignmentStatement(decl.getFileLocation(), lhs, initExp);
      return ImmutableList.of(assignment);

    } else if (init instanceof CInitializerList) {
      return handleInitializerList(lhs, (CInitializerList)init,
          decl.getFileLocation(), edge);

    } else {
      throw new UnrecognizedCCodeException("Unknown initializer type", edge, init);
    }
  }

  /**
   * Handle a brace-delimited initializer list as defined in § 6.7.9 of the C standard.
   * @param currentObject The "current object".
   * @param initializerList The initializer list for the "current object".
   */
  private static List<CExpressionAssignmentStatement> handleInitializerList(
      final CExpression currentObject, final CInitializerList initializerList,
      final FileLocation loc, final CFAEdge edge) throws UnrecognizedCCodeException {

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
      // We cannot go to the first suboject at the deepest nesting level
      // as findFirstSubobjectWithType does in case the first initializer value
      // is a nested brace-delimited initializer list.
      currentSubobjects.push(currentObject);
      CType currentType = currentObject.getExpressionType().getCanonicalType();
      boolean successful;

      if (currentType instanceof CCompositeType && ((CCompositeType) currentType).getKind() != ComplexTypeKind.ENUM) {
        successful = handleInitializerForCompositeType(currentObject, Optional.<String>absent(),
            (CCompositeType)currentType,
            currentSubobjects, nextSubobjects, loc, edge, null);

      } else if (currentType instanceof CArrayType) {
        successful = handleInitializerForArray(currentObject, 0L, (CArrayType)currentType,
            currentSubobjects, nextSubobjects, loc, edge, null);
      } else {
        throw new UnrecognizedCCodeException("Unexpected initializer list for type " + currentType, edge, initializerList);
      }

      if (!successful) {
        // struct or array was empty, no initializing needed
        if (!initializerList.getInitializers().isEmpty()) {
          throw new UnrecognizedCCodeException("Too many values in initializer list", edge, initializerList);
        }
        return ImmutableList.of();
      }
    }

    List<CExpressionAssignmentStatement> result = new ArrayList<>(initializerList.getInitializers().size());
    for (CInitializer init : initializerList.getInitializers()) {

      if (init instanceof CDesignatedInitializer) {
        // first, this resets everything except the "current object"
        findDesignatedSubobject(((CDesignatedInitializer)init).getDesignators(),
            currentObject, currentSubobjects, nextSubobjects, loc, edge);

        // now analyze the real initializer part
        init = ((CDesignatedInitializer)init).getRightHandSide();
        if (init instanceof CDesignatedInitializer) {
          throw new UnrecognizedCCodeException("Too complex struct initializer", edge, initializerList);
        }
      }

      if (currentSubobjects.isEmpty()) {
        throw new UnrecognizedCCodeException("Too many values in initializer list", edge, initializerList);
      }

      if (init instanceof CInitializerList) {
        // nested bracketed initializer, handle recursively
        // (the content of the brackets has the current subobject as the "current object"

        final CExpression currentSubobject = currentSubobjects.pop();
        result.addAll(handleInitializerList(currentSubobject, (CInitializerList)init,
                                  loc, edge));

      } else if (init instanceof CInitializerExpression) {
        // simple expression,
        CExpression initExp = ((CInitializerExpression)init).getExpression();
        CType initType = initExp.getExpressionType().getCanonicalType();

        // This applies to the first field/element of the current subobject,
        // which might be on a deeper nesting level than we currently are,
        // so we build the stacks if necessary.
        findFirstSubobjectWithType(initType, currentSubobjects, nextSubobjects, loc, edge);

        assert currentSubobjects.peek() instanceof CLeftHandSide : "Object hast to be a LeftHandSide";
        final CLeftHandSide currentSubobject = (CLeftHandSide) currentSubobjects.pop();

        // Do a regular assignment
        CExpressionAssignmentStatement assignment =
            new CExpressionAssignmentStatement(loc, currentSubobject, initExp);

        result.add(assignment);

      } else {
        throw new UnrecognizedCCodeException("Unknown initializer type", edge, init);
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

    return result;
  }


  /**
   * Given a designator list of an initializer for an aggregate type,
   * this method builds the two stacks for the subobjects and the iterators
   * such that the designated field/element is the next that will be accessed.
   * Prior to that, both stacks are reset.
   * @param designators A list of designators (e.g. ".f[2][1-4].t")
   * @param currentObject the "current object" with which this whole chain of initializers is associated
   * @param currentSubobjects as in {@link #handleInitializerList(CExpression, CInitializerList, FileLocation, CFAEdge, StatementToFormulaVisitor)}
   * @param nextSubobjects as in {@link #handleInitializerList(CExpression, CInitializerList, FileLocation, CFAEdge, StatementToFormulaVisitor)}
   */
  private static void findDesignatedSubobject(final List<CDesignator> designators,
      final CExpression currentObject,
      final Deque<CExpression> currentSubobjects, final Deque<Iterator<CExpression>> nextSubobjects,
      final FileLocation loc, final CFAEdge edge) throws UnrecognizedCCodeException {

    currentSubobjects.clear();
    nextSubobjects.clear();
    currentSubobjects.push(currentObject);

    // We just iterate through the designators and push onto both stacks.
    for (CDesignator designator : designators) {
      final CExpression currentSubobject = currentSubobjects.peek();
      final CType currentType = currentSubobject.getExpressionType().getCanonicalType();
      boolean successful;

      if (designator instanceof CFieldDesignator) {
        String fieldName = ((CFieldDesignator)designator).getFieldName();
        if (!(currentType instanceof CCompositeType)
            || ((CCompositeType)currentType).getKind() == ComplexTypeKind.ENUM) {
          throw new UnrecognizedCCodeException("Designated field initializer for non-struct type", edge, designator);
        }

        successful = handleInitializerForCompositeType(currentSubobject, Optional.of(fieldName),
            (CCompositeType)currentType,
            currentSubobjects, nextSubobjects, loc, edge, designator);

      } else if (designator instanceof CArrayDesignator) {
        if (!(currentType instanceof CArrayType)) {
          throw new UnrecognizedCCodeException("Designated array initializer for non-array type", edge, designator);
        }

        CArrayType arrayType = (CArrayType)currentType;
        CExpression indexExp = ((CArrayDesignator)designator).getSubscriptExpression();

        if (!(indexExp instanceof CIntegerLiteralExpression)) {
          throw new UnrecognizedCCodeException("Cannot evaluate expression as array designator", edge, designator);
        }

        BigInteger index = ((CIntegerLiteralExpression)indexExp).getValue();
        if (!BigInteger.valueOf(index.longValue()).equals(index)) {
          throw new UnrecognizedCCodeException("Array designator is too large to initialize explicitly", edge, designator);
        }

        successful = handleInitializerForArray(currentSubobject, index.longValue(), arrayType,
            currentSubobjects, nextSubobjects, loc, edge, designator);

      } else {
        throw new UnrecognizedCCodeException("Unrecognized initializer designator", edge, designator);
      }

      if (!successful) {
        throw new UnrecognizedCCodeException("Empty struct or array is not supported as field", edge, currentSubobject);
      }
    }
  }

  // safe because Iterator is covariant
  @SuppressWarnings("unchecked")
  private static <T> Iterator<T> safeCast(Iterator<? extends T> it) {
    return (Iterator<T>)it;
  }

  /**
   * Find the first subobject inside the current subobject that may be
   * initialized with a value of a given type.
   * Usually this method just enters nested structs and arrays until
   * it finds the first field/element that is not of an aggegrate type.
   * However, if the given type is for example a struct,
   * and it encounters a field/element of this type, it does not enter
   * this subobject.
   *
   * This method only pushes objects on the two stacks until their position is correct.
   *
   * @param targetType The type to search.
   * @param currentSubobjects as in {@link #handleInitializerList(CExpression, CInitializerList, FileLocation, CFAEdge, StatementToFormulaVisitor)}
   * @param nextSubobjects as in {@link #handleInitializerList(CExpression, CInitializerList, FileLocation, CFAEdge, StatementToFormulaVisitor)}
   * @param loc
   * @param edge
   * @throws UnrecognizedCCodeException
   */
  private static void findFirstSubobjectWithType(final CType targetType,
      final Deque<CExpression> currentSubobjects, final Deque<Iterator<CExpression>> nextSubobjects,
      final FileLocation loc, CFAEdge edge) throws UnrecognizedCCodeException {

    while (true) {
      final CExpression currentSubobject = currentSubobjects.peek();
      final CType currentType = currentSubobject.getExpressionType().getCanonicalType();

      if (targetType.equals(currentType)) {
        break;
      }
      boolean successful;

      if (currentType instanceof CCompositeType && ((CCompositeType) currentType).getKind() != ComplexTypeKind.ENUM) {
        successful = handleInitializerForCompositeType(currentSubobject, Optional.<String>absent(),
            (CCompositeType)currentType,
            currentSubobjects, nextSubobjects, loc, edge, null);

      } else if (currentType instanceof CArrayType) {
        successful = handleInitializerForArray(currentSubobject, 0L, (CArrayType)currentType,
            currentSubobjects, nextSubobjects, loc, edge, null);

      } else {
        // any other type is not an aggregate type
        break;
      }

      if (!successful) {
        throw new UnrecognizedCCodeException("Empty struct or array is not supported as field", edge, currentSubobject);
      }
    }
  }

  /**
   * Handle the case when the current subobject that will be initialized next
   * is a composite type (struct or union).
   * This method only prepares the two stacks by pushing one object on both
   * of them (for the next field to be initialized, and the iterator for the
   * remainder of the fields).
   * If a field name is given, this method ignores (i.e., jumps over)
   * all fields that appear _before_ that given field.
   * @param currentSubobject The struct/union to be initialized
   * @param startingFieldName The optional field name to look for
   * @param structType The type of currentSubobject
   * @param currentSubobjects as in {@link #handleInitializerList(CExpression, CInitializerList, FileLocation, CFAEdge, StatementToFormulaVisitor)}
   * @param nextSubobjects as in {@link #handleInitializerList(CExpression, CInitializerList, FileLocation, CFAEdge, StatementToFormulaVisitor)}
   */
  private static boolean handleInitializerForCompositeType(final CExpression currentSubobject,
      final Optional<String> startingFieldName, final CCompositeType structType,
      final Deque<CExpression> currentSubobjects, final Deque<Iterator<CExpression>> nextSubobjects,
      final FileLocation loc, final CFAEdge edge, final CDesignator designator)
          throws UnrecognizedCCodeException {

    Iterator<CFieldReference> fields = from(structType.getMembers()).transform(
        new Function<CCompositeTypeMemberDeclaration, CFieldReference>() {
          @Override
          public CFieldReference apply(CCompositeTypeMemberDeclaration field) {
            return new CFieldReference(loc, field.getType(),
                field.getName(), currentSubobject, false);
          }
        }).iterator();

    if (!fields.hasNext()) {
      // empty struct
      return false;
    }

    CFieldReference designatedField = null;

    if (startingFieldName.isPresent()) {
      // find the designated field and advance the iterator up to this point
      while (fields.hasNext()) {
        CFieldReference f = fields.next();
        if (f.getFieldName().equals(startingFieldName.get())) {
          designatedField = f;
          break;
        }
      }
      if (designatedField == null) {
        throw new UnrecognizedCCodeException("Initializer for field " + startingFieldName.get()
            + " but no field with this name exists in " + structType, edge, designator);
      }

    } else {
      // first field
      designatedField = fields.next();
    }

    currentSubobjects.push(designatedField);

    switch (structType.getKind()) {
    case STRUCT:
      nextSubobjects.push(CInitializers.<CExpression>safeCast(fields));
      break;
    case UNION:
      // unions only have their first field initialized, ignore the rest
      nextSubobjects.push(Iterators.<CExpression>emptyIterator());
      break;
    default:
      throw new AssertionError();
    }

    return true;
  }

  /**
   * Handle the case when the current subobject that will be initialized next
   * is an array.
   * This method only prepares the two stacks by pushing one object on both
   * of them (for the next element to be initialized, and the iterator for the
   * remainder of the elements).
   * @param currentSubobject The struct/union to be initialized
   * @param startIndex The index of the first element to be initialized
   * @param arrayType The type of currentSubobject
   * @param currentSubobjects as in {@link #handleInitializerList(CExpression, CInitializerList, FileLocation, CFAEdge, StatementToFormulaVisitor)}
   * @param nextSubobjects as in {@link #handleInitializerList(CExpression, CInitializerList, FileLocation, CFAEdge, StatementToFormulaVisitor)}
   */
  private static boolean handleInitializerForArray(final CExpression currentSubobject,
      final long startIndex, final CArrayType arrayType,
      final Deque<CExpression> currentSubobjects, final Deque<Iterator<CExpression>> nextSubobjects,
      final FileLocation loc, final CFAEdge edge, final CDesignator designator)
          throws UnrecognizedCCodeException {

    Range<Long> arrayIndices;
    if (arrayType.getLength() instanceof CIntegerLiteralExpression) {
      // fixed-size array
      BigInteger size = ((CIntegerLiteralExpression)arrayType.getLength()).getValue();
      if (!BigInteger.valueOf(size.longValue()).equals(size)) {
        throw new UnrecognizedCCodeException("Size of type " + arrayType + " is too large to initialize explicitly", edge, designator);
      }
      // TODO use DiscreteDomain.bigintegers() when it's available.

      arrayIndices = Range.closedOpen(startIndex, size.longValue());

    } else if (arrayType.getLength() == null) {
      // variable-length array, this array goes until there are no more initializer values

      arrayIndices = Range.atLeast(startIndex);

    } else {
      throw new UnrecognizedCCodeException("Cannot initialize arrays with variable modified type like " + arrayType, edge, designator);
    }

    if (arrayIndices.isEmpty()) {
      return false;
    }

    final CType elementType = arrayType.getType();

    Set<Long> indexSet = ContiguousSet.create(arrayIndices, DiscreteDomain.longs());
    Iterator<CExpression> elements = from(indexSet).transform(
        new Function<Long, CExpression>() {
          @Override
          public CExpression apply(Long pInput) {
            CExpression index = new CIntegerLiteralExpression(loc,
                CNumericTypes.INT, BigInteger.valueOf(pInput.longValue()));

            return new CArraySubscriptExpression(
                loc, elementType, currentSubobject, index);
          }
        }).iterator();

    CExpression firstElement = elements.next();

    currentSubobjects.push(firstElement);
    nextSubobjects.push(elements);

    return true;
  }
}