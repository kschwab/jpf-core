#!/usr/bin/env python3

import os
import sys
from pysu import pysu

if __name__ == '__main__':
    if os.geteuid() == 0 and 'ENTRYPOINT_UID' in os.environ:
        uid = os.environ.get('ENTRYPOINT_UID')
        gid = os.environ.get('ENTRYPOINT_GID', uid)
        name = os.environ.get('ENTRYPOINT_NAME', 'app')
        os.system(f'groupadd -g {gid} {name} > /dev/null 2>&1')
        os.system(f'useradd -u {uid} -o -m {name} -g {gid} > /dev/null 2>&1')
        os.system(f'echo PS1=\\"[📦 {name}]\\\$ \\" >> /home/$ENTRYPOINT_NAME/.bashrc')
        os.system(f'ln -s {os.getcwd()} /home/{name}/{os.getcwd()} > /dev/null 2>&1')
        os.system(f'chown -h {uid}:{gid} /home/{name}/{os.getcwd()} > /dev/null 2>&1')
    else:
        uid = os.getuid()
        gid = os.getgid()

    pysu(f'{uid}:{gid}', sys.argv[1:])
