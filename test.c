/*001:*/ void reach_error()
/*002:*/ {
/*003:*/ }
/*004:*/ 
/*005:*/ extern void abort(void);
/*006:*/ int __describerr_reached_error_once = 0;
/*007:*/ int __describerr_abort(int input)
/*008:*/ {
/*009:*/   _Bool __describerr_last_selector = 1;
/*010:*/   _Bool __describerr_check_contains_all_unsafe_traces = 0;
/*011:*/   if (__describerr_check_contains_all_unsafe_traces)
/*012:*/   {
/*013:*/     if ((__describerr_last_selector == 0) && (__describerr_reached_error_once == 1))
/*014:*/     {
/*015:*/       reach_error();
/*016:*/     }
/*017:*/   }
/*018:*/   else
/*019:*/   {
/*020:*/     if ((__describerr_last_selector == 1) && (__describerr_reached_error_once == 0))
/*021:*/     {
/*022:*/       reach_error();
/*023:*/     }
/*024:*/   }
/*025:*/   abort();
/*026:*/   return input;
/*027:*/ }
/*028:*/ 
/*029:*/ void __describerr_mark_error()
/*030:*/ {
/*031:*/   __describerr_reached_error_once = 1;
/*032:*/   __describerr_abort(0);
/*033:*/ }
/*034:*/ 
/*035:*/ int __INSTR_INT_MAX = 2147483647;
/*036:*/ int __INSTR_INT_MIN = - 2147483648;
/*037:*/ long long __INSTR_LONG_LONG_MIN = - 9223372036854775807;
/*038:*/ long long __INSTR_LONG_LONG_MAX = 9223372036854775807;
/*039:*/ extern int __VERIFIER_nondet_int(void);
/*040:*/ extern void __assert_fail_INSTR(const char *, const char *, unsigned int, const char *) __attribute__((__nothrow__, __leaf__, __noreturn__));
/*041:*/ void __VERIFIER_assert(int cond)
/*042:*/ {
/*043:*/   if (! cond)
/*044:*/   {
/*045:*/     ERROR:
/*046:*/     reach_error();
/*047:*/ 
/*048:*/   }
/*049:*/   return;
/*050:*/ }
/*051:*/ 
/*052:*/ extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__((__nothrow__, __leaf__, __noreturn__));
/*053:*/ void reach_error_INSTR()
/*054:*/ {
/*055:*/   __assert_fail("0", "terminator_02-1.c", 3, "reach_error");
/*056:*/ }
/*057:*/ 
/*058:*/ void __VERIFIER_assert_INSTR(int cond)
/*059:*/ {
/*060:*/   if (! cond)
/*061:*/   {
/*062:*/     ERROR:
/*063:*/     {
/*064:*/       abort();
/*065:*/       abort();
/*066:*/     }
/*067:*/ 
/*068:*/   }
/*069:*/   return;
/*070:*/ }
/*071:*/ 
/*072:*/ int __VERIFIER_nondet_int();
/*073:*/ _Bool __VERIFIER_nondet_bool();
/*074:*/ int main()
/*075:*/ {
/*076:*/   int x = __VERIFIER_nondet_int();
/*077:*/   int y = __VERIFIER_nondet_int();
/*078:*/   int z = __VERIFIER_nondet_int();
/*079:*/   while ((x < 100) && (100 < z))
/*080:*/   {
/*081:*/     _Bool tmp = __VERIFIER_nondet_bool();
/*082:*/     if (tmp)
/*083:*/     {
/*084:*/       if (1 && 1)
/*085:*/       {
/*086:*/         __VERIFIER_assert(! (((1 > 0) && (x > (__INSTR_INT_MAX - 1))) || ((1 < 0) && (x < (__INSTR_INT_MIN - 1)))));
/*087:*/       }
/*088:*/       if (1)
/*089:*/       {
/*090:*/         __VERIFIER_assert(! (((1 > 0) && (x > (__INSTR_INT_MAX - 1))) || ((1 < 0) && (x < (__INSTR_INT_MIN - 1)))));
/*091:*/       }
/*092:*/       x++;
/*093:*/     }
/*094:*/     else
/*095:*/     {
/*096:*/       if (1 && 1)
/*097:*/       {
/*098:*/         __VERIFIER_assert(! (((1 > 0) && (x < (__INSTR_INT_MIN + 1))) || ((1 < 0) && (x > (__INSTR_INT_MAX + 1)))));
/*099:*/       }
/*100:*/       if (1)
/*101:*/       {
/*102:*/         __VERIFIER_assert(! (((1 > 0) && (x < (__INSTR_INT_MIN + 1))) || ((1 < 0) && (x > (__INSTR_INT_MAX + 1)))));
/*103:*/       }
/*104:*/       x--;
/*105:*/       if (1 && 1)
/*106:*/       {
/*107:*/         __VERIFIER_assert(! (((1 > 0) && (z < (__INSTR_INT_MIN + 1))) || ((1 < 0) && (z > (__INSTR_INT_MAX + 1)))));
/*108:*/       }
/*109:*/       if (1)
/*110:*/       {
/*111:*/         __VERIFIER_assert(! (((1 > 0) && (z < (__INSTR_INT_MIN + 1))) || ((1 < 0) && (z > (__INSTR_INT_MAX + 1)))));
/*112:*/       }
/*113:*/       z--;
/*114:*/     }
/*115:*/   }
/*116:*/ 
/*117:*/   __VERIFIER_assert_INSTR(0);
/*118:*/   return 0;
/*119:*/ }
/*120:*/ 
