#! /usr/bin/python

DEBUG=False

import sys
import os.path

if len(sys.argv) != 2:
    sys.exit("Z3 directory required as single command-line argument.")

HEADER = '''// THIS FILE IS BUILD AUTOMATICALLY, DO NOT CHANGE!!!
#include<jni.h>
#include<stdlib.h>
#include"z3.h"

// include CPAchecker-specific parts
#include"includes/function.h"
#include"includes/arguments.h"
#include"includes/types.h"
#include"includes/error_handling.h"
'''

# Add debugging statements.
DEBUG = False


def process_text(text):
    """
    :param text: String containing the API text
    """
    # filter comments and stuff
    header_1 = 'interface Z3 {'
    header_2 = 'extern "C"'
    if header_1 in text:
        text = text[text.find(header_1)+len(header_1)+1:]
    elif header_2 in text:
        text = text[text.find(header_2)+len(header_2)+2:]
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
    out4=[]

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

        x = []

        x.append("DEFINE_FUNC(" + getType(retval) + ", " + name.replace("Z3_","").replace("_", "_1") + ") ")
        l = len(params)
        isVoidArgCall = (l==0 or (l==1 and params[0] == "void"))
        if isVoidArgCall:
            x.append("WITHOUT_ARGS")
        else:
            x.append("WITH_" + str(l) + "_ARGS(")

        inputs = []
        cleanups = []
        typs = []

        def checkAndClean(inp, typ, i):
            inputs.append(                inp + "_" + typ + "(" + str(i+1) + ")")
            cleanups.insert(0, "CLEAN_" + inp + "_" + typ + "(" + str(i+1) + ")")

        def d(t):
            if DEBUG:
                x.append("\n// %s\n" % t)

        for i, param in enumerate(params):
            parts = param.split()
           # print(parts)

            if parts[0] == "void" and len(parts) == 1:
                # Nothing to do.
                d("branch 1")

            # special case: string_ptr == string*
            elif not "*" in param and len(parts) == 2 and parts[0] == "Z3_string_ptr":
                d("branch 2")
                typ = "Z3_string"
                typs.append(getType(typ) + "_pointer")
                inp = typ.replace("Z3_", "").upper()
                checkAndClean(inp, "POINTER_ARG", i)
                cleanups.insert(0, "SET_" + inp + "_POINTER_ARG(" + str(i+1) + ")")

            elif len(parts) >= 2 and "[" not in param:

                # type + * + pname
                if "*" in param and parts[1] == "*":
                    d("branch 3-1")
                    typ = parts[0].replace("__","")
                    typs.append(getType(typ) + "_pointer")
                    inp = typ.replace("Z3_", "").upper()
                    checkAndClean(inp, "POINTER_ARG", i)

                elif "*" in param:
                    d("branch 3-2")
                    star_index = parts.index("*")
                    typ = "_".join(parts[0:star_index]).replace("__","") # unsigned + __int64
                    typs.append(getType(typ) + "_pointer")
                    inp = typ.replace("Z3_", "").upper()
                    checkAndClean(inp, "POINTER_ARG", i)

                # type... + pname
                else:
                    d("branch 3-3")
                    assert len(parts) >= 2
                    typ = "_".join(parts[0:-1]).replace("__","") # unsigned + __int64
                    typs.append(getType(typ))
                    inp = typ.replace("Z3_", "").upper()
                    checkAndClean(inp, "ARG", i)

            # type + pname[]
            elif len(parts) == 2 and parts[1].endswith("[]"):
                d("branch 4")
                typ = parts[0]
                typs.append(getType(typ) + "_array")
                inp = typ.replace("Z3_", "").upper()
                checkAndClean(inp, "ARRAY_ARG", i)

            # type + * + pname --> in java we use an array
            elif parts[1] == "*" and len(parts) == 3 and parts[1] is "*":
                d("branch 5")
                typ = parts[0]
                typs.append(getType(typ) + "_array")
                inp = typ.replace("Z3_", "").upper()
                checkAndClean(inp, "ARRAY_ARG", i)


            # type... + * + pname
            elif "*" in param and len(parts) >= 3 and parts[-2] == "*":
                d("branch 6")
                typ = "_".join(parts[1:-2]).replace("__","") # unsigned + __int64
                typs.append(getType(typ) + "_pointer")
                inp = typ.replace("Z3_", "").upper()
                checkAndClean(inp, "POINTER_ARG", i)
                cleanups.insert(0, "SET_" + inp + "_POINTER_ARG(" + str(i+1) + ")")

            elif "*" in param and len(parts) == 3 and parts[1] == "*":
                d("branch 7")
                typ = parts[0]
                typs.append(getType(typ) + "_pointer")
                inp = typ.replace("Z3_", "").upper()
                checkAndClean(inp, "POINTER_ARG", i)
                cleanups.insert(0, "SET_" + inp + "_POINTER_ARG(" + str(i+1) + ")")

            # "Z3_constructor constructors[]"
            elif parts[1].endswith("[]"):
                d("branch 8")

                typ = parts[1]
                typs.append(getType(typ) + "_array")
                inp = typ.replace("Z3_", "").upper()
                checkAndClean(inp, "OUT_ARRAY_ARG", i)
                cleanups.insert(0, "SET_" + inp + "_OUT_ARRAY_ARG(" + str(i+1) + ")")

            # "Z3_sort sorts[]"
            elif len(parts) == 2 and parts[1].endswith("[]"):
                d("branch 9")

                typ = parts[0]
                typs.append(getType(typ) + "_array")
                inp = typ.replace("Z3_", "").upper()
                checkAndClean(inp, "OUT_ARRAY_ARG", i)
                cleanups.insert(0, "SET_" + inp + "_OUT_ARRAY_ARG(" + str(i+1) + ")")

            elif len(parts) == 3 and parts[1] == "*":
                d("branch 10")

                typ = parts[0]
                typs.append(getType(typ) + "_array")
                inp = typ.replace("Z3_", "").upper()
                checkAndClean(inp, "OUT_ARRAY_ARG", i)
                cleanups.insert(0, "SET_" + inp + "_OUT_ARRAY_ARG(" + str(i+1) + ")")

            else:
                d("branch 11")
                # normal param: [type, pname]
                assert len(parts) == 2, parts
                typ, pname = parts
                typs.append(getType(typ))
                inp = typ.replace("Z3_", "").upper()
                checkAndClean(inp, "ARG", i)


        x.append(", ".join(typs))

        if isVoidArgCall:
            x.append("\n")
        else:
            x.append(")\n")

        # INPUT_ARG
        if inputs:
            x.append("\n".join(inputs) + "\n")

        # CALL()
        if retval == "void":
            if isVoidArgCall:
                x.append( "VOID_CALL0(")
            else:
                x.append( "VOID_CALL" + str(l) + "(")
        else:
            if isVoidArgCall:
                x.append( "CALL0(" + retval + ", ")
            else:
                x.append( "CALL" + str(l) + "(" + retval + ", ")

        x.append( name.replace("Z3_", "")  + ")\n")

        # FREE_ARG, SET_ARG
        if cleanups:
            x.append( "\n".join(cleanups) + "\n")

        # RETURN_VALUE
        simpleRetvals = ["void", "Z3_bool", "Z3_lbool", "Z3_bool_opt", "unsigned", "int", "double", "Z3_error_code", "Z3_goal_prec"]
        if retval not in simpleRetvals and not retval.endswith("kind") \
              and typs and typs[0] == "J_context":
            x.append( retval.replace("Z3_", "").upper() + "_RETURN_WITH_CONTEXT\n")
        else:
            x.append( retval.replace("Z3_", "").upper() + "_RETURN\n")

        out4.append("".join(x))

    tmp4 = "\n".join(out4)
    if DEBUG: open("out4","w").write(tmp4)
    return tmp4

def main():
    api = open(os.path.join(sys.argv[1], "z3_api.h")).read()
    interp_api = open(os.path.join(sys.argv[1], "z3_interp.h")).read()

    result_text = process_text(api)
    result_interp = process_text(interp_api)

    out_f = open("org_sosy_lab_cpachecker_util_predicates_z3_Z3NativeApi.c","w")

    # Write result
    out_f.write(HEADER)
    out_f.write(result_text)
    out_f.write('\n\n// INTERPOLATION\n\n')
    out_f.write(result_interp)

    out_f.close()

def getType(typ):
    return typ.replace("Z3","J")

if __name__ == "__main__":
    main()
