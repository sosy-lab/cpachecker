/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.policies;

import com.google.common.collect.ComparisonChain;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class Constellation<T extends SecurityClasses, E extends SecurityClasses>{

  Set<Pair<T, E>> internalSet;

  public Constellation() {
    internalSet = new TreeSet<>();
  }

  public void addPair(T pFirst, E pSecond) {
    internalSet.add(new Pair<>(pFirst, pSecond));
  }

  public void removePair(T pFirst, E pSecond) {
    internalSet.remove(new Pair<>(pFirst, pSecond));
  }

  public boolean containsPair(T pFirst, E pSecond) {
    return internalSet.contains(new Pair<>(pFirst, pSecond));
  }

  public int size(){
    return internalSet.size();
  }

  class Pair<T2 extends Comparable<? super T2>, E2 extends Comparable<? super E2>>
      implements Comparable<Pair<T2, E2>>, Serializable {

    private static final long serialVersionUID = -8159968133789322657L;
    T2 first;
    E2 second;

    public Pair(T2 pFirst, E2 pSecond) {
      this.first = pFirst;
      this.second = pSecond;
    }

    public T2 getFirst() {
      return first;
    }

    public void setFirst(T2 pFirst) {
      first = pFirst;
    }

    public E2 getSecond() {
      return second;
    }

    public void setSecond(E2 pSecond) {
      second = pSecond;
    }

    @Override
    public String toString() {
      // TODO Auto-generated method stub
      return "(" + first.toString() + "," + second.toString() + ")";
    }

    @Override
    public int compareTo(Pair<T2, E2> pOtherPair) {
      return ComparisonChain.start()
          .compare(first, pOtherPair.first)
          .compare(second, pOtherPair.second)
          .result();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object pObj) {
      if (this == pObj) { return true; }
      if (pObj instanceof Pair) {
        Pair<T2, E2> other = (Pair<T2, E2>) pObj;
        return first.equals(other.first)
            && second.equals(other.second);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(first, second);
    }

  }

  @Override
  public String toString() {
    return internalSet.toString();
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    Constellation<SecurityClasses, SecurityClasses> clone=new Constellation<>();
    for(Constellation<T, E>.Pair<T, E> pair:this.internalSet){
      T first=pair.first;
      E second=pair.second;
      clone.addPair(first, second);
    }
    return clone;
  }

}
