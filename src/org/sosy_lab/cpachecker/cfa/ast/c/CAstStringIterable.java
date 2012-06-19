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
package org.sosy_lab.cpachecker.cfa.ast.c;

import java.util.Iterator;

import com.google.common.collect.UnmodifiableIterator;

public class CAstStringIterable implements Iterable<String> {

  private final Iterable<? extends CAstNode> collection;

  public CAstStringIterable(Iterable<? extends CAstNode> pCollection) {
    collection = pCollection;
  }

  @Override
  public Iterator<String> iterator() {
    return new ASTStringIterator(collection);
  }

  private class ASTStringIterator extends UnmodifiableIterator<String> {

    private final Iterator<? extends CAstNode> subIterator;

    public ASTStringIterator(Iterable<? extends CAstNode> pCollection) {
      subIterator = pCollection.iterator();
    }

    @Override
    public boolean hasNext() {
      return subIterator.hasNext();
    }

    @Override
    public String next() {
      return subIterator.next().toASTString();
    }
  }
}
