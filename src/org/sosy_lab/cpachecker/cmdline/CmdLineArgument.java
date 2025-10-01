// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cmdline;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cmdline.CmdLineArguments.putIfNotExistent;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.checkerframework.checker.nullness.qual.Nullable;

abstract class CmdLineArgument implements Comparable<CmdLineArgument> {

  private final ImmutableSet<String> names;
  private String description = ""; // changed later, if needed
  private @Nullable String replacementInfo;

  CmdLineArgument(String... pNames) {
    names = ImmutableSet.copyOf(pNames);
  }

  @CanIgnoreReturnValue
  CmdLineArgument withDescription(String pDescription) {
    description = pDescription;
    return this;
  }

  @CanIgnoreReturnValue
  CmdLineArgument withReplacementInfo(String pDeprecationNotice) {
    replacementInfo = pDeprecationNotice;
    return this;
  }

  String getMainName() {
    return names.asList().get(0);
  }

  @Override
  public int compareTo(CmdLineArgument other) {
    return names.toString().compareTo(other.names.toString());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o instanceof CmdLineArgument cmdLineArgument && names.equals(cmdLineArgument.names);
  }

  @Override
  public int hashCode() {
    return names.hashCode();
  }

  @Override
  public String toString() {
    String s =
        from(names).filter(arg -> !CmdLineArguments.isOldStyleArgument(arg)).join(Joiner.on("/"));
    if (description.isEmpty()) {
      return s;
    } else {
      // we rjust the description at column 20.
      return String.format("%1$-20s %2$s", s, description);
    }
  }

  boolean apply(Map<String, String> properties, String currentArg, Iterator<String> argsIt)
      throws InvalidCmdlineArgumentException {
    if (names.contains(currentArg)) {
      if (replacementInfo != null) {
        Output.warning(
            "The command-line argument '%s' is deprecated. It can be replaced by %s.",
            currentArg, replacementInfo);
      }
      apply0(properties, currentArg, argsIt);
      return true;
    }
    return false;
  }

  abstract void apply0(Map<String, String> properties, String currentArg, Iterator<String> argsIt)
      throws InvalidCmdlineArgumentException;

  /** The arg is a short replacement for an option with 'one' value given as next argument. */
  static class CmdLineArgument1 extends CmdLineArgument {

    private String option;

    CmdLineArgument1(String... pNames) {
      super(pNames);
    }

    CmdLineArgument1 settingOption(String pOption) {
      option = pOption;
      return this;
    }

    @Override
    final void apply0(Map<String, String> properties, String currentArg, Iterator<String> args)
        throws InvalidCmdlineArgumentException {
      if (args.hasNext()) {
        handleArg(properties, currentArg, args.next());
      } else {
        throw new InvalidCmdlineArgumentException(currentArg + " argument missing.");
      }
    }

    /**
     * Handles a command-line argument.
     *
     * @param pProperties the map of configuration properties.
     * @param pCurrentArg the command-line argument that is currently being handled
     * @param pArgValue the value of the configuration option represented by this argument.
     */
    void handleArg(Map<String, String> pProperties, String pCurrentArg, String pArgValue)
        throws InvalidCmdlineArgumentException {
      checkState(option != null);
      putIfNotExistent(pProperties, option, pArgValue);
    }
  }

  /** This is a command-line argument that sets some properties to fixed values. */
  static class PropertyAddingCmdLineArgument extends CmdLineArgument {

    private final Map<String, String> additionalIfNotExistentArgs = new HashMap<>();
    private final Map<String, String> additionalArgs = new HashMap<>();

    PropertyAddingCmdLineArgument(String... pNames) {
      super(pNames);
    }

    @CanIgnoreReturnValue
    PropertyAddingCmdLineArgument settingProperty(String pName, String pValue) {
      additionalIfNotExistentArgs.put(pName, pValue);
      return this;
    }

    @CanIgnoreReturnValue
    PropertyAddingCmdLineArgument overridingProperty(String pName, String pValue) {
      additionalArgs.put(pName, pValue);
      return this;
    }

    @Override
    void apply0(Map<String, String> properties, String currentArg, Iterator<String> args)
        throws InvalidCmdlineArgumentException {
      for (Entry<String, String> e : additionalIfNotExistentArgs.entrySet()) {
        putIfNotExistent(properties, e.getKey(), e.getValue());
      }
      for (Entry<String, String> e : additionalArgs.entrySet()) {
        properties.put(e.getKey(), e.getValue());
      }
    }
  }
}
