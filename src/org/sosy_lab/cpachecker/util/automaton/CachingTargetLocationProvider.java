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

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;

public class CachingTargetLocationProvider implements TargetLocationProvider {

  private final TargetLocationProvider backingTargetLocationProvider;

  private final LoadingCache<CFANode, ImmutableSet<CFANode>> cache =
      CacheBuilder.newBuilder()
          .weakKeys()
          .weakValues()
          .<CFANode, ImmutableSet<CFANode>>build(
              new CacheLoader<CFANode, ImmutableSet<CFANode>>() {

                @Override
                public ImmutableSet<CFANode> load(CFANode pRootNode) {
                  return backingTargetLocationProvider.tryGetAutomatonTargetLocations(pRootNode);
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
    return cache.getUnchecked(pRootNode);
  }

}
