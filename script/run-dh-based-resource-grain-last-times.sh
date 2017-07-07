#!/bin/bash

for i in `seq 0 15`; do
    echo "Time $i"

    # configuring constraint on and 1 minimal server small grain
    cp default-dh-simulator.conf resource-grain-simulation.conf

    sed -i -e 's/\$infra_path\$/data\/machine-input.txt/g' resource-grain-simulation.conf
    sed -i -e 's/\$constraint_on\$/yes/g' resource-grain-simulation.conf
    sed -i -e 's/\$mem_grain\$/0.03125/g' resource-grain-simulation.conf
    sed -i -e 's/\$cpu_grain\$/0.015625/g' resource-grain-simulation.conf
    sed -i -e 's/\$out_dir\$/dh-based-results\/time'"$i"'\//g' resource-grain-simulation.conf
    sed -i -e 's/\$min_servers\$/1/g' resource-grain-simulation.conf
    sed -i -e 's/\$max_mem_capacity\$/1/g' resource-grain-simulation.conf
    sed -i -e 's/\$max_cpu_capacity\$/1/g' resource-grain-simulation.conf

    sed -i -e 's/\$workload_path\$/timestamps\/all-tasks-time-'"$i"'-for-java.csv/g' resource-grain-simulation.conf

    # command
    java -cp borg-simulator.jar org.cloudish.dh.DHMainExecutor resource-grain-simulation.conf > log-rs-1-on-min-1-small-time"$i".out 2> log-rs-1-on-min-1-small-time"$i".err
#    cat resource-grain-simulation.conf
    rm resource-grain-simulation.conf

    # configuring constraint on and 12477 minimal server small grain
    cp default-dh-simulator.conf resource-grain-simulation.conf

    sed -i -e 's/\$infra_path\$/data\/machine-input.txt/g' resource-grain-simulation.conf
    sed -i -e 's/\$constraint_on\$/yes/g' resource-grain-simulation.conf
    sed -i -e 's/\$mem_grain\$/0.03125/g' resource-grain-simulation.conf
    sed -i -e 's/\$cpu_grain\$/0.015625/g' resource-grain-simulation.conf
    sed -i -e 's/\$out_dir\$/dh-based-results\/time'"$i"'\//g' resource-grain-simulation.conf
    sed -i -e 's/\$min_servers\$/12477/g' resource-grain-simulation.conf
    sed -i -e 's/\$max_mem_capacity\$/1/g' resource-grain-simulation.conf
    sed -i -e 's/\$max_cpu_capacity\$/1/g' resource-grain-simulation.conf

    sed -i -e 's/\$workload_path\$/timestamps\/all-tasks-time-'"$i"'-for-java.csv/g' resource-grain-simulation.conf

    # command
    java -cp borg-simulator.jar org.cloudish.dh.DHMainExecutor resource-grain-simulation.conf > log-rs-1-on-min-12477-small-time"$i".out 2> log-rs-1-on-min-12477-small-time"$i".err
#    cat resource-grain-simulation.conf

    rm resource-grain-simulation.conf

    # configuring constraint on and 1 minimal server medium grain
    cp default-dh-simulator.conf resource-grain-simulation.conf

    sed -i -e 's/\$infra_path\$/data\/machine-input.txt/g' resource-grain-simulation.conf
    sed -i -e 's/\$constraint_on\$/yes/g' resource-grain-simulation.conf
    sed -i -e 's/\$mem_grain\$/0.0625/g' resource-grain-simulation.conf
    sed -i -e 's/\$cpu_grain\$/0.03125/g' resource-grain-simulation.conf
    sed -i -e 's/\$out_dir\$/dh-based-results\/time'"$i"'\//g' resource-grain-simulation.conf
    sed -i -e 's/\$min_servers\$/1/g' resource-grain-simulation.conf
    sed -i -e 's/\$max_mem_capacity\$/1/g' resource-grain-simulation.conf
    sed -i -e 's/\$max_cpu_capacity\$/1/g' resource-grain-simulation.conf

    sed -i -e 's/\$workload_path\$/timestamps\/all-tasks-time-'"$i"'-for-java.csv/g' resource-grain-simulation.conf

    # command
    java -cp borg-simulator.jar org.cloudish.dh.DHMainExecutor resource-grain-simulation.conf > log-rs-1-on-min-1-medium-time"$i".out 2> log-rs-1-on-min-1-medium-time"$i".err
#    cat resource-grain-simulation.conf
    rm resource-grain-simulation.conf

    # configuring constraint on and 12477 minimal server medium grain
    cp default-dh-simulator.conf resource-grain-simulation.conf

    sed -i -e 's/\$infra_path\$/data\/machine-input.txt/g' resource-grain-simulation.conf
    sed -i -e 's/\$constraint_on\$/yes/g' resource-grain-simulation.conf
    sed -i -e 's/\$mem_grain\$/0.0625/g' resource-grain-simulation.conf
    sed -i -e 's/\$cpu_grain\$/0.03125/g' resource-grain-simulation.conf
    sed -i -e 's/\$out_dir\$/dh-based-results\/time'"$i"'\//g' resource-grain-simulation.conf
    sed -i -e 's/\$min_servers\$/12477/g' resource-grain-simulation.conf
    sed -i -e 's/\$max_mem_capacity\$/1/g' resource-grain-simulation.conf
    sed -i -e 's/\$max_cpu_capacity\$/1/g' resource-grain-simulation.conf

    sed -i -e 's/\$workload_path\$/timestamps\/all-tasks-time-'"$i"'-for-java.csv/g' resource-grain-simulation.conf

    # command
    java -cp borg-simulator.jar org.cloudish.dh.DHMainExecutor resource-grain-simulation.conf > log-rs-1-on-min-12477-medium-time"$i".out 2> log-rs-1-on-min-12477-medium-time"$i".err
#    cat resource-grain-simulation.conf

    rm resource-grain-simulation.conf


    # configuring constraint on and 1 minimal server greatest grain
    cp default-dh-simulator.conf resource-grain-simulation.conf

    sed -i -e 's/\$infra_path\$/data\/machine-input.txt/g' resource-grain-simulation.conf
    sed -i -e 's/\$constraint_on\$/yes/g' resource-grain-simulation.conf
    sed -i -e 's/\$mem_grain\$/0.125/g' resource-grain-simulation.conf
    sed -i -e 's/\$cpu_grain\$/0.0625/g' resource-grain-simulation.conf
    sed -i -e 's/\$out_dir\$/dh-based-results\/time'"$i"'\//g' resource-grain-simulation.conf
    sed -i -e 's/\$min_servers\$/1/g' resource-grain-simulation.conf
    sed -i -e 's/\$max_mem_capacity\$/1/g' resource-grain-simulation.conf
    sed -i -e 's/\$max_cpu_capacity\$/1/g' resource-grain-simulation.conf

    sed -i -e 's/\$workload_path\$/timestamps\/all-tasks-time-'"$i"'-for-java.csv/g' resource-grain-simulation.conf

    # command
    java -cp borg-simulator.jar org.cloudish.dh.DHMainExecutor resource-grain-simulation.conf > log-rs-1-on-min-1-big-time"$i".out 2> log-rs-1-on-min-1-big-time"$i".err
#    cat resource-grain-simulation.conf
    rm resource-grain-simulation.conf

    # configuring constraint on and 12477 minimal server greatest grain
    cp default-dh-simulator.conf resource-grain-simulation.conf

    sed -i -e 's/\$infra_path\$/data\/machine-input.txt/g' resource-grain-simulation.conf
    sed -i -e 's/\$constraint_on\$/yes/g' resource-grain-simulation.conf
    sed -i -e 's/\$mem_grain\$/0.125/g' resource-grain-simulation.conf
    sed -i -e 's/\$cpu_grain\$/0.0625/g' resource-grain-simulation.conf
    sed -i -e 's/\$out_dir\$/dh-based-results\/time'"$i"'\//g' resource-grain-simulation.conf
    sed -i -e 's/\$min_servers\$/12477/g' resource-grain-simulation.conf
    sed -i -e 's/\$max_mem_capacity\$/1/g' resource-grain-simulation.conf
    sed -i -e 's/\$max_cpu_capacity\$/1/g' resource-grain-simulation.conf

    sed -i -e 's/\$workload_path\$/timestamps\/all-tasks-time-'"$i"'-for-java.csv/g' resource-grain-simulation.conf

    # command
    java -cp borg-simulator.jar org.cloudish.dh.DHMainExecutor resource-grain-simulation.conf > log-rs-1-on-min-12477-big-time"$i".out 2> log-rs-1-on-min-12477-big-time"$i".err
#    cat resource-grain-simulation.conf

    rm resource-grain-simulation.conf

done
