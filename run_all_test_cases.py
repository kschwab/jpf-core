#!/usr/bin/env python3

import os
import pathlib

if __name__ == '__main__':
   os.chdir(pathlib.Path(__file__).resolve().parent)
   for test_case in range(21):
      os.system(f'java -Xmx1G -jar ./build/RunJPF.jar +classpath=. src/examples/RacerInterleave.jpf {test_case}')
      os.system(f'dot -Tsvg jpf-state-space.dot -o {test_case}.svg')
