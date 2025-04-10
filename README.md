# entropy-miner
open-source hardware random number generator

### enable ssh
sudo raspi-config
3 Interface Options
11 SSH
<Yes>
<Ok>
<Finish>

### java
sudo apt update
sudo apt install openjdk-17-jdk

### remove symlink
sudo rm /dev/gpiochip4

