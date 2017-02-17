#!/bin/bash

count() { echo $#; }

CPA_EXEC=`which cpa.sh`
[[ -z $CPA_EXEC ]] && [[ -f './cpa.sh' ]] && CPA_EXEC='./cpa.sh'
[[ -z $CPA_EXEC ]] && [[ -f './scripts/cpa.sh' ]] && CPA_EXEC='./scripts/cpa.sh'

echo "$@";

EXPECTED_RETURN_CODE=107

declare -a ARGS 

file=""
for arg in "$@";
do
  ARGS+=("$arg")
  file="$arg"
done # store last argument into $file

# Use 32 bit as default since CPAchecker's default is 32bit
gcc_machine_model_arg="-m32"
output_path='output'
while [[ $# -gt 0 ]]
do
key="$1"
case $key in
    -outputpath)
    output_path="$2"
    shift
    ;;
    -32)
    gcc_machine_model_arg="-m32"
    ;;
    -64)
    gcc_machine_model_arg="-m64"
    ;;
    -spec|-config|-cp|-classpath|-cpas|-entryfunction|-logfile|-outputpath|-setprop|-sourcepath|-timelimit|-witness)
    shift
    ;;
    -h|-help)
    $CPA_EXEC -h
    exit 0
    ;;
    -v|-version)
    $CPA_EXEC -v
    exit 0
    ;;
esac
shift # past argument or value
done

GCCARGS_GLOBAL=("$gcc_machine_model_arg" "'-D__alias__(x)='" "-o" "${output_path}/test_suite")

harness_gen_output=`"$CPA_EXEC" "${ARGS[@]}"`
harnesses=`find $output_path -name '*harness.c'` 
echo "`count $harnesses` harnesses for witness."  
for harness in $harnesses; do
  echo "Looking at $harness"

  GCCARGS=(${GCCARGS_GLOBAL[@]} "$harness" "$file")
  GCC_CMD="gcc -std=c11 ${GCCARGS[@]}"
  echo "$GCC_CMD"
  $GCC_CMD
  test_return=$?
  if [[ test_return -ne 0 ]]; then
    GCC_CMD="gcc -std=c90 ${GCCARGS[@]}"
    echo "$GCC_CMD"
    $GCC_CMD
  fi

  ${output_path}/test_suite > ${output_path}/stdout.txt 2> ${output_path}/stderr.txt
  test_return=$?
  if [[ test_return -eq $EXPECTED_RETURN_CODE ]]; then
    echo "Verification result: FALSE. Harness $harness reached expected error location."
    exit 0

  else 
    echo "Run with harness $harness was not successful."
  fi
done 

echo "Verification result: UNKNOWN. No harness for witness was sucessful or no harness was produced."
