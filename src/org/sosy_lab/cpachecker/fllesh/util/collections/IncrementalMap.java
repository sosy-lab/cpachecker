package org.sosy_lab.cpachecker.fllesh.util.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IncrementalMap<K, V> implements Map<K, V> {

  private Map<K, V> mSubmap;
  private Map<K, V> mMap;
  
  public IncrementalMap() {
    mSubmap = Collections.emptyMap();
    
  }
  
  public IncrementalMap(Map<K, V> pSubmap) {
    mSubmap = pSubmap;
    // TODO: can we make this more generic?
    mMap = new HashMap<K, V>();
  }
  
  @Override
  public int size() {
    return mSubmap.size() + mMap.size();
  }

  @Override
  public boolean isEmpty() {
    return mSubmap.isEmpty() && mMap.isEmpty();
  }

  @Override
  public boolean containsKey(Object pKey) {
    return mMap.containsKey(pKey) || mSubmap.containsKey(pKey);
  }

  @Override
  public boolean containsValue(Object pValue) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public V get(Object pKey) {
    if (mMap.containsKey(pKey)) {
      return mMap.get(pKey);
    }
    else {
      return mSubmap.get(pKey);
    }
  }

  @Override
  public V put(K pKey, V pValue) {
    if (mMap.containsKey(pKey)) {
      return mMap.put(pKey, pValue);
    }
    else if (mSubmap.containsKey(pKey)) {
      V lPreviousValue = mSubmap.get(pKey);
      mMap.put(pKey, pValue);
      return lPreviousValue;
    }
    else {
      mMap.put(pKey, pValue);
      return null;
    }
  }

  @Override
  public V remove(Object pKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> pM) {
    for (Entry<? extends K, ? extends V> lEntry : pM.entrySet()) {
      put(lEntry.getKey(), lEntry.getValue());
    }
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<K> keySet() {
    Set<K> lKeys = new HashSet<K>();
    lKeys.addAll(mSubmap.keySet());
    lKeys.addAll(mMap.keySet());
    
    return lKeys;
  }

  @Override
  public Collection<V> values() {
    Collection<V> lValues = new HashSet<V>();
    
    for (K lKey : keySet()) {
      lValues.add(get(lKey));
    }
    
    return lValues;
  }

  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    // TODO Auto-generated method stub
    return null;
  }

}
