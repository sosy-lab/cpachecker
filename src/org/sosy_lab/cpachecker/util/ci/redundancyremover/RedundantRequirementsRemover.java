/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ci.redundancyremover;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Ordering;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.cpa.sign.SignState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class RedundantRequirementsRemover {



  public static List<Pair<ARGState, Collection<ARGState>>> removeRedundantRequirements(
      final List<Pair<ARGState, Collection<ARGState>>> requirements,
      final List<Pair<List<String>, List<String>>> inputOutputSignatures,
      final Class<? extends AbstractState> reqStateClass) {

    RedundantRequirementsRemoverImplementation<? extends AbstractState, ? extends Object> remover;

    if (reqStateClass.equals(ValueAnalysisState.class)) {
      remover = new RedundantRequirementsValueAnalysisStateImplementation();
    } else if (reqStateClass.equals(IntervalAnalysisState.class)) {
      remover = new RedundantRequirementsRemoverIntervalStateImplementation();
    } else if (reqStateClass.equals(SignState.class)) {
      remover = new RedundantRequirementsRemoverSignStateImplementation();
    } else {
      return requirements;
    }
    return remover.identifyAndRemoveRedundantRequirements(requirements, inputOutputSignatures);
  }

  public static abstract class RedundantRequirementsRemoverImplementation<S extends AbstractState, V>
      implements Comparator<V>, Serializable {

    private static final long serialVersionUID = 2610823786116954949L;
    private SortingArrayHelper sortHelper = new SortingArrayHelper();

    protected abstract boolean covers(final V covering, final V covered);

    protected abstract V getAbstractValue(final S abstractState, final String varOrConst);

    protected abstract V[] emptyArrayOfSize(final int size);

    protected abstract V[][] emptyMatrixOfSize(final int size);

    protected abstract S extractState(final AbstractState wrapperState);

    private V[] getAbstractValues(final S abstractState, final List<String> varsAndConsts) {
      V[] result = emptyArrayOfSize(varsAndConsts.size());
      int i = 0;
      for (String varOrConst : varsAndConsts) {
        result[i] = getAbstractValue(abstractState, varOrConst);
      }
      return result;
    }

    private V[][] getAbstractValuesForSignature(final ARGState start,
        final Collection<ARGState> ends, final List<String> inputVarsAndConsts) throws CPAException {
      V[][] result = emptyMatrixOfSize(1 + ends.size());
      List<V[]> intermediate = new ArrayList<>(ends.size());

      result[0] = getAbstractValues(extractState(start), inputVarsAndConsts);

      CFANode loc = null;
      for (ARGState end : ends) {
        if (loc == null) {
          loc = AbstractStates.extractLocation(end);
        } else {
          if (!loc.equals(AbstractStates.extractLocation(end))) { throw new CPAException(""); }
        }

        intermediate.add(getAbstractValues(extractState(start), inputVarsAndConsts));
      }

      Collections.sort(intermediate, sortHelper);

      for (int i = 0, j = 1; i < intermediate.size(); i++, j++) {
        result[j] = intermediate.get(i);
      }

      return result;
    }

    private boolean covers(final V[] covering, final V[] covered) {
      if (covering.length == covered.length) {
        for (int i = 0; i < covering.length; i++) {
          if (covers(covering[i], covered[i])) { return false; }
        }
        return true;
      }
      return false;
    }

    private boolean covers(V[][] covering, final V[][] covered) {
      if (covers(covering[0], covered[0])) {
        boolean isCovered;
        for (int i = 1; i < covering.length; i++) {
          isCovered = false;

          for (int j = 1; j < covered.length; j++) {
            if (covers(covered[j], covering[i])) {
              isCovered = true;
              break;
            }
          }

          if (!isCovered) { return false; }
        }
        return true;
      }
      return false;
    }

    public List<Pair<ARGState, Collection<ARGState>>> identifyAndRemoveRedundantRequirements(
        final List<Pair<ARGState, Collection<ARGState>>> requirements,
        final List<Pair<List<String>, List<String>>> inputOutputSignatures) {
      assert (requirements.size() == inputOutputSignatures.size());

      // get values for signature
      List<Pair<V[][], Pair<ARGState, Collection<ARGState>>>> sortList =
          new ArrayList<>(requirements.size());
      try {
        for (int i = 0; i < requirements.size(); i++) {
          sortList.add(Pair.of(
              getAbstractValuesForSignature(requirements.get(i).getFirst(), requirements.get(i)
                  .getSecond(), inputOutputSignatures.get(i).getFirst()), requirements.get(i)));
        }
        // sort according to signature values
        Collections.sort(sortList, new SortingHelper());

        List<Pair<ARGState, Collection<ARGState>>> reducedReq =
            new ArrayList<>(sortList.size());

        // check for covered requirements
        nextReq: for (int i = 0; i < sortList.size(); i++) {
          for (int j = 0; j < i; j++) {
            if (covers(sortList.get(j).getFirst(), sortList.get(i).getFirst())) {
              continue nextReq;
            }
          }
          reducedReq.add(sortList.get(i).getSecond());
        }

        return reducedReq;
      } catch (CPAException e) {
        // return unmodified set
        return requirements;
      }
    }

    @SuppressFBWarnings(value="SE_INNER_CLASS", justification="Cannot make class static as suggested because require generic type parameters of outer class. Removing interface Serializable is also no option because it introduces another warning suggesting to implement Serializable interface.")
    private class SortingArrayHelper implements Comparator<V[]>, Serializable {

      private static final long serialVersionUID = 3970718511743910013L;

      @Override
      public int compare(final V[] arg0, final V[] arg1) {
        checkNotNull(arg0);
        checkNotNull(arg1);

        if (arg0.length == 0 || arg1.length == 0) {
          return Integer.compare(arg1.length, arg0.length); // reverse
        }

        return Ordering.from(RedundantRequirementsRemoverImplementation.this)
            .lexicographical()
            .compare(Arrays.asList(arg0), Arrays.asList(arg1));
      }

    }


    @SuppressFBWarnings(value="SE_INNER_CLASS", justification="Cannot make class static as suggested because require generic type parameters of outer class. Removing interface Serializable is also no option because it introduces another warning suggesting to implement Serializable interface.")
    private class SortingHelper implements
        Comparator<Pair<V[][], Pair<ARGState, Collection<ARGState>>>>, Serializable {

      private static final long serialVersionUID = 3894486288294859800L;

      @Override
      public int compare(final Pair<V[][], Pair<ARGState, Collection<ARGState>>> arg0,
          final Pair<V[][], Pair<ARGState, Collection<ARGState>>> arg1) {
        if (arg0 == null || arg1 == null) { throw new NullPointerException(); }

        V[][] firstArg = arg0.getFirst();
        V[][] secondArg = arg1.getFirst();

        if (firstArg == null || secondArg == null) { return firstArg == null ? 1
            : 0 + (secondArg == null ? -1 : 0); }

        if (firstArg.length == 0 || secondArg.length == 0) { return -(firstArg.length - secondArg.length); }

        // compare first
        if (firstArg[0].length != secondArg[0].length) {
          return Integer.compare(secondArg[0].length, firstArg[0].length); // reverse
        }

        int r = sortHelper.compare(secondArg[0],firstArg[0]);

        if (r != 0) { return r; }

        // compare remaining parts
        if (firstArg.length != secondArg.length) {
          return Integer.compare(secondArg.length, firstArg.length); // reverse
        }

        return Ordering.from(sortHelper)
            .lexicographical()
            .compare(
                Arrays.asList(firstArg).subList(1, firstArg.length),
                Arrays.asList(secondArg).subList(1, secondArg.length));
      }
    }

  }

}
