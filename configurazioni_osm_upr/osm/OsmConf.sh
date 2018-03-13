#!bin/bash


sudo route add -net 172.24.4.0/24 gw 10.211.55.1
sudo route add -net 10.208.0.0/24 gw 10.211.55.1
