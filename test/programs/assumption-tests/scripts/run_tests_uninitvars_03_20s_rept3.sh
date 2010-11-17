BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/uninitvars-limits_03_20s_rept3.properties"
OUTPUTNM="output/uninitvars/20sTimeLimit/uninitvars_03_20s_rept3.log"
INPUTS="testsets/uninitvars.set"
TIMELIMIT=120 #in seconds

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_uninitvars.py -t $TIMELIMIT --config=$CONFIG --output=$OUTPUTNM "$@" $INSTANCES
