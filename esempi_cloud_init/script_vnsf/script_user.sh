#!/bin/sh
#source network.sh
IFS=$' \t\n'
echo "Start interfaces configuration..."
echo "Set mgmt iface..."
ip_subnet=$(ip route | grep ens4 | awk '{print $1}')
echo $ip_subnet
ip=$(ifconfig ens4 2>/dev/null | awk '/inet addr:/ {print $2}'| sed 's/addr://')
echo "Address node: $ip"
ip_nmap=$(nmap -sn $ip_subnet -oG - | awk '$4=="Status:" && $5=="Up" {print $2}')
echo $ip_nmap
for i in $ip_nmap
do
if [ $i != $ip ] && [ $(echo $i | cut -f 4 -d.) != "2" ]
then
  ip_ens4_next=$i
fi

done
echo $ip_ens4_next
ip_final=$(echo $ip_ens4_next)
echo "ip nexthop: $ip_final"
IFS=$' '
# echo "set DNAT"
# sudo iptables -t nat -A PREROUTING -i ens4 -j DNAT --to-destination $ip_final
# echo "set SNAT"
# sudo iptables -t nat -A POSTROUTING -o ens5 -d $ip_final -j SNAT --to-source $ip

# This command allow natting capability on the output interfaces
sudo iptables -t nat -A POSTROUTING -o ens4 -j MASQUERADE

# These commands allow to forward packet in the chain
# You should use more interesting mechanisms such as OpenStack networking-sfc
sudo ip route add 8.8.8.8 via 10.208.0.1 dev ens3
sudo ip route add 130.192.1.95 via 10.208.0.1 dev ens3
sudo ip route del default
sudo ip route add default via $ip_final dev ens4



unset IFS
