#!/usr/bin/python3
import sys
import os
import re

#
re_variable = re.compile("[a-zA-Z_][a-zA-Z0-9_@^::$]*")

if len(sys.argv) < 3:
    exit(0)
infile = sys.argv[1]
outfile = sys.argv[2]

f_in = open(infile, 'r')
f_out = open(outfile, 'w')

formula = f_in.read()
variables = re_variable.findall(formula)

var_str = "VAR "
for i in range(len(variables)):
    if i == len(variables)-1:
        var_str = var_str + variables[i] + " : REAL\n"
    else:
        var_str = var_str + variables[i] + ", " 

f_out.write(var_str)
f_out.write("FORMULA "+formula)

f_out.close()
print("Done")
