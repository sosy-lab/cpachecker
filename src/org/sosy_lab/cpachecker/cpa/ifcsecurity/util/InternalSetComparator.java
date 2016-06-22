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

import com.google.common.collect.Ordering;

import java.io.Serializable;
import java.util.Comparator;
import java.util.SortedSet;

/**
 * Comparator for Sets. Establish a Ordering depending on
 * 1. Size
 * 2. Comparator of elements.
 * @param <E> Type of elements in the sets.
 */
public class InternalSetComparator<E extends Comparable<? super E>> implements Serializable, Comparator<SortedSet<E>>{

    private static final long serialVersionUID = -4025316246248802824L;

    @Override
    public int compare(SortedSet<E> pObj1, SortedSet<E> pObj2) {
      if(pObj1.size()<pObj2.size()){
        return -1;
      }
      if(pObj1.size()>pObj2.size()){
        return 1;
      }
      return Ordering.<E>natural().lexicographical().compare(pObj1, pObj2);
    }
  }