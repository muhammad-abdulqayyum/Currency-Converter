#!/usr/bin/env python3
"""Check the distributed preview on a dedicated emulator with synthetic local rates."""
import base64,datetime,json,os,pathlib,re,subprocess,time
import xml.etree.ElementTree as ET
import xml.sax.saxutils as esc
ROOT=pathlib.Path(__file__).resolve().parents[1]
OUT=ROOT/'device-checks';OUT.mkdir(exist_ok=True)
PKG='com.example.muhammad.usd_converter'
ADB=str(pathlib.Path(os.environ['ANDROID_HOME'])/'platform-tools/adb')
def adb(*args):
 return subprocess.check_output([ADB,*args],timeout=45)
def ui():
 adb('shell','uiautomator','dump','/sdcard/window.xml')
 data=adb('exec-out','cat','/sdcard/window.xml');(OUT/'last.xml').write_bytes(data);return data

def tap(key,text=False):
 for n in ET.fromstring(ui()).iter('node'):
  if (n.get('text')==key if text else n.get('resource-id','').endswith('/'+key)):
   x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
   if y2>y1:
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.3);return
 raise AssertionError('Missing UI element '+key)
def shot(name):
 (OUT/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
 (OUT/(name+'.xml')).write_bytes(ui())
def top():adb('shell','input','swipe','200','200','200','830','200')
def bottom():adb('shell','input','swipe','200','800','200','150','200')
def choose(side,category,name,query=None):
 top();tap(side);tap(category)
 if query:
  tap('asset_search');adb('shell','input','text',query);adb('shell','input','keyevent','4')
 tap(name,True)
def check(expected):
 bottom();tap('convert');time.sleep(.5)
 assert expected.encode() in ui(),expected
 print('PASS:',expected,flush=True)
def launch():
 adb('shell','am','force-stop',PKG);adb('shell','am','start','-W','-n',PKG+'/.MainActivity');time.sleep(1)
serial=adb('get-serialno').decode().strip()
assert serial.startswith('emulator-'),'Use a dedicated emulator only'
adb('install','-r',str(ROOT/'android-installer-file/Currency-Converter-4.0.apk'))
adb('shell','wm','size','420x900');adb('shell','wm','density','160');adb('shell','input','keyevent','82');adb('logcat','-c')
now=int(time.time()*1000);day=datetime.datetime.now(datetime.timezone.utc).date();prefs=['<map>']
for code,rate in {'GBP':'0.8','BRL':'5','PHP':'60','XAU':'0.00025','XAG':'0.025','BTC':'0.00001'}.items():
 date=(day-datetime.timedelta(days=1) if code=='BTC' else day).isoformat()
 body=json.dumps({'base':'USD','quote':code,'rate':rate,'date':date,'providers':[{'key':'ESTV' if code=='BTC' else 'ECB'}]})
 prefs.append('<string name="v3.'+code+'">'+esc.escape(body)+'</string><long name="v3.'+code+'.time" value="'+str(now)+'"/>')
prefs.append('</map>');(OUT/'fixture.xml').write_text(''.join(prefs));adb('push',str(OUT/'fixture.xml'),'/data/local/tmp/fixture.xml');adb('shell','run-as',PKG,'mkdir','-p','shared_prefs');adb('shell','run-as',PKG,'cp','/data/local/tmp/fixture.xml','shared_prefs/reference_rates.xml')
try:
 launch();time.sleep(4);shot('home');assert b'VERSION 4' in ui()
 top();tap('from_asset');tap('metals_tab');shot('metals-picker');tap('Gold',True)
 choose('to_asset','world_tab','US Dollar','USD');check('400,000.00 USD')
 top();tap('swap');check('0.025 troy oz of Gold')
 top();tap('from_asset');tap('crypto_tab');shot('crypto-picker');tap('Bitcoin',True);check('2,500 troy oz of Gold');assert b'Different reference dates' in ui();print('PASS: dates disclosed',flush=True)
 choose('to_asset','metals_tab','Silver');check('250,000 troy oz of Silver')
 choose('from_asset','world_tab','Brazilian Real','BRL');choose('to_asset','world_tab','Philippine Peso','PHP');check('1,200.00 PHP')
 choose('from_asset','metals_tab','Palladium');choose('to_asset','metals_tab','Palladium');check('100 troy oz of Palladium')
 launch();bottom();bottom();shot('footer');tap('about');shot('about');assert b'Version 4.0' in ui();adb('shell','input','keyevent','4')
 adb('shell','settings','put','system','font_scale','1.3');adb('shell','wm','size','360x800');launch();shot('large-text');tap('from_asset');tap('metals_tab');shot('large-picker');tap('Gold',True);bottom();bottom();shot('large-footer')
 logs=adb('logcat','-d');(OUT/'logcat.txt').write_bytes(logs);assert b'FATAL EXCEPTION' not in logs
 print('ALL DEVICE CHECKS PASSED',flush=True)
 for name in ['home','metals-picker','crypto-picker','footer','large-picker','large-footer']:
  print('SCREENSHOT_'+name+'='+base64.b64encode((OUT/(name+'.png')).read_bytes()).decode(),flush=True)
finally:
 adb('shell','settings','put','system','font_scale','1.0')
