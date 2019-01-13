/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import static java.util.stream.Collectors.toMap;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.OBJECT_FIELDS_TYPE;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralField;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/**
 * Management of formula encoding of JavaScript objects and their properties. Note that property
 * access and prototype chain is managed by {@link PropertyAccessManager}.
 *
 * <p>An object is represented by an ID, which is encoded as a unique integer (formula). Those IDs
 * are managed by {@link ObjectIdFormulaManager}. The <a
 * href="https://www.ecma-international.org/ecma-262/5.1/#sec-4.3.11">null value</a> is represented
 * by a particular object ID (see {@link ObjectIdFormulaManager#getNullObjectId()}).
 *
 * <p>Properties are managed as an array formula that maps each property name (see {@link
 * StringFormulaManager}) to a variable (see {@link VariableManager}). The variable is used to
 * represent the value of the property. If a property is not set on an object it is mapped to a
 * special variable, which represents an unset field. That means that all property names (i.e.
 * strings) that might be used in the program (see {@link StringFormulaManager#getIdRange()}) are
 * mapped by the array formula.
 *
 * <p>Properties of an object may change in the course of the program. That means that there is a
 * array formula (property mapping) for each point in time. If a property is changed a new array
 * formula (updated property mapping) is created based on the old array formula. Those array
 * formulas are managed by a variable <i>objectFields</i> where a static index (see {@link SSAMap})
 * is used to represent the point in time ({@code objectFields@<point-in-time>}). Since there may
 * exist multiple objects in a program, the <i>objectFields</i> variable itself is an array that
 * maps object IDs to their current property mapping, i.e. <i>objectFields</i> is of SMT-formula
 * type:
 *
 * <pre>(Array objectId (Array propertyName propertyVariable))</pre>
 *
 * @see PropertyAccessManager For a description of the prototype chain formula encoding.
 */
class ObjectFormulaManager extends ManagerWithEdgeContext {

  static final String OBJECT_FIELDS_VARIABLE_NAME = "objectFields";

  /** Variable formula that represents an unset property. */
  private final IntegerFormula objectFieldNotSet;

  ObjectFormulaManager(final EdgeManagerContext pCtx) {
    super(pCtx);
    // Used as a special field value that represents unset fields of an object.
    // Since, variables are used as named values, but no explicit value is assigned to a variable
    // name, any value (here 0) can be used to represent a special variable value (that is not used
    // as another special variable value somewhere else)
    objectFieldNotSet = ifmgr.makeNumber(0);
  }

  TypedValue createObject(final JSObjectLiteralExpression pObjectLiteralExpression)
      throws UnrecognizedCodeException {
    final TypedValue objectValue = tvmgr.createObjectValue(objIdMgr.createObjectId());
    final IntegerFormula ovv = (IntegerFormula) objectValue.getValue();
    setObjectFields(ovv, pObjectLiteralExpression.getFields());
    return objectValue;
  }

  ArrayFormula<IntegerFormula, IntegerFormula> getObjectFields(final IntegerFormula pObjectId) {
    return afmgr.select(makeObjectFieldsVariable(), pObjectId);
  }

  private ArrayFormula<IntegerFormula, IntegerFormula> getObjectFields(
      final List<JSObjectLiteralField> pFields) throws UnrecognizedCodeException {
    final Map<IntegerFormula, JSObjectLiteralField> fieldById =
        pFields
            .stream()
            .collect(toMap(field -> strMgr.getStringFormula(field.getFieldName()), field -> field));
    ArrayFormula<IntegerFormula, IntegerFormula> objectFields =
        afmgr.makeArray("emptyObjectFields", OBJECT_FIELDS_TYPE);
    for (int stringId : strMgr.getIdRange()) {
      final IntegerFormula idFormula = ifmgr.makeNumber(stringId);
      final IntegerFormula fieldValue;
      if (fieldById.containsKey(idFormula)) {
        final JSObjectLiteralField field = fieldById.get(idFormula);
        final IntegerFormula fieldFormula = makeFieldVariable(field.getFieldName());
        ctx.constraints.addConstraint(markFieldAsSet(fieldFormula));
        ctx.constraints.addConstraint(
            ctx.assignmentMgr.makeAssignment(
                fieldFormula, ctx.exprMgr.makeExpression(field.getInitializer())));
        fieldValue = fieldFormula;
      } else {
        fieldValue = objectFieldNotSet;
      }
      objectFields = afmgr.store(objectFields, idFormula, fieldValue);
    }
    return objectFields;
  }

  void setObjectFields(final IntegerFormula pObjectId, final List<JSObjectLiteralField> pFields)
      throws UnrecognizedCodeException {
    setObjectFields(pObjectId, getObjectFields(pFields));
  }

  void setObjectFields(
      final IntegerFormula pObjectId,
      final ArrayFormula<IntegerFormula, IntegerFormula> pObjectFields) {
    ctx.constraints.addConstraint(
        afmgr.equivalence(
            afmgr.store(makeObjectFieldsVariable(), pObjectId, pObjectFields),
            fmgr.makeVariable(
                Types.OBJECT_FIELDS_VARIABLE_TYPE,
                OBJECT_FIELDS_VARIABLE_NAME,
                ctx.varIdMgr.makeFreshIndex(OBJECT_FIELDS_VARIABLE_NAME))));
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Nonnull
  private ArrayFormula<IntegerFormula, ArrayFormula<IntegerFormula, IntegerFormula>>
      makeObjectFieldsVariable() {
    if (!ctx.ssa.allVariables().contains(OBJECT_FIELDS_VARIABLE_NAME)) {
      ctx.varIdMgr.makeFreshIndex(OBJECT_FIELDS_VARIABLE_NAME);
      return afmgr.makeArray(
          OBJECT_FIELDS_VARIABLE_NAME,
          ctx.varIdMgr.makeFreshIndex(OBJECT_FIELDS_VARIABLE_NAME),
          Types.OBJECT_FIELDS_VARIABLE_TYPE);
    }
    return fmgr.makeVariable(
        Types.OBJECT_FIELDS_VARIABLE_TYPE,
        OBJECT_FIELDS_VARIABLE_NAME,
        ctx.ssa.getIndex(OBJECT_FIELDS_VARIABLE_NAME));
  }

  @Nonnull
  BooleanFormula markFieldAsNotSet(final IntegerFormula pField) {
    return fmgr.makeEqual(pField, objectFieldNotSet);
  }

  @Nonnull
  BooleanFormula markFieldAsSet(final IntegerFormula pField) {
    return bfmgr.not(markFieldAsNotSet(pField));
  }

  @Nonnull
  IntegerFormula makeFieldVariable(final String pFieldName) {
    return ctx.varMgr.makeFreshVariable("field_" + pFieldName);
  }

  /**
   * Make variable for field with unknown name, e.g. the name of the field is stored in a variable
   * like in <code>obj[fieldName]</code>.
   *
   * @return Formula of the field.
   */
  @Nonnull
  IntegerFormula makeFieldVariable() {
    return ctx.varMgr.makeFreshVariable("field");
  }
}
