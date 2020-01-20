int main() {
  
  //Object type: (cast to void* and back doesn't change anything)
  int coordinates[2] = {3,2};
  void* p = coordinates;
  int* a = ((int*)p);
  if (a[0] != 3) goto ERROR;

  //incomplete types can be assigned to void*
  int* incomplete;
  void* inc_p = incomplete;
  int* incomplete_2 = (int*)inc_p;

  return;

ERROR:
  return 1;

EXIT:
  return 0;
}
