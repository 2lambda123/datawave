#!/usr/bin/env bash

source ~/.bashrc

# If DOCKER_HOST is defined update Accumulo and Hadoop bind hosts
if [ "${DOCKER_HOST}" != "localhost" ] ; then
   # Update Accumulo bind hosts
   sed -i'' -e "s/localhost/${DOCKER_HOST}/g" ${ACCUMULO_HOME}/conf/gc
   sed -i'' -e "s/localhost/${DOCKER_HOST}/g" ${ACCUMULO_HOME}/conf/masters
   sed -i'' -e "s/localhost/${DOCKER_HOST}/g" ${ACCUMULO_HOME}/conf/monitor
   sed -i'' -e "s/localhost/${DOCKER_HOST}/g" ${ACCUMULO_HOME}/conf/slaves
   sed -i'' -e "s/localhost/${DOCKER_HOST}/g" ${ACCUMULO_HOME}/conf/tracers

   # Create hadoop client configs
   mkdir -p ${HADOOP_HOME}/client/conf
   cp -r ${HADOOP_CONF_DIR}/*-site.xml ${HADOOP_HOME}/client/conf
   sed -i'' -e "s/${DW_BIND_HOST}/${DOCKER_HOST}/g" ${HADOOP_HOME}/client/conf/*-site.xml
fi

START_AS_DAEMON=true

START_ACCUMULO=false
START_WEB=false
START_TEST=false
START_INGEST=false

for arg in "$@"
do
    case "$arg" in
          --bash)
             START_AS_DAEMON=false
             ;;
          --accumulo)
             START_ACCUMULO=true
             ;;
          --web)
             START_WEB=true
             ;;
          --webdebug)
             START_WEB_DEBUG=true
             ;;
          --test)
             START_TEST=true
             START_AS_DAEMON=false
             ;;
          --ingest)
             START_INGEST=true
             ;;
          *)
             echo "Invalid argument passed to $( basename "$0" ): $arg"
             exit 1
    esac
done

[ "${START_INGEST}" == true ] && datawaveIngestStart

[ "${START_ACCUMULO}" == true ] && accumuloStart

[ "${START_WEB}" == true ] && datawaveWebStart

[ "${START_WEB_DEBUG}" == true ] && datawaveWebStart --debug

if [ "${START_TEST}" == true ] ; then
    datawaveWebStart
    status=$?

    if [ "$status" != "0" ] ; then
        echo "datawaveWebStart Failed"
        cat ${WILDFLY_HOME}/standalone/log/server.log
        exit $status
    else
        datawaveWebTest --blacklist-files QueryMetrics
        status=$?

        if [ "$status" != "0" ] ; then
            echo "datawaveWebTest Failed"
            cat ${WILDFLY_HOME}/standalone/log/server.log
        fi

        allStop
        exit $status
    fi
fi

if [ "${START_AS_DAEMON}" == true ] ; then
    while true; do sleep 1000; done
fi

exec /bin/bash
