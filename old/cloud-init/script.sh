#!/bin/sh
#source network.sh
IFS=$' \t\n'
echo "Start interfaces configuration..."
echo "Set mgmt iface..."
ip_subnet=$(ip route | grep ens5 | awk '{print $1}')
echo $ip_subnet
ip=$(ifconfig ens5 2>/dev/null | awk '/inet addr:/ {print $2}'| sed 's/addr://')
echo "Address node: $ip"
ip_nmap=$(nmap -sn $ip_subnet -oG - | awk '$4=="Status:" && $5=="Up" {print $2}')
echo $ip_nmap
for i in $ip_nmap
do
if [ $i != $ip ] && [ $(echo $i | cut -f 4 -d.) != "2" ]
then
  ip_ens5_next=$i
fi

done
echo $ip_ens5_next
ip_final=$(echo $ip_ens5_next)
echo "ip nexthop: $ip_final"
IFS=$' '
echo "set DNAT"
sudo iptables -t nat -A PREROUTING -i ens4 -j DNAT --to-destination $ip_final
echo "set SNAT"
sudo iptables -t nat -A POSTROUTING -o ens5 -d $ip_final -j SNAT --to-source $ip

#sudo ip route add 8.8.8.0/24 via $ip_final dev ens5
#sudo ip route add 8.8.8.0/24 via $ip_final dev ens5
#sudo ip route add 10.208.0.0/24 via 10.208.0.1 dev ens3
#echo "ens3 configured..."
#sudo ip route del default
#echo "wait..."
#sudo ip route add default via $ip dev ens5
#echo "ens5 configured..."
#sudo iptables -t nat -A POSTROUTING -o ens5 -j MASQUERADE
#echo "iptables configured..."
unset IFS
