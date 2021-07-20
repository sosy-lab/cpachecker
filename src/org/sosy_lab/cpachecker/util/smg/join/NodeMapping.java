// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class NodeMapping {

  private final Map<SMGObject, SMGObject> objectMap = new HashMap<>();
  private final Map<SMGValue, SMGValue> valueMap = new HashMap<>();
}
