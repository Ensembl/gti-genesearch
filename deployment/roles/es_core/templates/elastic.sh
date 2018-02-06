#!/bin/bash --

JAVA_HOME={{ java_dir }}
export PATH=$JAVA_HOME/bin:$PATH

cd {{ es_dir }}
PID_FILE={{ es_pid_file }}

function stop_es {
    if [ -e "$PID_FILE" ]; then
	echo "Stopping ES (pid file $PID_FILE)" 1>&2
	kill -SIGTERM $(cat $PID_FILE)
	rm -f $PID_FILE
    else
	echo "ES not running" 1>&2
    fi  
}
function start_es {
    if [ -e "$PID_FILE" ]; then
	echo "ES still running (pid file $PID_FILE)" 1>&2
	exit 2
    fi
    ./bin/elasticsearch -p $PID_FILE -d  || {
	echo "Could not start ES" 1>&2
	exit 3
    }
}
action=$1
if [ "$action" == "start" ]; then
    start_es
elif [ "$action" == "stop" ]; then
    stop_es
elif [ "$action" == "restart" ]; then
    stop_es
    start_es
else
    echo "Usage: $0 start|stop|restart" 1>&2
    exit 1
fi
