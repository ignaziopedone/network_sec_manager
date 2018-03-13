#!/bin/bash
ifconfig eth1 72.161.0.2 netmask 255.255.255.252
ifconfig eth2 72.161.1.1 netmask 255.255.255.252
route del default
route add default gw 72.161.1.2
iptables --table nat --append POSTROUTING --out-interface eth2 -j MASQUERADE
iptables --append FORWARD --in-interface eth1 -j ACCEPT
./scripts/init.sh
exit
