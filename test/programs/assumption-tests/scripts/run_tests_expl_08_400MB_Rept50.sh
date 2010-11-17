BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/explicitAnalysis_08_400MB_rept50.properties"
OUTPUTM="output/explicit/400MBMemLimit/explicitAnalysis_08_400MB_rept50.log"
INPUTS="testsets/explicit.set"

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_explicit.py --config=$CONFIG --output=$OUTPUTM "$@" $INSTANCES
