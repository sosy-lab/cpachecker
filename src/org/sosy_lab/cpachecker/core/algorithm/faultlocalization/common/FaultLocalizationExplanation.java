package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common;

import java.util.Set;

@FunctionalInterface
public interface FaultLocalizationExplanation {

  String explanationFor(Set<? extends FaultLocalizationOutput> subset);
}
