(function(){
"use strict";
function addCell(row,value,header){
  var cell=document.createElement(header?"th":"td");
  cell.textContent=String(value==null?"":value);
  cell.style.cssText="padding:7px 5px;border:1px solid #d9e2f0;text-align:right;vertical-align:top;overflow-wrap:anywhere;"+(header?"background:#ebf2fd;color:#2458a7;font-weight:bold;":"");
  row.appendChild(cell);
}
window.MasarExportDriverOrders=async function(driver,shipments){
  if(!driver||!shipments||!shipments.length)throw new Error("اختر مندوبًا لديه شحنات أولًا");
  if(typeof window.html2pdf!=="function")throw new Error("مكتبة إنشاء PDF لم تُحمّل؛ تأكد من الاتصال بالإنترنت");
  var box=document.createElement("div");
  box.lang="ar";box.dir="rtl";box.style.cssText="position:relative;left:auto;top:auto;width:950px;padding:32px;background:#fff;color:#142746;direction:rtl;font-family:Cairo,Tahoma,Arial,sans-serif;box-sizing:border-box;";
  var title=document.createElement("h2");title.textContent="مَسار | كشف أوردرات المندوب";title.style.cssText="font-size:25px;color:#265dd0;border-bottom:3px solid #265dd0;padding-bottom:12px;margin:0 0 12px;";box.appendChild(title);
  var meta=document.createElement("p");meta.textContent="المندوب: "+driver.name+" | الهاتف: "+(driver.phone||"—")+" | عدد الأوردرات: "+shipments.length+" | تاريخ التصدير: "+new Date().toLocaleDateString("ar-EG");meta.style="font-size:13px;margin:0 0 18px";box.appendChild(meta);
  var table=document.createElement("table");table.style.cssText="width:100%;border-collapse:collapse;table-layout:fixed;direction:rtl;font-size:11px;";
  var tr=document.createElement("tr");["م","رقم الشحنة","العميل","الهاتف","العنوان","المنتج","الإجمالي","المقدم","المطلوب تحصيله","الحالة المسجلة","الحالة"].forEach(function(n){addCell(tr,n,true)});
  var head=document.createElement("thead");head.appendChild(tr);table.appendChild(head);
  var body=document.createElement("tbody");var sum=0,advanceSum=0,remainingSum=0;
  shipments.forEach(function(s,i){
    var row=document.createElement("tr");row.style.pageBreakInside="avoid";
    var amount=Number(s.total)||0,advance=Number(s.advance_amount)||0,remaining=Math.max(0,amount-advance);sum+=amount;advanceSum+=advance;remainingSum+=remaining;
    var status=(window.MasarOrderStatuses&&window.MasarOrderStatuses[s.status])||s.status;
    [i+1,s.tracking,s.customer,s.phone,s.address,s.product,(amount/100).toFixed(2),(advance/100).toFixed(2),(remaining/100).toFixed(2),status,""].forEach(function(v){addCell(row,v,false)});
    body.appendChild(row);
  });
  table.appendChild(body);box.appendChild(table);
  var footer=document.createElement("p");footer.textContent="عدد الشحنات: "+shipments.length+" | إجمالي قيمة الشحنات: "+(sum/100).toFixed(2)+" ج.م | المقدم: "+(advanceSum/100).toFixed(2)+" ج.م | المطلوب تحصيله: "+(remainingSum/100).toFixed(2)+" ج.م";footer.style.cssText="font-size:12px;margin-top:18px;border-top:1px solid #cbd5e7;padding-top:12px;";box.appendChild(footer);
  // Keep the source off-screen, but never copy that off-screen positioning into
  // html2pdf's cloned document (otherwise html2canvas captures a blank page).
  var staging=document.createElement("div");
  staging.style.cssText="position:fixed;left:-12000px;top:0;width:1000px;pointer-events:none;overflow:visible;";
  staging.appendChild(box);
  document.body.appendChild(staging);
  try{
    if(document.fonts&&document.fonts.ready)await document.fonts.ready;
    var name="Masar-Orders-"+driver.id+"-"+new Date().toISOString().slice(0,10)+".pdf";
    var worker=window.html2pdf().set({margin:7,image:{type:"jpeg",quality:0.95},html2canvas:{scale:1.5,useCORS:true,backgroundColor:"#fff",scrollX:0,scrollY:0},jsPDF:{unit:"mm",format:"a4",orientation:"landscape"},pagebreak:{mode:["css","legacy"],avoid:["tr"]}}).from(box);
    var canvas=await worker.toCanvas().get("canvas");
    if(!canvas||canvas.width<50||canvas.height<50)throw new Error("تعذر إنشاء محتوى PDF");
    // Reject blank renders rather than saving a deceptively empty PDF.
    var ctx=canvas.getContext("2d"), pixels=ctx.getImageData(0,0,Math.min(canvas.width,1400),Math.min(canvas.height,360)).data, ink=0;
    for(var p=0;p<pixels.length;p+=16){
      if(pixels[p]<215||pixels[p+1]<215||pixels[p+2]<215)ink++;
    }
    if(ink<25)throw new Error("تصدير PDF طلع فارغ، حاول مرة أخرى");
    var doc=await worker.toPdf().get("pdf");
    if(window.MasarAndroid&&typeof window.MasarAndroid.savePdf==="function")window.MasarAndroid.savePdf(doc.output("datauristring"),name);
    else doc.save(name);
  }finally{staging.remove();}
};
})();