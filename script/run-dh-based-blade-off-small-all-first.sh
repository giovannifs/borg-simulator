#!/bin/bash

for i in `seq 1 14`; do
    echo "Time $i"

    # configuring constraint on and 1 minimal server
    cp default-dh-simulator.conf simulation-blade-off.conf

    sed -i -e 's/\$infra_path\$/data\/machine-input.txt/g' simulation-blade-off.conf
    sed -i -e 's/\$constraint_on\$/no/g' simulation-blade-off.conf
    sed -i -e 's/\$mem_grain\$/0.0625/g' simulation-blade-off.conf
    sed -i -e 's/\$cpu_grain\$/0.03125/g' simulation-blade-off.conf
    sed -i -e 's/\$out_dir\$/dh-based-results\/time'"$i"'\//g' simulation-blade-off.conf
    sed -i -e 's/\$dh_model\$/blade/g' simulation-blade-off.conf
    sed -i -e 's/\$max_mem_capacity\$/1/g' simulation-blade-off.conf
    sed -i -e 's/\$max_cpu_capacity\$/1/g' simulation-blade-off.conf

    sed -i -e 's/\$workload_path\$/timestamps\/all-tasks-time-'"$i"'-for-java.csv/g' simulation-blade-off.conf

    # command
    java -cp borg-simulator.jar org.cloudish.dh.DHMainExecutor simulation-blade-off.conf > log-blade-off-time"$i".out 2> log-blade-off-time"$i".err
done
