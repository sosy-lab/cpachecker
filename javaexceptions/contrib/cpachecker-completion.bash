#!/bin/bash

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

#   README:
# This script provides bash completion (the thing that happens when you press
# the TAB key) for CPAchecker (more specifically, the `cpa.sh` executable).
# This script is designed for use with the bash shell, but also works with zsh.
# To check your current shell, execute `echo $BASH`. If some path
# to the bash command is displayed, you are using bash.
#
#   Installation for bash:
# To enable the bash completion, you have two options:
# 1) For temporary use, source the file: `source cpachecker-completion.bash`
# 2) For persistent use, create a symlink to this file in directory
#    `/etc/bash_completion.d/`
#    or put the line `source $DIR_TO_THIS_SCRIPT` into your `.bashrc`
#    (with $DIR_TO_THIS_SCRIPT replaced with the location of this file)
#
#    Installation for zsh:
# Run the following commands (for persistent use, put them in your ~/.zshrc):
#   ```
#   autoload bashcompinit
#   bashcompinit
#   source $DIR_TO_THIS_SCRIPT
#   ```
# (with $DIR_TO_THIS_SCRIPT replaced with the location of this file)
#
#
#   Trying it out:
# From the CPAchecker directory, type `scripts/cpa.sh -` and press
# the TAB key. Bash will provide you with all possible command line options
# for CPAchecker.

_cpachecker_completions() {
    local prev="${COMP_WORDS[COMP_CWORD-1]}"
    local current="${COMP_WORDS[COMP_CWORD]}"
    local cpachecker_dir="$(dirname $(which "$1"))/.."
    if [[ "$prev" == "-spec" ]]; then
        local spec_dir=${cpachecker_dir}/config/specification
        if [[ -e "$spec_dir" ]]; then
            local specs=$(find "$spec_dir" -maxdepth 1 -name '*.spc' -printf '%f\n' | rev | cut -d"." -f2- | rev)
        fi

        COMPREPLY+=($(compgen -W "${specs}" -- "$current"))

    elif [[ "$current" == "-"* ]]; then
        local config_dir=${cpachecker_dir}/config
        local params="-32 -64 -benchmark -cbmc -cmc -config -cp -classpath -cpas -entryfunction -h -help -java -logfile -nolog -noout -outputpath -preprocess -printOptions -printUsedOptions -secureMode -setprop -skipRecursion -sourcepath -spec -stats -timelimit -witness"

        if [[ -e $config_dir ]]; then
            params="$params $(find $config_dir -maxdepth 1 -name '*.properties' -printf '-%f\n' | rev | cut -d"." -f2- | rev)"
        fi

        COMPREPLY+=($(compgen -W "${params}" -- "$current"))
    fi
}

complete -o default -F _cpachecker_completions cpa.sh
