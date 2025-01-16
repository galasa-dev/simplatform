#! /usr/bin/env bash 

#
# Copyright contributors to the Galasa project
#
# SPDX-License-Identifier: EPL-2.0
#
#-----------------------------------------------------------------------------------------                   
#
# Objectives: Build this repository code locally.
# 
#-----------------------------------------------------------------------------------------                   

# Where is this script executing from ?
BASEDIR=$(dirname "$0");pushd $BASEDIR 2>&1 >> /dev/null ;BASEDIR=$(pwd);popd 2>&1 >> /dev/null
# echo "Running from directory ${BASEDIR}"
export ORIGINAL_DIR=$(pwd)
# cd "${BASEDIR}"

cd "${BASEDIR}/.."
WORKSPACE_DIR=$(pwd)


#-----------------------------------------------------------------------------------------                   
#
# Set Colors
#
#-----------------------------------------------------------------------------------------                   
bold=$(tput bold)
underline=$(tput sgr 0 1)
reset=$(tput sgr0)
red=$(tput setaf 1)
green=$(tput setaf 76)
white=$(tput setaf 7)
tan=$(tput setaf 202)
blue=$(tput setaf 25)

#-----------------------------------------------------------------------------------------                   
#
# Headers and Logging
#
#-----------------------------------------------------------------------------------------                   
underline() { printf "${underline}${bold}%s${reset}\n" "$@" ;}
h1() { printf "\n${underline}${bold}${blue}%s${reset}\n" "$@" ;}
h2() { printf "\n${underline}${bold}${white}%s${reset}\n" "$@" ;}
debug() { printf "${white}%s${reset}\n" "$@" ;}
info() { printf "${white}➜ %s${reset}\n" "$@" ;}
success() { printf "${green}✔ %s${reset}\n" "$@" ;}
error() { printf "${red}✖ %s${reset}\n" "$@" ;}
warn() { printf "${tan}➜ %s${reset}\n" "$@" ;}
bold() { printf "${bold}%s${reset}\n" "$@" ;}
note() { printf "\n${underline}${bold}${blue}Note:${reset} ${blue}%s${reset}\n" "$@" ;}

#-----------------------------------------------------------------------------------------                   
# Functions
#-----------------------------------------------------------------------------------------                   
function usage {
    info "Syntax: test-locally.sh [OPTIONS]"
    cat << EOF
Options are:
-h | --help : Display this help text

Environment Variables:
None

EOF
}

#-----------------------------------------------------------------------------------------                   
# Process parameters
#-----------------------------------------------------------------------------------------                   
export build_type=""

while [ "$1" != "" ]; do
    case $1 in
        -h | --help )           usage
                                exit
                                ;;
        * )                     error "Unexpected argument $1"
                                usage
                                exit 1
    esac
    shift
done

function checkGalasaCtlAvailable {
    which galasactl
    rc=$?
    if [[ "${rc}" != "0" ]]; then 
        error  "The 'galasactl' tool is not available. Install the tool and try again. rc:$rc"
        exit 1
    fi
}

#-----------------------------------------------------------------------------------------                   
# Main logic.
#-----------------------------------------------------------------------------------------                   
source_dir="."

project=$(basename ${BASEDIR})
SIMBANK_VERSION="0.40.0"

h1 "Running Simbank application tests"

checkGalasaCtlAvailable

TEST_OBR_VERSION="0.40.0"

mkdir -p ${BASEDIR}/temp
cd ${BASEDIR}/temp

cmd="galasactl runs submit local \
--obr mvn:dev.galasa/dev.galasa.simbank.obr/${TEST_OBR_VERSION}/obr \
--class dev.galasa.simbank.tests/dev.galasa.simbank.tests.SIMBANKIVT \
--log ${BASEDIR}/temp/log.txt"

info "Command is ${cmd}"

$cmd
rc=$?
if [[ "${rc}" != "0" ]]; then
    error "Command to run tests failed. rc=${rc}. Log is at ${BASEDIR}/temp/log.txt"
    exit 1
fi

success "Ran Simbank test OK"