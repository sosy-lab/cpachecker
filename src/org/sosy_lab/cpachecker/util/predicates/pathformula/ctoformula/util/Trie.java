/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.util;

import java.util.HashMap;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;


public class Trie<K, V> {

  public Trie() {
    this.value = null;
  }

  public void add(final List<K> path, final V value) {
    add(FluentIterable.from(path), value);
  }

  public V get(final List<K> path) {
    return get(FluentIterable.from(path));
  }

  private void add(final FluentIterable<K> path, final V value) {
    if (path.isEmpty()) {
      Preconditions.checkState(this.value  == null, "Trie key collision");
      this.value = value;
    } else {
      final K key = path.first().get();
      Trie<K, V> child = children.get(key);
      if (child == null) {
        child = new Trie<>();
        children.put(key, child);
      }
      child.add(path.skip(1), value);
    }
  }

  private V get(final FluentIterable<K> path) {
    if (path.isEmpty()) {
      return value;
    } else {
      final Trie<K, V> child = children.get(path.first().get());
      return child != null ? child.get(path.skip(1)) : null;
    }
  }

  private final HashMap<K, Trie<K, V>> children = new HashMap<>();
  private V value;
}
