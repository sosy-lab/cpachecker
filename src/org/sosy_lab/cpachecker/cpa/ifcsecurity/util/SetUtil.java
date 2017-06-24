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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
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
     * @param pSet1 a Set.
     * @param pSet2 the other Set
     * @return Setunion.
     */
    public static <E> SortedSet<E> union(SortedSet<E> pSet1,SortedSet<E> pSet2){
      SortedSet<E> result=new TreeSet<>();
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
     * @param pSet1 a Set.
     * @param pSet2 the other Set
     * @return Setintersection.
     */
    public static <E extends Comparable<? super E>> SortedSet<E> intersect(SortedSet<E> pSet1,SortedSet<E> pSet2){
      SortedSet<E> result=new TreeSet<>();
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
     * @param pSet1 a Set.
     * @param pSet2 the other Set
     * @return Setminus.
     */
    public static <E> SortedSet<E> setminus(SortedSet<E> pSet1,SortedSet<E> pSet2){
      SortedSet<E> result=new TreeSet<>();
    for (E elem : pSet1) {
      if (!pSet2.contains(elem)) {
          result.add(elem);
        }
      }
      return result;
    }

    /**
     * Computes a new set, that is the powerset of <i>set1</i>
     * @param pSet1 a Set
     * @return Powerset.
     */
    public static <E extends Comparable<? super E>> java.util.SortedSet<java.util.SortedSet<E>> getPowerSet(SortedSet<E> pSet1){
      List<E> list = new ArrayList<>(pSet1);
      int n = list.size();

      java.util.SortedSet<java.util.SortedSet<E>> powerSet = new TreeSet<>(new InternalSetComparator<E>());

      for( long i = 0; i < (1 << n); i++) {
          java.util.SortedSet<E> element = new TreeSet<>();
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
