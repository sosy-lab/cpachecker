/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options(prefix = "cpa.smg")
public class SMGOptions {

  @Option(
      secure = true,
      description = "with this option enabled, a check for unreachable memory occurs whenever a function returns, and not only at the end of the main function")
  private boolean checkForMemLeaksAtEveryFrameDrop = true;

  @Option(
      secure = true,
      description = "with this option enabled, memory that is not freed before the end of main is reported as memleak even if it is reachable from local variables in main")
  private boolean handleNonFreedMemoryInMainAsMemLeak = false;

  @Option(
      secure = true,
      name = "enableMallocFail",
      description = "If this Option is enabled, failure of malloc" + "is simulated")
  private boolean enableMallocFailure = true;

  @Option(
      secure = true,
      toUppercase = true,
      name = "handleUnknownFunctions",
      description = "Sets how unknown functions are handled.")
  private UnknownFunctionHandling handleUnknownFunctions = UnknownFunctionHandling.STRICT;
  public static enum UnknownFunctionHandling {STRICT, ASSUME_SAFE}

  @Option(
      secure = true,
      toUppercase = true,
      name = "GCCZeroLengthArray",
      description = "Enable GCC extension 'Arrays of Length Zero'.")
  private boolean GCCZeroLengthArray = false;

  @Option(
      secure = true,
      name = "guessSizeOfUnknownMemorySize",
      description = "Size of memory that cannot be calculated will be guessed.")
  private boolean guessSizeOfUnknownMemorySize = false;

  @Option(
      secure = true,
      name = "memoryAllocationFunctions",
      description = "Memory allocation functions")
  private ImmutableSet<String> memoryAllocationFunctions = ImmutableSet.of("malloc");

  @Option(
      secure = true,
      name = "memoryAllocationFunctionsSizeParameter",
      description = "Size parameter of memory allocation functions")
  private int memoryAllocationFunctionsSizeParameter = 0;

  @Option(
      secure = true,
      name = "arrayAllocationFunctions",
      description = "Array allocation functions")
  private ImmutableSet<String> arrayAllocationFunctions = ImmutableSet.of("calloc");

  @Option(
      secure = true,
      name = "memoryArrayAllocationFunctionsNumParameter",
      description = "Position of number of element parameter for array allocation functions")
  private int memoryArrayAllocationFunctionsNumParameter = 0;

  @Option(
      secure = true,
      name = "memoryArrayAllocationFunctionsElemSizeParameter",
      description = "Position of element size parameter for array allocation functions")
  private int memoryArrayAllocationFunctionsElemSizeParameter = 1;

  @Option(
      secure = true,
      name = "zeroingMemoryAllocation",
      description = "Allocation functions which set memory to zero")
  private ImmutableSet<String> zeroingMemoryAllocation = ImmutableSet.of("calloc");

  @Option(secure = true, name = "deallocationFunctions", description = "Deallocation functions")
  private ImmutableSet<String> deallocationFunctions = ImmutableSet.of("free");

  @Option(
      secure = true,
      name = "externalAllocationFunction",
      description = "Functions which indicate on external allocated memory")
  private ImmutableSet<String> externalAllocationFunction = ImmutableSet.of("ext_allocation");

  public SMGOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  public boolean isCheckForMemLeaksAtEveryFrameDrop() {
    return checkForMemLeaksAtEveryFrameDrop;
  }

  public boolean isHandleNonFreedMemoryInMainAsMemLeak() {
    return handleNonFreedMemoryInMainAsMemLeak;
  }

  public boolean isEnableMallocFailure() {
    return enableMallocFailure;
  }

  public UnknownFunctionHandling getHandleUnknownFunctions() {
    return handleUnknownFunctions;
  }

  public boolean isGCCZeroLengthArray() {
    return GCCZeroLengthArray;
  }

  public boolean isGuessSizeOfUnknownMemorySize() {
    return guessSizeOfUnknownMemorySize;
  }

  public ImmutableSet<String> getMemoryAllocationFunctions() {
    return memoryAllocationFunctions;
  }

  public int getMemoryAllocationFunctionsSizeParameter() {
    return memoryAllocationFunctionsSizeParameter;
  }

  public ImmutableSet<String> getArrayAllocationFunctions() {
    return arrayAllocationFunctions;
  }

  public int getMemoryArrayAllocationFunctionsNumParameter() {
    return memoryArrayAllocationFunctionsNumParameter;
  }

  public int getMemoryArrayAllocationFunctionsElemSizeParameter() {
    return memoryArrayAllocationFunctionsElemSizeParameter;
  }

  public ImmutableSet<String> getZeroingMemoryAllocation() {
    return zeroingMemoryAllocation;
  }

  public ImmutableSet<String> getDeallocationFunctions() {
    return deallocationFunctions;
  }

  public ImmutableSet<String> getExternalAllocationFunction() {
    return externalAllocationFunction;
  }
}