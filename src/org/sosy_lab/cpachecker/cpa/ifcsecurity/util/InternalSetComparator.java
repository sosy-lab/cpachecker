// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity.util;

import com.google.common.collect.Comparators;
import java.io.Serializable;
import java.util.Comparator;
import java.util.NavigableSet;

/**
 * Comparator for Sets. Establish a Ordering depending on 1. Size 2. Comparator of elements.
 *
 * @param <E> Type of elements in the sets.
 */
public class InternalSetComparator<E extends Comparable<? super E>>
    implements Serializable, Comparator<NavigableSet<E>> {

    private static final long serialVersionUID = -4025316246248802824L;

  private final Comparator<Iterable<E>> setComparator =
      Comparators.lexicographical(Comparator.<E>naturalOrder());

  @Override
  public int compare(NavigableSet<E> pObj1, NavigableSet<E> pObj2) {
      if(pObj1.size()<pObj2.size()){
        return -1;
      }
      if(pObj1.size()>pObj2.size()){
        return 1;
      }
    return setComparator.compare(pObj1, pObj2);
    }
  }