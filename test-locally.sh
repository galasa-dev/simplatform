#! /usr/bin/env bash 

#
# Copyright contributors to the Galasa project
#
# SPDX-License-Identifier: EPL-2.0
#
#-----------------------------------------------------------------------------------------                   
#
# Objectives: Run the Simbank tests locally.
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

function checkSimBankTestAreBuilt {
    SIMBANK_TESTS_PATH=$(ls ~/.m2/repository/dev/galasa/dev.galasa.simbank.tests/${SIMBANK_VERSION}/dev.galasa.simbank.tests-${SIMBANK_VERSION}.jar 2>/dev/null)

    if [ -f "$SIMBANK_TESTS_PATH" ]; then
        info "Found dev.galasa.simbank.tests in local Maven repo: $SIMBANK_TESTS_PATH"
    else
        error "dev.galasa.simbank.tests not found in local Maven repo." >&2
        exit 1
    fi
}

function checkSimBankAppIsBuilt {
    SIMBANK_APP_PATH=$(ls ~/.m2/repository/dev/galasa/galasa-simplatform/${SIMBANK_VERSION}/galasa-simplatform-${SIMBANK_VERSION}.jar 2>/dev/null)

    if [ -f "$SIMBANK_APP_PATH" ]; then
        info "Found galasa-simplatform in local Maven repo: $SIMBANK_APP_PATH"
    else
        error "galasa-simplatform not found in local Maven repo." >&2
        exit 1
    fi
}

#-----------------------------------------------------------------------------------------                   
# Test methods.
#-----------------------------------------------------------------------------------------                   
function runSimBankIVT {
    cmd="galasactl runs submit local \
    --obr mvn:dev.galasa/dev.galasa.simbank.obr/${TEST_OBR_VERSION}/obr \
    --obr mvn:dev.galasa/dev.galasa.uber.obr/${TEST_OBR_VERSION}/obr \
    --class dev.galasa.simbank.tests/dev.galasa.simbank.tests.SimBankIVT \
    --log ${BASEDIR}/temp/simbank-test-log.txt"

    info "Command is ${cmd}"

    $cmd
    rc=$?
    if [[ "${rc}" != "0" ]]; then
        error "Command to run tests failed. rc=${rc}. Log is at ${BASEDIR}/temp/log.txt"
        exit 1
    fi

    success "Ran SimBankIVT test OK"
}

function runBasicAccountCreditTest {
    cmd="galasactl runs submit local \
    --obr mvn:dev.galasa/dev.galasa.simbank.obr/${TEST_OBR_VERSION}/obr \
    --obr mvn:dev.galasa/dev.galasa.uber.obr/${TEST_OBR_VERSION}/obr \
    --class dev.galasa.simbank.tests/dev.galasa.simbank.tests.BasicAccountCreditTest \
    --log ${BASEDIR}/temp/simbank-test-log.txt"

    info "Command is ${cmd}"

    $cmd
    rc=$?
    if [[ "${rc}" != "0" ]]; then
        error "Command to run tests failed. rc=${rc}. Log is at ${BASEDIR}/temp/log.txt"
        exit 1
    fi

    success "Ran BasicAccountCreditTest test OK"
}

function runProvisionedAccountCreditTests {
    cmd="galasactl runs submit local \
    --obr mvn:dev.galasa/dev.galasa.simbank.obr/${TEST_OBR_VERSION}/obr \
    --obr mvn:dev.galasa/dev.galasa.uber.obr/${TEST_OBR_VERSION}/obr \
    --class dev.galasa.simbank.tests/dev.galasa.simbank.tests.ProvisionedAccountCreditTests \
    --log ${BASEDIR}/temp/simbank-test-log.txt"

    info "Command is ${cmd}"

    $cmd
    rc=$?
    if [[ "${rc}" != "0" ]]; then
        error "Command to run tests failed. rc=${rc}. Log is at ${BASEDIR}/temp/log.txt"
        exit 1
    fi

    success "Ran ProvisionedAccountCreditTests test OK"
}


#-----------------------------------------------------------------------------------------                   
# Main logic.
#-----------------------------------------------------------------------------------------                   
source_dir="."

project=$(basename ${BASEDIR})
SIMBANK_VERSION="0.43.0"

checkGalasaCtlAvailable
checkSimBankTestAreBuilt
checkSimBankAppIsBuilt

mkdir -p ${BASEDIR}/temp
cd ${BASEDIR}/temp

# Start the Simbank back-end server application in a background process...
h1 "Running Simbank back-end server application (version ${SIMBANK_VERSION}) ..."

java -jar ~/.m2/repository/dev/galasa/galasa-simplatform/${SIMBANK_VERSION}/galasa-simplatform-${SIMBANK_VERSION}.jar > ${BASEDIR}/temp/simbank-app-log.txt 2>&1 &

success "Simbank application started OK."

TEST_OBR_VERSION="0.43.0"

h1 "Running Simbank tests"

runSimBankIVT
runBasicAccountCreditTest
runProvisionedAccountCreditTests

success "All SimBank tests Passed."