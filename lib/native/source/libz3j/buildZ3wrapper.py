#! /usr/bin/python

DEBUG=False

import sys
import os.path

if len(sys.argv) != 2:
    sys.exit("Z3 directory required as single command-line argument.")

# filter comments and stuff

text = open(os.path.join(sys.argv[1], "z3_api.h")).read()

text = text[text.find("interface Z3 {")+15:]
deprecatedBlock = False
comment=False
out1 = []
for i,c in enumerate(text):

  # skip deprecated functions
  if text[i:i+17] == "@name Deprecated ": deprecatedBlock = True
  if text[i:i+5] == "/*@}*": deprecatedBlock = False
  if text[i] == "/" and text[i+1] == "*": comment=True
  if text[i-1] == "/" and text[i-2] == "*": comment=False
  if not comment and not deprecatedBlock: out1.append(c)

tmp1 = "".join(out1)
if DEBUG: open("out1","w").write(tmp1)


# make one function per line

out2=[]
for line in tmp1.splitlines():
  line = line.strip()
  if not line or line == "}" or line == "};": continue
  if line.startswith("#") or line.startswith("//"): continue
  if line in ["BEGIN_MLAPI_EXCLUDE", "END_MLAPI_EXCLUDE"]: continue

  out2.append(line)

tmp2 = "".join(out2)
tmp2 = tmp2.replace(";", ";\n")
if DEBUG: open("out2","w").write(tmp2)


# remove unused information

extBlock = False
out3=[]
for line in tmp2.splitlines():
  assert line.endswith(");")

  # skip ext-functions
  if "Z3_reduce_eq_callback_fptr" in line:
    extBlock = True
  if "Z3_theory_get_app" in line:
    extBlock = False
    continue
  if extBlock: continue

  if "fptr" in line: continue # functionpointers currently unsupported
  if "set_error_handler" in line: continue # not supported
  if "_interpolation_problem" in line: continue # not supported
  if "_check_interpolant" in line: continue # not supported
  if line.startswith("typedef"): continue
  line = line.replace(" Z3_API ", " ")
  line = line.replace("const ", " ")
  line = line.replace("const*", " * ")
  line = line.replace("*", " * ")
  out3.append(line)
  # if "__" in line: print line

tmp3 = "\n".join(out3)
if DEBUG: open("out3","w").write(tmp3)


# now do the real replacing and build the new language...

def getType(typ):
    return typ.replace("Z3","J")

HEADER = '''// this file is build automatically, do not change
#include<jni.h>
#include<stdlib.h>
#include"z3.h"

// include CPAchecker-specific parts
#include"includes/function.h"
#include"includes/arguments.h"
#include"includes/types.h"
#include"includes/interpolation.h"
'''

out4=[HEADER]

for line in tmp3.splitlines():
  spl = line.split()
  retval = spl[0]
  name0 = spl[1]
  assert "(" in name0
  name = name0[:name0.find("(")]
  #print retval, "\t", name
  assert spl[-1].endswith(");")
  paramStr = name0[name0.find("(")+1:] + " " + " ".join(spl[2:])
  params = [p for p in paramStr.replace(");", "").strip().split(",") if p]
  #print "\t\t", paramStr, params

  x = "DEFINE_FUNC(" + getType(retval) + ", " + name.replace("Z3_","").replace("_", "_1") + ") "
  l = len(params)
  isVoidArgCall = (l==0 or (l==1 and params[0] == "void"))
  if isVoidArgCall:
      x += "WITHOUT_ARGS"
  else:
      x += "WITH_" + str(l) + "_ARGS("

  inputs = []
  cleanups = []
  typs = []

  def checkAndClean(inp, typ, i):
      inputs.append(                inp + "_" + typ + "(" + str(i+1) + ")")
      cleanups.insert(0, "CLEAN_" + inp + "_" + typ + "(" + str(i+1) + ")")

  for i, param in enumerate(params):
      parts = param.split()
     # print(parts)

      # f(void) --> nothing to do
      if parts[0] == "void":
        assert len(parts) == 1
        continue

      # modifier available
      if parts[0].startswith("__"):
        mod = parts[0]


        if mod == "__in":
            assert "[" not in param

            # mod + type + * + pname
            if "*" in param:
                assert parts[2] == "*"
                typ = parts[1]
                typs.append(getType(typ) + "_pointer")
                inp = typ.replace("Z3_", "").upper()
                checkAndClean(inp, "POINTER_ARG", i)

            # mod + type... + pname
            else:
                assert len(parts) >= 3
                typ = "_".join(parts[1:-1]).replace("__","") # unsigned + __int64
                typs.append(getType(typ))
                inp = typ.replace("Z3_", "").upper()
                checkAndClean(inp, "ARG", i)

        # mod + type + pname[]
        elif mod.startswith("__in_ecount(") \
                and len(parts) == 3 and parts[2].endswith("[]"):
            typ = parts[1]
            typs.append(getType(typ) + "_array")
            inp = typ.replace("Z3_", "").upper()
            checkAndClean(inp, "ARRAY_ARG", i)

        # mod + type + * + pname --> in java we use an array
        elif mod.startswith("__in_ecount("):
            assert len(parts) == 4 and parts[2] is "*"
            typ = parts[1]
            typs.append(getType(typ) + "_array")
            inp = typ.replace("Z3_", "").upper()
            checkAndClean(inp, "ARRAY_ARG", i)

        # special case: string_ptr == string*
        elif mod == "__out_opt" and not "*" in param:
            assert len(parts) == 3 and parts[1] == "Z3_string_ptr"
            typ = "Z3_string"
            typs.append(getType(typ) + "_pointer")
            inp = typ.replace("Z3_", "").upper()
            checkAndClean(inp, "POINTER_ARG", i)
            cleanups.insert(0, "SET_" + inp + "_POINTER_ARG(" + str(i+1) + ")")

        # mod + type... + * + pname
        elif mod == "__out" or mod == "__out_opt":
            assert "*" in param and len(parts) >= 4 and parts[-2] == "*"
            typ = "_".join(parts[1:-2]).replace("__","") # unsigned + __int64
            typs.append(getType(typ) + "_pointer")
            inp = typ.replace("Z3_", "").upper()
            checkAndClean(inp, "POINTER_ARG", i)
            cleanups.insert(0, "SET_" + inp + "_POINTER_ARG(" + str(i+1) + ")")

        # "__inout unsigned * core_size"
        elif mod.startswith("__inout") and "*" in param:
            assert len(parts) == 4 and parts[2] == "*"
            typ = parts[1]
            typs.append(getType(typ) + "_pointer")
            inp = typ.replace("Z3_", "").upper()
            checkAndClean(inp, "POINTER_ARG", i)
            cleanups.insert(0, "SET_" + inp + "_POINTER_ARG(" + str(i+1) + ")")

        # "__inout_ecount(num_constructors) Z3_constructor constructors[]"
        elif mod.startswith("__inout_ecount("):
            assert len(parts) == 3 and parts[2].endswith("[]")

            #lenParam = mod[15 : -1] # value of __inout_ecount()
            #pnames = [p.split()[-1] for p in params]
            #numLenParam = pnames.index(lenParam)

            typ = parts[1]
            typs.append(getType(typ) + "_array")
            inp = typ.replace("Z3_", "").upper()
            checkAndClean(inp, "OUT_ARRAY_ARG", i)
            cleanups.insert(0, "SET_" + inp + "_OUT_ARRAY_ARG(" + str(i+1) + ")")

        # "__out_ecount(num_sorts) Z3_sort sorts[]"
        elif mod.startswith("__out_ecount(") \
            and len(parts) == 3 and parts[2].endswith("[]"):

            #lenParam = mod[13 : -1] # value of __out_ecount()
            #pnames = [p.split()[-1] for p in params]
            #numLenParam = pnames.index(lenParam)

            typ = parts[1]
            typs.append(getType(typ) + "_array")
            inp = typ.replace("Z3_", "").upper()
            checkAndClean(inp, "OUT_ARRAY_ARG", i)
            cleanups.insert(0, "SET_" + inp + "_OUT_ARRAY_ARG(" + str(i+1) + ")")

        # "__out_ecount(num_sorts) Z3_sort sorts[]"
        elif mod.startswith("__out_ecount("):
            assert len(parts) == 4 and parts[2] is "*"

            #lenParam = mod[13 : -1] # value of __out_ecount()
            #pnames = [p.split()[-1] for p in params]
            #numLenParam = pnames.index(lenParam)

            typ = parts[1]
            typs.append(getType(typ) + "_array")
            inp = typ.replace("Z3_", "").upper()
            checkAndClean(inp, "OUT_ARRAY_ARG", i)
            cleanups.insert(0, "SET_" + inp + "_OUT_ARRAY_ARG(" + str(i+1) + ")")

        else:
            pass
            print parts


      # normal param: [type, pname]
      else:
        try:
            assert len(parts) == 2, parts
        except:
            import pdb; pdb.set_trace()
        typ, pname = parts
        typs.append(getType(typ))
        inp = typ.replace("Z3_", "").upper()
        checkAndClean(inp, "ARG", i)


  x += ", ".join(typs)

  if isVoidArgCall:
      x += "\n"
  else:
      x += ")\n"

  # INPUT_ARG
  if inputs: x += "\n".join(inputs) + "\n"

  # CALL()
  if retval == "void":
      if isVoidArgCall:
          x += "VOID_CALL0("
      else:
          x += "VOID_CALL" + str(l) + "("
  else:
      if isVoidArgCall:
          x += "CALL0(" + retval + ", "
      else:
          x += "CALL" + str(l) + "(" + retval + ", "

  x += name.replace("Z3_", "")  + ")\n"

  # FREE_ARG, SET_ARG
  if cleanups: x += "\n".join(cleanups) + "\n"

  # RETURN_VALUE
  simpleRetvals = ["void", "Z3_bool", "Z3_lbool", "Z3_bool_opt", "unsigned", "int", "double", "Z3_error_code", "Z3_goal_prec"]
  if retval not in simpleRetvals and not retval.endswith("kind") \
        and typs and typs[0] == "J_context":
      x += retval.replace("Z3_", "").upper() + "_RETURN_WITH_CONTEXT\n"
  else:
      x += retval.replace("Z3_", "").upper() + "_RETURN\n"

  out4.append(x)

tmp4 = "\n".join(out4)
if DEBUG: open("out4","w").write(tmp4)


# write result
open("org_sosy_lab_cpachecker_util_predicates_z3_Z3NativeApi.c","w").write(tmp4)
