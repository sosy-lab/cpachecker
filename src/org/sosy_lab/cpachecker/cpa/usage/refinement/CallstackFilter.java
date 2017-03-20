/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage.refinement;

import static com.google.common.collect.FluentIterable.from;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.Pair;

/** This filter is used for filtering races by callstacks
 *  For instance, there can not be a race, if the two usages starts with the same interrupt handler -
 *  it can not be executed in parallel with itself.
 *  There also can be functions, which can be executed only in one thread.
 */

@Options(prefix="cpa.usagestatistics")
public class CallstackFilter extends GenericFilter<String> {

  @Option(name = "notSelfParallelFunctions", description = "The functions, which cannot be executed in parallel with themselves")
  protected Set<String> notSelfParallelFunctions = new HashSet<>();

  @Option(name = "singleThreadFunctions", description = "The functions, which are executed in one thread")
  protected Set<String> singleThreadFunctions = new HashSet<>();

  public CallstackFilter(ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> pWrapper,
      Configuration pConfig) throws InvalidConfigurationException {
    super(pWrapper, pConfig);
    pConfig.inject(this, CallstackFilter.class);
  }

  @Override
  protected Boolean filter(String pFirstPathCore, String pSecondPathCore) {
    if (notSelfParallelFunctions.contains(pFirstPathCore) && pFirstPathCore.equals(pSecondPathCore)) {
      return false;
    } else if (singleThreadFunctions.contains(pFirstPathCore) || singleThreadFunctions.contains(pSecondPathCore)) {
      return false;
    } else {
      return true;
    }
  }

  @Override
  protected String getPathCore(ExtendedARGPath pPath) {
    List<ARGState> firstCalls = from(pPath.getStateSet()).filter(isFirstCall).toList();
    List<String> callerFunctions = from(firstCalls).
        transform(getFunctionName).toList();

    //TODO Now I believe, it is enough to check the last function called from main - this is related to the call stack
    if (callerFunctions.size() >= 1) {
      return callerFunctions.get(callerFunctions.size() - 1);
    } else {
      //Usage in main
      return null;
    }
  }

}
