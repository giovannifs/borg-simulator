#!/bin/bash

for i in `seq 1 14`; do
    echo "Time $i"

    # configuring constraint on and 1 minimal server
    cp default-dh-simulator.conf simulation-blade-on.conf

    sed -i -e 's/\$infra_path\$/data\/machine-input.txt/g' simulation-blade-on.conf
    sed -i -e 's/\$constraint_on\$/yes/g' simulation-blade-on.conf
    sed -i -e 's/\$mem_grain\$/0.0625/g' simulation-blade-on.conf
    sed -i -e 's/\$cpu_grain\$/0.03125/g' simulation-blade-on.conf
    sed -i -e 's/\$out_dir\$/dh-based-results\/time'"$i"'\//g' simulation-blade-on.conf
    sed -i -e 's/\$dh_model\$/blade/g' simulation-blade-on.conf
    sed -i -e 's/\$max_mem_capacity\$/1/g' simulation-blade-on.conf
    sed -i -e 's/\$max_cpu_capacity\$/1/g' simulation-blade-on.conf

    sed -i -e 's/\$workload_path\$/timestamps\/all-tasks-time-'"$i"'-for-java.csv/g' simulation-blade-on.conf

    # command
    java -cp borg-simulator.jar org.cloudish.dh.DHMainExecutor simulation-blade-on.conf > log-blade-on-time"$i".out 2> log-blade-on-time"$i".err
done
