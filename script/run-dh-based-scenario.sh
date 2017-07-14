#!/bin/bash

FIRST_TIME=$1
LAST_TIME=$2
DH_MODEL=$3
MAX_CAPACITY=$4
CONSTRAINT=$5
CPU_GRAIN=$6
RAM_GRAIN=$7
GRAIN_LABEL=$8

CONF_FILE="simulation-"$DH_MODEL"-"$GRAIN_LABEL"-"$FIRST_TIME".conf"

for i in `seq $FIRST_TIME $LAST_TIME`; do
    echo "Time $i"

    # configuring constraint on blade model with large grain
    cp default-dh-simulator.conf $CONF_FILE

    sed -i -e 's/\$infra_path\$/data\/machine-input.txt/g' $CONF_FILE
    sed -i -e 's/\$constraint_on\$/'"$CONSTRAINT"'/g' $CONF_FILE
    sed -i -e 's/\$mem_grain\$/'"$RAM_GRAIN"'/g' $CONF_FILE
    sed -i -e 's/\$cpu_grain\$/'"$CPU_GRAIN"'/g' $CONF_FILE
    sed -i -e 's/\$out_dir\$/dh-based-results\/time'"$i"'\/'"$GRAIN_LABEL"'\//g' $CONF_FILE
    sed -i -e 's/\$dh_model\$/'"$DH_MODEL"'/g' $CONF_FILE
    sed -i -e 's/\$max_mem_capacity\$/'"$MAX_CAPACITY"'/g' $CONF_FILE
    sed -i -e 's/\$max_cpu_capacity\$/'"$MAX_CAPACITY"'/g' $CONF_FILE

    sed -i -e 's/\$workload_path\$/timestamps\/all-tasks-time-'"$i"'-for-java.csv/g' $CONF_FILE

    # command
    java -cp borg-simulator.jar org.cloudish.dh.DHMainExecutor $CONF_FILE > log-$DH_MODEL-$CONSTRAINT-$GRAIN_LABEL-time"$i".out 2> log-$DH_MODEL-$CONSTRAINT-$GRAIN_LABEL-time"$i".err
#    cat $CONF_FILE
    rm $CONF_FILE
done
