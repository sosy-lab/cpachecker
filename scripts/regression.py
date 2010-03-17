#!/usr/bin/env python

import os, subprocess

config_dir = '../test/config/'
test_set_dir = '../test/test-sets/' 

def list_of_files_in(dir):
    result = []
    for dirpath, dirnames, filenames in os.walk(dir):
        for file in filenames:
            result.append(file)
    return result

def main():
    for config in filter(lambda str: str.endswith('.properties'),
                         list_of_files_in(config_dir)):
        for test_set in filter(lambda str: str.endswith('.set'),
                               list_of_files_in(test_set_dir)):
            subprocess.call(['./run_test_set.sh', test_set, config])

if __name__ == '__main__':
    main()