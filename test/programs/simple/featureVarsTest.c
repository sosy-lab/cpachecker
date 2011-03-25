int featureModelValid() {
  if (! __SELECTED_FEATURE_Verify)
	  if (__SELECTED_FEATURE_Forward)
		  if (! __SELECTED_FEATURE_Sign)
			  return 0;
		   else return 1;
	   else return 1;
   else return 1;
}

int main() {
	int tmp = featureModelValid();
	if (! tmp)
		error: fail();
/*
	int tmp = 0;
	  if (! __SELECTED_FEATURE_Verify)
		  if (__SELECTED_FEATURE_Forward)
			  if (! __SELECTED_FEATURE_Sign)
				  tmp = 1;
			   else tmp = 1;
		   else tmp = 1;
	   else tmp = 1;

	  if (0) {}

	  if (__SELECTED_FEATURE_Sign) {
		  error: fail();
	  }
*/

}
