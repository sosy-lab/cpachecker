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
package org.sosy_lab.cpachecker.util.automaton;

import java.util.Objects;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;

public class CachingTargetLocationProvider implements TargetLocationProvider {

  private final TargetLocationProvider backingTargetLocationProvider;

  private final LoadingCache<CacheKey, ImmutableSet<CFANode>> cache =
      CacheBuilder.newBuilder()
          .weakKeys()
          .weakValues()
          .<CacheKey, ImmutableSet<CFANode>>build(
              new CacheLoader<CacheKey, ImmutableSet<CFANode>>() {

                @Override
                public ImmutableSet<CFANode> load(CacheKey pCacheKey) {
                  return backingTargetLocationProvider.tryGetAutomatonTargetLocations(
                      pCacheKey.node, pCacheKey.automaton);
                }
              });

  public CachingTargetLocationProvider(TargetLocationProvider pBackingTargetLocationProvider) {
    this.backingTargetLocationProvider = pBackingTargetLocationProvider;
  }

  public CachingTargetLocationProvider(ReachedSetFactory pReachedSetFactory, ShutdownNotifier pShutdownNotifier,
      LogManager pLogManager, Configuration pConfig, CFA pCfa) {
    this(new TargetLocationProviderImpl(pReachedSetFactory, pShutdownNotifier, pLogManager, pConfig, pCfa));
  }

  @Override
  public ImmutableSet<CFANode> tryGetAutomatonTargetLocations(CFANode pRootNode) {
    return tryGetAutomatonTargetLocations(pRootNode, Optional.<Automaton>absent());
  }

  @Override
  public ImmutableSet<CFANode> tryGetAutomatonTargetLocations(
      CFANode pRootNode, Optional<Automaton> pAutomaton) {
    return cache.getUnchecked(new CacheKey(pRootNode, pAutomaton));
  }

  private static class CacheKey {

    private final CFANode node;

    private final Optional<Automaton> automaton;

    public CacheKey(CFANode pNode, Optional<Automaton> pAutomaton) {
      node = pNode;
      automaton = pAutomaton;
    }

    @Override
    public String toString() {
      return node + ": " + automaton;
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, automaton);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof CacheKey) {
        CacheKey other = (CacheKey) pObj;
        return node.equals(other.node) && automaton.equals(other.automaton);
      }
      return false;
    }

  }

}
