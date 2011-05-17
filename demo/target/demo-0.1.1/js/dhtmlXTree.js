//<![CDATA[

function dhtmlXTreeObject(htmlObject,width,height,rootId){
 if(typeof(htmlObject)!="object")
 this.parentObject=document.getElementById(htmlObject);
 else
 this.parentObject=htmlObject;
 this.mytype="tree";
 this.width=width;
 this.height=height;
 this.rootId=rootId;

 this.style_pointer="pointer";
 if(navigator.appName == 'Microsoft Internet Explorer')this.style_pointer="hand";

 this.hfMode=0;
 this.nodeCut=0;
 this.XMLsource=0;
 this.XMLloadingWarning=0;
 this._globalIdStorage=new Array();
 this.globalNodeStorage=new Array();
 this._globalIdStorageSize=0;
 this.treeLinesOn=true;
 this.checkFuncHandler=0;
 this.openFuncHandler=0;
 this.dblclickFuncHandler=0;
 this.tscheck=false;
 this.timgen=true;

 this.imPath="treeImgs/";
 this.checkArray=new Array("iconUnCheckAll.gif","iconCheckAll.gif","iconCheckGray.gif");
 this.lineArray=new Array("line2.gif","line3.gif","line4.gif","blank.gif","blank.gif");
 this.minusArray=new Array("minus2.gif","minus3.gif","minus4.gif","minus.gif","minus5.gif");
 this.plusArray=new Array("plus2.gif","plus3.gif","plus4.gif","plus.gif","plus5.gif");
 this.imageArray=new Array("leaf.gif","folderOpen.gif","folderClosed.gif");
 this.cutImg= new Array(0,0,0);
 this.cutImage="but_cut.gif";

 this.dragger= new dhtmlDragAndDropObject();
 this.htmlNode=new dhtmlXTreeItemObject(this.rootId,"",0,this);
 this.htmlNode.htmlNode.childNodes[0].childNodes[0].style.display="none";
 this.htmlNode.htmlNode.childNodes[0].childNodes[0].childNodes[0].className="hiddenRow";
 this.allTree=this._createSelf();
 this.allTree.appendChild(this.htmlNode.htmlNode);
 this.allTree.onselectstart=new Function("return false;");
 this.XMLLoader=new dtmlXMLLoaderObject(this._parseXMLTree,this);

 this.dragger.addDragLanding(this.allTree,this);
 return this;
};


function dhtmlXTreeItemObject(itemId,itemText,parentObject,treeObject,actionHandler,mode){
 this.htmlNode="";
 this.acolor="";
 this.scolor="";
 this.tr=0;
 this.childsCount=0;
 this.tempDOMM=0;
 this.tempDOMU=0;
 this.dragSpan=0;
 this.dragMove=0;
 this.span=0;
 this.closeble=1;
 this.childNodes=new Array();

 this.checkstate=0;
 this.treeNod=treeObject;
 this.label=itemText;
 this.parentObject=parentObject;
 this.actionHandler=actionHandler;
 this.images=new Array(treeObject.imageArray[0],treeObject.imageArray[1],treeObject.imageArray[2]);


 this.id=treeObject._globalIdStorageAdd(itemId,this);
 if(this.treeNod.checkBoxOff)this.htmlNode=this.treeNod._createItem(1,this,mode);
 else this.htmlNode=this.treeNod._createItem(0,this,mode);
 this.htmlNode.objBelong=this;
 return this;
};



 dhtmlXTreeObject.prototype._globalIdStorageAdd=function(itemId,itemObject){
 if(this._globalIdStorageFind(itemId)){d=new Date();itemId=d.valueOf()+"_"+itemId;return this._globalIdStorageAdd(itemId,itemObject);}
 this._globalIdStorage[this._globalIdStorageSize]=itemId;
 this.globalNodeStorage[this._globalIdStorageSize]=itemObject;
 this._globalIdStorageSize++;
 return itemId;
};

 dhtmlXTreeObject.prototype._globalIdStorageSub=function(itemId){
 for(var i=0;i<this._globalIdStorageSize;i++)
 if(this._globalIdStorage[i]==itemId)
{
 this._globalIdStorage[i]=this._globalIdStorage[this._globalIdStorageSize-1];
 this.globalNodeStorage[i]=this.globalNodeStorage[this._globalIdStorageSize-1];
 this._globalIdStorageSize--;
 this._globalIdStorage[this._globalIdStorageSize]=0;
 this.globalNodeStorage[this._globalIdStorageSize]=0;
}
};


 dhtmlXTreeObject.prototype._globalIdStorageFind=function(itemId){
 for(var i=0;i<this._globalIdStorageSize;i++)
 if(this._globalIdStorage[i]==itemId)
 return this.globalNodeStorage[i];
 return 0;
};




 dhtmlXTreeObject.prototype._drawNewTr=function(htmlObject)
{
 var tr =document.createElement('tr');
 var td1=document.createElement('td');
 var td2=document.createElement('td');
 td1.appendChild(document.createTextNode(" "));
 td2.colSpan=3;td2.appendChild(htmlObject);tr.appendChild(td1);tr.appendChild(td2);
 return tr;
};

 dhtmlXTreeObject.prototype.loadXMLString=function(xmlString){this.XMLLoader.loadXMLString(xmlString);};

 dhtmlXTreeObject.prototype.loadXML=function(file){this.XMLLoader.loadXML(file);};

 dhtmlXTreeObject.prototype._attachChildNode=function(parentObject,itemId,itemText,itemActionHandler,image1,image2,image3,optionStr,childs,beforeNode){
 if(beforeNode)parentObject=beforeNode.parentObject;
 if(((parentObject.XMLload==0)&&(this.XMLsource))&&(!this.XMLloadingWarning))
{
 parentObject.XMLload=1;this.loadXML(this.XMLsource+getUrlSymbol(this.XMLsource)+"itemId="+escape(parentObject.itemId));
}

 var Count=parentObject.childsCount;
 var Nodes=parentObject.childNodes;

 if((!itemActionHandler)&&(this.aFunc))itemActionHandler=this.aFunc;
 Nodes[Count]=new dhtmlXTreeItemObject(itemId,itemText,parentObject,this,itemActionHandler,1);

 if(image1)Nodes[Count].images[0]=image1;
 if(image2)Nodes[Count].images[1]=image2;
 if(image3)Nodes[Count].images[2]=image3;

 parentObject.childsCount++;
 var tr=this._drawNewTr(Nodes[Count].htmlNode);
 if(this.XMLloadingWarning)
 Nodes[Count].htmlNode.parentNode.parentNode.style.display="none";

 if(optionStr){
 var tempStr=optionStr.split(",");
 for(var i=0;i<tempStr.length;i++)
{
 switch(tempStr[i])
{
 case "TOP": if(parentObject.childsCount>1)beforeNode=parentObject.htmlNode.childNodes[0].childNodes[1].nodem.previousSibling;break;
}
};
};

 if((beforeNode)&&(beforeNode.tr.nextSibling))
 parentObject.htmlNode.childNodes[0].insertBefore(tr,beforeNode.tr.nextSibling);
 else
 parentObject.htmlNode.childNodes[0].appendChild(tr);

 if(this.XMLsource)if((childs)&&(childs!=0))Nodes[Count].XMLload=0;else Nodes[Count].XMLload=1;

 Nodes[Count].tr=tr;
 tr.nodem=Nodes[Count];

 if(parentObject.itemId==0)
 tr.childNodes[0].className="hitemIddenRow";

 if(optionStr){
 var tempStr=optionStr.split(",");
 for(var i=0;i<tempStr.length;i++)
{
 switch(tempStr[i])
{
 case "SELECT": this.selectItem(itemId,false);break;
 case "CALL": this.selectItem(itemId,true);break;
 case "CHILD": Nodes[Count].XMLload=0;break;
 case "CHECKED":
 if(this.XMLloadingWarning)
 this.setCheckList+=itemId;
 else
 this.setCheck(itemId,1);
 break;
 case "OPEN": Nodes[Count].openMe=1;break;
}
};
};

 if(!this.XMLloadingWarning)
{
 if(this._getOpenState(parentObject)<0)
 this.openItem(parentObject.id);

 if(beforeNode)
{
 this._correctPlus(beforeNode);
 this._correctLine(beforeNode);
}
 this._correctPlus(parentObject);
 this._correctLine(parentObject);
 this._correctPlus(Nodes[Count]);
 if(parentObject.childsCount>=2)
{
 this._correctPlus(Nodes[parentObject.childsCount-2]);
 this._correctLine(Nodes[parentObject.childsCount-2]);
}
 if(parentObject.childsCount!=2)this._correctPlus(Nodes[0]);
 if(this.tscheck)this._correctCheckStates(parentObject);
}

 return Nodes[Count];
};


 dhtmlXTreeObject.prototype.insertNewItem=function(parentId,itemId,itemText,itemActionHandler,image1,image2,image3,optionStr,childs){
 var parentObject=this._globalIdStorageFind(parentId);
 if(!parentObject)return(-1);
 return this._attachChildNode(parentObject,itemId,itemText,itemActionHandler,image1,image2,image3,optionStr,childs);
};

 dhtmlXTreeObject.prototype._parseXMLTree=function(dhtmlObject,node,parentId,level){
 dhtmlObject.XMLloadingWarning=1;
 var nodeAskingCall="";
 if(!node){
 node=dhtmlObject.XMLLoader.getXMLTopNode("tree");
 parentId=node.getAttribute("id");
 dhtmlObject.setCheckList="";
}

 for(var i=0;i<node.childNodes.length;i++)
{
 if((node.childNodes[i].nodeType==1)&&(node.childNodes[i].tagName == "item"))
{
 var name=node.childNodes[i].getAttribute("text");
 var cId=node.childNodes[i].getAttribute("id");

 var im0=node.childNodes[i].getAttribute("im0");
 var im1=node.childNodes[i].getAttribute("im1");
 var im2=node.childNodes[i].getAttribute("im2");

 var aColor=node.childNodes[i].getAttribute("aCol");
 var sColor=node.childNodes[i].getAttribute("sCol");

 var chd=node.childNodes[i].getAttribute("child");

 var atop=node.childNodes[i].getAttribute("top");
 var aopen=node.childNodes[i].getAttribute("open");
 var aselect=node.childNodes[i].getAttribute("select");
 var acall=node.childNodes[i].getAttribute("call");
 var achecked=node.childNodes[i].getAttribute("checked");
 var closeable=node.childNodes[i].getAttribute("closeable");

 var zST="";
 if(aselect)zST+=",SELECT";
 if(atop)zST+=",TOP";
 if(acall)nodeAskingCall=cId;
 if(achecked)zST+=",CHECKED";
 if((aopen)&&(aopen!="0"))zST+=",OPEN";

 var temp=dhtmlObject._globalIdStorageFind(parentId);
 temp.XMLload=1;
 dhtmlObject.insertNewItem(parentId,cId,name,0,im0,im1,im2,zST,chd);
 if(dhtmlObject.parserExtension)dhtmlObject.parserExtension._parseExtension(node.childNodes[i],dhtmlObject.parserExtension,cId,parentId);
 dhtmlObject.setItemColor(cId,aColor,sColor);
 if((closeable=="0")||(closeable=="1"))dhtmlObject.setItemCloseable(cId,closeable);
 var zcall=dhtmlObject._parseXMLTree(dhtmlObject,node.childNodes[i],cId,1);
 if(zcall!="")nodeAskingCall=zcall;
}
 else
 if((node.childNodes[i].nodeType==1)&&(node.childNodes[i].tagName == "userdata"))
{
 var name=node.childNodes[i].getAttribute("name");
 if((name)&&(node.childNodes[i].childNodes[0])){
 dhtmlObject.setUserData(parentId,name,node.childNodes[i].childNodes[0].data);
};
};
};

 if(!level){
 dhtmlObject.lastLoadedXMLId=parentId;
 dhtmlObject._redrawFrom(dhtmlObject);
 dhtmlObject.XMLloadingWarning=0;
 var chArr=dhtmlObject.setCheckList.split(",");
 for(var n=0;n<chArr.length;n++)
 if(chArr[n])dhtmlObject.setCheck(chArr[n],1);
 if(nodeAskingCall!="")dhtmlObject.selectItem(nodeAskingCall,true);
}
 return nodeAskingCall;
};


 dhtmlXTreeObject.prototype._redrawFrom=function(dhtmlObject,itemObject){
 if(!itemObject){
 var tempx=dhtmlObject._globalIdStorageFind(dhtmlObject.lastLoadedXMLId);
 dhtmlObject.lastLoadedXMLId=-1;
 if(!tempx)return 0;
}
 else tempx=itemObject;
 for(var i=0;i<tempx.childsCount;i++)
{
 if(!itemObject)tempx.childNodes[i].htmlNode.parentNode.parentNode.style.display="";
 if(tempx.childNodes[i].openMe==1)
 for(var zy=0;zy<tempx.childNodes[i].childNodes.length;zy++)
 tempx.childNodes[i].htmlNode.childNodes[0].childNodes[zy+1].style.display="";
 dhtmlObject._redrawFrom(dhtmlObject,tempx.childNodes[i]);
 dhtmlObject._correctLine(tempx.childNodes[i]);
 dhtmlObject._correctPlus(tempx.childNodes[i]);
};
 dhtmlObject._correctLine(tempx);
 dhtmlObject._correctPlus(tempx);
};


 dhtmlXTreeObject.prototype._createSelf=function(){
 var div=document.createElement('div');
 div.className="containerTableStyle";
 div.style.width=this.width;
 div.style.height=this.height;
 this.parentObject.appendChild(div);
 return div;
};


 dhtmlXTreeObject.prototype._xcloseAll=function(itemObject)
{
 if(this.rootId!=itemObject.id)this._HideShow(itemObject,1);
 for(var i=0;i<itemObject.childsCount;i++)
 this._xcloseAll(itemObject.childNodes[i]);
};

 dhtmlXTreeObject.prototype._xopenAll=function(itemObject)
{
 this._HideShow(itemObject,2);
 for(var i=0;i<itemObject.childsCount;i++)
 this._xopenAll(itemObject.childNodes[i]);
};

 dhtmlXTreeObject.prototype._correctPlus=function(itemObject){
 var workArray=this.lineArray;
 if((this.XMLsource)&&(!itemObject.XMLload))
{
 var workArray=this.plusArray;
 itemObject.htmlNode.childNodes[0].childNodes[0].childNodes[2].childNodes[0].src=this.imPath+itemObject.images[2];
}
 else
try{
 if(itemObject.childsCount)
{
 if(itemObject.htmlNode.childNodes[0].childNodes[1].style.display!="none")
{
 var workArray=this.minusArray;
 itemObject.htmlNode.childNodes[0].childNodes[0].childNodes[2].childNodes[0].src=this.imPath+itemObject.images[1];
}
 else
{
 var workArray=this.plusArray;
 itemObject.htmlNode.childNodes[0].childNodes[0].childNodes[2].childNodes[0].src=this.imPath+itemObject.images[2];
}
}
 else
{
 itemObject.htmlNode.childNodes[0].childNodes[0].childNodes[2].childNodes[0].src=this.imPath+itemObject.images[0];
}
}
 catch(e){};

 var tempNum=2;
 if(!itemObject.treeNod.treeLinesOn)itemObject.htmlNode.childNodes[0].childNodes[0].childNodes[0].childNodes[0].src=this.imPath+workArray[3];
 else{
 if(itemObject.parentObject)tempNum=this._getCountStatus(itemObject.id,itemObject.parentObject);
 itemObject.htmlNode.childNodes[0].childNodes[0].childNodes[0].childNodes[0].src=this.imPath+workArray[tempNum];
}
};

 dhtmlXTreeObject.prototype._correctLine=function(itemObject){
 var sNode=itemObject.parentObject;
 try{
 if(sNode)
 if((this._getLineStatus(itemObject.id,sNode)==0)||(!this.treeLinesOn))
{
 for(var i=1;i<=itemObject.childsCount;i++)
{
 itemObject.htmlNode.childNodes[0].childNodes[i].childNodes[0].style.backgroundImage="";
 itemObject.htmlNode.childNodes[0].childNodes[i].childNodes[0].style.backgroundRepeat="";
}
}
 else
 for(var i=1;i<=itemObject.childsCount;i++)
{
 itemObject.htmlNode.childNodes[0].childNodes[i].childNodes[0].style.backgroundImage="url("+this.imPath+"line1.gif)";
 itemObject.htmlNode.childNodes[0].childNodes[i].childNodes[0].style.backgroundRepeat="repeat-y";
}
}
 catch(e){};
};

 dhtmlXTreeObject.prototype._getCountStatus=function(itemId,itemObject){
 try{
 if(itemObject.childsCount<=1){if(itemObject.id==this.rootId)return 4;else return 0;}

 if(itemObject.htmlNode.childNodes[0].childNodes[1].nodem.id==itemId)if(!itemObject.id)return 2;else return 1;
 if(itemObject.htmlNode.childNodes[0].childNodes[itemObject.childsCount].nodem.id==itemId)return 0;
}
 catch(e){};
 return 1;
};

 dhtmlXTreeObject.prototype._getLineStatus =function(itemId,itemObject){
 if(itemObject.htmlNode.childNodes[0].childNodes[itemObject.childsCount].nodem.id==itemId)return 0;
 return 1;
}


 dhtmlXTreeObject.prototype._HideShow=function(itemObject,mode){
 if(((this.XMLsource)&&(!itemObject.XMLload))&&(!mode)){itemObject.XMLload=1;this.loadXML(this.XMLsource+getUrlSymbol(this.XMLsource)+"id="+escape(itemObject.id));return;};
 var Nodes=itemObject.htmlNode.childNodes[0].childNodes;var Count=Nodes.length;
 if(Count>1){
 if(((Nodes[1].style.display!="none")||(mode==1))&&(mode!=2))nodestyle="none";else nodestyle="";
 for(var i=1;i<Count;i++)
 Nodes[i].style.display=nodestyle;
}
 this._correctPlus(itemObject);
}

 dhtmlXTreeObject.prototype._getOpenState=function(itemObject){
 var z=itemObject.htmlNode.childNodes[0].childNodes;
 if(z.length<=1)return 0;
 if(z[1].style.display!="none")return 1;
 else return -1;
}




 dhtmlXTreeObject.prototype.onRowClick2=function(){
 if(this.parentObject.treeNod.dblclickFuncHandler)if(!this.parentObject.treeNod.dblclickFuncHandler(this.parentObject.id))return 0;
 if((this.parentObject.closeble)&&(this.parentObject.closeble!="0"))
 this.parentObject.treeNod._HideShow(this.parentObject);
 else
 this.parentObject.treeNod._HideShow(this.parentObject,2);
};

 dhtmlXTreeObject.prototype.onRowClick=function(){
 if(this.parentObject.treeNod.openFuncHandler)if(!this.parentObject.treeNod.openFuncHandler(this.parentObject.id,this.parentObject.treeNod._getOpenState(this.parentObject)))return 0;
 if((this.parentObject.closeble)&&(this.parentObject.closeble!="0"))
 this.parentObject.treeNod._HideShow(this.parentObject);
 else
 this.parentObject.treeNod._HideShow(this.parentObject,2);
};


 dhtmlXTreeObject.prototype.onRowSelect=function(e,htmlObject,mode){
 if(!htmlObject)htmlObject=this;
 htmlObject.childNodes[0].className="selectedTreeRow";
 if(htmlObject.parentObject.scolor)htmlObject.parentObject.span.style.color=htmlObject.parentObject.scolor;
 if((htmlObject.parentObject.treeNod.lastSelected)&&(htmlObject.parentObject.treeNod.lastSelected!=htmlObject))
{
 htmlObject.parentObject.treeNod.lastSelected.childNodes[0].className="standartTreeRow";
 if(htmlObject.parentObject.treeNod.lastSelected.parentObject.acolor)htmlObject.parentObject.treeNod.lastSelected.parentObject.span.style.color=htmlObject.parentObject.treeNod.lastSelected.parentObject.acolor;
}
 htmlObject.parentObject.treeNod.lastSelected=htmlObject;
 if(!mode){if(htmlObject.parentObject.actionHandler)htmlObject.parentObject.actionHandler(htmlObject.parentObject.id);}
};






dhtmlXTreeObject.prototype._correctCheckStates=function(dhtmlObject){
 if(!this.tscheck)return;
 if(dhtmlObject.id==this.rootId)return;
 var act=dhtmlObject.htmlNode.childNodes[0].childNodes;
 var flag1=0;var flag2=0;
 if(act.length<2)return;
 for(var i=1;i<act.length;i++)
 if(act[i].nodem.checkstate==0)flag1=1;
 else if(act[i].nodem.checkstate==1)flag2=1;
 else{flag1=1;flag2=1;break;}

 if((flag1)&&(flag2))this._setCheck(dhtmlObject,"notsure");
 else if(flag1)this._setCheck(dhtmlObject,false);
 else this._setCheck(dhtmlObject,true);

 this._correctCheckStates(dhtmlObject.parentObject);
}


 dhtmlXTreeObject.prototype.onCheckBoxClick=function(e){
 if(this.treeNod.tscheck)
 if(this.parentObject.checkstate==1)this.treeNod._setSubChecked(false,this.parentObject);
 else this.treeNod._setSubChecked(true,this.parentObject);
 else
 if(this.parentObject.checkstate==1)this.treeNod._setCheck(this.parentObject,false);
 else this.treeNod._setCheck(this.parentObject,true);
 this.treeNod._correctCheckStates(this.parentObject.parentObject);
 if(this.treeNod.checkFuncHandler)return(this.treeNod.checkFuncHandler(this.parentObject.id,this.parentObject.checkstate));
 else return true;
};

 dhtmlXTreeObject.prototype._createItem=function(acheck,itemObject,mode){
 var table=document.createElement('table');
 table.cellSpacing=0;table.cellPadding=0;
 table.border=0;
 if(this.hfMode)table.style.tableLayout="fixed";
 table.style.margin=0;table.style.padding=0;

 var tbody=document.createElement('tbody');
 var tr=document.createElement('tr');
 var td1=document.createElement('td');
 td1.className="standartTreeImage";
 var img0=document.createElement("img");
 img0.border="0";td1.appendChild(img0);img0.style.padding=0;

 var td11=document.createElement('td');
 var inp=document.createElement("img");inp.checked=0;inp.src=this.imPath+this.checkArray[0];inp.style.width="16px";inp.style.height="16px";

 if(!acheck)inp.style.display="none";
 td11.appendChild(inp);
 td11.width="16px";
 inp.onclick=this.onCheckBoxClick;
 inp.treeNod=this;
 inp.parentObject=itemObject;

 var td12=document.createElement('td');
 td12.className="standartTreeImage";
 var img=document.createElement("img");img.onmousedown=this._preventNsDrag;
 img.border="0";
 if(!mode)img.src=this.imPath+this.imageArray[0];
 td12.appendChild(img);img.style.padding=0;img.style.margin=0;
 if(this.timgen)
{img.style.width="18px";img.style.height="18px";}
 else
{img.style.width="0px";img.style.height="0px";}


 var td2=document.createElement('td');
 td2.noWrap=true;
 itemObject.span=document.createElement('span');
 itemObject.span.className="standartTreeRow";
 td2.style.width="100%";
 itemObject.span.appendChild(document.createTextNode(itemObject.label));
 td2.appendChild(itemObject.span);
 td2.parentObject=itemObject;td1.parentObject=itemObject;
 td2.onclick=this.onRowSelect;td1.onclick=this.onRowClick;td2.ondblclick=this.onRowClick2;

 if(this.dragAndDropOff)this.dragger.addDraggableItem(td2,this);

 td2.style.verticalAlign="";
 td2.style.cursor=this.style_pointer;
 tr.appendChild(td1);tr.appendChild(td11);tr.appendChild(td12);
 tr.appendChild(td2);
 tbody.appendChild(tr);
 table.appendChild(tbody);
 return table;
};



 dhtmlXTreeObject.prototype.setImagePath=function(newPath){this.imPath=newPath;};



 dhtmlXTreeObject.prototype.setOnClickHandler=function(func){if(typeof(func)=="function")this.aFunc=func;else this.aFunc=eval(func);};


 dhtmlXTreeObject.prototype.setXMLAutoLoading=function(filePath){this.XMLsource=filePath;};

 dhtmlXTreeObject.prototype.setOnCheckHandler=function(func){if(typeof(func)=="function")this.checkFuncHandler=func;else this.checkFuncHandler=eval(func);};



 dhtmlXTreeObject.prototype.setOnOpenHandler=function(func){if(typeof(func)=="function")this.openFuncHandler=func;else this.openFuncHandler=eval(func);};


 dhtmlXTreeObject.prototype.setOnDblClickHandler=function(func){if(typeof(func)=="function")this.dblclickFuncHandler=func;else this.dblclickFuncHandler=eval(func);};










 dhtmlXTreeObject.prototype.openAllItems=function(itemId)
{
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;
 this._xopenAll(temp);
};


 dhtmlXTreeObject.prototype.getOpenState=function(itemId){
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return "";
 return this._getOpenState(temp);
};


 dhtmlXTreeObject.prototype.closeAllItems=function(itemId)
{
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;
 this._xcloseAll(temp);
};



 dhtmlXTreeObject.prototype.setUserData=function(itemId,name,value){
 var sNode=this._globalIdStorageFind(itemId);
 if(!sNode)return;
 if(name=="hint")sNode.htmlNode.childNodes[0].childNodes[0].title=value;
 sNode[name]=value;
};


 dhtmlXTreeObject.prototype.getUserData=function(itemId,name){
 var sNode=this._globalIdStorageFind(itemId);
 if(!sNode)return;
 return eval("sNode."+name);
};


 dhtmlXTreeObject.prototype.getSelectedItemId=function()
{
 if(this.lastSelected)
 if(this._globalIdStorageFind(this.lastSelected.parentObject.id))
 return this.lastSelected.parentObject.id;
 return("");
};


 dhtmlXTreeObject.prototype.getItemColor=function(itemId)
{
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;

 var res= new Object();
 if(temp.acolor)res.acolor=temp.acolor;
 if(temp.acolor)res.scolor=temp.scolor;
 return res;
};

 dhtmlXTreeObject.prototype.setItemColor=function(itemId,defaultColor,selectedColor)
{
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;
 else{
 if((this.lastSelected)&&(temp.tr==this.lastSelected.parentObject.tr))
{if(selectedColor)temp.span.style.color=selectedColor;}
 else
{if(defaultColor)temp.span.style.color=defaultColor;}

 if(selectedColor)temp.scolor=selectedColor;
 if(defaultColor)temp.acolor=defaultColor;
}
};


 dhtmlXTreeObject.prototype.getItemText=function(itemId)
{
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;
 return(temp.htmlNode.childNodes[0].childNodes[0].childNodes[3].childNodes[0].innerHTML);
};

 dhtmlXTreeObject.prototype.getParentId=function(itemId)
{
 var temp=this._globalIdStorageFind(itemId);
 if((!temp)||(!temp.parentObject))return "";
 return temp.parentObject.id;
};




 dhtmlXTreeObject.prototype.changeItemId=function(itemId,newItemId)
{
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;
 temp.id=newItemId;
 for(var i=0;i<this._globalIdStorageSize;i++)
 if(this._globalIdStorage[i]==itemId)
{
 this._globalIdStorage[i]=newItemId;
}
};



 dhtmlXTreeObject.prototype.doCut=function(){
 if(this.nodeCut)this.clearCut();
 this.nodeCut=this.lastSelected;
 if(this.nodeCut)
{
 var tempa=this.nodeCut.parentObject;
 this.cutImg[0]=tempa.images[0];
 this.cutImg[1]=tempa.images[1];
 this.cutImg[2]=tempa.images[2];
 tempa.images[0]=tempa.images[1]=tempa.images[2]=this.cutImage;
 this._correctPlus(tempa);
}
};


 dhtmlXTreeObject.prototype.doPaste=function(itemId){
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;
 if(this.nodeCut){
 if((!this._checkParenNodes(this.nodeCut.parentObject.id,temp))&&(id!=this.nodeCut.parentObject.parentObject.id))
 this._moveNode(temp,this.nodeCut.parentObject);
 this.clearCut();
}
};


 dhtmlXTreeObject.prototype.clearCut=function(){
 if(this.nodeCut)
{
 var tempa=this.nodeCut.parentObject;
 tempa.images[0]=this.cutImg[0];
 tempa.images[1]=this.cutImg[1];
 tempa.images[2]=this.cutImg[2];
 if(tempa.parentObject)this._correctPlus(tempa);
 if(tempa.parentObject)this._correctLine(tempa);
 this.nodeCut=0;
}
};




 dhtmlXTreeObject.prototype._moveNode=function(itemObject,targetObject){
 if(this.dragFunc)if(!this.dragFunc(itemObject.id,targetObject.id))return false;
 if((targetObject.XMLload==0)&&(this.XMLsource))
{
 targetObject.XMLload=1;this.loadXML(this.XMLsource+getUrlSymbol(this.XMLsource)+"id="+escape(targetObject.id));
}
 this.openItem(targetObject.id);
 var oldTree=itemObject.treeNod;
 var c=itemObject.parentObject.childsCount;
 var z=itemObject.parentObject;
 var Count=targetObject.childsCount;var Nodes=targetObject.childNodes;
 Nodes[Count]=itemObject;
 itemObject.treeNod=targetObject.treeNod;
 targetObject.childsCount++;

 var tr=this._drawNewTr(Nodes[Count].htmlNode);
 targetObject.htmlNode.childNodes[0].appendChild(tr);

 itemObject.parentObject.htmlNode.childNodes[0].removeChild(itemObject.tr);


 for(var i=0;i<z.childsCount;i++)
 if(z.childNodes[i].id==itemObject.id){
 z.childNodes[i]=0;
 break;}
 oldTree._compressChildList(z.childsCount,z.childNodes);
 z.childsCount--;
 itemObject.tr=tr;
 tr.nodem=itemObject;

 itemObject.parentObject=targetObject;
 if(oldTree!=targetObject.treeNod){if(itemObject.treeNod._registerBranch(itemObject,oldTree))return;this._clearStyles(itemObject);};


 if(c>1){oldTree._correctPlus(z.childNodes[c-2]);
 oldTree._correctLine(z.childNodes[c-2]);}
 this._correctPlus(targetObject);
 this._correctLine(targetObject);
 oldTree._correctPlus(z);
 this._correctLine(itemObject);
 this._correctPlus(Nodes[Count]);
 if(targetObject.childsCount>=2)
{
 this._correctPlus(Nodes[targetObject.childsCount-2]);
 this._correctLine(Nodes[targetObject.childsCount-2]);
}
 if(this.tscheck)this._correctCheckStates(targetObject);
 if(oldTree.tscheck)oldTree._correctCheckStates(z);
 return true;
};


dhtmlXTreeObject.prototype._checkParenNodes=function(itemId,htmlObject,shtmlObject){
 if(shtmlObject){if(shtmlObject.parentObject.id==htmlObject.id)return 1;}
 if(htmlObject.id==itemId)return 1;
 if(htmlObject.parentObject)return this._checkParenNodes(itemId,htmlObject.parentObject);else return 0;
};




 dhtmlXTreeObject.prototype._clearStyles=function(itemObject){
 var td1=itemObject.htmlNode.childNodes[0].childNodes[0].childNodes[1];
 var td3=td1.nextSibling.nextSibling;

 if(this.checkBoxOff){td1.childNodes[0].style.display="";td1.childNodes[0].onclick=this.onCheckBoxClick;}
 else td1.childNodes[0].style.display="none";
 td1.childNodes[0].treeNod=this;

 this.dragger.removeDraggableItem(td3);
 if(this.dragAndDropOff)this.dragger.addDraggableItem(td3,this);
 td3.childNodes[0].className="standartTreeRow";
 td3.onclick=this.onRowSelect;td3.ondblclick=this.onRowClick2;
 td1.previousSibling.onclick=this.onRowClick;


 this._correctLine(itemObject);
 this._correctPlus(itemObject);
 for(var i=0;i<itemObject.childsCount;i++)this._clearStyles(itemObject.childNodes[i]);
};

 dhtmlXTreeObject.prototype._registerBranch=function(itemObject,oldTree){

 itemObject.id=this._globalIdStorageAdd(itemObject.id,itemObject);
 itemObject.treeNod=this;
 if(oldTree)oldTree._globalIdStorageSub(itemObject.id);
 for(var i=0;i<itemObject.childsCount;i++)
 this._registerBranch(itemObject.childNodes[i],oldTree);
 return 0;
};



 dhtmlXTreeObject.prototype.enableThreeStateCheckboxes=function(mode){this.tscheck=convertStringToBoolean(mode);};


 dhtmlXTreeObject.prototype.enableTreeImages=function(mode){this.timgen=convertStringToBoolean(mode);};



 dhtmlXTreeObject.prototype.enableFixedMode=function(mode){this.hfMode=convertStringToBoolean(mode);};


 dhtmlXTreeObject.prototype.enableCheckBoxes=function(mode){this.checkBoxOff=convertStringToBoolean(mode);};

 dhtmlXTreeObject.prototype.setStdImages=function(image1,image2,image3){
 this.imageArray[0]=image1;this.imageArray[1]=image2;this.imageArray[2]=image3;};


 dhtmlXTreeObject.prototype.enableTreeLines=function(mode){
 this.treeLinesOn=convertStringToBoolean(mode);
}


 dhtmlXTreeObject.prototype.setImageArrays=function(arrayName,image1,image2,image3,image4,image5){
 switch(arrayName){
 case "plus": this.plusArray[0]=image1;this.plusArray[1]=image2;this.plusArray[2]=image3;this.plusArray[3]=image4;this.plusArray[4]=image5;break;
 case "minus": this.minusArray[0]=image1;this.minusArray[1]=image2;this.minusArray[2]=image3;this.minusArray[3]=image4;this.minusArray[4]=image5;break;
}
};


 dhtmlXTreeObject.prototype.openItem=function(itemId){
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;
 this._HideShow(temp,2);
 if((temp.parentObject)&&(this._getOpenState(temp.parentObject)<0))
 this.openItem(temp.parentObject.id);
};


 dhtmlXTreeObject.prototype.closeItem=function(itemId){
 if(this.rootId==itemId)return 0;
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;
 if(temp.closeble)
 this._HideShow(temp,1);
};

 dhtmlXTreeObject.prototype.getLevel=function(itemId){
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;
 return this._getNodeLevel(temp,0);
};




 dhtmlXTreeObject.prototype.setItemCloseable=function(itemId,flag)
{
 flag=convertStringToBoolean(flag);
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;
 temp.closeble=flag;
};


 dhtmlXTreeObject.prototype._getNodeLevel=function(itemObject,count){
 if(itemObject.parentObject)return this._getNodeLevel(itemObject.parentObject,count+1);
 return(count);
};


 dhtmlXTreeObject.prototype.hasChildren=function(itemId){
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;
 else
{
 if((this.XMLsource)&&(!temp.XMLload))return true;
 else
 return temp.childsCount;
};
};



 dhtmlXTreeObject.prototype.setItemText=function(itemId,newLabel)
{
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;
 temp.label=newLabel;
 temp.htmlNode.childNodes[0].childNodes[0].childNodes[3].childNodes[0].innerHTML=newLabel;
};

 dhtmlXTreeObject.prototype.refreshItem=function(itemId){
 if(!itemId)itemId=this.rootId;
 var temp=this._globalIdStorageFind(itemId);
 this.deleteChildItems(itemId);
 this.loadXML(this.XMLsource+getUrlSymbol(this.XMLsource)+"id="+escape(itemId));
};


 dhtmlXTreeObject.prototype.setItemImage2=function(itemId,image1,image2,image3){
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;
 temp.images[1]=image2;
 temp.images[2]=image3;
 temp.images[0]=image1;
 this._correctPlus(temp);
};

 dhtmlXTreeObject.prototype.setItemImage=function(itemId,image1,image2)
{
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;
 if(image2)
{
 temp.images[1]=image1;
 temp.images[2]=image2;
}
 else temp.images[0]=image1;
 this._correctPlus(temp);
};



 dhtmlXTreeObject.prototype.getSubItems =function(itemId)
{
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;

 var z="";
 for(i=0;i<temp.childsCount;i++)
 if(!z)z=temp.childNodes[i].id;
 else z+=","+temp.childNodes[i].id;
 return z;
};

 dhtmlXTreeObject.prototype.getAllSubItems =function(itemId){
 return this._getAllSubItems(itemId);
}


 dhtmlXTreeObject.prototype._getAllSubItems =function(itemId,z,node)
{
 if(node)temp=node;
 else{
 var temp=this._globalIdStorageFind(itemId);
};
 if(!temp)return 0;

 z="";
 for(var i=0;i<temp.childsCount;i++)
{
 if(!z)z=temp.childNodes[i].id;
 else z+=","+temp.childNodes[i].id;
 var zb=this._getAllSubItems(0,z,temp.childNodes[i])
 if(zb)z+=","+zb;
}
 return z;
};




 dhtmlXTreeObject.prototype.selectItem=function(itemId,mode){
 mode=convertStringToBoolean(mode);
 var temp=this._globalIdStorageFind(itemId);
 if(!temp)return 0;
 if(this._getOpenState(temp.parentObject)==-1)
 this.openItem(itemId);
 if(mode)
 this.onRowSelect(0,temp.htmlNode.childNodes[0].childNodes[0].childNodes[3],false);
 else
 this.onRowSelect(0,temp.htmlNode.childNodes[0].childNodes[0].childNodes[3],true);
};


 dhtmlXTreeObject.prototype.getSelectedItemText=function()
{
 if(this.lastSelected)
 return this.lastSelected.parentObject.htmlNode.childNodes[0].childNodes[0].childNodes[3].childNodes[0].innerHTML;
 else return("");
};





 dhtmlXTreeObject.prototype._compressChildList=function(Count,Nodes)
{
 Count--;
 for(var i=0;i<Count;i++)
{
 if(Nodes[i]==0){Nodes[i]=Nodes[i+1];Nodes[i+1]=0;}
};
};

 dhtmlXTreeObject.prototype._deleteNode=function(itemId,htmlObject,skip){

 if(!skip){
 this._globalIdStorageRecSub(htmlObject);
}

 if((!htmlObject)||(!htmlObject.parentObject))return 0;
 var tempos=0;var tempos2=0;
 if(htmlObject.tr.nextSibling)tempos=htmlObject.tr.nextSibling.nodem;
 if(htmlObject.tr.previousSibling)tempos2=htmlObject.tr.previousSibling.nodem;

 var sN=htmlObject.parentObject;
 var Count=sN.childsCount;
 var Nodes=sN.childNodes;
 for(var i=0;i<Count;i++)
{
 if(Nodes[i].id==itemId){
 if(!skip)sN.htmlNode.childNodes[0].removeChild(Nodes[i].tr);
 Nodes[i]=0;
 break;
}
}
 this._compressChildList(Count,Nodes);
 if(!skip){
 sN.childsCount--;
}

 if(tempos){
 this._correctPlus(tempos);
 this._correctLine(tempos);
}
 if(tempos2){
 this._correctPlus(tempos2);
 this._correctLine(tempos2);
}
 if(this.tscheck)this._correctCheckStates(sN);
};

 dhtmlXTreeObject.prototype.setCheck=function(itemId,state){
 state=convertStringToBoolean(state);
 var sNode=this._globalIdStorageFind(itemId);
 if(!sNode)return;
 if(!this.tscheck)return this._setSubChecked(state,sNode);
 else this._setCheck(sNode,state);
 this._correctCheckStates(sNode.parentObject);
};

 dhtmlXTreeObject.prototype._setCheck=function(sNode,state){
 var z=sNode.htmlNode.childNodes[0].childNodes[0].childNodes[1].childNodes[0];
 if(state=="notsure")sNode.checkstate=2;
 else if(state)sNode.checkstate=1;else sNode.checkstate=0;

 z.src=this.imPath+this.checkArray[sNode.checkstate];
};


dhtmlXTreeObject.prototype.setSubChecked=function(itemId,state){
 var sNode=this._globalIdStorageFind(itemId);
 this._setSubChecked(state,sNode);
 this._correctCheckStates(sNode.parentObject);
}

 dhtmlXTreeObject.prototype._setSubChecked=function(state,sNode){
 state=convertStringToBoolean(state);
 if(!sNode)return;
 for(var i=0;i<sNode.childsCount;i++)
{
 this._setSubChecked(state,sNode.childNodes[i]);
};
 var z=sNode.htmlNode.childNodes[0].childNodes[0].childNodes[1].childNodes[0];
 if(state)sNode.checkstate=1;
 else sNode.checkstate=0;
 z.src=this.imPath+this.checkArray[sNode.checkstate];
};


 dhtmlXTreeObject.prototype.isItemChecked=function(itemId){
 var sNode=this._globalIdStorageFind(itemId);
 if(!sNode)return;
 return sNode.checkstate;
};






 dhtmlXTreeObject.prototype.getAllChecked=function(){
 return this._getAllChecked("","",1);
}

 dhtmlXTreeObject.prototype.getAllCheckedBranches=function(){
 return this._getAllChecked("","",0);
}


 dhtmlXTreeObject.prototype._getAllChecked=function(htmlNode,list,mode){
 if(!htmlNode)htmlNode=this.htmlNode;
 if(((mode)&&(htmlNode.checkstate==1))||((!mode)&&(htmlNode.checkstate>0))){if(list)list+=","+htmlNode.id;else list=htmlNode.id;}
 var j=htmlNode.childsCount;
 for(var i=0;i<j;i++)
{
 list=this._getAllChecked(htmlNode.childNodes[i],list,mode);
};
 if(list)return list;else return "";
};

 dhtmlXTreeObject.prototype.deleteChildItems=function(itemId)
{
 var sNode=this._globalIdStorageFind(itemId);
 if(!sNode)return;
 var j=sNode.childsCount;
 for(var i=0;i<j;i++)
{
 this._deleteNode(sNode.childNodes[0].id,sNode.childNodes[0]);
};
};


dhtmlXTreeObject.prototype.deleteItem=function(itemId,selectParent){
 this._deleteItem(itemId,selectParent);
}

dhtmlXTreeObject.prototype._deleteItem=function(itemId,selectParent,skip){
 selectParent=convertStringToBoolean(selectParent);
 var sNode=this._globalIdStorageFind(itemId);
 if(!sNode)return;
 if(selectParent)this.selectItem(this.getParentId(this.getSelectedItemId()),1);
 if(!skip){
 this._globalIdStorageRecSub(sNode);
};
 var zTemp=sNode.parentObject;
 this._deleteNode(itemId,sNode,skip);
 this._correctPlus(zTemp);
 this._correctLine(zTemp);
};


 dhtmlXTreeObject.prototype._globalIdStorageRecSub=function(itemObject){
 for(var i=0;i<itemObject.childsCount;i++)
{
 this._globalIdStorageRecSub(itemObject.childNodes[i]);
 this._globalIdStorageSub(itemObject.childNodes[i].id);
};
 this._globalIdStorageSub(itemObject.id);
};


 dhtmlXTreeObject.prototype.insertNewNext=function(parentItemId,itemId,itemName,itemActionHandler,image1,image2,image3,optionStr,childs){
 var sNode=this._globalIdStorageFind(parentItemId);
 if(!sNode)return(0);
 this._attachChildNode(0,itemId,itemName,itemActionHandler,image1,image2,image3,optionStr,childs,sNode);
};




 dhtmlXTreeObject.prototype.getItemIdByIndex=function(itemId,index){
 var z=this._globalIdStorageFind(itemId);
 if(!z)return 0;
 var temp=z.htmlNode.childNodes[0].childNodes[0];
 while(index>0)
{
 temp=temp.nextSibling;
 if((!temp)||(!temp.nodem))return 0;
 index--;
}
 return temp.nodem.id;
};

 dhtmlXTreeObject.prototype.getChildItemIdByIndex=function(itemId,index){
 var sNode=this._globalIdStorageFind(itemId);
 if(!sNode)return(0);
 if(this.hasChildren(itemId)<index)return 0;
 return sNode.htmlNode.childNodes[0].childNodes[index].nodem.id;
};






 dhtmlXTreeObject.prototype.setDragHandler=function(func){if(typeof(func)=="function")this.dragFunc=func;else this.dragFunc=eval(func);};


 dhtmlXTreeObject.prototype._clearMove=function(htmlNode){
 if(htmlNode.parentObject.span){
 htmlNode.parentObject.span.className='standartTreeRow';
 if(htmlNode.parentObject.acolor)htmlNode.parentObject.span.style.color=htmlNode.parentObject.acolor;
}
};


 dhtmlXTreeObject.prototype.enableDragAndDrop=function(mode){this.dragAndDropOff=convertStringToBoolean(mode);};


 dhtmlXTreeObject.prototype._setMove=function(htmlNode){
 if(htmlNode.parentObject.span){
 htmlNode.parentObject.span.className='selectedTreeRow';
 if(htmlNode.parentObject.scolor)htmlNode.parentObject.span.style.color=htmlNode.parentObject.scolor;
}
};



dhtmlXTreeObject.prototype._createDragNode=function(htmlObject){
 dhtmlObject=htmlObject.parentObject;
 if(this.lastSelected)this._clearMove(this.lastSelected);
 var dragSpan=document.createElement('div');
 dragSpan.appendChild(document.createTextNode(dhtmlObject.label));
 dragSpan.style.position="absolute";
 dragSpan.className="dragSpanDiv";
 return dragSpan;
}


















dhtmlXTreeObject.prototype._preventNsDrag=function(e){
 if((e)&&(e.preventDefault)){e.preventDefault();return false;}
}

dhtmlXTreeObject.prototype._drag=function(sourceHtmlObject,dhtmlObject,targetHtmlObject){
 if(!targetHtmlObject.parentObject){targetHtmlObject=this.htmlNode.htmlNode.childNodes[0].childNodes[0].childNodes[1].childNodes[0];}
 else this._clearMove(targetHtmlObject);
 if(dhtmlObject.lastSelected)dhtmlObject._setMove(dhtmlObject.lastSelected);
 if((!this.dragMove)||(this.dragMove()))this._moveNode(sourceHtmlObject.parentObject,targetHtmlObject.parentObject);

}

dhtmlXTreeObject.prototype._dragIn=function(htmlObject,shtmlObject){
 if(!htmlObject.parentObject)
{

 return htmlObject;
}
 if((!this._checkParenNodes(shtmlObject.parentObject.id,htmlObject.parentObject,shtmlObject.parentObject))&&(htmlObject.parentObject.id!=shtmlObject.parentObject.id))
{
 this._setMove(htmlObject);
 if(this._getOpenState(htmlObject.parentObject)<0)
 this._autoOpenTimer=window.setTimeout(new callerFunction(this._autoOpenItem,this),1000);
 this._autoOpenId=htmlObject.parentObject.id;
 return htmlObject;
}
 else return 0;
}
dhtmlXTreeObject.prototype._autoOpenItem=function(e,treeObject){
 treeObject.openItem(treeObject._autoOpenId);
};
dhtmlXTreeObject.prototype._dragOut=function(htmlObject){
 if(!htmlObject.parentObject)return 0;
this._clearMove(htmlObject);if(this._autoOpenTimer)clearTimeout(this._autoOpenTimer);}
//]]>

