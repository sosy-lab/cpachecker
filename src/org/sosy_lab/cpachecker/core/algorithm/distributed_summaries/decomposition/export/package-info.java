// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package contains classes for exporting block nodes in the DSS framework to YML files. The
 * format is heavily inspired by software verification witnesses 2.0. The exported data can be used
 * to export C code for the specific blocks with an external tool and is manily used in
 * multiprocessing DSS.
 */
package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.export;
