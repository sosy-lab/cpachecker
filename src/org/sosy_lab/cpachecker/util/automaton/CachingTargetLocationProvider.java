// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.automaton;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.specification.Specification;

public class CachingTargetLocationProvider implements TargetLocationProvider {

  private final TargetLocationProvider backingTargetLocationProvider;

  private final LoadingCache<CacheKey, ImmutableSet<CFANode>> cache =
      CacheBuilder.newBuilder()
          .weakKeys()
          .weakValues()
          .build(
              new CacheLoader<CacheKey, ImmutableSet<CFANode>>() {

                @Override
                public ImmutableSet<CFANode> load(CacheKey pCacheKey) {
                  return backingTargetLocationProvider.tryGetAutomatonTargetLocations(
                      pCacheKey.node, pCacheKey.specification);
                }
              });

  public CachingTargetLocationProvider(TargetLocationProvider pBackingTargetLocationProvider) {
    backingTargetLocationProvider = pBackingTargetLocationProvider;
  }

  public CachingTargetLocationProvider(
      ShutdownNotifier pShutdownNotifier, LogManager pLogManager, CFA pCfa) {
    this(new TargetLocationProviderImpl(pShutdownNotifier, pLogManager, pCfa));
  }

  @Override
  public ImmutableSet<CFANode> tryGetAutomatonTargetLocations(
      CFANode pRootNode, Specification specification) {
    return cache.getUnchecked(new CacheKey(pRootNode, specification));
  }

  private static class CacheKey {

    private final CFANode node;

    private final Specification specification;

    public CacheKey(CFANode pNode, Specification pSpecification) {
      node = pNode;
      specification = pSpecification;
    }

    @Override
    public String toString() {
      return node + ": " + specification;
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, specification);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof CacheKey) {
        CacheKey other = (CacheKey) pObj;
        return node.equals(other.node) && specification.equals(other.specification);
      }
      return false;
    }
  }
}
