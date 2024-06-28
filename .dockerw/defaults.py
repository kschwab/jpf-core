import os
import platform
import pathlib

ROOT_PATH = pathlib.PosixPath(__file__).resolve().parent.parent
TMP_JPF_CORE_GRADLE_PATH = pathlib.Path('/tmp/jpf-core/gradle')
TMP_JPF_CORE_GRADLE_PATH.mkdir(parents=True, exist_ok=True)
UID = os.environ.get("SUDO_UID", os.getuid())
GID = os.environ.get("SUDO_GID", os.getgid())
os.system(f'chown -R {UID}:{GID} {TMP_JPF_CORE_GRADLE_PATH.parent}')
WORKDIR_PATH = pathlib.Path('/jpf-core')
try:
    WORKDIR_PATH /= pathlib.Path.cwd().relative_to(ROOT_PATH)
except:
    pass

dockerw_defaults = [
    '--defaults',
    f'-w {WORKDIR_PATH}',
    f'-v {ROOT_PATH}:{WORKDIR_PATH}',
    f'-e GRADLE_USER_HOME={TMP_JPF_CORE_GRADLE_PATH}',
    f'-v {TMP_JPF_CORE_GRADLE_PATH}:{TMP_JPF_CORE_GRADLE_PATH}',
    '--prompt-banner=phd-computing-artifact-0.3',
    '--name=phd-computing-artifact-0.3',
    '--default-image=ghcr.io/kschwab/jpf-core/phd-computing-artifact:0.3',
    '--auto-attach',
    '--default-shell=bash'
]
