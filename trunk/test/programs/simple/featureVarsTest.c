
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
	int tmp;
	tmp = featureModelValid();
	if (! tmp)
		ERROR: return 1;
}
