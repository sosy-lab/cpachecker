// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import com.google.common.base.Joiner;
import com.google.errorprone.annotations.Immutable;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@Immutable(containerOf = {"K", "V"})
abstract class AbstractImmutableMap<K, V> implements Map<K, V> {

  @Deprecated
  @Override
  public final void clear() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public final V put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public final V putIfAbsent(K pKey, V pValue) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public final void putAll(Map<? extends K, ? extends V> pM) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public final V remove(Object pKey) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public final boolean remove(Object pKey, Object pValue) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public final V replace(K pKey, V pValue) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public final boolean replace(K pKey, V pOldValue, V pNewValue) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public final V compute(K pKey, BiFunction<? super K, ? super V, ? extends V> pRemappingFunction) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public final V computeIfAbsent(K pKey, Function<? super K, ? extends V> pMappingFunction) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public final V computeIfPresent(
      K pKey, BiFunction<? super K, ? super V, ? extends V> pRemappingFunction) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public final V merge(
      K pKey, V pValue, BiFunction<? super V, ? super V, ? extends V> pRemappingFunction) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public final void replaceAll(BiFunction<? super K, ? super V, ? extends V> pFunction) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsValue(Object pValue) {
    return values().contains(pValue);
  }

  @Override
  public Collection<V> values() {
    return new MapValues<>(this);
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return "{}";
    }
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    Joiner.on(", ").withKeyValueSeparator("=").useForNull("null").appendTo(sb, this);
    sb.append('}');
    return sb.toString();
  }
}
