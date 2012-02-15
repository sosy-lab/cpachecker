/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;


/**
 * Specifies a collection of concurrent threads.
 */
public class ParallelCFAS implements Iterable<ThreadCFA> {

  private final ImmutableList<ThreadCFA> cfas;
  private final ImmutableSet<String> globalVariables;
  private final ImmutableSet<String> allVariables;
  private final int threadNo;

  public ParallelCFAS(List<ThreadCFA> cfas, Set<String> globalVariables) {
    this.cfas = ImmutableList.copyOf(cfas);
    this.globalVariables = ImmutableSet.copyOf(globalVariables);
    this.threadNo = cfas.size();

    Builder<String> bldr = ImmutableSet.builder();
    bldr = bldr.addAll(globalVariables);

    for (ThreadCFA cfa : cfas){
      bldr = bldr.addAll(cfa.getLocalVars());
    }

    this.allVariables = bldr.build();
  }

  public ImmutableList<ThreadCFA> getCfas() {
    return cfas;
  }

  public ThreadCFA getCfa(int i) {
    return cfas.get(i);
  }

  public ImmutableSet<String> getGlobalVariables() {
    return globalVariables;
  }

  public ImmutableSet<String> getLocalVars(int tid) {
    return cfas.get(tid).getLocalVars();
  }

  public int getThreadNo() {
    return threadNo;
  }

  public ImmutableSet<String> getAllVariables() {
    return allVariables;
  }

  @Override
  public Iterator<ThreadCFA> iterator() {
    return cfas.iterator();
  }




}
