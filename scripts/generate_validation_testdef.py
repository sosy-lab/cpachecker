#!/usr/bin/env python

"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2017  Dirk Beyer
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

# prepare for Python 3
from __future__ import absolute_import, print_function, unicode_literals

import sys
import os
from lxml import etree

sys.dont_write_bytecode = True # prevent creation of .pyc files

def _get_cpachecker_directory():
  scripts_dir = os.path.dirname(os.path.realpath(__file__))
  return os.path.dirname(scripts_dir)

def _get_test_directory():
  return _get_cpachecker_directory() + '/test/'

def _strip_xml_extension(path):
  expected_extension = '.xml'
  if path.lower().endswith(expected_extension):
    return path[:-len(expected_extension)]
  return path

def _get_validation_path(testdef_path):
  return _strip_xml_extension(testdef_path) + '-validation.xml'

def _generate_validation_file(testdef_path):
  testdef = etree.parse(testdef_path)
  benchmark = testdef.getroot()
  rundef = benchmark.find('rundefinition')
  input_rundef_name = rundef.get('name')

  # Remove the rundefinition for the verification runs
  for child in rundef:
    rundef.remove(child)
  rundef.text = None

  # Replace the rundefinition with one for witness validation
  witness_file = ('results/'
    + _strip_xml_extension(os.path.basename(testdef_path))
    + '.logfiles/')
  if input_rundef_name:
    witness_file = witness_file + input_rundef_name + '.'
  witness_file = witness_file + '${inputfile_name}.files/' + 'output/witness.graphml.gz'
  test_dir = _get_test_directory()
  rundef.set('name', 'witnessValidation')
  analysis_option = etree.Element('option')
  analysis_option.set('name', '-witnessValidation')
  rundef.append(analysis_option)
  witness_option = etree.Element('option')
  witness_option.set('name', '-witness')
  witness_option.text = (os.path.relpath(test_dir, _get_cpachecker_directory())
    + '/'
    + witness_file)
  rundef.append(witness_option)
  requiredfiles = etree.Element('requiredfiles')
  requiredfiles.text = (os.path.relpath(test_dir, os.path.dirname(testdef_path))
    + '/'
    + witness_file)
  rundef.append(requiredfiles)

  # Remove the resultfiles tag
  resultfiles = benchmark.find('resultfiles')
  resultfiles.getparent().remove(resultfiles)

  # Write the validation file
  with open(_get_validation_path(testdef_path), 'wb') as output_file:
    testdef.write(output_file, pretty_print=True)


if __name__ == "__main__":
  if not os.path.isdir(_get_test_directory()):
    sys.exit('The CPAchecker test directory was not found.')
  args = sys.argv[1:]
  for path in args:
    if not os.path.isfile(path):
      sys.exit('The input-file path {0} does not exist.'.format(path))
    try:
      testdef = etree.parse(path)
      benchmark = testdef.getroot()
      if benchmark is None:
        sys.exit('The input file {0} contains no root element.'.format(path))
      rundef = benchmark.find('rundefinition')
      if rundef is None:
        sys.exit('The input file {0} contains no rundefinition.'.format(path))
      resultfiles = benchmark.find('resultfiles')
      if resultfiles is None:
        sys.exit(('The input file {0} does not specify any result files, '
          + 'so it is guaranteed that it will not yield any witnesses').format(path))
    except etree.ParseError:
      sys.exit('The input file {0} is not a well-formed XML file.'.format(path))
    validation_path = _get_validation_path(path)
    if os.path.isfile(validation_path):
      sys.exit('The output-file path {0} already exists.'.format(validation_path))
  for path in args:
    _generate_validation_file(path)
