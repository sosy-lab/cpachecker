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
package org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl;

import java.lang.reflect.Array;
import java.util.List;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;


public class GenericsHelper {
  public static long[] toPrimitiveL(List<Long> types) {
    return Longs.toArray(types);
  }
  public static int[] toPrimitiveI(List<Integer> types) {
    return Ints.toArray(types);
  }

  public static long[] toPrimitive(Long[] longs){
    long[] l = new long[longs.length];
    for (int i = 0; i< longs.length; i++){
      l[i] = longs[i];
    }
    return l;
  }
  public static Long[] toGeneric(long[] longs){
    Long[] l = new Long[longs.length];
    for (int i = 0; i< longs.length; i++){
      l[i] = longs[i];
    }
    return l;
  }

  @SuppressWarnings("unchecked")
  public static <T> T[] toArray(Class<T> tclazz, List<T> pArgs) {
    T[] retArray = (T[]) Array.newInstance(tclazz, pArgs.size());
    return pArgs.toArray(retArray);
  }
}
