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
package org.sosy_lab.cpachecker.cpa.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class CacheTransferRelation implements TransferRelation {

  private final TransferRelation mCachedTransferRelation;
  //private Map<CFAEdge, Map<AbstractElement, Map<Precision, Collection<? extends AbstractElement>>>> mSuccessorsCache;
  private Map<Precision, Map<CFAEdge, Map<AbstractElement, Collection<? extends AbstractElement>>>> mSuccessorsCache;

  private int lCacheMisses = 0;
  private int lCacheHits = 0;


  //private Set<CFAEdge> mHitEdges;


  public CacheTransferRelation(TransferRelation pCachedTransferRelation) {
    mCachedTransferRelation = pCachedTransferRelation;
    //mSuccessorsCache = new HashMap<CFAEdge, Map<AbstractElement, Map<Precision, Collection<? extends AbstractElement>>>>();
    mSuccessorsCache = new HashMap<Precision, Map<CFAEdge, Map<AbstractElement, Collection<? extends AbstractElement>>>>();

    //mHitEdges = new HashSet<CFAEdge>();
  }

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    /*if (pCfaEdge.getPredecessor().getNodeNumber() == 1) {
      System.out.println("##########################");
      System.out.println(pElement);
      System.out.println(pPrecision);
    }*/

    /*Map<AbstractElement, Map<Precision, Collection<? extends AbstractElement>>> lLevel1Cache = mSuccessorsCache.get(pCfaEdge);

    if (lLevel1Cache == null) {
      lLevel1Cache = new HashMap<AbstractElement, Map<Precision, Collection<? extends AbstractElement>>>();
      mSuccessorsCache.put(pCfaEdge, lLevel1Cache);
    }

    Map<Precision, Collection<? extends AbstractElement>> lLevel2Cache = lLevel1Cache.get(pElement);

    if (lLevel2Cache == null) {
      lLevel2Cache = new HashMap<Precision, Collection<? extends AbstractElement>>();
      lLevel1Cache.put(pElement, lLevel2Cache);
    }

    Collection<? extends AbstractElement> lSuccessors = lLevel2Cache.get(pPrecision);

    if (lSuccessors == null) {
      lSuccessors = mCachedTransferRelation.getAbstractSuccessors(pElement, pPrecision, pCfaEdge);
      lLevel2Cache.put(pPrecision, lSuccessors);

      lCacheMisses++;
    }
    else {
      lCacheHits++;
    }

    if ((lCacheMisses + lCacheHits) % 100 == 0 ) {
      System.out.println("Misses: " + lCacheMisses + ", hits: " + lCacheHits + ", sum: " + (lCacheMisses + lCacheHits));
    }

    return lSuccessors;*/

    Map<CFAEdge, Map<AbstractElement, Collection<? extends AbstractElement>>> lLevel1Cache = mSuccessorsCache.get(pPrecision);

    if (lLevel1Cache == null) {
      lLevel1Cache = new HashMap<CFAEdge, Map<AbstractElement, Collection<? extends AbstractElement>>>();
      mSuccessorsCache.put(pPrecision, lLevel1Cache);
    }

    Map<AbstractElement, Collection<? extends AbstractElement>> lLevel2Cache = lLevel1Cache.get(pCfaEdge);

    if (lLevel2Cache == null) {
      lLevel2Cache = new HashMap<AbstractElement, Collection<? extends AbstractElement>>();
      lLevel1Cache.put(pCfaEdge, lLevel2Cache);
    }

    Collection<? extends AbstractElement> lSuccessors = lLevel2Cache.get(pElement);

    if (lSuccessors == null) {
      lSuccessors = mCachedTransferRelation.getAbstractSuccessors(pElement, pPrecision, pCfaEdge);
      lLevel2Cache.put(pElement, lSuccessors);

      lCacheMisses++;
    }
    else {
      lCacheHits++;
    }

    if ((lCacheMisses + lCacheHits) % 100 == 0 ) {
      System.out.println("Misses: " + lCacheMisses + ", hits: " + lCacheHits + ", sum: " + (lCacheMisses + lCacheHits));
    }

    return lSuccessors;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {

    // TODO implement caching

    return mCachedTransferRelation.strengthen(pElement, pOtherElements, pCfaEdge, pPrecision);
  }

}
