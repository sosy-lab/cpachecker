/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.mav;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.core.defaults.AdjustablePrecision;

/**
 * Utility class for representation of specification.
 * This class is designed for Multi-Aspect Verification
 * (options analysis.stopAfterError = false, analysis.multiAspectVerification = true).
 * Each specification differs by its unique id.
 */
public class RuleSpecification {
  private final SpecificationKey specificationKey;
  private Long cpuTime; // in ms

  /**
   * Corresponding precisions, that were added when this specification was checking.
   * Key is the class name of this precision.
   * All precisions, that will be used, must implement AdjustablePrecision
   * (for adding/subtracting).
   */
  private Map<Class<? extends AdjustablePrecision>, AdjustablePrecision> precisions;

  /**
   * Possible verdicts for each specification:
   * CHECKING - specification is checking (initial);
   * UNSAFE - error trace (counterexample) was found for this RS;
   * UNKNOWN - specification has exhausted its internal resources;
   * RECHECK - specification has exhausted its internal resources
   * (probably because of other specification and should be rechecked);
   * SAFE - analysis has been completed for this specification without finding any error traces
   * or exhausting internal resources.
   */
  public enum SpecificationStatus {CHECKING, UNSAFE, UNKNOWN, RECHECK, SAFE}
  private SpecificationStatus specificationStatus;


  public RuleSpecification (SpecificationKey specificationKey)
  {
    this.specificationKey = specificationKey;
    cpuTime = 0L;
    specificationStatus = SpecificationStatus.CHECKING;
    precisions = new HashMap<>();
  }

  public void addPrecision(AdjustablePrecision otherPrecision) {
    Class <? extends AdjustablePrecision> type = otherPrecision.getClass();
    if (precisions.containsKey(type))
    {
      precisions.put(type, precisions.get(type).add(otherPrecision));
    }
    else
    {
      precisions.put(type, otherPrecision);
    }
  }

  public AdjustablePrecision getPrecision(Class<? extends AdjustablePrecision> pPrecisionType) {
    return precisions.get(pPrecisionType);
  }

  public Set<Class<? extends AdjustablePrecision>> getPrecisionTypes() {
    return precisions.keySet();
  }

  public void setStatus(SpecificationStatus status) {
    // UNSAFE is final verdict (it can't be changed to UNKNOWN).
    if (specificationStatus != SpecificationStatus.UNSAFE) {
      specificationStatus = status;
    }
  }

  public SpecificationStatus getStatus() {
    return specificationStatus;
  }

  public SpecificationKey getSpecificationKey() {
    return specificationKey;
  }

  public void addCpuTime(Long addingTime) {
    cpuTime += addingTime;
  }

  public Long getCpuTime() {
    return cpuTime;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((specificationKey == null) ? 0 : specificationKey.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    RuleSpecification other = (RuleSpecification) obj;
    if (specificationKey == null) {
      if (other.specificationKey != null) {
        return false;
      }
    } else if (!specificationKey.equals(other.specificationKey)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "[specification=" + specificationKey + ", time=" + cpuTime +
        ", status=" + specificationStatus + "]";
  }

}