"""Supervisor script-only update adapter. Exit 2 means use the normal cold updater."""
import sys,json,time,hashlib,os,zipfile,io
from pathlib import Path
from urllib.request import Request,urlopen
from urllib.error import HTTPError
bundle=Path(__file__).resolve().parent
home=Path.home()/'.runelite'/'cooks-hot'
repo='julienlemyre92-commits/tutorial-patches'

def status():
    try:
        p=dict(line.split('=',1) for line in (home/'status.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
        if 0<=time.time()*1000-int(p['timestamp'])<5000 and p.get('hostVersion')=='1':return p
    except (OSError,ValueError,KeyError):pass
    return None

def get(path):
    headers={'User-Agent':'Alex-hot-script-updater','Accept':'application/vnd.github.raw+json'}
    token=(bundle/'github-token.txt').read_text().strip()
    if token:headers['Authorization']='Bearer '+token
    with urlopen(Request('https://api.github.com/repos/'+repo+'/contents/'+path+'?ref=main',headers=headers),timeout=15) as response:
        data=response.read()
    # GitHub media negotiation can return the contents envelope.
    try:
        obj=json.loads(data)
        if isinstance(obj,dict) and obj.get('encoding')=='base64':
            import base64
            return base64.b64decode(obj['content'])
    except (ValueError,UnicodeDecodeError):pass
    return data

def main():
    remote=int(sys.argv[1]); before=status()
    if not before:return 2
    try:meta=json.loads(get(f'patches/patch-{remote}.hot.json'))
    except HTTPError as exc:
        if exc.code==404:return 2
        print('HOT UPDATE HELD: manifest fetch HTTP',exc.code,flush=True);return 0
    if meta.get('hostVersion')!=1 or meta.get('patch')!=remote:return 2
    expected=int(meta['build']);sha=meta['sha256']
    if len(sha)!=64 or any(c not in '0123456789abcdef' for c in sha):raise ValueError('invalid hash')
    if int(before['build'])==expected and not before.get('error'):
        print(f'HOT BUILD {expected} running; client PID {before["pid"]} retained',flush=True);return 0
    if before.get('rejected')==sha and before.get('error'):
        print('HOT UPDATE HELD:',before['error'],flush=True);return 0
    artifact=get(f'patches/cooks-{expected}.jar')
    if hashlib.sha256(artifact).hexdigest()!=sha:raise ValueError('artifact hash mismatch')
    prefix='net/runelite/client/plugins/microbot/cooksassistant/CooksAssistantScript'
    with zipfile.ZipFile(io.BytesIO(artifact)) as z:
        for n in z.namelist():
            if not(n==prefix+'.class' or n.startswith(prefix+'$') and n.endswith('.class')):
                raise ValueError('artifact contains non-script class')
        if z.testzip():raise ValueError('archive CRC failure')
    dest=home/(sha+'.jar');temp=home/(sha+'.tmp')
    temp.write_bytes(artifact);os.replace(temp,dest)
    request=home/'request.tmp'
    request.write_text(f'build={expected}\nsha256={sha}\n',encoding='ascii')
    os.replace(request,home/'request.properties')
    print(f'HOT UPDATE requested build {expected}; preserving client PID {before["pid"]}',flush=True)
    deadline=time.monotonic()+22
    while time.monotonic()<deadline:
        now=status()
        if now and now.get('pid')!=before['pid']:raise RuntimeError('client PID changed during update')
        if now and int(now['build'])==expected and now.get('sha256')==sha and not now.get('error'):
            print(f'HOT UPDATE VERIFIED build={expected} pid={now["pid"]} sha={sha}',flush=True);return 0
        if now and now.get('rejected')==sha and now.get('error'):
            print('HOT UPDATE HELD:',now['error'],flush=True);return 0
        time.sleep(.5)
    print('HOT UPDATE pending: no verified replacement yet; preserving client for diagnostics',flush=True)
    return 0

if __name__=='__main__':
    try:sys.exit(main())
    except Exception as exc:
        print('HOT UPDATE HELD:',str(exc),flush=True)
        sys.exit(0)
