# Running MediTracker on a server

The site runs as four containers (Postgres, Spring Boot, nginx, cloudflared). The public reaches it
only through a Cloudflare Tunnel, so the server needs **no open inbound ports** except SSH.

Any always-on Linux box with Docker works: a small VPS (1-2 vCPU, 2 GB RAM is plenty) or a home machine.

## 1. Prepare the server (Ubuntu 24.04)

```bash
# updates install themselves
sudo apt update && sudo apt -y upgrade
sudo apt -y install unattended-upgrades git curl
sudo dpkg-reconfigure -plow unattended-upgrades

# firewall: SSH only. The tunnel connects outbound, nothing else needs to be open.
sudo ufw allow OpenSSH
sudo ufw enable

# Docker (official install script), then let your user run it
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER   # log out and back in after this
```

Use SSH keys and turn off password login (`PasswordAuthentication no` in `/etc/ssh/sshd_config`).

## 2. Create the tunnel

In the Cloudflare dashboard: **Zero Trust → Networks → Tunnels → Create a tunnel → Cloudflared**.

1. Name it `meditracker`. Copy the token from the install command (the long string after `--token`).
2. Under **Public hostname**, add: subdomain `medi`, domain `koala.rip`, service **HTTP** `frontend:80`.
   Cloudflare creates the DNS record for `medi.koala.rip` itself.

## 3. Deploy

```bash
git clone https://github.com/KoalaAwad/MediTracker.git
cd MediTracker/deploy
cp .env.example .env
nano .env    # fill in DB_PASSWORD, JWT_SECRET, ADMIN_*, MAIL_*, TUNNEL_TOKEN
./deploy.sh --profile tunnel
```

Generate secrets with `openssl rand -hex 32`. Postgres only reads `DB_USER`/`DB_PASSWORD` when it
creates the database the first time; changing them later in `.env` makes the backend fail to log in.
To change the password afterwards, run `ALTER USER` inside the database first. Never reuse the old JWT secret or
`admin@meditracker.com` / `password`; both are in the public git history.

Check it: `docker compose ps` shows everything `running`/`healthy`, and https://medi.koala.rip loads.

**Updating later:** `git pull && ./deploy.sh --profile tunnel`. Migrations run on backend start.

## 4. Backups

`backup.sh` dumps the database, checks the dump can be read back and keeps 14 days of local copies.

**Off-site copy in R2 (recommended).** Local copies die with the server.
1. Cloudflare dashboard → **R2 → Create bucket**, e.g. `meditracker-backups`.
2. In the bucket's **Settings → Object lifecycle rules**, delete objects after 30 days.
3. **R2 → Manage API tokens → Create token** with *Object Read & Write* on that bucket only.
4. Put the account ID, bucket name and token keys into the `R2_*` lines of `.env`.

**Alert if backups stop (optional).** Make a free check at healthchecks.io with a 1-day period
and put its ping URL in `BACKUP_PING_URL`.

Run it once by hand, then schedule it:

```bash
./backup.sh
crontab -e
# add (use your real path):
0 3 * * * /home/YOU/MediTracker/deploy/backup.sh >> /home/YOU/MediTracker/deploy/db-backup/backup.log 2>&1
```

**Restore** (replaces the live database; asks you to type `restore`):

```bash
./restore.sh db-backup/meditracker-YYYYMMDD-HHMMSS.dump
```

For an R2 copy, download the `.dump` from the bucket into `db-backup/` first.
Test a restore once before you rely on the backups.
