#!/usr/bin/env bash
#@REM ************************************************************************************
#@REM Description: run BattleshipGames Client
#@REM Author: Rui S. Moreira (adapted by João Santos)
#@REM ************************************************************************************
source ./setenv.sh client

cd ${ABSPATH2CLASSES}

${JDK}/bin/java -cp "${CLASSPATH}" \
     -Djava.rmi.server.codebase=${SERVER_CODEBASE} \
     -Djava.rmi.server.hostname=${SERVER_RMI_HOST} \
     ${CLIENT_CLASS} --servers ${REGISTRY_HOST}:${REGISTRY_PORT} ${REGISTRY_HOST}:${REGISTRY_PORT_B}

cd ${JAVAPROJ}/${JAVASCRIPTSPATH}
