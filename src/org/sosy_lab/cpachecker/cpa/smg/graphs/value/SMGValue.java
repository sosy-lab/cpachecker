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
package org.sosy_lab.cpachecker.cpa.smg.graphs.value;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

/**
 * SMGs consists of two types of nodes: {@link SMGObject}s and {@link SMGValue}s. {@link SMGValue}s
 * represent addresses or data stored in {@link SMGObject}s. All values are abstract, such that we
 * only know whether they are equal or not. The only exception is the value 0 that is used to
 * represent 0 in all possible types as well as the address of the {@link SMGNullObject}.
 */
public interface SMGValue {

  public boolean isUnknown();

  public BigInteger getValue();

  public int getAsInt();

  public long getAsLong();
}
