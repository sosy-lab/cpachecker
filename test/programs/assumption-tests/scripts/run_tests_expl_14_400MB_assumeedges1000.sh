BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/explicitAnalysis_14_400MB_assumeedges1000.properties"
OUTPUTM="output/explicit/400MBMemLimit/explicitAnalysis_14_400MB_assumeedges1000.log"
INPUTS="testsets/explicit_BUG.set"

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_explicit.py --config=$CONFIG --output=$OUTPUTM "$@" $INSTANCES
