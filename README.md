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

### autostart
/etc/rc.local
```
#!/bin/sh -e

cd /home/user
/usr/bin/java -jar entropy-miner-1.0-jar-with-dependencies.jar
```
sudo chmod a+x /etc/rc.local
