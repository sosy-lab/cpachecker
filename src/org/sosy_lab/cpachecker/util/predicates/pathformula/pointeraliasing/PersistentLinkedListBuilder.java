/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import org.sosy_lab.common.collect.PersistentLinkedList;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class PersistentLinkedListBuilder<T> {
  public void add(final T e) {
    list = list.with(e);
  }

  public PersistentLinkedList<T> build() {
    return list;
  }

  public static <T> Collector<T, ?, PersistentLinkedList<T>> toPersistentLinkedList() {
    return new Collector<T, PersistentLinkedListBuilder<T>, PersistentLinkedList<T>>() {

      @Override
      public Supplier<PersistentLinkedListBuilder<T>> supplier() {
        return PersistentLinkedListBuilder::new;
      }

      @Override
      public BiConsumer<PersistentLinkedListBuilder<T>, T> accumulator() {
        return PersistentLinkedListBuilder::add;
      }

      @Override
      public BinaryOperator<PersistentLinkedListBuilder<T>> combiner() {
        return (_a1, _a2) -> { throw new UnsupportedOperationException("Should be used sequentially"); };
      }

      @Override
      public java.util.function.Function<PersistentLinkedListBuilder<T>, PersistentLinkedList<T>> finisher() {
        return PersistentLinkedListBuilder::build;
      }

      @Override
      public Set<Characteristics> characteristics() {
        return EnumSet.noneOf(Characteristics.class);
      }
    };
  }

  private PersistentLinkedList<T> list = PersistentLinkedList.of();
}