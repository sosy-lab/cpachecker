int __SELECTED_FEATURE_line_13;
int __SELECTED_FEATURE_line_18;
int __SELECTED_FEATURE_line_9;
int root;
int __SELECTED_FEATURE_line_20;
#ifndef LOCAL
extern int __VERIFIER_nondet_int();
#endif

extern int __VERIFIER_nondet_int();
int __SELECTED_FEATURE_mutant_1;
int __SELECTED_FEATURE_mutant_2;
int __SELECTED_FEATURE_mutant_3;
int __SELECTED_FEATURE_mutant_4;
int __SELECTED_FEATURE_mutant_5;

int gcd (int x  , int y ) {

int r = 0;

int a;

int b;

if(x > y) {

if(((__SELECTED_FEATURE_mutant_1) || (__SELECTED_FEATURE_mutant_2) || (__SELECTED_FEATURE_mutant_4) || (__SELECTED_FEATURE_mutant_5)) && !((__SELECTED_FEATURE_mutant_3))) {

a = x;

VP_229:

;

if(!((__SELECTED_FEATURE_mutant_4))) {

if(!((__SELECTED_FEATURE_mutant_2))) {

if((__SELECTED_FEATURE_mutant_5)) {

if(!(x != y)) {

PS_236:

;

b = y;

}

else {

PS_237:

;

b = x;

}

r = b;

VP_214:

;

if(!((__SELECTED_FEATURE_mutant_4))) {

if(!((__SELECTED_FEATURE_mutant_3))) {

if(!((__SELECTED_FEATURE_mutant_2))) {

if((__SELECTED_FEATURE_mutant_5)) {

if((a % b) != 0) {

VP_198:

;

if(((__SELECTED_FEATURE_mutant_1) || (__SELECTED_FEATURE_mutant_3) || (__SELECTED_FEATURE_mutant_5)) && !((__SELECTED_FEATURE_mutant_4))) {

PS_238:

;

r = a % b;

PS_235:

;

a = b;

b = r;

goto VP_214;

}

else {

r = a * b;

goto PS_235;

}

}

else {

PS_234:

;

return r;

}

}

else {

if((a % b) != 0) {

goto VP_198;

}

else {

goto PS_234;

}

}

}

else {

if((a % b) != 0) {

goto PS_234;

}

else {

goto PS_238;

}

}

}

else {

if((a % b) != 0) {

goto VP_198;

}

else {

goto PS_234;

}

}

}

else {

if((a % b) != 0) {

goto VP_198;

}

else {

goto PS_234;

}

}

}

else {

if(x >= y) {

goto PS_237;

}

else {

goto PS_236;

}

}

}

else {

if(x < y) {

goto PS_237;

}

else {

goto PS_236;

}

}

}

else {

if(!(x < y)) {

goto PS_236;

}

else {

goto PS_237;

}

}

}

else {

a = y;

N58:

;

if(!(x < y)) {

goto PS_236;

}

else {

goto PS_237;

}

}

}

else {

if(((__SELECTED_FEATURE_mutant_3)) && !((__SELECTED_FEATURE_mutant_1) || (__SELECTED_FEATURE_mutant_2) || (__SELECTED_FEATURE_mutant_4) || (__SELECTED_FEATURE_mutant_5))) {

a = x;

goto N58;

}

else {

a = y;

goto VP_229;

}

}

}

extern int input(); 

int main() {



__SELECTED_FEATURE_mutant_1 = __VERIFIER_nondet_int();
__SELECTED_FEATURE_mutant_2 = __VERIFIER_nondet_int();
__SELECTED_FEATURE_mutant_3 = __VERIFIER_nondet_int();
__SELECTED_FEATURE_mutant_4 = __VERIFIER_nondet_int();
__SELECTED_FEATURE_mutant_5 = __VERIFIER_nondet_int();

if(__SELECTED_FEATURE_mutant_3 || __SELECTED_FEATURE_mutant_4 || __SELECTED_FEATURE_mutant_5 || __SELECTED_FEATURE_mutant_1 || __SELECTED_FEATURE_mutant_2){
int x = input(); 
int y = input(); 


gcd(x  , y );

}
else{
}
return 0;

}


int gcd_mutation_0 (int x  , int y ) {
int r = 0 ;
int a ;
int b ;
if ( x > y ) {
if ( ( ( __SELECTED_FEATURE_mutant_1 ) || ( __SELECTED_FEATURE_mutant_2 ) || ( __SELECTED_FEATURE_mutant_4 ) || ( __SELECTED_FEATURE_mutant_5 ) ) && ! ( ( __SELECTED_FEATURE_mutant_3 ) ) ) {
a = x ;
VP_229:;
if ( ! ( ( __SELECTED_FEATURE_mutant_4 ) ) ) {
if ( ! ( ( __SELECTED_FEATURE_mutant_2 ) ) ) {
if ( ( __SELECTED_FEATURE_mutant_5 ) ) {
if ( ! ( x != y ) ) {
PS_236:;
b = y ;
}
else {
PS_237:;
b = x ;
}

r = b ;
VP_214:;
if ( ! ( ( __SELECTED_FEATURE_mutant_4 ) ) ) {
if ( ! ( ( __SELECTED_FEATURE_mutant_3 ) ) ) {
if ( ! ( ( __SELECTED_FEATURE_mutant_2 ) ) ) {
if ( ( __SELECTED_FEATURE_mutant_5 ) ) {
MUTATION /** ORRN **/:if ( ( a % b ) >= 0 ) {
VP_198:;
if ( ( ( __SELECTED_FEATURE_mutant_1 ) || ( __SELECTED_FEATURE_mutant_3 ) || ( __SELECTED_FEATURE_mutant_5 ) ) && ! ( ( __SELECTED_FEATURE_mutant_4 ) ) ) {
PS_238:;
r = a % b ;
PS_235:;
a = b ;
b = r ;
goto VP_214 ;
}
else {
r = a * b ;
goto PS_235 ;
}

}
else {
PS_234:;
return r ;
}

}
else {
if ( ( a % b ) != 0 ) {
goto VP_198 ;
}
else {
goto PS_234 ;
}

}

}
else {
if ( ( a % b ) != 0 ) {
goto PS_234 ;
}
else {
goto PS_238 ;
}

}

}
else {
if ( ( a % b ) != 0 ) {
goto VP_198 ;
}
else {
goto PS_234 ;
}

}

}
else {
if ( ( a % b ) != 0 ) {
goto VP_198 ;
}
else {
goto PS_234 ;
}

}

}
else {
if ( x >= y ) {
goto PS_237 ;
}
else {
goto PS_236 ;
}

}

}
else {
if ( x < y ) {
goto PS_237 ;
}
else {
goto PS_236 ;
}

}

}
else {
if ( ! ( x < y ) ) {
goto PS_236 ;
}
else {
goto PS_237 ;
}

}

}
else {
a = y ;
N58:;
if ( ! ( x < y ) ) {
goto PS_236 ;
}
else {
goto PS_237 ;
}

}

}
else {
if ( ( ( __SELECTED_FEATURE_mutant_3 ) ) && ! ( ( __SELECTED_FEATURE_mutant_1 ) || ( __SELECTED_FEATURE_mutant_2 ) || ( __SELECTED_FEATURE_mutant_4 ) || ( __SELECTED_FEATURE_mutant_5 ) ) ) {
a = x ;
goto N58 ;
}
else {
a = y ;
goto VP_229 ;
}

}

}

int isValid(){
	 if ((root) && 
((root || !(__SELECTED_FEATURE_line_13 || __SELECTED_FEATURE_line_20 || __SELECTED_FEATURE_line_9 || __SELECTED_FEATURE_line_18)) && (!root || (__SELECTED_FEATURE_line_13 || __SELECTED_FEATURE_line_20 || __SELECTED_FEATURE_line_9 || __SELECTED_FEATURE_line_18))) && 
((__SELECTED_FEATURE_mutant_5 || !(!__SELECTED_FEATURE_mutant_1 && __SELECTED_FEATURE_line_13)) && (!__SELECTED_FEATURE_mutant_5 || (!__SELECTED_FEATURE_mutant_1 && __SELECTED_FEATURE_line_13))) && 
((__SELECTED_FEATURE_mutant_1 || !(!__SELECTED_FEATURE_mutant_5 && __SELECTED_FEATURE_line_13)) && (!__SELECTED_FEATURE_mutant_1 || (!__SELECTED_FEATURE_mutant_5 && __SELECTED_FEATURE_line_13))) && 
((__SELECTED_FEATURE_mutant_4 || !__SELECTED_FEATURE_line_20) && (!__SELECTED_FEATURE_mutant_4 || __SELECTED_FEATURE_line_20)) && 
((__SELECTED_FEATURE_mutant_3 || !__SELECTED_FEATURE_line_9) && (!__SELECTED_FEATURE_mutant_3 || __SELECTED_FEATURE_line_9)) && 
((__SELECTED_FEATURE_mutant_2 || !__SELECTED_FEATURE_line_18) && (!__SELECTED_FEATURE_mutant_2 || __SELECTED_FEATURE_line_18))){
		 return 1;
	 }
	return 0;
}

int main_run(){
	int x = __VERIFIER_nondet_int();
	int y = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_line_20 = __VERIFIER_nondet_int();
	root = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_mutant_4 = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_line_9 = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_mutant_3 = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_line_18 = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_mutant_5 = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_line_13 = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_mutant_2 = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_mutant_1 = __VERIFIER_nondet_int();

if (isValid()){ 
	int test_0_0 = gcd(x,y);
int test_0_0_mutation_0 = gcd_mutation_0(x,y);
if(test_0_0!=test_0_0_mutation_0){
		label_0_0: printf("label_0_0");
}
}}
