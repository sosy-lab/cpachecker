BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/predabs-lbe.properties"
OUTPUTNM="output/predabs-lbe/predabs-lbe_noMonitoring.log"
INPUTS="testsets/predabs-lbe.set"
MEMLIMIT=200000 #in kb

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_predabs-lbe.py -m $MEMLIMIT --config=$CONFIG --output=$OUTPUTNM "$@" $INSTANCES
