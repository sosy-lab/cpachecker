BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/uninitvars-limits_09_20s_assumeedges50.properties"
OUTPUTNM="output/uninitvars/20sTimeLimit/uninitvars_09_20s_assumeedges50.log"
INPUTS="testsets/uninitvars.set"
TIMELIMIT=120 #in seconds

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_uninitvars.py -t $TIMELIMIT --config=$CONFIG --output=$OUTPUTNM "$@" $INSTANCES
