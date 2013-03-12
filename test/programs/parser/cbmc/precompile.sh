#!/bin/bash

ARRAY=("${@}")
ELEMENTS=${#ARRAY[@]}

for((i = 0; i < ${ELEMENTS}; i++)); do
   DIR=$(echo "${ARRAY[${i}]}" | awk -F/ '{print $1}')
   OUT="${DIR}/${DIR}.i"
   echo "${OUT}"
   gcc -E "${ARRAY[${i}]}" -o "${OUT}"

done


