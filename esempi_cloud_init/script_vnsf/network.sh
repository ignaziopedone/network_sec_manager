sudo sh -c 'cat append.sh >> /etc/network/interfaces.d/50-cloud-init.cfg'
sudo sh -c '/etc/init.d/networking restart'
