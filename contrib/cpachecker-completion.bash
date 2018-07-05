#!/bin/bash
#
# CPAchecker is a tool for configurable software verification.
# This file is part of CPAchecker.
#
# Copyright (C) 2007-2018  Dirk Beyer
# All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# CPAchecker web page:
#   http://cpachecker.sosy-lab.org


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
    if [[ "$2" == "-"* ]]; then
        local cpachecker_dir="$(dirname $(which "$1"))/.."
        local config_dir=${cpachecker_dir}/config
        local params="-32 -64 -benchmark -cbmc -cmc -config -cp -classpath -cpas -entryfunction -h -help -java -logfile -nolog -noout -outputpath -preprocess -printOptions -printUsedOptions -secureMode -setprop -skipRecursion -sourcepath -spec -stats -timelimit -witness"

        if [[ -e $config_dir ]]; then
            params="$params $(find $config_dir -maxdepth 1 -name '*.properties' -printf '-%f\n' | rev | cut -d"." -f2- | rev)"
        fi

        COMPREPLY+=($(compgen -W "${params}" -- "$2"))
    fi
}

complete -o default -F _cpachecker_completions cpa.sh
