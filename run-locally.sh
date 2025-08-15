#! /usr/bin/env bash 

#
# Copyright contributors to the Galasa project
#
# SPDX-License-Identifier: EPL-2.0
#
#-----------------------------------------------------------------------------------------                   
#
# Objectives: Start the Simbank application
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
    info "Syntax: run-locally.sh [OPTIONS]"
    cat << EOF
Options are:
-h | --help : Display this help text
--server : Launch the back-end server 3270 application. Ctrl-C to end it.
--ui : Launch the web user interface application which talks to the back-end server. Ctrl-C to end it.
--tests : Launch the tests
Environment Variables:
None


EOF
}

#-----------------------------------------------------------------------------------------                   
# Process parameters
#-----------------------------------------------------------------------------------------                   
export build_type=""
export is_server=false
export is_ui=false


while [ "$1" != "" ]; do
    case $1 in
        -h | --help )           usage
                                exit
                                ;;
        --server )              is_server=true
                                ;;
        --ui )                  is_ui=true
                                ;;
        --tests )               is_tests=true
                                ;;
        * )                     error "Unexpected argument $1"
                                usage
                                exit 1
    esac
    shift
done

if [[ "$is_ui" == false ]] && [[ "$is_server" == false  ]] && [[ "$is_tests" == false ]] ; then
    error "Not enough parameters."
    usage 
    exit 1
fi

if [[ "$is_ui" == true ]]  && [[ "$is_server" == true  ]] && [[ "$is_tests" == true  ]]; then
    error "Too many parameters. Either the --server, --ui or --tests parameter is needed, not both."
    usage
    exit 1
fi

#-----------------------------------------------------------------------------------------                   
# Main logic.
#-----------------------------------------------------------------------------------------                   

SIMBANK_VERSION="0.44.0"

function run_server {
    h1 "Running Simbank back-end server application (version ${SIMBANK_VERSION}) ..."
    info "Use Ctrl-C to stop it.\n"

    java -jar ~/.m2/repository/dev/galasa/galasa-simplatform/${SIMBANK_VERSION}/galasa-simplatform-${SIMBANK_VERSION}.jar
    rc=$?
    if [[ "${rc}" != "130" ]]; then
        error "Failed. Exit code was ${rc}"
        exit 1
    fi
    info "Passed. Exit code was $rc. (130 means user killed the process)"
    success "Ran Simbank application OK"
}

function run_ui {
    h1 "Running Simbank web user interface application (version ${SIMBANK_VERSION}) ..."
    info "Use Ctrl-C to stop it.\n"
    container_id=$(docker run --rm -p 8080:8080 -d galasa-simplatform-webapp)
    info "Launch the Simbank web UI here: http://localhost:8080/galasa-simplatform-webapp/index.html"
    docker attach ${container_id}
}

function run_tests {
    h1 "Running Simbank tests ..."
    
    cmd="galasactl runs submit local --log - \
    --obr mvn:dev.galasa.example.banking/dev.galasa.example.banking.obr/0.0.1-SNAPSHOT/obr \
    --class dev.galasa.example.banking.account/dev.galasa.example.banking.account.TestAccount \
    "

    info "Running this command: $cmd"
    $cmd
    rc=$?
    if [[ "${rc}" != "0" ]]; then
        error "Failed to run the tests. Exit code was ${rc}"
        exit 1
    fi
    success "Tests ran OK"
}


if [[ "$is_server" == true ]]; then 
    run_server 
fi

if [[ "$is_ui" == true ]]; then 
    run_ui 
fi

if [[ "$is_tests" == true ]]; then 
    run_tests 
fi