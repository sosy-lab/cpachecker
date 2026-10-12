// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

/** Specifier for Points-to-Edges (pointers). */
public enum SMGTargetSpecifier {
  /**
   * This specifier symbolises the Points-to-Edge towards a concrete memory region. The
   * nesting-level is equal to the target object.
   */
  IS_REGION {
    @Override
    public boolean isRegion() {
      return true;
    }

    @Override
    public String toString() {
      return "reg";
    }
  },
  /**
   * This specifier symbolises the Points-to-Edge towards the FIRST list element of an abstracted
   * linked-list. The nesting-level is equal to the target object.
   */
  IS_FIRST_POINTER {
    @Override
    public boolean isFirst() {
      return true;
    }

    @Override
    public String toString() {
      return "fst";
    }
  },
  /**
   * This specifier symbolises the Points-to-Edge towards the LAST list element of an abstracted
   * linked-list. The nesting-level is equal to the target object.
   */
  IS_LAST_POINTER {
    @Override
    public boolean isLast() {
      return true;
    }

    @Override
    public String toString() {
      return "lst";
    }
  },
  /**
   * This specifier symbolises that the Points-to-Edge refers to each memory of the abstracted
   * object. These PTEs are materialized such that there is a PTE towards ALL list segments
   * materialized. Nesting-Level of the Points-to-Edge P is 1 smaller than the target object
   * o(P(a)).
   *
   * <p>Page 8, 10.1007/978-3-642-38856-9_13:
   *
   * <p>"On the other hand, addresses with the all target go up one level in the nesting hierarchy,
   * i.e., ∀a ∈ A : tg(P(a)) == all ⇒ level(a) == level(o(P(a))) + 1."
   */
  IS_ALL_POINTER {
    @Override
    public boolean isAll() {
      return true;
    }

    @Override
    public String toString() {
      return "all";
    }
  };

  public boolean isRegion() {
    return false;
  }

  public boolean isAll() {
    return false;
  }

  public boolean isFirst() {
    return false;
  }

  public boolean isLast() {
    return false;
  }
}
