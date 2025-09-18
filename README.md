# entropy-miner
open-source hardware random number generator

[video](https://youtu.be/QL051UbuOBY)

![entropy miner](https://img.youtube.com/vi/QL051UbuOBY/mqdefault.jpg)

### create user
```
adduser user
apt update
apt install ssh
```

### enable ssh RPi
```
sudo raspi-config
3 Interface Options
11 SSH
<Yes>
<Ok>
<Finish>
```

### java
```
sudo apt update
sudo apt install openjdk-17-jdk
```

### gpio group
```
sudo groupadd gpio
sudo usermod -aG gpio $USER
sudo vi /etc/udev/rules.d/99-com.rules
SUBSYSTEM=="gpio", GROUP="gpio", MODE="0660"
```

### remove symlink
```
ls -l /dev/gpiochip*
sudo rm /dev/gpiochip4
```

### adjust the volume
```
alsamixer
```

### autostart
/etc/rc.local
```
#!/bin/sh -e

gpioset 0 14=0
gpioset 0 15=0
cd /home/user
/usr/bin/java -jar entropy-miner-1.0-jar-with-dependencies.jar
```

```
sudo chmod a+x /etc/rc.local
```
