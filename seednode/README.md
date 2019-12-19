# Bisq Seednode setup guide

## Hardware

Highly recommended to use SSD specs!

* CPU: 4 cores
* RAM: 8 GB
* SSD: 512 GB (HDD is too slow)

## Software

The following OS are known to work well:

* Ubuntu 18
* FreeBSD 12

These instructions are for a base Ubuntu 18.04 LTS server installation.

### Operating System

Upgrade your OS before proceeding, and install basic build tools.

```bash
sudo apt-get update
sudo apt-get upgrade -y
sudo apt-get install build-essential libtool autotools-dev automake pkg-config bsdmainutils python3
```

### Tor

First thing we need is Tor:

```bash
sudo apt-get install tor
```

Configure your Tor node based on the following `torrc` file:

```bash
SOCKSPort 9050 # Default: Bind to localhost:9050 for local connections.
Log notice syslog
RunAsDaemon 1
ControlPort 9051
CookieAuthentication 1
CookieAuthFileGroupReadable 1
DataDirectoryGroupReadable 1
KeepalivePeriod 42
```

This will allow users in the `debian-tor` group to access the cookie.

### Bitcoin

Create a `bitcoin` user account in the `debian-tor` group:

```bash
sudo useradd -d /bitcoin -G debian-tor bitcoin
sudo mkdir /bitcoin
sudo chown bitcoin:bitcoin /bitcoin
```

Install necessary build dependencies:
```bash
sudo apt-get install -y libevent-dev libboost-system-dev libboost-filesystem-dev libboost-chrono-dev libboost-test-dev libboost-thread-dev
```

Clone and build Bitcoin from GitHub, install into OS
```bash
sudo su - bitcoin
git clone https://github.com/bitcoin/bitcoin
cd bitcoin
git checkout v0.19.0.1
./autogen.sh
./configure
make
exit
sudo 'cd /bitcoin/bitcoin && make install'
```

Configure your Bitcoin node based on the following `bitcoin.conf`:
```bash
server=1
txindex=1
dbcache=1337
maxconnections=1337
timeout=30000
listen=1
discover=1
onion=127.0.0.1:9050
externalip=foo.onion
rpcallowip=127.0.0.1
rpcuser=foo
rpcpassword=foo
blocknotify=/bitcoin/blocknotify.sh %s
```

Modify the above paths, onion hostname, and RPC user/password as necessary.

### Bisq

After your Bitcoin node is fully synced, you can continue with:

* Build the Bisq seednode 
* Install bisq-seednode.service in /etc/systemd/system
* Install bisq-seednode.env in /etc/default
* Install blocknotify.sh in bitcoind's ~/.bitcoin/ folder and chmod 700 it
* Modify the executable paths and configuration as necessary
* Then you can do:

```bash
systemctl enable bisq-seednode.service
systemctl start bisq-seednode.service
```
Check the logs created by the service by inspecting

```bash
journalctl --unit bisq-seednode --follow
```

### BSQ Block Explorer (optional)

After your Bisq seednode is fully synced, you can optionally setup a BSQ Block Explorer:

