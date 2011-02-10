BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/uninitvars-limits_04_20s_pathlenght1000.properties"
OUTPUTNM="output/uninitvars/20sTimeLimit/uninitvars_04_20s_pathlenght1000.log"
INPUTS="testsets/uninitvars.set"
TIMELIMIT=120 #in seconds

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_uninitvars.py -t $TIMELIMIT --config=$CONFIG --output=$OUTPUTNM "$@" $INSTANCES
