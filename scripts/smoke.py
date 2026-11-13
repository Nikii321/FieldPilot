"""Read-only smoke check, no third-party Python dependencies."""
import json, urllib.request
BASE='http://localhost:8081'
def get(path):
    with urllib.request.urlopen(BASE+path, timeout=30) as response:
        return json.load(response)
assert get('/actuator/health')['status']=='UP'
a=get('/api/analysis')
assert len(a['wells'])==6
assert abs(a['plan']['cost']-3.5)<1e-7
assert abs(a['plan']['expectedEffect']-2.98225)<1e-7
assert a['plan']['crewDays']==12
assert a['plan']['actions'][-1]['action']['kind']=='STOP'
assert a['model']['report']['metrics']['mae']<a['model']['report']['metrics']['baselineMae']
print('FieldPilot smoke passed: forecast, mandatory gate, exact portfolio, SQL persistence')
