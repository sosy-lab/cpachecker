BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/uninitvars-limits_08_20s_assumeedges20.properties"
OUTPUTNM="output/uninitvars/20sTimeLimit/uninitvars_08_20s_assumeedges20.log"
INPUTS="testsets/uninitvars.set"
TIMELIMIT=120 #in seconds

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_uninitvars.py -t $TIMELIMIT --config=$CONFIG --output=$OUTPUTNM "$@" $INSTANCES
