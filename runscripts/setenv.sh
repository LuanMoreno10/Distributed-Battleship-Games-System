#!/usr/bin/env bash
#@REM ************************************************************************************
#@REM Description: set environment variables for Distributed Battleship Games System
#@REM Author: Rui S. Moreira (adapted by João Santos)
#@REM ************************************************************************************

#@REM ======================== Use Shell Parameters ========================
export SCRIPT_ROLE=$1

#@REM =====================================================================================================
#@REM ======================== CHANGE BELOW ACCORDING YOUR PROJECT and PC SETTINGS ========================
#@REM =====================================================================================================
#@REM ==== PC STUFF ====
export JDK=/usr/lib/jvm/jdk-23.0.2-oracle-x64
export USERNAME=joaosantos

#@REM ==== JAVA NAMING STUFF ====
export JAVAPROJ_NAME=Distributed-Battleship-Games-System
export JAVAPROJ=/home/${USERNAME}/Documents/Faculdade/3º/2º/${JAVAPROJ_NAME}

export PACKAGE_PREFIX=edu.ufp.inf.sd.battleshipgame
export PACKAGE_PREFIX_FOLDERS=edu/ufp/inf/sd/battleshipgame

export SERVER_CLASS=${PACKAGE_PREFIX}.Server.ServerApp
export CLIENT_CLASS=${PACKAGE_PREFIX}.Client.ClientApp

#@REM ==== NETWORK STUFF ====
export MYLOCALIP=localhost
export REGISTRY_HOST=${MYLOCALIP}
export REGISTRY_PORT=1099
export REGISTRY_PORT_B=1100
export SERVER_RMI_HOST=${REGISTRY_HOST}
export SERVER_CODEBASE_HOST=${SERVER_RMI_HOST}
export SERVER_CODEBASE_PORT=8000

#@REM =====================================================================================================
#@REM ======================== DO NOT CHANGE AFTER THIS POINT =============================================
#@REM =====================================================================================================
export JAVAPROJ_CLASSES=target/classes
export JAVAPROJ_DEP=target/dependency

export JAVAPROJ_CLASSES_FOLDER=${JAVAPROJ}/${JAVAPROJ_CLASSES}
export JAVAPROJ_DEP_FOLDER=${JAVAPROJ}/${JAVAPROJ_DEP}

export ABSPATH2CLASSES=${JAVAPROJ_CLASSES_FOLDER}
export ABSPATH2SRC=${JAVAPROJ}/src
export JAVASCRIPTSPATH=runscripts

export CLASSPATH=${JAVAPROJ_CLASSES_FOLDER}:${JAVAPROJ_DEP_FOLDER}/*

export SERVER_CODEBASE=http://${SERVER_CODEBASE_HOST}:${SERVER_CODEBASE_PORT}/${JAVAPROJ_CLASSES}/

export PATH=${PATH}:${JDK}/bin
