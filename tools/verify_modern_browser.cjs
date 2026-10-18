const {chromium}=require('D:/speclinker-main/node_modules/playwright-core');
const fs=require('fs'),path=require('path'),cp=require('child_process'),assert=require('assert/strict');
const base='http://127.0.0.1:18985',ui='http://127.0.0.1:18772',out='D:/sl-shop-runtime/setup/browser';
fs.mkdirSync(out,{recursive:true});
(async()=>{const b=await chromium.launch({channel:'chrome',headless:true,args:['--remote-debugging-port=19225']});const report={status:'running',checks:[]};let current;
try{
 const c=await b.newContext(),p=await c.newPage();current=p;
 await p.goto(base+'/shop#/login');await p.getByLabel('이메일',{exact:true}).fill('buyer@speclinker.local');await p.getByLabel(/^비밀번호/).fill('Shop-demo-local-20261018!');
 const login=p.waitForResponse(r=>r.url().endsWith('/api/members/login')&&r.request().method()==='POST');await p.getByRole('button',{name:'로그인',exact:true}).click();assert.equal((await login).status(),200);await p.waitForFunction(()=>!!localStorage.getItem('sl.member.session'));
 await p.goto(base+'/shop#/shop/cart');await p.getByRole('heading',{name:'장바구니',exact:true}).waitFor();
 const log=cp.execFileSync(process.execPath,['D:/speclinker-main/scripts/e2e_capture_state.js','--ws','D:/sl-shop','--env','local'],{encoding:'utf8',timeout:30000});fs.writeFileSync(path.join(out,'capture.log'),log);
 const reuse=await b.newContext({storageState:'D:/sl-shop/.speclinker/e2e_storage.json'}),r=await reuse.newPage();
 const response=r.waitForResponse(x=>x.url().includes('/api/cart?memberId=M-9001'));await r.goto(base+'/shop#/shop/cart');assert.equal((await response).status(),200);await r.getByText('담긴 상품이 없습니다',{exact:true}).waitFor();report.checks.push({kind:'native CDP capture and fresh-context authenticated reuse',passed:true});
 for(const width of [1440,390]){
  const ctx=await b.newContext({viewport:{width,height:1000}}),q=await ctx.newPage();current=q;const errors=[];q.on('pageerror',e=>errors.push(String(e)));
  const ping=await(await ctx.request.get(ui+'/api/ping',{headers:{'X-SL':'1'}})).json();assert.equal(path.resolve(ping.ws),path.resolve('D:/sl-shop'));
  await q.goto(ui+'/docs/viewer2/index.html#/work');await q.getByTestId('stream-new').click();const dialog=q.getByRole('dialog',{name:'요청 만들기',exact:true});assert.equal(await dialog.getByRole('radio',{name:/일반 요청/}).isChecked(),true);await q.screenshot({path:path.join(out,'intake-'+width+'.png'),fullPage:true});
  await q.goto(ui+'/docs/viewer2/index.html#/sr/SR-902?view=work');await q.getByRole('list',{name:'단계 진행',exact:true}).waitFor();assert.equal(await q.getByRole('list',{name:'단계 진행',exact:true}).locator('li').count(),7);
  await q.getByTestId('tobe-edit').click();const editor=q.getByRole('region',{name:'변경명세 편집',exact:true});await editor.waitFor();assert.ok(await editor.locator('fieldset').count()>0);await q.screenshot({path:path.join(out,'design-editor-'+width+'.png'),fullPage:true});
  assert.equal(await q.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false);assert.deepEqual(errors,[]);
  report.checks.push({width,workspace:'D:/sl-shop',general_intake:true,steps:7,native_design_editor:true,page_errors:errors,horizontal_overflow:false});await ctx.close();
 }
 report.status='passed';
}catch(e){report.status='failed';report.error=String(e);if(current&&!current.isClosed())await current.screenshot({path:path.join(out,'failure.png'),fullPage:true});throw e}
finally{fs.writeFileSync(path.join(out,'summary.json'),JSON.stringify(report,null,2));await b.close()}
console.log(JSON.stringify({status:report.status,checks:report.checks.length}));
})().catch(e=>{console.error(e);process.exitCode=1});
