## AuraDE-Linux

AuraDE is a small Linux DE (Desktop Environment) project, aiming for old + modern combination, written in Java.

# Manual installation only for now!

# How to install:

## Step 1: copy auramass file and start-aurade

To do that, you need to Download all the files in https://github.com/ImDamianHehe/AuraDE-Linux/tree/main/jar%20file(s) then do these commands:

### Create Directories

```bash
mkdir ~/.local/aurade
mkdir ~/.AuraMass
```

### Copy files

```bash
cp -r /path/to/auramass ~/.local/bin/auramass
sudo cp -r /path/to/start-aurade /usr/local/bin/start-aurade
cp -r /path/to/auramass.jar ~/.AuraMass/auramass.jar
cp -r /path/to/aurade.jar ~/.local/aurade/aurade.jar
sudo cp -r /path/to/aurade.desktop /usr/share/xsessions/aurade.desktop
```

### Optional files to copy

```bash
cp -r /path/to/wallpaper.jpg ~/.local/aurade/wallpaper.jpg
```

### Make files executable

```bash
chmod +x ~/.local/bin/auramass
sudo chmod +x /usr/local/bin/start-aurade
```

# Required packages to install:

## Arch Linux:

```bash
sudo pacman -S \
xorg-server \
xorg-xinit \
xorg-xrandr \
xorg-xsetroot \
xorg-xprop \
openbox \
firefox \
less \
jre-openjdk \
feh \
tint2 \
pcmanfm \
dbus \
xdg-utils \
xdg-user-dirs \
ttf-dejavu \
ttf-liberation \
sddm
```

## Ubuntu:

```bash
sudo apt install -y \
xorg \
xinit \
x11-xserver-utils \
x11-utils \
x11-xkb-utils \
openbox \
firefox \
less \
default-jre \
feh \
tint2 \
pcmanfm \
xdg-utils \
xdg-user-dirs \
fonts-dejavu \
fonts-liberation \
sddm
```

### Optional packages

`obconf` or `obconf-qt` (for window decoration themes, etc.)
