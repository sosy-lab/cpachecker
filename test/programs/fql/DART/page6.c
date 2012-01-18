void abort();

                       /* initially,       */
int is_room_hot=0;     /* room is not hot  */
int is_door_closed=0;  /* and door is open */
int ac=0;              /* so, ac is off    */

void ac_controller(int message) {
  
  int tmp_cond1;
  int tmp_cond2;
  int tmp_cond3;

  if (message == 0) 
  {
    is_room_hot=1;
  }
  
  if (message == 1) 
  {
    is_room_hot=0;
  }
  
  if (message == 2) 
  {
    is_door_closed=0;
    ac=0;
  }
  
  if (message == 3) 
  {
    is_door_closed=1;
    if (is_room_hot) 
    {
      ac=1;
    }
  }

  tmp_cond1 = 0;

  if (is_room_hot) 
  {
    if (is_door_closed)
    {
      if (!ac)
      {
        tmp_cond1 = 1;
      }
    }
  }

  if (tmp_cond1)
  {
    abort();    /* check correctness */
  }
}

