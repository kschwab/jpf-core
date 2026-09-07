#!/usr/bin/env python3

import os
import pathlib

CURRENT_DIR = pathlib.Path(__file__).resolve().parent
JPF_CORE_GRADLE_PATH = CURRENT_DIR / 'gradle'
JPF_CORE_GRADLE_PATH.mkdir(parents=True, exist_ok=True)
UID = os.environ.get('SUDO_UID', os.getuid())
GID = os.environ.get('SUDO_GID', os.getgid())
os.system(f'chown -R {UID}:{GID} {JPF_CORE_GRADLE_PATH.parent}')
