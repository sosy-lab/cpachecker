"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2013  Dirk Beyer
All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


CPAchecker web page:
  http://cpachecker.sosy-lab.org
"""

from __future__ import absolute_import, unicode_literals

import benchmark.util as util

STR_SAFE = 'safe'
STR_UNSAFE = 'unsafe'
STR_UNKNOWN = 'unknown'
STR_PROP_DEREF = 'false(valid-deref)'
STR_PROP_FREE = 'false(valid-free)'
STR_PROP_MEMTRACK = 'false(valid-memtrack)'

# string searched in filenames to determine correct or incorrect status.
# use lower case! the dict contains assignments 'filename' --> 'status'
BUG_SUBSTRINGS = {'_false-valid-deref':    STR_PROP_DEREF,
                  '_false-valid-free':     STR_PROP_FREE,
                  '_false-valid-memtrack': STR_PROP_MEMTRACK,
                  '_unsafe':               STR_UNSAFE, # deprecated, maybe removed soon
                  '_false':                STR_UNSAFE
                  }

SAFE_SUBSTRINGS ={'_safe':                 STR_SAFE, # deprecated, maybe removed soon
                  '_true':                 STR_SAFE
                  }

# Score values taken from http://sv-comp.sosy-lab.org/
SCORE_CORRECT_SAFE = 2
SCORE_CORRECT_UNSAFE = 1
SCORE_UNKNOWN = 0
SCORE_WRONG_UNSAFE = -4
SCORE_WRONG_SAFE = -8

CATEGORY_UNKNOWN = ('', )

RESULT_CORRECT_SAFE = ('correct', STR_SAFE)
RESULT_CORRECT_UNSAFE = ('correct', STR_UNSAFE)
RESULT_CORRECT_PROP_DEREF = ('correct', STR_PROP_DEREF)
RESULT_CORRECT_PROP_FREE = ('correct', STR_PROP_FREE)
RESULT_CORRECT_PROP_MEMTRACK = ('correct', STR_PROP_MEMTRACK)

RESULT_UNKNOWN = ('unknown', )
RESULT_ERROR = ('error', )

RESULT_WRONG_UNSAFE = ('wrong', STR_UNSAFE)
RESULT_WRONG_SAFE = ('wrong', STR_SAFE)
RESULT_WRONG_PROP_DEREF = ('wrong', STR_PROP_DEREF)
RESULT_WRONG_PROP_FREE = ('wrong', STR_PROP_FREE)
RESULT_WRONG_PROP_MEMTRACK = ('wrong', STR_PROP_MEMTRACK)


def statusOfFile(filename):
    """
    This function returns the status of a file, 
    this is the property in the filename.
    """
    for key in BUG_SUBSTRINGS:
        if key in filename.lower():
            return BUG_SUBSTRINGS[key]
    return STR_SAFE # if no hint is given, assume SAFE


def fileIsUnsafe(filename):
    return util.containsAny(filename, BUG_SUBSTRINGS.keys())

def fileIsSafe(filename):
    return util.containsAny(filename, SAFE_SUBSTRINGS.keys())


def getResultCategory(filename, status):
    '''
    This function return a string
    that shows the relation between status and file.
    '''
    status = status.lower()
    fileStatus = statusOfFile(filename)

    if status == fileStatus:
        prefix = 'correct'
    else:
        prefix = 'wrong'

    if status in [STR_PROP_DEREF, STR_PROP_FREE, STR_PROP_MEMTRACK, STR_SAFE, STR_UNSAFE]:
        return (prefix, status)
    elif status == STR_UNKNOWN:
        return RESULT_UNKNOWN
    else:
        return RESULT_ERROR


def calculateScore(category):
    return {RESULT_CORRECT_SAFE:   SCORE_CORRECT_SAFE,
            RESULT_WRONG_SAFE:     SCORE_WRONG_SAFE,
            RESULT_CORRECT_UNSAFE: SCORE_CORRECT_UNSAFE,
            RESULT_WRONG_UNSAFE:   SCORE_WRONG_UNSAFE,
            RESULT_CORRECT_PROP_DEREF:    SCORE_CORRECT_UNSAFE,
            RESULT_CORRECT_PROP_FREE:     SCORE_CORRECT_UNSAFE,
            RESULT_CORRECT_PROP_MEMTRACK: SCORE_CORRECT_UNSAFE,
            RESULT_WRONG_PROP_DEREF:      SCORE_WRONG_UNSAFE,
            RESULT_WRONG_PROP_FREE:       SCORE_WRONG_UNSAFE,
            RESULT_WRONG_PROP_MEMTRACK:   SCORE_WRONG_UNSAFE,
            }.get(category,  SCORE_UNKNOWN)
