BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/predabs-lbe_timememlimits.properties"
OUTPUTNM="output/predabs-lbe/predabs-lbe_resourceLimits.log"
INPUTS="testsets/predabs-lbe.set"


INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_predabs-lbe.py --config=$CONFIG --output=$OUTPUTNM "$@" $INSTANCES
