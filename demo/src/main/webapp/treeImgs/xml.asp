<?xml version="1.0"?>
<%
Response.ContentType="text/xml"

Dim url_id
Dim intCurrPos
url_var=request.querystring("id")

Response.Write 	"<tree id='"+url_var+"'>"
         For intCurrPos = 0 To 4
			 Response.Write 	"<item child='1' id='"+url_var+CStr(intCurrPos)+"' text='Item "+url_var+"-"+CStr(intCurrPos)+"'></item>"
         Next
Response.Write 	"</tree>"
%> 
