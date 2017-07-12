#!/bin/bash

for i in `seq 15 28`; do
    echo "Time $i"

    # configuring constraint on blade model with large grain
    cp default-dh-simulator.conf resource-grain-simulation-large-grain-2.conf

    sed -i -e 's/\$infra_path\$/data\/machine-input.txt/g' resource-grain-simulation-large-grain-2.conf
    sed -i -e 's/\$constraint_on\$/yes/g' resource-grain-simulation-large-grain-2.conf
    sed -i -e 's/\$mem_grain\$/0.125/g' resource-grain-simulation-large-grain-2.conf
    sed -i -e 's/\$cpu_grain\$/0.0625/g' resource-grain-simulation-large-grain-2.conf
    sed -i -e 's/\$out_dir\$/dh-based-results\/time'"$i"'\/large-grain\//g' resource-grain-simulation-large-grain-2.conf
    sed -i -e 's/\$dh_model\$/blade/g' resource-grain-simulation-large-grain-2.conf
    sed -i -e 's/\$max_mem_capacity\$/1/g' resource-grain-simulation-large-grain-2.conf
    sed -i -e 's/\$max_cpu_capacity\$/1/g' resource-grain-simulation-large-grain-2.conf

    sed -i -e 's/\$workload_path\$/timestamps\/all-tasks-time-'"$i"'-for-java.csv/g' resource-grain-simulation-large-grain-2.conf

    # command
    java -cp borg-simulator.jar org.cloudish.dh.DHMainExecutor resource-grain-simulation-large-grain-2.conf > log-blade-on-large-time"$i".out 2> log-blade-on-large-time"$i".err
#    cat resource-grain-simulation-large-grain-2.conf
    rm resource-grain-simulation-large-grain-2.conf
done
