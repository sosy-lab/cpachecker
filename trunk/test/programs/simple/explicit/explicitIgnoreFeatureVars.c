/*
 * This is a test to determine if the value-analysis can be configured to ignore the feature variable.
 * tested by cpa.value.ValueAnalysisTest
 * The error should only be found if the option
 * "cpa.value.precision.variableBlacklist" is set to "__SELECTED_FEATURE_(\\w)*" .
 */
void error()
{
	ERROR: goto ERROR;
}
int __SELECTED_FEATURE_base;

int main()
{
	__SELECTED_FEATURE_base = 0;
	if (__SELECTED_FEATURE_base) {
		error();
	}
}
