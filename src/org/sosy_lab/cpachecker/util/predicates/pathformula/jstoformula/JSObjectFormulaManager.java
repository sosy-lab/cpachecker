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
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.OBJECT_ID_TYPE;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralField;
import org.sosy_lab.cpachecker.cfa.ast.js.JSRightHandSideVisitor;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

class JSObjectFormulaManager {

  static final String OBJECT_FIELDS_VARIABLE_NAME = "objectFields";
  private long objectIdCounter;
  private final IntegerFormula objectFieldNotSet;

  // TODO only needs makeFreshIndex and makeFreshVariable of converter
  private final JSToFormulaConverter conv;
  private final FormulaManagerView fmgr;
  private final ArrayFormulaManagerView afmgr;
  private final BooleanFormulaManagerView bfmgr;

  JSObjectFormulaManager(final JSToFormulaConverter pConv, final FormulaManagerView pFmgr) {
    conv = pConv;
    fmgr = pFmgr;
    afmgr = fmgr.getArrayFormulaManager();
    bfmgr = pFmgr.getBooleanFormulaManager();

    objectIdCounter = 0;
    // Used as a special field value that represents unset fields of an object.
    // Since, variables are used as named values, but no explicit value is assigned to a variable
    // name, any value (here 0) can be used to represent a special variable value (that is not used
    // as another special variable value somewhere else)
    objectFieldNotSet = pFmgr.getIntegerFormulaManager().makeNumber(0);
  }

  IntegerFormula createObjectId() {
    return fmgr.makeNumber(OBJECT_ID_TYPE, ++objectIdCounter);
  }

  JSObjectFormulaManagerWithContext withContext(
      final SSAMapBuilder pSsa,
      final Constraints pConstraints,
      final JSRightHandSideVisitor<TypedValue, UnrecognizedCodeException> pExprMgr) {
    return new JSObjectFormulaManagerWithContext(pSsa, pConstraints, pExprMgr);
  }

  class JSObjectFormulaManagerWithContext {
    final SSAMapBuilder ssa;
    final Constraints constraints;
    private final JSRightHandSideVisitor<TypedValue, UnrecognizedCodeException> exprMgr;

    JSObjectFormulaManagerWithContext(
        final SSAMapBuilder pSsa,
        final Constraints pConstraints,
        final JSRightHandSideVisitor<TypedValue, UnrecognizedCodeException> pExprMgr) {
      ssa = pSsa;
      constraints = pConstraints;
      exprMgr = pExprMgr;
    }

    TypedValue createObject(final JSObjectLiteralExpression pObjectLiteralExpression)
        throws UnrecognizedCodeException {
      final TypedValue objectValue = conv.tvmgr.createObjectValue(createObjectId());
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
              .collect(toMap(field -> conv.getStringFormula(field.getFieldName()), field -> field));
      ArrayFormula<IntegerFormula, IntegerFormula> objectFields =
          conv.afmgr.makeArray("emptyObjectFields", OBJECT_FIELDS_TYPE);
      for (int stringId = 1; stringId <= conv.maxFieldNameCount; stringId++) {
        final IntegerFormula idFormula = conv.ifmgr.makeNumber(stringId);
        final IntegerFormula fieldValue;
        if (fieldById.containsKey(idFormula)) {
          final JSObjectLiteralField field = fieldById.get(idFormula);
          final IntegerFormula fieldFormula = makeFieldVariable(field.getFieldName());
          constraints.addConstraint(markFieldAsSet(fieldFormula));
          constraints.addConstraint(
              conv.makeAssignment(fieldFormula, exprMgr.visit(field.getInitializer())));
          fieldValue = fieldFormula;
        } else {
          fieldValue = objectFieldNotSet;
        }
        objectFields = conv.afmgr.store(objectFields, idFormula, fieldValue);
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
      constraints.addConstraint(
          afmgr.equivalence(
              afmgr.store(makeObjectFieldsVariable(), pObjectId, pObjectFields),
              fmgr.makeVariable(
                  Types.OBJECT_FIELDS_VARIABLE_TYPE,
                  OBJECT_FIELDS_VARIABLE_NAME,
                  conv.makeFreshIndex(OBJECT_FIELDS_VARIABLE_NAME, ssa))));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Nonnull
    private ArrayFormula<IntegerFormula, ArrayFormula<IntegerFormula, IntegerFormula>>
        makeObjectFieldsVariable() {
      if (!ssa.allVariables().contains(OBJECT_FIELDS_VARIABLE_NAME)) {
        conv.makeFreshIndex(OBJECT_FIELDS_VARIABLE_NAME, ssa);
        return afmgr.makeArray(
            OBJECT_FIELDS_VARIABLE_NAME,
            conv.makeFreshIndex(OBJECT_FIELDS_VARIABLE_NAME, ssa),
            Types.OBJECT_FIELDS_VARIABLE_TYPE);
      }
      return fmgr.makeVariable(
          Types.OBJECT_FIELDS_VARIABLE_TYPE,
          OBJECT_FIELDS_VARIABLE_NAME,
          ssa.getIndex(OBJECT_FIELDS_VARIABLE_NAME));
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
      return conv.makeFreshVariable("field_" + pFieldName, ssa);
    }

    /**
     * Make variable for field with unknown name, e.g. the name of the field is stored in a variable
     * like in <code>obj[fieldName]</code>.
     *
     * @return Formula of the field.
     */
    @Nonnull
    IntegerFormula makeFieldVariable() {
      return conv.makeFreshVariable("field", ssa);
    }
  }
}
