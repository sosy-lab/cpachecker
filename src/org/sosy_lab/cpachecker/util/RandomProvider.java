/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.util;

import com.google.common.base.Preconditions;
import java.util.Random;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options(prefix="random")
public class RandomProvider {

  private static RandomProvider provider;

  @Option(name="seed", description="Random seed to use. Uses a random number if null is given",
      secure=true)
  private Long randomSeed = null;

  private Random random;

  private RandomProvider(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    if (randomSeed == null){
      random = new Random();
    } else {
      random = new Random(randomSeed);
    }
  }

  private Random getRandom() {
    return random;
  }

  public static void initialize(Configuration pConfig) throws InvalidConfigurationException {
    Preconditions.checkState(provider == null);
    provider = new RandomProvider(pConfig);
  }

  /** This method allows initialization more than once. For use in tests only! */
  public static void initializeForTesting(Configuration pConfig)
      throws InvalidConfigurationException {
    provider = new RandomProvider(pConfig);
  }

  public static Random get() {
    Preconditions.checkState(provider != null,
        RandomProvider.class.getSimpleName() + " not initialized");
    return provider.getRandom();
  }
}
