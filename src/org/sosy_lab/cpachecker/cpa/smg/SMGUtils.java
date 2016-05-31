/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.base.Predicate;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
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
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGEdgeHasValueTemplate;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGEdgeHasValueTemplateWithConcreteValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGEdgePointsToTemplate;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGObjectTemplate;

import java.util.List;
import java.util.Set;

/**
 * This class contains smg utilities, for example filters.
 */
public final class SMGUtils {


  public static class FilterFieldsOfValue
      implements Predicate<SMGEdgeHasValueTemplate> {

    private final int value;

    public FilterFieldsOfValue(int pValue) {
      value = pValue;
    }

    @Override
    public boolean apply(SMGEdgeHasValueTemplate pEdge) {
      return value == pEdge.getAbstractValue();
    }
  }

  private SMGUtils() {}

  public static Set<SMGEdgeHasValue> getFieldsOfObject(SMGObject pSmgObject, SMG pInputSMG) {

    SMGEdgeHasValueFilter edgeFilter = SMGEdgeHasValueFilter.objectFilter(pSmgObject);
    return pInputSMG.getHVEdges(edgeFilter);
  }

  public static Set<SMGEdgePointsTo> getPointerToThisObject(SMGObject pSmgObject, SMG pInputSMG) {
    SMGEdgePointsToFilter objectFilter = SMGEdgePointsToFilter.targetObjectFilter(pSmgObject);
    return pInputSMG.getPtEdges(objectFilter);
  }

  public static Set<SMGEdgeHasValue> getFieldsofThisValue(int value, SMG pInputSMG) {
    SMGEdgeHasValueFilter valueFilter = SMGEdgeHasValueFilter.valueFilter(value);
    return pInputSMG.getHVEdges(valueFilter);
  }

  public static class FilterTargetTemplate implements Predicate<SMGEdgePointsToTemplate> {

    private final SMGObjectTemplate objectTemplate;

    public FilterTargetTemplate(SMGObjectTemplate pObjectTemplate) {
      objectTemplate = pObjectTemplate;
    }

    @Override
    public boolean apply(SMGEdgePointsToTemplate ptEdge) {
      return ptEdge.getObjectTemplate() == objectTemplate;
    }
  }

  public static class FilterTemplateObjectFieldsWithConcreteValue implements Predicate<SMGEdgeHasValueTemplateWithConcreteValue> {

    private final SMGObjectTemplate objectTemplate;

    public FilterTemplateObjectFieldsWithConcreteValue(SMGObjectTemplate pObjectTemplate) {
      objectTemplate = pObjectTemplate;
    }

    @Override
    public boolean apply(SMGEdgeHasValueTemplateWithConcreteValue ptEdge) {
      return ptEdge.getObjectTemplate() == objectTemplate;
    }
  }

  public static boolean isRecursiveOnOffset(CType pType, int fieldOffset, MachineModel pModel) {

    CFieldTypeVisitor v = new CFieldTypeVisitor(fieldOffset, pModel);

    CType typeAtOffset = pType.accept(v);

    if (CFieldTypeVisitor.isUnkownInstance(typeAtOffset)) {
      return false;
    }

    return pType.getCanonicalType().equals(typeAtOffset.getCanonicalType());
  }

  private static class CFieldTypeVisitor implements CTypeVisitor<CType, RuntimeException> {

    private final int fieldOffset;
    private final MachineModel model;
    private static final CType UNKNOWN = new CSimpleType(false, false, CBasicType.UNSPECIFIED,
        false, false, false, false, false, false, false);

    public CFieldTypeVisitor(int pFieldOffset, MachineModel pModel) {
      fieldOffset = pFieldOffset;
      model = pModel;
    }

    public static boolean isUnkownInstance(CType type) {
      return type == UNKNOWN;
    }

    @Override
    public CType visit(CArrayType pArrayType) {
      if (model.getSizeof(pArrayType) % fieldOffset == 0) {
        return pArrayType.getType();
      } else {
        return UNKNOWN;
      }
    }

    @Override
    public CType visit(CCompositeType pCompositeType) {

      List<CCompositeTypeMemberDeclaration> members = pCompositeType.getMembers();

      int memberOffset = 0;
      for (CCompositeTypeMemberDeclaration member : members) {

        if (fieldOffset == memberOffset) {
          return member.getType();
        } else if (memberOffset > fieldOffset) {
          return UNKNOWN;
        } else {
          memberOffset = memberOffset + model.getSizeof(member.getType());
        }
      }

      return UNKNOWN;
    }

    @Override
    public CType visit(CElaboratedType pElaboratedType) {
      return pElaboratedType.getRealType().accept(this);
    }

    @Override
    public CType visit(CEnumType pEnumType) {
      return UNKNOWN;
    }

    @Override
    public CType visit(CFunctionType pFunctionType) {
      return UNKNOWN;
    }

    @Override
    public CType visit(CPointerType pPointerType) {
      return pPointerType.getType().accept(this);
    }

    @Override
    public CType visit(CProblemType pProblemType) {
      return UNKNOWN;
    }

    @Override
    public CType visit(CSimpleType pSimpleType) {
      return UNKNOWN;
    }

    @Override
    public CType visit(CTypedefType pTypedefType) {
      return pTypedefType.getRealType();
    }

    @Override
    public CType visit(CVoidType pVoidType) {
      return UNKNOWN;
    }
  }
}