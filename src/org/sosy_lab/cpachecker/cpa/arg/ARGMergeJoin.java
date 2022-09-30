// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class ARGMergeJoin implements MergeOperator {

  @Options(prefix = "cpa.arg")
  static class MergeOptions {

    MergeOptions(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }

    @Option(
        secure = true,
        description =
            "If this option is enabled, ARG states will also be merged if the first wrapped state "
                + "is subsumed by the second wrapped state (and the parents are not yet subsumed).")
    private boolean mergeOnWrappedSubsumption = false;

    @Option(
        secure = true,
        description =
            "What do to on a late merge, i.e., if the second parameter of the merge already has"
                + " child states (cf. issue #991):\n"
                + "- ALLOW: Just merge as usual.\n"
                + "- ALLOW_WARN: Log a warning the first time this happens, then ALLOW.\n"
                + "- PREVENT: Do not merge, i.e., enforce merge-sep for such situations.\n"
                + "- PREVENT_WARN: Log a warning the first time this happens, then PREVENT.\n"
                + "- CRASH: Crash CPAchecker as soon as this happens\n"
                + "  (useful for cases where a late merge should never happen).")
    // This field cannot be in ARGMergeJoin because we potentially write to it and changes must be
    // visible to later instances of ARGMergeJoin as well.
    private ARGMergeJoin.LateMergeHandling lateMerge = LateMergeHandling.ALLOW;
  }

  enum LateMergeHandling {
    ALLOW,
    ALLOW_WARN,
    PREVENT,
    PREVENT_WARN,
    CRASH;
  }

  private final MergeOperator wrappedMerge;
  private final AbstractDomain wrappedDomain;
  private final LogManager logger;
  private final MergeOptions options;

  public ARGMergeJoin(
      MergeOperator pWrappedMerge,
      AbstractDomain pWrappedDomain,
      LogManager pLogger,
      MergeOptions pOptions) {
    wrappedMerge = pWrappedMerge;
    wrappedDomain = pWrappedDomain;
    logger = checkNotNull(pLogger);
    options = checkNotNull(pOptions);
  }

  @Override
  public AbstractState merge(AbstractState pElement1, AbstractState pElement2, Precision pPrecision)
      throws CPAException, InterruptedException {

    ARGState argElement1 = (ARGState) pElement1;
    ARGState argElement2 = (ARGState) pElement2;

    checkArgument(!argElement1.isCovered(), "Trying to merge covered element %s", argElement1);
    checkArgument(
        argElement1.getChildren().isEmpty(),
        "First parameter %s of merge operator unexpectedly already has children",
        argElement1);

    if (!argElement2.mayCover()) {
      // elements that may not cover should also not be used for merge
      return pElement2;
    }

    if (argElement1.getMergedWith() != null) {
      // element was already merged into another element, don't try to widen argElement2
      // TODO In the optimal case (if all merge & stop operators as well as the reached set
      // partitioning fit well together)
      // this case shouldn't happen, but it does sometimes (at least with
      // ExplicitCPA+FeatureVarsCPA).
      return pElement2;
    }

    if (options.lateMerge == LateMergeHandling.PREVENT && !argElement2.getChildren().isEmpty()) {
      // shortcut, do not even call wrapped merge in this case
      return pElement2;
    }

    AbstractState wrappedState1 = argElement1.getWrappedState();
    AbstractState wrappedState2 = argElement2.getWrappedState();
    AbstractState retElement = wrappedMerge.merge(wrappedState1, wrappedState2, pPrecision);

    boolean continueMerge = !retElement.equals(wrappedState2);
    if (options.mergeOnWrappedSubsumption) {
      Set<ARGState> parents1 = ImmutableSet.copyOf(argElement1.getParents());
      Set<ARGState> parents2 = ImmutableSet.copyOf(argElement2.getParents());
      continueMerge =
          continueMerge
              || (!parents2.containsAll(parents1)
                  && wrappedDomain.isLessOrEqual(wrappedState1, wrappedState2));
    }
    if (!continueMerge) {
      return pElement2;
    }

    if (!argElement2.getChildren().isEmpty()) {
      // This is a late merge (cf. #991) were a merge was actually performed.
      switch (options.lateMerge) {
        case ALLOW_WARN:
          logger.log(
              Level.WARNING,
              getLateMergeMessage(argElement1, argElement2),
              "This is allowed but was configured to produce a warning because it is unexpected."
                  + " The merge will be performed as usual and no further cases will be logged. Cf."
                  + " issue #991 for more information about late merges.");
          options.lateMerge = LateMergeHandling.ALLOW;
          // $FALL-THROUGH$
        case ALLOW:
          break;
        case PREVENT_WARN:
          logger.log(
              Level.WARNING,
              getLateMergeMessage(argElement1, argElement2),
              "This is allowed but was configured to produce a warning because it is unexpected."
                  + " This merge and all other late merges will be prevented, but no further cases"
                  + " will be logged. Cf. issue #991 for more information about late merges.");
          options.lateMerge = LateMergeHandling.PREVENT;
          // $FALL-THROUGH$
        case PREVENT:
          return pElement2;
        case CRASH:
          throw new AssertionError(
              getLateMergeMessage(argElement1, argElement2)
                  + " This was configured to crash with option cpa.arg.lateMerge because it should"
                  + " not happen for this configuration. Either set this option to a different"
                  + " value or file a bug about this crash in the issue tracker. Cf. issue #991 for"
                  + " more information about late merges.");
        default:
          throw new AssertionError("missing switch case");
      }
    }

    ARGState mergedElement = new ARGState(retElement, null);

    // now replace argElement2 by mergedElement in ARG
    argElement2.replaceInARGWith(mergedElement);

    // and also replace argElement1 with it
    for (ARGState parentOfElement1 : argElement1.getParents()) {
      mergedElement.addParent(parentOfElement1);
    }

    // argElement1 is the current successor, it does not have any children yet and covered nodes yet
    assert argElement1.getChildren().isEmpty();
    assert argElement1.getCoveredByThis().isEmpty();

    // ARGElement1 will only be removed from ARG if stop(e1, reached) returns true.
    // So we can't actually remove it now, but we need to remember this later.
    argElement1.setMergedWith(mergedElement);
    return mergedElement;
  }

  private static String getLateMergeMessage(ARGState s1, ARGState s2) {
    @Nullable CFANode cfaNode = AbstractStates.extractLocation(s2);
    String location =
        cfaNode != null
            ? " at CFA Node " + cfaNode + " (" + cfaNode.describeFileLocation() + ")"
            : "";
    return "Late merge detected"
        + location
        + ": "
        + s1.toShortString()
        + " was merged into "
        + s2.toShortString()
        + ".";
  }
}
