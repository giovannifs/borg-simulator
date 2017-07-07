#!/bin/bash

for i in `seq 16 30`; do
    echo "Time $i"

    # configuring constraint on and 1 minimal server
    cp default-dh-simulator.conf server-size-simulation2.conf

    sed -i -e 's/\$infra_path\$/data\/machine-input.txt/g' server-size-simulation2.conf
    sed -i -e 's/\$constraint_on\$/yes/g' server-size-simulation2.conf
    sed -i -e 's/\$mem_grain\$/0.0625/g' server-size-simulation2.conf
    sed -i -e 's/\$cpu_grain\$/0.03125/g' server-size-simulation2.conf
    sed -i -e 's/\$out_dir\$/dh-based-results\/time'"$i"'\//g' server-size-simulation2.conf
    sed -i -e 's/\$min_servers\$/1/g' server-size-simulation2.conf
    sed -i -e 's/\$max_mem_capacity\$/16/g' server-size-simulation2.conf
    sed -i -e 's/\$max_cpu_capacity\$/16/g' server-size-simulation2.conf

    sed -i -e 's/\$workload_path\$/timestamps\/all-tasks-time-'"$i"'-for-java.csv/g' server-size-simulation2.conf

    # command
    java -cp borg-simulator.jar org.cloudish.dh.DHMainExecutor server-size-simulation2.conf > log-ss-1-on-min-1-time"$i".out 2> log-ss-1-on-min-1-time"$i".err
#    cat server-size-simulation2.conf
    rm server-size-simulation2.conf

    # configuring constraint on and 12477 minimal server
    cp default-dh-simulator.conf server-size-simulation2.conf

    sed -i -e 's/\$infra_path\$/data\/machine-input.txt/g' server-size-simulation2.conf
    sed -i -e 's/\$constraint_on\$/yes/g' server-size-simulation2.conf
    sed -i -e 's/\$mem_grain\$/0.0625/g' server-size-simulation2.conf
    sed -i -e 's/\$cpu_grain\$/0.03125/g' server-size-simulation2.conf
    sed -i -e 's/\$out_dir\$/dh-based-results\/time'"$i"'\//g' server-size-simulation2.conf
    sed -i -e 's/\$min_servers\$/12477/g' server-size-simulation2.conf
    sed -i -e 's/\$max_mem_capacity\$/16/g' server-size-simulation2.conf
    sed -i -e 's/\$max_cpu_capacity\$/16/g' server-size-simulation2.conf

    sed -i -e 's/\$workload_path\$/timestamps\/all-tasks-time-'"$i"'-for-java.csv/g' server-size-simulation2.conf

    # command
    java -cp borg-simulator.jar org.cloudish.dh.DHMainExecutor server-size-simulation2.conf > log-ss-1-on-min-12477-time"$i".out 2> log-ss-1-on-min-12477-time"$i".err
#    cat server-size-simulation2.conf

    rm server-size-simulation2.conf

    # configuring constraint off and 1 minimal server

    cp default-dh-simulator.conf server-size-simulation2.conf

    sed -i -e 's/\$infra_path\$/data\/machine-input.txt/g' server-size-simulation2.conf
    sed -i -e 's/\$constraint_on\$/no/g' server-size-simulation2.conf
    sed -i -e 's/\$mem_grain\$/0.0625/g' server-size-simulation2.conf
    sed -i -e 's/\$cpu_grain\$/0.03125/g' server-size-simulation2.conf
    sed -i -e 's/\$out_dir\$/dh-based-results\/time'"$i"'\//g' server-size-simulation2.conf
    sed -i -e 's/\$min_servers\$/1/g' server-size-simulation2.conf
    sed -i -e 's/\$max_mem_capacity\$/16/g' server-size-simulation2.conf
    sed -i -e 's/\$max_cpu_capacity\$/16/g' server-size-simulation2.conf

    sed -i -e 's/\$workload_path\$/timestamps\/all-tasks-time-'"$i"'-for-java.csv/g' server-size-simulation2.conf

    # command
    java -cp borg-simulator.jar org.cloudish.dh.DHMainExecutor server-size-simulation2.conf > log-ss-1-off-min-1-time"$i".out 2> log-ss-1-off-min-1-time"$i".err
#    cat server-size-simulation2.conf

    rm server-size-simulation2.conf

    # configuring constraint off and 12477 minimal server

    cp default-dh-simulator.conf server-size-simulation2.conf

    sed -i -e 's/\$infra_path\$/data\/machine-input.txt/g' server-size-simulation2.conf
    sed -i -e 's/\$constraint_on\$/no/g' server-size-simulation2.conf
    sed -i -e 's/\$mem_grain\$/0.0625/g' server-size-simulation2.conf
    sed -i -e 's/\$cpu_grain\$/0.03125/g' server-size-simulation2.conf
    sed -i -e 's/\$out_dir\$/dh-based-results\/time'"$i"'\//g' server-size-simulation2.conf
    sed -i -e 's/\$min_servers\$/12477/g' server-size-simulation2.conf
    sed -i -e 's/\$max_mem_capacity\$/16/g' server-size-simulation2.conf
    sed -i -e 's/\$max_cpu_capacity\$/16/g' server-size-simulation2.conf

    sed -i -e 's/\$workload_path\$/timestamps\/all-tasks-time-'"$i"'-for-java.csv/g' server-size-simulation2.conf

    # command
    java -cp borg-simulator.jar org.cloudish.dh.DHMainExecutor server-size-simulation2.conf > log-ss-1-off-min-12477-time"$i".out 2> log-ss-1-off-min-12477-time"$i".err
#    cat server-size-simulation2.conf

    rm server-size-simulation2.conf
done
