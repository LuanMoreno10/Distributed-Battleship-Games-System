#!/usr/bin/env bash
#@REM ************************************************************************************
#@REM Description: run BattleshipGames Server (nó único, sem R5)
#@REM Author: Rui S. Moreira (adapted by João Santos)
#@REM ************************************************************************************
source ./setenv.sh server

cd ${ABSPATH2CLASSES}

${JDK}/bin/java -cp "${CLASSPATH}" \
     -Djava.rmi.server.codebase=${SERVER_CODEBASE} \
     -Djava.rmi.server.hostname=${SERVER_RMI_HOST} \
     ${SERVER_CLASS}

cd ${JAVAPROJ}/${JAVASCRIPTSPATH}
