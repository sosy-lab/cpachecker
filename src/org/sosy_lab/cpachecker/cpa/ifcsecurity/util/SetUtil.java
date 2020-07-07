// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * Utilities for computation of classical set-operations.
 * @param <E> Type of elements in the sets.
 */
public final class SetUtil<E extends Comparable<? super E>> {

  private SetUtil() {
    // Private constructor for utility class
  }

  /**
   * Computes a new set, that is the union of <i>set1</i> and <i>set2</i>.
   *
   * @param pSet1 a Set.
   * @param pSet2 the other Set
   * @return Setunion.
   */
  public static <E> NavigableSet<E> union(NavigableSet<E> pSet1, NavigableSet<E> pSet2) {
    NavigableSet<E> result = new TreeSet<>();
      E elem;

    for (E aPSet1 : pSet1) {
      elem = aPSet1;
        result.add(elem);
      }
    for (E aPSet2 : pSet2) {
      elem = aPSet2;
        result.add(elem);
      }
      return result;
    }

  /**
   * Computes a new set, that is the intersection of <i>set1</i> and <i>set2</i>.
   *
   * @param pSet1 a Set.
   * @param pSet2 the other Set
   * @return Setintersection.
   */
  public static <E extends Comparable<? super E>> NavigableSet<E> intersect(
      NavigableSet<E> pSet1, NavigableSet<E> pSet2) {
    NavigableSet<E> result = new TreeSet<>();
      Iterator<E> it1 = pSet1.iterator();
      Iterator<E> it2 = pSet2.iterator();

      if(!it1.hasNext()){
        return result;
      }
      if(!it2.hasNext()){
        return result;
      }

      E elem1=pSet1.first();
      E elem2=pSet2.first();

      while(true){
        if(elem1.compareTo(elem2)==0){
          result.add(elem1);
          if(!it1.hasNext()){
            break;
          }
          if(!it2.hasNext()){
            break;
          }
          elem1=it1.next();
          elem2=it2.next();
        }
        if(elem1.compareTo(elem2)<0){
          if(!it1.hasNext()){
            break;
          }
          elem1=it1.next();
        }
        if(elem1.compareTo(elem2)>0){
          if(!it2.hasNext()){
            break;
          }
          elem2=it2.next();
        }
      }

      return result;
    }

  /**
   * Computes a new set, that is the setminus of <i>set1</i> and <i>set2</i>.
   *
   * @param pSet1 a Set.
   * @param pSet2 the other Set
   * @return Setminus.
   */
  public static <E> NavigableSet<E> setminus(NavigableSet<E> pSet1, NavigableSet<E> pSet2) {
    NavigableSet<E> result = new TreeSet<>();
    for (E elem : pSet1) {
      if (!pSet2.contains(elem)) {
          result.add(elem);
        }
      }
      return result;
    }

  /**
   * Computes a new set, that is the powerset of <i>set1</i>
   *
   * @param pSet1 a Set
   * @return Powerset.
   */
  public static <E extends Comparable<? super E>>
      java.util.NavigableSet<java.util.NavigableSet<E>> getPowerSet(NavigableSet<E> pSet1) {
      List<E> list = new ArrayList<>(pSet1);
      int n = list.size();

    java.util.NavigableSet<java.util.NavigableSet<E>> powerSet =
        new TreeSet<>(new InternalSetComparator<E>());

      for( long i = 0; i < (1 << n); i++) {
      java.util.NavigableSet<E> element = new TreeSet<>();
          for( int j = 0; j < n; j++ ) {
            if( (i >> j) % 2 == 1 ) {
              element.add(list.get(j));
            }
          }
          powerSet.add(element);
      }
      return powerSet;
    }



}
