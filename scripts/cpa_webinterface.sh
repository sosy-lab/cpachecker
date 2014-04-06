#!/bin/bash

SUBMISSION_URL="http://vcloud.sosy-lab.org/submit.php"

function print_help_and_exit {
  echo "Submit files to VerifierCloud."
  echo "Parameters:"
  echo "  --help      Print this help"
  echo "  --analysis  Analysis to use"
  echo "  --file      File for verification"
  exit
}

# loop over all input parameters and parse them
declare -a OPTIONS
while [ $# -gt 0 ]; do
  case $1 in
    "--help")
      PRINT_HELP=1
      ;;

    "--analysis")
      shift
      ANALYSIS=$1
      ;;
    "--file")
      shift
      FILE=$1
      ;;

    *)
      echo "Unknown Parameter: $1"
      print_help_and_exit
      ;;
  esac

  shift
done

if [ -z "$ANALYSIS" -o -z "$FILE" ]; then
  print_help_and_exit
fi

if [ "$PRINT_HELP" ]; then
  print_help_and_exit
fi

curl -F "cfile=@$FILE" -F "analysis=$ANALYSIS" "$SUBMISSION_URL"


# vim:sts=2:sw=2:expandtab
