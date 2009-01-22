#!/usr/bin/perl -w

use strict;

my $in_input = 0;
my $in_counter = 0;
my $line = "";
my %input_values = ();

sub process_inputs () {
  my ($input_id, $input_line) = @_;
  # print "INPUT[$input_id]: $input_line\n";
  defined $input_values{$input_id} and die "Input $input_id is already known\n";
  $input_values{$input_id} = ();

  my $nesting = 0;
  my $var = undef;
  my $val = undef;
  foreach my $tok (split(/\s+/, $input_line)) {
    die "Failed to parse!" if ($nesting > 0 && !defined($var));
    if ($tok =~ /^\},?$/ ) {
      ($nesting > 0) or die "Invalid nesting!";
      $nesting--;
    } elsif ($tok =~ /\{$/) {
      $nesting++;
    }

    if (!defined($var)) {
      ($var, $val) = split('=', $tok); 
    } else {
      defined($val) or die "Unexpected token!";
      $val .= " $tok";
    }

    if ($nesting == 0) {
      $input_values{$input_id}{$var} = $val;
      $var = undef;
      $val = undef;
    }
  }
  ($nesting == 0) or die "Number of { does not match }, missing $nesting }\n";
}

while (<>) {
  if ( /^(feasible:)?\s+IN:/ ) {
    if ($line ne "") {
      &process_inputs($in_counter, $line);
      $in_counter++;
    }
    $in_input = 1;
    chomp;
    s/^(feasible:)?\s+IN:\s*//;
    $line = $_;
  } elsif ( $in_input == 1 && (
      /^\s+OUT:/ || /^Processing the query took/ || /^FORTAS>/ ) ) {
    if ($line ne "") {
      &process_inputs($in_counter, $line);
      $in_counter++;
    }
    $in_input = 0;
    $line = "";
  } elsif ( $in_input == 1 ) {
    chomp;
    s/^\s+//;
    $line = "$line $_";
  }
}

my %global_decls = ();
my $driver_defs = "";

foreach my $id (sort keys %input_values) {
  my $local_decls = "";
  my $assignments = "";
 
  foreach my $var (keys %{ $input_values{$id} }) {
    my $val = $input_values{$id}{$var};
    # print "$id: $var = $val\n";
    if ($var =~ /^([^:]+)::(\d+)::([^:]+)$/) {
      my $gvar = "_g_$1_$2_$3";
      my $lvar = "_l_$1_$2_$3";
      defined($global_decls{$var}) or
        $global_decls{$var} = {
          name => $gvar,
          type => "type_of_$var"
        };
      $local_decls .= "\ttypeof($gvar) $lvar = $val;\n";
      $assignments .= "\tmemcpy(&$gvar, &$lvar, sizeof($gvar));\n";
    } else {
      $local_decls .= "\ttypeof($var) _l_$var = $val;\n";
      $assignments .= "\tmemcpy(&$var, &_l_$var, sizeof($var));\n";
    }
  }

  $driver_defs .= "int test_driver_$id(int argc, char* argv[]) {\n";
  $driver_defs .= "$local_decls\n";
  $driver_defs .= "$assignments\n";
  $driver_defs .= "\treturn orig_main(argc, argv);\n";
  $driver_defs .= "}\n\n";
}

print "#include <assert.h>\n";
print "#include <string.h>\n";
print "#include <stdlib.h>\n";
print "#include <errno.h>\n";
print "#include <stdio.h>\n";
print "\n";
print "#error You need to lookup all the types and add proper initializers to the functions\n";
foreach my $var (keys %global_decls) {
  print $global_decls{$var}{type} . " " . $global_decls{$var}{name} . ";\n";
}
print "\n";
print $driver_defs;

print <<EOF;
int main(int argc, char* argv[]) {
  long driver = -1;
  assert(argc >= 2);
  errno = 0;
  driver = strtol(argv[1], NULL, 10);
  assert(errno == 0);
  argv[1] = argv[0];
  --argc; ++argv;

  switch(driver) {
EOF

foreach my $id (sort keys %input_values) {
  print "  \tcase $id:\n";
  print "  \t\treturn test_driver_$id(argc, argv);\n";
}

print <<EOF;
    default:
      printf("No test driver available for id %ld\\n", driver);
      assert(0);
  }

  return -1;
}
EOF

