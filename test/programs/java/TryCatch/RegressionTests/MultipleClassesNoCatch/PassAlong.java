// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class PassAlong {

    private EnterException e = null;
    private Boolean entered = false;

    public PassAlong(){
        e = new EnterException();
    }

    public void passAlong() {
        e.throwException();
        //this next line should never be called
        assert entered;
    }
}
