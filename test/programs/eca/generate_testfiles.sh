#!/bin/bash

REACHABILITY_RESULT="safety_information.files"
CPACHECKER_ERROR="\t\tERROR: goto ERROR;"
VERBOSE=1

if [ ! -e "$REACHABILITY_RESULT" ]; then
  echo "The file '$REACHABILITY_RESULT' with information"
  echo "about the reachability of labels can not be found."
  exit 1
fi

for target_file in $(cat "$REACHABILITY_RESULT"); do

  basename=$(echo "$target_file" \
    | sed 's/_.*c/.c/')

  if [ ! -e "$basename" ]; then
    echo "The file $basename can not be found."
    echo "Aborting."
    exit 1
  fi

  error_id=$(echo "$target_file" \
    | sed 's/.*_\(.*\)_.*/\1/')

  # there is a special error label "global_error" that
  # becomes error 60
  if [ "$error_id" = "60" ]; then
    error_label="globalError:"
  else
    single_digit=$(echo "$error_id" | sed 's/^0*\(.\)/\1/')
    error_label="error_$single_digit:"
  fi

  if [ -n "$VERBOSE" ]; then
    echo "$basename -> $target_file with the error label $error_label"
  fi

  cat "$basename" \
    | sed "s/^.*$error_label.*$/$CPACHECKER_ERROR/" \
    > "$target_file"

done


# vim:sts=2:sw=2:
