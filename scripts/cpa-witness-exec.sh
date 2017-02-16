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
output_path='output'

while [[ $# -gt 0 ]]
do
key="$1"
case $key in
    -outputpath)
    output_path="$2"
    shift
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

harness_gen_output=`"$CPA_EXEC" "${ARGS[@]}"`
harnesses=`find $output_path -name '*harness.c'` 
echo "`count $harnesses` harnesses for witness."  
for harness in $harnesses; do
  echo "Looking at $harness"

  GCCARGS=("-o" "${output_path}/test_suite" "$harness" "$file")

  gcc -std=c11 "${GCCARGS[@]}"
  test_return=$?
  if [[ test_return -ne 0 ]]; then
    gcc -std=c90 "${GCCARGS[@]}"
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
