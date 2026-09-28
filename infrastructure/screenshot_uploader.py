#!/usr/bin/env python3
"""
Screenshot uploader for Tutorial Island bot.
Watches the screenshots/ folder and uploads new images to GitHub.

Setup: put your GitHub token in github-token.txt (same folder as this script)
       Token needs 'repo' scope. Get one at:
       github.com -> Settings -> Developer settings -> Personal access tokens

Runs forever. Uploads each new screenshot once, then marks it done.
"""

import os
import sys
import time
import base64
import urllib.request
import urllib.error
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
SCREENSHOTS_DIR = os.path.join(SCRIPT_DIR, "screenshots")
TOKEN_FILE = os.path.join(SCRIPT_DIR, "github-token.txt")
DONE_FILE = os.path.join(SCRIPT_DIR, "uploaded-screenshots.txt")
REPO = "julienlemyre92-commits/tutorial-patches"

def get_token():
    with open(TOKEN_FILE) as f:
        return f.read().strip()

def github_api(method, path, data=None):
    url = f"https://api.github.com{path}"
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, method=method)
    req.add_header("Accept", "application/vnd.github.v3+json")
    req.add_header("Authorization", f"Bearer {get_token()}")
    if body:
        req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())

def get_uploaded():
    if os.path.exists(DONE_FILE):
        with open(DONE_FILE) as f:
            return set(line.strip() for line in f if line.strip())
    return set()

def mark_uploaded(name):
    with open(DONE_FILE, "a") as f:
        f.write(name + "\n")

def upload_screenshot(filepath, name):
    with open(filepath, "rb") as f:
        content = base64.b64encode(f.read()).decode()
    # Use timestamped path to avoid collisions
    gh_path = f"screenshots/{name}"
    try:
        existing = github_api("GET", f"/repos/{REPO}/contents/{gh_path}")
        sha = existing["sha"]
    except urllib.error.HTTPError as e:
        if e.code == 404:
            sha = None
        else:
            raise
    data = {"message": f"Screenshot {name}", "content": content}
    if sha:
        data["sha"] = sha
    github_api("PUT", f"/repos/{REPO}/contents/{gh_path}", data)
    print(f"Uploaded: {name}")

def main():
    if not os.path.exists(TOKEN_FILE):
        print(f"ERROR: {TOKEN_FILE} not found.")
        print("Create it with your GitHub personal access token (needs 'repo' scope).")
        print("Get one at: github.com -> Settings -> Developer settings -> Personal access tokens")
        input("Press Enter to close this window...")
        sys.exit(1)
    os.makedirs(SCREENSHOTS_DIR, exist_ok=True)
    print(f"Watching {SCREENSHOTS_DIR} for screenshots...")
    uploaded = get_uploaded()
    while True:
        try:
            for fname in sorted(os.listdir(SCREENSHOTS_DIR)):
                if not fname.lower().endswith((".png", ".jpg", ".jpeg")):
                    continue
                if fname in uploaded:
                    continue
                fpath = os.path.join(SCREENSHOTS_DIR, fname)
                # Wait until file is fully written (size stable)
                s1 = os.path.getsize(fpath)
                time.sleep(1)
                s2 = os.path.getsize(fpath)
                if s1 != s2:
                    continue
                try:
                    upload_screenshot(fpath, fname)
                    uploaded.add(fname)
                    mark_uploaded(fname)
                except Exception as e:
                    print(f"Upload failed for {fname}: {e}")
        except Exception as e:
            print(f"Watcher error: {e}")
        time.sleep(10)

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"FATAL ERROR: {e}")
        input("Press Enter to close this window...")
        raise
