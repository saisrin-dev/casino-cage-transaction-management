import {createServer} from 'node:http';
import {readFile,stat} from 'node:fs/promises';
import {resolve,extname} from 'node:path';
const root=resolve(process.env.STATIC_DIR||'frontend/dist/cage/browser');
const types={'.html':'text/html; charset=utf-8','.js':'application/javascript','.css':'text/css','.svg':'image/svg+xml','.png':'image/png','.ico':'image/x-icon','.woff2':'font/woff2'};
createServer(async(req,res)=>{
  const path=new URL(req.url,'http://localhost').pathname;
  if(path==='/actuator/health/readiness'||path==='/actuator/health'){res.writeHead(200,{'Content-Type':'application/json'});res.end('{"status":"UP","mode":"browser-demo"}');return;}
  if(req.method!=='GET'&&req.method!=='HEAD'){res.writeHead(405);res.end();return;}
  try{let file=resolve(root,'.'+decodeURIComponent(path));if(file!==root&&!file.startsWith(root+'/')){res.writeHead(403);res.end();return;}
    try{if((await stat(file)).isDirectory())file=resolve(file,'index.html');}catch{if(extname(file)){res.writeHead(404);res.end();return;}file=resolve(root,'index.html');}
    const data=await readFile(file);res.writeHead(200,{'Content-Type':types[extname(file)]||'application/octet-stream','Cache-Control':'no-cache','X-Content-Type-Options':'nosniff'});res.end(req.method==='HEAD'?undefined:data);
  }catch{res.writeHead(404);res.end('Not found');}
}).listen(Number(process.env.PORT||(process.argv.includes('--port')?process.argv[process.argv.indexOf('--port')+1]:undefined)||8080),'0.0.0.0',()=>console.log('CageLedger browser demo ready; no database or login required.'));
