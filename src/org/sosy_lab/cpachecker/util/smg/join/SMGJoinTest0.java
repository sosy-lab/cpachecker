// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import org.sosy_lab.cpachecker.util.smg.test.SMGTest0;

class SMGJoinTest0 extends SMGTest0 {

  protected NodeMapping cloneMapping(NodeMapping oldMapping) {
    NodeMapping copyMapping = new NodeMapping();
    oldMapping.getValueMap().forEach((key, value) -> copyMapping.addMapping(key, value));
    oldMapping.getObjectMap().forEach((key, value) -> copyMapping.addMapping(key, value));
    return copyMapping;
  }
}
