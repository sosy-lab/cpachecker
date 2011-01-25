BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/uninitvars-limits_20_40MB_assumeedges50.properties"
OUTPUTNM="output/uninitvars/40MBMemLimit/uninitvars-limits_20_40MB_assumeedges50.log"
INPUTS="testsets/uninitvars.set"

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_uninitvars.py --config=$CONFIG --output=$OUTPUTNM "$@" $INSTANCES
