// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs;

import java.util.Optional;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.smg.CLangStackFrame;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGMemoryPath;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentStack;

/**
 * A view on a CLangSMG, where no modifications are allowed.
 *
 * <p>All returned Collections are unmodifiable.
 */
public interface UnmodifiableCLangSMG extends UnmodifiableSMG {

  @Override
  CLangSMG copyOf();

  SMGRegion getObjectForVisibleVariable(String pVariableName);

  /**
   * Returns the (unmodifiable) stack of frames containing objects. The frames are ordered from
   * bottom (main function) to top (most local function call).
   */
  PersistentStack<CLangStackFrame> getStackFrames();

  /** return a unmodifiable view on all SMG-objects on the heap. */
  PersistentSet<SMGObject> getHeapObjects();

  /** check whether an object is part of the heap. */
  boolean isHeapObject(SMGObject object);

  PersistentMap<String, SMGRegion> getGlobalObjects();

  /**
   * return the FunctionReturn-object for the most recent function call, i.e., from the top-level
   * stackframe.
   */
  SMGObject getFunctionReturnObject();

  Optional<SMGEdgeHasValue> getHVEdgeFromMemoryLocation(SMGMemoryPath pLocation);
}
