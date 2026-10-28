module.exports=async function swipe(page,x1,y1,x2,y2){
 const session=await page.context().newCDPSession(page);
 try{await session.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x:x1,y:y1}]});for(let n=1;n<=12;n++)await session.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:x1+(x2-x1)*n/12,y:y1+(y2-y1)*n/12}]});await session.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});await page.waitForTimeout(80);}finally{await session.detach();}
};
