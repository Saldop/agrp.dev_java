#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.3.2
# ----------------------------------------------------------------------------

set -euf
[ "${MVNW_VERBOSE-}" != true ] || set -x

# Resolve the script location
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="${SCRIPT_DIR}/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="${SCRIPT_DIR}/.mvn/wrapper/maven-wrapper.properties"

# Read wrapperUrl from properties file
WRAPPER_URL=""
if [ -f "${WRAPPER_PROPERTIES}" ]; then
  WRAPPER_URL="$(grep '^wrapperUrl=' "${WRAPPER_PROPERTIES}" | cut -d'=' -f2-)"
fi

# Download the wrapper JAR if not present
if [ ! -f "${WRAPPER_JAR}" ]; then
  if [ -z "${WRAPPER_URL}" ]; then
    echo "Error: .mvn/wrapper/maven-wrapper.jar not found and wrapperUrl not set in maven-wrapper.properties" >&2
    exit 1
  fi
  echo "Downloading maven-wrapper.jar from: ${WRAPPER_URL}"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL -o "${WRAPPER_JAR}" "${WRAPPER_URL}" || { echo "Failed to download ${WRAPPER_URL}" >&2; exit 1; }
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "${WRAPPER_JAR}" "${WRAPPER_URL}" || { echo "Failed to download ${WRAPPER_URL}" >&2; exit 1; }
  else
    echo "Error: neither curl nor wget is available to download the Maven wrapper jar" >&2
    exit 1
  fi
fi

# Find java executable
if [ -n "${JAVA_HOME-}" ]; then
  JAVACMD="${JAVA_HOME}/bin/java"
else
  JAVACMD="$(command -v java)"
fi

if [ ! -x "${JAVACMD}" ]; then
  echo "Error: JAVA_HOME is not set and no java command could be found in PATH." >&2
  exit 1
fi

MAVEN_OPTS="${MAVEN_OPTS-} -Dmaven.multiModuleProjectDirectory=${SCRIPT_DIR}"

exec "${JAVACMD}" \
  ${MAVEN_OPTS} \
  -classpath "${WRAPPER_JAR}" \
  org.apache.maven.wrapper.MavenWrapperMain \
  "$@"
