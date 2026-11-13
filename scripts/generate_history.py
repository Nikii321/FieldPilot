"""Reproduce the checked-in SYNTHETIC fixture. Optional: pip install numpy.
The application and ML training do not use Python.
"""
from pathlib import Path
import csv
import numpy as np

root=Path(__file__).resolve().parents[1]
rng=np.random.default_rng(42)
path=root/'src/main/resources/demo/history.csv'
with path.open('w',newline='') as out:
    writer=csv.writer(out,lineterminator='\n')
    writer.writerow(['well_id','period','q7','water_cut','pressure','age_months','trend','q_next30'])
    for period in range(1,13):
        for well in range(1,61):
            q=rng.uniform(8,50)
            water=rng.uniform(.1,.95)
            pressure=rng.uniform(3,12)
            age=rng.uniform(0,60)
            trend=rng.uniform(-5,3)
            target=max(0,.91*q-4*water+.25*pressure-.035*age+.45*trend+rng.normal(0,1.8))
            writer.writerow([f'W{well:03d}',period,*[f'{v:.9f}' for v in [q,water,pressure,age,trend,target]]])
print(path)
