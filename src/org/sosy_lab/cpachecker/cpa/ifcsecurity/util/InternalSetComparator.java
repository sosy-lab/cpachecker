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

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

/**
 * Comparator for Sets. Establish a Ordering depending on
 * 1. Size
 * 2. Comparator of elements.
 * @param <E> Type of elements in the sets.
 */
public class InternalSetComparator<E extends Comparable<? super E>> implements Serializable, Comparator<SortedSet<E>>{
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(SortedSet<E> obj1, SortedSet<E> obj2) {
      if(obj1.size()<obj2.size()){
        return -1;
      }
      if(obj1.size()>obj2.size()){
        return 1;
      }
      if(obj1.size()==obj2.size()){
        int n=obj1.size();
        Iterator<E> it1 = obj1.iterator();
        Iterator<E> it2 = obj2.iterator();
        for(int i=0;i<n;i++){
          E elem1=it1.next();
          E elem2=it2.next();


          if(elem1.compareTo(elem2)!=0){
            return elem1.compareTo(elem2);
          }
        }
        return 0;
      }
      return 0;
    }
  }