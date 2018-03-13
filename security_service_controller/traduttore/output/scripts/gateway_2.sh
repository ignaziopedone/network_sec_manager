#!/bin/bash
ifconfig eth1 72.161.1.2 netmask 255.255.255.252
iptables --table nat --append POSTROUTING --out-interface eth0 -j MASQUERADE
iptables --append FORWARD --in-interface eth1 -j ACCEPT
./scripts/init.sh
exit
