void __ARSG__start();

void io_init(void);
void ctrl_get_vals(void);
void ctrl_set_vals(void);


int checkRun();
void waitForMotorStart();
void doCmd();
void doImpuls(int val, int motor, int reset);
void checkCmd();
void checkLevel();
void ctrl_loop();



int levelPos__0;
int levelPos__1;
int levelPos__2;
int levelPos__3;
int levelPos__4;
int levelPos__5;
int levelPos__6;
int levelPos__7;
int levelPos__8;
int levelPos__9;
int levelPos__10;
int levelPos__11;
int levelPos__12;
int levelPos__13;
int one_level;
int newLevel;



int cntValid;



int cnt;



int level;




int loadLevel;



int loadPending;



int loadSensor;





int cmd;

int timMotor;

int timImp;




int directionUp;




int lastImp;

int dbgCnt;




int endCnt;



int ctrl_io_in__0;
int ctrl_io_in__1;
int ctrl_io_in__2;
int ctrl_io_in__3;
int ctrl_io_in__4;
int ctrl_io_in__5;
int ctrl_io_in__6;
int ctrl_io_in__7;
int ctrl_io_in__8;
int ctrl_io_in__9;

int ctrl_io_out__0;
int ctrl_io_out__1;
int ctrl_io_out__2;
int ctrl_io_out__3;

int ctrl_io_analog__0;
int ctrl_io_analog__1;
int ctrl_io_analog__2;
int ctrl_io_analog__3;

int ctrl_io_led__0;
int ctrl_io_led__1;
int ctrl_io_led__2;
int ctrl_io_led__3;
int ctrl_io_led__4;
int ctrl_io_led__5;
int ctrl_io_led__6;
int ctrl_io_led__7;
int ctrl_io_led__8;
int ctrl_io_led__9;
int ctrl_io_led__10;
int ctrl_io_led__11;
int ctrl_io_led__12;
int ctrl_io_led__13;
int ctrl_dly1, ctrl_dly2;


volatile int simio_in;
volatile int simio_out;
volatile int simio_led;
volatile int simio_adc1, simio_adc2, simio_adc3;

void io_init()
{
  ctrl_dly1 = 0;
  ctrl_dly2 = 0;
}

void ctrl_set_vals()
{ int val = 0, i=4 -1;
# 89 "lift_io.c"
    val = val + val;
    if (ctrl_io_out__3) {
      val = val + 1;
    }
    val = val + val;
    if (ctrl_io_out__2) {
      val = val + 1;
    }
    val = val + val;
    if (ctrl_io_out__1) {
      val = val + 1;
    }
    val = val + val;
    if (ctrl_io_out__0) {
      val = val + 1;
    }
  simio_out = val;
  i=14 -1;

    val = val + val;
    if (ctrl_io_led__13) {
      val = val + 1;
    }
    val = val + val;
    if (ctrl_io_led__12) {
      val = val + 1;
    }
    val = val + val;
    if (ctrl_io_led__11) {
      val = val + 1;
    }
    val = val + val;
    if (ctrl_io_led__10) {
      val = val + 1;
    }
    val = val + val;
    if (ctrl_io_led__9) {
      val = val + 1;
    }
    val = val + val;
    if (ctrl_io_led__8) {
      val = val + 1;
    }
    val = val + val;
    if (ctrl_io_led__7) {
      val = val + 1;
    }
    val = val + val;
    if (ctrl_io_led__6) {
      val = val + 1;
    }
    val = val + val;
    if (ctrl_io_led__5) {
      val = val + 1;
    }
    val = val + val;
    if (ctrl_io_led__4) {
      val = val + 1;
    }
    val = val + val;
    if (ctrl_io_led__3) {
      val = val + 1;
    }
    val = val + val;
    if (ctrl_io_led__2) {
      val = val + 1;
    }
    val = val + val;
    if (ctrl_io_led__1) {
      val = val + 1;
    }
    val = val + val;
    if (ctrl_io_led__0) {
      val = val + 1;
    }
  simio_led = val;
}

void ctrl_get_vals()
{ 
  int __BLAST_NONDET;

  // simio_in is volatile
  simio_in = __BLAST_NONDET;

	int i=0,in0 = simio_in,in1 = ctrl_dly1, in2 = ctrl_dly2;
  int in0_bit0__0, in1_bit0__0, in2_bit0__0;
  int in0_bit0__1, in1_bit0__1, in2_bit0__1;
  int in0_bit0__2, in1_bit0__2, in2_bit0__2;
  int in0_bit0__3, in1_bit0__3, in2_bit0__3;
  int in0_bit0__4, in1_bit0__4, in2_bit0__4;
  int in0_bit0__5, in1_bit0__5, in2_bit0__5;
  int in0_bit0__6, in1_bit0__6, in2_bit0__6;
  int in0_bit0__7, in1_bit0__7, in2_bit0__7;
  int in0_bit0__8, in1_bit0__8, in2_bit0__8;
  int in0_bit0__9, in1_bit0__9, in2_bit0__9;

  int tmp;
  tmp = __BLAST_NONDET;
  in0_bit0__0 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in0_bit0__1 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in0_bit0__2 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in0_bit0__3 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in0_bit0__4 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in0_bit0__5 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in0_bit0__6 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in0_bit0__7 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in0_bit0__8 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in0_bit0__9 = (tmp == 0);

  // lets assume in0 is a 10-bit word
	__CPROVER_assume(in0 == (in0_bit0__0 + 2 * in0_bit0__1 + 4 * in0_bit0__2 + 8 * in0_bit0__3 + 16 * in0_bit0__4 + 32 * in0_bit0__5 + 64 * in0_bit0__6 + 128 * in0_bit0__7 + 256 * in0_bit0__8 + 512 * in0_bit0__9));

	tmp = __BLAST_NONDET;
  in1_bit0__0 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in1_bit0__1 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in1_bit0__2 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in1_bit0__3 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in1_bit0__4 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in1_bit0__5 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in1_bit0__6 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in1_bit0__7 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in1_bit0__8 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in1_bit0__9 = (tmp == 0);

  // lets assume in1 is a 10-bit word
	__CPROVER_assume(in1 == (in1_bit0__0 + 2 * in1_bit0__1 + 4 * in1_bit0__2 + 8 * in1_bit0__3 + 16 * in1_bit0__4 + 32 * in1_bit0__5 + 64 * in1_bit0__6 + 128 * in1_bit0__7 + 256 * in1_bit0__8 + 512 * in1_bit0__9));

	tmp = __BLAST_NONDET;
  in2_bit0__0 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in2_bit0__1 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in2_bit0__2 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in2_bit0__3 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in2_bit0__4 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in2_bit0__5 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in2_bit0__6 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in2_bit0__7 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in2_bit0__8 = (tmp == 0);
	tmp = __BLAST_NONDET;
  in2_bit0__9 = (tmp == 0);

  // lets assume in2 is a 10-bit word
	__CPROVER_assume(in2 == (in2_bit0__0 + 2 * in2_bit0__1 + 4 * in2_bit0__2 + 8 * in2_bit0__3 + 16 * in2_bit0__4 + 32 * in2_bit0__5 + 64 * in2_bit0__6 + 128 * in2_bit0__7 + 256 * in2_bit0__8 + 512 * in2_bit0__9));


  ctrl_dly2 = ctrl_dly1;
  ctrl_dly1 = in0;
    ctrl_io_in__0 = (in0_bit0__0 + in1_bit0__0 + in2_bit0__0) > 1;
    ctrl_io_in__1 = (in0_bit0__1 + in1_bit0__1 + in2_bit0__1) > 1;
    ctrl_io_in__2 = (in0_bit0__2 + in1_bit0__2 + in2_bit0__2) > 1;
    ctrl_io_in__3 = (in0_bit0__3 + in1_bit0__3 + in2_bit0__3) > 1;
    ctrl_io_in__4 = (in0_bit0__4 + in1_bit0__4 + in2_bit0__4) > 1;
    ctrl_io_in__5 = (in0_bit0__5 + in1_bit0__5 + in2_bit0__5) > 1;
    ctrl_io_in__6 = (in0_bit0__6 + in1_bit0__6 + in2_bit0__6) > 1;
    ctrl_io_in__7 = (in0_bit0__7 + in1_bit0__7 + in2_bit0__7) > 1;
    ctrl_io_in__8 = (in0_bit0__8 + in1_bit0__8 + in2_bit0__8) > 1;
    ctrl_io_in__9 = (in0_bit0__9 + in1_bit0__9 + in2_bit0__9) > 1;

  ctrl_io_analog__0 = simio_adc1;
  ctrl_io_analog__1 = simio_adc2;
  ctrl_io_analog__2 = simio_adc3;
}

void ctrl_init()
{
  int i=0;
  io_init();
  cntValid = 0;
  cnt = 0;
  cmd = 0;
  timMotor = 0;
  timImp = 0;
  directionUp = 1;
  lastImp = 0;
  loadLevel = 0;
  loadPending = 0;
  loadSensor = 0;
  i = 0;
  levelPos__0 = 0;
  levelPos__1=58;
  levelPos__2=115;
  levelPos__3=173;
  levelPos__4=230;
  levelPos__5=288;
  levelPos__6=346;
  levelPos__7=403;
  levelPos__8=461;
  levelPos__9=518;
  levelPos__10=576;
  levelPos__11=634;
  levelPos__12=691;
  levelPos__13=749;


  one_level = levelPos__1;
}

void ctrl_loop()
{ int checkLevel_i=0, checkLevel_middle=0; int run = 0;

  if (cmd==0) {


    if (loadPending) {

      if (ctrl_io_in__2) {
 cmd = 1;
      }

    } else if (ctrl_io_in__4) {

      if (!ctrl_io_in__1) {
 if ( level!=14 ) {
   cmd = 3;
 }
      }

    } else if (ctrl_io_in__5) {

      if (!ctrl_io_in__2) {
 if (level!=1){
   cmd = 4;
 }
      }

    } else if (ctrl_io_in__8) {
      if (loadLevel!=0) {
 if (level<loadLevel){
   cmd = 1;
 }
 else {
   cmd = 2;
 }
      } else {
 cmd = 2;
      }
      loadPending = 1;
      loadSensor = 0;

    } else if (ctrl_io_in__6) {

      if (!ctrl_io_in__1) {
 cmd = 1;
      }

    } else if (ctrl_io_in__7) {

      if (!ctrl_io_in__2) {
 cmd = 2;
      }
    }
    if (cmd!=0) {
      timMotor = 50;
    }


  } else {



    if (ctrl_io_in__0) {
      if (!lastImp){

 if (ctrl_io_out__0) {
   if (directionUp) {
     ++cnt;
   } else {
     --cnt;
   }
 }
 else if (timImp>0) {
   if (directionUp) {
     ++cnt;
   } else {
     --cnt;
   }
 }
      }
    }

    if (ctrl_io_in__2) {
      cnt = 0;
      cntValid = 1;
    }

    lastImp = ctrl_io_in__0;
    if (timImp>0) {
      --timImp;
      if (timImp==0) {
 if (cmd!=0) {
   cmd = 0;
 }
      }
    }



    if (timMotor > 0) {

      --timMotor;
      if (cmd==3) {
 directionUp = 1;
      }
      else if (cmd==1) {
 directionUp = 1;
      }

      ctrl_io_out__1 = directionUp;
      if (!cntValid) {
 cnt = 0;
 if (cmd==3) {
   endCnt = one_level;
 } else {
   endCnt = -one_level;
 }
      } else {
 endCnt = cnt;
 newLevel = -99;
 if (cmd==3) {
   newLevel = level+1;
 } else if (cmd==4){
   newLevel = level-1;
 }
 --newLevel;
 if (newLevel>=0) {
   if (newLevel<14) {

      switch (newLevel) {
        case 0:
         endCnt = levelPos__0;
          break;
        case 1:
         endCnt = levelPos__1;
          break;
        case 2:
         endCnt = levelPos__2;
          break;
        case 3:
         endCnt = levelPos__3;
          break;
        case 4:
         endCnt = levelPos__4;
          break;
        case 5:
         endCnt = levelPos__5;
          break;
        case 6:
         endCnt = levelPos__6;
          break;
        case 7:
         endCnt = levelPos__7;
          break;
        case 8:
         endCnt = levelPos__8;
          break;
        case 9:
         endCnt = levelPos__9;
          break;
        case 10:
         endCnt = levelPos__10;
          break;
        case 11:
         endCnt = levelPos__11;
          break;
        case 12:
         endCnt = levelPos__12;
          break;
        case 13:
         endCnt = levelPos__13;
          break;
      }
   }
 }
      }
    } else {

      run = 0;

      if (cmd == 3) {
 if (cnt < endCnt - 1)
 {

   if (!ctrl_io_in__1) {
     run = 1;
     goto RETURN_checkRun;
   }
 }
      } else if (cmd == 4) {
 if (cnt > endCnt + 1)
 {

   if (!ctrl_io_in__2) {
     run = 1;
     goto RETURN_checkRun;
   }
 }
      } else if (cmd == 1) {
 if (loadPending) {

   if (ctrl_io_in__3) {

     loadLevel = level;
     loadPending = 0;
     run = 0;
     goto RETURN_checkRun;
   }
 }

 if (!ctrl_io_in__1)
 {
   run = 1;
   goto RETURN_checkRun;
 }

 loadPending = 0;
      } else if (cmd == 2) {
 if (loadPending) {
   if (loadSensor) {

     if (!ctrl_io_in__3) {
       loadSensor = 0;

       loadPending = 0;
       loadLevel = level;
       run = 0;
       goto RETURN_checkRun;
     }
   }

   loadSensor = ctrl_io_in__3;
 }

 if (!ctrl_io_in__2)
 {
   run = 1;
   goto RETURN_checkRun;
 }
      }
      run = 0;
RETURN_checkRun:



      if (ctrl_io_out__0) {
 if (!run) {

   cmd = 99;
   timImp = 50;
 }
      }

      ctrl_io_out__0 = run;
    }
  }

  checkLevel_middle = one_level>>2;
  if (cntValid) {
    level=1;
    while ( level < 14 ) {

      switch (level) {
        case 0:
          if (cnt < levelPos__0-checkLevel_middle)
            goto out;
          break;
        case 1:
          if (cnt < levelPos__1-checkLevel_middle)
            goto out;
          break;
        case 2:
          if (cnt < levelPos__2-checkLevel_middle)
            goto out;
          break;
        case 3:
          if (cnt < levelPos__3-checkLevel_middle)
            goto out;
          break;
        case 4:
          if (cnt < levelPos__4-checkLevel_middle)
            goto out;
          break;
        case 5:
          if (cnt < levelPos__5-checkLevel_middle)
            goto out;
          break;
        case 6:
          if (cnt < levelPos__6-checkLevel_middle)
            goto out;
          break;
        case 7:
          if (cnt < levelPos__7-checkLevel_middle)
            goto out;
          break;
        case 8:
          if (cnt < levelPos__8-checkLevel_middle)
            goto out;
          break;
        case 9:
          if (cnt < levelPos__9-checkLevel_middle)
            goto out;
          break;
        case 10:
          if (cnt < levelPos__10-checkLevel_middle)
            goto out;
          break;
        case 11:
          if (cnt < levelPos__11-checkLevel_middle)
            goto out;
          break;
        case 12:
          if (cnt < levelPos__12-checkLevel_middle)
            goto out;
          break;
        case 13:
          if (cnt < levelPos__13-checkLevel_middle)
            goto out;
          break;

      }
      ++level;
    }
out: ;
  } else {
    level = 0;
  }
  checkLevel_i=0;

    ctrl_io_led__0 = (checkLevel_i == level-1);
    ++checkLevel_i;
    ctrl_io_led__1 = (checkLevel_i == level-1);
    ++checkLevel_i;
    ctrl_io_led__2 = (checkLevel_i == level-1);
    ++checkLevel_i;
    ctrl_io_led__3 = (checkLevel_i == level-1);
    ++checkLevel_i;
    ctrl_io_led__4 = (checkLevel_i == level-1);
    ++checkLevel_i;
    ctrl_io_led__5 = (checkLevel_i == level-1);
    ++checkLevel_i;
    ctrl_io_led__6 = (checkLevel_i == level-1);
    ++checkLevel_i;
    ctrl_io_led__7 = (checkLevel_i == level-1);
    ++checkLevel_i;
    ctrl_io_led__8 = (checkLevel_i == level-1);
    ++checkLevel_i;
    ctrl_io_led__9 = (checkLevel_i == level-1);
    ++checkLevel_i;
    ctrl_io_led__10 = (checkLevel_i == level-1);
    ++checkLevel_i;
    ctrl_io_led__11 = (checkLevel_i == level-1);
    ++checkLevel_i;
    ctrl_io_led__12 = (checkLevel_i == level-1);
    ++checkLevel_i;
    ctrl_io_led__13 = (checkLevel_i == level-1);
    ++checkLevel_i;

  ctrl_io_led__13 = (dbgCnt&0x80) != 0;
  ++dbgCnt;
}


void controller() {
  ctrl_get_vals();
  ctrl_loop();
  ctrl_set_vals();
}

int main()
{

  ctrl_init();
  controller();
  return 0;
}

