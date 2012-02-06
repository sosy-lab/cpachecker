/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.Collection;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.AssignmentsInPathConditionElement;
import org.sosy_lab.cpachecker.cpa.location.LocationElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractElements;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;

public class OmniscientCompositePrecisionAdjustment implements PrecisionAdjustment {

  private boolean modified = false;

  public OmniscientCompositePrecisionAdjustment() { }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment#prec(org.sosy_lab.cpachecker.core.interfaces.AbstractElement, org.sosy_lab.cpachecker.core.interfaces.Precision, java.util.Collection)
   */
  @Override
  public Triple<AbstractElement, Precision, Action> prec(AbstractElement pElement,
                                               Precision pPrecision,
                                               UnmodifiableReachedSet pElements) throws CPAException {
    modified = false;

    CompositeElement composite    = (CompositeElement)pElement;
    CompositePrecision precision  = (CompositePrecision)pPrecision;
    assert (composite.getElements().size() == precision.getPrecisions().size());

    int indexOfExplicitState = getIndexOfExplicitState(composite);
    if(indexOfExplicitState == -1) {
      throw new CPAException("The OmniscientCompositePrecisionAdjustment needs an ExplicitElement");
    }

    ImmutableList.Builder<AbstractElement> outElements  = ImmutableList.builder();
    ImmutableList.Builder<Precision> outPrecisions      = ImmutableList.builder();

    for (int i = 0, size = composite.getElements().size(); i < size; ++i) {
      if(i == indexOfExplicitState) {
        ExplicitElement explicit                  = AbstractElements.extractElementByType(composite, ExplicitElement.class);
        LocationElement location                  = AbstractElements.extractElementByType(composite, LocationElement.class);
        AssignmentsInPathConditionElement assigns = AbstractElements.extractElementByType(composite, AssignmentsInPathConditionElement.class);
        ExplicitPrecision explicitPrecision       = (ExplicitPrecision)precision.get(indexOfExplicitState);

        UnmodifiableReachedSet slice =
          new UnmodifiableReachedSetView(pElements, new ExplicitElementProjection(indexOfExplicitState), new ExplicitPrecisionProjection(indexOfExplicitState));

        ExplicitElement newElement = enforceReachedSetThreshold(explicit, explicitPrecision, slice.getReached(location.getLocationNode()));
        Pair<ExplicitElement, ExplicitPrecision> result = enforcePathThreshold(newElement, explicitPrecision, assigns);

        outElements.add(result.getFirst());
        outPrecisions.add(result.getSecond());
      }
      else {
        outElements.add(composite.get(i));
        outPrecisions.add(precision.get(i));
      }
    }

    AbstractElement outElement = modified ? new CompositeElement(outElements.build())     : pElement;
    Precision outPrecision     = modified ? new CompositePrecision(outPrecisions.build()) : pPrecision;

    return new Triple<AbstractElement, Precision, Action>(outElement, outPrecision, Action.CONTINUE);
  }

  private ExplicitElement enforceReachedSetThreshold(ExplicitElement element, ExplicitPrecision precision, Collection<AbstractElement> reachedSetAtLocation) {

    // create the mapping from variable name to the number of different values this variable has
    HashMultimap<String, Long> valueMapping = createMappingFromReachedSet(reachedSetAtLocation);

    // forget the value for all variables that exceed their threshold
    for(String variable : valueMapping.keySet()) {
      if(precision.getReachedSetThresholds().exceeds(variable, valueMapping.get(variable).size())) {
        //System.out.println("reachedSet: forgetting var " + variable);
        precision.getReachedSetThresholds().setExceeded(variable);
        element.forget(variable);
      }
    }

    return element;
  }

  private Pair<ExplicitElement, ExplicitPrecision> enforcePathThreshold(ExplicitElement element, ExplicitPrecision precision, AssignmentsInPathConditionElement assigns) {

    if(assigns != null) {
      // forget the value for all variables that exceed their threshold
      for(Map.Entry<String, Integer> entry : assigns.getAssignmentCounts().entrySet()) {
        if(precision.getPathThresholds().exceeds(entry.getKey(), entry.getValue())) {
          //System.out.println("path: forgetting var " + entry.getKey());

          // the path threshold precision is path sensitive, therefore, mutating a clone is mandatory
          if(modified == false) {
            precision = new ExplicitPrecision(precision);
            modified = true;
          }

          precision.getReachedSetThresholds().setExceeded(entry.getKey());
          element.forget(entry.getKey());
        }
      }
    }

    return Pair.of(element, precision);
  }

  private int getIndexOfExplicitState(CompositeElement composite) {
    for (int i = 0; i < composite.getElements().size(); ++i) {
      if(composite.get(i) instanceof ExplicitElement) {
        return i;
      }
    }

    return -1;
  }

  /**
   * This method creates the map which tracks how many different values are stored for a variable, based on the elements in the reached set.
   *
   * @param reachedSetAtLocation the collection of AbstractElements in the reached set that refer to the current location
   */
  private HashMultimap<String, Long> createMappingFromReachedSet(Collection<AbstractElement> reachedSetAtLocation) {
    HashMultimap<String, Long> valueMapping = HashMultimap.create();

    for(AbstractElement element : reachedSetAtLocation) {
      for(Map.Entry<String, Long> entry : ((ExplicitElement)element).getConstantsMap().entrySet()) {
        valueMapping.put(entry.getKey(), entry.getValue());
      }
    }

    return valueMapping;
  }

  private static class ExplicitElementProjection
    implements Function<AbstractElement, AbstractElement>
  {
    private final int dimension;

    public ExplicitElementProjection(int d) {
      dimension = d;
    }

    @Override
    public AbstractElement apply(AbstractElement from) {
      return ((CompositeElement)from).get(dimension);
    }
  }

  private static class ExplicitPrecisionProjection
  implements Function<Precision, Precision>
  {
    private final int dimension;

    public ExplicitPrecisionProjection(int d) {
      dimension = d;
    }

    @Override
    public Precision apply(Precision from) {
      return ((CompositePrecision)from).get(dimension);
    }
  }
}
