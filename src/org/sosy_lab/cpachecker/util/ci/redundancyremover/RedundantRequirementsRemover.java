// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci.redundancyremover;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Comparators;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.cpa.sign.SignState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

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

  public abstract static class RedundantRequirementsRemoverImplementation<
          S extends AbstractState, V>
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
        result[i++] = getAbstractValue(abstractState, varOrConst);
      }
      return result;
    }

    private V[][] getAbstractValuesForSignature(
        final ARGState start,
        final Collection<ARGState> ends,
        final List<String> inputVarsAndConsts)
        throws CPAException {
      V[][] result = emptyMatrixOfSize(1 + ends.size());

      result[0] = getAbstractValues(extractState(start), inputVarsAndConsts);
      int i = 1;

      CFANode loc = null;
      for (ARGState end : ends) {
        if (loc == null) {
          loc = AbstractStates.extractLocation(end);
        } else {
          if (!loc.equals(AbstractStates.extractLocation(end))) {
            throw new CPAException("");
          }
        }

        result[i] = getAbstractValues(extractState(end), inputVarsAndConsts);
        i++;
      }

      Arrays.sort(result, 1, ends.size() + 1, sortHelper);

      return result;
    }

    private boolean covers(final V[] covering, final V[] covered) {
      if (covering.length == covered.length) {
        for (int i = 0; i < covering.length; i++) {
          if (covers(covering[i], covered[i])) {
            return false;
          }
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

          if (!isCovered) {
            return false;
          }
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
          sortList.add(
              Pair.of(
                  getAbstractValuesForSignature(
                      requirements.get(i).getFirst(),
                      requirements.get(i).getSecond(),
                      inputOutputSignatures.get(i).getFirst()),
                  requirements.get(i)));
        }
        // sort according to signature values
        sortList.sort(new SortingHelper());

        List<Pair<ARGState, Collection<ARGState>>> reducedReq = new ArrayList<>(sortList.size());

        // check for covered requirements
        nextReq:
        for (int i = 0; i < sortList.size(); i++) {
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

    @SuppressFBWarnings(
        value = "SE_INNER_CLASS",
        justification =
            "Cannot make class static as suggested because require generic type parameters of outer"
                + " class. Removing interface Serializable is also no option because it introduces"
                + " another warning suggesting to implement Serializable interface.")
    private class SortingArrayHelper implements Comparator<V[]>, Serializable {

      private static final long serialVersionUID = 3970718511743910013L;

      @Override
      public int compare(final V[] arg0, final V[] arg1) {
        checkNotNull(arg0);
        checkNotNull(arg1);

        if (arg0.length == 0 || arg1.length == 0) {
          return Integer.compare(arg1.length, arg0.length); // reverse
        }

        return Comparators.lexicographical(RedundantRequirementsRemoverImplementation.this)
            .compare(Arrays.asList(arg0), Arrays.asList(arg1));
      }
    }

    @SuppressFBWarnings(
        value = "SE_INNER_CLASS",
        justification =
            "Cannot make class static as suggested because require generic type parameters of outer"
                + " class. Removing interface Serializable is also no option because it introduces"
                + " another warning suggesting to implement Serializable interface.")
    private class SortingHelper
        implements Comparator<Pair<V[][], Pair<ARGState, Collection<ARGState>>>>, Serializable {

      private static final long serialVersionUID = 3894486288294859800L;

      @Override
      public int compare(
          final Pair<V[][], Pair<ARGState, Collection<ARGState>>> arg0,
          final Pair<V[][], Pair<ARGState, Collection<ARGState>>> arg1) {
        if (arg0 == null || arg1 == null) {
          throw new NullPointerException();
        }

        V[][] firstArg = arg0.getFirst();
        V[][] secondArg = arg1.getFirst();

        if (firstArg == null || secondArg == null) {
          return firstArg == null ? 1 : 0 + (secondArg == null ? -1 : 0);
        }

        if (firstArg.length == 0 || secondArg.length == 0) {
          return -(firstArg.length - secondArg.length);
        }

        // compare first
        if (firstArg[0].length != secondArg[0].length) {
          return Integer.compare(secondArg[0].length, firstArg[0].length); // reverse
        }

        int r = sortHelper.compare(secondArg[0], firstArg[0]);

        if (r != 0) {
          return r;
        }

        // compare remaining parts
        if (firstArg.length != secondArg.length) {
          return Integer.compare(secondArg.length, firstArg.length); // reverse
        }

        return Comparators.lexicographical(sortHelper)
            .compare(
                Arrays.asList(firstArg).subList(1, firstArg.length),
                Arrays.asList(secondArg).subList(1, secondArg.length));
      }
    }
  }
}
