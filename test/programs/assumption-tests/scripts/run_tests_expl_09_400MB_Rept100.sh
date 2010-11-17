BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/explicitAnalysis_09_400MB_rept100.properties"
OUTPUTM="output/explicit/400MBMemLimit/explicitAnalysis_09_400MB_rept100.log"
INPUTS="testsets/explicit.set"

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_explicit.py --config=$CONFIG --output=$OUTPUTM "$@" $INSTANCES
