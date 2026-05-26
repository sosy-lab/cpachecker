// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.automaton;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.specification.Specification;

public class CachingTargetLocationProvider implements TargetLocationProvider {

  private final TargetLocationProvider backingTargetLocationProvider;

  private final LoadingCache<CacheKey, ImmutableSet<CFANode>> cache =
      CacheBuilder.newBuilder()
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

  private record CacheKey(CFANode node, Specification specification) {

    CacheKey {
      checkNotNull(node);
      checkNotNull(specification);
    }
  }
}
