// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.CfaPostProcessor;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;

/**
 * A factory that creates new {@link CFA} instances for CFAs represented by ({@link CfaNetwork},
 * {@link CfaMetadata}) pairs.
 *
 * <p>A factory can create multiple CFAs. Every time the {@link CfaFactory#createCfa(CfaNetwork,
 * CfaMetadata, LogManager, ShutdownNotifier) createCfa} method is invoked, a new {@link CFA}
 * instance is created.
 *
 * <p>{@link CfaFactory} implementations may run {@link CfaPostProcessor CFA post-processors} during
 * CFA creation.
 */
public interface CfaFactory {

  /**
   * Returns a new CFA for the specified {@link CfaNetwork} and {@link CfaMetadata}.
   *
   * <p>Every time this method is invoked, a new {@link CFA} instance is created.
   *
   * <p>The returned CFA resembles the CFA represented by the {@link CfaNetwork} as closely as
   * possible. {@link CfaFactory} implementations may run {@link CfaPostProcessor CFA
   * post-processors} during CFA creation, which may lead to differences between the specified
   * {@link CfaNetwork} and returned {@link CFA}.
   *
   * @param pCfaNetwork the {@link CfaNetwork} to create a CFA for
   * @param pCfaMetadata the {@link CfaMetadata} of the specified {@link CfaNetwork}
   * @param pLogger the logger to use during CFA creation
   * @param pShutdownNotifier the shutdown notifier to use during CFA creation
   * @return a new CFA for the specified {@link CfaNetwork} and {@link CfaMetadata}
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if the specified {@link CfaNetwork} doesn't represent a valid
   *     CFA
   * @throws IllegalArgumentException if the specified {@link CfaMetadata} doesn't fit the CFA
   *     represented by the {@link CfaNetwork} (e.g., if the {@link
   *     CfaMetadata#getMainFunctionEntry() function entry node} doesn't exist in the {@link
   *     CfaNetwork})
   */
  CFA createCfa(
      CfaNetwork pCfaNetwork,
      CfaMetadata pCfaMetadata,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier);
}
