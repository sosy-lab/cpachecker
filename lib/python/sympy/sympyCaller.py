#!/usr/bin/python

import sys

# Fix for Python 3 (where "execfile" was removed)
# Taken from http://code.activestate.com/recipes/577942-execfile-is-back-backwards-compatibility-part-5/
if sys.version[0] == "3":
  def execfile(STRING_some_path_and_file, globals={}, locals={}):
    exec(open(STRING_some_path_and_file).read()) in globals, locals

if __name__ == "__main__":
  tmpFilename = sys.stdin.read()
  execfile(tmpFilename)

