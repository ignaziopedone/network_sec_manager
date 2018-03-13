#!/bin/bash
ifconfig eth1 72.161.0.1 netmask 255.255.255.252
route del default
route add default gw 72.161.0.2
./scripts/init.sh
exit
