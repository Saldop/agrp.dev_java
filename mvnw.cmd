@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM
@REM Optional ENV vars:
@REM   JAVA_HOME - location of a JDK home dir, required when JAVA is not on PATH
@REM   MVNW_REPOURL - repo url base for downloading maven distribution
@REM   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
@REM   MVNW_VERBOSE - true: enable verbose log; others: silence the output
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0") ELSE SET "BASE_DIR=%__MVNW_ARG0_NAME__%"

@SET MAVEN_PROJECTBASEDIR=%BASE_DIR%
@SET WRAPPER_PROPERTIES=%BASE_DIR%.mvn\wrapper\maven-wrapper.properties
@SET DOWNLOAD_URL=
@SET WRAPPER_JAR=

@FOR /F "usebackq tokens=1,2 delims==" %%a IN ("%WRAPPER_PROPERTIES%") DO (
    @IF "%%a"=="distributionUrl" SET DISTRIBUTION_URL=%%b
    @IF "%%a"=="wrapperUrl" SET WRAPPER_JAR_URL=%%b
)

@SET HASH=%DISTRIBUTION_URL%
@SET MAVEN_USER_HOME=%USERPROFILE%\.m2
@SET MAVEN_WRAPPER_JAR=%MAVEN_USER_HOME%\wrapper\dists\%HASH%\maven-wrapper.jar

@IF NOT EXIST "%MAVEN_WRAPPER_JAR%" (
    @IF NOT "%WRAPPER_JAR_URL%"=="" (
        @ECHO Downloading maven-wrapper.jar from %WRAPPER_JAR_URL%
        @MKDIR "%MAVEN_WRAPPER_JAR%\.." 2>NUL
        @powershell -Command "&{"^
          "$webclient = new-object System.Net.WebClient;"^
          "if (-not ([string]::IsNullOrEmpty('%MVNW_USERNAME%') -and [string]::IsNullOrEmpty('%MVNW_PASSWORD%'))) {"^
          "$webclient.Credentials = new-object System.Net.NetworkCredential('%MVNW_USERNAME%', '%MVNW_PASSWORD%');"^
          "}"^
          "$webclient.DownloadFile('%WRAPPER_JAR_URL%', '%MAVEN_WRAPPER_JAR%')"^
          "}"
    ) ELSE (
        @ECHO Error: wrapperUrl not set and maven-wrapper.jar not found
        @EXIT /B 1
    )
)

@SET JAVA_HOME_SUFFIX=\bin\java.exe
@IF NOT "%JAVA_HOME%"=="" (
    @SET JAVACMD=%JAVA_HOME%%JAVA_HOME_SUFFIX%
) ELSE (
    @FOR /F "usebackq tokens=*" %%i IN (`where java 2^>NUL`) DO @SET JAVACMD=%%i
)

@IF "%JAVACMD%"=="" (
    @ECHO Error: JAVA_HOME is not set and no java command could be found in your PATH.
    @EXIT /B 1
)

@SET MAVEN_OPTS=%MAVEN_OPTS% -Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%

@"%JAVACMD%" ^
  %MAVEN_OPTS% ^
  -classpath "%MAVEN_WRAPPER_JAR%" ^
  org.apache.maven.wrapper.MavenWrapperMain ^
  %*
