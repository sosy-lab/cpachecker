/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.invariants.balancer;

import java.util.HashSet;

/**
 *  Iterates over all subsets of {start, start+1, ..., whole} of order part,
 */
public class SubsetGenerator {

  private int whole;
  private int part;
  private int choice;
  private boolean hasmore;
  private SubsetGenerator rest;

  public SubsetGenerator(int whole, int part) {
    build(whole, part, 0);
  }

  public SubsetGenerator(int whole, int part, int start) {
    build(whole, part, start);
  }

  private void build(int whole, int part, int start) {

    // If part > whole, or start > whole - part, throw exception.
    if (part > whole || start > whole - part) {
      throw new IllegalArgumentException("Must have part <= whole and start <= whole - part.");
    }

    this.whole = whole;
    this.part = part;
    choice = start;
    if (part > 0) {
      rest = new SubsetGenerator(whole, part-1, choice+1);
    } else {
      rest = null;
    }
    hasmore = true;

  }

  public boolean hasMore() {
    return hasmore;
  }

  public HashSet<Integer> getNext() {
    // If hasMore returned false, you get null.
    HashSet<Integer> R = null;
    if (part == 0) {
      R = new HashSet<>();
      hasmore = false;
    } else if (rest.hasMore()) {
      R = rest.getNext();
      R.add(Integer.valueOf(choice));
    } else if (choice < whole - part) {
      choice += 1;
      rest = new SubsetGenerator(whole, part-1, choice+1);
      R = rest.getNext();
      R.add(Integer.valueOf(choice));
    }
    if (choice == whole - part && (rest == null || !rest.hasMore() )) {
      hasmore = false;
    }
    return R;
  }

  public static void main(String[] args) {
    // Test:
    SubsetGenerator SG = new SubsetGenerator(5, 3);
    HashSet<Integer> S;
    while (SG.hasMore()) {
      S = SG.getNext();
      System.out.println(S);
    }
  }

}
