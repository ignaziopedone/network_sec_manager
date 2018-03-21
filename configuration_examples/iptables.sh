#!/bin/bash

sudo iptables -A FORWARD -p TCP -d 96.100.46.185 -j DROP
sudo iptables -A FORWARD -p TCP -d 8.8.8.8 -j DROP
sudo iptables -A FORWARD -p TCP -d 6.8.9.12 -j DROP
sudo iptables -A FORWARD -p TCP -d 12.7.10.16 -j DROP
sudo iptables -A FORWARD -p TCP -d 42.0.12.13 -j DROP
sudo iptables -A FORWARD -p TCP -d 32.12.8.12 -j DROP
sudo iptables -A FORWARD -p TCP -d 190.12.18.12 -j DROP
sudo iptables -A FORWARD -p TCP -d 22.12.85.12 -j DROP
sudo iptables -A FORWARD -p all -d 8.8.4.4 -j DROP 
