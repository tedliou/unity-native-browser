# SSH Commit Signing Setup for GitHub Verified Commits

This guide configures Git to sign commits with an SSH key so GitHub displays the "Verified" badge.

## Prerequisites

- Git 2.34+ (SSH signing support)
- An SSH key pair (Ed25519 recommended)
- A GitHub account

## Step 1: Generate SSH Key (skip if you already have one)

```bash
ssh-keygen -t ed25519 -C "mail@tedliou.com"
```

Default location: `~/.ssh/id_ed25519` (private) and `~/.ssh/id_ed25519.pub` (public).

## Step 2: Add Signing Key to GitHub

1. Copy your **public** key:
   ```bash
   # Windows
   type %USERPROFILE%\.ssh\id_ed25519.pub | clip

   # Linux/macOS
   cat ~/.ssh/id_ed25519.pub | pbcopy  # macOS
   cat ~/.ssh/id_ed25519.pub | xclip   # Linux
   ```

2. Go to **GitHub → Settings → SSH and GPG keys**
3. Click **New SSH key**
4. **Key type**: Select **Signing Key** (NOT "Authentication Key")
5. Paste the public key and save

> You can use the same key for both authentication and signing, but you must add it **twice** — once as Authentication Key and once as Signing Key.

## Step 3: Configure Git

```bash
# Set SSH as the signing format
git config --global gpg.format ssh

# Point to your signing key
git config --global user.signingkey ~/.ssh/id_ed25519.pub

# Enable auto-signing for all commits
git config --global commit.gpgsign true

# Enable auto-signing for all tags
git config --global tag.gpgsign true
```

### Windows-Specific Path

On Windows, use the full path:

```bash
git config --global user.signingkey "C:/Users/Ted/.ssh/id_ed25519.pub"
```

## Step 4: Create Allowed Signers File (for local verification)

```bash
# Create the file
echo "mail@tedliou.com $(cat ~/.ssh/id_ed25519.pub)" > ~/.ssh/allowed_signers

# Tell Git about it
git config --global gpg.ssh.allowedSignersFile ~/.ssh/allowed_signers
```

## Step 5: Verify Setup

```bash
# Make a test commit
git commit --allow-empty -m "test: verify SSH signing"

# Check signature
git log --show-signature -1
```

Expected output should include: `Good "git" signature for mail@tedliou.com`

## Troubleshooting

| Issue | Fix |
|-------|-----|
| `error: Load key ... No such file or directory` | Check `user.signingkey` path is correct |
| `error: ssh-keygen -Y sign` fails | Update Git to 2.34+ |
| GitHub shows "Unverified" | Ensure key is added as **Signing Key** (not just Authentication) |
| Signing works locally but not on GitHub | Email in key comment must match GitHub verified email |

## Verify Existing Commits After History Rewrite

After rewriting history with `git filter-repo`, old commits won't be signed. To sign the latest commit:

```bash
git commit --amend --no-edit -S
```

> Note: Signing ALL historical commits requires `git rebase --exec 'git commit --amend --no-edit -S' --root`. This is optional — GitHub only requires signing for new commits going forward.
