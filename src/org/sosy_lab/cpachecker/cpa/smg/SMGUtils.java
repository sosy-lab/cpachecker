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

import java.util.Set;

import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGEdgeHasValueTemplate;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGEdgeHasValueTemplateWithConcreteValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGEdgePointsToTemplate;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGObjectTemplate;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

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
    Set<SMGEdgePointsTo> result = FluentIterable.from(pInputSMG.getPTEdges().values())
        .filter(new FilterTargetObject(pSmgObject)).toSet();
    return result;
  }

  public static Set<SMGEdgeHasValue> getFieldsofThisValue(int value, SMG pInputSMG) {
    SMGEdgeHasValueFilter valueFilter = new SMGEdgeHasValueFilter();
    valueFilter.filterHavingValue(value);
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

  public static class FilterTargetObject implements Predicate<SMGEdgePointsTo> {

    private final SMGObject object;

    public FilterTargetObject(SMGObject pObject) {
      object = pObject;
    }

    @Override
    public boolean apply(SMGEdgePointsTo ptEdge) {
      return ptEdge.getObject() == object;
    }
  }
}