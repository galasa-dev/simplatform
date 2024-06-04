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
underline() { printf "${underline}${bold}%s${reset}\n" "$@"
}
h1() { printf "\n${underline}${bold}${blue}%s${reset}\n" "$@"
}
h2() { printf "\n${underline}${bold}${white}%s${reset}\n" "$@"
}
debug() { printf "${white}[.] %s${reset}\n" "$@"
}
info()  { printf "${white}[➜] %s${reset}\n" "$@"
}
success() { printf "${white}[${green}✔${white}] ${green}%s${reset}\n" "$@"
}
error() { printf "${white}[${red}✖${white}] ${red}%s${reset}\n" "$@"
}
warn() { printf "${white}[${tan}➜${white}] ${tan}%s${reset}\n" "$@"
}
bold() { printf "${bold}%s${reset}\n" "$@"
}
note() { printf "\n${underline}${bold}${blue}Note:${reset} ${blue}%s${reset}\n" "$@"
}

#-----------------------------------------------------------------------------------------                   
# Functions
#-----------------------------------------------------------------------------------------                   
function usage {
    info "Syntax: build-locally.sh [OPTIONS]"
    cat << EOF
Options are:
-h | --help : Display this help text

Environment Variables:
SOURCE_MAVEN :
    Used to indicate where parts of the OBR can be obtained.
    Optional. Defaults to https://development.galasa.dev/main/maven-repo/obr/
     
LOGS_DIR :
    Controls where logs are placed. 
    Optional. Defaults to creating a new temporary folder

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


#-----------------------------------------------------------------------------------------                   
# Main logic.
#-----------------------------------------------------------------------------------------                   
source_dir="."

project=$(basename ${BASEDIR})
h1 "Building ${project}"


# Over-rode SOURCE_MAVEN if you want to build from a different maven repo...
if [[ -z ${SOURCE_MAVEN} ]]; then
    export SOURCE_MAVEN=https://development.galasa.dev/main/maven-repo/obr/
    info "SOURCE_MAVEN repo defaulting to ${SOURCE_MAVEN}."
    info "Set this environment variable if you want to over-ride this value."
else
    info "SOURCE_MAVEN set to ${SOURCE_MAVEN} by caller."
fi

# Create a temporary dir.
# Note: This bash 'spell' works in OSX and Linux.
if [[ -z ${LOGS_DIR} ]]; then
    export LOGS_DIR=$(mktemp -d 2>/dev/null || mktemp -d -t "galasa-logs")
    info "Logs are stored in the ${LOGS_DIR} folder."
    info "Over-ride this setting using the LOGS_DIR environment variable."
else
    mkdir -p ${LOGS_DIR} 2>&1 > /dev/null # Don't show output. We don't care if it already existed.
    info "Logs are stored in the ${LOGS_DIR} folder."
    info "Over-ridden by caller using the LOGS_DIR variable."
fi

info "Using source code at ${source_dir}"
cd ${BASEDIR}/${source_dir}
if [[ "${DEBUG}" == "1" ]]; then
    OPTIONAL_DEBUG_FLAG="-debug"
else
    OPTIONAL_DEBUG_FLAG="-info"
fi

log_file=${LOGS_DIR}/${project}.txt
info "Log will be placed at ${log_file}"
date > ${log_file}


function build_application_code {
    h1 "Building simplatform application using maven"
    cd ${BASEDIR}/galasa-simplatform-application
    mvn clean install
    rc=$?
    if [[ "${rc}" != "0" ]]; then 
        error "make clean install failed. rc=${rc}"
        exit 1
    fi
    success "OK"
}

function build_test_code {
    h1 "Building simbank tests using maven"
    cd ${BASEDIR}/galasa-simbank-tests
    mvn clean install
    rc=$?
    if [[ "${rc}" != "0" ]]; then 
        error "make clean install failed. rc=${rc}"
        exit 1
    fi
    success "OK"
}

function build_docker_image {
    h1 "Building docker image"
    cd $BASEDIR/galasa-simplatform-application/galasa-simplatform-webapp
    docker build --tag galasa-simplatform-webapp . 
    rc=$?
    if [[ "${rc}" != "0" ]]; then 
        error "Failed to create a docker image for the UI. rc=${rc}"
        exit 1
    fi
    success "OK"
}

build_application_code
build_test_code
build_docker_image