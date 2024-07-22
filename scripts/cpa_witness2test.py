#!/usr/bin/env python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import importlib.machinery
import importlib.util
import os

# We want to delegate completely to "../bin/cpa-witness2test".
# In Bash this would be "exec ../bin/cpa-witness2test",
# in Python it is more complicated.
# The following solution is based on https://stackoverflow.com/a/19011259/396730.
# We import the target file using some low-level code
# that allows us to import a module from an arbitrary file (without .py suffix)
# and to set __name__ inside the imported module
# to the same value as in the current module.
target_file = os.path.join(os.path.dirname(__file__), "..", "bin", "cpa-witness2test")
loader = importlib.machinery.SourceFileLoader(__name__, target_file)
spec = importlib.util.spec_from_loader(loader.name, loader)
module = importlib.util.module_from_spec(spec)

# The next line performs the actual import and executes the module-level code
# inside the target. If __name__ is "__main__", this will also run the actual code.
loader.exec_module(module)

# We end up here if the called script did not throw an exception.
# This is either because it terminated regularly, or because someone imported us
# and __name__ is not equal to "__main__".
# In order to provide as much backward compatibility as possible,
# we simulate a "from cpa-witness2test import *" with the following statement,
# such that an importer of the current script will find everything as expected.
globals().update(
    {name: value for name, value in module.__dict__.items() if not name.startswith("_")}
)
