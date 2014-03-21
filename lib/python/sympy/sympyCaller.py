#!/usr/bin/python

import sys

if __name__ == "__main__":
  tmpFilename = sys.stdin.read()
  execfile(tmpFilename)

