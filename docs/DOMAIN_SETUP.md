# Zaythar AI custom domain and GitHub Pages

Primary domain: **https://zaytharai.com**

## Included in this repository

- `website/`: responsive static landing page (separate from the native Android app).
- `CNAME`: GitHub Pages custom-domain declaration.
- `.github/workflows/deploy-pages.yml`: validates and deploys the site from the `website/` directory.
- `robots.txt` and `sitemap.xml`: basic search-engine discovery files.

## One-time GitHub Pages configuration

1. Merge the website pull request into `main`.
2. Open **Settings → Pages** in this repository.
3. Set **Build and deployment → Source** to **GitHub Actions**.
4. After the first successful deployment, confirm the custom domain is `zaytharai.com` and enable **Enforce HTTPS** when GitHub reports the certificate is ready.

The workflow requires GitHub Pages to be enabled for this repository. Repository configuration may require an owner action; this source change cannot purchase a domain or independently change registrar DNS.

## DNS records at your domain registrar

For the apex/root domain `zaytharai.com`, configure these GitHub Pages A records:

| Type | Host | Value |
|---|---|---|
| A | @ | 185.199.108.153 |
| A | @ | 185.199.109.153 |
| A | @ | 185.199.110.153 |
| A | @ | 185.199.111.153 |

Optional IPv6 records (AAAA):

| Type | Host | Value |
|---|---|---|
| AAAA | @ | 2606:50c0:8000::153 |
| AAAA | @ | 2606:50c0:8001::153 |
| AAAA | @ | 2606:50c0:8002::153 |
| AAAA | @ | 2606:50c0:8003::153 |

For `www.zaytharai.com`, add a CNAME record with host `www` and target `abdullakhan-tech.github.io`. Do not point a CNAME at an IP address. Remove conflicting old A/AAAA/CNAME records for the same hosts.

## Verification checklist

- Confirm the domain is registered and controlled by you.
- Wait for DNS propagation and verify records at your registrar/DNS provider.
- Check the domain in repository Settings → Pages and complete any ownership verification GitHub requests.
- Wait for GitHub's TLS certificate provisioning, then enable Enforce HTTPS.
- Confirm `https://zaytharai.com/`, `https://zaytharai.com/robots.txt`, and `https://zaytharai.com/sitemap.xml` load.

## Important product boundary

This is a public project landing page, not a browser port of the Android assistant. It does not expose Firebase credentials, API keys, user data, or a web chat endpoint. The Android app remains the actual assistant client.
