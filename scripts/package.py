from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
root=Path(__file__).resolve().parents[1]
skip={'.git','node_modules','target','dist','.angular','build','test-results','playwright-report','data','__pycache__'}
out=root.parent/'Casino-Cage-Project.zip'
with ZipFile(out,'w',ZIP_DEFLATED) as z:
 for p in sorted(root.rglob('*')):
  rel=p.relative_to(root)
  if p.is_file() and not any(part in skip for part in rel.parts) and p.name!='.env' and p.suffix not in {'.log','.class','.zip'} and not str(rel).startswith('backend/src/main/resources/static/'):
   z.write(p,Path('casino-cage-management')/rel)
print(out)
