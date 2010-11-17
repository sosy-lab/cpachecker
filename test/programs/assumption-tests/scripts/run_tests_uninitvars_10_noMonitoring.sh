BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/uninitvars-limits_10_noMonitoring.properties"
OUTPUTNM="output/uninitvars/40MBMemLimit/uninitvars-limits_10_noMonitoring.log"
INPUTS="testsets/uninitvars.set"
MEMLIMIT=200000 #in kb

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_uninitvars40MB.py -m $MEMLIMIT --config=$CONFIG --output=$OUTPUTNM "$@" $INSTANCES
