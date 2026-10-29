#!/usr/bin/env python3
"""Build and test the real iOS app on the available Apple-hosted simulators."""
import base64,json,pathlib,subprocess,time
ROOT=pathlib.Path(__file__).resolve().parents[1]
OUT=ROOT/'ios/build';OUT.mkdir(exist_ok=True)
def run(*args):
 return subprocess.check_output(args,text=True)
def xcode(*args,log):
 with (OUT/log).open('w') as stream:
  result=subprocess.run(['xcodebuild','-project',str(ROOT/'ios/UniversalCurrencyConverter.xcodeproj'),'-scheme','UniversalCurrencyConverter',
    '-derivedDataPath',str(OUT/'simulator'),'CODE_SIGNING_ALLOWED=NO',*args],stdout=stream,stderr=subprocess.STDOUT)
 if result.returncode:
  print((OUT/log).read_text()[-16000:],flush=True);raise SystemExit(result.returncode)
devices=json.loads(run('xcrun','simctl','list','devices','available','--json'))['devices']
available=[d for runtime,ds in devices.items() if 'iOS' in runtime for d in ds if d.get('isAvailable')]
def choose(preferences,prefix):
 for name in preferences:
  for d in available:
   if d['name']==name:return d
 return next(d for d in available if d['name'].startswith(prefix))
large=choose(['iPhone 17','iPhone 16','iPhone 16 Pro'],'iPhone')
small=choose(['iPhone SE (3rd generation)','iPhone SE (2nd generation)','iPhone 16e'],'iPhone')
tablet=choose(['iPad (A16)','iPad (10th generation)','iPad Air 11-inch (M3)'],'iPad')
matrix=[]
for label,device in [('phone',large),('small-phone',small),('ipad',tablet)]:
 if device['udid'] in [x[1]['udid'] for x in matrix]:continue
 matrix.append((label,device))
print('Simulator matrix:',[(label,d['name']) for label,d in matrix],flush=True)
xcode('-destination','generic/platform=iOS Simulator','build-for-testing',log='simulator-build.log')
for label,device in matrix:
 udid=device['udid'];subprocess.run(['xcrun','simctl','shutdown','all'],check=False)
 subprocess.run(['xcrun','simctl','boot',udid],check=True)
 subprocess.run(['xcrun','simctl','bootstatus',udid,'-b'],check=True)
 xcode('-destination','id='+udid,'-parallel-testing-enabled','NO','-resultBundlePath',str(OUT/(label+'.xcresult')),
       'test-without-building',log=label+'-tests.log')
 # A clean initial screen has no synthetic conversion prices in its screenshot.
 app=OUT/'simulator/Build/Products/Debug-iphonesimulator/UniversalCurrencyConverter.app'
 subprocess.run(['xcrun','simctl','install',udid,str(app)],check=True)
 subprocess.run(['xcrun','simctl','terminate',udid,'com.muhammadabdulqayyum.universalconverter'],check=False)
 subprocess.run(['xcrun','simctl','launch',udid,'com.muhammadabdulqayyum.universalconverter','-ui-testing'],check=True)
 time.sleep(2)
 subprocess.run(['xcrun','simctl','io',udid,'screenshot',str(OUT/(label+'.png'))],check=True)
 print('PASS: '+label+' '+device['name'],flush=True)
 # Reduce screenshot size only for log transfer; the full original is in the artifact.
 subprocess.run(['sips','-Z','1000',str(OUT/(label+'.png')),'--out',str(OUT/(label+'-preview.png'))],check=True)
 print('IOS_SCREENSHOT_'+label+'='+base64.b64encode((OUT/(label+'-preview.png')).read_bytes()).decode(),flush=True)
(OUT/'validation.json').write_text(json.dumps({'deploymentTarget':'15.0','version':'4.0.2','simulators':[{'name':d['name'],'label':label} for label,d in matrix],
 'uiChecks':'passed','minimumOSRuntimeTested':False,'fixtureQuotes':'Debug UI tests only; not in Release'},indent=2))
print('ALL IOS SIMULATOR CHECKS PASSED',flush=True)
