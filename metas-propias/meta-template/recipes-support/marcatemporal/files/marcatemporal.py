#!/usr/bin/env python3
from datetime import datetime
from pathlib import Path

path = Path('/home/admin/marcatemporal.txt')
path.parent.mkdir(parents=True, exist_ok=True)
with path.open('a', encoding='utf-8') as f:
    f.write(datetime.now().strftime('%Y-%m-%d %H:%M:%S') + '\n')
